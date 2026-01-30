package start;


import static vcf.OutgroupSampleAdder.addOutgroupSample;

public class Start {

    static void main(String[] args) {
        // 建议使用绝对路径或从配置文件读取
        String inputVCF = args[0];
        String outputVcf = args[1];
        String ancFilePath = args[2];
        String outgroupSampleName = args[3];
        addOutgroupSample(inputVCF, outputVcf, ancFilePath, outgroupSampleName);
    }
}
