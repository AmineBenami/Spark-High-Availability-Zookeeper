#!/bin/bash
export SPARK_MASTER_HOST=`hostname`
. "/home/guest/spark/sbin/spark-config.sh"
. "/home/guest/spark/bin/load-spark-env.sh"
export SPARK_HOME=/home/guest/spark

