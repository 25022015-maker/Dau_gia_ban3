package com.auction.project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServer {

    private int port;
    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public SocketServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server running at port " + port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connected: " + socket);

                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(Object msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    public static void main(String[] args) {
        new SocketServer(1234).start();
    }
}