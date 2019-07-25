package com.databootcamp.sparkjobs;


import org.apache.spark.sql.Dataset;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.execution.datasources.hbase.HBaseTableCatalog;

import java.util.HashMap;
import java.util.Map;


public class ReadFromKafkaSinkToHbase {

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 5) {
            System.err
                    .println("Usage: ReadHbaseSourceUpdate {hbaseZookeeperHost} {hbaseZookeeperPort} {ServiceName} {table} {columnFamily}");
            System.exit(1);
        }

        String hbaseZookeeperQuorumHost = args[0];
        int hbaseZookeeperPort = Integer.parseInt(args[1]);
        String serviceName = args[2];
        String tableName = args[3];
        String columnFamily = args[4];


        Duration batchInterval = new Duration(2000);
        SparkConf conf = new SparkConf().setAppName("ReadHbaseSourceUpdate");
        JavaStreamingContext sc = new JavaStreamingContext(conf, batchInterval);

        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);
        javaSparkContext.hadoopConfiguration().set("hbase.zookeeper.quorum", hbaseZookeeperQuorumHost);
        javaSparkContext.hadoopConfiguration().setInt("hbase.zookeeper.property.clientPort", hbaseZookeeperPort);
        javaSparkContext.hadoopConfiguration().setInt("hbase.client.retries.number", 7);
        javaSparkContext.hadoopConfiguration().setInt("ipc.client.connect.max.retries", 3);
        SQLContext sqlContext = SQLContext.getOrCreate(javaSparkContext.sc());

        String catalog = "{\n" +
                "\t\"table\":{\"namespace\":\"" + serviceName + "\", \"name\":\""+ tableName +"\", \"tableCoder\":\"PrimitiveType\"},\n" +
                "    \"rowkey\":\"key\",\n" +
                "    \"columns\":{\n" +
                "\t    \"col0\":{\"cf\":\"rowkey\", \"col\":\"key\", \"type\":\"string\"},\n" +
                "\t    \"col1\":{\"cf\":\""+ columnFamily  + "\", \"col\":\"Counter\", \"type\":\"int\"}\n" +
                "    }\n" +
                "}";
        Map<String, String> optionsMap = new HashMap<>();
        String htc = HBaseTableCatalog.tableCatalog();
        optionsMap.put(htc, catalog);

        Dataset dataset = sqlContext.read().options(optionsMap).format("org.apache.spark.sql.execution.datasources.hbase").load();
    }
}
