package com.example.service;

import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    // Đổi thành IP máy chạy server nếu client chạy trên máy khác, VD: "http://192.168.1.10:8080"
    public static final String BASE_URL = System.getProperty("server.url", "http://10.11.201.1:8080");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new GsonBuilder().create();

    // ── Auth ──────────────────────────────────────────────────────────────────

    public static JsonObject login(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        return post("/api/auth/login", body.toString(), false);
    }

    public static JsonObject register(String username, String email, String password, String role) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("role", role);
        return post("/api/auth/register", body.toString(), false);
    }

    // ── Auction ───────────────────────────────────────────────────────────────

    public static java.time.LocalDateTime getServerTime() {
        try {
            HttpRequest req = anonBuilder("/api/time").GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            String raw = resp.body();
            String timeStr = JsonParser.parseString(raw).getAsJsonObject().get("serverTime").getAsString();
            timeStr = timeStr.length() > 19 ? timeStr.substring(0, 19) : timeStr;
            return java.time.LocalDateTime.parse(timeStr);
        } catch (Exception e) {
            return java.time.LocalDateTime.now();
        }
    }

    public static JsonArray getAuctions() {
        String raw = get("/api/auctions");
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    public static JsonObject getAuction(long id) {
        String raw = get("/api/auctions/" + id);
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    public static JsonArray getBidHistory(long auctionId) {
        String raw = get("/api/auctions/" + auctionId + "/bids");
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    public static JsonObject placeBid(long auctionId, long amount) {
        JsonObject body = new JsonObject();
        body.addProperty("auctionId", auctionId);
        body.addProperty("amount", amount);
        return post("/api/auctions/" + auctionId + "/bid", body.toString(), true);
    }

    public static void registerAutoBid(long auctionId, long maxBid, long increment) {
        JsonObject body = new JsonObject();
        body.addProperty("auctionId", auctionId);
        body.addProperty("maxBid", maxBid);
        body.addProperty("increment", increment);
        postRaw("/api/auctions/" + auctionId + "/auto-bid", body.toString());
    }

    public static JsonObject createAuction(JsonObject req) {
        return post("/api/auctions", GSON.toJson(req), true);
    }

    public static void cancelAuction(long auctionId) {
        delete("/api/auctions/" + auctionId);
    }

    // ── User ──────────────────────────────────────────────────────────────────

    public static JsonObject getProfile() {
        String raw = get("/api/users/me");
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    public static JsonObject topUp(long amount) {
        String body = "{\"amount\":" + amount + "}";
        return post("/api/users/me/top-up", body, true);
    }

    public static JsonArray getNotifications() {
        String raw = get("/api/users/me/notifications");
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    public static void markNotificationsRead() {
        putRaw("/api/users/me/notifications/read");
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    public static JsonObject getAdminStats() {
        String raw = get("/api/admin/stats");
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    public static JsonArray getAllUsers() {
        String raw = get("/api/admin/users");
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    public static JsonObject updateUserStatus(long userId, String status) {
        String body = "{\"status\":\"" + status + "\"}";
        return put("/api/admin/users/" + userId + "/status", body);
    }

    public static JsonArray getAdminAuctions() {
        String raw = get("/api/admin/auctions");
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    public static void approveAuction(long auctionId) {
        putRaw("/api/admin/auctions/" + auctionId + "/approve");
    }

    public static void adminCancelAuction(long auctionId) {
        putRaw("/api/admin/auctions/" + auctionId + "/cancel");
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static String get(String path) {
        try {
            HttpRequest req = authedBuilder(path).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
            return resp.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static JsonObject post(String path, String body, boolean auth) {
        try {
            HttpRequest.Builder b = auth ? authedBuilder(path) : anonBuilder(path);
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
            return JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static void postRaw(String path, String body) {
        try {
            HttpRequest req = authedBuilder(path)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static JsonObject put(String path, String body) {
        try {
            HttpRequest req = authedBuilder(path)
                    .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
            return JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static void putRaw(String path) {
        try {
            HttpRequest req = authedBuilder(path)
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static void delete(String path) {
        try {
            HttpRequest req = authedBuilder(path).DELETE().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối: " + e.getMessage(), e);
        }
    }

    private static HttpRequest.Builder authedBuilder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
        String token = SessionManager.getToken();
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private static HttpRequest.Builder anonBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    private static void checkStatus(HttpResponse<String> resp) {
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) return;
        try {
            JsonObject err = JsonParser.parseString(resp.body()).getAsJsonObject();
            String msg = err.has("error") ? err.get("error").getAsString() : resp.body();
            throw new RuntimeException(msg);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }
}
