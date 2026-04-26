package com.auction.project.Client;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {

        SocketClient client = new SocketClient("localhost", 1234);
        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        String user = sc.nextLine();

        client.login(user, "123");

        while (true) {
            System.out.print("Nhập giá: ");
            double price = sc.nextDouble();

            client.bid(price);
        }
    }
}