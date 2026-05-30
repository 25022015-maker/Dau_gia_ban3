package com.auction.project.service;

import com.auction.project.dto.UserProfileResponse;
import com.auction.project.entity.User;
import com.auction.project.entity.enums.Role;
import com.auction.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private SimpMessagingTemplate ws;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "passwordHash", "test@example.com", Role.BIDDER);
        testUser.setId(1L);
        testUser.setBalance(100_000L);
        testUser.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Lấy thông tin cá nhân thành công")
    void getProfile_ValidUsername_ReturnsProfile() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getProfile("testuser");

        assertNotNull(response);
        assertEquals("testuser", response.username());
        assertEquals("test@example.com", response.email());
        assertEquals(100_000L, response.balance());
    }

    @Test
    @DisplayName("Lấy thông tin cá nhân thất bại khi không tìm thấy user")
    void getProfile_UserNotFound_ThrowsRuntimeException() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getProfile("unknown"));
    }

    @Test
    @DisplayName("Nạp tiền hợp lệ thành công")
    void topUp_ValidAmount_UpdatesBalance() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.topUp("testuser", 50_000L);

        assertEquals(150_000L, response.balance());
        verify(userRepo, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Nạp tiền không hợp lệ (nhỏ hơn hoặc bằng 0) ném ngoại lệ")
    void topUp_InvalidAmount_ThrowsRuntimeException() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.topUp("testuser", 0L));
        assertThrows(RuntimeException.class, () -> userService.topUp("testuser", -500L));
    }

    @Test
    @DisplayName("Cập nhật trạng thái tài khoản thành công")
    void updateStatus_ValidId_UpdatesStatus() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateStatus(1L, "BANNED");

        assertEquals("BANNED", response.status());
        verify(userRepo, times(1)).save(testUser);
    }
}