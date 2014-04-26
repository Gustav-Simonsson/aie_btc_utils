package io.aie_btc_service.aie_btc_service;

import org.spongycastle.crypto.digests.ShortenedDigest;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class TransactionAndSighash {
    public final Transaction tx;
    public final Sha256Hash sighash;

    public TransactionAndSighash(Transaction t, Sha256Hash sh) {
        this.tx = t;
        this.sighash = sh;
    }
}
