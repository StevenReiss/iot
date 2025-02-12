#! /bin/csh

flutter build web --base-href /iqsign/

pushd build/web
tar cf - . | (cd /Library/WebServer/Documents/iqsign; tar xf -)
popd

pushd build/web
scp -r * sherpa.cs.brown.edu:/vol/web/html/iqsign
scp -r * ssh.cs.brown.edu:/pro/web/web/people/spr/iqsign
popd




































