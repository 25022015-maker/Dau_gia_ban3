package com.auction.project.Client;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        SocketClient client = new SocketClient("localhost", 1234);
        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        String user = sc.nextLine();

        System.out.print("Password: ");
        String pass = sc.nextLine();

        client.login(user, pass);
        System.out.println("Đã gửi yêu cầu đăng nhập.");

        while (true) {
            System.out.print("Nhập giá (0 để thoát): ");
            if (sc.hasNextDouble()) {
                double price = sc.nextDouble();
                if (price == 0) {
                    client.close();
                    sc.close();
                    break;
                }
                client.bid(price);
            } else {
                System.out.println("Vui lòng nhập số!");
                sc.next(); // bỏ input sai
            }
        }
    }
}
