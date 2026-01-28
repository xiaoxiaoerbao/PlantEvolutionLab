package dutils;

import java.util.EnumSet;

public enum Subgenome {
    A, B, D, R, H;

    public EnumSet<Chromosome> getSubgenomes() {
        switch (this) {
            case A:
                return EnumSet.of(Chromosome.A1, Chromosome.A2, Chromosome.A3, Chromosome.A4, Chromosome.A5, Chromosome.A6, Chromosome.A7);
            case B:
                return EnumSet.of(Chromosome.B1, Chromosome.B2, Chromosome.B3, Chromosome.B4, Chromosome.B5, Chromosome.B6, Chromosome.B7);
            case D:
                return EnumSet.of(Chromosome.D1, Chromosome.D2, Chromosome.D3, Chromosome.D4, Chromosome.D5, Chromosome.D6, Chromosome.D7);
            case R:
                return EnumSet.of(Chromosome.R1, Chromosome.R2, Chromosome.R3, Chromosome.R4, Chromosome.R5, Chromosome.R6, Chromosome.R7);
            case H:
                return EnumSet.of(Chromosome.H1, Chromosome.H2, Chromosome.H3, Chromosome.H4, Chromosome.H5, Chromosome.H6, Chromosome.H7);
            default:
                return EnumSet.noneOf(Chromosome.class);
        }
    }
}
