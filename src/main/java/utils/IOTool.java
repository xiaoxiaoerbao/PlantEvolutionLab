package utils;

import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class for file input/output operations, including handling compressed files (GZIP),
 * reading and writing lines, and filtering files based on certain conditions.
 */
public class IOTool {

    /**
     * Returns a BufferedReader for reading a file.
     *
     * @param fileName The name of the file.
     * @return A BufferedReader to read the file.
     */
    public static BufferedReader getBufferedReaderFromFile(String fileName) {
        try {
            return new BufferedReader(new FileReader(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a BufferedReader for reading a GZipped file.
     *
     * @param file The path to the gzipped file.
     * @return A BufferedReader to read the gzipped file.
     */
    public static BufferedReader getBufferedReaderFromGZFile(String file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
            return new BufferedReader(new InputStreamReader(gzipInputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a BufferedReader for reading a file with Chinese encoding (GB18030).
     *
     * @param file The path to the file.
     * @return A BufferedReader to read the file with GB18030 encoding.
     */
    public static BufferedReader getBufferedReaderFromChineseFile(String file) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "GB18030");
            return new BufferedReader(inputStreamReader);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a BufferedReader based on file extension, handling gzipped and normal files.
     *
     * @param file The path to the file.
     * @return A BufferedReader to read the file, handling gzipped files.
     */
    public static BufferedReader getBufferedReader(String file) {
        if (file.endsWith(".gz")) {
            return getBufferedReaderFromGZFile(file);
        }else {
            return getBufferedReaderFromFile(file);
        }
    }

    /**
     * Returns a BufferedReader for reading a file based on the File object (handling gzipped files).
     *
     * @param file The File object representing the file.
     * @return A BufferedReader to read the file.
     */
    public static BufferedReader getBufferedReader(File file) {
        if (file.getAbsolutePath().endsWith(".gz")) {
            return getBufferedReaderFromGZFile(file.getAbsolutePath());
        }else {
            return getBufferedReaderFromFile(file.getAbsolutePath());
        }
    }

    /**
     * Returns a BufferedWriter for writing to a regular file.
     *
     * @param file The file to write to.
     * @return A BufferedWriter to write to the file.
     */
    public static BufferedWriter getBufferedWriterForFile(String file) {
        try {
            return new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a BufferedWriter for writing to a gzipped file.
     *
     * @param file The gzipped file to write to.
     * @return A BufferedWriter to write to the gzipped file.
     */
    public static BufferedWriter getBufferedWriterForGZFile(String file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            return new BufferedWriter(new OutputStreamWriter(gzipOutputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a BufferedWriter based on file extension, handling gzipped and normal files.
     *
     * @param file The file path.
     * @return A BufferedWriter to write to the file.
     */
    public static BufferedWriter getBufferedWriter(String file) {
        if (file.endsWith(".gz")) {
            return getBufferedWriterForGZFile(file);
        }else {
            return getBufferedWriterForFile(file);
        }
    }

    /**
     * Returns a BufferedWriter for a file based on the File object (handling gzipped files).
     *
     * @param file The File object representing the file.
     * @return A BufferedWriter to write to the file.
     */
    public static BufferedWriter getBufferedWriter(File file) {
        if (file.getAbsolutePath().endsWith(".gz")) {
            return getBufferedWriterForGZFile(file.getAbsolutePath());
        }else {
            return getBufferedWriterForFile(file.getAbsolutePath());
        }
    }

    /**
     * Filters and returns files that start with a given prefix.
     *
     * @param fAll An array of files to filter.
     * @param startStr The prefix to filter by.
     * @return An array of files that start with the given prefix.
     */
    private static File[] listFilesStartsWith (File[] fAll, String startStr) {
        List<File> al = new ArrayList<>();
        for (File file : fAll) {
            if (file.isHidden()) continue;
            if (file.getName().startsWith(startStr)) al.add(file);
        }
        return al.toArray(new File[0]);
    }

    /**
     * Filters and returns files that end with a given suffix.
     *
     * @param fAll An array of files to filter.
     * @param endStr The suffix to filter by.
     * @return An array of files that end with the given suffix.
     */
    private static File[] listFilesEndWith (File[] fAll, String endStr) {
        List<File> al = new ArrayList<>();
        for (File file : fAll) {
            if (file.isHidden()) continue;
            if (file.getName().endsWith(endStr)) al.add(file);
        }
        return al.toArray(new File[0]);
    }

    /**
     * Returns a sorted list of files in the given directory that start with a given prefix.
     *
     * @param inDirS The directory to search.
     * @param startStr The prefix to filter by.
     * @return A sorted list of files in the directory that start with the given prefix.
     */
    public static List<File> getFileListInDirStartsWith (String inDirS, String startStr) {
        File[] fs = new File(inDirS).listFiles();
        assert fs != null;
        fs = listFilesStartsWith(fs, startStr);
        List<File> fList = new ArrayList<>(Arrays.asList(fs));
        Collections.sort(fList);
        return fList;
    }

    /**
     * Returns a sorted list of files in the given directory that end with a given suffix.
     *
     * @param inDirS The directory to search.
     * @param endStr The suffix to filter by.
     * @return A sorted list of files in the directory that end with the given suffix.
     */
    public static List<File> getFileListInDirEndWith (String inDirS, String endStr) {
        File[] fs = new File(inDirS).listFiles();
        assert fs != null;
        fs = listFilesEndWith(fs, endStr);
        List<File> fList = new ArrayList<>(Arrays.asList(fs));
        Collections.sort(fList);
        return fList;
    }

    /**
     * Recursively retrieves all visible (non-hidden) files and directories in the given directory.
     *
     * @param dir The directory to search.
     * @return A sorted list of visible files and directories.
     */
    public static List<File> getVisibleFileRecursiveDir(String dir){
        File inputDirFile = new File(dir);
        TreeSet<File> fileTree = new TreeSet<>();
        for (File entry : Objects.requireNonNull(inputDirFile.listFiles())) {
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
     * Retrieves all visible (non-hidden) files (excluding directories) in the given directory.
     *
     * @param dir The directory to search.
     * @return A sorted list of visible files in the directory.
     */
    public static List<File> getVisibleDir(String dir){
        File[] files=new File(dir).listFiles();
        Predicate<File> hidden=File::isHidden;
        Predicate<File> file=File::isFile;
        assert files != null;
        return Arrays.stream(files).filter(hidden.negate().and(file)).sorted().collect(Collectors.toList());
    }

    /**
     * Views the header of a file, printing its columns.
     *
     * @param file The file to view.
     * @param delimiter The delimiter used to separate columns.
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

    /**
     * Reads the header from a file and prints its columns.
     * The default delimiter is tab ("\t").
     *
     * @param file The file to read the header from.
     */
    public static void viewHeader(String file){
        viewHeader(file,"\t");
    }

    /**
     * Reads all lines from a file into a list.
     *
     * @param file The file to read.
     * @return A list of strings representing all the lines in the file.
     */
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
        return lines;
    }

    /**
     * Writes all lines to a file. Each line is written with a new line separator.
     *
     * @param outFile The file to write to.
     * @param lines A list of strings to be written to the file.
     */
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

    /**
     * Generates a list of log files with the specified title and log file count. The log files are named using the title and a digit suffix.
     *
     * @param title The title to be used in the log file names.
     * @param logDir The directory where the log files are stored.
     * @param logFileNum The number of log files to generate.
     * @return A list of File objects representing the log files.
     */
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
