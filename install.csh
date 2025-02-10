#! /bin/csh -f

pushd /pro/ivy
# ant
make jar
popd

git commit -a --dry-run >&! /dev/null
if ($status == 0) then
   git commit -a
   if ($status > 0) exit;
endif

git push


ssh sherpa.cs.brown.edu '(cd /vol/iot; git pull)'
if ($status > 0) exit;

set images = ( baby.jpg snowflake.jpg spring.png )
pushd savedimages
# copy any images that you want to maintain here
# scp baby.jpg sherpa.cs.brown.edu:/vol/iot/images
foreach i ( $images )
   scp $i sherpa.cs.brown.edu:/vol/iot/savedimages
end
popd

scp flutter/iqsign/assets/*.html sherpa.cs.brown.edu:/vol/web/html/iqsign
scp flutter/iqsign/assets/images/*.png sherpa.cs.brown.edu:/vol/web/html/iqsign/images
scp /pro/ivy/lib/ivy.jar sherpa.cs.brown.edu:/vol/iot/catre/lib
scp /pro/ivy/lib/ivy.jar sherpa.cs.brown.edu:/vol/iot/signmaker/lib

set ivylib = ( ivy.jar postgresql.jar mysql.jar json.jar jakarta.mail.jar jakarta.activation.jar slf4j-api.jar )
foreach i ( $ivylib )
   scp /pro/ivy/lib/$i sherpa.cs.brown.edu:/vol/ivy/lib
end


pushd secret
update.csh
popd

pushd devices
ant
popd

pushd catre
ant
popd

pushd jiqsign
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
ssh sherpa.cs.brown.edu '(cd /vol/iot/jiqsign; ant)'
echo jiqsign ant status $status
ssh sherpa.cs.brown.edu '(cd /vol/iot/cedes; npm update)'
echo npm status $status


ssh sherpa.cs.brown.edu '(cd /vol/iot/jiqsign; start.csh)'
echo jiqsign start status $status
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

pushd flutter/iqsign
buildweb.csh >&! buildweb.out
popd
