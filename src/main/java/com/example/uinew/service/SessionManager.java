package com.example.uinew.service;

import com.auction.project.Client.SocketClient;

/**
 * Lưu trạng thái đăng nhập toàn cục.
 * Dùng static để truy cập từ bất kỳ Controller nào mà không cần truyền tham số.
 */
public class SessionManager {

    private static String currentUser;
    private static SocketClient socketClient;

    public static void setCurrentUser(String username) { currentUser = username; }
    public static String getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }

    public static void setSocketClient(SocketClient client) { socketClient = client; }
    public static SocketClient getSocketClient() { return socketClient; }

    public static void logout() {
        currentUser = null;
        if (socketClient != null) {
            socketClient.disconnect();
            socketClient = null;
        }
    }
}