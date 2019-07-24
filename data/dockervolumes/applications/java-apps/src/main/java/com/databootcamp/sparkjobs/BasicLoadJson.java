package com.databootcamp.sparkjobs;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;


public class BasicLoadJson {

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
        if (args.length != 2) {
            throw new Exception("Usage : BasicLoadJson inputJsonFile outputJsonFile");
        }
        SparkConf conf = new SparkConf().setAppName("basicloadjson");
        JavaSparkContext sc = new JavaSparkContext(conf);
        SparkSession sparkSession = new SparkSession(sc.sc());
        sc.setLogLevel("INFO");
        Encoder<Person> personEncoder = Encoders.bean(Person.class);
        Dataset<Row> items = sparkSession.read().json(args[0]);
        Dataset<Person> people = items.filter(items.col("receiveTweets").cast("string").equalTo("true")).as(personEncoder).distinct();
        Dataset<String> formatted = people.map((MapFunction<Person, String>) p1 -> {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(p1);
        }, Encoders.STRING());
        formatted.coalesce(1).write().mode("append").json(args[1]);
    }
}
