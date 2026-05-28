package com.example.service;

import java.time.Duration;
import java.time.LocalDateTime;

public class SessionManager {

    private static String   token;
    private static Long     userId;
    private static String   username;
    private static String   role;
    private static long     balance;
    private static Duration serverOffset = Duration.ZERO;

    public static void setSession(String token, Long userId, String username, String role, long balance) {
        SessionManager.token    = token;
        SessionManager.userId   = userId;
        SessionManager.username = username;
        SessionManager.role     = role;
        SessionManager.balance  = balance;
    }

    public static void clear() {
        token = null; userId = null; username = null; role = null; balance = 0;
        serverOffset = Duration.ZERO;
        StompClient.getInstance().disconnect();
    }

    /** Lưu độ lệch (server time − client time) để dùng serverNow(). */
    public static void syncServerOffset(LocalDateTime serverTime) {
        serverOffset = Duration.between(LocalDateTime.now(), serverTime);
    }

    /** Trả về thời gian hiện tại theo đồng hồ server. */
    public static LocalDateTime serverNow() {
        return LocalDateTime.now().plus(serverOffset);
    }

    public static void setBalance(long b) { balance = b; }

    public static String  getToken()    { return token; }
    public static Long    getUserId()   { return userId; }
    public static String  getUsername() { return username; }
    public static String  getRole()     { return role; }
    public static long    getBalance()  { return balance; }
    public static boolean isLoggedIn()  { return token != null; }
    public static boolean isAdmin()     { return "ADMIN".equals(role); }
    public static boolean isSeller()    { return "SELLER".equals(role) || "ADMIN".equals(role); }
}
