package io.aie_btc_service.aie_btc_service;


import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.PostgresFullPrunedBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class FullClient {

    public static final Logger Log = LoggerFactory.getLogger(FullClient.class);
    public final static NetworkParameters TEST_NET_3_PARAMS = new TestNet3Params();
    public static final String DB_HOST = "localhost";
    public static final String DB_NAME = "aie_bitcoin3";
//    public static final String DB_NAME = "aie_bitcoin2";
    public static final String DB_USER = "biafra";
    public static final String DB_PASSWORD = "";
    private static PostgresFullPrunedBlockStore blockStore;

    public static void main(String[] args) {
        new FullClient().run();
    }

    private static PeerGroup peerGroup;

    public void run() {

        connectBlockChain();

//        String[] addresses = new String[]{
//                "18Nnr1236PeuVXpceXKbDJbAhWK6dYxaS9",
//                "1Dh2Pzbqe15QPsrHXrurLCHpY28K6ZGQMx",
//                "1PBuXpUVcLZEpoTSRC1tLpyV6xtGCvEeJR",
//                "1KBmj2QQs9yiq3wW2jEJuVnuBybWvLv6fV",
//                "1E1xPBdS85g8Eocs28NRHU4JQ1PEdbVgPg",
//                "1LttvufSeNniUFTMRJq46ZRbLhZoQvFVNJ"
//        };
//        for (String address : addresses) {
//            try {
//
//                getBalanceForAddress(address);
//
//            } catch (BlockStoreException e) {
//                Log.error("Exception while calculating balance", e);
//            } catch (AddressFormatException e) {
//                Log.error("Exception while calculating balance", e);
//            }
//        }
    }

    private void connectBlockChain() {

        try {

            blockStore = new PostgresFullPrunedBlockStore(TEST_NET_3_PARAMS, 10000, DB_HOST, DB_NAME, DB_USER, DB_PASSWORD);
            FullPrunedBlockChain blockChain = new FullPrunedBlockChain(TEST_NET_3_PARAMS, blockStore);
            PeerDiscovery peerDiscovery = new DnsDiscovery(TEST_NET_3_PARAMS);

            //faster
            blockChain.setRunScripts(false);

            peerGroup = new PeerGroup(TEST_NET_3_PARAMS, blockChain);
            peerGroup.addPeerDiscovery(peerDiscovery);
            InetAddress ia = null;
            InetAddress ia2 = null;
            InetAddress ia3 = null;
            InetAddress ia4 = null;
            try {
                ia  = InetAddress.getByName("54.243.211.176");
                ia2 = InetAddress.getByName("148.251.6.214");
                ia3 = InetAddress.getByName("188.226.139.138");
                ia4 = InetAddress.getByName("144.76.165.115");
            } catch (UnknownHostException e){ Log.error("omg :O " + e);};
            peerGroup.addAddress(new PeerAddress(ia, 18333));
            peerGroup.addAddress(new PeerAddress(ia2, 18333));
            peerGroup.addAddress(new PeerAddress(ia3, 18333));
            peerGroup.addAddress(new PeerAddress(ia4, 18333));

            PeerEventListener listener = new TxListener2();
            peerGroup.addEventListener(listener);

            Log.info("calling startAndWait...");

            peerGroup.startAndWait();
            Log.info("Starting up. Soon ready for Queries.");

        } catch (BlockStoreException e) {

            Log.error("Exception while opening blockstore", e);

        }

    }

    public static BigInteger getBalanceForAddress(String address) throws BlockStoreException, AddressFormatException {
        long start = System.currentTimeMillis();
        BigInteger balance;

        balance = blockStore.calculateBalanceForAddress(new Address(TEST_NET_3_PARAMS, address));
        Log.info("Balance for address: " + address + " is " + balance + ". Calulated in " + (System.currentTimeMillis() - start) + "ms");

        return balance;
    }

    public static PeerDiscovery getLocalHostPeerDiscovery() {
        return new PeerDiscovery() {
            @Override
            public InetSocketAddress[] getPeers(long timeoutValue, TimeUnit timeoutUnit) throws PeerDiscoveryException {
                InetSocketAddress[] result = new InetSocketAddress[1];
                result[0] = new InetSocketAddress(DB_HOST, 8333);
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

        peerGroup.broadcastTransaction(t2, 2);

    }
}
