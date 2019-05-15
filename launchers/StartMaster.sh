#!/bin/bash
. "./Init.sh"
if [ -z "$SPARK_MASTER_HOST" ]
then
    echo "Please define SPARK_MASTER_HOST: where master will serve"
    exit
fi
if [ -z "$SPARK_MASTER_PORT" ]
then
    export SPARK_MASTER_PORT=7077
fi
if [ -z "$SPARK_MASTER_WEBUI_PORT" ]
then
    export SPARK_MASTER_WEBUI_PORT=8080
fi

PROPERTIES=""
CONF_FILE="/home/guest/spark/conf/spark-zookeeper.conf"
if [ ! -z "$SPARK_RECOVERYMODE" ]
then
    if [ "$SPARK_RECOVERYMODE" == "ZOOKEEPER" ]
    then
        if [ -z "$SPARK_ZOOKEEPER_URL" ] || [ -z "$SPARK_ZOOKEEPER_DIR" ]
        then
            echo "In Zokkeeper Cluster Mode , you have to specify SPARK_ZOOKEEPER_URL and SPARK_ZOOKEEPER_DIR"
        else
            if [ ! -f $CONF_FILE ]
            then
                touch $CONF_FILE
            fi
            sed -r '/^spark.deploy.recoveryMode=.*$/d' -i $CONF_FILE
            echo "spark.deploy.recoveryMode=ZOOKEEPER" >> $CONF_FILE
            sed -r '/^spark.deploy.zookeeper.url.*$/d' -i $CONF_FILE
            echo "spark.deploy.zookeeper.url=${SPARK_ZOOKEEPER_URL}" >> $CONF_FILE
            sed -r '/^spark.deploy.zookeeper.dir.*$/d' -i $CONF_FILE
            echo "spark.deploy.zookeeper.dir=${SPARK_ZOOKEEPER_DIR}" >> $CONF_FILE
            PROPERTIES="--properties-file $CONF_FILE"
        fi
    fi
fi
./spark/bin/spark-class org.apache.spark.deploy.master.Master --ip $SPARK_MASTER_HOST --port $SPARK_MASTER_PORT --webui-port $SPARK_MASTER_WEBUI_PORT $PROPERTIES
