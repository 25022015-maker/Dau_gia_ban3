package com.example.uinew.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public interface OnEnter {
    // 1. Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    void onUsernameEnter(ActionEvent event);

    @FXML
     void onPasswordEnter(ActionEvent event);
}
