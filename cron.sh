#!/bin/bash
set -euo pipefail # See http://redsymbol.net/articles/unofficial-bash-strict-mode/
IFS=$'\n\t'

# Execute via crontab by sol@quaoar1:
# 00 6 * * * cd /home/sol/git/lobid-organisations ; bash cron.sh > logs/cron.sh.log 2>&1

curl --verbose -XPOST http://quaoar1.hbz-nrw.de:7200/organisations/transform
curl --verbose -XPOST http://quaoar1.hbz-nrw.de:7200/organisations/index
