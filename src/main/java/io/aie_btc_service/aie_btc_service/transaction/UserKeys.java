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
    private final String userId;
    private final ECKey ecKey;
    private final Postgres postgres;

    private final byte[] privateKey;

    public UserKeys(final String userId, final String walletInput, final Postgres postgres) {
        this.userId = userId;
        this.ecKey = getECKeyFromWalletImportFormat(walletInput);
        this.privateKey = ecKey.getPrivKeyBytes();
        this.postgres = postgres;
    }

    public TransactionOutPoint getTransactionOutPoint() {
        return new TransactionOutPoint(netParams, getOpenOutput().index, getOpenOutput().getShaHash());
    }

    public TransactionInput getTransactionInput() {
        return new TransactionInput(netParams, null, new byte[]{}, getTransactionOutPoint());
    }

    public String getUserId() {
        return userId;
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

    private OpenOutput getOpenOutput() {
        return postgres.getOpenOutput(ecKey.toAddress(netParams).toString());
    }
}
