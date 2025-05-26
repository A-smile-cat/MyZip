package com.example.task1MyZip;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

    public class FileInfo {
        private StringProperty filename = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty fileSize = new SimpleStringProperty();
        private final StringProperty location = new SimpleStringProperty();

        public FileInfo(String filename, String type, String fileSize, String location) {
            this.filename.set(filename);
            this.type.set(type);
            this.fileSize.set(fileSize);
            this.location.set(location);
        }

        public StringProperty filenameProperty() {
            return filename;
        }

        public StringProperty typeProperty() {
            return type;
        }

        public StringProperty fileSizeProperty() {
            return fileSize;
        }

        public StringProperty locationProperty() {
            return location;
        }

        public void setName(String newValue) {
            this.filename.set(newValue);
        }
        public String getName() {
           return filename.get();
        }
        public String getPath() {
            return location.get();
        }

        public long getSize() {
            String s = fileSize.get();
            return Long.parseLong(s);
        }
    }

