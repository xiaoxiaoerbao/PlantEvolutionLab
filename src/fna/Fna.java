package fna;

import utils.CommandUtils;
import utils.IOTool;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Fna {
    String refDir = "/Users/xudaxing/Data/wheatReference/ref";
    String referenceDir = "/Volumes/T7/006_analysis/001_wheatReference";
    String reference1A1B1DDir = "/Volumes/T7/006_analysis/002_wheatReference1A1B1D";
    String logDir = "/Volumes/T7/006_analysis/log";
    int threadsNum = 10;

    public Fna(){
        this.filterRename(refDir, referenceDir);
        this.retain(referenceDir, reference1A1B1DDir, "1A");
        this.retain(referenceDir, reference1A1B1DDir, "1B");
        this.retain(referenceDir, reference1A1B1DDir, "1D");
        this.gzip(reference1A1B1DDir);
        this.gzip(referenceDir);
    }

    public void filterRename(File file, File outFile){
        String fileName = file.getName();
        String[] temp = StringUtils.split(fileName, "_");
        String prefix = temp[0]+"_"+temp[1];
        try (BufferedReader br = IOTool.getBufferedReader(file);
             BufferedWriter bw = IOTool.getBufferedWriter(outFile)) {
            String line;
            StringBuilder sb = new StringBuilder();
            boolean ifSelected = false;
            while ((line=br.readLine()) != null) {
                if (line.startsWith(">")){
                    String regex = "\\b\\d+[ABD]\\b";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()){
                        ifSelected = true;
                        sb.setLength(0);
                        sb.append(">").append(prefix).append("_").append(matcher.group());
                        bw.write(sb.toString());
                        bw.newLine();
                        continue;
                    }else {
                        ifSelected = false;
                        continue;
                    }
                }
                if (ifSelected){
                    bw.write(line);
                    bw.newLine();
                }
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void filterRename(String refDir, String referenceDir){
        List<File> files = IOTool.getFileListInDirEndWith(refDir, "fna.gz");
        String[] outFileNames = files.stream().map(File::getName).map(s -> s.replaceAll("\\.fna\\.gz",".fna")).toArray(String[]::new);
        for (int i = 0; i < files.size(); i++) {
            filterRename(files.get(i), new File(referenceDir+"/"+outFileNames[i]));
        }
    }

    public void retain(File file, File outFile, String chromosomeName){
        try (BufferedReader br = IOTool.getBufferedReader(file);
             BufferedWriter bw = IOTool.getBufferedWriter(outFile)) {
            String line;
            boolean ifSelected = false;
            while ((line=br.readLine())!=null){
                if (line.startsWith(">")){
                    if (line.endsWith(chromosomeName)){
                        ifSelected = true;
                        bw.write(line);
                        bw.newLine();
                        continue;
                    }else {
                        ifSelected = false;
                    }
                }
                if (ifSelected){
                    bw.write(line);
                    bw.newLine();
                }
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void retain(String refDir, String ref1ADir, String chromosomeName){
        List<File> files = IOTool.getFileListInDirEndWith(refDir, "fna");
        String[] outFileNames = files.stream().map(File::getName).map(s -> s.replaceAll("genomic", chromosomeName)).toArray(String[]::new);
        for (int i = 0; i < files.size(); i++) {
            retain(files.get(i), new File(ref1ADir, outFileNames[i]), chromosomeName);
        }
    }

    public void gzip(String inputDir){
        List<File> files = IOTool.getVisibleFileRecursiveDir(inputDir);
        List<String> commandList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.setLength(0);
            sb.append("gzip ").append(file.getAbsolutePath()).append("\n");
            commandList.add(sb.toString());
        }
        CommandUtils.runSH_multipleCommands("gz", commandList, inputDir, logDir, threadsNum);
    }

    public static void main(String[] args) {
        Fna fna  = new Fna();
    }






}
