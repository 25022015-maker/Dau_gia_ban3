module com.auction.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.management;
    requires java.logging;
    requires com.google.gson;
    requires java.sql;

    opens com.example.uinew to javafx.fxml;

    exports com.auction.project.UI.Controller;
    opens com.auction.project.UI.Controller to javafx.fxml, com.google.gson;
    exports com.auction.project.UI.View;
    opens com.auction.project.UI.View to javafx.fxml;
    exports com.auction.project.UI.Interface;
    opens com.auction.project.UI.Interface to javafx.fxml;
    exports com.auction.project.Entities;
    opens com.auction.project.Entities to javafx.fxml, com.google.gson;
    exports com.auction.project.UI.service;
    opens com.auction.project.UI.service to javafx.fxml, com.google.gson;

    // Mở các package backend để UI có thể truy cập
    opens com.auction.project.Client to javafx.fxml;
    opens com.auction.project.Packets to javafx.fxml, com.google.gson;
}