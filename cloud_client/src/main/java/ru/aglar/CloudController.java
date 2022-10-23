package ru.aglar;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Modality;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class CloudController implements Initializable, EventHandler<ActionEvent> {
    @FXML public TableView localFilesTable;
    public Button sendButton;
    public MenuItem exitMenuItem;

    private NetIo net;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 888);
            this.net = new NetIo(socket);
            Platform.runLater(()-> sendButton.getScene().getWindow().setOnCloseRequest(event -> this.exit()));
            exitMenuItem.setOnAction(this);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Connection is not established!", ButtonType.OK).show();
            e.printStackTrace();
        }
    }

    @Override
    public void handle(ActionEvent event) {
        if (event.getSource().equals(exitMenuItem)) {
            exit();
        }
    }

    public void exit() {
        try {
            net.close();
            Platform.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}