sh run.sh | sed 's/<!-- end record -->//g' |  xmllint --format - | xsltproc re2.xslt - 
