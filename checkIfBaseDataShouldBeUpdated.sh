# mail if basedump is more than a year old
# see https://github.com/hbz/lobid-organisations/issues/504

MAIL_TO=$(cat .secrets/MAIL_TO)
MAIL_FROM=$(cat .secrets/MAIL_FROM)

OLD_BASEDUMP_DATE=$(grep 'transformation.updates.start'  conf/application.conf |cut -f2 -d "\"")
OLD_BASEDUMP_DATE_PLUS_ONE_YEAR=$(date '+%C%y%m%d' -d"${OLD_BASEDUMP_DATE}+1 year")
NOW=$(date '+%C%y%m%d')

if [ $NOW -gt $OLD_BASEDUMP_DATE_PLUS_ONE_YEAR ]; then
  mail -s "Zeit fuer neuen Pica basedump fuer lobid-organisations" "${MAIL_TO}" -a "From: ${MAIL_FROM}" << EOF
Getriggert und ausgeführt in $(pwd)/checkIfBaseDataShouldBeUpdated.sh :

Es ist Zeit sich von der ZDB einen neuen binary PICA Sigel basedump zu holen.
Siehe https://github.com/hbz/lobid-organisations/issues/504.

EOF
fi


