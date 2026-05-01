package com.auction.project.Server;

import com.auction.project.Entities.*;
import com.auction.project.Exception.AuctionClosedException;
import com.auction.project.Exception.InvalidBidException;
import com.auction.project.Manager.*;
import com.auction.project.Observer.Observer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ClientHandler implements Runnable, Observer {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                String command = (String) in.readObject();
                handleCommand(command);
            }
        } catch (Exception e) {
            System.out.println("Thiết bị ngắt kết nối .");
        }
    }

    private void handleCommand(String command) throws IOException, ClassNotFoundException {
        AuctionManager manager = AuctionManager.getInstance();

        switch (command) {
            case "LOGIN":
                String username = (String) in.readObject();
                this.currentUser = new Bidder(username, username + "@auction.com");
                out.writeObject("SUCCESS");
                break;

            case "PLACE_BID":
                int auctionId = (int) in.readObject();
                double amount = (double) in.readObject();
                Auction auction = manager.getAuction(auctionId);

                synchronized (auction) {
                    boolean success = auction.placeBid((Bidder) currentUser, amount);
                    out.writeObject(success ? "BID_ACCEPTED" : "BID_REJECTED");
                    auction.registerObserver(this);
                }
                break;

            case "GET_ALL_AUCTIONS":
                out.writeObject(manager.getAllAuctions());
                break;
        }
        out.flush();
    }

    @Override
    public void update(int auctionId, double newPrice, String bidderName) {
        try {
            out.writeObject("UPDATE_PRICE");
            out.writeObject(auctionId);
            out.writeObject(newPrice);
            out.writeObject(bidderName);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleBid(int auctionId, double amount) {
        try {
            Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction == null) {
                out.writeObject("ERROR: Không tìm thấy phiên đấu giá.");
                return;
            }

            auction.placeBid((Bidder) this.currentUser, amount);

            out.writeObject("SUCCESS: Đặt giá thành công.");

        } catch (InvalidBidException | AuctionClosedException e) {
            try {
                out.writeObject("BID_FAILED: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            try {
                out.writeObject("ERROR: Lỗi hệ thống.");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}