#!/bin/bash
./spark/bin/spark-submit --master $SPARK_MASTER --deploy-mode client $@
