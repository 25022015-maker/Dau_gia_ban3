package com.auction.project.Client;

import com.auction.project.Packets.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public SocketClient(String host, int port) {
        try {
            socket = new Socket(host, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new ClientHandler(in).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Object req) {
        try {
            out.writeObject(req);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(String u, String p) {
        send(new LoginRequest(u, p));
    }

    public void bid(double price) {
        send(new BidRequest(price));
    }
}