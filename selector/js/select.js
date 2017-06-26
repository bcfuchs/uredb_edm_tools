! function() {
/** 
    selection builder/controller
*/
    var builder = (function (){

	var config = {
	    seljson:  "data/sel.json", 
	    templateSel : "#form-template",
	    itemSel: ".item",
	    innerSel: ".innerItem"

	}
	function build_grid(){
	    // get template
	    var t = $(config.templateSel).clone().attr("id","");
	    this.item = $(t).find(config.itemSel).remove()[0];
	    // get innerItem
	    this.inner = $(this.item).find(config.innerSel).remove()[0];
	    for (var i  = 0;i < 10; i++) 
		this.add_inner("df"+i,"/img/test.png",true);

	    console.log(this.item);
	}

	function add_inner(id,src,checked) {
	    var inner = $(this.inner).clone().attr("id",id);
	    $(inner).find("img").attr("src",src).attr("alt",src);
	    $(inner).find(".thumb-select");
	    if (checked === true) {
		$(inner).attr("checked",true);
	    }
	    $(this.item).append(inner);

	}
	
	return {

	    build_grid:build_grid,
	    add_inner:add_inner,
	    config:config
	}
		
		   
	})();
		   $(document).ready(function(){
		       builder.build_grid();
		   });
}();
    




