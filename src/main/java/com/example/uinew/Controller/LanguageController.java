package com.example.uinew.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class LanguageController {

    @FXML private HBox cardVI;
    @FXML private HBox cardEN;
    @FXML private Label checkVI;
    @FXML private Label checkEN;
    @FXML private Label lblSaved;

    private String currentLang = "VI"; // mặc định Tiếng Việt

    @FXML
    public void initialize() {
        updateUI();
    }

    @FXML
    public void selectVietnamese() {
        currentLang = "VI";
        updateUI();
        showSaved("✅ Đã chuyển sang Tiếng Việt");
    }

    @FXML
    public void selectEnglish() {
        currentLang = "EN";
        updateUI();
        showSaved("✅ Switched to English");
    }

    private void updateUI() {
        String active = "-fx-background-color: #16213e; -fx-background-radius: 10;" +
                "-fx-border-color: #6c3483; -fx-border-radius: 10; -fx-border-width: 2;" +
                "-fx-padding: 20; -fx-cursor: hand;";
        String normal = "-fx-background-color: #16213e; -fx-background-radius: 10;" +
                "-fx-border-color: #2d2d4e; -fx-border-radius: 10; -fx-border-width: 2;" +
                "-fx-padding: 20; -fx-cursor: hand;";

        cardVI.setStyle("VI".equals(currentLang) ? active : normal);
        cardEN.setStyle("EN".equals(currentLang) ? active : normal);
        checkVI.setVisible("VI".equals(currentLang));
        checkEN.setVisible("EN".equals(currentLang));
    }

    private void showSaved(String msg) {
        lblSaved.setText(msg);
        lblSaved.setVisible(true);
        lblSaved.setManaged(true);
    }
}