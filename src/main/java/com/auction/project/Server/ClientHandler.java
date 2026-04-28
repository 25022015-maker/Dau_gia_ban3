package com.auction.project.Server;

import com.auction.project.Packets.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // Lưu danh sách user (username -> password)
    private static ConcurrentHashMap<String, String> users = DataStorage.loadUsers();
    // Giá hiện tại của phiên đấu giá
    private static double currentBid = 0;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
            // Xóa client khỏi danh sách khi disconnect
            SocketServer.clients.remove(this);
            closeResources();
        }
    }

    // Xử lý login
    private void handleLogin(LoginRequest req) {
        if (users.containsKey(req.getUsername())) {
            if (users.get(req.getUsername()).equals(req.getPassword())) {
                send(new Response("LOGIN", "Xin chào " + req.getUsername()));
            } else {
                send(new Response("ERROR", "Sai mật khẩu"));
            }
        } else {
            users.put(req.getUsername(), req.getPassword());
            DataStorage.saveUsers(users);
            send(new Response("LOGIN", "Tạo mới tài khoản: " + req.getUsername()));
        }
    }

    // Xử lý bid (đồng bộ để tránh race condition)
    private synchronized void handleBid(BidRequest req) {
        if (req.getAmount() > currentBid) {
            currentBid = req.getAmount();
            Response res = new Response("BID", "Giá mới: " + currentBid);
            SocketServer.broadcast(res);
        } else {
            send(new Response("ERROR", "Phải > " + currentBid));
        }
    }

    // Gửi dữ liệu về client
    public void send(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Đóng socket và stream
    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
