package com.auction.project.UI.service;

import com.auction.project.Client.NetworkClient;

/**
 * Lưu trạng thái đăng nhập toàn cục.
 * Dùng static để truy cập từ bất kỳ Controller nào mà không cần truyền tham số.
 */
public class SessionManager {

    private static String currentUser;
    private static NetworkClient networkClient;

    public static void setCurrentUser(String username) { currentUser = username; }
    public static String getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }

    public static void setNetworkClient(NetworkClient client) { networkClient = client; }
    public static NetworkClient getNetworkClient() { return networkClient; }

    public static void logout() {
        currentUser = null;
        if (networkClient != null) {
            networkClient.disconnect();
            networkClient = null;
        }
    }
}