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
import org.apache.spark.sql.SQLContext;


public class SaveHive {

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
            throw new Exception("Usage : SaveHive inputJsonFile HiveTableName");
        }

        SparkConf conf = new SparkConf().setAppName("basicloadjson")
                .set("spark.sql.warehouse.dir", "/data/warehouse")
                .set("spark.sql.catalogImplementation", "hive");
        JavaSparkContext sc = new JavaSparkContext(conf);
        SparkSession sparkSession = new SparkSession(sc.sc());
        Encoder<Person> personEncoder = Encoders.bean(Person.class);
        Dataset<Row> items = sparkSession.read().json(args[0]);
        Dataset<Person> people = items.filter(items.col("receiveTweets").cast("string").equalTo("true")).as(personEncoder).distinct();
        Dataset<String> formatted = people.map((MapFunction<Person, String>) p1 -> {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(p1);
        }, Encoders.STRING());
        formatted.coalesce(1).write().mode("append").saveAsTable(args[1]);
        SQLContext sqlContext = SQLContext.getOrCreate(sc.sc());
        System.out.println("---> Show the  result: ");
        sqlContext.sql("SELECT * from " + args[1]).show();
        System.out.println("----> Show list of tables: ");
        sqlContext.sql("show tables").show();
    }
}
