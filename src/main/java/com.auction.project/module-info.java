module com.example.dau_gia_ban3 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.dau_gia_ban3 to javafx.fxml;
    exports com.example.dau_gia_ban3;
}