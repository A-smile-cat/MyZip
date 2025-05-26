package com.example.task1MyZip;


import javafx.concurrent.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ZipFileEmptyException extends Exception{
    private int errorCode;
    private String message;
    ZipFileEmptyException(){
        this.message = "无法完成解压，压缩文件夹是空的！";
        this.errorCode = 4;
    }
    ZipFileEmptyException(int errorCode, String message) {
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


public class UnZipTask extends Task<Void> {
    private List<FileInfo> fileList;
    private String outputFilePath;
    Long totalSize;
    long count ;
    int mode;
    private static final String ILLEGAL_CHARACTERS_PATTERN = "[/\\:*?\"<>|]";

    public UnZipTask(List<FileInfo> fileList, String outputFilePath,long totalSize,int mode) {
        this.fileList = fileList;
        this.outputFilePath = outputFilePath;
        this.totalSize = totalSize;
        this.mode = mode;
    }

    @Override
    protected Void call() throws Exception {

        try {
            if(fileList.isEmpty()){

                FileListEmptyException fle = new FileListEmptyException(1,"文件列表无内容，请至少选择一个需要解压的文件！");
                throw fle;
            }
            for (FileInfo fileinfo : fileList) {

                File f = new File(fileinfo.getPath());
                String s = fileinfo.getName();
                String fpath ;
                if(s.lastIndexOf(".zip")==-1){
                    fpath = outputFilePath+s+File.separator;
                }
                else{
                    fpath = outputFilePath+s.substring(0,s.lastIndexOf("."))+File.separator;
                }

                fileUnZip(f, fpath, mode);

            }
        } catch (ZipFileEmptyException e) {
            throw e;
        } catch (FileListEmptyException e) {
            throw e;
        } catch (IOException e) {
            File folder = new File(outputFilePath);

            if(Pattern.compile(ILLEGAL_CHARACTERS_PATTERN).matcher(folder.getName()).find()){
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


    void fileUnZip(File file, String newPath,int mode) throws Exception {

        if(mode == 1){
            int flag = 0;
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file), Charset.forName("gbk"));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                flag = 1;
                if (entry.isDirectory()) {
                    File dir = new File(newPath + entry.getName());

                    dir.mkdirs();

                } else {
                    File pa = new File(newPath + entry.getName());

                    pa.getParentFile().mkdirs();

                    FileOutputStream fos = new FileOutputStream(newPath + entry.getName());
                    int c;
                    byte[] b = new byte[4096];
                    while ((c = zis.read(b)) != -1) {
                        fos.write(b,0,c);
                        count += c;
                        updateProgress(count,totalSize);
                        double percent =(double) count / totalSize * 100;
                        updateMessage(Math.round(percent)+"%");
                    }
                    fos.flush();
                    fos.close();
                    zis.closeEntry();
                }
            }
            if(flag == 0){
                ZipFileEmptyException zfe = new ZipFileEmptyException();
                throw zfe;
            }
            zis.close();
        }
        else if(mode == 2){
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file), Charset.forName("gbk"));
            int flag = 0;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                flag = 1;
                if (entry.isDirectory()) {
                    File dir = new File(newPath + entry.getName());
                    dir.mkdirs();
                } else {
                    File pa = new File(newPath + entry.getName());
                    pa.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(newPath + entry.getName());
                    int c;
                    byte[] b = new byte[4096];
                    while ((c = zis.read(b)) != -1) {
                        fos.write(b,0,c);
                        count += c;
                        updateProgress(count,totalSize);
                        double percent =(double) count / totalSize * 100;
                        updateMessage(Math.round(percent)+"%");

                    }
                    fos.flush();
                    fos.close();
                    zis.closeEntry();
                    if(entry.getName().endsWith(".zip")){

                        File fol = new File(pa.getParentFile().getPath()+File.separator+pa.getName().replace(".zip","(zip)")+File.separator);
                        if (fol.exists()) {
                            int count = 1;
                            File ftemp;
                            do {
                                ftemp = new File(fol.getParentFile(), fol.getName() + "(" + count + ")" + File.separator);
                                count++;
                            } while (ftemp.exists());
                            fol = ftemp;
                        }
                        fol.mkdirs();

                        File zfile = new File(pa.getPath());
                        totalSize += zfile.length();

                        fileUnZip(pa,fol.getPath()+File.separator,2);
                        pa.delete();
                    }
                }
            }
            if(flag == 0){
                ZipFileEmptyException zfe = new ZipFileEmptyException();
                throw zfe;
            }
            zis.close();
        }

    }
}
