package com.auction.project.Client;

import com.auction.project.Packets.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientHandler handler;

    public SocketClient(String host, int port) {
        try {
            // Kết nối tới server
            socket = new Socket(host, port);

            // Tạo luồng vào/ra
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Khởi động thread lắng nghe phản hồi từ server
            handler = new ClientHandler(in);
            handler.start();

            System.out.println("Connected to server at " + host + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gửi object tới server (thread-safe)
    public synchronized void send(Object req) {
        try {
            out.writeObject(req);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gửi yêu cầu đăng nhập
    public void login(String username, String password) {
        send(new LoginRequest(username, password));
    }

    // Gửi yêu cầu đặt giá
    public void bid(double price) {
        send(new BidRequest(price));
    }

    // Đóng kết nối gọn gàng
    public void close() {
        try {
            if (handler != null) handler.interrupt();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected from server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
