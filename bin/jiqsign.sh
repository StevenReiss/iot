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

echo java -cp $LIB/jakarta.mail.jar:$LIB/jakarta.activation.jar -jar $SRD1/iqsign.jar -server -L jiqsign.log -S -LD

# java -jar $SRD1/iqsign.jar -server -L jiqsign.log -S -LD










