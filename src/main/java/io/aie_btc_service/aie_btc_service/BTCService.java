package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;

import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;

import com.google.bitcoin.crypto.TransactionSignature;

import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet2Params;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.collect.ImmutableList;

import net.jcip.annotations.Immutable;

import org.h2.value.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.encoders.Hex;
import org.spongycastle.crypto.digests.ShortenedDigest;

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

import javax.xml.bind.DatatypeConverter;

public class BTCService {
    public static final Logger slf4jLogger =
        LoggerFactory.getLogger(BTCService.class);
    public static final TestNet3Params netParams = new TestNet3Params();
    public static final SigHash sigHashAll = Transaction.SigHash.ALL;

    public static void main(String[] args) {
        BriefLogFormatter.init();
        slf4jLogger.info("starting...");

        // add harcoded testnet priv keys here for testing
        ECKey aliceKey = getECKeyFromWalletImportFormat("");
        ECKey bobKey   = getECKeyFromWalletImportFormat("");

        slf4jLogger.info("alice address: " + aliceKey.toAddress(netParams));

        // TODO: proper thread management, this is just for testing
        new Thread(new BTCNode()).start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            slf4jLogger.info("Interrupted :O...");
        }

        slf4jLogger.info("stopping...");
    }

    // Returns T2 without inputs and without signatures
    public static Transaction getT2S1(byte[] giverPubKey,
                                      byte[] takerPubKey,
                                      byte[] eventPubKey,
                                      BigInteger SatoshiAmount
                                      ) {
        /* Validations:
           1. Each pubkey is a different one
           2. The amount in satoshis is sufficiently bigger than dust amount
         */
        checkMinimumAmount(SatoshiAmount, "10"); // TODO: what multiple?
        if ((giverPubKey == takerPubKey)  ||
            (giverPubKey == eventPubKey)  ||
            (takerPubKey == eventPubKey)) {
            throw new RuntimeException("giver, taker, event pubkeys " +
                                       "not unique: " +
                                       giverPubKey + ", " +
                                       takerPubKey + ", " +
                                       eventPubKey);
        }
        // NOTE: These ECKeys contain only the public part of the EC key pair.
        ECKey giverKey = new ECKey(null, giverPubKey);
        ECKey takerKey = new ECKey(null, takerPubKey);
        // NOTE: we have a single event key. Outcome is handled by encrypting
        // its private part two times - with 'yes' and 'no' pubkey of oracle
        ECKey eventKey = new ECKey(null, eventPubKey);
        List<ECKey> keys = ImmutableList.of(giverKey, takerKey, eventKey);
        // 2 out of 3 multisig script
        Script script = ScriptBuilder.createMultiSigOutputScript(2, keys);
        Transaction contractTx = new Transaction(netParams);
        // TODO: Add default miner fee?
        contractTx.addOutput(SatoshiAmount, script);
        return contractTx;
    }

    public static TransactionAndSighash addInputToT2(Transaction t2,
                                                     TransactionOutput t1Output,
                                                     byte[] giverPubKey,
                                                     byte[] takerPubKey
                                                     ) {
        // Add input from either giver or taker
        // one of them can already be present and signed
        int outputCount    = t2.getOutputs().size();
        int inputCount     = t2.getInputs().size();
        boolean firstInput = inputCount == 0;
        if (inputCount > 1) {
            // TODO: design proper exception classes
            throw new RuntimeException("More than one input in T2 step 2: " + firstInput);
        }
        if (outputCount != 1) {
            throw new RuntimeException("T2 does not have single output in T2 step 2: " + outputCount);
        }

        /* Validations:
           1. t1Output is a "normal" pay-to-address output
           2. t1Output's address is from one of the pubkeys in t2's output
           3. when we have two inputs to t2, they are each for one of the
           t2 output pubkeys but NOT the for the same one
        */
        Script t1OutputScript = t1Output.getScriptPubKey();
        if (!t1OutputScript.isSentToAddress()) {
            throw new RuntimeException("t1Output is not DUP HASH160 EQUALVERIFY CHECKSIG");
        }

        Address t1toAddress = t1OutputScript.getToAddress(netParams);

        if (!firstInput) {
            Script secondInputScript = new Script(t2.getInput(0).getConnectedOutput().getScriptBytes());
            Address secondInputToAddress = secondInputScript.getToAddress(netParams);
            if (secondInputToAddress == t1toAddress)
                throw new RuntimeException("second input to T2 cannot be same as first input");
        }
        checkMinimumAmount(t1Output.getValue(), "2"); // TODO: what multiple?
        t2.addInput(t1Output);
        t2.setLockTime(1398617867 + (86400 * 10));
        Sha256Hash sighash = t2.hashForSignature(inputCount, t1OutputScript, sigHashAll, false);
        return new TransactionAndSighash(t2, sighash);
    }

    // Returns T3 unsigned
    public static Transaction getT3(Transaction LockFundsTx,
                                    Address receiverAddress) {
        Transaction spendTx = new Transaction(netParams);
        TransactionOutput multisigOutput = LockFundsTx.getOutput(0);
        // TODO: validate output, amount
        // Script multisigScript = multisigOutput.getScriptPubKey();
        // TODO: Add default miner fee?
        BigInteger amount = multisigOutput.getValue();
        spendTx.addOutput(amount, receiverAddress);
        spendTx.addInput(multisigOutput);
        return spendTx;
    }

    private static void checkMinimumAmount(BigInteger amount,
                                           String dustMultiplier) {
        BigInteger min = Transaction.MIN_NONDUST_OUTPUT.multiply(new BigInteger(dustMultiplier));
        if (1 != amount.compareTo(min)) {
            throw new RuntimeException("contract amount too low: " + amount);
        }
        return;
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
}
