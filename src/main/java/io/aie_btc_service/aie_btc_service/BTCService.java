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
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.collect.ImmutableList;

import io.aie_btc_service.aie_btc_service.model.T2PartiallySigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import static io.aie_btc_service.aie_btc_service.FullClient.NETWORK_PARAMETERS;

public class BTCService {
    private static final Logger Log = LoggerFactory.getLogger(BTCService.class);
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
            Log.info("-------------------------------------------------------");
            Log.info("oracleKey.address: " + oracleKey.toAddress(NETWORK_PARAMETERS));
            Log.info("oracleKey.pubkey: " + DatatypeConverter.printHexBinary(oracleKey.getPubKey()));
            Log.info("oracleKey.balance: " + FullClient.getBalanceForAddress(oracleKey.toAddress(NETWORK_PARAMETERS).toString()));
            Log.info("-------------------------");
            Log.info("aliceKey.address: " + aliceKey.toAddress(NETWORK_PARAMETERS));
            Log.info("aliceKey.pubkey: " + DatatypeConverter.printHexBinary((aliceKey.getPubKey())));
            Log.info("aliceKey.balance: " + FullClient.getBalanceForAddress(aliceKey.toAddress(NETWORK_PARAMETERS).toString()));
            Log.info("-------------------------");
            Log.info("bobKey.address: " + bobKey.toAddress(NETWORK_PARAMETERS));
            Log.info("bobKey.pubkey: " + DatatypeConverter.printHexBinary(bobKey.getPubKey()));
            Log.info("bobKey.balance: " + FullClient.getBalanceForAddress(bobKey.toAddress(NETWORK_PARAMETERS).toString()));
            Log.info("-------------------------------------------------------");


        } catch (BlockStoreException e) {
            e.printStackTrace();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }


        Log.info("myPubKey: " + DatatypeConverter.printHexBinary(oracleKey.getPubKey()));
        Log.info("myAddress: " + oracleKey.toAddress(NETWORK_PARAMETERS));
        Log.info("alicePubKey: " + DatatypeConverter.printHexBinary(aliceKey.getPubKey()));
        Log.info("aliceAddress: " + aliceKey.toAddress(NETWORK_PARAMETERS));
        Log.info("bobPubKey: " + DatatypeConverter.printHexBinary(bobKey.getPubKey()));
        Log.info("bobAddress: " + bobKey.toAddress(NETWORK_PARAMETERS));

        /* create T2 */
        CreateIncompleteT2A createIncompleteT2A = new CreateIncompleteT2A(oracleKey, aliceKey, bobKey, txForGiver, value).invoke();
        Transaction t2 = createIncompleteT2A.getT2();
        Sha256Hash t2HashForSigning = createIncompleteT2A.getT2HashForSigning();
        Sha256Hash t2HashForSigning2 = createIncompleteT2A.getT2HashForSigning2();

        Log.info(" t2HashForSigning: " + t2HashForSigning);
        Log.info("t2HashForSigning2: " + t2HashForSigning2);

        // t2 has the multisig output and inputs from alice and bob
        t2.verify();
//        Log.info("unsignedOO.value: " + DatatypeConverter.printHexBinary(aliceOO.value));
        Log.info("           t2: " + t2);
        Log.info("      t2.hash: " + t2.getHashAsString());
        Log.info("t2 output scriptPubKeyBytes: " + DatatypeConverter.printHexBinary(t2.getOutput(0).getScriptBytes()));
        Log.info("t2 serialized: " + DatatypeConverter.printHexBinary(t2.bitcoinSerialize()));

        return new IncompleteT2WithHash(t2, t2HashForSigning, t2HashForSigning2);
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
        Transaction contractTx = new Transaction(NETWORK_PARAMETERS);
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
        Postgres pg = new Postgres(FullClient.DB_HOST, FullClient.DB_NAME, FullClient.DB_USER, FullClient.DB_PASSWORD);
        OpenOutput oo = pg.getOpenOutput(fromAddress.toString());

        TransactionOutPoint txOutPoint = new TransactionOutPoint(NETWORK_PARAMETERS, (long) oo.index, new Sha256Hash(oo.hash));
        TransactionInput spendInput = new TransactionInput(NETWORK_PARAMETERS, null, new byte[]{}, txOutPoint);

        Transaction spendTx = new Transaction(NETWORK_PARAMETERS);
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

    public static ECKey getECKeyFromWalletImportFormat(String s) {
        ECKey key = new ECKey();
        try {
            key = (new DumpedPrivateKey(NETWORK_PARAMETERS, s)).getKey();
        }
        catch (AddressFormatException e) {
            Log.error("Caught AddressFormatException: ", e);
        }
        return key;
    }

    public T2PartiallySigned submitFirstT2Signature(String t2Signature, String t2Raw, String pubKey, boolean signForGiver) {


        int inputIndex = signForGiver ? 0 : 1;
        Log.info("inputIndex: " + inputIndex);
        Log.info("    pubKey: " + pubKey);
        //TODO:
        // 1. decode t2raw tx into byte[]
        byte[] t2Bytes = DatatypeConverter.parseHexBinary(t2Raw);

        // 2. create Transaction.java
        Transaction t2 = new Transaction(NETWORK_PARAMETERS, t2Bytes);
        Log.info("---> t2: " + t2);

        // 3. convert signature to ECDSASignature and then TransactionSignature.java type
        byte[] t2SignatureBytes = DatatypeConverter.parseHexBinary(t2Signature);
//        ECKey.ECDSASignature t2ECDSASignature = ECKey.ECDSASignature.decodeFromDER(t2SignatureBytes);
//        Log.info("t2ECDSASignature: " + t2ECDSASignature);

        TransactionSignature t2TransactionSignature = TransactionSignature.decodeFromBitcoin(t2SignatureBytes, true);
        Log.info("t2TransactionSignature.decodeFromBitcoin: " + DatatypeConverter.printHexBinary(t2TransactionSignature.encodeToBitcoin()));

        // 4. Decode pubkey
        // TODO: change from hex bin to base64 for more standarised serialisation
        byte[] pubkeyBytes = DatatypeConverter.parseHexBinary(pubKey);
        ECKey ecPubKey = new ECKey(null, pubkeyBytes, false);
        Log.info("ecPubKey: " + ecPubKey);

        Log.info("signForGiver: " + signForGiver);
        // (5. Apply signature to Transaction)
        // 6. Sign input 0 or 1
        t2.getInput(signForGiver ? 0 : 1).setScriptSig(ScriptBuilder.createInputScript(t2TransactionSignature, ecPubKey));

        Log.info(" t2.getHashAsString(): " + t2.getHashAsString());
        Log.info("t2.input.scriptSig(0): " + t2.getInput(0).getScriptSig());
        Log.info("t2.input.scriptSig(1): " + t2.getInput(1).getScriptSig());
        Log.info("         t2.serialize: " + DatatypeConverter.printHexBinary(t2.bitcoinSerialize()));


        //check signature is ok
        byte[] scriptSig = t2.getInput(inputIndex).getScriptBytes();
        byte[] hashForSignature = t2.hashForSignature(inputIndex, scriptSig, SigHash.ALL, true).getBytes();
        ECKey.ECDSASignature signature = new ECKey.ECDSASignature(t2TransactionSignature.r, t2TransactionSignature.s);
        boolean validSignature = ECKey.verify(hashForSignature, signature, pubkeyBytes);

        if (validSignature) {
            Log.info("-----> Yay");
        } else {
            Log.info("-> Nay");
        }

        return new T2PartiallySigned(t2);
    }

    private static class CreateIncompleteT2A {
        private ECKey oracleKey;
        private ECKey aliceKey;
        private ECKey bobKey;
        private Transaction t2;
        private OpenOutput unsignedAliceOO;
        private OpenOutput unsignedBobOO;
        private Sha256Hash t2HashForAlice;
        private Sha256Hash t2HashForBob;
        private BigInteger value;
        private boolean txForGiver;

        public CreateIncompleteT2A(ECKey oracleKey, ECKey aliceKey, ECKey bobKey, boolean txForGiver, BigInteger value) {
            this.oracleKey = oracleKey;
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

        public Sha256Hash getT2HashForSigning2() {

            return t2HashForBob;
        }
        public OpenOutput getUnsignedAliceOO() {
            return unsignedAliceOO;
        }

        public OpenOutput getUnsignedBobOO() {
            return unsignedBobOO;
        }

        public CreateIncompleteT2A invoke() {
            // T2 Step A : "T2 without inputs, without signatures"
            t2 = getT2SA(aliceKey.getPubKey(), // giver
                    bobKey.getPubKey(),   // taker
                    oracleKey.getPubKey(),    // event key
                    value);

            // Add inputs to T2 and create hashes for alice and bob to sign
            Postgres pg = new Postgres(FullClient.DB_HOST, FullClient.DB_NAME, FullClient.DB_USER, FullClient.DB_PASSWORD);

            unsignedAliceOO = pg.getOpenOutput(aliceKey.toAddress(NETWORK_PARAMETERS).toString());
            unsignedBobOO = pg.getOpenOutput(bobKey.toAddress(NETWORK_PARAMETERS).toString());

            Log.info("                    aliceKey: " + aliceKey);
            Log.info("             unsignedAliceOO: " + unsignedAliceOO);
            Log.info("       unsignedAliceOO.index: " + unsignedAliceOO.index);
            Log.info(" unsignedAliceOO.hash.length: " + unsignedAliceOO.hash.length);

            Log.info("                      bobKey: " + bobKey);
            Log.info("               unsignedBobOO: " + unsignedBobOO);
            Log.info("         unsignedBobOO.index: " + unsignedBobOO.index);
            Log.info("   unsignedBobOO.hash.length: " + unsignedBobOO.hash.length);

            TransactionOutPoint aliceT1OutPoint = new TransactionOutPoint(NETWORK_PARAMETERS, (long) unsignedAliceOO.index, new Sha256Hash(unsignedAliceOO.hash));
            TransactionOutPoint bobT1OutPoint = new TransactionOutPoint(NETWORK_PARAMETERS, (long) unsignedBobOO.index, new Sha256Hash(unsignedBobOO.hash));

            TransactionInput aliceT1Input = new TransactionInput(NETWORK_PARAMETERS, null, new byte[]{}, aliceT1OutPoint);
            TransactionInput bobT1Input = new TransactionInput(NETWORK_PARAMETERS, null, new byte[]{}, bobT1OutPoint);

            t2HashForAlice = addInputToT2(t2, aliceT1Input, unsignedAliceOO.scriptbytes);
            t2HashForBob = addInputToT2(t2, bobT1Input, unsignedBobOO.scriptbytes);

            Log.info("t2HashForAlice: " + t2HashForAlice);
            Log.info("  t2HashForBob: " + t2HashForBob);

            //TODO: move this function after the transaction broadcast is confirmed
            StoredTransactionOutput sout = new StoredTransactionOutput(t2.getHash(), t2.getOutput(0), 0, false);
            Log.info("sout value: " + DatatypeConverter.printHexBinary(sout.getValue().toByteArray()));
            pg.insertOpenOutput(sout);
            return this;
        }
    }
}
