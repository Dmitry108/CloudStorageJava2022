package ru.aglar;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CloudController implements Initializable, EventHandler<ActionEvent>, ResponseListener {
    @FXML public TableView<FileInfo> localFilesTable;
    @FXML public TableView<FileInfo> remoteFilesTable;
    @FXML public MenuItem exitMenuItem;
    @FXML public Button sendButton;
    @FXML public Button downloadButton;

    private Path clientDir;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.clientDir = Paths.get("local_storage");
        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(() -> {
            Network.getInstance().start(this, clientDir, cdl);
        }).start();
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        refreshRemoteFileTable();
        Platform.runLater(() -> sendButton.getScene().getWindow().setOnCloseRequest(event -> this.exit()));
        exitMenuItem.setOnAction(this);
        sendButton.setOnAction(this);
        downloadButton.setOnAction(this);
        initFileTable(localFilesTable);
        initFileTable(remoteFilesTable);
        fillLocalFileTable();
    }

    private void initFileTable(TableView<FileInfo> table) {
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
    }

    public void fillRemoteFileTable(List<FileInfo> files) {
        remoteFilesTable.getItems().clear();
        files.forEach(file -> remoteFilesTable.getItems().add(file));
    }

    public void refreshRemoteFileTable() {
        Network.getInstance().getSocketChannel().writeAndFlush(CloudProtocol.getFilesStructureRequest());
    }

    public void fillLocalFileTable() {
        try {
            Files.list(clientDir).forEach(p -> localFilesTable.getItems().add(new FileInfo(p.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(exitMenuItem)) {
            exit();
        } else if (source.equals(sendButton)) {
            if (localFilesTable.getSelectionModel().getSelectedItem() != null) {
                sendFile(clientDir.resolve(localFilesTable.getSelectionModel().getSelectedItem().getFilename()));
            }
        }
    }

    private void sendFile(Path file) {
        boolean isExists = remoteFilesTable.getItems().stream()
                .anyMatch(fileInfo -> fileInfo.getFilename().equals(file.getFileName().toString()));
        if (isExists) {
            onMessageReceive("File with such name already exists on server!");
        } else {
            Network.getInstance().sendFile(file, new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    refreshRemoteFileTable();
                }
            });
        }
    }

    private void showException(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    public void exit() {
        Network.getInstance().stop();
        Platform.exit();
    }

    @Override
    public void onSuccess(Object response, Class<?> responseType) {

    }

    @Override
    public void onReceiveFile(FileInfo fileInfo) {

    }

    @Override
    public void onMessageReceive(String message) {
        System.out.println(message);
    }

    @Override
    public void onFileStructureReceive(List<FileInfo> filesList) {
        fillRemoteFileTable(filesList);
    }
}