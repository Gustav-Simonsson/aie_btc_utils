package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.*;
import com.google.bitcoin.script.Script;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;

import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet2Params;
import com.google.bitcoin.params.TestNet3Params;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.sql.*;
import java.util.*;


public class Postgres {
    public static final TestNet3Params netParams = new TestNet3Params();
    private static final Logger log = LoggerFactory.getLogger(Postgres.class);
    private ThreadLocal<Connection> conn;
    private List<Connection> allConnections;
    private String connectionURL;
    private String username;
    private String password;

    private static final String driver = "org.postgresql.Driver";

    public Postgres(String hostname, String dbName, String username, String password) {
        connectionURL = "jdbc:postgresql://" + hostname + "/" + dbName;
        this.username = username;
        this.password = password;

        conn = new ThreadLocal<Connection>();
        allConnections = new LinkedList<Connection>();

        try {
            Class.forName(driver);
            log.info(driver + " loaded. ");
        } catch (java.lang.ClassNotFoundException e) {
            log.error("check CLASSPATH for Postgres jar ", e);
        }

        maybeConnect();
    }

    // TODO: fix proper exception handling
    // TODO: make it work for multiple outputs
    public OpenOutput getOpenOutput(String a) {
        Address address = null;
        try {
            address = new Address(netParams, a);
        }
        catch (AddressFormatException e) {}

        maybeConnect();
        PreparedStatement s = null;
        OpenOutput oo = null;
        try {
            s = conn.get().prepareStatement("SELECT hash, index, value, scriptbytes " +
                                            "FROM openOutputs WHERE toaddress = ?");
            s.setString(1, address.toString());
            ResultSet res = s.executeQuery();
            log.info("SQL res: " + res.toString());
            if (res.next()) {
                byte[] hash = res.getBytes(1);
                int index = res.getInt(2);
                byte[] value = res.getBytes(3);
                byte[] scriptBytes = res.getBytes(4);
                oo = new OpenOutput(hash, index, value, scriptBytes);
                return oo;
            } else {
                log.error("SQL query failed :O :O ");
                return oo;
            }
        } catch (SQLException ex) {
                log.error("SQLException :O " + ex);
                return oo;
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (SQLException ex2) {
                    log.error("SQLException :O " + ex2);
                }
        }
    }

    private synchronized void maybeConnect() {
        try {
            if (conn.get() != null)
                return;

            Properties props = new Properties();
            props.setProperty("user", this.username);
            props.setProperty("password", this.password);

            conn.set(DriverManager.getConnection(connectionURL, props));

            allConnections.add(conn.get());
            log.info("Made a new connection to database " + connectionURL);
        } catch (SQLException ex) {
            log.error("SQLException: " + ex);
        }
    }

    public synchronized void close() {
        for (Connection conn : allConnections) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        allConnections.clear();
    }

}
