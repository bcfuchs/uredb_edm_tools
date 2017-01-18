@Grab('mysql:mysql-connector-java:5.1.6')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def type = args[0];
switch(type) {
case "ure":
    ure();
case "test":
    test()
}





def ure() {

    def configFile = "config.groovy";
    def cf = new ConfigSlurper('dev').parse(new File(configFile).toURL());
    def uredb = new Uredb(cf);
    def edm = new Edm();
    def ure_uri = "http://uremuseum.org/cgi-bin/ure/uredb.cgi?rec=";
    // get the edm
    /**
       missing
       edm:TimeSpan
       dcterms:temporal 
       dc:subject 
     */
    // go through each record, get parts
    uredb.uremeta.each {rec ->

	def out = [];

	def accnum = rec.accession_number;
	def uri = ure_uri + accnum;

	def place = {
	    def a = uredb.get_place(accnum);
	    if (a != null) {
		return [uri:"http://www.geonames.org/" + a.guid,
			name:a.surrogate]
	    }
	    return null
	}()
	

	     def type = "pot"; // ASK AMY!!! -- which metadata determine this??
	
	//not always a date or place
	def cho = edm.get_cho([date:rec.date,
			       about:uri,
			       description:rec.description,
			       identifier:rec.accnum,
			       geonames_spatial:{ if (place != null) { return place.uri} else return ""}(),
			       title:rec.short_title,
			       resource1:'some concept resource url',
			       resource2:'some concept resource url',
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
	println object_url
	def thumb = {
	    if (images.thumb)
		return images.thumb;
	    return ""
		    
	}
	//TODO  might not have images....
	out << edm.ore_aggregation([about:object_url,
				   resource_id:object_url,
				   has_views:has_views.join(""),
				   is_shown_at:object_url,
				   is_shown_by:thumb]);
	out = edm.prefix()+out.join("")+'</rdf:RDF>';
	println out
    }
    
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

	    
    }
    def has_view(data) {
	def text = ''' <edm:hasView rdf:resource="$has_view"/>
'''
	return _doTemplate(text,data);


    }
    def ore_aggregation(data) {
	def text = '''

<ore:Aggregation rdf:about="$about">
  <edm:aggregatedCHO rdf:resource="$resource_id"/>
  <edm:dataProvider>Collections Trust</edm:dataProvider>
   $has_views
  <edm:isShownAt rdf:resource="$is_shown_at"/> 
  <edm:isShownBy rdf:resource="$is_shown_by"/> 
  <edm:object rdf:resource="$is_shown_by"/>
  <edm:provider>Ure Museum of Classical Archaeology</edm:provider> 
  <edm:rights rdf:resource="''' + this.license + '''"/>
</ore:Aggregation> </rdf:RDF>

'''
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
