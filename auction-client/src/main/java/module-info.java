module auction.client {
    requires auction.common; // Gọi module common
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires com.google.gson;

    opens com.example.uinew to javafx.fxml;
    opens com.auction.project.UI.Controller to javafx.fxml;
    opens com.auction.project.UI.View to javafx.fxml;

    exports com.auction.project.Client;
    exports com.auction.project.UI.Controller;
    exports com.auction.project.UI.View;
}