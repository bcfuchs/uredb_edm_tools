@Grab('mysql:mysql-connector-java:5.1.44')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import groovy.transform.Field;

@Field cf; //config
@Field  no_title; // log file for records without title
@Field record_uri;  // db uri;
@Field processed;  // log file for acc nums

def contactFilename;
def choices;
def errlog = { m-> System.err.println(m) }
    
// get config

def configFile = "config.groovy";
cf = new ConfigSlurper('dev').parse(new File(configFile).toURL());
def db = new ConfigSlurper('dev').parse(new File(cf.db.configfile).toURL());
cf.db = db.db;

// pg2geo
def p2g =  {
    def out = [:]
    // load the file
    
    new File(cf.places.p2g).splitEachLine(",") {fields ->
	out[fields[0]] = fields[1] 


	    }
    return out;
}()
    



// test or for real?
def type = args[0];

//  thumbnail log
if (args.size() > 1) {
    contactFilename = args[1];
}
else {
    contactFilename = cf.data.contacts_file
}

def cFile = new File(contactFilename).newOutputStream();

choices = ( new groovy.json.JsonSlurper()).parse(new File(cf.data.choices_file));


no_title = new File(cf.log.no_titles_file);
record_uri = cf.urls.record_uri;
processed = new File(cf.log.processed_file);

def wipe(files) {

    files.each { file->
	    if (file.exists()){
		RandomAccessFile raf = new RandomAccessFile(processed, "rw");
		raf.setLength(0);
	    }
    }
    
}


wipe([no_title,processed]);

processed << "[";

switch(type) {
  case "ure":
      ure(cFile,choices,p2g);
    break;
  case "test":
      test();
      
}

processed <<'""]';

def get_templates(t) {
    def out = [:]
    t.each {k,v ->
	    out[k] = new File(v).getText();

	    }
    return out;

}


// return true if record matches a filter
def record_filters(rec) {

    if (rec.short_title =~ /^\s*$/) {
	no_title << '<a href="'+record_uri + rec.accession_number + '">' + rec.accession_number+ "</a>\n";
	return true
	    }
    if (rec.accession_number =~ /REDMG/) 
	return true
     
	   
    return false;


}

def ure(cFile,choices,p2g) {


    def err = { m->System.err.println(m)}

    def uredb = new Uredb(cf);    
    // load templates from config.groovy into edm
    def edm = new Edm(templates:get_templates(cf.templates));
    def ure_uri = cf.urls.record_uri;
    def print_count = {
	    def reccount = 0;
	    def reccount_max = 50
	    return {
		reccount++;
		if (reccount  % reccount_max == 0)
		    System.err.println "" + reccount + " records";
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
	 // rdf prefix with namespaces
	 println edm.prefix();
    // go through each record, get parts, print as xml
    
    uredb.uremeta.each {rec ->
	    def shouldPrint = true; // print if true -- set to false if no images.
	    print_count(); // progress
	    // skip rec  if it matches a filter
	    
	    if (record_filters(rec)) {
		shouldPrint = false

	    }
	def out = []; // string array containing parts to print

	// fix the date
	def date = uredb.date_correct(rec.date);
	def accnum = rec.accession_number;
	processed << '"' + accnum + '",\n '
	def uri = ure_uri + accnum;
	def identifier = '#' +accnum;
	// fix description
	def description = uredb.string_correct(rec.description);
	def provenance = rec.provenience;
	
	// get the placename from pelagios data
	def place = {
	    def a = uredb.get_place(accnum);

	    if (a != null) {
		def place_out = []


		// make 2 entries if there is a geoname
		if (p2g["http://pleiades.stoa.org/places/" + a.guid]) {

		    def geonames = p2g["http://pleiades.stoa.org/places/" + a.guid]
			place_out << [uri:geonames,name:a.surrogate]
			
			}	       
		else {
		    System.err.println "NO geo " + accnum;
		}
		
		place_out << [uri:"http://pleiades.stoa.org/places/" + a.guid,
			name:a.surrogate]
		    return place_out;
	    }
	    return null;
	}();
	def resource_urls = [
			     //	    'ceramics':"http://www.eionet.europa.eu/gemet/concept/1266"
			     'ceramic ware (visual works)':"http://vocab.getty.edu/aat/300386879"
	]
	def resources = {
	    
	 
	    if (rec.material =~ /Terracotta/ ) {
		def name = 'ceramic ware (visual works)';
		return edm.resource([resource:resource_urls[name],name:name]);

	    }
	    
	    if (rec.material =~ /Coarse/ ) {
		def name = 'ceramic ware (visual works)';
		System.err.println "       >>" + rec.material + " " + rec.artist
		return edm.resource([resource:resource_urls[name],name:name]);

	    }
	    
	    return ""
	}();
	
	def type = "IMAGE"; // ASK AMY!!! -- which metadata determine this??
		
	//not always a date or place
	def cho = edm.get_cho([date:date,
			       about:identifier,
			       // Shape--- dc:type
			       //Material Terracotta  dcterms:medium
			       // Condition dc:description
			       // Provenance  dcterms provenance
			       provenance:provenance, 
			       // Period dcterms:temporal
			       
			       description:description,
			       identifier:identifier,
			       //			       identifier:rec.accession_number,
			       geonames_spatial: place,
			       title:uredb.string_correct(rec.short_title),
			       resources:resources,
			       edm_type:type]);

	out << cho;
	// place
	
	
	if (place != null) {
	    for (int i = 0; i < place.size; i++) {
		out << edm.place([uri:place[i].uri,location:place[i].name]);
	    }
	}
	// get images from db
	def Images images = uredb.get_pix(rec.id.toString());

	// image rights
	images.pix.each {
	    
	    if (! (it.uri_local =~ /null/)) {
		

		url = it.uri_local + "/xlarge/" + it.uri
	        out <<  edm.rights([wr_about:url]);
	    }

	}
	// thumbnail rights
	if (images.pix &&  images.pix[0]) {
		def url = images.pix[0].uri_local + "/sm/" + images.pix[0].uri;
		out <<  edm.rights([wr_about:url]);	
	    }

	// views 
	def has_views = [];
	images.pix.each {
	    if (!(it.uri_local =~ /null/)) {
		has_views << edm.has_view([has_view:it.uri_local + "/xlarge/" + it.uri]);
	    }
	    }
	if (!images.pix || !images.pix[0].uri_local)
	    shouldPrint = false;
   


	def isShownBy = {
	    // TODO -- now defined by choices.json
	    if (images.pix && images.pix[0]) {


	       def pre_url = images.pix[0].uri_local;
	       def post_url = images.pix[0].uri;
	       if (choices[accnum]) {

		   // get the image
		  images.pix.each {
		      if ( it.uri == choices[accnum]) {
			  pre_url = it.uri_local;
			  post_url = it.uri;
			  //			  System.err.println "================>"+post_url;
		      }
		      
		   }
	       }
	       return images.pix[0].uri_local + "/sm/" + images.pix[0].uri
	    }
	    else {
		return ""
	    }
		    }()
			 
	def object_url = isShownBy
			 
			 //TODO  might not have images....
			 out << edm.ore_aggregation([about:uri,
				   resource_id:identifier,
				   has_views:has_views.join(""),
				   is_shown_at:uri,
				   is_shown_by:isShownBy]);

	out << '<!-- end record -->';
	cFile <<  accnum + "\t" + isShownBy + "\n"; 	    
	if (shouldPrint) {
	    println '<record>';

	    println out.join("\n");
	 
	    println '</record>';		
	}
	}

	println '</rdf:RDF>'
	    cFile.close();
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
 *  template methods for creating edm xml
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

	    // return "" if no geo info
	    if (binding.geonames_spatial == null)
		return "";

	    def out = []

	    binding.geonames_spatial.each { d->
		    def t = '''<dcterms:spatial rdf:resource="${geonames_spatial}"/>'''
		    out << _doTemplate(t,[geonames_spatial:d.uri]);
		    }

	    
	    return out.join("\n")
	}()

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
<rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" 
         xmlns:dcterms="http://purl.org/dc/terms/"
         xmlns:edm="http://www.europeana.eu/schemas/edm/" 
         xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#" 
         xmlns:oai="http://www.openarchives.org/OAI/2.0/" 
         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
         xmlns:ore="http://www.openarchives.org/ore/terms/" 
         xmlns:skos="http://www.w3.org/2004/02/skos/core#">
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
    Map choices;
    Sql  sql;

    
    Uredb(cf) {
	  this.cf = cf;
	  
	  _load();
	  //	  _make_media_dict();
	      
      }

    // various fixes for description typos
    def String string_correct(String desc) {
	def out = desc;
	if (desc != null) 
	    out = desc.replaceAll("\\\\", "")

	return	out;


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
	def hasPix = false;
	if (this.accnum2media[id]) {
	    def t =  this.accnum2media[id];
	    if (!(t[0].uri_local =~ /null/)) {
		hasPix = true;
	    }
	    else {

	    }
	}
	
	if (hasPix) {

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
