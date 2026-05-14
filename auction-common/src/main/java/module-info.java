module auction.common {
    // Thư viện bên ngoài cần thiết
    requires com.google.gson;

    // Cho phép các module khác (Client, Server) gọi đến các package này
    exports com.auction.project.Entities;
    exports com.auction.project.Entities.enums;
    exports com.auction.project.Packets;
    exports com.auction.project.Exception;
    exports com.auction.project.Observer;
    exports com.auction.project.Factory;
    exports com.auction.project.ManagerServer; // Chứa class BidTransaction dùng chung

    // RẤT QUAN TRỌNG: Cho phép Gson dùng Reflection để đọc/ghi dữ liệu private
    // từ các class Entities và Packets thành chuỗi JSON.
    opens com.auction.project.Entities to com.google.gson;
    opens com.auction.project.Packets to com.google.gson;
}