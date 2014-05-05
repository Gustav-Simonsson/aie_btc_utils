package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;

import com.google.bitcoin.core.FullPrunedBlockChain;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.BlockStoreException;

import com.google.bitcoin.store.H2FullPrunedBlockStore;

import com.google.bitcoin.store.PostgresFullPrunedBlockStore;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.uri.BitcoinURI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class BTCNode implements Runnable {
    public static final Logger slf4jLogger =
        LoggerFactory.getLogger(BTCNode.class);
    public static final TestNet3Params netParams = new TestNet3Params();

    public void run() {
        slf4jLogger.info("starting AIE bitcoinj node...");

        try {
            PostgresFullPrunedBlockStore store = new PostgresFullPrunedBlockStore(netParams,
                                                                                  235067,
                                                                                  "localhost",
                                                                                  "aie_bitcoin2",
                                                                                  "aie_bitcoin",
                                                                                  "aie_bitcoin");
            slf4jLogger.info("PostgresFullPrunedBlockStore constructed");

            long oneDayAgo = (System.currentTimeMillis() / 1000L) - (86400 * 10);

            FullPrunedBlockChain blockChain = new FullPrunedBlockChain(netParams,
                                                                       store);
            PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
            // peerGroup.setFastCatchupTimeSecs(oneDayAgo);
            // peerGroup.setBloomFilterFalsePositiveRate(1.0); // TODO: does this matter for us?
            peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            slf4jLogger.info("Starting peerGroup ...");
            peerGroup.startAndWait();
            PeerEventListener listener = new TxListener2();
            peerGroup.addEventListener(listener);
        }
        catch (BlockStoreException e) {slf4jLogger.error("Caught BlockStoreException: ", e);}
        // catch (IOException e) {slf4jLogger.error("Caught IOException: ", e);}
    }
}

class TxListener2 implements PeerEventListener {
    public static final TestNet3Params netParams = new TestNet3Params();
    public final Logger slf4jLogger = LoggerFactory.getLogger(BTCNode.class);

    public List<Message> getData (Peer p, GetDataMessage m) {
        slf4jLogger.info("Message received: ");
        return null;
    }
    public void onBlocksDownloaded(Peer p, Block b, int i) {
        slf4jLogger.info("Block downloaded: ");
    }
    public void onChainDownloadStarted(Peer arg0, int arg1) {
        slf4jLogger.info("blockchain download started.");
    }
    public void onPeerConnected(Peer arg0, int arg1) {
        slf4jLogger.info("Peer Connected.");
    }
    public void onPeerDisconnected(Peer arg0, int arg1) {
        slf4jLogger.info("Peer Disonnected.");
    }
    public Message onPreMessageReceived(Peer arg0, Message m) {
        slf4jLogger.info("PreMessage Received:: ");
        return null;
    }
    public void onTransaction(Peer peer, Transaction tx) {
        String txHash = tx.getHashAsString();
        slf4jLogger.info("tx: " + txHash);
    }
}
