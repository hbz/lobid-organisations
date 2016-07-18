#!/bin/bash
set -euo pipefail # See http://redsymbol.net/articles/unofficial-bash-strict-mode/
IFS=$'\n\t'

# Execute via crontab by hduser@weywot1:
# 00 5 * * * ssh sol@quaoar1 "cd /home/sol/git/lobid-organisations ; bash -x cron.sh >> logs/cron.sh.log 2>&1"

curl --verbose -XPOST http://localhost:7200/organisations/transform
curl --verbose -XPOST http://localhost:7200/organisations/index
