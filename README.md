# Spark-High-Availability-Zookeeper
## Launch Basic Spark Container
To get the image<br/>
`docker pull foodytechnologies/spark-openjdk8-alpine`<br/>
To run simple container<br/>
`docker run -p 4040:4040 -dti --privileged foodytechnologies/spark-openjdk11-alpine`<br/>
## Setup Cluster
It's a Spark Standalone Cluster With Zookeepercomposed of two zookeeper server, two spark masters, two slaves with each of them 5 workers and 1 application submitter
`docker-compose up -d --scale LocalClusterNetwork.spark.Slave=2`
To launch a local python application
` docker exec -ti ApplicationSubmitter sh StartApplication.sh /apps/python-apps/example.py`
To Laumch a local java application 
`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicLoadJson /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/tweets.json /data/HaveTweets`
`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicFlatMap /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar /data/spark.txt`
`docker exec -ti ApplicationSubmitter sh StartApplication.sh --class  com.databootcamp.sparkjobs.BasicAvg /apps/java-apps/target/sparkjobs-1.0.0-SNAPSHOT.jar`
