package com.example.task1MyZip;

public class Settings {
    private String defaultName;
    private String defaultFilePath;
    private int unZipMode;

    // 构造函数
    public Settings() {
    }

    // getter 和 setter 方法
    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public String getDefaultFilePath() {
        return defaultFilePath;
    }

    public void setDefaultFilePath(String defaultFilePath) {
        this.defaultFilePath = defaultFilePath;
    }

    public int getUnZipMode() {
        return unZipMode;
    }

    public void setUnZipMode(int unZipMode) {
        this.unZipMode = unZipMode;
    }
}
