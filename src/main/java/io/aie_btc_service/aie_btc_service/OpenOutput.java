package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

import java.util.Arrays;

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

    @Override
    public String toString() {
        return "OpenOutput{" +
                "hash=" + Arrays.toString(hash) +
                ", index=" + index +
                ", value=" + Arrays.toString(value) +
                ", scriptbytes=" + Arrays.toString(scriptbytes) +
                '}';
    }
}