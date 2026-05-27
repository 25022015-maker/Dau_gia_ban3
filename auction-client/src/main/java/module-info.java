module auction.client {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires com.google.gson;
    requires java.net.http;

    opens com.example.controller to javafx.fxml;

    exports com.example.app;
}