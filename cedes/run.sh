#! /bin/bash -vx

if [ "$HOST" = "sprhome" ];  then
   cd /pro/iot/cedes
   node server.js
else
   source ~/.bashrc1
   cd /vol/iot/cedes
   nvm run default server.js
fi

echo cedes running
