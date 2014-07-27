package io.aie_btc_service.aie_btc_service;


import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.PostgresFullPrunedBlockStore;
import com.google.bitcoin.utils.BriefLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class FullClient {

    public static final Logger Log = LoggerFactory.getLogger(FullClient.class);
    public static NetworkParameters NETWORK_PARAMETERS;
    //    public final static NetworkParameters NETWORK_PARAMETERS = new TestNet3Params();
    public static String DB_HOST;
    public static String DB_NAME;
    public static String DB_USER;
    public static String DB_PASSWORD;
    public static boolean BITCOIN_RUNSCRIPTS;
    private static boolean BITCOIN_NETWORK_LOCALHOST;
    private static PostgresFullPrunedBlockStore blockStore;

    public static void main(String[] args) {
        new FullClient().run();
    }

    private static PeerGroup peerGroup;

    public void run() {

        BriefLogFormatter.initVerbose();

        Properties properties = getProperties();

        DB_HOST = properties.getProperty("db.host");
        DB_NAME = properties.getProperty("db.name");
        DB_USER = properties.getProperty("db.username");
        DB_PASSWORD = properties.getProperty("db.password");
        BITCOIN_RUNSCRIPTS = "true".equals(properties.getProperty("bitcoin.runscripts"));
        BITCOIN_NETWORK_LOCALHOST = "localhost".equals(properties.getProperty("bitcoin.network.host"));

        String networkParameterProperty = properties.getProperty("bitcoin.network");

        if ("main".equals(networkParameterProperty)) {
            NETWORK_PARAMETERS = new MainNetParams();
        } else if ("test".equals(networkParameterProperty)) {
            NETWORK_PARAMETERS = new TestNet3Params();
        } else {
            Log.info("Please define bitcoin.network property");
            System.exit(110);
        }
        Log.info("DB_NAME: " + DB_NAME);
        Log.info("DB_USER: " + DB_USER);
        Log.info("DB_HOST: " + DB_HOST);
        Log.info("NETWORK_PARAMETERS.getId()  : " + NETWORK_PARAMETERS.getId());
        Log.info("NETWORK_PARAMETERS.getPort(): " + NETWORK_PARAMETERS.getPort());
        Log.info("BITCOIN_RUNSCRIPTS: " + BITCOIN_RUNSCRIPTS);
        Log.info("FullClient.run() called");
        connectBlockChain();

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

    private void connectBlockChain() {

        try {

            blockStore = new PostgresFullPrunedBlockStore(NETWORK_PARAMETERS, 0, DB_HOST, DB_NAME, DB_USER, DB_PASSWORD);
            FullPrunedBlockChain blockChain = new FullPrunedBlockChain(NETWORK_PARAMETERS, blockStore);

            PeerDiscovery peerDiscovery;
            if (BITCOIN_NETWORK_LOCALHOST) {
                peerDiscovery = getLocalHostPeerDiscovery();
            } else {
                peerDiscovery = new DnsDiscovery(NETWORK_PARAMETERS);
            }

            //faster if false
            blockChain.setRunScripts(BITCOIN_RUNSCRIPTS);

            peerGroup = new PeerGroup(NETWORK_PARAMETERS, blockChain);
            peerGroup.addPeerDiscovery(peerDiscovery);

//            PeerEventListener listener = new TxListener2();
//            peerGroup.addEventListener(listener);

//            peerGroup.startBlockChainDownload(listener);
            peerGroup.start();
            Log.info("Starting up. Soon ready for Queries.");

        } catch (BlockStoreException e) {

            Log.error("Exception while opening block store", e);
        }
    }

    public static BigInteger getBalanceForAddress(String address) throws BlockStoreException, AddressFormatException {
        long start = System.currentTimeMillis();
        BigInteger balance;

        balance = blockStore.calculateBalanceForAddress(new Address(NETWORK_PARAMETERS, address));
        Log.info("Balance for address: " + address + " is " + balance + ". Calulated in " + (System.currentTimeMillis() - start) + "ms");

        return balance;
    }

    public static PeerDiscovery getLocalHostPeerDiscovery() {
        return new PeerDiscovery() {
            @Override
            public InetSocketAddress[] getPeers(long timeoutValue, TimeUnit timeoutUnit) throws PeerDiscoveryException {
                InetSocketAddress[] result = new InetSocketAddress[1];
                result[0] = new InetSocketAddress("localhost", 8333);
                return result;
            }

            @Override
            public void shutdown() {
                Log.info("shutDown");
            }
        };
    }


    public static PostgresFullPrunedBlockStore getBlockStore() {
        return blockStore;
    }

    public static PeerGroup getPeerGroup() {
        return peerGroup;
    }

    public void broadcast(Transaction t2) {

        t2.verify();

        peerGroup.broadcastTransaction(t2, 1);

    }
}
