#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db



ENUM SignDim "( '16by9', '4by3', '16by10', 'other' )"



$run $host $db <<EOF

$runcmd

ALTER TABLE iQsignUsers ADD temppassword text;

ALTER TABLE iQsignImages ADD is_border bool DEFAULT false;
ALTER TABLE iQsignImages ADD description text;


EOF
