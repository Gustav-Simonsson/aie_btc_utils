package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.common.collect.ImmutableList;

import net.jcip.annotations.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class BTCService {
    public static final Logger slf4jLogger =
        LoggerFactory.getLogger(BTCService.class);
    public static final MainNetParams netParams = new MainNetParams();

    public static void main(String[] args) {
        slf4jLogger.info("starting...");

        // TODO: replace these hardcoded values with values from JSON API
        // TODO: for testing, we'll use the public and private parts of these
        // as we like, later on they will all come from JSON API input
        ECKey TestKey1 = new ECKey();
        ECKey TestKey2 = new ECKey();
        ECKey TestKey3 = new ECKey();

        Transaction partialContractTx =
            getContractFundsLockTx(TestKey1.getPubKey(),
                                   TestKey2.getPubKey(),
                                   TestKey3.getPubKey(),
                                   new BigInteger("100000000") // 1 BTC
                                   );

        // TODO: add test inputs to partialContractTx
        Transaction spendTx = getContractSpendTx(partialContractTx,
                                                 TestKey2.getPubKey());
        slf4jLogger.info("contractTx:");
        slf4jLogger.info(partialContractTx.toString());

        slf4jLogger.info("spendTx:");
        slf4jLogger.info(spendTx.toString());

        slf4jLogger.info("stopping...");
    }

    // Returns contract tx without inputs which are added by offerer later on
    public static Transaction getContractFundsLockTx(byte[] offererPubKeyBytes,
                                                     byte[] takerPubKeyBytes,
                                                     byte[] oraclePubKeyBytes,
                                                     BigInteger SatoshiAmount
                                                     ) {
        // NOTE: These ECKeys contain only the public part of the EC key pair.
        ECKey offererKey = new ECKey(null, offererPubKeyBytes);
        ECKey takerKey   = new ECKey(null, takerPubKeyBytes);
        ECKey oracleKey  = new ECKey(null, oraclePubKeyBytes);
        List<ECKey> keys = ImmutableList.of(offererKey, takerKey, oracleKey);
        // 2 out of 3 multisig script
        Script script = ScriptBuilder.createMultiSigOutputScript(2, keys);
        Transaction contractTx = new Transaction(netParams);
        contractTx.addOutput(SatoshiAmount, script);
        return contractTx;
    }

    public static Transaction getContractSpendTx(Transaction LockFundsTx,
                                                 byte[] receiverPubKeyBytes) {
        Transaction spendTx = new Transaction(netParams);
        ECKey receiverKey = new ECKey(null, receiverPubKeyBytes);
        TransactionOutput multisigOutput = LockFundsTx.getOutput(0);
        // TODO: validate output, amount
        // Script multisigScript = multisigOutput.getScriptPubKey();
        BigInteger amount = multisigOutput.getValue();
        spendTx.addOutput(amount, receiverKey);
        spendTx.addInput(multisigOutput);
        return spendTx;
    }
}

