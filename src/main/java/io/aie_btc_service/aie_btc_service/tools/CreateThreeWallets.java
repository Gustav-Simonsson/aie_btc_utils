package io.aie_btc_service.aie_btc_service.tools;


import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.aie_btc_service.aie_btc_service.FullClient.NETWORK_PARAMETERS;

public class CreateThreeWallets {

    private static final Logger Log = LoggerFactory.getLogger(CreateThreeWallets.class);

    public static void main(String[] args) {
        new CreateThreeWallets().run();
    }

    public void run() {
        BriefLogFormatter.init();

        Properties properties = getProperties();

        String networkParameterProperty = properties.getProperty("bitcoin.network");

        if ("main".equals(networkParameterProperty)) {
            NETWORK_PARAMETERS = new MainNetParams();
        } else if ("test".equals(networkParameterProperty)) {
            NETWORK_PARAMETERS = new TestNet3Params();
        } else {
            Log.info("Please define bitcoin.network property");
            System.exit(110);
        }

        ECKey oracleKey = new ECKey();
        Log.info("Oracle private Key: " + oracleKey.getPrivateKeyEncoded(NETWORK_PARAMETERS));
        Log.info("Oracle     address: " + oracleKey.toAddress(NETWORK_PARAMETERS));

        ECKey aliceKey = new ECKey();
        Log.info("Alice (giver)  private Key: " + aliceKey.getPrivateKeyEncoded(NETWORK_PARAMETERS));
        Log.info("Alice              address: " + aliceKey.toAddress(NETWORK_PARAMETERS));

        ECKey bobKey = new ECKey();
        Log.info("Bob (taker)  private Key: " + bobKey.getPrivateKeyEncoded(NETWORK_PARAMETERS));
        Log.info("Bob              address: " + bobKey.toAddress(NETWORK_PARAMETERS));
/* TESTNET!!!
08:48:56 29 CreateThreeWallets.main: Oracle private Key: cUssvG7iDD4ZZeMzgVDMpqwgbXfiv9XZipKYxF73Qn9JqtideQw2
08:48:56 29 CreateThreeWallets.main: Oracle     address: mjwtbiPTV23b61VPPw3dxAM689iimbmnH9
08:48:56 29 CreateThreeWallets.main: Alice (giver)  private Key: cQPEgupjVLWtsx3YU3ukqnZ2de9gmJLj67SYKqdTxwGcUZn6a31W
08:48:56 29 CreateThreeWallets.main: Alice              address: n2p9hhptX5wnM7FdHjARxkY4eGateS8HCq - 20 mBTC
08:48:56 29 CreateThreeWallets.main: Bob (taker)  private Key: cTTdqnanz2MuF2xbNuVkutVZERs9QryjJVtFhNPZkwe6fmUz4VCS
08:48:56 29 CreateThreeWallets.main: Bob              address: msUM5rDq1HihtvZh2EnB7vQCx6oFUYFj7h   - 10 mBTC
 */
/*
11:05:51 30 .main: Oracle private Key: Kz3XP25cwgdpuWjz2ivbJbdrtrN5KLYGJX1hp4nF9Xaz8untnz11
11:05:51 30 .main: Oracle     address: 1CaSvnFKW6DnEGUJVDyZ7FvLBAqX3WQ8v9
11:05:51 30 .main: Alice (giver)  private Key: KyVTTGNzFwtxA7Kn8R9jEqqLWJLsGf1KkqtisfWWdaTZEV4Vbn3P
11:05:51 30 .main: Alice              address: 1GdoRxhKcgj95nujg188X5Z7YaytDjv8o2
11:05:51 30 .main: Bob (taker)  private Key: L1TCq6BgW3gyeuWQSNagoNUZWBQ3uGmgA9Ud2bFVddTAmW7qRYcE
11:05:51 30 .main: Bob              address: 13BpPWgkBQSh566i1iejtdMmErq7ENagqV
 */

        /*
13:52:36,727 INFO  [CreateThreeWallets] - Oracle private Key: L4c57HvgacBHYW91KdMpFLt3PL45AfPS25m95pp3PzxQR527F36X
13:52:36,733 INFO  [CreateThreeWallets] - Oracle     address: 15fkF5G8GNmLp8WPtoWSYPDpKpGmFoMocT
13:52:36,767 INFO  [CreateThreeWallets] - Alice (giver)  private Key: L3WcabMX9KNWkaPFPSKuMX8gBkakw5cJB1nHKm7r9gf915MHqM1e
13:52:36,767 INFO  [CreateThreeWallets] - Alice              address: 1AqNuhoLJd1enGHAycoWTy6ReHBwuPB4ZN
13:52:36,797 INFO  [CreateThreeWallets] - Bob (taker)  private Key: L5o9x3A8xV4cbA6mih41RJng8VFwEcKWA9jq5Fr9HzW8VH7knTsM
13:52:36,798 INFO  [CreateThreeWallets] - Bob              address: 13Suv8AVFctBWWDjsVkRVZokdYjmfVZtbD
         */

    }

    private Properties getProperties() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
