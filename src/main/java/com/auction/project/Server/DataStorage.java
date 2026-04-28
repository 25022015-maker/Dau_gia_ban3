package com.auction.project.Server;

import com.auction.project.Packets.BidRequest;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {

    private static final String USER_FILE = "users.dat";
    private static final String BID_FILE = "bids.dat";

    // Lưu danh sách user
    public static void saveUsers(ConcurrentHashMap<String, String> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
            System.out.println("Đã lưu " + users.size() + " users vào " + USER_FILE);
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tải danh sách user
    @SuppressWarnings("unchecked")
    public static ConcurrentHashMap<String, String> loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            System.out.println("Chưa có file users, tạo mới.");
            return new ConcurrentHashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_FILE))) {
            return (ConcurrentHashMap<String, String>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Lỗi khi đọc users: " + e.getMessage());
            e.printStackTrace();
            return new ConcurrentHashMap<>();
        }
    }

    // Lưu danh sách bid
    public static void saveBids(List<BidRequest> bids) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(BID_FILE))) {
            oos.writeObject(bids);
            System.out.println("Đã lưu " + bids.size() + " bids vào " + BID_FILE);
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu bids: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tải danh sách bid
    @SuppressWarnings("unchecked")
    public static List<BidRequest> loadBids() {
        File file = new File(BID_FILE);
        if (!file.exists()) {
            System.out.println("Chưa có file bids, tạo mới.");
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(BID_FILE))) {
            return (List<BidRequest>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Lỗi khi đọc bids: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
