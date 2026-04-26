package com.auction.project.Server;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorage {

    public static void saveUsers(ConcurrentHashMap<String, String> users) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream("users.dat"))) {

            oos.writeObject(users);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<String, String> loadUsers() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream("users.dat"))) {

            return (ConcurrentHashMap<String, String>) ois.readObject();

        } catch (Exception e) {
            return new ConcurrentHashMap<>();
        }
    }
}