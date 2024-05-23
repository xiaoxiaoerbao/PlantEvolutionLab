import IOTool.IOTool;
import it.unimi.dsi.fastutil.bytes.Byte2CharMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ByteMap;
import it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * This class is used to compress and store genomic DNA, which contains only 'A','T','C','G','N','a','t','c','g'
 * Every two bases are compressed into one byte.
 * @author xudaxing
 */
public class GenomeSequence {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors()-1;
    private static final int BLOCK_SIZE = 10_000_000;
    byte[][] seq;
    String[] scaffoldNames;

    private static final char[] atcgnChar = {'A','T','C','G','N','a','t','c','g'};
    private static final byte[] atcgnByte = {0b0001,0b0010,0b0011,0b0100,0b0101,0b0110,0b0111,0b1000,0b1001};
    private static final Char2ByteMap char2ByteMap = getChar2ByteMap();
    private static final Byte2CharMap byte2CharMap = getByte2CharMap();

    private static Char2ByteMap getChar2ByteMap(){
        Char2ByteMap char2ByteMap = new Char2ByteOpenHashMap();
        for (int i = 0; i < atcgnChar.length; i++) {
            char2ByteMap.put(atcgnChar[i], atcgnByte[i]);
        }
        return char2ByteMap;
    }

    private static Byte2CharMap getByte2CharMap(){
        Byte2CharMap byte2CharMap = new Byte2CharOpenHashMap();
        for (int i = 0; i < atcgnChar.length; i++) {
            byte2CharMap.put(atcgnByte[i], atcgnChar[i]);
        }
        return byte2CharMap;
    }

    public GenomeSequence(String fnaFile){
        List<String> scaffoldNameList = new ArrayList<>();
        IntList scaffold2BlockCountList = new IntArrayList();
        try (BufferedReader br = IOTool.getBufferedReader(fnaFile)) {
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
            String line;
            StringBuilder sb = new StringBuilder();
            int currentScaffoldIndex = 0;
            int nextScaffoldIndex = -1;
            int blockCount = 0;
            List<Callable<byte[]>> callableList = new ArrayList<>();
            while ((line=br.readLine()) != null) {
                if (line.startsWith(">")){
                    scaffoldNameList.add(line.substring(1));
                    nextScaffoldIndex++;
                    continue;
                }
                if (nextScaffoldIndex == currentScaffoldIndex){
                    sb.append(line);
                    if (sb.length() > BLOCK_SIZE){
                        String blockCharacters = sb.toString();
                        callableList.add(()->this.processBlock(blockCharacters));
                        blockCount++;
                        sb.setLength(0);
                    }
                }else {
                    if (sb.length() > 0){
                        String blockCharacters = sb.toString();
                        callableList.add(()->this.processBlock(blockCharacters));
                        blockCount++;
                        scaffold2BlockCountList.add(blockCount);
                    }else {
                        scaffold2BlockCountList.add(blockCount);
                    }
                    sb.setLength(0);
                    sb.append(line);
                    currentScaffoldIndex = nextScaffoldIndex;
                }
            }
            if (sb.length() > 0){
                String blockCharacters = sb.toString();
                callableList.add(()->this.processBlock(blockCharacters));
                blockCount++;
                scaffold2BlockCountList.add(blockCount);
            }
            this.scaffoldNames=scaffoldNameList.toArray(new String[0]);
            this.seq = new byte[scaffoldNameList.size()][];
            List<Future<byte[]>> futureList = executorService.invokeAll(callableList);
            int scaffoldFromIndex = 0;
            int scaffoldToIndex;
            int scaffoldByteSize;
            byte[] currentBlock;
            int blockTo;
            for (int i = 0; i < scaffold2BlockCountList.size(); i++) {
                scaffoldToIndex = scaffold2BlockCountList.getInt(i);
                scaffoldByteSize = 0;
                for (int j = scaffoldFromIndex; j < scaffoldToIndex; j++) {
                    scaffoldByteSize += futureList.get(j).get().length;
                }
                this.seq[i] = new byte[scaffoldByteSize];
                blockTo = 0;
                for (int j = scaffoldFromIndex; j < scaffoldToIndex; j++) {
                    currentBlock = futureList.get(j).get();
                    System.arraycopy(currentBlock,0, this.seq[i], blockTo, currentBlock.length);
                    blockTo += currentBlock.length;
                }
                scaffoldFromIndex = scaffoldToIndex;
            }
            executorService.shutdown();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] processBlock(String scaffoldCharacters){
        return this.encoding(scaffoldCharacters.toCharArray());
    }

    private byte[] encoding(char[] chars){
        int length = chars.length;
        int compressedSeqLength = (length & 1) == 0 ? length/2 : length/2 + 1;
        byte[] compressedSeq = new byte[compressedSeqLength];
        byte current, next;
        for (int i = 0; i < chars.length; i=i+2) {
            current = char2ByteMap.get(chars[i]);
            current <<= 4;
            if (i+1 < chars.length) {
                next = char2ByteMap.get(chars[i+1]);
            }else {
                next = 0;
            }
            compressedSeq[i/2] = (byte)(current | next);
        }
        return compressedSeq;
    }

    private char[] decoding(byte[] seq){
        CharList baseList = new CharArrayList();
        byte halfA, halfB;
        for (byte b : seq) {
            halfA = (byte) (0b1111 & (b >> 4));
            halfB = (byte) (0b00001111 & b);
            baseList.add(byte2CharMap.get(halfA));
            if (halfB != 0) {
                baseList.add(byte2CharMap.get(halfB));
            }
        }
        char[] chars = new char[baseList.size()];
        baseList.toArray(chars);
        return chars;
    }

    public void rename(String[] scaffoldNames){
        this.scaffoldNames=scaffoldNames;
    }

    public char[][] getAtcgnChar(String scaffoldName){
        char[][] chars = new char[scaffoldNames.length][];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = decoding(seq[i]);
        }
        return chars;
    }

    public static void main(String[] args) {
        String file = "/Users/xudaxing/Data/wheatReference/ref/CS1_v2.1_genomic.fna.gz";
        long startTime = System.currentTimeMillis();
        GenomeSequence genomeSequence = new GenomeSequence(file);
        long endTime = System.currentTimeMillis();
        System.out.println((endTime-startTime)/(1000*1000) + "s");
        char[][] chars = genomeSequence.getAtcgnChar(file);
    }

}
