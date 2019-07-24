# Spark-High-Availability-Zookeeper
## Launch Basic Spark Container
To get the image<br/>
`docker pull foodytechnologies/spark-openjdk8-alpine`<br/>
To run simple container<br/>
`docker run -p 4040:4040 -dti --privileged foodytechnologies/spark-openjdk11-alpine`<br/>
<br/>
## Setup Cluster
It's a Spark Standalone Cluster With Zookeeper composed of two zookeeper server, two spark masters, two slaves with each of them 5 workers and 1 application submitter<br/>
`docker-compose up -d --scale LocalClusterNetwork.spark.Slave=2`<br/>
<br/>
## Launch Applications on Spark Cluster
- To launch a local python application<br/>
` docker exec -ti ApplicationSubmitter sh StartApplication.sh /apps/python-apps/example.py`<br/>
<br/>

- To Launch a local java application<br/>
> Compile your jobs source:<br/>
```
$ cd ./data/dockervolumes/applications/java-apps/
$ mvn package
```
> Docker compose will mount local `./data/dockervolumes/applications` directory on `/apps` directory of Application and slaves containers.<br/>
We can also pass files/data as arguments to jobs by placing them on local directory `./data/dockervolumes/data` (we should give directory write authorization if jobs will save some files on it)
Docker compose will bind this local directory on `/data` directory of started containers<br/>
<br/>

## Examples
**_Manipulate a json file and generate a new one:_**<br/> `docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicLoadJson /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/tweets.json /data/HaveTweets`<br/><br/>
**_Basic Flat Map by reading a file:_**<br/>`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicFlatMap /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/spark.txt`<br/><br/>
**_Basic Avg:_**<br/>`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicAvg /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar`<br/><br/>
**_Expose Context with thrift server:_**<br/>
+ start standalone thrift server and expose a context in temporary view:<br/>```docker exec -ti ApplicationSubmitter sh StartApplication.sh --class com.databootcamp.sparkjobs.ExposeContextWithLiveThrift /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar `docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}},{{end}}' ApplicationSubmitter | cut -d',' -f1` 10011 /data/tweets.json exposethecontext```<br/>
+ start thrift client and read context:<br/>
`beeline -u jdbc:hive2://IP_TO_SPARK_EXECUTOR:10011`<br/>
`0: jdbc:hive2://172.28.0.5:10011> show tables;`<br/>
<br/>

**_Export json file to Hive table:_** to keep data and meta-data we should configure hive-site.xml, core-site.xml (for security configuration), and hdfs-site.xml (for HDFS configuration) file in conf/<br/>
` docker exec -ti ApplicationSubmitter sh StartApplication.sh --class
com.databootcamp.sparkjobs.SaveHive /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/tweets.json tweets`<br/>
<br/>

**_Read Streaming from Flume Agent (polling mode) and save in Hbase table (in batch mode):_**<br/>
##### - Start Flume Service: <br/>
please refer to the example described in the "Flume" repository: [MedAmineBB/Flume](https://github.com/MedAmineBB/Flume)<br/>
##### - Start HBase Cluser Service:<br/>
please refert to the how to describe in the "Hbase" repository: [MedAmineBB/HBaseWithHDFS](https://github.com/MedAmineBB/HBaseWithHDFS) by applying section "Launch Hdfs and Hbase"<br/>
##### - Connect Hbase, Spark and Flume containers: <br/>
```
$ # Create a network where we will expose all dockers that shall communicate in this example
$ docker network create -d bridge --subnet 172.28.0.0/16 bridge_nw
$ # Expose Hbase layer (zookeeper, HMaster and Region Servers)
$ docker network connect bridge_nw zoo1
$ docker network connect bridge_nw zoo2
$ docker network connect bridge_nw zoo3
$ docker network connect bridge_nw rs1
$ docker network connect bridge_nw rs2
$ docker network connect bridge_nw rs3
$ docker network connect bridge_nw hm1
$ docker network connect bridge_nw hm2
$ # Expose Flume layer
$ docker network connect bridge_nw relayer
$ # Expose Spark AllplicationSubmitter, Slaves, masters
$ docker network connect ApplicationSubmitter
$ docker network connect ownspark_LocalClusterNetwork.spark.Slave_COMPLETE_THAT
$ docker network connect Master0
$ docker network connect Master1
```
<br/>

##### - Start Spark Job (from streaming data to batch data):
```
EXTRA_JARS=`docker exec -ti ApplicationSubmitter sh -c 'ls -p /apps/java-apps/target/libs/*.jar | tr "\n" ","'` sh -c 'docker exec -ti ApplicationSubmitter sh StartApplication.sh --jars $EXTRA_JARS --class com.databootcamp.sparkjobs.StreamingFromFlumeToHBase /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar relayer 4545 zoo1,zoo2,zoo3 2181 databootcamp netcat data'
```
