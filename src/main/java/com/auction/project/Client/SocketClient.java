package com.auction.project.Client;

import com.auction.project.Packets.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientHandler handler;
    private String username;

    public SocketClient(String host, int port) {
        try {
            socket  = new Socket(host, port);
            out     = new ObjectOutputStream(socket.getOutputStream());
            in      = new ObjectInputStream(socket.getInputStream());
            handler = new ClientHandler(in);
            handler.start();
            System.out.println("Connected to server at " + host + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Cho phép ClientApp / GUI đăng ký callback nhận response
    public void setOnResponse(Consumer<Response> onResponse) {
        handler.setOnResponse(onResponse);
    }

    public synchronized void send(Object req) {
        try {
            out.writeObject(req);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(String username, String password) {
        this.username = username;
        send(new LoginRequest(username, password));
    }

    public void bid(double price) {
        if (username == null) {
            System.err.println("Chưa đăng nhập, không thể đặt giá.");
            return;
        }
        send(new BidRequest(price, username));
    }

    public void close() {
        try {
            if (handler != null) handler.interrupt();
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected from server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}