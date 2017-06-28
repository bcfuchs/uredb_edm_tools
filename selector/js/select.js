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
			 var item_id = items[j]['id']
			 var item_url = items[j]['link']
			
			 if (j === 0)
			     checked = true;
			 var inner = get_inner(item_id,item_url,checked,this_inner);
			 
			 if ( !(item_url.match(/null/)) ) {
			     $(item).append(inner);
			 }
		     }
		 }
//		 console.log(item[0]);
//		 console.log(data[i]);
		 $(config.frameSel).append(item[0])
	 }

	 }
	

	function get_inner(id,src,checked,this_inner) {
	    var inner = $(this_inner).clone().attr("id",id);
	    $(inner).find("img").attr("src",src).attr("alt",src);

//	    console.log(inner);
	    if (checked === true) {
		$(inner).find(config.radioSel).attr("checked",true);
	    }
	    return inner

	}
	
	return {

	    build_grid:build_grid,
	    get_inner:get_inner,
	    config:config
	}
		
		   
	})();
		   $(document).ready(function(){
		       
		       $.getJSON(builder.config.seljson,builder.build_grid);

	   
       

		   });
}();
    




