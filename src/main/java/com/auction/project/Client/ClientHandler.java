package com.auction.project.Client;
import com.auction.project.Packets.Response;
import java.io.ObjectInputStream;

public class ClientHandler extends Thread {
    private ObjectInputStream in;
    public ClientHandler(ObjectInputStream in) {
        this.in = in;
    }
    public void run() {
        try {
            while (true) {
                Object obj = in.readObject();

                if (obj instanceof Response res) {
                    System.out.println("SERVER: " + res.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Disconnected");
        }
    }
}