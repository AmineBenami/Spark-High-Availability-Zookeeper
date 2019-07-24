package com.databootcamp.sparkjobs;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.spark.SparkConf;


import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class BasicFlatMap {
  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
        throw new Exception("Usage : BasicFlatMap inputFile");
    }
    SparkConf conf = new SparkConf().setAppName("basicflatmap");
    JavaSparkContext sc = new JavaSparkContext(conf);
    sc.setLogLevel("INFO");

    JavaRDD<String> rdd = sc.textFile(args[0]);
    JavaRDD<String> words = rdd.flatMap( l ->  Arrays.asList(l.split(" ")).iterator() );
    Map<String, Long> result = words.countByValue();
    for (Entry<String, Long> entry: result.entrySet()) {
      System.out.println(entry.getKey() + ":" + entry.getValue());
    }
  }
}
