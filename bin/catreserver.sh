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

CP=$LIB/jakarta.mail.jar:$LIB/jakarta.activation.jar:$SRD1/catre.jar
echo java -cp $CP edu.brown.cs.catre/catmain.CatmainMain -server -L catreserver.log -S -LD
java -cp $CP edu.brown.cs.catre/catmain.CatmainMain -server -L catreserver.log -S -LD












