package dutils;

public enum Chromosome {

    A1("1A",1),
    A2("2A",2),
    A3("3A",3),
    A4("4A",4),
    A5("5A",5),
    A6("6A",6),
    A7("7A",7),
    B1("1B",1),
    B2("2B",2),
    B3("3B",3),
    B4("4B",4),
    B5("5B",5),
    B6("6B",6),
    B7("7B",7),
    D1("1D",1),
    D2("2D",2),
    D3("3D",3),
    D4("4D",4),
    D5("5D",5),
    D6("6D",6),
    D7("7D",7),
    R1("1R",1),
    R2("2R",2),
    R3("3R",3),
    R4("4R",4),
    R5("5R",5),
    R6("6R",6),
    R7("7R",7),
    H1("1H",1),
    H2("2H",2),
    H3("3H",3),
    H4("4H",4),
    H5("5H",5),
    H6("6H",6),
    H7("7H",7);

    private final String name;
    private final int chrID;

    Chromosome(String name,int chrID) {
        this.name = name;
        this.chrID = chrID;
    }

    public String getName() {
        return name;
    }

    public int getChrID() {
        return chrID;
    }
}
