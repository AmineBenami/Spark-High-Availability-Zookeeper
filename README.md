# Spark-High-Availability-Zookeeper
To get the image<br/>
`docker pull foodytechnologies/alpine-kafka-rest-proxy`<br/>
To run simple container<br/>
`docker run -p 4040:4040 -dti --privileged foodytechnologies/spark-openjdk11-alpine`<br/>
To setup Cluster: composed of two zookeeper server, two spark masters, two slaves with each of them 5 workers and 1 application submitter
`docker-compose up -d --scale LocalClusterNetwork.spark.Slave=2`
To launch an application
`docker exec -ti ApplicationSubmitter sh StartApplication.sh python-apps/example.py`
