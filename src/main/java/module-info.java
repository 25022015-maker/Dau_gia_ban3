module com.auction.project {
        requires javafx.controls;
        requires javafx.fxml;
        requires org.controlsfx.controls;
        requires net.synedra.validatorfx;
        requires org.kordamp.bootstrapfx.core;
        requires java.desktop;

        opens com.auction.project to javafx.fxml;
        exports com.auction.project;
        }