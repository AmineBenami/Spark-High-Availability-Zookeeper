#!/bin/bash
. "./Init.sh"
if [ -z "$SPARK_MASTERI" ]
then
    echo "Shall define SPARK_MASTER: where master is serving"
    exit
fi
if [ -z "$SPARK_WORKER_WEBUI_PORT" ]
then
    export SPARK_MASTER_WEBUI_PORT=8081
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
./spark/sbin/start-slave.sh $SPARK_MASTERI
tail -f /dev/null
