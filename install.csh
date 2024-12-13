#! /bin/csh -f

git commit -a --dry-run >&! /dev/null
if ($status == 0) then
   git commit -a
   if ($status > 0) exit;
endif

git push


ssh sherpa.cs.brown.edu '(cd /vol/iot; git pull)'
if ($status > 0) exit;

pushd savedimages
# copy any images that you want to maintain here
# scp baby.jpg sherpa.cs.brown.edu:/vol/iot/images
popd

scp flutter/iqsign/assets/*.html sherpa.cs.brown.edu:/vol/web/html/iqsign
scp flutter/iqsign/assets/images/*.png sherpa.cs.brown.edu:/vol/web/html/iqsign/images
scp /pro/ivy/lib/ivy.jar sherpa.cs.brown.edu:/vol/iot/catre/lib
scp /pro/ivy/lib/ivy.jar sherpa.cs.brown.edu:/vol/iot/signmaker/lib


pushd secret
update.csh
popd

pushd devices
ant
popd

pushd catre
ant
popd

ssh sherpa.cs.brown.edu '(cd /vol/iot/iqsign; npm update)'
echo npm status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/signmaker; ant)'
echo signmaker ant status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/catre; ant)'
echo catre ant status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/devices; ant)'
echo devices ant status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/cedes; npm update)'
echo npm status $status


ssh sherpa.cs.brown.edu '(cd /vol/iot/iqsign; start.csh)'
echo iqsign start status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/cedes; start.csh)'
echo cedes start status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/signmaker; start.csh)'
echo signmaker start status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/iqsign; starto.csh)'
echo oauth start status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/catre; start.csh)'
echo catre start status $status

pushd devices
start.csh
popd
