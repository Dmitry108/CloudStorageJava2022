package ru.aglar;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class CloudController implements Initializable, EventHandler<ActionEvent> {
    @FXML public TableView<FileInfo> localFilesTable;
    @FXML public TableView<FileInfo> remoteFilesTable;
    @FXML public MenuItem exitMenuItem;
    @FXML public Button sendButton;

    private NetIo net;
    private File clientDir;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 888);
            this.net = new NetIo(socket, this::onReceiveMessage);
            this.clientDir = new File("cloud_client", "local_storage");
            Platform.runLater(() -> sendButton.getScene().getWindow().setOnCloseRequest(event -> this.exit()));
            exitMenuItem.setOnAction(this);
            sendButton.setOnAction(this);
            initFileTable(localFilesTable, clientDir);
        } catch (IOException e) {
            showException("Connection is not established!");
            e.printStackTrace();
        }
    }

    private void initFileTable(TableView<FileInfo> table, File file) {
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileInfo, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileInfo, String> param) {
                return new SimpleStringProperty(param.getValue().getFilename());
            }
        });
        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileInfo, Long>, ObservableValue<Long>>() {
            @Override
            public ObservableValue<Long> call(TableColumn.CellDataFeatures<FileInfo, Long> param) {
                return new SimpleObjectProperty<Long>(param.getValue().getSize());
            }
        });
        sizeColumn.setCellFactory(new Callback<TableColumn<FileInfo, Long>, TableCell<FileInfo, Long>>() {
            @Override
            public TableCell<FileInfo, Long> call(TableColumn<FileInfo, Long> param) {
                return new TableCell<FileInfo, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        this.setText(empty || item == -1L ? null : String.format("%,d bytes", item));
                    }
                };
            }
        });
        table.getColumns().addAll(filenameColumn, sizeColumn);
        table.getSortOrder().add(sizeColumn);
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                localFilesTable.getItems().add(new FileInfo(f));
            }
        }
    }

    @Override
    public void handle(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(exitMenuItem)) {
            exit();
        } else if (source.equals(sendButton)) {
            if (localFilesTable.getSelectionModel().getSelectedItem() != null) {
                sendFile(new File(clientDir, localFilesTable.getSelectionModel().getSelectedItem().getFilename()));
            }
        }
    }

    private void sendFile(File file) {
        try {
            net.sendMessage("$file");
            net.sendMessage(file.getName().replaceAll(" +", "_"));
            net.sendFileSize(file.length());
            try (FileInputStream fis = new FileInputStream(file)){
                int read;
                byte[] buf = new byte[4096];
                while ((read = fis.read(buf)) > 0) {
                    net.sendFile(buf, 0, read);
                }
            }
        } catch (IOException e) {
            showException("Error on sending file!");
            e.printStackTrace();
        }
    }

    private void onReceiveMessage(String message) {
        System.out.println(message);
    }

    private void showException(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
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