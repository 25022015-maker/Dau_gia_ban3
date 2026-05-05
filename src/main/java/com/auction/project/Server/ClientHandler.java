package com.auction.project.Server;

import com.auction.project.Packets.*;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler extends Thread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // ── Shared state (static = dùng chung giữa tất cả ClientHandler) ─────────
    private static final ConcurrentHashMap<String, String> users = DataStorage.loadUsers();

    // dùng volatile để các thread luôn đọc giá trị mới nhất
    private static volatile double currentBid = 0;

    // Lưu lịch sử bid để có thể serialize xuống file
    private static final List<BidRequest> bidHistory = new CopyOnWriteArrayList<>(DataStorage.loadBids());

    //  lock dùng chung cho TẤT CẢ ClientHandler (class-level lock)
    private static final Object BID_LOCK = new Object();

    // ── Constructor ───────────────────────────────────────────────────────────

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ── Getter để ServerApp/SocketServer truy cập cho auto-save ──────────────

    public static ConcurrentHashMap<String, String> getUsers() {
        return users;
    }

    public static List<BidRequest> getBidHistory() {
        return bidHistory;
    }

    public static double getCurrentBid() {
        return currentBid;
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof LoginRequest req) {
                    handleLogin(req);
                } else if (obj instanceof BidRequest req) {
                    handleBid(req);
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            SocketServer.clients.remove(this);
            closeResources();
        }
    }

    // ── Xử lý login ──────────────────────────────────────────────────────────

    private void handleLogin(LoginRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();

        if (users.containsKey(username)) {
            if (users.get(username).equals(password)) {
                send(new Response("LOGIN", "Xin chào " + username));
            } else {
                send(new Response("ERROR", "Sai mật khẩu"));
            }
        } else {
            users.put(username, password);
            DataStorage.saveUsers(users);   // lưu ngay khi có user mới
            send(new Response("LOGIN", "Tạo mới tài khoản: " + username));
        }
    }

    // ── Xử lý bid ─────────────────────────────────────────────────────────────
    //
    //  FIX: synchronized trên BID_LOCK (static) thay vì 'this'
    //  → đảm bảo chỉ 1 thread xử lý bid tại một thời điểm dù có nhiều client
    //
    private void handleBid(BidRequest req) {
        synchronized (BID_LOCK) {
            if (req.getAmount() > currentBid) {
                currentBid = req.getAmount();
                bidHistory.add(req);        // lưu vào lịch sử

                Response res = new Response("BID", "Giá mới: " + currentBid);
                SocketServer.broadcast(res);
            } else {
                send(new Response("ERROR", "Giá phải lớn hơn " + currentBid));
            }
        }
    }

    // ── Gửi object về client ──────────────────────────────────────────────────

    public void send(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            System.err.println("Lỗi gửi dữ liệu: " + e.getMessage());
        }
    }

    // ── Đóng tài nguyên ───────────────────────────────────────────────────────

    private void closeResources() {
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}