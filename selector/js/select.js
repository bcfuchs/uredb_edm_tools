! function() {
/** 
    selection builder/controller
*/
    var builder = (function (){

	var config = {
	    seljson:  "data/choices.json",
	    templateSel : "#form-template",
	    frameSel: "#choice-frame",
	    radioSel: ".thumb-select",
	    thumb_titleSel: ".thumb-title",	    
	    itemSel: ".item",
	    innerSel: ".innerItem"

	};



         function build_grid(data){

	     // get group template

	     var t = $(config.templateSel).clone().attr("id","");
	     
	     var this_item = $(t).find(config.itemSel).remove()[0];
	     // get innerItem
	     
	     var this_inner = $(this_item).find(config.innerSel).remove()[0];

	     for (var i =0,z = 10;i < z; i++) {
		 
		 var id = data[i]['id'];
		 var group_id = id;
		 var title = data[i]['title'];
		 var link= data[i]['link'];
		 var raw_items = data[i]['items'];
		 var item  = $(this_item).clone().attr("id",id);
		 $(item).find(".title").html(title);

		 $(item).find("a.item-link").attr('href',link)
		 $(item).find("a.item-link").html(title);

		 //		 console.log(item[0]);
		 var items= [];
		 for (var j  = 0,q = raw_items.length;j < q; j++)  {
		     if (!(raw_items[j]['link'].match(/null/))) {
			 items.push(raw_items[j])
		     }
		 }
		 if (items.length > 0) {
		     for (var j  = 0,q = items.length;j < q; j++)  {
			 var checked = false;
			 var inner_id = items[j]['id']
			 var inner_url = items[j]['link']
			
			 if (j === 0)
			     checked = true;
			 var inner = get_inner(inner_id,inner_url,checked,this_inner,group_id);
			 
			 if ( !(inner_url.match(/null/)) ) {
			     $(item).append(inner);
			 }
		     }
		 }
//		 console.log(item[0]);
//		 console.log(data[i]);
		 $(config.frameSel).append(item[0])
	 }
	     set_listener();
	 }
	
	function set_listener(){
	    var f = function() {

		var m = $(this).attr("id");
		var name = $(this).attr("name");
		console.log("changed thumb for " + name + " to " + m);
	    }
	    $(config.radioSel).change(f);

	}
	function get_inner(id,src,checked,this_inner,group_id) {
	    var inner = $(this_inner).clone().attr("id",id);
	    $(inner).find("img").attr("src",src).attr("alt",src);
	    
	    //	    console.log(inner);
	    $(inner).find(config.radioSel).attr("id",id);
	    $(inner).find(config.thumb_titleSSel).attr("id",id);
	    $(inner).find(config.radioSel).attr("name",group_id);
	    if (checked === true) {
		$(inner).find(config.radioSel).attr("checked",true);
	    }
	    return inner

	}
	function init() {

	    $.getJSON(config.seljson,build_grid);
	}
	return {
	    init: init,
	    build_grid:build_grid,
	    get_inner:get_inner,
	    set_listener:set_listener,
	    config:config
	}
		
		   
	})();
		   $(document).ready(function(){
		       
		       builder.init();

	   
       

		   });
}();
    




