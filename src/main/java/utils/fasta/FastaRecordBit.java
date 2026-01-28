package utils.fasta;

import org.apache.commons.lang3.StringUtils;

public class FastaRecordBit implements Comparable<FastaRecordBit>{

    String fastaRecordName;
    byte[] seq;

    public FastaRecordBit(String fastaRecordName, byte[] seq) {
        this.fastaRecordName = fastaRecordName;
        this.seq = seq;
    }

    public String getFastaRecordName() {
        return fastaRecordName;
    }

    public byte[] getSeq() {
        return seq;
    }

    public void setSeq(byte[] seq) {
        this.seq = seq;
    }

    public int getSeqLen(){
        return seq.length;
    }

    public void setFastaRecordName(String newFastaRecordName) {
        this.fastaRecordName = newFastaRecordName;
    }

    public String getSeqString(){
        StringBuilder sb = new StringBuilder();
        for (byte b : seq) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    public boolean containsContigsOrScaffold(){
        String name = this.fastaRecordName.toLowerCase();
        String[] temp = StringUtils.split(name, " ");
        return name.contains("contig") || name.contains("scaffold") || temp[0].length() > 6;
    }

    @Override
    public int compareTo(FastaRecordBit o) {
       return this.fastaRecordName.compareTo(o.getFastaRecordName());
    }
}
