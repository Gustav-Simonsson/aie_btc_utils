package io.aie_btc_service.aie_btc_service;

import com.google.bitcoin.core.*;
import com.google.bitcoin.core.StoredTransactionOutput;
import com.google.bitcoin.params.TestNet3Params;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static io.aie_btc_service.aie_btc_service.FullClient.TEST_NET_3_PARAMS;


public class Postgres {
    private static final Logger Log = LoggerFactory.getLogger(Postgres.class);
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
            Log.info(driver + " loaded. ");
        } catch (java.lang.ClassNotFoundException e) {
            Log.error("check CLASSPATH for Postgres jar ", e);
        }

        maybeConnect();
    }

    // TODO: fix proper exception handling
    // TODO: make it work for multiple outputs
    public OpenOutput getOpenOutput(String a) {
        Log.info("getOpenOutput(): address: " + a);

        Address address = null;
        try {
            address = new Address(TEST_NET_3_PARAMS, a);
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
            Log.info("SQL res: " + res.toString());
            if (res.next()) {
                byte[] hash = res.getBytes(1);
                int index = res.getInt(2);
                byte[] value = res.getBytes(3);
                byte[] scriptBytes = res.getBytes(4);
                oo = new OpenOutput(hash, index, value, scriptBytes);
                return oo;
            } else {
                Log.error("SQL query failed :O :O ");
                return oo;
            }
        } catch (SQLException ex) {
                Log.error("SQLException :O " + ex);
                return oo;
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (SQLException ex2) {
                    Log.error("SQLException :O " + ex2);
                }
        }
    }

    public void insertOpenOutput(StoredTransactionOutput out) {
        maybeConnect();
        PreparedStatement s = null;
        try {
            s = conn.get().prepareStatement("INSERT INTO openOutputs (hash, index, height, value, scriptBytes, toAddress, addressTargetable) " +
                    "VALUES (?, ?, ?, ?, ?, NULL, NULL)");
            s.setBytes(1, out.getHash().getBytes());
            s.setInt(2, (int)out.getIndex()); // index is actually an unsigned int
            s.setInt(3, out.getHeight());
            s.setBytes(4, out.getValue().toByteArray());
            s.setBytes(5, out.getScriptBytes());
            s.executeUpdate();
            s.close();
        } catch (SQLException e) {
            Log.error("SQLException :O " + e);
            return;
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (SQLException e) {
                    Log.error("SQLException :O " + e);
                    return;
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
            Log.info("Made a new connection to database " + connectionURL);
        } catch (SQLException ex) {
            Log.error("SQLException: " + ex);
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
