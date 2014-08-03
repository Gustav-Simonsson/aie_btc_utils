package io.aie_btc_service.aie_btc_service.tools;


import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.aie_btc_service.aie_btc_service.BTCService;
import io.aie_btc_service.aie_btc_service.model.IncompleteT2AResponse;
import io.aie_btc_service.aie_btc_service.model.T2PartiallySigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SignIncompleteT2 {

    private static final Logger Log = LoggerFactory.getLogger(SignIncompleteT2.class);
    private static Gson gson;

    public static void main(String[] args) throws Exception {

        String baseUrl = "" ;
        init();
        //Secretkey:
        // add harcoded testnet priv keys here for testing
//        ECKey oracleKey = BTCService.getECKeyFromWalletImportFormat("92r2FtYSQcqQMgzoXs3AzDAtu7Q3hgXmRD2HpcDM7g7UgArcxq6");
//        ECKey aliceKey = BTCService.getECKeyFromWalletImportFormat("92pJFTW3srGK11RDeWkXqVv3H1MvWd2xeqkB8W2eWFaftsoRGNk");
//        ECKey bobKey = BTCService.getECKeyFromWalletImportFormat("92SL8DDiEpTiaqWHtHufG8vW2wpZkwSrL3796oUDV6yaWLM3qnB");

        //TESTNET
//        ECKey oracleKey = BTCService.getECKeyFromWalletImportFormat("cUssvG7iDD4ZZeMzgVDMpqwgbXfiv9XZipKYxF73Qn9JqtideQw2");
//        ECKey aliceKey = BTCService.getECKeyFromWalletImportFormat("cQPEgupjVLWtsx3YU3ukqnZ2de9gmJLj67SYKqdTxwGcUZn6a31W");
//        ECKey bobKey = BTCService.getECKeyFromWalletImportFormat("cTTdqnanz2MuF2xbNuVkutVZERs9QryjJVtFhNPZkwe6fmUz4VCS");

//MAINNET
/*
11:05:51 30 .main: Oracle private Key: Kz3XP25cwgdpuWjz2ivbJbdrtrN5KLYGJX1hp4nF9Xaz8untnz11
11:05:51 30 .main: Oracle     address: 1CaSvnFKW6DnEGUJVDyZ7FvLBAqX3WQ8v9
11:05:51 30 .main: Alice (giver)  private Key: KyVTTGNzFwtxA7Kn8R9jEqqLWJLsGf1KkqtisfWWdaTZEV4Vbn3P
11:05:51 30 .main: Alice              address: 1GdoRxhKcgj95nujg188X5Z7YaytDjv8o2
11:05:51 30 .main: Bob (taker)  private Key: L1TCq6BgW3gyeuWQSNagoNUZWBQ3uGmgA9Ud2bFVddTAmW7qRYcE
11:05:51 30 .main: Bob              address: 13BpPWgkBQSh566i1iejtdMmErq7ENagqV
 */

        ECKey oracleKey = BTCService.getECKeyFromWalletImportFormat("Kz3XP25cwgdpuWjz2ivbJbdrtrN5KLYGJX1hp4nF9Xaz8untnz11");
        ECKey aliceKey = BTCService.getECKeyFromWalletImportFormat("KyVTTGNzFwtxA7Kn8R9jEqqLWJLsGf1KkqtisfWWdaTZEV4Vbn3P");
        ECKey bobKey = BTCService.getECKeyFromWalletImportFormat("L1TCq6BgW3gyeuWQSNagoNUZWBQ3uGmgA9Ud2bFVddTAmW7qRYcE");


        String giverPubkey = DatatypeConverter.printHexBinary(aliceKey.getPubKey());
        String takerPubKey = DatatypeConverter.printHexBinary(bobKey.getPubKey());
        String oraclePubKey = DatatypeConverter.printHexBinary(oracleKey.getPubKey());
        String ownerOfInputToSign = "giver";

        long value = 100000;

        //1. prepare request for getting unsigned T2A
        String url = "http://127.0.0.1:4567/get-unsigned-t2?" + String.format("giver-pubkey=%s&taker-pubkey=%s&event-pubkey=%s&value=%s&owner-of-input-to-sign=%s",
                URLEncoder.encode(giverPubkey),
                URLEncoder.encode(takerPubKey),
                URLEncoder.encode(oraclePubKey),
                URLEncoder.encode("" + value),
                URLEncoder.encode(ownerOfInputToSign)

        );
        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();

        Log.info("Url.get-incomplete-t2: " + url);

        //2. issue request
        client.connect();

        //3. Use result to  sign it
        IncompleteT2AResponse result = gson.fromJson(new InputStreamReader(client.getInputStream()), IncompleteT2AResponse.class);
        Log.info("Result.IncompleteT2AResponse: " + gson.toJson(result));

        //4. Sign the stuff
        Sha256Hash input0Hash = new Sha256Hash(result.getT2SigHashInput0());
        Sha256Hash input1Hash = new Sha256Hash(result.getT2SigHashInput1());

        ECKey.ECDSASignature aliceSignature = aliceKey.sign(input0Hash);
        ECKey.ECDSASignature bobSignature = bobKey.sign(input1Hash);

        Transaction.SigHash sigHash = Transaction.SigHash.ALL;
        TransactionSignature aliceTransactionSignature = new TransactionSignature(aliceSignature, sigHash, true);
        TransactionSignature bobTransactionSignature = new TransactionSignature(bobSignature, sigHash, true);

        //
        // Have the server sign it for the first time
        //

        //5. send signed result to API to /submit-first-t2-signature
        url = "http://127.0.0.1:4567/submit-first-t2-signature?" + String.format("t2-signature=%s&t2-raw=%s&pubkey=%s&sign-for=%s",
                DatatypeConverter.printHexBinary(aliceTransactionSignature.encodeToBitcoin()),
                URLEncoder.encode(result.getT2Raw()),
                giverPubkey,
                "giver"
        );
        client = (HttpURLConnection) new URL(url).openConnection();

        Log.info("Url.submit-first-t2-signature: " + url);

        //2. issue request
        client.connect();

        T2PartiallySigned t2PartiallySigned = gson.fromJson(new InputStreamReader(client.getInputStream()), T2PartiallySigned.class);
        Log.info("Result.T2PartiallySigned: " + gson.toJson(t2PartiallySigned));

        //
        // Sign second time
        //

        url = "http://127.0.0.1:4567/submit-first-t2-signature?" + String.format("t2-signature=%s&t2-raw=%s&pubkey=%s&sign-for=%s",
                DatatypeConverter.printHexBinary(bobTransactionSignature.encodeToBitcoin()),
                URLEncoder.encode(t2PartiallySigned.getT2RawPartiallySigned()),
                takerPubKey,
                "taker"
        );

        client = (HttpURLConnection) new URL(url).openConnection();

        Log.info("Url.submit-first-t2-signature: " + url);

        //2. issue request
        client.connect();

        t2PartiallySigned = gson.fromJson(new InputStreamReader(client.getInputStream()), T2PartiallySigned.class);
        Log.info("Result.T2Signed: " + gson.toJson(t2PartiallySigned));

    }

    private static void init() {
        BriefLogFormatter.init();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .setPrettyPrinting()
                .create();
    }

}

