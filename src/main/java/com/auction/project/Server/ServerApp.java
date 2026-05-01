package com.auction.project.Server;

import com.auction.project.Entities.Auction;
import com.auction.project.Entities.Item;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final int PORT = 12345;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void startServer() {
        System.out.println("--- HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (SERVER) ---");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[INFO] Server đang lắng nghe tại cổng: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[CONNECT] Thiết bị mới: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Lỗi Server: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
    public static void main(String[] args) {
        new ServerApp().startServer();
    }
}