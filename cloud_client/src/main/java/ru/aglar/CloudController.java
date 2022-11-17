package ru.aglar;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class CloudController implements Initializable, EventHandler<ActionEvent>, ViewCallback {
    @FXML public TableView<FileInfo> localFilesTable;
    @FXML public TableView<FileInfo> remoteFilesTable;
    @FXML public MenuItem exitMenuItem;
    @FXML public Button sendButton;

//    private Network net;
    private Path clientDir;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(() -> {
            Network.getInstance().start(this, cdl);
        }).start();
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Network.getInstance().getSocketChannel().writeAndFlush(CloudProtocol.getFilesStructureRequest());
        this.clientDir = Paths.get("local_storage");
        Platform.runLater(() -> sendButton.getScene().getWindow().setOnCloseRequest(event -> this.exit()));
        exitMenuItem.setOnAction(this);
        sendButton.setOnAction(this);
        initFileTable(localFilesTable, clientDir);
    }

    private void initFileTable(TableView<FileInfo> table, Path path) {
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<Long>(param.getValue().getSize()));
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
        try {
            Files.list(path).forEach(p -> localFilesTable.getItems().add(new FileInfo(p.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initFileTable(TableView<FileInfo> table, List<String> filenames) {
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        table.getColumns().addAll(filenameColumn);
        filenames.forEach(filename -> {
            remoteFilesTable.getItems().add(new FileInfo(filename, -1L));
        });
    }

    @Override
    public void handle(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(exitMenuItem)) {
            exit();
        } else if (source.equals(sendButton)) {
            if (localFilesTable.getSelectionModel().getSelectedItem() != null) {
                sendFile(new File(clientDir.toFile(), localFilesTable.getSelectionModel().getSelectedItem().getFilename()));
            }
        }
    }

    private void sendFile(File file) {
        Network.getInstance().sendFile(file.toPath(), null);
    }

    @Override
    public void onReceiveMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void filledRemoteFiles(List<String> filenames) {
        initFileTable(remoteFilesTable, filenames);
    }

    private void showException(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    public void exit() {
        Network.getInstance().stop();
        Platform.exit();
    }
}