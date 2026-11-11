package start;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Bounded-memory external sort and deduplication of non-negative big-endian BED endpoints. */
final class IntFileSorter {
    private static final int FAN_IN = 32;

    static void sortUnique(Path input, Path output, int chunkSize) throws IOException {
        if (Files.size(input) % Integer.BYTES != 0) throw new IOException("Truncated endpoint file: " + input);
        long remaining = Files.size(input) / Integer.BYTES;
        int[] values = new int[(int) Math.min(chunkSize, remaining)];
        List<Path> runs = new ArrayList<>();
        try (DataInputStream in = input(input)) {
            while (remaining > 0) {
                int count = (int) Math.min(values.length, remaining);
                for (int i = 0; i < count; i++) values[i] = in.readInt();
                remaining -= count;
                Arrays.sort(values, 0, count);
                Path run = Files.createTempFile(input.getParent(), "endpoint-run-", ".bin");
                runs.add(run);
                try (DataOutputStream out = output(run)) {
                    for (int i = 0; i < count; i++) if (i == 0 || values[i] != values[i - 1]) out.writeInt(values[i]);
                }
            }
        }
        while (runs.size() > 1) {
            List<Path> next = new ArrayList<>();
            for (int start = 0; start < runs.size(); start += FAN_IN) {
                List<Path> group = runs.subList(start, Math.min(start + FAN_IN, runs.size()));
                Path merged = Files.createTempFile(input.getParent(), "endpoint-merge-", ".bin");
                merge(group, merged);
                next.add(merged);
                for (Path path : group) Files.delete(path);
            }
            runs = next;
        }
        if (runs.isEmpty()) Files.createFile(output);
        else Files.move(runs.getFirst(), output);
        Files.delete(input);
    }

    private record Head(int value, int stream) {}

    private static void merge(List<Path> paths, Path output) throws IOException {
        List<DataInputStream> streams = new ArrayList<>();
        PriorityQueue<Head> heap = new PriorityQueue<>(Comparator.comparingInt(Head::value));
        try (DataOutputStream out = output(output)) {
            for (int i = 0; i < paths.size(); i++) {
                DataInputStream in = input(paths.get(i));
                streams.add(in);
                if (in.available() > 0) heap.add(new Head(in.readInt(), i));
            }
            int previous = -1;
            while (!heap.isEmpty()) {
                Head head = heap.remove();
                if (head.value() != previous) {
                    out.writeInt(head.value());
                    previous = head.value();
                }
                DataInputStream in = streams.get(head.stream());
                if (in.available() > 0) heap.add(new Head(in.readInt(), head.stream()));
            }
        } finally {
            IOException failure = null;
            for (DataInputStream stream : streams) {
                try { stream.close(); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            }
            if (failure != null) throw failure;
        }
    }

    static DataInputStream input(Path path) throws IOException {
        return new DataInputStream(new BufferedInputStream(Files.newInputStream(path), 64 * 1024));
    }

    static DataOutputStream output(Path path) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path), 64 * 1024));
    }
}
