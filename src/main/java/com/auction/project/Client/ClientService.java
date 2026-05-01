package com.auction.project.Client;

import com.auction.project.Entities.Bidder;
import com.auction.project.Manager.BidTransaction;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;

public class ClientService {
    private static ClientService instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;


    private BiConsumer<Integer, Double> onPriceUpdate;

    private ClientService() {}

    public static ClientService getInstance() {
        if (instance == null) instance = new ClientService();
        return instance;
    }

    /**
     * Kết nối và khởi động luồng nghe Real-time
     */
    public void connect(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());

        startListening();
    }

    /**
     * Luồng lắng nghe các cập nhật từ Server (Observer Pattern phía Client)
     */
    private void startListening() {
        Thread listener = new Thread(() -> {
            try {
                while (true) {
                    Object response = in.readObject();
                    if ("UPDATE_PRICE".equals(response)) {
                        int auctionId = (int) in.readObject();
                        double newPrice = (double) in.readObject();
                        String bidderName = (String) in.readObject();

                        System.out.println("[REAL-TIME] Đấu giá " + auctionId + " tăng lên " + newPrice);

                        if (onPriceUpdate != null) {
                            Platform.runLater(() -> onPriceUpdate.accept(auctionId, newPrice));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối với Server.");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public void setOnPriceUpdate(BiConsumer<Integer, Double> callback) {
        this.onPriceUpdate = callback;
    }


    public String placeBid(int auctionId, double amount) {
        try {
            out.writeObject("PLACE_BID");

            out.writeObject(auctionId);
            out.writeObject(amount);
            out.flush();

            // 3. Đợi nhận phản hồi kết quả (Thành công/Thất bại từ Server)
            Object result = in.readObject();
            return (String) result;

        } catch (IOException | ClassNotFoundException e) {
            return "Lỗi kết nối: " + e.getMessage();
        }
    }


    public void createLocalRecord(Bidder bidder, double amount) {
        BidTransaction bid = new BidTransaction(bidder, amount);
        System.out.println("Đã tạo bản ghi cục bộ cho: " + bidder.getUsername());
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}