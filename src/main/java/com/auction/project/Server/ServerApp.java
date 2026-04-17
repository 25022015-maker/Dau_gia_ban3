package com.auction.project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static final int PORT = 12345; // Cổng kết nối của Server

    public void startServer() {
        // Tạo ServerSocket lắng nghe tại cổng 12345
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đấu giá đang chạy tại cổng: " + PORT);

            while (true) {
                // Chấp nhận một kết nối mới từ Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có thiết bị mới kết nối: " + clientSocket.getInetAddress());

                // Tạo một luồng riêng (ClientHandler) cho Client này và bắt đầu chạy
                // Giúp Server xử lý đồng thời nhiều người (Concurrency - Tuần 10)
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ServerApp().startServer();
    }
}