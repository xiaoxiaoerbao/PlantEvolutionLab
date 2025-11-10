package maf;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum Species {

    AS("As", "Aegilops speltoides", "Speltoid goatgrass"),
    ASH("Ash", "Aegilops sharonensis", "Sharon goatgrass"),
    AT("At", "Aegilops tauschii", "Tausch’s goatgrass"),
    BD("Bd", "Brachypodium distachyon", "Stiff brome"),
    HV("Hv", "Hordeum vulgare", "Barley"),
    SC("Sc", "Secale cereale", "Rye"),
    TAV1("Tav1", "Triticum aestivum", "Bread wheat"),
    TAV2("Tav2", "Triticum aestivum", "Bread wheat"),
    TE("Te", "Thinopyrum elongatum", "Tall wheatgrass"),
    TM("Tm", "Triticum monococcum", "Einkorn wheat"),
    TT("Tt", "Triticum turgidum", "Durum wheat");

    final String speciesNameID;
    final String speciesName;
    final String commonName;

    Species(String speciesNameID, String speciesName, String commonName) {
        this.speciesNameID = speciesNameID;
        this.speciesName=speciesName;
        this.commonName=commonName;
    }

    public String getSpeciesNameID() {
        return speciesNameID;
    }

    public String getSpeciesName(){
        return speciesName;
    }

    public String getCommonName() {
        return commonName;
    }

    private static final Map<String,Species> speciesNameID2SpeciesMap = Arrays.stream(values()).collect(Collectors.toMap(Species::getSpeciesNameID, e->e));

     public static Species getInstanceFromStr(String speciesNameID){
         assert speciesNameID2SpeciesMap.containsKey(speciesNameID) : "speciesNameID not found: " + speciesNameID;
         return speciesNameID2SpeciesMap.get(speciesNameID);
    }

}
