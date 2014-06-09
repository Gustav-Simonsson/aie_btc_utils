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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SignIncompleteT2 {

    private static final Logger Log = LoggerFactory.getLogger(SignIncompleteT2.class);
    private static Gson gson;

    public static void main(String[] args) throws Exception {

        init();
        //Secretkey:
        // add harcoded testnet priv keys here for testing
        ECKey oracleKey = BTCService.getECKeyFromWalletImportFormat("92r2FtYSQcqQMgzoXs3AzDAtu7Q3hgXmRD2HpcDM7g7UgArcxq6");
        ECKey aliceKey = BTCService.getECKeyFromWalletImportFormat("92pJFTW3srGK11RDeWkXqVv3H1MvWd2xeqkB8W2eWFaftsoRGNk");
        ECKey bobKey = BTCService.getECKeyFromWalletImportFormat("92SL8DDiEpTiaqWHtHufG8vW2wpZkwSrL3796oUDV6yaWLM3qnB");

        String giverPubkey = DatatypeConverter.printHexBinary(aliceKey.getPubKey());
        String takerPubKey = DatatypeConverter.printHexBinary(bobKey.getPubKey());
        String oraclePubKey = DatatypeConverter.printHexBinary(oracleKey.getPubKey());
        String ownerOfInputToSign = "giver";
        long value = 100000;

        //1. prepare request for getting unsigend T2A
        String query = String.format("giver-pubkey=%s&taker-pubkey=%s&event-pubkey=%s&value=%s&owner-of-input-to-sign=%s",
                URLEncoder.encode(giverPubkey),
                URLEncoder.encode(takerPubKey),
                URLEncoder.encode(oraclePubKey),
                URLEncoder.encode("" + value),
                URLEncoder.encode(ownerOfInputToSign)

        );
        String url = "http://127.0.0.1:4567/get-incomplete-t2-A?" + query;
        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();

        Log.info("Url.get-incomplete-t2-A: " + url);

        //2. issue request
        client.connect();

        //3. Use result to  sign it
        IncompleteT2AResponse result = gson.fromJson(new InputStreamReader(client.getInputStream()), IncompleteT2AResponse.class);
        Log.info("Result.IncompleteT2AResponse: " + gson.toJson(result));

        //4. Sign the stuff
        Sha256Hash hash = new Sha256Hash(result.getT2SigHash());
        ECKey.ECDSASignature signature = aliceKey.sign(hash);

        Transaction.SigHash sigHash = Transaction.SigHash.ALL;
        TransactionSignature foo = new TransactionSignature(signature, sigHash, true);

        //5. send signed result to API to /submit-first-t2-signature
        query = String.format("t2-signature=%s&t2-raw=%s&pubkey=%s&sign-for=%s",
                URLEncoder.encode(DatatypeConverter.printHexBinary(foo.encodeToBitcoin())),
                URLEncoder.encode(result.getT2Raw()),
                URLEncoder.encode(DatatypeConverter.printHexBinary(giverPubkey.getBytes())),
                "giver"
        );

        url = "http://127.0.0.1:4567/submit-first-t2-signature?" + query;
        client = (HttpURLConnection) new URL(url).openConnection();

        Log.info("Url.submit-first-t2-signature: " + url);

        //2. issue request
        client.connect();

        T2PartiallySigned t2PartiallySigned = gson.fromJson(new InputStreamReader(client.getInputStream()), T2PartiallySigned.class);
        Log.info("Result.T2PartiallySigned: " + gson.toJson(t2PartiallySigned));

    }

    private static void init() {
        BriefLogFormatter.init();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .setPrettyPrinting()
                .create();
    }

}

