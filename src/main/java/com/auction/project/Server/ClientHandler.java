package com.auction.project.Server;

import com.auction.project.Common.entitiesclasses.BidTransaction;
import java.io.*;
import java.net.Socket;

/**
 * Lớp xử lý từng Client riêng biệt (Tuần 10: Xử lý đa luồng)
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Thiết lập luồng gửi/nhận đối tượng (Tuần 9: Serialization) [cite: 11]
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                // 1. Đọc đối tượng gửi từ Client
                Object obj = in.readObject();

                // 2. Kiểm tra nếu đối tượng nhận được là một giao dịch đấu giá
                if (obj instanceof BidTransaction) {
                    BidTransaction bid = (BidTransaction) obj;

                    // 3. Sử dụng các phương thức đã định nghĩa trong BidTransaction
                    double bidPrice = bid.getAmount();      // Lấy số tiền từ getAmount()
                    String bidderInfo = bid.getBidder();    // Lấy thông tin người đấu giá từ bidder.getInfo()

                    // Ghi chú: bid.getInfo() trả về LocalDateTime (timestamp)
                    System.out.println("--- GIAO DỊCH MỚI ---");
                    System.out.println("Người đặt: " + bidderInfo);
                    System.out.println("Mức giá: " + bidPrice);
                    System.out.println("Thời gian: " + bid.getInfo());

                    // 4. Phản hồi lại cho Client (đáp ứng yêu cầu thông báo realtime cơ bản)
                    out.writeObject("Hệ thống: Đã ghi nhận mức giá " + bidPrice + " từ " + bidderInfo);
                    out.flush();// du lieu chuyen tu may minh sang may khac
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Xử lý khi người dùng tắt ứng dụng hoặc lỗi kết nối [cite: 56, 60]
            System.err.println("Lỗi kết nối với Client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}