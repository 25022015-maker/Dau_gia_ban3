package com.auction.project.service;

import com.auction.project.dto.AuthResponse;
import com.auction.project.dto.LoginRequest;
import com.auction.project.dto.RegisterRequest;
import com.auction.project.entity.User;
import com.auction.project.entity.enums.Role;
import com.auction.project.repository.UserRepository;
import com.auction.project.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý đăng ký và đăng nhập.
 * Mật khẩu lưu dạng BCrypt hash, không bao giờ lưu plain text.
 */
@Service
public class
AuthService {

    private final UserRepository       userRepo;
    private final PasswordEncoder      encoder;
    private final JwtUtil              jwtUtil;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder,
                       JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.userRepo    = userRepo;
        this.encoder     = encoder;
        this.jwtUtil     = jwtUtil;
        this.authManager = authManager;
    }

    /**
     * Đăng ký tài khoản mới.
     * Kiểm tra username và email chưa tồn tại trước khi lưu.
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.username())) {
            throw new RuntimeException("Username đã tồn tại: " + req.username());
        }
        if (userRepo.existsByEmail(req.email())) {
            throw new RuntimeException("Email đã được sử dụng: " + req.email());
        }

        // Chỉ cho phép BIDDER hoặc SELLER qua đăng ký công khai
        Role role;
        try {
            role = Role.valueOf(req.role().toUpperCase());
            if (role == Role.ADMIN) role = Role.BIDDER;
        } catch (IllegalArgumentException e) {
            role = Role.BIDDER;
        }

        User user = new User(req.username(), encoder.encode(req.password()), req.email(), role);
        User saved = userRepo.save(user);

        String token = jwtUtil.generateToken(saved.getUsername());
        return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getRole().name());
    }

    /**
     * Đăng nhập - Spring Security xác thực username/password,
     * ném BadCredentialsException nếu sai (bắt bởi GlobalExceptionHandler).
     */
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if ("BANNED".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ admin.");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }
}
