package dutils;

import java.util.EnumSet;

public enum GenomeType {
    AABBDD("ABD"),
    AABB("AB"),
    AA("A"),
    DD("D"),
    RR("R"),
    HH("H");

    private final String shortName;

    GenomeType(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * Returns the genome type with the given short name (case-sensitive).
     *
     * @throws IllegalArgumentException if shortName is null or unknown
     */
    public static GenomeType fromShortName(String shortName) {
        for (GenomeType genomeType : values()) {
            if (genomeType.shortName.equals(shortName)) {
                return genomeType;
            }
        }
        throw new IllegalArgumentException("Unknown genome short name: " + shortName);
    }

    public EnumSet<Subgenome> getGenomeTypes() {
        return switch (this) {
            case AABBDD -> EnumSet.of(Subgenome.A, Subgenome.B, Subgenome.D);
            case AABB -> EnumSet.of(Subgenome.A, Subgenome.B);
            case AA -> EnumSet.of(Subgenome.A);
            case DD -> EnumSet.of(Subgenome.D);
            case RR -> EnumSet.of(Subgenome.R);
            case HH -> EnumSet.of(Subgenome.H);
            default -> EnumSet.noneOf(Subgenome.class);
        };

    }

    public EnumSet<Chromosome> getChromosomes() {
        EnumSet<Chromosome> chromosomeTypes = EnumSet.noneOf(Chromosome.class);
        EnumSet<Subgenome> genomeTypes = this.getGenomeTypes();
        for (Subgenome genomeType : genomeTypes) {
            chromosomeTypes.addAll(genomeType.getSubgenomes());
        }
        return chromosomeTypes;
    }
}
