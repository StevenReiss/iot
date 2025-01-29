#! /bin/csh -f

pm2 stop jiqsign

rm iqsign.log

pm2 start --log iqsign.log --name jiqsign ../bin/signmakerserver.sh
