package com.example.task1MyZip;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.*;
import java.util.List;
import java.util.regex.Pattern;
import javafx.concurrent.Task;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import javafx.util.StringConverter;


public class HelloApplication extends Application {

    private static final String ILLEGAL_CHARACTERS_PATTERN = "[/\\:*?\"<>|]";
    private int currentIndex = 0;
    private int unZipMode = 1;
    String defalutName = "new.zip" ;
    String defalutFilePath = System.getProperty("user.home") + File.separator + "Desktop"+File.separator;
    StringProperty fs = new SimpleStringProperty("0 B");
    private ObservableList<FileInfo> fileList = FXCollections.observableArrayList();
    private Font font1 = Font.font( "1",FontWeight.BOLD, 15);
    private Font font2 = Font.font( "YaHei",17);
    private ImageView[] images = {
            new ImageView(new Image("image0.png")),
            new ImageView(new Image("image1.png")),
            new ImageView(new Image("image2.png")),
            new ImageView(new Image("image3.png")),
            new ImageView(new Image("image4.png")),
    };

    public void saveSettings(String fileName, String defaultName, String defaultFilePath, int unZipMode) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("defaultName=" + defaultName);
            writer.newLine();
            writer.write("defaultFilePath=" + defaultFilePath);
            writer.newLine();
            writer.write("unZipMode=" + unZipMode);
        } catch (IOException e) {
            String msg = e.getMessage();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setContentText(msg);
            alert.show();
        }
    }
    public void loadSettings(String fileName, Settings settings) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("defaultName=")) {
                    settings.setDefaultName(line.substring("defaultName=".length()));
                } else if (line.startsWith("defaultFilePath=")) {
                    settings.setDefaultFilePath(line.substring("defaultFilePath=".length()));
                } else if (line.startsWith("unZipMode=")) {
                    settings.setUnZipMode(Integer.parseInt(line.substring("unZipMode=".length())));
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setContentText(msg);
            alert.show();
        }
    }

    public String getFileSize(File file){
        if(file.isFile()){

            return String.valueOf(file.length());
        }
        else{
            long sum = 0;
            File[] f = file.listFiles();
            for(File fi : f ){
                sum = sum + Long.parseLong(getFileSize(fi));
            }

            return String.valueOf(sum);
        }
    };

    public String convertSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String determineFileType(File file) {
        if (file.isDirectory()) {
            return "directory";
        } else {
            String filename = new String(file.getName());
            String fileType = filename.substring(filename.lastIndexOf(".")+1).toLowerCase();
            if(fileType.isEmpty()){
                fileType = "data";
            }
            return fileType;
        }
    }

    private long updateFileSize(TableView<FileInfo> tableView){
        long totalSize = 0;

        for(FileInfo item : tableView.getItems()){
            long size = item.getSize();
            totalSize += size;
        }
         return totalSize;

    }

    private boolean isFileNameExists(String filename){
        for(FileInfo f : fileList){
            if(filename.equals(f.getName())){
                return true;
            }
        }
        return false;
    }

    private void openSettings(Stage owner) {

        // 创建弹出窗口
        Stage stageSettings = new Stage();
        stageSettings.initModality(Modality.WINDOW_MODAL); // 设置为模态窗口
        stageSettings.initOwner(owner); // 设置主窗口为弹出窗口的所有者

        BorderPane pane = new BorderPane();
        VBox vbox = new VBox(10);

        Label label1 = new Label("默认压缩文件名：");
        label1.setFont(font1);
        TextField textField = new TextField(defalutName);
        textField.setPrefColumnCount(33);
        Button btn = new Button("确定");
        btn.setOnAction(event ->{
            defalutName = textField.getText();
            saveSettings("Settings.txt",defalutName,defalutFilePath,unZipMode);
        });
        HBox hb0 = new HBox();
        hb0.setSpacing(10);
        hb0.getChildren().addAll(textField,btn);


        Label label2 = new Label("解压模式选择：");
        label2.setFont(font1);
        ToggleGroup group1  = new ToggleGroup();
        RadioButton rb1 = new RadioButton("标准模式：解压选中的ZIP文件，每个文件夹对应一个压缩包");
        rb1.setToggleGroup(group1);
        RadioButton rb2 = new RadioButton("递归模式：如果压缩文件中还包含ZIP文件，对于嵌套的ZIP文件也会进行解压");
        rb2.setToggleGroup(group1);
        rb1.setUserData(1);
        rb2.setUserData(2);
        if(unZipMode == 2){
            rb2.setSelected(true);
        } else{
            rb1.setSelected(true);
        }
        group1.selectedToggleProperty().addListener((ov, old_toggle, new_toggle) -> {
            if (new_toggle != null) {
                RadioButton selectedRadioButton = (RadioButton) new_toggle;
                unZipMode = (int)selectedRadioButton.getUserData();
                saveSettings("Settings.txt",defalutName,defalutFilePath,unZipMode);
            }
        });

        Label label3 = new Label("解压文件时，默认解压文件存放路径：");
        label3.setFont(font1);
        TextField filePath = new TextField();
        filePath.setEditable(true);
        filePath.setPrefColumnCount(33);
        // 设置默认路径为桌面路径
        filePath.setText(defalutFilePath);
        //filePath.setText(System.getProperty("user.home") + File.separator + "Desktop"+File.separator);

        // 创建按钮用于打开文件夹选择对话框
        Button browseButton = new Button("浏览...");
        browseButton.setOnAction(event1 -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择文件夹");
            // 设置初始目录为桌面
            File initialDirectory = new File(System.getProperty("user.home") + File.separator + "Desktop");
            directoryChooser.setInitialDirectory(initialDirectory);
            File selectedDirectory = directoryChooser.showDialog(stageSettings);
            if (selectedDirectory != null) {
                filePath.setText(selectedDirectory.getAbsolutePath()+File.separator);
            }
        });
        Button btn1 = new Button("确定");
        btn1.setOnAction(event ->{
            defalutFilePath=filePath.getText();
            saveSettings("Settings.txt",defalutName,defalutFilePath,unZipMode);
        });
        HBox hb = new HBox();
        hb.setSpacing(10);
        hb.getChildren().addAll(filePath,browseButton,btn1);

        vbox.getChildren().addAll(label1,hb0,label2,rb1,rb2,label3,hb);
        vbox.setPadding(new Insets(10,10,10,10));
        pane.setCenter(vbox);
        Scene scene = new Scene(pane,550,245);
        stageSettings.setScene(scene);
        stageSettings.setTitle("设置");
        stageSettings.setResizable(false);
        stageSettings.show();

        // 设置弹出窗口的位置相对于主窗口的偏移量
        double offsetX = 100; // X轴偏移量
        double offsetY = 100; // Y轴偏移量
        stageSettings.setX(owner.getX() + offsetX);
        stageSettings.setY(owner.getY() + offsetY);

        stageSettings.show(); // 显示弹出窗口
    }

    private void executeZipTask(Task task,TableView tableView,Stage stage2,File f){
        // 启动任务
        Thread zipThread = new Thread(task);
        zipThread.start();
        Stage stage = new Stage();
        ProgressBar progressBar = new ProgressBar();
        // 将任务的进度和消息绑定到进度条和状态标签上
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded((event) -> {
            tableView.getItems().clear();
            fs.set(convertSize(updateFileSize(tableView)));
            stage.close();
        });

        task.setOnFailed((event) -> {
            Throwable throwable = task.getException();

            if(throwable != null){
                if(throwable instanceof FileListEmptyException){
                    String msg = ((FileListEmptyException)throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("提示");
                    alert.setContentText(msg);
                    alert.show();
                }
                else if(throwable instanceof FileNameErrorException){
                    String msg = ((FileNameErrorException) throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
                else if(throwable instanceof FolderPathErrorException){
                    String msg =  ((FolderPathErrorException) throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
                else{
                    String msg = "未知错误：\n"+throwable.getMessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
            }
            stage.close();
        });
        Label label1 = new Label("正在压缩...");
        label1.setFont(font1);
        Label statusLabel = new Label();
        statusLabel.setFont(font1);
        statusLabel.textProperty().bind(task.messageProperty());
        Label canLabel = new Label("正在取消...");
        canLabel.setVisible(false);
        Button stopbtn = new Button("取消");
        stopbtn.setOnAction(event ->{
            task.cancel(true);
            canLabel.setVisible(true);
            int maxValue = 10;
            int c= 0;

            while (!f.delete() && c <= maxValue) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    String msg1 = e.getMessage();
                    Alert alert1 = new Alert(Alert.AlertType.ERROR);
                    alert1.setTitle("错误");
                    alert1.setContentText(msg1);
                    alert1.show();
                }
            }
            if(!f.exists()){
                stage.close();
            }
            else{
                stage.close();
                String msg2 = "无法删除未压缩完成的文件(超时)，请手动删除！\n文件路径：\n"+f.getAbsolutePath();
                Alert alert2 = new Alert(Alert.AlertType.WARNING);
                alert2.setTitle("提示");
                alert2.setContentText(msg2);
                alert2.show();
            }

        });

        stage.setResizable(false);
        stage.setOnCloseRequest(event -> {
            event.consume();
        });

        GridPane gridPane = new GridPane();
        gridPane.add(label1,0,0);
        gridPane.add(progressBar,0,1);
        gridPane.add(statusLabel,1,1);
        gridPane.add(canLabel,0,2);
        gridPane.add(stopbtn,2,2);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        Scene scene = new Scene(gridPane, 210, 100);
        stage.setScene(scene);

        // 设置弹出窗口的位置相对于主窗口的偏移量
        double offsetX = 170; // X轴偏移量
        double offsetY = 100; // Y轴偏移量
        stage.setX(stage2.getX() + offsetX);
        stage.setY(stage2.getY() + offsetY);

        stage.show();
    }

    private void executeUnZipTask(Task task,TableView tableView,Stage stage2){
        // 启动任务
        Thread UnzipThread = new Thread(task);
        UnzipThread.start();
        Stage stage = new Stage();
        ProgressBar progressBar = new ProgressBar();
        // 将任务的进度和消息绑定到进度条和状态标签上
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded((event) -> {
            tableView.getItems().clear();
            fs.set(convertSize(updateFileSize(tableView)));
            stage.close();
        });

        task.setOnFailed((event) -> {
            Throwable throwable = task.getException();

            if(throwable != null){
                if(throwable instanceof FileListEmptyException){
                    String msg = ((FileListEmptyException)throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("提示");
                    alert.setContentText(msg);
                    alert.show();
                }
                else if(throwable instanceof FileNameErrorException){
                    String msg = ((FileNameErrorException) throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
                else if(throwable instanceof FolderPathErrorException){
                    String msg =  ((FolderPathErrorException) throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
                else if(throwable instanceof ZipFileEmptyException){
                    String msg =  ((ZipFileEmptyException) throwable).getmessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
                else{
                    String msg = "取消解压任务或未知错误：\n请检查压缩文件是否正常\n"+throwable.getMessage();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
            }
            stage.close();
        });
        Label label1 = new Label("正在解压...");
        label1.setFont(font1);
        Label statusLabel = new Label();
        statusLabel.setFont(font1);
        statusLabel.textProperty().bind(task.messageProperty());
        Button stopbtn = new Button("取消");
        stopbtn.setOnAction(event ->{
            task.cancel(true);
            stage.close();
        });

        stage.setOnCloseRequest(event -> {
            event.consume();
        });

        GridPane gridPane = new GridPane();
        gridPane.add(label1,0,0);
        gridPane.add(progressBar,0,1);
        gridPane.add(statusLabel,1,1);
        gridPane.add(stopbtn,2,2);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        Scene scene = new Scene(gridPane, 210, 100);
        stage.setScene(scene);

        // 设置弹出窗口的位置相对于主窗口的偏移量
        double offsetX = 170; // X轴偏移量
        double offsetY = 100; // Y轴偏移量
        stage.setX(stage2.getX() + offsetX);
        stage.setY(stage2.getY() + offsetY);

        stage.show();
    }

    private void openZipPage(Stage mainStage){
        Stage stage2 = new Stage();
        stage2.initModality(Modality.WINDOW_MODAL); // 设置为模态窗口
        stage2.initOwner(mainStage);
        BorderPane pane1 = new BorderPane();
        TableView<FileInfo> tableView = new TableView<>();
        tableView.setEditable(true);
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Filename");
        filenameColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.3));
        filenameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        filenameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        filenameColumn.setOnEditCommit(event -> {
            FileInfo rowData = event.getRowValue();
            String newValue = event.getNewValue();
            if(isFileNameExists(newValue)){
                String msg = "文件名已存在，不允许修改！";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setContentText(msg);
                alert.show();
                rowData.filenameProperty().set(event.getOldValue());
                tableView.refresh();
            } else if (Pattern.compile(ILLEGAL_CHARACTERS_PATTERN).matcher(newValue).find()) {
                String msg = "文件名不能存在下列字符：/\\:*?\"<>|";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setContentText(msg);
                alert.show();
                rowData.filenameProperty().set(event.getOldValue());
                tableView.refresh();
            } else{
                rowData.filenameProperty().set(newValue);
            }

        });
        filenameColumn.setCellFactory(column -> new TextFieldTableCell<FileInfo, String>(new StringConverter<String>() {
            @Override
            public String toString(String value) {
                return value != null ? value : "";
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        }) {
            private final Tooltip tooltip = new Tooltip("选中后单击文件名单元格可修改文件名，按下Enter键保存修改");

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    // tooltip.show(stage2,900,270);
                    tooltip.setShowDelay(Duration.millis(1000));
                    tooltip.setHideDelay(Duration.millis(3000));
                    setTooltip(tooltip); // 设置固定的提示
                }
            }


        });

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
        TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("File Size(Bytes)");
        sizeColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.2));
        TableColumn<FileInfo, String> locationColumn = new TableColumn<>("Location");
        locationColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.4));
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().fileSizeProperty());
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().locationProperty());

        tableView.getColumns().addAll(filenameColumn,typeColumn,sizeColumn,locationColumn);
        tableView.setItems(fileList);




        VBox buttonBox = new VBox(5); // 5是按钮之间的间距
        buttonBox.setPadding(new Insets(3)); // 内边距
        buttonBox.setMinWidth(90);

        Button addFileButton = new Button("Add File(s)");
        addFileButton.setOnAction(fcevent ->{
            Stage fileChooseStage = new Stage();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose File(s)");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt","*.pdf","*.doc","*.docx","*.ppt","*.pptx","*.xls"),
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
            );
            File cacheFile = new File("cache.txt");
            if (cacheFile.exists()) {
                try (InputStream inputStream = new FileInputStream(cacheFile)) {
                    byte[] bytes = new byte[(int) cacheFile.length()];
                    // Read the contents of the cache.txt
                    inputStream.read(bytes);
                    File directory = new File(new String(bytes));
                    if (directory.exists()) {
                        fileChooser.setInitialDirectory(directory);
                    }
                } catch (IOException e) {
                    String msg = "配置文件目录或其他错误";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                    File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                    fileChooser.setInitialDirectory(iniDir);
                }
            }
            else{
                File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                fileChooser.setInitialDirectory(iniDir);
            }

            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(fileChooseStage);
            try {
                if (!selectedFiles.isEmpty()) {
                    // Store the directory to the cache.txt
                    try (OutputStream outputStream = new FileOutputStream(cacheFile)) {
                        byte[] bytes = selectedFiles.get(selectedFiles.size() - 1).getParent().getBytes();
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        String msg = "配置文件目录或其他错误";
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setContentText(msg);
                        alert.show();
                    }
                    for (File file : selectedFiles) {
                        if (isFileNameExists(file.getName())) {

                        } else {
                            tableView.getItems().add(new FileInfo(file.getName(), determineFileType(file), getFileSize(file), file.getAbsolutePath()));
                        }
                    }
                    fs.set(convertSize(updateFileSize(tableView)));
                    //tableView.refresh();
                }
            } catch (Exception e) {

            }


        });
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(fdevent -> {
            if (!tableView.getSelectionModel().isEmpty()) {
                fileList.remove(tableView.getSelectionModel().getSelectedItem());
                fs.set(convertSize(updateFileSize(tableView)));
                tableView.refresh();
            }
        });
        Button addFloderButton = new Button("Add Folder(s)");
        addFloderButton.setOnAction(focevent->{
            Stage stage = new Stage();
            DirectoryChooser directoryChooser  = new DirectoryChooser();
            directoryChooser.setTitle("Choose folder(s)");
            //
            File cacheFile = new File("cache_floder.txt");
            if (cacheFile.exists()) {
                try (InputStream inputStream = new FileInputStream(cacheFile)) {
                    byte[] bytes = new byte[(int) cacheFile.length()];

                    inputStream.read(bytes);
                    File directory = new File(new String(bytes));
                    if (directory.exists()) {
                        directoryChooser.setInitialDirectory(directory);
                    }
                } catch (IOException e) {
                    String msg = "配置文件目录或其他错误，无法打开最近目录位置";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                    File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                    directoryChooser.setInitialDirectory(iniDir);
                }
            }
            else{
                File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                directoryChooser.setInitialDirectory(iniDir);
            }

            File selectedDirectory = directoryChooser.showDialog(stage);

            if(selectedDirectory!=null){
                try (OutputStream outputStream = new FileOutputStream(cacheFile)) {
                    byte[] bytes = selectedDirectory.getParent().getBytes();
                    outputStream.write(bytes);
                } catch (IOException e) {
                    String msg = "配置文件目录或其他错误";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                }
            }
            if (selectedDirectory != null) {
                File file = selectedDirectory;
                if(isFileNameExists(file.getName())){

                }
                else{
                    tableView.getItems().add(new FileInfo(file.getName(),determineFileType(file),getFileSize(file),file.getAbsolutePath()));
                }
            }

            fs.set(convertSize(updateFileSize(tableView)));
            //tableView.refresh();
        });

        buttonBox.getChildren().addAll(addFileButton,addFloderButton,deleteButton);


        HBox centerLayout = new HBox(10);
        centerLayout.setPadding(new Insets(10));
        centerLayout.getChildren().addAll(tableView,buttonBox);
        HBox.setHgrow(tableView, Priority.ALWAYS);


        HBox upbottomLayout = new HBox(10);
        upbottomLayout.setPadding(new Insets(10));
        Label flab = new Label("File name and path:");
        TextField tf = new TextField();
        tf.setEditable(true);
        tf.setPrefColumnCount(33);
        // 设置默认路径为桌面路径
        tf.setText(System.getProperty("user.home") + File.separator + "Desktop"+File.separator+defalutName);
        Button fbtn = new Button("浏览...");
        fbtn.setOnAction(event1 -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择文件夹");
            // 设置初始目录为桌面
            File initialDirectory = new File(System.getProperty("user.home") + File.separator + "Desktop");
            directoryChooser.setInitialDirectory(initialDirectory);
            File selectedDirectory = directoryChooser.showDialog(stage2);
            if (selectedDirectory != null) {
                tf.setText(selectedDirectory.getAbsolutePath()+File.separator+defalutName);
            }
        });

        upbottomLayout.getChildren().addAll(flab,tf,fbtn);

        HBox bottomLayout = new HBox(10);

        bottomLayout.setPadding(new Insets(10));
        //Label label = new Label("Total size: "+ fs);
        Label label = new Label("Total size: ");
        label.textProperty().bind(Bindings.concat("Total size: ",fs));



        Button cancelbtn = new Button("Cancel");
        cancelbtn.setOnAction(event2 ->{
            tableView.getItems().clear();
            fs.set("0 B");
            stage2.close();
        });
        Button continuebtn = new Button("Continue");
        continuebtn.setOnAction(event3 -> {

            ZipTask task = new ZipTask(fileList, tf.getText(),updateFileSize(tableView));

            File f = new File(tf.getText());
            if(!f.getName().endsWith(".zip")){
                String msg = "请给出一个正确的文件名！";
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("提示");
                alert.setContentText(msg);
                alert.show();
            }
            else if(f.exists()){
               String msg = "存在同名文件，确定要覆盖吗？";
               Alert alert = new Alert(Alert.AlertType.WARNING);
               alert.setTitle("提示");
               alert.setContentText(msg);

               ButtonType buttonTypeYes = new ButtonType("确定");
               ButtonType buttonTypeNo = new ButtonType("取消");
               alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
               ButtonType result = alert.showAndWait().orElse(null);

               if (result == buttonTypeYes) {
                   executeZipTask(task,tableView,stage2,f);
               } else {

               }
           }
           else{
               executeZipTask(task,tableView,stage2,f);
           }

        });

        bottomLayout.getChildren().addAll(label,continuebtn,cancelbtn);
        bottomLayout.setMargin(continuebtn, new Insets(0,0,0,400));

        VBox vbn = new VBox();
        vbn.getChildren().addAll(upbottomLayout,bottomLayout);

        pane1.setCenter(centerLayout);
        pane1.setBottom(vbn);

        Scene scene2 = new Scene(pane1,750,450);
        tableView.getItems().clear();
        stage2.setScene(scene2);
        stage2.setResizable(true);
        stage2.setTitle("Zip(new)");

        // 设置弹出窗口的位置相对于主窗口的偏移量
        double offsetX = 70; // X轴偏移量
        double offsetY = 70; // Y轴偏移量
        stage2.setX(mainStage.getX() + offsetX);
        stage2.setY(mainStage.getY() + offsetY);

        stage2.show();
    }

    private void openUnZipPage(Stage mainStage){
        Stage stage2 = new Stage();
        stage2.initModality(Modality.WINDOW_MODAL); // 设置为模态窗口
        stage2.initOwner(mainStage);
        BorderPane pane1 = new BorderPane();
        TableView<FileInfo> tableView = new TableView<>();
        tableView.setEditable(true);
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Filename");
        filenameColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.3));
        filenameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        filenameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        filenameColumn.setOnEditCommit(event -> {
            FileInfo rowData = event.getRowValue();
            String newValue = event.getNewValue();
            if(isFileNameExists(newValue)){
                String msg = "文件名已存在，不允许修改！";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setContentText(msg);
                alert.show();
                rowData.filenameProperty().set(event.getOldValue());
                tableView.refresh();
            }else if (Pattern.compile(ILLEGAL_CHARACTERS_PATTERN).matcher(newValue).find()) {
                String msg = "文件名不能存在下列字符：/\\:*?\"<>|";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setContentText(msg);
                alert.show();
                rowData.filenameProperty().set(event.getOldValue());
                tableView.refresh();
            }
            else{
                rowData.filenameProperty().set(newValue);
            }

        });
        filenameColumn.setCellFactory(column -> new TextFieldTableCell<FileInfo, String>(new StringConverter<String>() {
            @Override
            public String toString(String value) {
                return value != null ? value : "";
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        }) {
            private final Tooltip tooltip = new Tooltip("选中后单击文件名单元格可修改文件名，按下Enter键保存修改");

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    tooltip.setShowDelay(Duration.millis(1000));
                    tooltip.setHideDelay(Duration.millis(3000));
                    setTooltip(tooltip); // 设置固定的提示
                }
            }


        });

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
        TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("File Size(Bytes)");
        sizeColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.2));
        TableColumn<FileInfo, String> locationColumn = new TableColumn<>("Location");
        locationColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.4));
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().fileSizeProperty());
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().locationProperty());

        tableView.getColumns().addAll(filenameColumn,typeColumn,sizeColumn,locationColumn);
        tableView.setItems(fileList);




        VBox buttonBox = new VBox(5); // 5是按钮之间的间距
        buttonBox.setPadding(new Insets(3)); // 内边距
        buttonBox.setMinWidth(90);

        Button addFileButton = new Button("Add File(s)");
        addFileButton.setOnAction(fcevent ->{
            Stage fileChooseStage = new Stage();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose File(s)");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Zip Files", "*.zip")
            );
            File cacheFile = new File("unZip_cache.txt");
            if (cacheFile.exists()) {
                try (InputStream inputStream = new FileInputStream(cacheFile)) {
                    byte[] bytes = new byte[(int) cacheFile.length()];
                    // Read the contents of the unZip_cache.txt
                    inputStream.read(bytes);
                    File directory = new File(new String(bytes));
                    if (directory.exists()) {
                        fileChooser.setInitialDirectory(directory);
                    }
                } catch (IOException e) {
                    String msg = "配置文件目录或其他错误";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();
                    File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                    fileChooser.setInitialDirectory(iniDir);
                }
            }
            else{
                File iniDir = new File(System.getProperty("user.home") + File.separator + "Desktop"+File.separator );
                fileChooser.setInitialDirectory(iniDir);
            }

            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(fileChooseStage);
            try {
                if (!selectedFiles.isEmpty()) {
                    // Store the directory
                    try (OutputStream outputStream = new FileOutputStream(cacheFile)) {
                        byte[] bytes = selectedFiles.get(selectedFiles.size() - 1).getParent().getBytes();
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        String msg = "配置文件目录或其他错误";
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setContentText(msg);
                        alert.show();
                    }
                    for (File file : selectedFiles) {
                        if (isFileNameExists(file.getName())) {

                        } else {
                            tableView.getItems().add(new FileInfo(file.getName(), determineFileType(file), getFileSize(file), file.getAbsolutePath()));
                        }
                    }
                    fs.set(convertSize(updateFileSize(tableView)));
                    //tableView.refresh();
                }
            } catch (Exception e) {

            }

        });
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(fdevent -> {
            if (!tableView.getSelectionModel().isEmpty()) {
                fileList.remove(tableView.getSelectionModel().getSelectedItem());
                fs.set(convertSize(updateFileSize(tableView)));
                tableView.refresh();
            }
        });
        Label tiplabel = new Label("当前模式：");
        tiplabel.setFont(font1);
        Label tipButton = new Label();
        tipButton.setFont(font1);
        if(unZipMode == 1){
            tipButton.setText("标准模式");
        }else{
            tipButton.setText("递归模式");
        }
        buttonBox.getChildren().addAll(addFileButton,deleteButton,tiplabel,tipButton);


        HBox centerLayout = new HBox(10);
        centerLayout.setPadding(new Insets(10));
        centerLayout.getChildren().addAll(tableView,buttonBox);
        HBox.setHgrow(tableView, Priority.ALWAYS);


        HBox upbottomLayout = new HBox(10);
        upbottomLayout.setPadding(new Insets(10));
        Label flab = new Label("File path:");
        TextField tf = new TextField();
        tf.setEditable(true);
        tf.setPrefColumnCount(33);
        // 设置默认路径
        if(!defalutFilePath.endsWith(File.separator)){
            defalutFilePath = defalutFilePath+File.separator;
        }
        tf.setText(defalutFilePath);
        Button fbtn = new Button("浏览...");
        fbtn.setOnAction(event1 -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择文件夹");
            // 设置初始目录
            File initialDirectory = new File(defalutFilePath);
            directoryChooser.setInitialDirectory(initialDirectory);
            File selectedDirectory = directoryChooser.showDialog(stage2);
            if (selectedDirectory != null) {
                tf.setText(selectedDirectory.getAbsolutePath()+File.separator);
            }
        });

        upbottomLayout.getChildren().addAll(flab,tf,fbtn);

        HBox bottomLayout = new HBox(10);

        bottomLayout.setPadding(new Insets(10));
        Label label = new Label("Total size: ");
        label.textProperty().bind(Bindings.concat("Total size: ",fs));


        Button cancelbtn = new Button("Cancel");
        cancelbtn.setOnAction(event2 ->{
            tableView.getItems().clear();
            fs.set("0 B");
            stage2.close();
        });
        Button continuebtn = new Button("Continue");
        continuebtn.setOnAction(event3 -> {

            UnZipTask task = new UnZipTask(fileList, tf.getText(),updateFileSize(tableView),unZipMode);

            boolean isFileExisted =false;
            boolean isFolderPathRight=true;
            for(FileInfo f :fileList ){
                File fol = new File(tf.getText());
                if(!fol.exists()){
                    isFolderPathRight = false;
                    String msg = "文件夹路径不存在";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setContentText(msg);
                    alert.show();

                }
                else{
                    File fi = new File(tf.getText()+f.getName().substring(0,f.getName().lastIndexOf(".zip")==-1?f.getName().length():f.getName().lastIndexOf(".zip")).toLowerCase()+File.separator);
                    if(fi.exists()){
                        isFileExisted = true;
                    }
                }

            }
            if(isFileExisted && isFolderPathRight){
                String msg = "存在同名文件夹，确定要继续吗？";
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("提示");
                alert.setContentText(msg);

                ButtonType buttonTypeYes = new ButtonType("确定");
                ButtonType buttonTypeNo = new ButtonType("取消");
                alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
                ButtonType result = alert.showAndWait().orElse(null);

                if (result == buttonTypeYes) {
                    executeUnZipTask(task,tableView,stage2);
                } else {

                }
            }
            else if(isFolderPathRight && !isFileExisted){
                executeUnZipTask(task,tableView,stage2);
            }
            else{

            }

        });

        bottomLayout.getChildren().addAll(label,continuebtn,cancelbtn);
        bottomLayout.setMargin(continuebtn, new Insets(0,0,0,400));

        VBox vbn = new VBox();
        vbn.getChildren().addAll(upbottomLayout,bottomLayout);

        pane1.setCenter(centerLayout);
        pane1.setBottom(vbn);

        Scene scene2 = new Scene(pane1,750,450);
        tableView.getItems().clear();
        stage2.setScene(scene2);
        stage2.setResizable(true);
        stage2.setTitle("UnZip");

        // 设置弹出窗口的位置相对于主窗口的偏移量
        double offsetX = 70; // X轴偏移量
        double offsetY = 70; // Y轴偏移量
        stage2.setX(mainStage.getX() + offsetX);
        stage2.setY(mainStage.getY() + offsetY);

        stage2.show();
    }



    @Override
    public void start(Stage primaryStage) {

        Settings set = new Settings();
        File settingsFile = new File("Settings.txt");
        if(settingsFile.exists()){
            loadSettings("Settings.txt",set);
            defalutFilePath = set.getDefaultFilePath();
            defalutName = set.getDefaultName();
            unZipMode = set.getUnZipMode();
        }
        else{
            defalutFilePath = System.getProperty("user.home") + File.separator + "Desktop"+File.separator;
            defalutName = "new.zip";
            unZipMode = 1;
            saveSettings("Settings.txt",defalutName,defalutFilePath,unZipMode);
        }


        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuSettings = new Menu();
        Menu menuHelp = new Menu("Help");
        Menu menuExit = new Menu();
        //Menu-part1
        MenuItem FileUnzip = new MenuItem("UnZip");
        MenuItem Filenew = new MenuItem("Zip(New)");
        menuFile.getItems().addAll(Filenew,FileUnzip);
        Filenew.setAccelerator(KeyCombination.valueOf("ctrl+S"));
        FileUnzip.setAccelerator(KeyCombination.valueOf("ctrl+U"));
        Filenew.setOnAction(event->{
            openZipPage(primaryStage);
        });
        FileUnzip.setOnAction(event->{
            openUnZipPage(primaryStage);
        });
        //Menu-part2
        Label labelSet = new Label("Settings");
        labelSet.setOnMouseClicked(event->{
            openSettings(primaryStage);
        });
        //将标签设置到menu的graphic属性当中
        menuSettings.setGraphic(labelSet);
        //Menu-part3
        MenuItem HelpHelp = new MenuItem("Help");
        MenuItem HelpAboutUs = new MenuItem("About Us");
        menuHelp.getItems().addAll(HelpHelp,HelpAboutUs);

        HelpHelp.setAccelerator(KeyCombination.valueOf("ctrl+h"));
        HelpAboutUs.setAccelerator(KeyCombination.valueOf("ctrl+i"));
        HelpHelp.setOnAction(event->{
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            VBox vb = new VBox();
            Label label1 = new Label("1.在Settings中可以设置默认的解压方式及（解）压缩路径;");
            Label label2 = new Label("2.不能压缩或解压空文件夹;");
            Label label3 = new Label("3.选中后单击文件名单元格可修改文件名，按下Enter键保存修改;");
            Label label4 = new Label("4.不能存在同名文件，文件名不能存在下列字符：/\\:*?\"<>| ;");
            Label label5 = new Label("5.当取消解压任务时，会出现卡顿，该现象为正常现象，\n表明程序正在删除未压缩完成的文件，删除文件后程序可正常使用.");
            label1.setFont(font2);
            label2.setFont(font2);
            label3.setFont(font2);
            label4.setFont(font2);
            label5.setFont(font2);
            vb.getChildren().addAll(label1,label2,label3,label4,label5);
            vb.setPadding(new Insets(10));
            vb.setSpacing(10);

            Scene scene = new Scene(vb);
            stage.setScene(scene);
            stage.setTitle("帮助");
            stage.show();
        });
        HelpAboutUs.setOnAction(event->{
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(primaryStage);
            VBox vb = new VBox();
            Label label1 = new Label("程序开发者：庄严");
            Label label2 = new Label("完成时间：2024年10月06日");
            Label label3 = new Label("联系方式（问题反馈）：");
            Label label4 = new Label("Tel: 13153488621");
            Label label5 = new Label("Email: 2976377647@qq.com");
            Label[] labels = {label1,label2,label3,label4,label5};
            for(Label label : labels){
                label.setFont(font1);
            }
            vb.getChildren().addAll(label1,label2,label3,label4,label5);
            vb.setPadding(new Insets(10));
            vb.setSpacing(10);

            Scene scene = new Scene(vb);
            stage.setScene(scene);
            stage.setTitle("关于");
            stage.show();
        });
        //Menu-part4
        Label labelExit = new Label("Exit");
        labelExit.setOnMouseClicked(event -> {
            String msg = "您确定要退出程序吗？";
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setContentText(msg);

            ButtonType buttonTypeYes = new ButtonType("确定");
            ButtonType buttonTypeNo = new ButtonType("取消");
            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
            ButtonType result = alert.showAndWait().orElse(null);

            if (result == buttonTypeYes) {
                primaryStage.close();
                Platform.exit();
            } else {
                // 如果点击“取消”或关闭对话框，则不进行任何操作
            }

        });
        menuExit.setGraphic(labelExit);

        menuBar.getMenus().addAll(menuFile,menuSettings,menuHelp,menuExit);

        StackPane root = new StackPane();
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(images);

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(2), event -> {
                    currentIndex = (currentIndex + 1) % images.length;
                    for (int i = 0; i < images.length; i++) {
                        images[i].setVisible(i == currentIndex);
                    }
                })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        VBox vb = new VBox();
        Scene scene = new Scene(vb,500,200);
        Label welcome = new Label("  欢迎使用：");
        Label tips1 = new Label("  快捷键>>>新建压缩文件：ctrl+s ");
        Label tips2 = new Label("  快捷键>>>解压文件：ctrl+u ");
        welcome.setFont(font1);
        tips1.setFont(font1);
        tips2.setFont(font1);
        vb.getChildren().addAll(menuBar,welcome,root,tips1,tips2);
        vb.setSpacing(10);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Welcome to use MyZip!");
        primaryStage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}

