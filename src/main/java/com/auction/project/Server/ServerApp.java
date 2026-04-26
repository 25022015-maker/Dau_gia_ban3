package com.auction.project.Server;

public class ServerApp {
    public static void main(String[] args) {
        SocketServer server = new SocketServer(1234);
        server.start();
    }
}