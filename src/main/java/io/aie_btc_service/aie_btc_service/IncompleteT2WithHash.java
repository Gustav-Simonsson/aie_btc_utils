package io.aie_btc_service.aie_btc_service;


import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class IncompleteT2WithHash {

    private Transaction t2;
    private Sha256Hash txHash;

    public IncompleteT2WithHash(Transaction t2, Sha256Hash txHash) {
        this.t2 = t2;
        this.txHash = txHash;
    }

    public Transaction getT2() {
        return t2;
    }

    public void setT2(Transaction t2) {
        this.t2 = t2;
    }

    public Sha256Hash getTxHash() {
        return txHash;
    }

    public void setTxHash(Sha256Hash txHash) {
        this.txHash = txHash;
    }
}
