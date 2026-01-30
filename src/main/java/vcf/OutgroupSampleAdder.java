package vcf;

import org.apache.commons.lang3.StringUtils;
import utils.IOTool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class OutgroupSampleAdder {

    public static void addOutgroupSample(String inputVcf, String outputVcf, String ancFilePath, String outgroupSampleName) {
        try (BufferedReader br = IOTool.getBufferedReader(inputVcf);
             BufferedReader brAnc = IOTool.getBufferedReader(ancFilePath);
             BufferedWriter bw = IOTool.getBufferedWriter(outputVcf)) {

            String line;
            String ancLine = brAnc.readLine(); // 预读第一行祖先数据

            // 1. 处理元信息行 (##)
            while ((line = br.readLine()) != null && line.startsWith("##")) {
                bw.write(line);
                bw.newLine();
            }

            // 2. 处理表头行 (#CHROM) 并添加新样本名
            if (line != null && line.startsWith("#")) {
                bw.write(line + "\t" + outgroupSampleName);
                bw.newLine();
            }

            int countRef = 0;
            int countAlt = 0;

            // 3. 核心遍历：同步 VCF 和 Ancestral File
            while ((line = br.readLine()) != null) {
                String[] vcfTemp = StringUtils.split(line, "\t");
                String vcfChr = vcfTemp[0];
                int vcfPos = Integer.parseInt(vcfTemp[1]);
                String refAllele = vcfTemp[3];
                String altAllele = vcfTemp[4];

                String ancAllele = "./."; // 默认缺失

                // 双指针同步：如果祖先文件没读完，尝试匹配坐标
                while (ancLine != null) {
                    String[] ancTemp = StringUtils.split(ancLine, "\t");
                    String ancChr = ancTemp[0];
                    int ancPos = Integer.parseInt(ancTemp[1]);

                    // 情况 A: 祖先文件坐标落后，继续读下一行
                    if (ancChr.compareTo(vcfChr) < 0 || (ancChr.equals(vcfChr) && ancPos < vcfPos)) {
                        ancLine = brAnc.readLine();
                        continue;
                    }

                    // 情况 B: 坐标完全匹配
                    if (ancChr.equals(vcfChr) && ancPos == vcfPos) {
                        String rawAnc = ancTemp[3]; // README 提到的第 4 列
                        // 将祖先碱基转化为 VCF 的 GT 索引 (0 是 Ref, 1,2.. 是 Alt)
                        if (rawAnc.equals(refAllele)) {
                            ancAllele = "0/0";
                            countRef++;
                        } else if (rawAnc.equals(altAllele)){
                            // 这里简单处理为 1/1，严格来说需对比 Alt 列表，小麦研究中通常设为 1/1
                            ancAllele = "1/1";
                            countAlt++;
                        } else {
                            ancAllele = "./.";
                        }
                    }
                    // 情况 C: 祖先坐标超前了，跳出内循环等待 VCF 追赶
                    break;
                }

                // 4. 拼接新行并写入
                // 假设 VCF 格式包含 FORMAT 列，我们添加对应样本的 GT
                bw.write(line + "\t" + ancAllele);
                bw.newLine();
            }

            System.out.println("Ref count: " + countRef);
            System.out.println("Alt count: " + countAlt);
            System.out.println("Processing complete: " + outputVcf);

        } catch (IOException e) {
            throw new RuntimeException("Error processing wheat genomic data", e);
        }
    }

}