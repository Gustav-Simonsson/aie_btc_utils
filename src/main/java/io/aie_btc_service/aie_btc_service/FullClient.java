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
    private final static NetworkParameters netParams = new TestNet3Params();
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

            blockStore = new PostgresFullPrunedBlockStore(netParams, 0, "localhost", "aie_bitcoin2", "biafra", "");
            FullPrunedBlockChain blockChain = new FullPrunedBlockChain(netParams, blockStore);
            PeerDiscovery peerDiscovery = new DnsDiscovery(netParams);

            //faster
            blockChain.setRunScripts(false);

            peerGroup = new PeerGroup(netParams, blockChain);
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

            peerGroup.start();
            Log.info("Starting up. Soon ready for Queries.");

        } catch (BlockStoreException e) {

            Log.error("Exception while opening blockstore", e);

        }

    }

    public static BigInteger getBalanceForAddress(String address) throws BlockStoreException, AddressFormatException {
        long start = System.currentTimeMillis();
        BigInteger balance;

        balance = blockStore.calculateBalanceForAddress(new Address(netParams, address));
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
}
