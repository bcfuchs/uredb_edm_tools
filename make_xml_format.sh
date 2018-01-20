sh run.sh | sed 's/<!-- end record -->//g' | sed 's/\\\//\//g' | xmllint --format - | xsltproc remove_empty.xslt - 
