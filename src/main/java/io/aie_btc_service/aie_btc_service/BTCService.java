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
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
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

        // TODO: replace these hardcoded values with values from JSON API
        // TODO: for testing, we'll use the public and private parts of these
        // as we like, later on they will all come from JSON API input
        ECKey giverKey  = new ECKey(Hex.decode("E5983C1611045D6AC4CD216F836A2D3CB094E71F6B407E19089819C1AC10CD14"), Hex.decode("03525B5A1FF10811AEF3A22651EC62BABB8C2613E5DECB8E912E3EDD4316EDA4CC"));
        ECKey takerKey  = new ECKey(Hex.decode("9CF387B3E3748AAFBBAEECF34FEEFEC1530E1776AB085CBD2CF6BF5AA8FB226C"), Hex.decode("028621D3223E6A9780C6FF8BBAC6C27E85FF99976726D8D80350C82B463671BEAE"));
        ECKey oracleKey = new ECKey(Hex.decode("9F5DB851AFD9352F3AC36DA95ED1DEB70622C33781D609C5B3D16CC503F9878B"), Hex.decode("0235D32F838D6E85088192D13F211BBA79E4C25B6EB376FA7961EA919B754CABF7"));

        Transaction t2s1 = getT2S1(giverKey.getPubKey(),
                                   takerKey.getPubKey(),
                                   oracleKey.getPubKey(),
                                   new BigInteger("100000000") // 1 BTC
                                   );

        BigInteger giverAmount = new BigInteger("98000000"); // 0.98 BTC

        Transaction t1 = new Transaction(netParams);
        Script t1OutputScript =
            ScriptBuilder.createOutputScript(giverKey.toAddress(netParams));
        t1.addOutput(giverAmount, t1OutputScript);

        TransactionOutput giverInput = t1.getOutput(0);

        slf4jLogger.info("giverInput:");
        slf4jLogger.info(giverInput.toString());

        TransactionAndSighash txAndSighash = getT2S2(t2s1,
                                                     giverInput,
                                                     giverKey.getPubKey(),
                                                     takerKey.getPubKey());

        // TODO: signing

        Transaction t3 = getTx3(txAndSighash.tx,
                                takerKey.toAddress(netParams));

        slf4jLogger.info("T2S1: ");
        slf4jLogger.info(t2s1.toString());

        slf4jLogger.info("T2S2: ");
        slf4jLogger.info(txAndSighash.tx.toString());

        slf4jLogger.info("T2S2 sighash: ");
        slf4jLogger.info(txAndSighash.sighash.toString());

        slf4jLogger.info("T3: ");
        slf4jLogger.info(t3.toString());

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

    public static TransactionAndSighash getT2S2(Transaction t2,
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
            throw new RuntimeException("More than one input in T2 step 2: " +
                                       firstInput);
        }
        if (outputCount != 1) {
            throw new RuntimeException("T2 does not have single output in" +
                                       "T2 step 2: " + outputCount);
        }

        /* Validations:
           1. t1Output is a "normal" pay-to-address output
           2. t1Output's address is from one of the pubkeys in t2's output
           3. when we have two inputs to t2, they are each for one of the
           t2 output pubkeys but NOT the for the same one
        */
        Script t1OutputScript = t1Output.getScriptPubKey();
        if (!t1OutputScript.isSentToAddress()) {
            throw new RuntimeException("t1Output is not " +
                                       "DUP HASH160 EQUALVERIFY CHECKSIG");
        }

        Address t1toAddress = t1OutputScript.getToAddress(netParams);

        /*
        Address giverToAddress =
            new ECKey(null, giverPubKey).toAddress(netParams);
        Address takerToAddress =
            new ECKey(null, takerPubKey).toAddress(netParams);

            if (t1toAddress.toString() == giverToAddress.toString()) {
            throw new RuntimeException("input is not from giver or taker " +
                                       t1toAddress + ", " +
                                       giverToAddress + ", " +
                                       takerToAddress);
         }
        */

        if (!firstInput) {
            Address secondInputToAddress =
                t2.getInput(0).getScriptSig().getToAddress(netParams);
            if (secondInputToAddress == t1toAddress)
                throw new RuntimeException("second input to T2 " +
                                           "cannot be same as first input");
        }
        checkMinimumAmount(t1Output.getValue(), "2"); // TODO: what multiple?
        t2.addInput(t1Output);
        t2.setLockTime(1398617867 + (86400 * 10));
        Sha256Hash sighash = t2.hashForSignature(inputCount,
                                                 t1OutputScript,
                                                 sigHashAll,
                                                 false);
        return new TransactionAndSighash(t2, sighash);
    }

    // Returns T3 unsigned
    public static Transaction getTx3(Transaction LockFundsTx,
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
        BigInteger mul = new BigInteger(dustMultiplier);
        BigInteger min = Transaction.MIN_NONDUST_OUTPUT.multiply(mul);
        if (1 != amount.compareTo(min)) {
            throw new RuntimeException("contract amount too low: " + amount);
        }
        return;
    }
}
