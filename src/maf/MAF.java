package maf;

import dutils.Strand;
import org.apache.commons.lang3.StringUtils;
import utils.IOTool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * multiple alignment format
 */
public class MAF {

    List<MAFAlignment> mafAlignmentList;

    public MAF(File mafFile){
        List<MAFAlignment> mafAlignmentList= new ArrayList<>();
        MAFAlignment mafAlignment;
        List<String> linesPerAlignment= new ArrayList<>();
        String line;
        try (BufferedReader br = IOTool.getBufferedReader(mafFile)) {
            while ((line=br.readLine()).startsWith("#")){}
            while ((line=br.readLine())!=null){
                if (StringUtils.isBlank(line)){
                    mafAlignment = new MAFAlignment(linesPerAlignment);
                    mafAlignmentList.add(mafAlignment);
                    linesPerAlignment.clear();
                }else {
                    linesPerAlignment.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mafAlignmentList=mafAlignmentList;
    }

    public void callAncestralAllele(List<Species> speciesList, String outputFile){
        Set<Species> speciesSet = new HashSet<>(speciesList);
        assert speciesSet.size() == speciesList.size() : "speciesList contains duplicate species";
        Record record;
        StringBuilder sb=new StringBuilder();
        for (Species species : speciesList) {
            sb.append("Chr_").append(species.getSpeciesNameID()).append("\t");
            sb.append("Pos_").append(species.getSpeciesNameID()).append("\t");
            sb.append("Allele_").append(species.getSpeciesNameID()).append("\t");
        }
        EnumSet<Species> enumSet = EnumSet.copyOf(speciesList);
        sb.deleteCharAt(sb.length()-1);
        try (BufferedWriter bw = IOTool.getBufferedWriter(outputFile)) {
            bw.write(sb.toString());
            bw.newLine();
            int refPos1basedWithoutDash;
            byte baseAscII;
            for (MAFAlignment mafAlignment : mafAlignmentList) {
                for (int i = 0; i < mafAlignment.alignmentSize; i++) {
                    sb.setLength(0);
                    if (!mafAlignment.anyDash(enumSet, i)) {
                        for (Species sp : speciesList) {
                            record = mafAlignment.speciesMAFRecordEnumMap.get(sp);
                            baseAscII = record.baseAscIIArray[i];
                            refPos1basedWithoutDash = record.get1basedRefPosWithoutDash(i);
                            sb.append(record.refChr).append("\t").append(refPos1basedWithoutDash).append("\t");
                            sb.append((char) baseAscII).append("\t");
                        }
                    }
                    if (!sb.isEmpty()) {
                        sb.deleteCharAt(sb.length()-1);
                        bw.write(sb.toString());
                        bw.newLine();
                    }
                }
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Record {
        String refChr;
        int start;
        int size;
        Strand strand;
        int srcSize;
        byte[] baseAscIIArray;

        Record(String refChr, int start, int size, Strand strand, int srcSize, byte[] ascIIArray){
            this.refChr=refChr;
            this.start=start;
            this.size=size;
            this.strand=strand;
            this.srcSize=srcSize;
            this.baseAscIIArray =ascIIArray;
        }

        public boolean isDash(int indexInBaseAscIIArray){
            return baseAscIIArray[indexInBaseAscIIArray]==45;
        }

        public int getUCSCStartPosWithoutDash(int indexInBaseAscIIArray){
            int ucscPos= this.start;
            for (int i = 0; i < indexInBaseAscIIArray; i++) {
                if (isDash(i)) continue;
                ucscPos++;
            }
            return ucscPos;
        }

        public int get1basedRefPosWithoutDash(int indexInBaseAscIIArray){
            int ucscPos=this.getUCSCStartPosWithoutDash(indexInBaseAscIIArray);
            Coordinate coordinate = Coordinate.getInstance();
            coordinate.setFrom(strand, ucscPos, srcSize);
            return coordinate.getOneStart();
        }
    }

    public static class MAFAlignment{
        double score;
        int alignmentSize;
        EnumMap<Species, Record> speciesMAFRecordEnumMap= new EnumMap<>(Species.class);

        MAFAlignment(List<String> linesPerAlignment){
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
                record= new Record(refChr, start, size, strand, srcSize, ascIIArray);
                speciesMAFRecordEnumMap.put(species, record);
            }
            this.alignmentSize=alignmentSize;
        }

        /**
         * 指定的species 和 index 是否存在 dash(-)
         */
        public boolean anyDash(EnumSet<Species> speciesEnumSet, int indexInBaseAscIIArray){
            for (Species species : speciesEnumSet){
                if(speciesMAFRecordEnumMap.get(species).isDash(indexInBaseAscIIArray)) return true;
            }
            return false;
        }

    }

}
