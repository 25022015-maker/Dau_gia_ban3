package com.auction.project.service;

import com.auction.project.dto.UserProfileResponse;
import com.auction.project.entity.User;
import com.auction.project.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository        userRepo;
    private final SimpMessagingTemplate ws;

    public UserService(UserRepository userRepo, SimpMessagingTemplate ws) {
        this.userRepo = userRepo;
        this.ws       = ws;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserProfileResponse updateStatus(Long userId, String status) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + userId));
        u.setStatus(status);
        UserProfileResponse result = toResponse(userRepo.save(u));
        if ("BANNED".equals(status)) {
            ws.convertAndSend("/topic/user/" + userId, Map.of("type", "BANNED"));
        }
        return result;
    }

    /** Nạp tiền vào tài khoản */
    @Transactional
    public UserProfileResponse topUp(String username, Long amount) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
        if (amount <= 0) throw new RuntimeException("Số tiền nạp phải lớn hơn 0");
        u.setBalance(u.getBalance() + amount);
        return toResponse(userRepo.save(u));
    }

    private UserProfileResponse toResponse(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole().name(),
                u.getBalance(),
                u.getStatus(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }
}
