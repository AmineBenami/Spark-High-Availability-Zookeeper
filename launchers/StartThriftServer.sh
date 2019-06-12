#!/bin/bash
. "./Init.sh"
if [ -z "$SPARK_MASTERI" ]
then
    echo "Shall define SPARK_MASTER: where master is serving"
    exit
fi
masters=$(echo $SPARK_MASTERI | tr "," "\n")
masterUp=0
while true; do
    for master in $masters
    do
        if [[ $master == "spark://"* ]]
        then
            master="${master:8:512}"
        fi
        hp=$(echo $master | tr ":" "\n")
        echo ${hp[0]} ${hp[1]}
        nc ${hp[0]} ${hp[1]}
        if [ $? -eq 0 ]
        then
            masterUp=1
            break
        fi
    done
    if [ $masterUp -eq 0 ]
    then
        echo "any of masters is up"
        sleep 1
    else
        break
    fi
done

if [ -z "$TOTAL_EXECUTORS" ]
then
    TOTAL_EXECUTORS=2
fi

if [ -z "$WAREHOUSE_DIR" ]
then
    WAREHOUSE_DIR="/data/warehouse"
fi

./spark/sbin/start-thriftserver.sh --conf spark.sql.warehouse.dir=$WAREHOUSE_DIR --total-executor-cores $TOTAL_EXECUTORS --master $SPARK_MASTERI
tail -f /dev/null
