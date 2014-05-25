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
import com.google.bitcoin.core.ECKey.ECDSASignature;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredTransactionOutput;
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
import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
        ECKey myKey = getECKeyFromWalletImportFormat("92r2FtYSQcqQMgzoXs3AzDAtu7Q3hgXmRD2HpcDM7g7UgArcxq6");
        ECKey aliceKey = getECKeyFromWalletImportFormat("92pJFTW3srGK11RDeWkXqVv3H1MvWd2xeqkB8W2eWFaftsoRGNk");
        ECKey bobKey = getECKeyFromWalletImportFormat("92SL8DDiEpTiaqWHtHufG8vW2wpZkwSrL3796oUDV6yaWLM3qnB");

        slf4jLogger.info("myPubKey: " + DatatypeConverter.printHexBinary(myKey.getPubKey()));
        slf4jLogger.info("myAddress: " + myKey.toAddress(netParams));
        slf4jLogger.info("alicePubKey: " + DatatypeConverter.printHexBinary(aliceKey.getPubKey()));
        slf4jLogger.info("aliceAddress: " + aliceKey.toAddress(netParams));
        slf4jLogger.info("bobPubKey: " + DatatypeConverter.printHexBinary(bobKey.getPubKey()));
        slf4jLogger.info("bobAddress: " + bobKey.toAddress(netParams));

        /*
        // T2 Step A : "T2 without inputs, without signatures"
        Transaction t2 = getT2SA(aliceKey.getPubKey(), // giver
                                 bobKey.getPubKey(),   // taker
                                 myKey.getPubKey(),    // event key
                                 new BigInteger("279000000")); // total bet amount (what winner gets)

        // Add inputs to T2 and create hashes for alice and bob to sign
        Postgres pg = new Postgres("localhost","aie_bitcoin2", "aie_bitcoin", "aie_bitcoin");
        OpenOutput aliceOO = pg.getOpenOutput(aliceKey.toAddress(netParams).toString());
        OpenOutput bobOO   = pg.getOpenOutput(bobKey.toAddress(netParams).toString());

        TransactionOutPoint aliceT1OutPoint =
            new TransactionOutPoint(netParams, (long) aliceOO.index, new Sha256Hash(aliceOO.hash));
        TransactionOutPoint bobT1OutPoint =
            new TransactionOutPoint(netParams, (long) bobOO.index,   new Sha256Hash(bobOO.hash));

        TransactionInput aliceT1Input = new TransactionInput(netParams, null, new byte[]{}, aliceT1OutPoint);
        TransactionInput bobT1Input   = new TransactionInput(netParams, null, new byte[]{}, bobT1OutPoint);

        Sha256Hash t2HashForAlice = addInputToT2(t2, aliceT1Input, aliceOO.scriptbytes);
        Sha256Hash t2HashForBob   = addInputToT2(t2, bobT1Input,   bobOO.scriptbytes);

        // Signing, this will be done client-side in browser JS where privkey is available
        TransactionSignature aliceSignature = new TransactionSignature(aliceKey.sign(t2HashForAlice), sigHashAll, true);
        TransactionSignature bobSignature   = new TransactionSignature(bobKey.sign(t2HashForBob),     sigHashAll, true);

        // NOTE: only pubkey part of aliceKey / bobKey is used here - which we have available in backend
        t2.getInput(0).setScriptSig(ScriptBuilder.createInputScript(aliceSignature, aliceKey));
        t2.getInput(1).setScriptSig(ScriptBuilder.createInputScript(bobSignature,   bobKey));

        StoredTransactionOutput sout = new StoredTransactionOutput(t2.getHash(), t2.getOutput(0), 0, false);
        slf4jLogger.info("sout value: " + DatatypeConverter.printHexBinary(sout.getValue().toByteArray()));
        pg.insertOpenOutput(sout);

        // t2 has the multisig output and inputs from alice and bob
        t2.verify();
        slf4jLogger.info("aliceOO: " + aliceOO.value);
        slf4jLogger.info("bobOO: " + bobOO.value);
        slf4jLogger.info("t2: " + t2);
        slf4jLogger.info("t2 output scriptPubKeyBytes: " + DatatypeConverter.printHexBinary(t2.getOutput(0).getScriptBytes()));
        slf4jLogger.info("t2 serialized: " + DatatypeConverter.printHexBinary(t2.bitcoinSerialize()));

        //*/

        ///*
        // In normal operation, these will come from Postgres, here we hardcode them
        Sha256Hash t2Hash = new Sha256Hash("866cbe1e7ee2cc38b65f7584a72fcb60e0f41a3b15a0934a72c63c52bee48ef2");
        String t2MultisigOutputScriptBytesString = "52410457a6e187af6dcad28f678a92850610504aa64685b4d6f60cbc30c1a1407a0ce03df1d51102eb09aca7ca6df77c06fe3ef6054e2ee9dac7b5ac849f6e5c026b734104f37df2954632c965c828df1c09dcf70002861a981789d9df20600987dcb3975a7db3c2ad87b875d31103d7ffe43ded752cf1393a68053a5d0ec4061116ae553f4104b0024cc9260f4200147598408624a7b55839f75249da160a1906deaec006fdcb802b9a72d06f5e9ebc6cc108704e3789b8c515746022864e86457d96d8e116bf53ae";
        byte[] t2MultisigOutputScriptBytes = DatatypeConverter.parseHexBinary(t2MultisigOutputScriptBytesString);
        TransactionOutPoint t2MultisigOutPoint = new TransactionOutPoint(netParams, 0, t2Hash);
        TransactionInput t3Input = new TransactionInput(netParams, null, new byte[]{}, t2MultisigOutPoint);
        Transaction t3 = new Transaction(netParams);
        t3.addInput(t3Input);
        t3.addOutput(new BigInteger("270000000"), aliceKey.toAddress(netParams)); // big miner's fee for simplicity :)
        Script inputScript = new Script(t2MultisigOutputScriptBytes);
        Sha256Hash t3Sighash = t3.hashForSignature(0, inputScript, sigHashAll, false);
        TransactionSignature aliceSignature = new TransactionSignature(aliceKey.sign(t3Sighash), sigHashAll, false);
        TransactionSignature bobSignature   = new TransactionSignature(bobKey.sign(t3Sighash),   sigHashAll, false);
        List<TransactionSignature> signatures = ImmutableList.of(aliceSignature, bobSignature);
        t3.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures));
        t3.verify();
        slf4jLogger.info("t2hash: " + t2Hash);
        slf4jLogger.info("inputScript: " + inputScript);
        slf4jLogger.info("t3: " + t3);
        slf4jLogger.info("t3 bytes: " + DatatypeConverter.printHexBinary(t3.bitcoinSerialize()));
        /*
        Transaction spendTx = getP2PKTx(myKey.toAddress(netParams),
                                        bobKey.toAddress(netParams),
                                        120000000l,
                                        myKey);
        slf4jLogger.info("spendTx: " + spendTx);

        //*/
        // TODO: proper thread management, this is just for testing
        ///*
         BTCNode node = new BTCNode();
         new Thread(node).start();
         try { Thread.sleep(5000); } catch (InterruptedException e) { slf4jLogger.info("Interrupted :O..."); }

         ///*
         try {node.peerGroup.broadcastTransaction(t3).get(); }
         catch (ExecutionException ex) { slf4jLogger.info("uhoh :O..."); }
         catch (InterruptedException ex2) { slf4jLogger.info("uhoh :O..."); }
         //*/
        slf4jLogger.info("stopping...");
    }

    // Returns T2 without inputs and without signatures
    public static Transaction getT2SA(byte[] giverPubKey,
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

    public static Sha256Hash addInputToT2(Transaction t2,
                                          TransactionInput t1Output,
                                          byte[] t1OutputScriptBytes) {
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

        t2.addInput(t1Output);
        Sha256Hash sighash = t2.hashForSignature(inputCount, new Script(t1OutputScriptBytes), sigHashAll, true);
        return sighash;
    }

    // Assumes single output available as input, TODO: make work for multiple?
    // Assume there is enough amount in the output used as input
    // TODO: proper fee handling
    public static Transaction getP2PKTx(Address fromAddress,
                                        Address toAddress,
                                        long spendAmount,
                                        ECKey signKey) {
        Postgres pg = new Postgres("localhost","aie_bitcoin2", "aie_bitcoin", "aie_bitcoin");
        OpenOutput oo = pg.getOpenOutput(fromAddress.toString());

        TransactionOutPoint txOutPoint = new TransactionOutPoint(netParams, (long) oo.index, new Sha256Hash(oo.hash));
        TransactionInput spendInput = new TransactionInput(netParams, null, new byte[]{}, txOutPoint);

        Transaction spendTx = new Transaction(netParams);
        spendTx.addInput(spendInput);

        long amountAvailable = (long) (ByteBuffer.wrap(oo.value).getInt());
        long fee = 2 * Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.longValue();
        long change = amountAvailable - spendAmount - fee;

        spendTx.addOutput(BigInteger.valueOf(spendAmount), toAddress);
        spendTx.addOutput(BigInteger.valueOf(change), fromAddress);
         TransactionSignature txSign =
            spendTx.calculateSignature(0, signKey, new Script(oo.scriptbytes), sigHashAll, false);
        spendTx.getInput(0).setScriptSig(ScriptBuilder.createInputScript(txSign, signKey));
        spendTx.verify();
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
