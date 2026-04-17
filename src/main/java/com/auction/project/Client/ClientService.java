package com.auction.project.Client;

import com.auction.project.Common.entitiesclasses.BidTransaction;
import com.auction.project.Common.entitiesclasses.Bidder;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientService {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /**
     * Kết nối tới Server (Tuần 9:  Triển khai Java Socket)
     */
    public void connect(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        // Khởi tạo ObjectOutputStream trước để tránh lỗi treo luồng (Stream Corrupted)
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Gửi một lượt trả giá lên Server (Triển khai Serialization)
     * @param amount Số tiền người dùng muốn đặt
     * @param bidder Đối tượng người dùng đang tham gia đấu giá
     */
    public void sendBid(double amount, Bidder bidder) {
        try{
            // Truyền vào: số tiền, thời gian hiện tại, và đối tượng bidder
            BidTransaction bid = new BidTransaction(amount, LocalDateTime.now(), bidder);

            // Gửi đối tượng qua mạng (Serialization)
            out.writeObject(bid);
            out.flush(); // Đẩy dữ liệu đi ngay lập tức, không chờ bộ đệm đầy

            // Đợi nhận phản hồi từ Server (Server xử lý đồng thời)
            Object response = in.readObject();
            if (response instanceof String) {
                System.out.println("Phản hồi từ Server: " + response);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Lỗi khi gửi dữ liệu đấu giá: " + e.getMessage());
        }
    }

    /**
     * Đóng kết nối khi thoát ứng dụng
     */
    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}