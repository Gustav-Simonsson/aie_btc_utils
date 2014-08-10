package io.aie_btc_service.aie_btc_service.model;

public class NewT3 {

    private String newT3Hash;
    private String newT3Raw;
    private boolean t3Broadcasted;

    public NewT3(String newT3Hash, String newT3Raw, boolean t3Broadcasted) {
        this.newT3Hash = newT3Hash;
        this.newT3Raw = newT3Raw;
        this.t3Broadcasted = t3Broadcasted;
    }

    @Override
    public String toString() {
        return "NewT3{" +
                "newT3Hash='" + newT3Hash + '\'' +
                ", newT3Raw='" + newT3Raw + '\'' +
                ", t3Broadcasted=" + t3Broadcasted +
                '}';
    }
}
