package io.aie_btc_service.aie_btc_service.tools;


import com.google.gson.Gson;
import io.aie_btc_service.aie_btc_service.model.UnsignedT3Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class HandleT3 {
    private static final Logger Log = LoggerFactory.getLogger(SignIncompleteT2.class);

    //get unsigned t3 /get—unsigned-t3
    // params: "t2-hash", "to-address"
    // t2-hash: https://blockchain.info/tx/4E82E83E01ABED30257B5948251C27576A8D6EDAC30953E9B95095BF751F1E94

    public static void main(String[] args) throws Exception {
        Gson gson = SignIncompleteT2.initGson();

        String t2HashHexString = "4E82E83E01ABED30257B5948251C27576A8D6EDAC30953E9B95095BF751F1E94";
        String address = "1GdoRxhKcgj95nujg188X5Z7YaytDjv8o2"; //Alice A1

        String url = SignIncompleteT2.BASE_URL + "get—unsigned-t3?" + String.format("addres=%s&t2-raw=%s",
                URLEncoder.encode(address),
                URLEncoder.encode(t2HashHexString)
        );

        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();
        Log.info("Url.get-incomplete-t2: " + url);

        //2. issue request
        client.connect();

        UnsignedT3Response result = gson.fromJson(new InputStreamReader(client.getInputStream()), UnsignedT3Response.class);
        Log.info("Result.IncompleteT2AResponse: " + gson.toJson(result));

    }
}
