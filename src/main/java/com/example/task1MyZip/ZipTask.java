package com.example.task1MyZip;

import javafx.concurrent.Task;

import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.zip.*;

class FileListEmptyException extends Exception{
    private int errorCode;
    private String message;
    FileListEmptyException(){
        this.message = "文件列表无内容，您不能压缩空文件夹！";
        this.errorCode = 1;
    }
    FileListEmptyException(int errorCode, String message) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }
    int getErrorCode() {
        return errorCode;
    }
    String getmessage() {
        return message;
    }
}
class FolderPathErrorException extends Exception{
    private int errorCode;
    private String message;
    FolderPathErrorException() {
        this.message = "文件路径不存在！";
        this.errorCode = 2;
    }
    FolderPathErrorException(int errorCode, String message) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }
    int getErrorCode() {
        return errorCode;
    }
    String getmessage() {
        return message;
    }
}
class FileNameErrorException extends Exception{
    private int errorCode;
    private String message;
    FileNameErrorException() {
        this.message = "文件名不能存在下列字符：/\\:*?\"<>|";
        this.errorCode = 3;
    }
    FileNameErrorException(int errorCode, String message) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }
    int getErrorCode() {
        return errorCode;
    }
    String getmessage() {
        return message;
    }
}

public class ZipTask extends Task<Void> {
    private List<FileInfo> fileList;
    private String outputFilePath;
    private Long totalSize;
    long count ;
    private static final String ILLEGAL_CHARACTERS_PATTERN = "[/\\:*?\"<>|]";

    public ZipTask(List<FileInfo> fileList, String outputFilePath,long totalSize) {
        this.fileList = fileList;
        this.outputFilePath = outputFilePath;
        this.totalSize = totalSize;
    }

    @Override
    protected Void call() throws Exception {

        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFilePath));

            if(fileList.isEmpty()){
                FileListEmptyException fle = new FileListEmptyException();
                throw fle;
            }

            for (FileInfo fileinfo : fileList) {
                File f = new File(fileinfo.getPath());
                fileZip(f, fileinfo.getName(), zos,"");
            }

            zos.flush();
            zos.close();
        }catch (FileListEmptyException fle){
            throw fle;
        } catch (IOException e){
            File file = new File(outputFilePath);
            File folder = new File(file.getAbsolutePath());

            if(Pattern.compile(ILLEGAL_CHARACTERS_PATTERN).matcher(file.getName()).find()){
                FileNameErrorException fne = new FileNameErrorException();
                throw fne;
            }
            else if(!folder.exists()){
                FolderPathErrorException fpe = new FolderPathErrorException();
                throw fpe;
            }
            else{
                throw e;
            }
        }

        return null;
    }


    private void fileZip(File file,String filename,ZipOutputStream zos,String basePath) throws Exception {

        if(file.isDirectory()) {
            zos.putNextEntry(new ZipEntry(basePath+filename+File.separator));

            zos.closeEntry();

            File fol[] = file.listFiles();

            for(int i = 0; i < fol.length; i++) {
                fileZip(fol[i],fol[i].getName(),zos,basePath+filename+File.separator);
            }
        }
        else {

            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(basePath + filename));

            byte b[] = new byte[4096];
            int n;
            while ((n = fis.read(b)) != -1) {
                zos.write(b, 0, n);
                count += n;
                updateProgress(count,totalSize);
                double percent =(double) count / totalSize * 100;

                updateMessage(Math.round(percent)+"%");
            }
            zos.closeEntry();

            fis.close();
        }

    }

}





