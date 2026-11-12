package start;

import dutils.GenomeType;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/** Configuration-level results; undefined and inapplicable floating-point values are NaN (TSV: NA). */
public record ConfigurationStatistics(
        String configId, GenomeType genomeType, int regionCount, long totalLength, long gapLength,
        long y10, double c10, double a90, double t10, double u10,
        int invalidUCount, double invalidUFraction, double negativeB1, double gapFraction,
        int q20ExceedsQ0Count56, int q20ExceedsQ0Count57, int uAboveOneCount) {
    public static final String HEADER = "config_id\tgenome_type\talpha\tbeta\tdelta\tgamma\tlambda\trho"
            + "\tn_regions\tL_total_bp\tL_gap_bp\tY10_joint_bp\tY10_positive_bp\tC10\tA90\tT10\tU10"
            + "\tn_invalid_U\tinvalid_U_fraction\tB1_negative\tG\tn_q20_gt_q0_xp56\tn_q20_gt_q0_xp57\tn_u_gt1\n";

    public void writeTsv(Writer out, Configurations input, int index) throws IOException {
        boolean ab = genomeType == GenomeType.AABB || genomeType == GenomeType.AA;
        StringBuilder row = new StringBuilder(256);
        row.append(configId).append('\t').append(genomeType.getShortName()).append('\t').append(input.alpha[index]);
        RangeStatistics.append(row, input.beta[index] < 0 ? Double.NaN : input.beta[index],
                input.delta[index] < 0 ? Double.NaN : input.delta[index]);
        row.append('\t').append(input.gamma[index]).append('\t').append(input.lambda[index]).append('\t').append(input.rho[index])
                .append('\t').append(regionCount).append('\t').append(totalLength).append('\t').append(gapLength)
                .append('\t').append(ab ? Long.toString(y10) : "NA").append('\t').append(ab ? "NA" : Long.toString(y10));
        RangeStatistics.append(row, c10, a90, t10, u10);
        row.append('\t').append(invalidUCount);
        RangeStatistics.append(row, invalidUFraction, negativeB1, gapFraction);
        row.append('\t').append(q20ExceedsQ0Count56).append('\t').append(q20ExceedsQ0Count57).append('\t').append(uAboveOneCount);
        out.write(row.append('\n').toString());
    }

    /** Only the current configuration's two primitive quantile arrays are kept. */
    static final class Accumulator {
        private final String configId;
        private final GenomeType type;
        private final double[] scores;
        private final double[] validUs;
        private int count;
        private int validUCount;
        private int q20ExceedsQ0Count56, q20ExceedsQ0Count57, uAboveOneCount;
        private long totalLength, gapLength, y10, negativeBases1;

        Accumulator(String configId, GenomeType type, int capacity) {
            this.configId = configId;
            this.type = type;
            scores = new double[capacity];
            validUs = new double[capacity];
        }

        void add(RangeStatistics range) {
            scores[count++] = range.isAB() ? range.imbalance() : range.advantage();
            if (range.validU()) validUs[validUCount++] = range.u();
            if (range.q20ExceedsQ0_56()) q20ExceedsQ0Count56++;
            if (range.q20ExceedsQ0_57()) q20ExceedsQ0Count57++;
            if (range.uAboveOne()) uAboveOneCount++;
            totalLength += range.length();
            gapLength += range.gapLength();
            y10 += range.y10();
            negativeBases1 += range.negativeBases1();
        }

        ConfigurationStatistics finish() {
            boolean ab = type == GenomeType.AABB || type == GenomeType.AA;
            Arrays.sort(scores, 0, count);
            Arrays.sort(validUs, 0, validUCount);
            return new ConfigurationStatistics(configId, type, count, totalLength, gapLength, y10,
                    ratio(y10, totalLength), ab ? percentile(scores, count, 0.9) : Double.NaN,
                    ab ? Double.NaN : percentile(scores, count, 0.1), percentile(validUs, validUCount, 0.1),
                    count - validUCount, ratio(count - validUCount, count),
                    ab ? Double.NaN : ratio(negativeBases1, totalLength), ratio(gapLength, totalLength),
                    q20ExceedsQ0Count56, q20ExceedsQ0Count57, uAboveOneCount);
        }
    }

    /** Type-7 quantile on sorted values: h = (n - 1) p, linearly interpolate adjacent ranks. */
    static double percentile(double[] sorted, int count, double probability) {
        if (count == 0) return Double.NaN;
        double rank = (count - 1) * probability;
        int left = (int) Math.floor(rank);
        int right = Math.min(left + 1, count - 1);
        return sorted[left] + (rank - left) * (sorted[right] - sorted[left]);
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? Double.NaN : (double) numerator / denominator;
    }
}
