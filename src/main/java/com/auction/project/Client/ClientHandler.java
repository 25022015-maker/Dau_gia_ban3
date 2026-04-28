package com.auction.project.Client;

import java.io.ObjectInputStream;

public class ClientHandler extends Thread {
    private ObjectInputStream in;

    public ClientHandler(ObjectInputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object response = in.readObject();
                System.out.println("Server says: " + response);
            }
        } catch (Exception e) {
            System.out.println("Disconnected from server.");
        }
    }
}
