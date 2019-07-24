package com.databootcamp.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;


public class CreateTable {
    public static void main(String[] args) throws Exception {
        String hbaseZookeeperQuorumHost = args[0];
        int hbaseZookeeperPort = Integer.parseInt(args[1]);
        String serviceName = args[2];
        String tableName = args[3];
        String columnFamily = args[4];
        String[] families = {columnFamily};
        createTable(hbaseZookeeperQuorumHost, hbaseZookeeperPort, serviceName, tableName, families);
    }

    public static Configuration getConfiguration(String hbaseZookeeperQuorum, int hbaseZookeeperClientPort) {
        Configuration hConf = HBaseConfiguration.create();
        hConf.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        hConf.setInt("hbase.zookeeper.property.clientPort", hbaseZookeeperClientPort);
        hConf.setInt("hbase.client.retries.number", 7);
        hConf.setInt("ipc.client.connect.max.retries", 3);
        return hConf;
    }


    public static boolean tableExists(String hbaseZookeeperQuorum, int hbaseZookeeperClientPort, byte[] nameSpace, byte[] tableName) throws IOException {
        Admin admin = getAdmin(hbaseZookeeperQuorum, hbaseZookeeperClientPort);
        boolean exists = false;
        if (admin != null) {
            exists = admin.isTableAvailable(TableName.valueOf(nameSpace, tableName));
            admin.close();
        } else {
            System.err.println("---> cannot check table because cannot get admin from " + hbaseZookeeperQuorum + "  " + hbaseZookeeperClientPort);
        }
        return exists;
    }

    public static Admin getAdmin(String hbaseZookeeperQuorum, int hbaseZookeeperClientPort) {
        Configuration hConf = getConfiguration(hbaseZookeeperQuorum, hbaseZookeeperClientPort);
        Admin admin = null;
        try {
            Connection conn = ConnectionFactory.createConnection(hConf);
            admin = conn.getAdmin();
        } catch (Exception e) {
            System.out.println("---> getAdmin fails " + e.getMessage());
        }
        return admin;
    }

    public static ConnectionTable createConnection(String hbaseZookeeperQuorum, int hbaseZookeeperClientPort, byte[] nameSpace, byte[] tableName) throws IOException {
        ConnectionTable connectionTable = new ConnectionTable();
        if (tableExists(hbaseZookeeperQuorum, hbaseZookeeperClientPort, nameSpace, tableName) == true) {
            Configuration hConfig = getConfiguration(hbaseZookeeperQuorum, hbaseZookeeperClientPort);
            connectionTable.conn = ConnectionFactory.createConnection(hConfig);
            if (connectionTable.conn != null && connectionTable.conn.isClosed() == false) {
                try {
                    connectionTable.table = connectionTable.conn.getTable(TableName.valueOf(nameSpace, tableName));
                } catch (IOException e) {
                    System.err.println("---> cannot create connection to table  " + tableName + "  " + e.getMessage());
                }
            }
        } else {
            System.err.println("---> table " + tableName + " does not exists, cannot create connection");
        }
        return connectionTable;
    }

    public static void createTable(String hbaseZookeeperQuorum, int hbaseZookeeperClientPort, String nameSpaceStr, String tableNameStr, String[] columnsFamilyName) {
        try {
            byte[] tableName = Bytes.toBytes(tableNameStr);
            byte[] nameSpace = Bytes.toBytes(nameSpaceStr);
            if (tableExists(hbaseZookeeperQuorum, hbaseZookeeperClientPort, nameSpace, tableName) == false) {
                Admin admin = getAdmin(hbaseZookeeperQuorum, hbaseZookeeperClientPort);
                NamespaceDescriptor[] namespaceDescriptors = admin.listNamespaceDescriptors();
                boolean isExist = false;
                for (NamespaceDescriptor nd : namespaceDescriptors) {
                    if (nd.getName().equals(nameSpaceStr)) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    admin.createNamespace(NamespaceDescriptor.create(nameSpaceStr).build());
                }
                TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(TableName.valueOf(nameSpace, tableName));
                for (String columnFamilyName : columnsFamilyName) {
                    ColumnFamilyDescriptor cfd = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamilyName))
                            .setCompressionType(Compression.Algorithm.LZ4).setBloomFilterType(BloomType.ROW)
                            .setBlocksize(64 * 1024)
                            .build();
                    builder.setColumnFamily(cfd);
                }
                admin.createTable(builder.build());
                admin.close();
            }
        } catch (Exception e) {
            System.err.println("cannot create table " + tableNameStr + " " + e.getMessage());
        }

    }
}
