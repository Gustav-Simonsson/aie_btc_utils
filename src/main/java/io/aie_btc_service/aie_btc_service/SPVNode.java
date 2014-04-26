package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
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
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.BlockStoreException;
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

public class SPVNode implements Runnable {
    public static final Logger slf4jLogger =
        LoggerFactory.getLogger(SPVNode.class);
    public static final MainNetParams netParams = new MainNetParams();
    public static final String testSPVBlockStoreFilePath =
        "/var/tmp/aie_test_bitcoin_spv_block_store";
    public static final String testSPVWalletFile =
        "/var/tmp/aie_test_bitcoin_spv_wallet";

    public void run() {
        slf4jLogger.info("starting AIE Bitcoin SPV Node...");
        Wallet wallet = get_wallet(netParams);
        List<ECKey> keys = wallet.getKeys();
        Address address = keys.get(0).toAddress(netParams);
        File blockStoreFile = new File(testSPVBlockStoreFilePath);
        long offset = 0; // 86400 * 30;
        try {
            SPVBlockStore blockStore = new SPVBlockStore(netParams,
                                                         blockStoreFile);
            slf4jLogger.info("SPVBlockStore instantiated");

            InputStream stream =
                getClass().getClassLoader().getResourceAsStream("checkpoints");
            slf4jLogger.info("input stream debug: " + stream.available());
            long checkPointTime = wallet.getEarliestKeyCreationTime() - offset;
            CheckpointManager.checkpoint(netParams,
                                         stream, blockStore, checkPointTime);
            BlockChain blockChain = new BlockChain(netParams,
                                                   wallet,blockStore);
            PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
            peerGroup.addWallet(wallet);
            peerGroup.setFastCatchupTimeSecs(checkPointTime);
            // TODO: what bloomfilter rate makes sense?
            peerGroup.setBloomFilterFalsePositiveRate(1.0);
            // LocalPeer localPeer = new LocalPeer();
            // peerGroup.addPeerDiscovery(localPeer);
            peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            // InetAddress ia = InetAddress.getByName("2.221.132.213");
            // peerGroup.addAddress(new PeerAddress(ia, 8333));
            slf4jLogger.info("Starting peerGroup ...");
            peerGroup.startAndWait();
            PeerEventListener listener = new TxListener();
            peerGroup.addEventListener(listener);
        }
        catch (BlockStoreException e) {
            slf4jLogger.error("Caught BlockStoreException: ", e);
        }
        catch (UnknownHostException x) {
            slf4jLogger.error("Caught UnknownHostException: ", x);
        }
        catch (FileNotFoundException c) {
            slf4jLogger.error("Caught BlockStoreException: ", c);
        }
        catch (IOException ie) {
            slf4jLogger.error("Caught BlockStoreException: ", ie);
        }
    }

    public static Wallet get_wallet(MainNetParams netParams) {
        // TODO: put wallet file in Riak or somewhere persistent
        File walletFile = new File(testSPVWalletFile);
        Wallet wallet;
        try {
            wallet = Wallet.loadFromFile(walletFile);
        } catch (UnreadableWalletException e) {
            wallet = new Wallet(netParams);
            ECKey key = new ECKey();
            wallet.addKey(key);
            try {
                wallet.saveToFile(walletFile);
            } catch (IOException a) {
                slf4jLogger.error("Caught IOException: ", a);
            }
        }
        return wallet;
    }
}

class TxListener implements PeerEventListener {
    public static final MainNetParams netParams = new MainNetParams();
    public final Logger slf4jLogger = LoggerFactory.getLogger(SPVNode.class);

    public List<Message> getData (Peer p, GetDataMessage m) {
        slf4jLogger.info("Message received: " + m);
        return null;
    }
    public void onBlocksDownloaded(Peer p, Block b, int i) {
        slf4jLogger.info("Block downloaded:: " + b);
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
        slf4jLogger.info("PreMessage Received:: " + m);
        return null;
    }
    public void onTransaction(Peer peer, Transaction tx) {
        boolean validTx = true;
        String txHash = tx.getHashAsString();
        slf4jLogger.info("tx: " + txHash);
        List<TransactionOutput> txOutputs = tx.getOutputs();
        for (TransactionOutput output : txOutputs) {
            TransactionInput input = output.getSpentBy();
            try {
                if (output.getValue().
                    compareTo(output.getMinNonDustValue()) != 1) {
                    validTx = false;
                    break;
                }
                input.verify();
            }
            catch (RuntimeException epicfail) {
                validTx = false;
            }
        }
        slf4jLogger.info("valid tx: " + txHash);
    }
}
