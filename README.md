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
To launch a local python application<br/>
` docker exec -ti ApplicationSubmitter sh StartApplication.sh /apps/python-apps/example.py`<br/>
<br/>
To Launch a local java application, we move applications to local `./data/dockervolumes/applications` directory bound to Application and slaves containers on `/apps`.<br/>
We can also pass files as arguments to applications if they are placed on local directory `./data/dockervolumes/data` (we should give it write authorization if applications will save some files on it)
This directory is bound to containers on `/data` path<br/><br/>
## Examples
**_Manipulate a json file and generate a new one:_**<br/> `docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicLoadJson /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/tweets.json /data/HaveTweets`<br/>
**_Basic Flat Map by reading a file:_**<br/>`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicFlatMap /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/spark.txt`<br/>
**_Basic Avg:_**<br/>`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicAvg /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar`<br/>
**_Expose Context with thrift server:_**<br/>
- start standalone thrift server and expose a context in temporary view:<br/>`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class com.databootcamp.sparkjobs.ExposeContextWithLiveThrift /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar \`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}},{{end}}' ApplicationSubmitter | cut -d',' -f1\` 10011 /data/tweets.json exposethecontext`<br/>
- start thrift client and read context:<br/>
`beeline -u jdbc:hive2://IP_TO_SPARK_EXECUTOR:10011`<br/>
`0: jdbc:hive2://172.28.0.5:10011> show tables;)`<br/>
