#! /bin/csh

flutter build web --base-href /iqsign/

pushd build/web
tar cvf - . | (cd /Library/WebServer/Documents/iqsign/webapp; tar xvf -)
popd

pushd build/web
scp -r * sherpa.cs.brown.edu:/vol/web/iqsign/webapp
popd










