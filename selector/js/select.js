! function() {
/** 
    selection builder/controller
*/
    var chooser = (function (){
	var choices = {};
	var isInit = true;
	var cursor = 0; // where we are in pagination.
	var total = 0;
	var this_data;  // current data
	var config = {
	    syncFromRemoteOnInit: true, // sync server to localstorage on start up
	    //	    seljson:  "data/choices.json", // where to get the data.
	    seljson:  "data/choices.json", // where to get the data.
	    get_endpoint: "/api/selected_thumb", // endpoint to load choices when sync on init is true
	    endpoint: "/api/selected_thumb",  // endpoint to send json from a choice to
	    templateSel : "#form-template",
	    frameSel: "#choice-frame",
	    radioSel: ".thumb-select",
	    thumb_titleSel: ".thumb-title",	    
	    save2file_sel: "#save2file",
	    selectedClass: "selected",
	    itemSel: ".item",
	    paginateLink: ".paginate-link",
	    linkdivSel: "#page-links",
	    itemsPerPage: 100, // items to display per page
	    innerSel: ".innerItem",
	    localStoreName : "thumb_select"
	    
	};



         function build_grid(data){
	     // set data;
	     this_data = data;
	     console.log("data size :" + this_data.length);

	     // set total
	     total = data.length;


	     // get the local data

	     readLocal();

	     
	     // get group template

	     var t = $(config.templateSel).clone().attr("id","");
	     
	     var this_item = $(t).find(config.itemSel).remove()[0];
	     // get innerItem
	     
	     var this_inner = $(this_item).find(config.innerSel).remove()[0];
	     var f_cursor = cursor;
	     for (var i = f_cursor,z = config.itemsPerPage+ f_cursor;i < z; i++) {
		 
		 var id = data[i]['id'];
		 var group_id = id;
		 var selected_thumb = null;
		 var title = data[i]['title'];
		 var link= data[i]['link'];
		 var raw_items = data[i]['items'];
		 var item  = $(this_item).clone().attr("id",id);
		 $(item).find(".title").html(title);

		 $(item).find("a.item-link").attr('href',link)
		 $(item).find("a.item-link").html(id);


		 var items= [];
		 // get rid of nulls
		 for (var j  = 0,q = raw_items.length;j < q; j++)  {
		     if (!(raw_items[j]['link'].match(/null/))) {
			 items.push(raw_items[j])
		     }

		 }
		 // get the selected id if there is one
		 if (id in choices) {
		     selected_thumb = choices[id]
		 }
		 
		 // only do ones where there's a choice.
		 if (items.length > 1) {
		     for (var j  = 0,q = items.length;j < q; j++)  {
			 var checked = false;
			 var inner_id = items[j]['id']
			 var inner_url = items[j]['link']
			 // if no item has been selected by hand yet, check first one
			 if ( selected_thumb  === null) {
			     if (j === 0) {
				 checked = true;
			     }
			 }
			 // if an item has been selected, check that
			 else {
			     if (inner_id === selected_thumb) {

				 checked = true;
			     }

			 }


			 var inner = get_inner(inner_id,inner_url,checked,this_inner,group_id);
			 
			 if ( !(inner_url.match(/null/)) ) {
			     $(item).append(inner);
			 }
		     }
		 }

		 $(config.frameSel).append(item[0])
	 }
	     set_listener();
	     if (isInit === true)
	     {
		 // set the save link
		 $(config.sev2file_sel).click(save2file);
		 paginate();
	     }
	     highlight();
	     isInit  = false;
	 }
	function save2file() {
	    
	    var c = JSON.stringify(choices);
	    var blob = new Blob([c], {type: "text/plain;charset=utf-8"});
	    saveAs(blob, "europeana_choices.txt");

	}
	function paginate() {

	    for (var i = 0; i < total;) {

		var thisPage = i+1;
		var nextPage = thisPage + config.itemsPerPage
		var link = $('<span class="paginate-link" data-start="'+i+'">'+ thisPage + "-" + nextPage + '</span>');
		// set the underscore
		if (i === 0 )
		    $(link).addClass("underscore");
		$(config.linkdivSel).append(link);
		i = i + config.itemsPerPage;
	    }

	    // set the click
	    var f = function() {
		var itemNumber = $(this).data('start');
		$(config.paginateLink).removeClass("underscore");
		// underscore clicked link
		$(this).addClass("underscore");

		go_to_page(itemNumber);
	    }

	     $(config.paginateLink).click(f);


	}
	function readLocal() {
	    var name = config.localStoreName;
	    if (localStorage.getItem(name)) {
		choices = JSON.parse(localStorage.getItem(name));
		
	    }

	    

	}

	function readremote() {


	    

	}
	function save2local_bulk(data) {
	    var name = config.localStoreName;
	    localStorage.setItem(name,JSON.stringify(choices));
	}
	// 
	
	function save2local(selected_id,group_id) {
	    console.log("changed thumb for " + group_id + " to " + selected_id);
	    console.log(" 2 changed thumb for " + selected_id + " to " + group_id);
	    var name = config.localStoreName;
	    readLocal();

	    choices[group_id] = selected_id
	    localStorage.setItem(name, JSON.stringify(choices));
	    // TODO option in config. 
	    //	    save2remote();
	    // Only report current change to remote.
	    save1change2remote(selected_id,group_id);
	}
	
	function save1change2remote(inner_id,item_id) {
	    $.ajax({
		contentType : "application/json; charset=utf-8",
		url : config.endpoint,
		dataType : "json",
		type : "POST",
		data : JSON.stringify({item:item_id,choice:inner_id})
	    });
    

	}
	function save2remote() {
	    $.ajax({
		contentType : "application/json; charset=utf-8",
		url : config.endpoint,
		dataType : "json",
		type : "POST",
		data : JSON.stringify(choices)
	    });

	}
	
	function save_change(selected_id, group_id) {

	    save2local(selected_id,group_id);
	    // 
	    var postUrl = "/"
	    // $.post(postUrl,"hi");
	}
	function set_listener(){
	    var f = function() {
		$(this).parent().parent().find(".selected").removeClass("selected");
		console.log( $(this).parent().parent().find(".selected")[0]);
		var selected_id = $(this).attr("id");
		var group_id = $(this).attr("name");
		$(this).parent().addClass("selected");
		save_change(selected_id,group_id);
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
	// add css selector to parent of  the checked radio button
	function  highlight(){

	    var a = $(config.radioSel+":checked");
	    $(a).each(function(k,v) {
		$(v).parent().addClass(config.selectedClass);;

	    })
	    
	}
	
	function go_to_page(itemNumber) {
	    // wipe the html 
	    $(config.frameSel).html("");
	    // set the cursor
	    cursor = itemNumber;
	    
	    // re-use data . 
	    build_grid(this_data)

	}
	function init() {
	    if (config.syncFromRemoteOnInit === true) {
		$.getJSON(config.get_endpoint,function(data) {

		    // load choices first
		    choices = data;
		    save2local_bulk(data);
		    //  build
		    $.getJSON(config.seljson,build_grid);
		})
	    }
	    else {
		$.getJSON(config.seljson,build_grid);
	    }
	    }
	return {
	    init: init,
	    build: init,
	    build_grid:build_grid,
	    paginate: paginate,
	    save2file:save2file,
	    readLocal: readLocal,
	    readRemote: readremote,
	    save2local: save2local,
	    save2local_bulk: save2local_bulk,
	    save1change2remote: save1change2remote,
	    save2remote: save2remote,
	    save_change: save_change,
	    set_listener: set_listener,
	    get_inner: get_inner,
	    go_to_page: go_to_page,
	    get_inner:get_inner,
	    highlight:highlight,
	    set_listener:set_listener,
	    config:config
	}
		
		   
	})();
    $(document).ready(function(){
	
	chooser.build();

	$("#guide-toggle").click(
	    function(){
		$("#instructions").slideToggle();
		
	    });


	
    });
}();
    




