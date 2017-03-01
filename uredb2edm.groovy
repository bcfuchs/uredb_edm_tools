@Grab('mysql:mysql-connector-java:5.1.6')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def type = args[0];

switch(type) {
  case "ure":
    ure();
    break;
case "test":
    test()

	}




def get_templates(t) {
    def out = [:]
    t.each {k,v ->
	    out[k] = new File(v).getText();

	    }
    return out;

}
def ure() {

    def err = { m->System.err.println(m)}
    def configFile = "config.groovy";
    def cf = new ConfigSlurper('dev').parse(new File(configFile).toURL());
    def uredb = new Uredb(cf);    
    
    def edm = new Edm(templates:get_templates(cf.templates));

    def ure_uri = "http://uremuseum.org/record/";
    def print_count = {
	    def reccount = 0;
	    def reccount_max = 50
	    return {
		reccount++;
		if (reccount  % reccount_max == 0)
		    System.err.println "" + reccount + " records"
			}
    }()
    // get the edm
    /**
       edm concept 
       "http://www.eionet.europa.eu/gemet/concept/1266"

       "http://www.eionet.europa.eu/gemet/concept/4260",
       "http://www.eionet.europa.eu/gemet/concept/1266"

    */
    /**
       missing
       edm:TimeSpan
       dcterms:temporal 
       dc:subject 
     */
      println edm.prefix();
    // go through each record, get parts

     uredb.uremeta.each {rec ->

	print_count();
	def out = [];
	def date = uredb.date_correct(rec.date);
	//	println rec.date + " : " + date;
	def accnum = rec.accession_number;
	def uri = ure_uri + accnum;
	def place = {
	    def a = uredb.get_place(accnum);
	    if (a != null) {
		return [uri:"http://www.geonames.org/" + a.guid,
			name:a.surrogate]
	    }
	    return null
	}();
	
	def resources = {
	    def url = "http://www.eionet.europa.eu/gemet/concept/1266"
	    // switch based on field - get from config
	    
	    if (true) {

		return edm.resource([resource:url]);
	    }

	}();
	def type = "pot"; // ASK AMY!!! -- which metadata determine this??
		
	//not always a date or place
	def cho = edm.get_cho([date:date,
			       about:uri,
			       description:rec.description,
			       identifier:rec.accession_number,
			       geonames_spatial:{ if (place != null) { return place.uri} else return null}(),
			       title:rec.short_title,
			       resources:resources,
			       edm_type:type]);

	out << cho;

	def Images images = uredb.get_pix(rec.id.toString());

	images.pix.each {
	    def url = it.uri_local + "/sm/" + it.uri
	    out <<  edm.rights([wr_about:url]);

	}
	
	// place
	if (place != null)
	   	out << edm.place([uri:place.uri,location:place.name]);
	// views
	def has_views = [];
	images.pix.each {
	    has_views << edm.has_view([has_view:it.uri_local + "/sm/" + it.uri]);
	}
	
	def object_url = ure_uri + accnum;

	def isShownBy = {
	    if (images.pix && images.pix[0])
		return images.pix[0].uri_local + "/sm/" + images.pix[0].uri
		    
		    }()
	//TODO  might not have images....
	out << edm.ore_aggregation([about:object_url,
				   resource_id:object_url,
				   has_views:has_views.join(""),
				   is_shown_at:object_url,
				   is_shown_by:isShownBy]);

	def printer = {->
		       print sprintf( '%1$s\t %2$s\t %3$s\t %4$s\t', [rec.accession_number, rec.date, date,rec.description])
		       print "\n";
	};
	//	printer();
	println out.join("\n");
    }

	println '</rdf:RDF>'
}
       
       
       // construct the example
def test() {
    def edm = new Edm();
    def out = [];
    def cho = edm.get_cho([date:'date',about:'about',description:'description',identifier:"id",geonames_spatial:'geo1',
			   title:'title',resource1:'resource1',resource2:'resource2',edm_type:"IMAGE"]);
    out << cho;
    def web_resources = ['http://www.mimo-db.eu/media/UEDIN/VIDEO/0032195v.mpg',
			 'http://www.mimo-db.eu/media/UEDIN/AUDIO/0032195s.mp3',
			 'http://www.mimo-db.eu/media/UEDIN/IMAGE/0032195c.jpg'];
		     
    web_resources.each {
	out <<  edm.rights([wr_about:it]);
	
    }
    
    def concepts = [ [about:'http://www.mimo-db.eu/InstrumentsKeywords/4378',
		      label: 'Buccion'],
		     [about:'http://www.mimo-db.eu/HornbostelAndSachs/356',
		      label: '423.22 Labrosones with slides']
		     ];
    // need to get this from csv file in uredb_rdf_tools
    
    //    out << edm.place([uri:place_uri,location:place]);
    
    concepts.each {
	
	out <<  edm.skos_concept([uri:it.about,label:it.label]);
    }
    
    out = edm.prefix()+out.join("")+'</rdf:RDF>';
    println out;
}	



/** 
 * static template methods for creating edm xml
 */
class Edm {

    //    def engine = new groovy.text.XmlTemplateEngine();
    Map templates = [:]
    def engine = new groovy.text.GStringTemplateEngine();
    def license = "http://creativecommons.org/licenses/by-nc-sa/3.0/";
    def gin = [:]
	
    def place(data) {
	return _doTemplate(this.templates.place,data);	    
    }
	
    def has_view(data) {
	def text = ''' <edm:hasView rdf:resource="$has_view"/>'''
	return _doTemplate(text,data);
    }
    
    def ore_aggregation(data) {
	data.license = this.license
	return _doTemplate(this.templates.ore_aggregation,data);
    }

    def skos_concept(data) {
	    return _doTemplate(this.templates.skos_concept,data);	
    }

    def resource(data){
	def text = '''<dc:type rdf:resource="${resource}"/>''';
	return _doTemplate(text,data);
    }
    
   def rights(data) {
       data.license = this.license;
       return _doTemplate(this.templates.rights,data);

    }
    
    def get_cho(binding){
	// multiple rdf resources...
        //  no spatial el if no spatial data
	binding['geo_inset'] = {

		   if (binding.geonames_spatial == null)
		       return "";
		   def t = '''    <dcterms:spatial rdf:resource="${geonames_spatial}"/>'''
		   return	_doTemplate(t,[geonames_spatial:binding['geonames_spatial']]);
	}
	    
	return _doTemplate(this.templates.cho,binding);	    
	
    }

    def _doTemplate(text,binding){
	
	if (! this.gin[text]) {
	    this.gin[text] = engine.createTemplate(text)		
	}
	return this.gin[text].make(binding);

    }
    def prefix() {
return '''
<rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/" xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdaGr2="http://rdvocab.info/ElementsGr2/" xmlns:oai="http://www.openarchives.org/OAI/2.0/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:dcterms="http://purl.org/dc/terms/">
'''


    }
}
class Images {
    def String thumb;
    def String small;
    def String large;
    def List pix;
    def boolean hasPic;


}
class Uredb {
    Map cf;
    Map places;
    List  uremeta;
    Map accnum2media;
    List uremeta_media;
    Sql  sql;

    
    Uredb(cf) {
	  this.cf = cf;
	  
	  _load();
	  //	  _make_media_dict();
	      
      }


    // parse date and re-format
    def String date_correct(String date) {
	/**
	    500-450  500-450 BCE
	    1700-1750 1700-1750 CE
	    6 c     600-500 BCE
	    14th c 1400-1300 BCE
	    6 c or later   600-? BCE
	    

	*/

    def  check;

    //     5-6 c. AD : 5-6 c. AD CE
    check = (date=~/(?i)(\d+)-(\d+) c\. AD/)
    if (check) {
	def date1,date2
	date1 = check[0][1] + "00"
	date2 = check[0][2] + "00"
	return date1 + "-" + date2 + " CE";
	   
    }
    // 16-15 c. 
    check = (date=~/(?i)(\d+)-(\d+) c/)
    if (check) {
	def date1,date2
	date1 = check[0][1] + "00"
	date2 = check[0][2] + "00"
	return date1 + "-" + date2 + " BCE";
	   
    }

    // 500-400
    // 1939-45
    // 5-600
    check = (date=~/(\d+)-(\d+)/)
    if (check) {
	def date1,date2
	date1 = check[0][1]
	date2 = check[0][2]
	// 5-600
	if (date1.toInteger() < 20) {
	    date1 += "00"


	}
	
	else {
	    if (date1.size() > date2.size()) 
		date2 += "00"
	    date = date1+"-"+date2
	    
	}
	return  (date1.toInteger() > date2.toInteger())? date + " BCE": date + " CE"
	   
    }

    // 14th c. 
    check =  (date =~/(?i)(\d+)(\D+)\sc/);
    if (check) {
	def date1,date2, new_date1;
	date1 = check[0][1];
	date2   = date1.toInteger() - 1  ;
	new_date1 = date1 + "00";
	return new_date1 + "-" +date2 + "00 BCE"
    }
    
    // 6 c. or later
    check =  (date =~ /(?i)(\d+)\D+or later/);
    if (check) {

	def date1,date2, new_date1;
	date1 = check[0][1];
	new_date1 = date1 + "00";
	return new_date1 + "-?"

      }
    
    // 6 c
    check =   (date =~/(?i)(\d+)\sc/);
    if (check) {
       	def date1,date2, new_date1;
	date1 = check[0][1];
	date2   = date1.toInteger() - 1  ;
	new_date1 = date1 + "00";
	return new_date1 + "-" +date2 + "00 BCE"
	
    }

    


    
    if (date == " " )
	date = null
    if (date != null && date != "" ) {

    date  += " BCE"
    }
    return date

	}
    def _make_media_dict() {
	    /**
	 uremeta_media_id: 22
         media_id: 20609
	     */

	uremeta_media.each {

	    if ( ! this.accnum2media[it.uremeta_media_id] ) 
		this.accnum2media[it.uremeta_media_id] = [];
		
	    def uri = sql.firstRow('select uri from media where id="' + it.media_id+'"')[0];
	    def uri_local = sql.firstRow('select uri_local from media where id="' + it.media_id+'"')[0];
	    this.accnum2media[it.uremeta_media_id] << [uri:uri,uri_local:uri_local];

	}

    }
    def _load() {

	  sql = Sql.newInstance(cf.db.url,cf.db.user, cf.db.password, cf.db.driver);		
	  this.uremeta = sql.rows('select * from uremeta');
	  this.uremeta_media = sql.rows('select * from uremeta_media');
	  
	  // load places
	  def slurper = new groovy.json.JsonSlurper();
	  places = slurper.parse(new File(cf.places.file));
	  	  // load id 2 pix
	  
	  this.accnum2media = slurper.parse(new File(cf.id2media.file));    

      }


    
    def get_place(accnum) {
	if (places[accnum]){
	    return places[accnum][0]
	}
	return null;
    }
	    
    def get_pix(id) {
	Images im;

	if (this.accnum2media[id]) {
	    
	    def t =  this.accnum2media[id];
	    def thumb = t[0].uri_local + "/thumb/"+ t[0].uri;
	    def small  = t[0].uri_local + "/small/"+ t[0].uri;
	    def large = t[0].uri_local + "/large/"+ t[0].uri;
	    im = new Images(thumb:thumb,large:large,pix:t,hasPic:true);
	    

	}
	else {

	    im = new Images(hasPic:false);
	}
	return im;
    }
}
