! function(){
    $(document).ready(function(){
	var rchange = function(e) {
	    console.log(e.currentTarget);
	}
	$('.picsel').each(function(k,v) {
	    $(v).change(rchange);
	    
	});
	    
    })
}()
