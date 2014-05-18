package io.aie_btc_service.aie_btc_service.transaction;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.params.TestNet3Params;
import io.aie_btc_service.aie_btc_service.OpenOutput;
import io.aie_btc_service.aie_btc_service.Postgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserKeys {

    public static final Logger slf4jLogger = LoggerFactory.getLogger(UserKeys.class);
    public static final TestNet3Params netParams = new TestNet3Params();
    private final String id;
    private final ECKey ecKey;
    private final Postgres postgres;

    public UserKeys(final String id, final String walletInput, final Postgres postgres) {
        this.id = id;
        this.ecKey = getECKeyFromWalletImportFormat(walletInput);
        this.postgres = postgres;
    }

    public TransactionOutPoint getTransactionOutPoint() {
        return new TransactionOutPoint(netParams, getOpenOutput().index, getOpenOutput().getShaHash());
    }

    public String getId() {
        return id;
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public byte[] getPubKey() {
        return ecKey.getPubKey();
    }

    private static ECKey getECKeyFromWalletImportFormat(String s) {
        ECKey key = new ECKey();
        try {
            key = (new DumpedPrivateKey(netParams, s)).getKey();
        }
        catch (AddressFormatException e) {
            slf4jLogger.error("Caught AddressFormatException: ", e);
        }
        return key;
    }

    public OpenOutput getOpenOutput() {
        return postgres.getOpenOutput(ecKey.toAddress(netParams).toString());
    }
}
