#! /bin/csh -f

pm2 stop iqsign

rm iqsign.log

pm2 start --log iqsign.log --name iqsign ../bin/jiqsign.sh
