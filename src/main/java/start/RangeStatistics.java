package start;

import dutils.GenomeType;
import dutils.Range;
import java.io.IOException;
import java.io.Writer;

/** One final BED interval. All depth sums use equivalent (2 x observed) depth. */
public record RangeStatistics(
        String configId, GenomeType genomeType, int rangeIndex, Range range, int gapLength,
        long sum56, long sum57, long q0Sum56, long q0Sum57,
        int bases1_56, int bases1_57, int bases5_56, int bases5_57,
        int bases10_56, int bases10_57, int bases5Joint, int bases10Joint,
        double baseline56, double baseline57) {

    public static final String HEADER = "config_id\tgenome_type\trange_index\tchr\tstart\tend\tlength_bp\tgap_bp"
            + "\tq20_equiv_sum_xp56\tq20_equiv_sum_xp57\tq0_equiv_sum_xp56\tq0_equiv_sum_xp57"
            + "\tm_xp56\tm_xp57\tz_xp56\tz_xp57\tB5_xp56\tB5_xp57\tB10_xp56\tB10_xp57"
            + "\tB5_joint\tB10_joint\tA\tT\tU_xp56\tU_xp57\tu\tu_valid\tB1_negative\tgap_fraction"
            + "\tq20_gt_q0_xp56\tq20_gt_q0_xp57\tu_gt1\n";
    private static final double EPSILON = 0.05;
    private static final double LOG_2 = Math.log(2.0);

    public RangeStatistics {
        if (!(Double.isFinite(baseline56) && baseline56 > 0
                && Double.isFinite(baseline57) && baseline57 > 0)) {
            throw new IllegalArgumentException("Normalization baselines must be finite and positive");
        }
        if (sum56 < 0 || sum57 < 0 || q0Sum56 < 0 || q0Sum57 < 0) {
            throw new IllegalArgumentException(configId + " " + range + ": negative depth sum");
        }
        if (gapLength < 0 || gapLength > range.length()) throw new IllegalArgumentException("Invalid gap length");
        requireSupported(genomeType);
    }

    static void requireSupported(GenomeType genomeType) {
        if (genomeType != GenomeType.AABB && genomeType != GenomeType.DD && genomeType != GenomeType.RR) {
            throw new IllegalArgumentException("Evaluation supports AB, D and R configurations, found " + genomeType);
        }
    }

    public boolean isAB() { return genomeType == GenomeType.AABB; }
    public int length() { return range.length(); }
    public double mean56() { return (double) sum56 / length(); }
    public double mean57() { return (double) sum57 / length(); }
    public double normalized56() { return mean56() / baseline56; }
    public double normalized57() { return mean57() / baseline57; }
    public double b5_56() { return (double) bases5_56 / length(); }
    public double b5_57() { return (double) bases5_57 / length(); }
    public double b10_56() { return (double) bases10_56 / length(); }
    public double b10_57() { return (double) bases10_57 / length(); }
    public double b5Joint() { return (double) bases5Joint / length(); }
    public double b10Joint() { return (double) bases10Joint / length(); }
    public double imbalance() {
        return isAB() ? Math.abs(Math.log((normalized56() + EPSILON) / (normalized57() + EPSILON)) / LOG_2) : Double.NaN;
    }
    public double advantage() {
        if (isAB()) return Double.NaN;
        double positive = genomeType == GenomeType.RR ? normalized56() : normalized57();
        double negative = genomeType == GenomeType.RR ? normalized57() : normalized56();
        return Math.log((positive + EPSILON) / (negative + EPSILON)) / LOG_2;
    }
    public double u56() { return q0Sum56 == 0 ? Double.NaN : (double) sum56 / q0Sum56; }
    public double u57() { return q0Sum57 == 0 ? Double.NaN : (double) sum57 / q0Sum57; }
    public double u() { return isAB() ? Math.min(u56(), u57()) : genomeType == GenomeType.RR ? u56() : u57(); }
    public boolean validU() { return Double.isFinite(u()); }
    public boolean q20ExceedsQ0_56() { return sum56 > q0Sum56; }
    public boolean q20ExceedsQ0_57() { return sum57 > q0Sum57; }
    public boolean uAboveOne() { return validU() && u() > 1; }
    public long y10() { return isAB() ? bases10Joint : genomeType == GenomeType.RR ? bases10_56 : bases10_57; }
    public long negativeBases1() { return genomeType == GenomeType.RR ? bases1_57 : genomeType == GenomeType.DD ? bases1_56 : 0; }
    public double negativeB1() { return isAB() ? Double.NaN : (double) negativeBases1() / length(); }
    public double gapFraction() { return (double) gapLength / length(); }

    public void writeTsv(Writer out) throws IOException {
        StringBuilder row = new StringBuilder(512);
        row.append(configId).append('\t').append(genomeType.getShortName()).append('\t').append(rangeIndex)
                .append('\t').append(range.chr()).append('\t').append(range.start()).append('\t').append(range.end())
                .append('\t').append(length()).append('\t').append(gapLength)
                .append('\t').append(sum56).append('\t').append(sum57).append('\t').append(q0Sum56).append('\t').append(q0Sum57);
        append(row, mean56(), mean57(), normalized56(), normalized57(), b5_56(), b5_57(), b10_56(), b10_57(),
                isAB() ? b5Joint() : Double.NaN, isAB() ? b10Joint() : Double.NaN,
                imbalance(), advantage(), u56(), u57(), u());
        row.append('\t').append(validU());
        append(row, negativeB1(), gapFraction());
        row.append('\t').append(q20ExceedsQ0_56()).append('\t').append(q20ExceedsQ0_57()).append('\t').append(uAboveOne());
        out.write(row.append('\n').toString());
    }

    static void append(StringBuilder row, double... values) {
        for (double value : values) row.append('\t').append(Double.isFinite(value) ? Double.toString(value) : "NA");
    }
}
