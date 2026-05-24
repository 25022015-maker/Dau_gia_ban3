package com.auction.project.service;

import com.auction.project.dto.AuthResponse;
import com.auction.project.dto.LoginRequest;
import com.auction.project.dto.RegisterRequest;
import com.auction.project.entity.User;
import com.auction.project.entity.enums.Role;
import com.auction.project.repository.UserRepository;
import com.auction.project.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Đăng ký tài khoản mới thành công")
    void register_NewUser_ReturnsAuthResponse() {
        RegisterRequest req = new RegisterRequest("newuser", "password123", "new@example.com", "BIDDER");
        User savedUser = new User("newuser", "encodedPassword", "new@example.com", Role.BIDDER);
        savedUser.setId(10L);

        when(userRepo.existsByUsername("newuser")).thenReturn(false);
        when(userRepo.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepo.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("newuser")).thenReturn("mockedJwtToken");

        AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("mockedJwtToken", response.token());
        assertEquals("newuser", response.username());
        assertEquals("BIDDER", response.role());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi trùng tên đăng nhập")
    void register_DuplicateUsername_ThrowsRuntimeException() {
        RegisterRequest req = new RegisterRequest("existinguser", "password123", "new@example.com", "BIDDER");
        when(userRepo.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("Đăng nhập thành công và trả về token")
    void login_ValidCredentials_ReturnsAuthResponse() {
        LoginRequest req = new LoginRequest("testuser", "password123");
        User user = new User("testuser", "encodedPassword", "test@example.com", Role.BIDDER);
        user.setId(1L);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("testuser")).thenReturn("generatedToken");

        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("generatedToken", response.token());
        verify(authManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}