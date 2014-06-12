package io.aie_btc_service.aie_btc_service;


import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncompleteT2WithHash {
    private static final Logger Log = LoggerFactory.getLogger(IncompleteT2WithHash.class);

    private Transaction t2;
    private Sha256Hash input0Hash;
    private Sha256Hash input1Hash;

    public IncompleteT2WithHash(Transaction t2, Sha256Hash input0Hash, Sha256Hash input1Hash) {
        this.t2 = t2;
        this.input0Hash = input0Hash;
        this.input1Hash = input1Hash;

        Log.info("input0Hash: " + input0Hash);
        Log.info("input1Hash: " + input1Hash);
    }

    public Transaction getT2() {
        return t2;
    }

    public void setT2(Transaction t2) {
        this.t2 = t2;
    }

    public Sha256Hash getInput0Hash() {
        return input0Hash;
    }

    public Sha256Hash getInput1Hash() {
        return input1Hash;
    }

    public void setInput0Hash(Sha256Hash input0Hash) {
        this.input0Hash = input0Hash;
    }
}
