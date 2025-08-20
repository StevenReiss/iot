#! /bin/csh -f

pm2 stop lockalive

rm lockalive.log

pm2 start --log lockalive.log --name lockalive ../bin/lockalive.sh
