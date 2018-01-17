sh run.sh | sed 's/<!-- end record -->//g' |  xmllint --format - | xsltproc remove_empty.xslt - 
