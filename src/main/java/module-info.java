module com.example.uinew {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens com.example.uinew to javafx.fxml;

    exports com.example.uinew;
    exports com.example.uinew.Controller;
    opens com.example.uinew.Controller to javafx.fxml;
    exports com.example.uinew.View;
    opens com.example.uinew.View to javafx.fxml;
    exports com.example.uinew.Interface;
    opens com.example.uinew.Interface to javafx.fxml;
    exports com.example.uinew.model;
    opens com.example.uinew.model to javafx.fxml;
    exports com.example.uinew.service;
    opens com.example.uinew.service to javafx.fxml;
}