package utils.fasta;

import utils.IOTool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Fasta {

    FastaRecordBit[] fastaRecordBits;

    public Fasta(String fastaFile) {
        try (BufferedReader br = IOTool.getBufferedReader(fastaFile)) {
            List<FastaRecordBit> fastaRecordBitList = new ArrayList<>();
            String line, currentFastaRecordName = null;
            List<byte[]> seqList = new ArrayList<>();
            byte[] seq;
            int seqLen=0;
            int destPos=0;
            boolean ifFirst = true;
            FastaRecordBit fastaRecordBit;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">") && ifFirst){
                    currentFastaRecordName = line.substring(1);
                    ifFirst = false;
                    continue;
                }else if (line.startsWith(">") && (!ifFirst)) {
                    seq = new byte[seqLen];
                    Arrays.fill(seq, (byte) -1);
                    for (byte[] bytes : seqList) {
                        System.arraycopy(bytes, 0, seq, destPos, bytes.length);
                        destPos += bytes.length;
                    }
                    fastaRecordBit = new FastaRecordBit(currentFastaRecordName, seq);
                    System.out.println("Reading in "+ currentFastaRecordName+", its length is "+seq.length);
                    fastaRecordBitList.add(fastaRecordBit);
                    seqLen = 0;
                    destPos = 0;
                    seqList.clear();
                    currentFastaRecordName = line.substring(1);
                    continue;
                }
                seqLen += line.length();
                seqList.add(line.getBytes());
            }

            // 处理最后一个记录
            if (!seqList.isEmpty()) {
                seq = new byte[seqLen];
                for (byte[] seqPart : seqList) {
                    System.arraycopy(seqPart, 0, seq, destPos, seqPart.length);
                    destPos += seqPart.length;
                }
                fastaRecordBit = new FastaRecordBit(currentFastaRecordName, seq);
                System.out.println("Reading in "+ currentFastaRecordName);
                fastaRecordBitList.add(fastaRecordBit);
            }

            fastaRecordBits = fastaRecordBitList.toArray(new FastaRecordBit[0]);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FastaRecordBit[] getFastaRecordBits() {
        return fastaRecordBits;
    }

    public int getFastaRecordNum(){
        return fastaRecordBits.length;
    }

    public void setFastaRecordName(String[] fastaRecordNewNames){
        for (int i = 0; i < fastaRecordNewNames.length; i++) {
            this.setFastaRecordName(i, fastaRecordNewNames[i]);
        }
    }

    public void setFastaRecordName(int fastaRecordIndex, String fastaRecordNewName){
        this.fastaRecordBits[fastaRecordIndex].setFastaRecordName(fastaRecordNewName);
    }

    public String[] getFastaRecordNames(){
        String[] fastaRecordNames = new String[fastaRecordBits.length];
        for (int i = 0; i < fastaRecordBits.length; i++) {
            fastaRecordNames[i] = fastaRecordBits[i].getFastaRecordName();
        }
        return fastaRecordNames;
    }

    public void removeContigsOrScaffold(){
        boolean[] contigsOrScaffold = new boolean[this.getFastaRecordNum()];
        for (int i = 0; i < this.getFastaRecordNum(); i++) {
            contigsOrScaffold[i] = this.getFastaRecordBits()[i].containsContigsOrScaffold();
        }
        List<FastaRecordBit> fastaRecordBitList = new ArrayList<>();
        for (int i = 0; i < contigsOrScaffold.length; i++) {
            if (contigsOrScaffold[i]) {
                System.out.println(this.getFastaRecordNames()[i]+" were removed");
            }else {
                fastaRecordBitList.add(this.getFastaRecordBits()[i]);
            }
        }
        this.fastaRecordBits = fastaRecordBitList.toArray(new FastaRecordBit[0]);
    }

    public void writeToFile(String fastaFile, int lineLength) {
        try (BufferedWriter bw = IOTool.getBufferedWriter(fastaFile)) {
            for (FastaRecordBit record : this.getFastaRecordBits()) {
                // 写入 FASTA 名称行
                bw.write(record.getFastaRecordName());
                bw.newLine();

                // 获取序列字节并按指定长度换行
                byte[] seqBytes = record.getSeq();
                for (int i = 0; i < seqBytes.length; i += lineLength) {
                    int end = Math.min(i + lineLength, seqBytes.length);
                    bw.write(new String(seqBytes, i, end - i));
                    bw.newLine();
                }
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to file: " + fastaFile, e);
        }
    }

    public void writeToFile(String fastaFile) {
        writeToFile(fastaFile, 80);
    }
}
