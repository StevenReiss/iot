#! /bin/bash -f

SRD=$(dirname "${BASH_SOURCE[0]}")
pushd $SRD > /dev/null
SRD1=`pwd`
popd > /dev/null

LIB=$SRD/../../ivy/lib
echo check $LIB
if test -d $LIB; then
   echo use local lib
else
   LIB=/pro/ivy/lib
fi

java -jar $SRD1/signmaker.jar -server -L maker_log.log -S -LD

