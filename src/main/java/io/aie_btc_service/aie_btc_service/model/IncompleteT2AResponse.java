package io.aie_btc_service.aie_btc_service.model;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.MainNetParams;
import io.aie_btc_service.aie_btc_service.IncompleteT2WithHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;

import static io.aie_btc_service.aie_btc_service.FullClient.NETWORK_PARAMETERS;

public class IncompleteT2AResponse {

    private static final Logger Log = LoggerFactory.getLogger(IncompleteT2AResponse.class);

    public IncompleteT2AResponse(Sha256Hash t2SigHashInput0, Sha256Hash t2SigHashInput1, String t2SigHashFlag, String t2SigHashFlaganyonecanpay, String t2Raw) {
        if (t2SigHashInput0 != null) {
            this.t2SigHashInput0 = DatatypeConverter.printHexBinary(t2SigHashInput0.getBytes());
        } else {
            this.t2SigHashInput0 = null;
        }
        if (t2SigHashInput1 != null) {
            this.t2SigHashInput1 = DatatypeConverter.printHexBinary(t2SigHashInput1.getBytes());
        } else {
            this.t2SigHashInput1 = null;
        }
        Log.info("t2SigHashInput0: " + t2SigHashInput0);
        Log.info("t2SigHashInput1: " + t2SigHashInput1);

        this.t2SigHashFlag = t2SigHashFlag;
        this.t2SigHashFlaganyonecanpay = t2SigHashFlaganyonecanpay;
        this.t2Raw = t2Raw;
        this.t2Hash = getT2Hash(t2Raw);

        Log.info("t2Hash: " + t2Hash);
    }

    private String getT2Hash(String t2Raw) {

        byte[] t2RawBytes = DatatypeConverter.parseHexBinary(t2Raw);
        Log.info("t2Raw: " + t2Raw);
        Transaction t2 = new Transaction(NETWORK_PARAMETERS, t2RawBytes);
        return t2.getHashAsString();
    }

    public IncompleteT2AResponse(IncompleteT2WithHash incompleteT2WithHash) {
        this(incompleteT2WithHash.getInput0Hash(), incompleteT2WithHash.getInput1Hash(),
                "SIGHASH_ALL",
                "true",
                DatatypeConverter.printHexBinary(incompleteT2WithHash.getT2().bitcoinSerialize()));
    }

    private String t2SigHashInput0;
    private String t2SigHashInput1;
    private String t2SigHashFlag;
    private String t2SigHashFlaganyonecanpay;
    private String t2Raw;
    private String t2Hash;

    public String getT2SigHashInput0() {
        return t2SigHashInput0;
    }


    public String getT2SigHashInput1() {
        return t2SigHashInput1;
    }

    public void setT2SigHashInput0(String t2SigHashInput0) {
        this.t2SigHashInput0 = t2SigHashInput0;
    }

    public String getT2SigHashFlag() {
        return t2SigHashFlag;
    }

    public void setT2SigHashFlag(String t2SigHashFlag) {
        this.t2SigHashFlag = t2SigHashFlag;
    }

    public String getT2SigHashFlaganyonecanpay() {
        return t2SigHashFlaganyonecanpay;
    }

    public void setT2SigHashFlaganyonecanpay(String t2SigHashFlaganyonecanpay) {
        this.t2SigHashFlaganyonecanpay = t2SigHashFlaganyonecanpay;
    }

    public String getT2Raw() {
        return t2Raw;
    }

    public void setT2Raw(String t2Raw) {
        this.t2Raw = t2Raw;
    }

    @Override
    public String toString() {
        return "IncompleteT2AResponse{" +
                "t2SigHashInput0='" + t2SigHashInput0 + '\'' +
                ", t2SigHashInput1='" + t2SigHashInput1 + '\'' +
                ", t2SigHashFlag='" + t2SigHashFlag + '\'' +
                ", t2SigHashFlaganyonecanpay='" + t2SigHashFlaganyonecanpay + '\'' +
                ", t2Raw='" + t2Raw + '\'' +
                '}';
    }
}
