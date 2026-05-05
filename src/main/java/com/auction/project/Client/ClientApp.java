package com.auction.project.Client;

import com.auction.project.Packets.Response;

import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        SocketClient client = new SocketClient("localhost", 1234);
        Scanner sc = new Scanner(System.in);

        // Đăng ký callback: server gửi gì → in ra ngay lập tức
        client.setOnResponse(res -> {
            String prefix = "ERROR".equals(res.getType()) ? "[!] " : "[✓] ";
            System.out.println("\n" + prefix + res.getMessage());
            System.out.print("Nhập giá (0 để thoát): "); // in lại prompt
        });

        System.out.print("Username: ");
        String user = sc.nextLine();

        System.out.print("Password: ");
        String pass = sc.nextLine();

        client.login(user, pass);

        while (true) {
            System.out.print("Nhập giá (0 để thoát): ");
            String input = sc.nextLine().trim();

            try {
                double price = Double.parseDouble(input);
                if (price == 0) {
                    client.close();
                    sc.close();
                    break;
                }
                client.bid(price);
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số!");
            }
        }
    }
}