#!/bin/bash
application=/apps/$1
if [ ! -f $application ]
then
    echo "application do not exists"
    exit
fi
cd spark
./bin/spark-submit --master $SPARK_MASTER $application
