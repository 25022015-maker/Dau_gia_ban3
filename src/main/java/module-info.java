module com.example.uinew {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens com.example.uinew to javafx.fxml;

    exports com.example.uinew;
    exports com.example.uinew.Controller;
    opens com.example.uinew.Controller to javafx.fxml;
}