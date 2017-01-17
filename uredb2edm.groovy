@Grab('mysql:mysql-connector-java:5.1.6')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

ure();
//test();



def ure() {

    def configFile = "config.groovy";
    def cf = new ConfigSlurper('dev').parse(new File(configFile).toURL());
    def uredb = new Uredb(cf);
    def edm = new Edm();
    def ure_uri = "http://uremuseum.org/cgi-bin/ure/uredb.cgi?rec=";
    // get the edm

    uredb.uremeta.each {rec ->

	def out = [];

	def accession_number = rec.accession_number;
	def uri = ure_uri + accession_number;

	def place = {
	    def a = uredb.get_place(accession_number);
	    if (a != null) {
		return "http://www.geonames.org/" + a.guid
	    }
	    return null;
	}()
	

	     def type = "pot"; // ASK AMY!!!
	//not always a date or place
	def cho = edm.get_cho([date:rec.date,
			       about:uri,
			       description:rec.description,
			       identifier:rec.accession_number,
			       geonames_spatial:place,
			       title:rec.short_title,
			       resource1:'some concept resource url',
			       resource2:'some concept resource url',
			       edm_type:type]);

	out << cho;
	def Images images = uredb.get_pix(rec.id.toString());

	images.pix.each {
	    def url = it.uri_local + "/thumb/" + it.uri
	    out <<  edm.rights([wr_about:url]);

	}
		println out.join("");
    }
    
}
       
       
       // construct the example
def test() {
    def edm = new Edm();
    def out = [];
    def cho = edm.get_cho([date:'date',about:'about',description:'description',identifier:"id",geonames_spatial:'geo1',
			   title:'title',resource1:'resource1',resource2:'resource2']);
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
    def engine = new groovy.text.GStringTemplateEngine();
    def license = "http://creativecommons.org/licenses/by-nc-sa/3.0/";
    
    def place(data) {
	def text = '''
  <edm:Place rdf:about="${uri}">
    <skos:prefLabel xml:lang="en">
      ${location}
    </skos:prefLabel>
  </edm:Place>

''';
	return _doTemplate(text,data);
	    
    }
	
    def skos_concept(data) {
	def text = '''
 <skos:Concept rdf:about="${uri}">
    <skos:prefLabel xml:lang="en">${label}</skos:prefLabel>
  </skos:Concept>

'''
	return _doTemplate(text,data);

    }
	
   def rights(data) {
	def text = '''
   <edm:WebResource rdf:about="${wr_about}">
      <edm:rights rdf:resource="'''+license+'''"/>
  </edm:WebResource>
        '''
	return _doTemplate(text,data);

    }
    
    def get_cho(binding){
	// multiple rdf resources...
	// need separate method to get these. 
	def text = '''
  <edm:ProvidedCHO  rdf:about="$about">
    <dc:date>
    ${date}
    </dc:date>
    <dc:description>
      ${description}
    </dc:description>
    <dc:identifier>
      ${identifier}
    </dc:identifier>
    <dcterms:spatial rdf:resource="${geonames_spatial}"/>
    <dc:title>$title</dc:title>
    <dc:type rdf:resource="${resource1}"/>
    <dc:type rdf:resource="${resource2}"/> 
    <edm:type>${edm_type}</edm:type>
  </edm:ProvidedCHO>
'''

	    return _doTemplate(text,binding);

	    }

    def _doTemplate(text,binding){

	def template = engine.createTemplate(text).make(binding);
	return template;
    }
    def prefix() {
return '''
<rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/" xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdaGr2="http://rdvocab.info/ElementsGr2/" xmlns:oai="http://www.openarchives.org/OAI/2.0/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:dcterms="http://purl.org/dc/terms/">
'''


    }
}
class Images {
    def String thumb;
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
	println id
	if (this.accnum2media[id]) {
	    
	    def t =  this.accnum2media[id];
	    im = new Images(thumb:t[0],pix:t,hasPic:true);
	    

	}
	else {

	    im = new Images(hasPic:false);
	}
	return im;
    }
}
