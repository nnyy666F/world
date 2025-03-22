package com.mirror.world;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainGui extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainGui.class.getResource("gui.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1300, 800);
        scene.getStylesheets().add(getClass().getResource("blocks.css").toExternalForm());
        stage.setTitle("World -v0.1.1");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}