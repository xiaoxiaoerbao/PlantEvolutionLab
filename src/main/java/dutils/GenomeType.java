package dutils;

import java.util.EnumSet;

public enum GenomeType {
    AABBDD, AABB, AA, DD, RR, HH;

    public EnumSet<Subgenome> getGenomeTypes() {
        switch (this) {
            case AABBDD:
                return EnumSet.of(Subgenome.A, Subgenome.B, Subgenome.D);
            case AABB:
                return EnumSet.of(Subgenome.A, Subgenome.B);
            case AA:
                return EnumSet.of(Subgenome.A);
            case DD:
                return EnumSet.of(Subgenome.D);
            case RR:
                return EnumSet.of(Subgenome.R);
            case HH:
                return EnumSet.of(Subgenome.H);
            default:
                return EnumSet.noneOf(Subgenome.class);
        }

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
