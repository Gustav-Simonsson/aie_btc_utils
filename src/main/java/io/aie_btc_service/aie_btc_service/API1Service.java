package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.aie_btc_service.aie_btc_service.model.IncompleteT2AResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.math.BigInteger;

import static spark.Spark.get;


public class API1Service {

    public static final Logger Log = LoggerFactory.getLogger(API1Service.class);

    private static Gson gson;
    private static BTCService btcService = new BTCService();

    public static void main(String[] args) throws Exception {
        init();

        get(new Route("/get-incomplete-t2-A") {
            @Override
            public Object handle(Request request, Response response) {


                if (!checkQueryParameters(request, "giver-pubkey",
                        "taker-pubkey", "event-pubkey", "owner-of-input-to-sign", "value")) {
                    return "ERROR: Parameter(s) missing";
                }
                response.header("Content-type", "text/plain");
                long value = Long.parseLong(request.queryParams("value"));

                boolean txForGiver = "giver".equalsIgnoreCase(request.queryParams("owner-of-input-to-sign"));

                IncompleteT2WithHash t2WithHash = btcService.createIncompleteT2WithHash(
                        request.queryParams("giver-pubkey"),
                        request.queryParams("taker-pubkey"),
                        request.queryParams("event-pubkey"),
                        txForGiver,
                        new BigInteger("" + value));

                return gson.toJson(new IncompleteT2AResponse(t2WithHash));
            }

        });
    }

    private static void init() {
        BriefLogFormatter.init();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .setPrettyPrinting()
                .create();
        new FullClient().run();
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
