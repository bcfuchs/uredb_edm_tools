@Grab('net.sourceforge.nekohtml:nekohtml:1.9.16')
import groovy.xml.XmlUtil
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import groovy.text.*;
import groovy.util.XmlParser;

/**
Create an html widget for selecting profile thumbnails for each ure item
 */

def js = '''


'''    
def mediafile = 'data/id2media.json';
def t = new File(mediafile);
def id2media = (new JsonSlurper()).parse(t);
def id2accnum = [:];
def simple = new SimpleTemplateEngine();
def url = "http://beta.uremuseum.org"
def t1 = '''
<div class="item" id="$id">
<h3>$id $accnum $title</h3>

<a href="http://beta.uremuseum.org/record/$accnum" target="_blank">$accnum</a>
<br/>
<form class="picsel">
<% pix.eachWithIndex {pic,i -> 
thumb = pic['uri_local']+ '/thumb/' + pic['uri'];
sm = pic['uri_local']+ '/sm/' + pic['uri'];
picid = pic['uri'];
def checked
if (i == 0 ) {
checked = 'checked="checked"';
}
else {
checked = '';
}
%><div class="innerItem">
<img src="$thumb"/><br/>
<input type="radio" name="gender" value="$picid" $checked/>

</div>
<% } %>
</form>
</div>
<div style="clear:left"></div>
''';

def temp1 = simple.createTemplate(t1);
def divs = []	

def ids = new File("data/id_accnum_title.tsv").splitEachLine("\t") {fields ->
    id2accnum[fields[0]] = fields;
	
    };

id2media.each {
    divs.push(template(it.key,it.value,id2accnum[it.key][1],id2accnum[it.key][2],temp1));
		
};

print JsonOutput.prettyPrint(toJson(divs))
System.exit(0);

def divs2 = divs[0..100]
def out = html(divs2,simple);

def parser = new org.cyberneko.html.parsers.SAXParser()
def xml = new XmlParser(parser);
//def x = xml.parseText(out);
print out;
//println XmlUtil.serialize(x)

def html(divs,simple) {
    def temp = '''
<html>
<head>

<script src="//code.jquery.com/jquery-1.11.3.js"></script>
<script src="/pix.js"></script>>
<style>
body {
color: white;
background: black;
}
#wrapper {
width: 1000px;
margin: auto;
}
.item {

    clear: left;
    margin-bottom: 20px;
    border-top: 1px solid white;
    margin-top: 30px;

}
.innerItem {
   display: inline;
   float: left;
   margin-right: 15px;
}

</style>
</head>
<body>
<div id="wrapper">
<% divs.each {div->%>
$div
<% } %>
</div>
</body>
</html>
''';
    def binding = [divs:divs];
    def t = simple.createTemplate(temp);
    return  t.make(binding).toString();
}

    
    

def template(id,pix,accnum,title,temp) {

    def binding = [id:id,accnum:accnum,title:title,pix:pix]
    return fill(temp,binding)

    

}
def fill(t,binding) {

    return t.make(binding).toString()

}

