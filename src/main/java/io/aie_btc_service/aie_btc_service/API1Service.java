package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.aie_btc_service.aie_btc_service.model.IncompleteT2AResponse;

import io.aie_btc_service.aie_btc_service.model.T2PartiallySigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;

import static spark.Spark.get;


public class API1Service {

    public static final Logger Log = LoggerFactory.getLogger(API1Service.class);

    private static Gson gson;
    private static BTCService btcService = new BTCService();
    private static FullClient fullClient;

    public static void main(String[] args) throws Exception {
        init();

        get(new Route("/address-balance/:address") {
            @Override
            public Object handle(Request request, Response response) {
                BigInteger balance = new BigInteger("0");
                try {

                    Log.info("response: " + request.params("address"));
                    Log.info("response: " + response);

                    balance = fullClient.getBalanceForAddress(request.params("address"));

                } catch (Exception e) {
                    Log.error("Exception during processing: ", e);
                }

                return "" + balance;
            }
        });

        get(new Route("/get-unsigned-t2") {
            @Override
            public Object handle(Request request, Response response) {

                if (!checkQueryParameters(request, "giver-pubkey",
                        "taker-pubkey", "event-pubkey", "value")) {
                    return "ERROR: Parameter(s) missing";
                }
                response.header("Content-type", "text/plain");
                long value = Long.parseLong(request.queryParams("value"));

                IncompleteT2WithHash t2WithHash = btcService.createIncompleteT2WithHash(
                        request.queryParams("giver-pubkey"),
                        request.queryParams("taker-pubkey"),
                        request.queryParams("event-pubkey"),
                        new BigInteger("" + value));

                return gson.toJson(new IncompleteT2AResponse(t2WithHash));
            }

        });

        get(new Route("/submit-t2-signature") {
            @Override
            public Object handle(Request request, Response response) {


                if (!checkQueryParameters(request, "t2-signature", "t2-raw", "pubkey", "sign-for")) {
                    return "ERROR: Parameter(s) missing";
                }

                boolean signForGiver = "giver".equals(request.queryParams("sign-for"));

                Log.info("Working on: submit-first-t2-signature");
                T2PartiallySigned t2PartiallySigned = btcService.submitFirstT2Signature(
                        request.queryParams("t2-signature"),
                        request.queryParams("t2-raw"),
                        request.queryParams("pubkey"),
                        signForGiver);

                String t2String = t2PartiallySigned.getT2RawPartiallySigned();

                byte[] t2Bytes = DatatypeConverter.parseHexBinary(t2String);

                Transaction t2 = new Transaction(new TestNet3Params(), t2Bytes);

                if (!signForGiver) {
                    fullClient.broadcast(t2);
                }

                return gson.toJson(t2PartiallySigned);
            }

        });
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

    private static boolean checkQueryParameters(Request request, String... parameterNames) {

        for (String parameterName : parameterNames) {

            Log.info("request.params(parameterName): " + request.queryParams(parameterName));
            if (request.queryParams(parameterName) == null) {
                Log.error("Parameter missing: " + parameterName);
                return false;
            }
        }
        return true;
    }
}
