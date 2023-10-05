#!/bin/bash
set -euo pipefail # See http://redsymbol.net/articles/unofficial-bash-strict-mode/
IFS=$'\n\t'

# Execute via crontab by hduser@weywot1:
# 00 5 * * * ssh sol@quaoar1 "cd /home/sol/git/lobid-organisations ; bash -x cron.sh >> logs/cron.sh.log 2>&1"

curl --verbose -XPOST http://localhost:7200/organisations/transform
curl --verbose -XPOST http://localhost:7200/organisations/index

# see https://github.com/hbz/lobid-organisations/issues/482:
totalItems=$(curl "http://localhost:7200/organisations/search?q=_exists_%3AdbsID+AND+NOT+_exists_%3Aisil"|jq .totalItems)
if [ $totalItems -gt 0 ]; then
	mail -s "DB-ID without ISIL in lobid-organisations" "lobid-admin@hbz-nrw.de" -a "From: hduser@weywot1" << EOF
See https://github.com/hbz/lobid-organisations/issues/482
EOF
fi
