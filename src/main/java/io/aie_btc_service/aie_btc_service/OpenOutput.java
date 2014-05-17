package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class OpenOutput {
    public final byte[] hash;
    public final int index;
    // public final int height;
    public final byte[] value;
    public final byte[] scriptbytes;
    //public final String toaddress;
    // public final int addresstargetable;

    public OpenOutput(byte[] hsh,
                      int i,
                      // int h,
                      byte[] v,
                      byte[] sb
                      // String toa,
                      // int at
                      ) {
        this.hash = hsh;
        this.index = i;
        //this.height = h;
        this.value = v;
        this.scriptbytes = sb;
        //this.toaddress = toa;
        //this.addresstargetable = at;
    }

    public Sha256Hash getShaHash() {
        return new Sha256Hash(hash);
    }
}