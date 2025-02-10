#! /bin/csh

flutter build web --base-href /iqsign/

pushd build/web
tar cvf - . | (cd /Library/WebServer/Documents/iqsign; tar xvf -)
popd

pushd build/web
scp -r * sherpa.cs.brown.edu:/vol/web/html/iqsign
popd




































