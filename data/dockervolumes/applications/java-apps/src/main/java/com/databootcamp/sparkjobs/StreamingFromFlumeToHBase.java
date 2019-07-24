package com.databootcamp.sparkjobs;

import java.util.Arrays;

import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.flume.FlumeUtils;
import org.apache.spark.streaming.flume.SparkFlumeEvent;

import java.util.regex.Pattern;

import scala.Tuple2;
import com.databootcamp.hbase.HBaseCounterIncrementor;
import com.databootcamp.hbase.CreateTable;
import org.apache.spark.SparkConf;


public class StreamingFromFlumeToHBase {
    private static final Pattern SPACE = Pattern.compile(" ");

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 7) {
            System.err
                    .println("Usage: StreamingFromFlumeToHBase {hostFlume} {portFlume} {hbaseZookeeperHost} {hbaseZookeeperPort} {ServiceName} {table} {columnFamily}");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String hbaseZookeeperQuorumHost = args[2];
        int hbaseZookeeperPort = Integer.parseInt(args[3]);
        String serviceName = args[4];
        String tableName = args[5];
        String columnFamily = args[6];


        Duration batchInterval = new Duration(2000);
        String[] listOfColumns = {columnFamily};
        CreateTable.createTable(hbaseZookeeperQuorumHost, hbaseZookeeperPort, serviceName, tableName, listOfColumns);


        SparkConf conf = new SparkConf().setAppName("FlumeEventCount");
        JavaStreamingContext sc = new JavaStreamingContext(conf, batchInterval);

        JavaDStream<SparkFlumeEvent> flumeStream = FlumeUtils.createPollingStream(sc, host, port);
        System.out.println("flume stream create polling stream called " + host + "  " + port);
        JavaPairDStream<String, Integer> lastCounts = flumeStream
                .flatMap(event -> {
                            String line = (new String(event.event().getBody().array())).replaceAll("\\r\\n|\\r|\\n", " ");
                            return Arrays.asList(SPACE.split(line)).iterator();
                        }
                ).mapToPair(
                        str -> new Tuple2<String, Integer>(str, 1)
                ).reduceByKey(
                        (x, y) -> x.intValue() + y.intValue()
                );
        lastCounts.foreachRDD(
                (values, time) -> {
                    values.foreach(
                            (tuple) -> {
                                HBaseCounterIncrementor incrementor = HBaseCounterIncrementor.getInstance(hbaseZookeeperQuorumHost, hbaseZookeeperPort, serviceName, tableName, columnFamily);
                                incrementor.incerment("Counter", tuple._1(), tuple._2());
                            }
                    );
                });
        sc.start();
        sc.awaitTermination();
        sc.close();
    }
}
