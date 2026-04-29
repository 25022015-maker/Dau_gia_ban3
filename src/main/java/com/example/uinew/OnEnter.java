package com.example.uinew;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public interface OnEnter {
    // 1. Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    void onUsernameEnter(ActionEvent event);

    @FXML
    public void onPasswordEnter(ActionEvent event);
}
