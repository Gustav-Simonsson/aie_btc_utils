package io.aie_btc_service.aie_btc_service.tools;


import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.utils.BriefLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.aie_btc_service.aie_btc_service.FullClient.NETWORK_PARAMETERS;

public class CreateThreeWallets {

    private static final Logger Log = LoggerFactory.getLogger(CreateThreeWallets.class);

    public static void main(String[] args) {
        BriefLogFormatter.init();

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

    }

}
