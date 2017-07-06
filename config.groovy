db {

   configfile = "private/db.groovy"
}

urls {
    record_uri = "http://uremuseum.org/record/"

}
templates {
    ore_aggregation = 'templates/ore_aggregation.tmpl'
    edm_place = 'templates/edm_place.tmpl'
    skos_concept = 'templates/skos_concept.tmpl'
    cho = 'templates/cho.tmpl'
    place = 'templates/place.tmpl'
    rights = 'templates/rights.tmpl'
	}
places {

    file = "data/ure_places.json"

	}
data {
    choices_file = "selector/data/eu_choices_latest.json"    		
    contacts_file = "log/contacts.txt"
	}

log {

    no_titles_file = "log/no_titles.txt"
     processed_file = "log/processed.txt"
	

}

id2media.file = "data/id2media.json"
