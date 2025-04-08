#! /bin/csh

# for brown cs user /people/spr/sherpa/
flutter build web --base-href /sherpa/

pushd build/web
tar cf - . | (cd /Library/WebServer/Documents/sherpa; tar xf -)
popd

pushd build/web
scp -r * sherpa.cs.brown.edu:/vol/web/html/sherpa
popd




































