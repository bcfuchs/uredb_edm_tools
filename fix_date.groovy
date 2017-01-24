
System.in.withReader { console ->
	console.eachLine { line ->
	    println date_correct(line);
	    }
}

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

	date  += " BCE";
    }
    return date;
	
}
