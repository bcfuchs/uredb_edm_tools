@Grab('mysql:mysql-connector-java:5.1.6')
@GrabConfig(systemClassLoader=true)
import groovy.sql.Sql

def configFile = "config.groovy";
def cf = new ConfigSlurper('dev').parse(new File(configFile).toURL());
def sql = Sql.newInstance(cf.db.url, cf.db.user, cf.db.password, cf.db.driver)


def edm = new Edm();
def out = [];

def cho = edm.get_cho([date:'date',about:'about',description:'descrition',identifier:"id",geonames_spatial:'geo1',
		       title:'title',resource1:'resource1',image_uri:'image_url']);
out << cho;
def web_resources = ['http://www.mimo-db.eu/media/UEDIN/VIDEO/0032195v.mpg',
		     'http://www.mimo-db.eu/media/UEDIN/AUDIO/0032195s.mp3',
		     'http://www.mimo-db.eu/media/UEDIN/IMAGE/0032195c.jpg'];
		     
web_resources.each {
    out <<  edm.rights([wr_about:it]);

}

def concepts = [ [about:'http://www.mimo-db.eu/InstrumentsKeywords/4378',
		  label: 'Buccin'],
		 [about:'http://www.mimo-db.eu/HornbostelAndSachs/356',
		  label: '423.22 Labrosones with slides']
		 ];
// need to get this from csv file in uredb_rdf_tools

out << edm.place([uri:'http://www.geonames.org/390903',location:'Greece']);

concepts.each {

   out <<  edm.skos_concept([uri:it.about,label:it.label]);
}
    
out = edm.prefix()+out.join("")+'</rdf:RDF>'
println out

    def t = 1;


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
      <edm:rights rdf:resource="http://creativecommons.org/licenses/by-nc-sa/3.0/"/>
  </edm:WebResource>
        '''
	return _doTemplate(text,data);

    }
    
    def get_cho(binding){

	def text = '''
  <edm:ProvidedCHO  
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
    <dc:title>{$title}</dc:title>
    <dc:type rdf:resource="${resource1}"/>
    <dc:type rdf:resource="${image_uri}"/> <edm:type>IMAGE</edm:type>
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
