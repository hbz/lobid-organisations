# see #482

OUTPUT_FILE="./conf/wikidataLookup.tsv"
OUTPUT_FILE_TMP=${OUTPUT_FILE}".tmp"

# Items that have an ISIL or DBS ID, add GND ID if existing
curl --header "Accept: text/tab-separated-values" -G 'https://query.wikidata.org/sparql' --data-urlencode query='
SELECT ?item ?itemLabel ?isil ?gndId ?dbsId
WHERE
{
  { ?item wdt:P791 ?isil } # Give back entries that either have an ISIL
      UNION                   # or
     { ?item wdt:P4007 ?dbsId . } # a DBS ID 
     OPTIONAL { ?item wdt:P227 ?gndId . } # Add GND ID if in Wikidata.
    SERVICE wikibase:label { bd:serviceParam wikibase:language "de,en". }
}
' |sed 's#<##g' | sed 's#^"##g'|sed 's#"\t<#\t#g'|sed 's#>\t"#\t#g' |sed 's#"@..#\t#g'  |sed 's#\t"#\t#g' |sed 's#"\t#\t#g' |sed 's#"\^\^.*##g' |sed 's#\t\t#\t#g' |sed 's#"$##g' > $OUTPUT_FILE_TMP

if [ -s $OUTPUT_FILE_TMP ]; then
        mv $OUTPUT_FILE_TMP $OUTPUT_FILE
fi

