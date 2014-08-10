package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import io.aie_btc_service.aie_btc_service.model.IncompleteT2AResponse;

import io.aie_btc_service.aie_btc_service.model.IncompleteT3WithHash;
import io.aie_btc_service.aie_btc_service.model.T2PartiallySigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Properties;

import static io.aie_btc_service.aie_btc_service.FullClient.NETWORK_PARAMETERS;
import static spark.Spark.before;
import static spark.Spark.get;


public class API1Service {

    public static final Logger Log = LoggerFactory.getLogger(API1Service.class);
    public static final String PARAM_T2_RAW = "t2-raw";
    public static final String PARAM_SIGN_FOR = "sign-for";
    public static final String PARAM_SIGNER_PRIVKEY = "signer-privkey";
    public static final String PARAM_T2_SIGNATURE = "t2-signature";
    public static final String PARAM_PUBKEY = "pubkey";
    public static final String PARAM_VALUE_GIVER = "giver";
    public static final String PARAM_T2_SIG_HASH_INPUT0 = "t2-sig-hash-input0";

    private static Gson gson;
    private static BTCService btcService = new BTCService();
    private static FullClient fullClient;

    public static void main(String[] args) throws Exception {
        init();

        before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.type("text/plain");
            }
        });

        get(new Route("/address-balance/:address") {
            @Override
            public Object handle(Request request, Response response) {

                BigInteger balance = new BigInteger("0");
                try {

                    Log.info("response: " + request.params("address"));
                    Log.info("response: " + response);

                    balance = FullClient.getBalanceForAddress(request.params("address"));

                } catch (Exception e) {
                    Log.error("Exception during processing: ", e);
                }

                return "" + balance;
            }
        });

        get(new Route("/get-unsigned-t2") {
            @Override
            public Object handle(Request request, Response response) {

                try {
                    checkQueryParameters(request,
                            "giver-pubkey",
                            "taker-pubkey",
                            "event-pubkey",
                            "value");

                    long value = Long.parseLong(request.queryParams("value"));

                    IncompleteT2WithHash t2WithHash = btcService.createIncompleteT2WithHash(
                            request.queryParams("giver-pubkey"),
                            request.queryParams("taker-pubkey"),
                            request.queryParams("event-pubkey"),
                            new BigInteger("" + value));

                    return gson.toJson(new IncompleteT2AResponse(t2WithHash));
                } catch (Exception e) {
                    Log.error("Exception: ", e);
                    return renderStackTrace(e);
                }
            }

        });

        get(new Route("/submit-t2-signature") {
            @Override
            public Object handle(Request request, Response response) {

                try {

                    checkQueryParameters(request, PARAM_T2_SIGNATURE, PARAM_T2_RAW, PARAM_PUBKEY, PARAM_SIGN_FOR);
                    boolean signForGiver = PARAM_VALUE_GIVER.equals(request.queryParams(PARAM_SIGN_FOR));
                    Log.info("Working on: submit-first-t2-signature");

                    T2PartiallySigned t2PartiallySigned = btcService.submitFirstT2Signature(
                            request.queryParams(PARAM_T2_SIGNATURE),
                            request.queryParams(PARAM_T2_RAW),
                            request.queryParams(PARAM_PUBKEY),
                            signForGiver);

                    String t2String = t2PartiallySigned.getT2RawPartiallySigned();

                    byte[] t2Bytes = DatatypeConverter.parseHexBinary(t2String);

                    Transaction t2 = new Transaction(NETWORK_PARAMETERS, t2Bytes);

                    if (!signForGiver) {
                        Log.info("Not signed for giver");
                        fullClient.broadcast(t2);
                    }
                    return gson.toJson(t2PartiallySigned);

                } catch (Exception e) {
                    Log.error("Exception: ", e);
                    return renderStackTrace(e);
                }
            }

        });

        /**
         * This will call submit-t2-signature after creating t2-signature. pubkey will be derived from sec-key.
         * signfor is giver or taker
         */
        get(new Route("/sign-transaction") {
            @Override
            public Object handle(Request request, Response response) {
                Log.info("Working on: /sign-transaction");

                try {
                    checkQueryParameters(request, PARAM_T2_RAW, PARAM_SIGNER_PRIVKEY, PARAM_SIGN_FOR, PARAM_T2_SIG_HASH_INPUT0); //sign-for giver or taker
                    boolean signForGiver = PARAM_VALUE_GIVER.equals(request.queryParams(PARAM_SIGN_FOR));

                    String t2RawHexString = request.queryParams(PARAM_T2_RAW);
                    byte[] t2RawBytes = DatatypeConverter.parseHexBinary(t2RawHexString);
                    Transaction t2 = new Transaction(NETWORK_PARAMETERS, t2RawBytes);

                    String signerPrivkeyString = request.queryParams(PARAM_SIGNER_PRIVKEY);
                    byte[] signerPrivkeyBytes = DatatypeConverter.parseHexBinary(signerPrivkeyString);

                    ECKey signerKey = new ECKey(signerPrivkeyBytes, null);

                    String t2SigHash = request.queryParams(PARAM_T2_SIG_HASH_INPUT0);
                    byte[] t2SigHashBytes = DatatypeConverter.parseHexBinary(t2SigHash);

                    ECKey.ECDSASignature signature = btcService.signTransaction(signerKey, t2SigHashBytes);

                    TransactionSignature txSignature = new TransactionSignature(signature, Transaction.SigHash.ALL, true);
                    String t2SignatureHex = DatatypeConverter.printHexBinary(txSignature.encodeToBitcoin());

                    Log.info("     signature: " + signature);
                    Log.info("   txSignature: " + txSignature);
                    Log.info("t2SignatureHex: " + t2SignatureHex);

                    byte[] pubkey = signerKey.getPubKey();
                    String pubKeyHex = DatatypeConverter.printHexBinary(pubkey);

                    T2PartiallySigned t2PartiallySigned = btcService.submitFirstT2Signature(
                            t2SignatureHex,
                            t2RawHexString,
                            pubKeyHex,
                            signForGiver);

                    String t2String = t2PartiallySigned.getT2RawPartiallySigned();

                    if (!signForGiver) {
                        Log.info("Not signed for giver");
                        fullClient.broadcast(t2);
                    }
                    return gson.toJson(t2PartiallySigned);

                } catch (Exception e) {
                    Log.error("Exception: ", e);
                    return renderStackTrace(e);
                }
            }

        });

        get(new Route("/get-unsigned-t3") {
            @Override
            public Object handle(Request request, Response response) {
//                Log.info("handle()");
                try {

                    checkQueryParameters(request, "t2-hash", "to-address");
                    IncompleteT3WithHash t3WithHash = btcService.createUnsignedT3(
                            request.queryParams("t2-hash"),
                            request.queryParams("to-address"));

                    return gson.toJson(t3WithHash);
                } catch (Exception e) {
                    Log.error("Exception: ", e);
                    return renderStackTrace(e);
                }
            }

        });
    }

    private static String renderStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    private static void init() {
        BriefLogFormatter.init();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .setPrettyPrinting()
                .create();
        fullClient = new FullClient();
        fullClient.run();
    }

    /**
     * Throws
     *
     * @param request
     * @param parameterNames
     */
    private static void checkQueryParameters(Request request, String... parameterNames) {

        for (String parameterName : parameterNames) {

            Log.info("request.params(" + parameterName + "): " + request.queryParams(parameterName));
            if (request.queryParams(parameterName) == null) {
                Log.error("Parameter missing: " + parameterName);
                throw new RuntimeException("Parameter missing; " + parameterName);
            }
        }
    }
    public static Properties getProperties() {
        InputStream inputStream = API1Service.class.getClassLoader().getResourceAsStream("application.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

}
