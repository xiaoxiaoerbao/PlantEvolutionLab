package fa;

import utils.IOTool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * This class use a byte to store a base
 * Ascii char
 * 65 A
 * 67 C
 * 71 G
 * 84 T
 * 78 N
 */
public class Fasta {

    String[] headers;
    byte[][] sequences;

    public enum Base {
        A,T,C,G,N
    }

    public Fasta(String[] headers, byte[][] sequences) {
        this.headers = headers;
        this.sequences = sequences;
    }

    public String[] getHeaders() {
        return headers;
    }

    public byte[][] getSequences() {
        return sequences;
    }

    public int getFastaRecordCount(){
        return this.headers.length;
    }

    public static Fasta buildFastaFrom(String fastaFile) {
        List<String> headerList = new ArrayList<>();
        List<byte[]> sequenceList = new ArrayList<>();
        try (BufferedReader br = IOTool.getBufferedReader(fastaFile)) {
            String line;
            StringBuilder sb = new StringBuilder();
            byte[] seq;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (sb.length() > 0) {
                        seq = sb.toString().getBytes();
                        sequenceList.add(seq);
                    }
                    headerList.add(line);
                    sb.setLength(0);
                }else {
                    sb.append(line.toUpperCase());
                }
            }
            seq = sb.toString().getBytes();
            sequenceList.add(seq);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] headers = headerList.toArray(new String[0]);
        byte[][] sequences = sequenceList.toArray(new byte[sequenceList.size()][]);
        return new Fasta(headers, sequences);
    }

    /**
     * rename header with new header array
     * @param newHeader new header array
     */
    public void renameHeader(String[] newHeader) {
        String[] headers = new String[this.headers.length];
        for (int i = 0; i < newHeader.length; i++) {
            headers[i] = ">" + newHeader[i];
        }
        this.headers = headers;
    }

    public String getHeader(int index){
        return headers[index];
    }

    public byte[] getSequence(int index){
        return sequences[index];
    }

    public double[] calculateBaseProportions(Base base){
        return this.calculateBaseProportions(EnumSet.of(base));
    }

    public double[] calculateGCProportions(){
        return this.calculateBaseProportions(EnumSet.of(Base.C, Base.G));
    }

    private double[] calculateBaseProportions(EnumSet<Base> baseEnumSet){
        byte[] baseAsciis = new byte[baseEnumSet.size()];
        int setIndex = 0;
        for (Base base : baseEnumSet) {
            baseAsciis[setIndex] = base.toString().getBytes()[0];
            setIndex++;
        }
        byte[][] sequences = this.getSequences();
        int[] baseCounter = new int[sequences.length];
        double[] proportions = new double[sequences.length];
        for (int i = 0; i < sequences.length; i++) {
            for (int j = 0; j < sequences[i].length; j++) {
                for (byte baseAscii : baseAsciis) {
                    if (sequences[i][j] == baseAscii){
                        baseCounter[i]++;
                        break;
                    }
                }
            }
            proportions[i] = (double)baseCounter[i]/sequences[i].length;
        }
        return proportions;
    }

    /**
     *
     * @param fastaFile fasta file path
     * @param lineLength line length
     */
    public void writeFasta(String fastaFile, int lineLength) {
        try (BufferedWriter bw = IOTool.getBufferedWriter(fastaFile)) {
            byte[][] sequences = this.getSequences();
            String[] headers = this.getHeaders();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sequences.length; i++) {
                sb.setLength(0);
                sb.append(headers[i]).append("\n");
                for (int j = 0; j < sequences[i].length; j++) {
                    if (j > 0 && j % lineLength == 0){
                        sb.append("\n").append((char)sequences[i][j]);
                    }else {
                        sb.append((char) sequences[i][j]);
                    }
                }
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * write fasta file with line length 60
     * @param fastaFile fasta file path
     */
    public void writeFasta(String fastaFile) {
        this.writeFasta(fastaFile, 60);
    }

}
