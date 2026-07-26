#!/bin/sh
DIR=$(dirname $0)
$DIR/stop.sh
sleep 5
$DIR/start.sh
