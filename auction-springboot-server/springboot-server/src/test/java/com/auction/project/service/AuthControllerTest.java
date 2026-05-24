package com.auction.project.service;

import com.auction.project.controller.AuthController;
import com.auction.project.dto.AuthResponse;
import com.auction.project.dto.LoginRequest;
import com.auction.project.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Bỏ qua việc lọc JWT thực tế khi test Controller
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // --- THÊM 2 DÒNG MOCK BEAN NÀY ĐỂ GIẢI QUYẾT LỖI KHỞI TẠO CONTEXT ---
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("API đăng nhập trả về mã HTTP 200 và dữ liệu JSON hợp lệ")
    void login_ValidPayload_ReturnsOkAndJson() throws Exception {
        LoginRequest req = new LoginRequest("testuser", "password123");
        AuthResponse response = new AuthResponse("jwtTokenStr", 1L, "testuser", "BIDDER");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwtTokenStr"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("BIDDER"));
    }

    @Test
    @DisplayName("API đăng nhập trả về mã lỗi HTTP 400 khi thiếu thông tin bắt buộc")
    void login_InvalidPayload_ReturnsBadRequest() throws Exception {
        LoginRequest req = new LoginRequest("", ""); // Thiếu thông tin đầu vào

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}