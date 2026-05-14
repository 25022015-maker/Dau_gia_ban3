module auction.server {
    // Gọi module common (chứa Entity, Packet)
    requires auction.common;

    // Các thư viện bên ngoài dành cho Backend
    requires java.sql;       // Để dùng JDBC kết nối MySQL
    requires java.logging;   // Để ghi log (Logger)
    requires com.google.gson;

    // Export các package của server (để JVM có thể chạy được ServerApp)
    exports com.auction.project.Server;
    exports com.auction.project.Controllers;
    exports com.auction.project.DAO;

    // Mở package Server cho Gson
    // Lý do: Trong ClientHandlerObserver có class nội bộ BidUpdateData
    // cần được Gson chuyển thành JSON để gửi về Client.
    opens com.auction.project.Server to com.google.gson;
}