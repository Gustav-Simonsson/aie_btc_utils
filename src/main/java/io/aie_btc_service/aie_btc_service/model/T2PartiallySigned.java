package io.aie_btc_service.aie_btc_service.model;


import com.google.bitcoin.core.Transaction;

import javax.xml.bind.DatatypeConverter;

public class T2PartiallySigned {

    private String newT2Hash;
    private String t2RawPartiallySigned;


    public T2PartiallySigned(Transaction t) {
        this.newT2Hash = DatatypeConverter.printHexBinary(t.getHash().getBytes());
        this.t2RawPartiallySigned = DatatypeConverter.printHexBinary(t.bitcoinSerialize());
    }

    public T2PartiallySigned(String newT2Hash, String t2RawPartiallySigned) {
        this.newT2Hash = newT2Hash;
        this.t2RawPartiallySigned = t2RawPartiallySigned;
    }

    public String getNewT2Hash() {
        return newT2Hash;
    }

    public void setNewT2Hash(String newT2Hash) {
        this.newT2Hash = newT2Hash;
    }

    public String getT2RawPartiallySigned() {
        return t2RawPartiallySigned;
    }

    public void setT2RawPartiallySigned(String t2RawPartiallySigned) {
        this.t2RawPartiallySigned = t2RawPartiallySigned;
    }

    @Override
    public String toString() {
        return "T2PartiallySigned{" +
                "newT2Hash='" + newT2Hash + '\'' +
                ", t2RawPartiallySigned='" + t2RawPartiallySigned + '\'' +
                '}';
    }
}
