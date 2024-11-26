package utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class JWC {
    public static void temp1(String inputFile, Map<String, String> map, String outputFile) {
        int count = 0;
        try (BufferedReader br = IOTool.getBufferedReaderFromChineseFile(inputFile);
             BufferedWriter bw = IOTool.getBufferedWriter(outputFile)){
            String header = br.readLine();
            count++;
            bw.write(header);
            bw.newLine();
            String line, lin;
            String[] temp, tem;
            temp = StringUtils.split(header, "\t");
            int indexKey = ArrayUtils.indexOf(temp, "上课班级构成");
            int indexValue = ArrayUtils.indexOf(temp, "主要上课校内专业（大类）");
            StringBuilder sb = new StringBuilder();
            LinkedHashSet<String> majorSet = new LinkedHashSet<>();
            while ((line = br.readLine()) != null) {
                count++;
                temp = StringUtils.split(line, "\t");
                sb.setLength(0);
                for (int i = 0; i < indexValue; i++) {
//                    System.out.println(count);
                    sb.append(temp[i]).append("\t");
                }
                majorSet.clear();
                if (temp[indexKey].startsWith("\"")){
                    lin = StringUtils.substringBetween(temp[indexKey], "\"");
                    tem = StringUtils.split(lin, ",");
                    sb.append("\"");
                    for (String s : tem) {
                        majorSet.add(map.get(s));
                    }
                    for (String major : majorSet) {
                        sb.append(major).append(",");
                    }
                    sb.deleteCharAt(sb.length() - 1).append("\"");
                    sb.append("\t").append(temp[temp.length - 1]);
                }else {
                    lin = temp[indexKey];
                    sb.append(map.get(lin)).append("\t").append(temp[temp.length - 1]);
                }
                bw.write(sb.toString());
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            System.out.println(count);
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> map(String inputFile){
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = IOTool.getBufferedReaderFromChineseFile(inputFile)) {
            String line;
            String[] temp;
            line = br.readLine();
            temp = StringUtils.split(line,"\t");
            int classIndex = ArrayUtils.indexOf(temp, "班级名称");
            int majorIndex = ArrayUtils.indexOf(temp, "专业大类/专业");
            while ((line = br.readLine()) != null) {
                temp = StringUtils.splitPreserveAllTokens(line,"\t");
                map.put(temp[classIndex], temp[majorIndex]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static void main(String[] args) {
        String mapFile = "/Users/xudaxing/Documents/hist/教务处/李玲/working/班级+专业+院系.txt";
        String inputFile = "/Users/xudaxing/Documents/hist/教务处/李玲/working/评估课表.txt";
        String outputFile = "/Users/xudaxing/Documents/hist/教务处/李玲/working/评估课表_许达兴.txt";
        Map<String, String> map = map(mapFile);
        temp1(inputFile, map, outputFile);
    }
}
