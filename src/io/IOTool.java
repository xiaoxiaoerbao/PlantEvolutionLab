package io;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IOTool {

    public static BufferedReader getBufferedReaderFromFile(String fileName) {
        try {
            return new BufferedReader(new FileReader(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedReader getBufferedReaderFromGZFile(String file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
            return new BufferedReader(new InputStreamReader(gzipInputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedReader getBufferedReader(String file) {
        if (file.endsWith(".gz")) {
            return getBufferedReaderFromGZFile(file);
        }else {
            return getBufferedReaderFromFile(file);
        }
    }

    public static BufferedReader getBufferedReader(File file) {
        if (file.getAbsolutePath().endsWith(".gz")) {
            return getBufferedReaderFromGZFile(file.getAbsolutePath());
        }else {
            return getBufferedReaderFromFile(file.getAbsolutePath());
        }
    }

    public static BufferedWriter getBufferedWriterForFile(String file) {
        try {
            return new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedWriter getBufferedWriterForGZFile(String file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            return new BufferedWriter(new OutputStreamWriter(gzipOutputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedWriter getBufferedWriter(String file) {
        if (file.endsWith(".gz")) {
            return getBufferedWriterForGZFile(file);
        }else {
            return getBufferedWriterForFile(file);
        }
    }

    public static BufferedWriter getBufferedWriter(File file) {
        if (file.getAbsolutePath().endsWith(".gz")) {
            return getBufferedWriterForGZFile(file.getAbsolutePath());
        }else {
            return getBufferedWriterForFile(file.getAbsolutePath());
        }
    }
}
