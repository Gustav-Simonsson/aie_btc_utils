package io.aie_btc_service.aie_btc_service.model;

import io.aie_btc_service.aie_btc_service.IncompleteT2WithHash;

import javax.xml.bind.DatatypeConverter;

public class IncompleteT2AResponse {

    public IncompleteT2AResponse(String t2SigHash, String t2SigHashFlag, String t2SigHashFlaganyonecanpay, String t2Raw) {
        this.t2SigHash = t2SigHash;
        this.t2SigHashFlag = t2SigHashFlag;
        this.t2SigHashFlaganyonecanpay = t2SigHashFlaganyonecanpay;
        this.t2Raw = t2Raw;
    }

    public IncompleteT2AResponse(IncompleteT2WithHash incompleteT2WithHash) {
        this(DatatypeConverter.printHexBinary(incompleteT2WithHash.getTxHash().getBytes()),
                "SIGHASH_ALL",
                "true",
                DatatypeConverter.printHexBinary(incompleteT2WithHash.getT2().bitcoinSerialize()));
    }

    private String t2SigHash;
    private String t2SigHashFlag;
    private String t2SigHashFlaganyonecanpay;
    private String t2Raw;

    public String getT2SigHash() {
        return t2SigHash;
    }

    public void setT2SigHash(String t2SigHash) {
        this.t2SigHash = t2SigHash;
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
}
