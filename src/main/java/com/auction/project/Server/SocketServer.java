package com.auction.project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {

    private int port;
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ExecutorService pool = Executors.newCachedThreadPool();

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
                pool.execute(handler);
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
