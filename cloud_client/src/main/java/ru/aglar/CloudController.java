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
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class CloudController implements Initializable, EventHandler<ActionEvent>, ResponseListener {
    @FXML public TableView<FileInfo> localFilesTable;
    @FXML public TableView<FileInfo> remoteFilesTable;
    @FXML public MenuItem exitMenuItem;
    @FXML public Button sendButton;
    @FXML public Button downloadButton;
    @FXML public ComboBox<String> localDiskComboBox;
    @FXML public TextField localPathTextField;
    @FXML public Button localUpButton;

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
        FileSystems.getDefault().getRootDirectories().forEach(disk ->
                localDiskComboBox.getItems().add(disk.toString()));
        refreshLocalFileTable(clientDir);
        refreshRemoteFileTable();
        Platform.runLater(() -> sendButton.getScene().getWindow().setOnCloseRequest(event -> this.exit()));
        exitMenuItem.setOnAction(this);
        sendButton.setOnAction(this);
        downloadButton.setOnAction(this);
//        localUpButton.setOnAction(this);
//        localFilesTable.setOnMouseClicked(this::onTableClicked);
        initFileTable(localFilesTable);
        initFileTable(remoteFilesTable);
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

    private void refreshLocalFileTable(Path path) {
        try {
            localPathTextField.setText(path.normalize().toAbsolutePath().toString());
            localFilesTable.getItems().clear();
            Files.list(path).forEach(p -> localFilesTable.getItems().add(new FileInfo(p)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handle(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(exitMenuItem)) {
            exit();
        } else if (source.equals(sendButton)) {
            if (localFilesTable.getSelectionModel().getSelectedItem() != null) {
                sendFile(localFilesTable.getSelectionModel().getSelectedItem().getFilename());
            }
        } else if (source.equals(downloadButton)) {
            if (remoteFilesTable.getSelectionModel().getSelectedItem() != null) {
                sendFileRequest(remoteFilesTable.getSelectionModel().getSelectedItem().getFilename());
            }
        } else if (source.equals(localUpButton)) {
            Path path = Paths.get(localPathTextField.getText()).getParent();
            if (path != null) {
                refreshLocalFileTable(path);
            }
        }
    }

    private void onTableClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Path path = Paths.get(localPathTextField.getText(),
                    ((TableView<FileInfo>)event.getSource()).getSelectionModel().getSelectedItem().getFilename());
            if (Files.isDirectory(path)) {
                refreshLocalFileTable(path);
            }
        }
    }

    private void sendFileRequest(String filename) {
        try {
            if (Files.list(clientDir).anyMatch(file -> file.getFileName().toString().equals(filename))) {
                onMessageReceive(String.format("File %s already exists!", filename));
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Network.getInstance().sendFileRequest(filename);
    }

    private void showException(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    public void exit() {
        Network.getInstance().stop();
        Platform.exit();
    }

    @Override
    public void onReceiveFile(FileInfo fileInfo) {
        if (localPathTextField.getText().equals(clientDir.normalize().toAbsolutePath().toString())) {
            refreshLocalFileTable(Paths.get(localPathTextField.getText()));
        }
    }

    @Override
    public void onMessageReceive(String message) {
        System.out.println(message);
    }

    @Override
    public void onFileStructureReceive(List<FileInfo> filesList) {
        fillRemoteFileTable(filesList);
    }

    @Override
    public void sendFile(String filename) {
        Path file = clientDir.resolve(filename);
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

    @Override
    public void onExit() {
        onMessageReceive("Server stopped!");
        Network.getInstance().stop();
    }
}