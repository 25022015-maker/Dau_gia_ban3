package com.auction.project.service;

import com.auction.project.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Giả lập giá trị cấu hình từ application.properties
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "mySecretKeyForTestingMustBeLongEnoughToMeetHmacRequirements");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L); // 1 giờ
    }

    @Test
    @DisplayName("Tạo và trích xuất thành công username từ token hợp lệ")
    void generateAndExtractToken_ValidData_ReturnsUsername() {
        String username = "auctionUser";

        String token = jwtUtil.generateToken(username);
        assertNotNull(token);

        assertTrue(jwtUtil.validate(token));
        assertEquals(username, jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("Token không hợp lệ trả về kết quả validate sai")
    void validate_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalidHeader.invalidPayload.invalidSignature";

        assertFalse(jwtUtil.validate(invalidToken));
    }
}