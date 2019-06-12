package com.databootcamp.sparkjobs;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaPairRDD;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.hive.thriftserver.HiveThriftServer2;


public class ExposeContextWithLiveThrift {

  public static class Person implements java.io.Serializable {
    private String name;
    private Boolean receiveTweets;
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getReceiveTweets() {
      return receiveTweets;
    }

    public void setReceiveTweets(Boolean receiveTweets) {
      this.receiveTweets = receiveTweets;
    }
  }

  public static void main(String[] args) throws Exception {
    SparkConf conf = new SparkConf().setAppName("exposesession")
        .set("hive.server2.thrift.bind.host",  args[0])
        .set("hive.server2.thrift.port", args[1])
        .set("spark.sql.warehouse.dir", "/data/warehouse")
        .set("spark.sql.hive.thriftServer.singleSession", "True");
    JavaSparkContext sc = new JavaSparkContext(conf);
    SparkSession sparkSession= new SparkSession(sc.sc());
    sc.setLogLevel("INFO");
    Encoder<Person> personEncoder = Encoders.bean(Person.class);
    Dataset<Row> items = sparkSession.read().json(args[2]);
    Dataset<Person> people = items.filter(items.col("receiveTweets").cast("string").equalTo("true")).as(personEncoder).distinct();
    Dataset<String> formatted = people.map((MapFunction<Person, String>) p1 -> {ObjectMapper mapper = new ObjectMapper(); return mapper.writeValueAsString(p1);}, Encoders.STRING());
    //2-  create Or Replace temp view: temporary mode, shall loop infinitely to keep context and thrift server live
    formatted.coalesce(1).createOrReplaceTempView(args[3]);
    HiveThriftServer2.startWithContext(sparkSession.sqlContext());
    try { while(true) { Thread.sleep(50); } } catch(InterruptedException e) { e.printStackTrace(); } 
	}
}
