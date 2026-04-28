package com.auction.project.Server;

public class ServerApp {
    public static void main(String[] args) {
        int port = 1234;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port không hợp lệ, dùng mặc định: 1234");
            }
        }

        try {
            SocketServer server = new SocketServer(port);
            System.out.println("Server started on port " + port);
            server.start();
        } catch (Exception e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
