module com.auction.project { // Hoặc giữ tên cũ cũng được nhưng nên đổi
        requires javafx.controls;
        requires javafx.fxml;
        // ... các dòng requires khác giữ nguyên ...
        requires org.controlsfx.controls;
        requires net.synedra.validatorfx;
        requires org.kordamp.bootstrapfx.core;

        opens com.auction.project to javafx.fxml;
        exports com.auction.project;
        }