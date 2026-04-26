package com.auction.project.Server;

import com.auction.project.Packets.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private static ConcurrentHashMap<String, String> users = DataStorage.loadUsers();
    private static double currentBid = 0;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object obj;

            while ((obj = in.readObject()) != null) {

                if (obj instanceof LoginRequest req) {
                    handleLogin(req);
                }

                else if (obj instanceof BidRequest req) {
                    handleBid(req);
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }

    private void handleLogin(LoginRequest req) {
        users.put(req.getUsername(), req.getPassword());
        DataStorage.saveUsers(users);

        send(new Response("LOGIN", "Xin chào " + req.getUsername()));
    }

    private synchronized void handleBid(BidRequest req) {
        if (req.getAmount() > currentBid) {
            currentBid = req.getAmount();

            Response res = new Response("BID", "Giá mới: " + currentBid);
            SocketServer.broadcast(res);

        } else {
            send(new Response("ERROR", "Phải > " + currentBid));
        }
    }

    public void send(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}