package utils;

import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    public static File[] listFilesStartsWith (File[] fAll, String startStr) {
        List<File> al = new ArrayList<>();
        for (File file : fAll) {
            if (file.getName().startsWith(startStr)) al.add(file);
        }
        return al.toArray(new File[0]);
    }

    public static File[] listFilesEndWith (File[] fAll, String endStr) {
        List<File> al = new ArrayList<>();
        for (File file : fAll) {
            if (file.getName().endsWith(endStr)) al.add(file);
        }
        return al.toArray(new File[0]);
    }

    public static List<File> getFileListInDirStartsWith (String inDirS, String startStr) {
        File[] fs = new File(inDirS).listFiles();
        assert fs != null;
        fs = listFilesStartsWith(fs, startStr);
        List<File> fList = new ArrayList<>(Arrays.asList(fs));
        Collections.sort(fList);
        return fList;
    }

    public static List<File> getFileListInDirEndWith (String inDirS, String endStr) {
        File[] fs = new File(inDirS).listFiles();
        assert fs != null;
        fs = listFilesEndWith(fs, endStr);
        List<File> fList = new ArrayList<>(Arrays.asList(fs));
        Collections.sort(fList);
        return fList;
    }

    /**
     * 递归获取当前目录下的所有非隐藏文件和非隐藏目录
     * @param dir dir
     * @return 当前目录下的所有非隐藏文件和非隐藏目录
     */
    public static List<File> getVisibleFileRecursiveDir(String dir){
        File inputDirFile = new File(dir);
        TreeSet<File> fileTree = new TreeSet<>();
        for (File entry : inputDirFile.listFiles()) {
            if (entry.isHidden()) continue;
            if (entry.isFile()){
                fileTree.add(entry);
            }
            else {
                fileTree.addAll(getVisibleFileRecursiveDir(entry.getAbsolutePath()));
            }
        }
        return fileTree.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 获取当前目录下的所有非隐藏文件(不包含目录)，不递归
     * @param dir dir
     * @return
     */
    public static List<File> getVisibleDir(String dir){
        File[] files=new File(dir).listFiles();
        Predicate<File> hidden=File::isHidden;
        Predicate<File> file=File::isFile;
        return Arrays.stream(files).filter(hidden.negate().and(file)).sorted().collect(Collectors.toList());
    }

    /**
     * 查看文件header
     * @param file
     * @param delimiter
     */
    public static void viewHeader(String file, String delimiter){
        try (BufferedReader br = IOTool.getBufferedReader(file)) {
            String line=br.readLine();
            String[] header= StringUtils.split(line, delimiter);
            for (int i = 0; i < header.length; i++) {
                System.out.println(i+" "+header[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void viewHeader(String file){
        viewHeader(file,"\t");
    }

    public static List<String> readAllLines(String file){
        List<String> lines=new ArrayList<>();
        String line;
        try (BufferedReader br = IOTool.getBufferedReader(file)) {
            while ((line=br.readLine())!=null){
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert lines!=null : " check "+file;
        return lines;
    }

    public static void writeAllLines(File outFile, List<String> lines){
        try (BufferedWriter bw = IOTool.getBufferedWriter(outFile)) {
            for (String line:lines){
                bw.write(line);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<File> getLogFile(String title, String logDir, int logFileNum){
        assert logFileNum > 0 : logFileNum + " must be greater than 0";
        int digitsNum=(int)(Math.log10(logFileNum) +1);
        List<File> logFiles= new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        File file;
        for (int i = 0; i < logFileNum; i++) {
            sb.setLength(0);
            sb.append(title).append("_").append(PStringUtils.getNDigitNumber(digitsNum, i)).append(".log");
            file = new File(logDir, sb.toString());
            logFiles.add(file);
        }
        return logFiles;
    }
}
