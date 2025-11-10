package maf;

import dutils.Strand;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import utils.IOTool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parser and processor for Multi-Alignment Format (MAF) files.
 * Each MAF file contains multiple alignment blocks across species.
 * This class supports reading MAF blocks and calling ancestral alleles.
 * @author Xu Daxing
 * @version 1.0
 * @since 2025-11-10
 */
public class MAF {

    List<MAFBlock> mafBlockList;

    public MAF(String mafFile){
        List<MAFBlock> mafBlockList = new ArrayList<>();
        MAFBlock mafBlock;
        List<String> linesPerAlignment= new ArrayList<>();
        String line;
        try (BufferedReader br = IOTool.getBufferedReader(mafFile)) {
            while ((line=br.readLine()).startsWith("#")){}
            while ((line=br.readLine())!=null){
                if (StringUtils.isBlank(line)){
                    mafBlock = new MAFBlock(linesPerAlignment);
                    mafBlockList.add(mafBlock);
                    linesPerAlignment.clear();
                }else {
                    linesPerAlignment.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mafBlockList = mafBlockList;
    }

    /**
     * Call ancestral alleles at aligned genomic positions for specified species.
     * Only positions without gaps across species will be output.
     * @param speciesList  Ordered list of species to extract
     * @param outputFile   Output TSV file
     */
    public void callAncestralAllele(List<Species> speciesList, String outputFile){
        Set<Species> speciesSet = new HashSet<>(speciesList);
        assert speciesSet.size() == speciesList.size() : "speciesList contains duplicate species";
        StringBuilder sb=new StringBuilder();
        for (Species species : speciesList) {
            sb.append("Chr_").append(species.getSpeciesNameID()).append("\t");
            sb.append("Pos_").append(species.getSpeciesNameID()).append("\t");
            sb.append("Allele_").append(species.getSpeciesNameID()).append("\t");
        }
        sb.deleteCharAt(sb.length()-1);
        try (BufferedWriter bw = IOTool.getBufferedWriter(outputFile)) {
            bw.write(sb.toString());
            bw.newLine();
            int[] dashCountArray;
            byte baseAscII;
            int[] alignmentBlockStartPos;
            int currentPos;
            List<Record> subRecords = new ArrayList<>();
            boolean[] haveDash;
            for (MAFBlock mafBlock : mafBlockList) {
                subRecords.clear();
                subRecords = mafBlock.getSubRecords(speciesList);
                if (subRecords.size() < speciesList.size()) continue;
                alignmentBlockStartPos = new int[speciesList.size()];
                for (int i = 0; i < speciesList.size(); i++) {
                    if (subRecords.get(i).strandRange.getStrand().equals(Strand.PLUS)){
                        alignmentBlockStartPos[i] = subRecords.get(i).strandRange.getStart();
                    }else {
                        alignmentBlockStartPos[i] = subRecords.get(i).strandRange.getChromSize()-1-subRecords.get(i).strandRange.getStart();
                    }
                }
                dashCountArray = new int[speciesList.size()];
                for (int i = 0; i < mafBlock.alignmentSize; i++) {
                    sb.setLength(0);
                    haveDash = new boolean[speciesList.size()];
                    for (int j = 0; j < subRecords.size(); j++) {
                        if (subRecords.get(j).isDash(i)){
                            dashCountArray[j]++;
                            haveDash[j] = true;
                        }
                        if (subRecords.get(j).strandRange.getStrand().equals(Strand.PLUS)){
                            currentPos = alignmentBlockStartPos[j]+i-dashCountArray[j];
                        }else {
                            currentPos = alignmentBlockStartPos[j]-i-dashCountArray[j];
                        }
                        sb.append(subRecords.get(j).strandRange.getChr()).append("\t");
                        sb.append(currentPos+1).append("\t");
                        baseAscII=subRecords.get(j).baseAscIIArray[i];
                        sb.append((char)baseAscII).append("\t");
                    }
                    if (BooleanUtils.or(haveDash)) continue;
                    sb.deleteCharAt(sb.length()-1);
                    bw.write(sb.toString());
                    bw.newLine();
                }
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Represents one species sequence record within a MAF alignment block.
     * Stores species, genomic location, strand, and aligned base sequence.
     */
    public static class Record {
        Species species;
        StrandRange strandRange;
        byte[] baseAscIIArray; // Aligned sequence including gaps

        Record(Species species, String chr, int start, int size, Strand strand, int chromSize, byte[] ascIIArray){
            this.species=species;
            this.strandRange = new StrandRange(strand, chr, start, size, chromSize);
            this.baseAscIIArray =ascIIArray;
        }

        // Check if aligned base is a gap "-"
        public boolean isDash(int indexInBaseAscIIArray){
            return baseAscIIArray[indexInBaseAscIIArray]==45;
        }

    }

    /**
     * Enum of all supported species in the MAF.
     * Each species has an ID, full scientific name, and common name.
     */
    public enum Species {

        AS("As", "Aegilops speltoides", "Speltoid goatgrass"),
        ASH("Ash", "Aegilops sharonensis", "Sharon goatgrass"),
        AT("At", "Aegilops tauschii", "Tausch’s goatgrass"),
        BD("Bd", "Brachypodium distachyon", "Stiff brome"),
        HV("Hv", "Hordeum vulgare", "Barley"),
        SC("Sc", "Secale cereale", "Rye"),
        TAV1("Tav1", "Triticum aestivum", "Bread wheat"),
        TAV2("Tav2", "Triticum aestivum", "Bread wheat"),
        TE("Te", "Thinopyrum elongatum", "Tall wheatgrass"),
        TM("Tm", "Triticum monococcum", "Einkorn wheat"),
        TT("Tt", "Triticum turgidum", "Durum wheat");

        final String speciesNameID;
        final String speciesName;
        final String commonName;

        Species(String speciesNameID, String speciesName, String commonName) {
            this.speciesNameID = speciesNameID;
            this.speciesName=speciesName;
            this.commonName=commonName;
        }

        public String getSpeciesNameID() {
            return speciesNameID;
        }

        public String getSpeciesName(){
            return speciesName;
        }

        public String getCommonName() {
            return commonName;
        }

        // Map ID → Enum object, for fast lookup
        private static final Map<String,Species> speciesNameID2SpeciesMap = Arrays.stream(values()).collect(Collectors.toMap(Species::getSpeciesNameID, e->e));

        public static Species getInstanceFromStr(String speciesNameID){
            assert speciesNameID2SpeciesMap.containsKey(speciesNameID) : "speciesNameID not found: " + speciesNameID;
            return speciesNameID2SpeciesMap.get(speciesNameID);
        }

    }

    /**
     * Represents one alignment block in the MAF.
     * Each block has a score and multiple species records.
     */
    public static class MAFBlock {
        double score;
        int alignmentSize; // Alignment column length
        List<Record> recordList = new ArrayList<>();

        MAFBlock(List<String> linesPerAlignment){
            String[] temp, tem;
            score=Double.parseDouble(StringUtils.split(linesPerAlignment.get(0), "=")[1]);
            String refChr;
            int start, size, srcSize, alignmentSize=-1;
            Strand strand;
            byte[] ascIIArray;
            Species species;
            Record record;
            for (int i = 1; i < linesPerAlignment.size(); i++) {
                temp= StringUtils.split(linesPerAlignment.get(i), " ");
                tem=StringUtils.split(temp[1], ".");
                species= Species.getInstanceFromStr(tem[0]);
                refChr= tem[1].substring(3);
                start=Integer.parseInt(temp[2]);
                size = Integer.parseInt(temp[3]);
                strand=Strand.getInstanceFrom(temp[4]);
                srcSize=Integer.parseInt(temp[5]);
                alignmentSize=temp[6].length();
                ascIIArray=temp[6].toUpperCase().getBytes();
                record= new Record(species, refChr, start, size, strand, srcSize, ascIIArray);
                recordList.add(record);
            }
            this.alignmentSize=alignmentSize;
        }

        /**
         * Extract only the records belonging to the requested species (in input order).
         */
        public List<Record> getSubRecords(List<Species> speciesList){
            List<Record> subRecords = new ArrayList<>();
            for (Record record : recordList){
                if (speciesList.contains(record.species)){
                    subRecords.add(record);
                }
            }
            return subRecords;
        }

    }

    /**
     * Represents a genomic coordinate range on one strand.
     * Coordinates are always:
     *    - 0-based start
     *    - length (size of aligned region)
     *    - 0-based half-open interval [start, start + len)
     *    chromSize is needed to map reverse-strand positions:
     *    genomicPos = chromSize - 1 - (start + offset)
     */
    public static class StrandRange {

        /** DNA strand of this range, either PLUS or MINUS */
        Strand strand;

        private final String chr;
        /** Start position of the range (0-based, inclusive) */
        private final int start;

        /** Length of the range */
        private final int len;

        /** Total length of the chromosome */
        private final int chromSize;

        /**
         * Constructs a StrandRange object with the specified parameters.
         * @param strand     the DNA strand of the range, must be either PLUS or MINUS
         * @param start      the start position of the range (0-based, inclusive)
         * @param len        the length of the range
         * @param chromSize  the total length of the chromosome, must be greater than or equal to start + len
         */
        public StrandRange(Strand strand, String chr, int start, int len, int chromSize) {
            assert strand != null : "strand cannot be null";
            assert start >= 0 : "start cannot be negative";
            assert len >= 0 : "len cannot be negative";
            assert start + len <= chromSize : "start + len must be less than or equal to chromSize";
            this.strand = strand;
            this.chr = chr;
            this.start = start;
            this.len = len;
            this.chromSize = chromSize;
        }

        public String  getChr() {
            return chr;
        }

        public int getStart() {
            return start;
        }

        public int getLen() {
            return len;
        }

        public Strand getStrand() {
            return strand;
        }

        public int getChromSize() {
            return chromSize;
        }

        public int getEnd() {
            return start + len;
        }

        }

}
