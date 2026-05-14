package com.auction.project.UI.Interface;

import javafx.event.ActionEvent;

public interface SceneLoader {
    void changeScene(ActionEvent event, String fxmlFile, String title);
}
