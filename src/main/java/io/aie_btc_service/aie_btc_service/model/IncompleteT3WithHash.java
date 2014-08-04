package io.aie_btc_service.aie_btc_service.model;

public class IncompleteT3WithHash {

/*
"t3-sighash" : "A1EFFEC1CE0BABE" (ASCII HEX (if Henning reads this, he'll get the joke))
"t3-hash"    : "FFFF"
"t3-raw"     : "FFFF"
 */

    private String t3Sighash;
    private String t3Hash;
    private String t3Raw;

    public IncompleteT3WithHash(String t3Sighash, String t3Hash, String t3Raw) {
        this.t3Sighash = t3Sighash;
        this.t3Hash = t3Hash;
        this.t3Raw = t3Raw;
    }

    @Override
    public String toString() {
        return "IncompleteT3WithHash{" +
                "t3Sighash='" + t3Sighash + '\'' +
                ", t3Hash='" + t3Hash + '\'' +
                ", t3Raw='" + t3Raw + '\'' +
                '}';
    }

    public String getT3Sighash() {
        return t3Sighash;
    }

    public void setT3Sighash(String t3Sighash) {
        this.t3Sighash = t3Sighash;
    }

    public String getT3Hash() {
        return t3Hash;
    }

    public void setT3Hash(String t3Hash) {
        this.t3Hash = t3Hash;
    }

    public String getT3Raw() {
        return t3Raw;
    }

    public void setT3Raw(String t3Raw) {
        this.t3Raw = t3Raw;
    }
}
