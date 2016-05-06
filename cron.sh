#!/bin/bash
set -euo pipefail # See http://redsymbol.net/articles/unofficial-bash-strict-mode/
IFS=$'\n\t'

# Execute via crontab by sol@quaoar1:
# 00 6 * * * cd /home/sol/git/lobid-organisations ; bash cron.sh > logs/cron.sh.log 2>&1

LOGFILE="logs/$(date "+%Y%m%d").log"
mvn clean install -DskipTests > $LOGFILE 2>&1
mvn exec:java -Dexec.mainClass=transformation.Enrich -Dexec.args="2013-06-01 100 'http://gaia.hbz-nrw.de:7400'" > $LOGFILE 2>&1
mvn exec:java -Dexec.mainClass=transformation.Index -Dexec.args="'23000000' 'src/main/resources/output/enriched.out.json'" > $LOGFILE 2>&1
curl --verbose -XPOST http://quaoar1.hbz-nrw.de:7200/organisations/index
