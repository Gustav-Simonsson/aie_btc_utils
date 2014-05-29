package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredTransactionOutput;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

public class BTCService {
    private static final Logger Log = LoggerFactory.getLogger(BTCService.class);
    public static final TestNet3Params netParams = new TestNet3Params();
    public static final SigHash sigHashAll = Transaction.SigHash.ALL;

    public IncompleteT2WithHash createIncompleteT2WithHash(String giverKey, String takerKey,
                                                           String eventKey, boolean txForGiver, BigInteger value) {

        // add harcoded testnet priv keys here for testing
//        ECKey oracleKey = getECKeyFromWalletImportFormat("92r2FtYSQcqQMgzoXs3AzDAtu7Q3hgXmRD2HpcDM7g7UgArcxq6");
//        ECKey aliceKey = getECKeyFromWalletImportFormat("92pJFTW3srGK11RDeWkXqVv3H1MvWd2xeqkB8W2eWFaftsoRGNk");
//        ECKey bobKey = getECKeyFromWalletImportFormat("92SL8DDiEpTiaqWHtHufG8vW2wpZkwSrL3796oUDV6yaWLM3qnB");

        ECKey aliceKey = new ECKey(null, DatatypeConverter.parseHexBinary(giverKey));
        ECKey bobKey = new ECKey(null, DatatypeConverter.parseHexBinary(takerKey));
        ECKey oracleKey = new ECKey(null, DatatypeConverter.parseHexBinary(eventKey));


        try {

            Log.debug("oracleKey.address: " + oracleKey.toAddress(netParams));
            Log.debug("oracleKey.pubkey: " + DatatypeConverter.printHexBinary(oracleKey.getPubKey()));
            Log.debug("oracleKey.balance: " + FullClient.getBalanceForAddress(oracleKey.toAddress(netParams).toString()));

            Log.debug("aliceKey.address: " + aliceKey.toAddress(netParams));
            Log.debug("aliceKey.pubkey: " + DatatypeConverter.printHexBinary((aliceKey.getPubKey())));
            Log.debug("aliceKey.balance: " + FullClient.getBalanceForAddress(aliceKey.toAddress(netParams).toString()));

            Log.debug("bobKey.address: " + bobKey.toAddress(netParams));
            Log.debug("bobKey.pubkey: " + DatatypeConverter.printHexBinary(bobKey.getPubKey()));
            Log.debug("bobKey.balance: " + FullClient.getBalanceForAddress(bobKey.toAddress(netParams).toString()));


        } catch (BlockStoreException e) {
            e.printStackTrace();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }


        Log.info("myPubKey: " + DatatypeConverter.printHexBinary(oracleKey.getPubKey()));
        Log.info("myAddress: " + oracleKey.toAddress(netParams));
        Log.info("alicePubKey: " + DatatypeConverter.printHexBinary(aliceKey.getPubKey()));
        Log.info("aliceAddress: " + aliceKey.toAddress(netParams));
        Log.info("bobPubKey: " + DatatypeConverter.printHexBinary(bobKey.getPubKey()));
        Log.info("bobAddress: " + bobKey.toAddress(netParams));

        /* create T2 */
        CreateIncompleteT2A createIncompleteT2A = new CreateIncompleteT2A(oracleKey, aliceKey, bobKey, txForGiver, value).invoke();
        Transaction t2 = createIncompleteT2A.getT2();
        Sha256Hash t2HashForSigning = createIncompleteT2A.getT2HashForSigning();

//        OpenOutput aliceOO = createIncompleteT2A.getUnsignedOO();


        // t2 has the multisig output and inputs from alice and bob
        t2.verify();
//        Log.info("unsignedOO.value: " + DatatypeConverter.printHexBinary(aliceOO.value));
        Log.info("           t2: " + t2);
        Log.info("      t2.hash: " + t2.getHashAsString());
        Log.info("t2 output scriptPubKeyBytes: " + DatatypeConverter.printHexBinary(t2.getOutput(0).getScriptBytes()));
        Log.info("t2 serialized: " + DatatypeConverter.printHexBinary(t2.bitcoinSerialize()));

        return new IncompleteT2WithHash(t2, t2HashForSigning);
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
            Log.error("Caught AddressFormatException: ", e);
        }
        return key;
    }

    private static class CreateIncompleteT2A {
        private ECKey myKey;
        private ECKey aliceKey;
        private ECKey bobKey;
        private Transaction t2;
        private OpenOutput unsignedOO;
        private Sha256Hash t2HashForAlice;
        private BigInteger value;
        private boolean txForGiver;

        public CreateIncompleteT2A(ECKey myKey, ECKey aliceKey, ECKey bobKey, boolean txForGiver, BigInteger value) {
            this.myKey = myKey;
            this.aliceKey = aliceKey;
            this.bobKey = bobKey;
            this.value = value;
            this.txForGiver = txForGiver;
        }

        public Transaction getT2() {
            return t2;
        }

        public Sha256Hash getT2HashForSigning() {

            return t2HashForAlice;
        }

        public OpenOutput getUnsignedOO() {
            return unsignedOO;
        }

        public CreateIncompleteT2A invoke() {
            // T2 Step A : "T2 without inputs, without signatures"
            t2 = getT2SA(aliceKey.getPubKey(), // giver
                    bobKey.getPubKey(),   // taker
                    myKey.getPubKey(),    // event key
                    value);

            // Add inputs to T2 and create hashes for alice and bob to sign
            Postgres pg = new Postgres("localhost", "aie_bitcoin2", "biafra", "");

            if (txForGiver) {
                unsignedOO = pg.getOpenOutput(aliceKey.toAddress(netParams).toString());
            } else {
                unsignedOO = pg.getOpenOutput(bobKey.toAddress(netParams).toString());

            }
            Log.info("             unsignedOO: " + unsignedOO);
            Log.info("       unsignedOO.index: " + unsignedOO.index);
            Log.info(" unsignedOO.hash.length: " + unsignedOO.hash.length);
            TransactionOutPoint aliceT1OutPoint =
                    new TransactionOutPoint(netParams, (long) unsignedOO.index, new Sha256Hash(unsignedOO.hash));
            TransactionInput aliceT1Input = new TransactionInput(netParams, null, new byte[]{}, aliceT1OutPoint);

            t2HashForAlice = addInputToT2(t2, aliceT1Input, unsignedOO.scriptbytes);
            Log.debug("t2.getHashAsString(): " + t2.getHashAsString());
            Log.debug("        t2.serialize: " + DatatypeConverter.printHexBinary(t2.bitcoinSerialize()));

            StoredTransactionOutput sout = new StoredTransactionOutput(t2.getHash(), t2.getOutput(0), 0, false);
            Log.info("sout value: " + DatatypeConverter.printHexBinary(sout.getValue().toByteArray()));
            pg.insertOpenOutput(sout);
            return this;
        }
    }
}
