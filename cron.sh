#!/bin/bash
set -euo pipefail # See http://redsymbol.net/articles/unofficial-bash-strict-mode/
IFS=$'\n\t'

# Execute via crontab by hduser@weywot1:
# 00 5 * * * ssh sol@quaoar1 "cd /home/sol/git/lobid-organisations ; bash -x cron.sh >> logs/cron.sh.log 2>&1"

curl --verbose -XPOST http://localhost:7200/organisations/transform
curl --verbose -XPOST http://localhost:7200/organisations/index

# see https://github.com/hbz/lobid-organisations/issues/419:
totalItems=$(curl "https://lobid.org/organisations/search?q=_exists_%3AdbsID+AND+NOT+_exists_%3Aisil+AND+_exists_%3Alocation.geo"|jq .totalItems)
if [ $totalItems -lt 4000 ]; then
	mail -s "Missing geo data in lobid-organisations" "lobid-admin@hbz-nrw.de" -a "From: hduser@weywot1" << EOF
See https://github.com/hbz/lobid-organisations/issues/419
EOF
fi
