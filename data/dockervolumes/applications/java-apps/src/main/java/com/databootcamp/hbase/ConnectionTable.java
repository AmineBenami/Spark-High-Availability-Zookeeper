package com.databootcamp.hbase;

import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

public class ConnectionTable {
    public Connection conn = null;
    public Table table = null;

    public ConnectionTable() {
    }

    public boolean check() {
        if (conn == null || conn.isClosed() == true) {
            return false;
        }
        if (table == null) {
            return false;
        }
        return true;
    }

    public void close() throws IOException {
        if (table != null) {
            table.close();
            table = null;
        }
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }
}
