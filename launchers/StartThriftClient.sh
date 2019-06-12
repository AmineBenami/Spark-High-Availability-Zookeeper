#!/bin/bash
PORT=10000
HOST=localhost
if [ ! -z "$1" ]
then
    HOST=$1
fi
if [ ! -z "$2" ]
then
    PORT=$2
fi
./spark/bin/beeline -u jdbc:hive2://$HOST:$PORT
