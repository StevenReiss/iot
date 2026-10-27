#! /bin/bash -f

SRD=$(dirname "${BASH_SOURCE[0]}")
pushd $SRD > /dev/null
SRD1=`pwd`
popd > /dev/null

if [ "$HOST" = "Brown-F1QWPJJ9" ]; then
    export HOST="$(scutil --get ComputerName).local"
fi

java -jar $SRD1/devices.jar
