package start;

import dutils.GenomeType;
import dutils.Range;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sparse prefix tables at requested BED endpoints, stored on disk.
 * A boundary b stores sums/counts for 1-based positions <= b, so [start,end)
 * is prefix(end)-prefix(start). No genomic-length or whole-depth arrays are allocated.
 */
final class EndpointDepthIndex implements AutoCloseable {
    // Q20 record: two long depth sums followed by eight int threshold counts.
    private static final int Q20_BYTES = 48;
    private static final int Q0_BYTES = 16;
    private final Map<String, Entry> entries;
    private final BlockCache cache;
    private final long q20Records;
    private final long q0Records;

    private static final class Entry {
        final Path raw, endpoints, q20, q0;
        long count;
        Entry(Path work, int index) {
            String name = "chr-" + index;
            raw = work.resolve(name + ".raw");
            endpoints = work.resolve(name + ".endpoints");
            q20 = work.resolve(name + ".q20");
            q0 = work.resolve(name + ".q0");
        }
    }

    private EndpointDepthIndex(Map<String, Entry> entries, int cacheMiB, long q20Records, long q0Records) {
        this.entries = entries;
        cache = new BlockCache(cacheMiB);
        this.q20Records = q20Records;
        this.q0Records = q0Records;
    }

    static EndpointDepthIndex build(Configurations configs, File regionsDir, File q20, File q0,
                                    Path work, int sortChunkInts, int cacheMiB) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        Map<String, DataOutputStream> writers = new LinkedHashMap<>();
        try {
            for (int i = 0; i < configs.config_id.length; i++) {
                GenomeType type = configs.genomeType[i];
                RangeStatistics.requireSupported(type);
                Set<String> allowed = type.getChromosomes().stream().map(c -> c.getName()).collect(Collectors.toSet());
                File directory = new File(regionsDir, configs.config_id[i]);
                Range[] ranges = Configurations.readRegions(new File(directory, "final.bed"));
                for (Range range : ranges) {
                    if (!allowed.contains(range.chr())) {
                        throw new IllegalArgumentException(configs.config_id[i] + ": chromosome " + range.chr()
                                + " does not belong to configuration genomeType " + type.getShortName());
                    }
                    Entry entry = entries.get(range.chr());
                    if (entry == null) {
                        entry = new Entry(work, entries.size());
                        entries.put(range.chr(), entry);
                        writers.put(range.chr(), IntFileSorter.output(entry.raw));
                    }
                    DataOutputStream out = writers.get(range.chr());
                    out.writeInt(range.start());
                    out.writeInt(range.end());
                }
            }
        } finally {
            IOException failure = null;
            for (DataOutputStream writer : writers.values()) {
                try { writer.close(); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            }
            if (failure != null) throw failure;
        }
        long endpoints = 0;
        for (Entry entry : entries.values()) {
            IntFileSorter.sortUnique(entry.raw, entry.endpoints, sortChunkInts);
            entry.count = Files.size(entry.endpoints) / Integer.BYTES;
            endpoints += entry.count;
        }
        System.out.println("Unique BED endpoints: " + endpoints + "; chromosomes: " + entries.size());
        long n20 = buildDepthTables(entries, q20, true);
        long n0 = buildDepthTables(entries, q0, false);
        return new EndpointDepthIndex(entries, cacheMiB, n20, n0);
    }

    private static long buildDepthTables(Map<String, Entry> entries, File depthFile, boolean q20) throws IOException {
        String label = q20 ? "Q20" : "Q0";
        System.out.println("Streaming " + label + " depths: " + depthFile);
        Set<String> written = new HashSet<>();
        PrefixWriter writer = null;
        String chromosome = null;
        long count = 0;
        long blockCount = 0;
        try (PosDepth.DepthReader reader = new PosDepth.DepthReader(depthFile)) {
            while (reader.next()) {
                if (!reader.chromosome().equals(chromosome)) {
                    if (writer != null) { writer.close(); writer = null; }
                    if (chromosome != null) System.out.println("  " + label + " " + chromosome + ": " + blockCount + " records");
                    chromosome = reader.chromosome();
                    blockCount = 0;
                    Entry entry = entries.get(chromosome);
                    if (entry != null) {
                        writer = new PrefixWriter(entry, q20);
                        written.add(chromosome);
                    }
                }
                if (writer != null) writer.accept(reader.position(), reader.xp56Depth(), reader.xp57Depth());
                count++;
                blockCount++;
            }
        } finally {
            if (writer != null) writer.close();
        }
        if (chromosome != null) System.out.println("  " + label + " " + chromosome + ": " + blockCount + " records");
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            if (!written.contains(entry.getKey())) {
                // Entirely omitted chromosomes are all-zero under the input contract.
                try (PrefixWriter ignored = new PrefixWriter(entry.getValue(), q20)) { }
            }
        }
        return count;
    }

    /** Only cumulative totals and the next endpoint are held while streaming a chromosome. */
    private static final class PrefixWriter implements AutoCloseable {
        private final DataInputStream boundaries;
        private final DataOutputStream output;
        private final boolean q20;
        private long remaining;
        private int nextBoundary;
        private long sum56, sum57;
        private final int[] counts = new int[8];

        PrefixWriter(Entry entry, boolean q20) throws IOException {
            this.q20 = q20;
            boundaries = IntFileSorter.input(entry.endpoints);
            try { output = IntFileSorter.output(q20 ? entry.q20 : entry.q0); }
            catch (IOException e) { boundaries.close(); throw e; }
            remaining = entry.count;
            if (remaining > 0) nextBoundary = boundaries.readInt();
        }

        void accept(int position, int d56, int d57) throws IOException {
            // A 1-based base p contributes only to boundaries b >= p.
            while (remaining > 0 && nextBoundary < position) flushBoundary();
            sum56 += d56;
            sum57 += d57;
            if (q20) {
                if (d56 >= 1) counts[0]++;
                if (d57 >= 1) counts[1]++;
                if (d56 >= 5) counts[2]++;
                if (d57 >= 5) counts[3]++;
                if (d56 >= 10) counts[4]++;
                if (d57 >= 10) counts[5]++;
                if (d56 >= 5 && d57 >= 5) counts[6]++;
                if (d56 >= 10 && d57 >= 10) counts[7]++;
            }
        }

        private void flushBoundary() throws IOException {
            output.writeLong(sum56);
            output.writeLong(sum57);
            if (q20) for (int count : counts) output.writeInt(count);
            remaining--;
            if (remaining > 0) nextBoundary = boundaries.readInt();
        }

        @Override public void close() throws IOException {
            try { while (remaining > 0) flushBoundary(); }
            finally { try { output.close(); } finally { boundaries.close(); } }
        }
    }

    RangeStatistics query(String configId, GenomeType type, int rangeIndex, Range range, int gapLength,
                          double baseline56, double baseline57) throws IOException {
        Entry entry = entries.get(range.chr());
        if (entry == null) throw new IOException("BED inputs changed during evaluation: " + range);
        long left = find(entry, range.start());
        long right = find(entry, range.end());
        long l20 = left * Q20_BYTES, r20 = right * Q20_BYTES;
        long l0 = left * Q0_BYTES, r0 = right * Q0_BYTES;
        long sum56 = differenceLong(entry.q20, l20, r20);
        long sum57 = differenceLong(entry.q20, l20 + 8, r20 + 8);
        long q0Sum56 = differenceLong(entry.q0, l0, r0);
        long q0Sum57 = differenceLong(entry.q0, l0 + 8, r0 + 8);
        int[] counts = new int[8];
        for (int i = 0; i < counts.length; i++) counts[i] = cache.getInt(entry.q20, r20 + 16 + i * 4)
                - cache.getInt(entry.q20, l20 + 16 + i * 4);
        return new RangeStatistics(configId, type, rangeIndex, range, gapLength, sum56, sum57, q0Sum56, q0Sum57,
                counts[0], counts[1], counts[2], counts[3], counts[4], counts[5], counts[6], counts[7], baseline56, baseline57);
    }

    private long differenceLong(Path file, long left, long right) throws IOException {
        return cache.getLong(file, right) - cache.getLong(file, left);
    }

    private long find(Entry entry, int position) throws IOException {
        long low = 0, high = entry.count - 1;
        while (low <= high) {
            long mid = (low + high) >>> 1;
            int value = cache.getInt(entry.endpoints, mid * Integer.BYTES);
            if (value < position) low = mid + 1;
            else if (value > position) high = mid - 1;
            else return mid;
        }
        throw new IOException("BED endpoint absent from index; inputs changed during evaluation: " + position);
    }

    long q20Records() { return q20Records; }
    long q0Records() { return q0Records; }
    long endpointCount() { return entries.values().stream().mapToLong(e -> e.count).sum(); }
    @Override public void close() { cache.clear(); }

    /** One shared LRU budget for all chromosomes and both depth tables; no memory mapping. */
    private static final class BlockCache {
        private static final int BLOCK_BYTES = 64 * 1024;
        private record Key(Path file, long block) {}
        private final int capacity;
        private final LinkedHashMap<Key, ByteBuffer> blocks = new LinkedHashMap<>(16, 0.75f, true);

        BlockCache(int mebibytes) { capacity = Math.max(1, (int) ((long) mebibytes * 1024 * 1024 / BLOCK_BYTES)); }
        int getInt(Path file, long offset) throws IOException { return block(file, offset).getInt((int) (offset % BLOCK_BYTES)); }
        long getLong(Path file, long offset) throws IOException { return block(file, offset).getLong((int) (offset % BLOCK_BYTES)); }

        private ByteBuffer block(Path file, long offset) throws IOException {
            long number = offset / BLOCK_BYTES;
            Key key = new Key(file, number);
            ByteBuffer result = blocks.get(key);
            if (result != null) return result;
            // Evict before allocating, keeping live buffers within the configured budget.
            if (blocks.size() >= capacity) blocks.pollFirstEntry();
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                long start = number * BLOCK_BYTES;
                int size = (int) Math.min(BLOCK_BYTES, channel.size() - start);
                if (size <= 0) throw new IOException("Truncated prefix table: " + file);
                result = ByteBuffer.allocate(size);
                while (result.hasRemaining()) {
                    int n = channel.read(result, start + result.position());
                    if (n < 0) throw new IOException("Truncated prefix table: " + file);
                }
                result.flip();
            }
            blocks.put(key, result);
            return result;
        }

        void clear() { blocks.clear(); }
    }
}
