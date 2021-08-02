/*

 */

static String version()	{  return '0.0.1'  }


definition (
	name: 			"Hub Information Aggregation", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides a utility to compare multiple Hub Information devices",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubInfoAggregation.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "hubAttrSelect"
   page name: "hubInfoAgg"
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
                input "qryDevice", "capability.initialize", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null){
                    hubDevCheck = true
                    qryDevice.each{
                        if(it.typeName != 'Hub Information') hubDevCheck = false
                    }
                    if(hubDevCheck) {
                        href "hubAttrSelect", title: "Select Attributes", required: true
                        href "hubInfoAgg", title: "Show Hubs", required: false
                    } else
                        paragraph "Invalid Device Selected"
                }
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def hubAttrSelect(){
    dynamicPage (name: "hubAttrSelect", title: "Hub Attribute Selection", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          dev = qryDevice[0]
          def attrList=[]
          dev.supportedAttributes.each{
              attrList.add(it.toString())
          }
          sortedList=attrList.sort()
          sortedList.each{
            input "$it", "bool", title: "$it", required: false, defaultValue: false
          }


          paragraph "<p>$strWork</p>"
       }
    }
}

def hubInfoAgg(){
    dynamicPage (name: "hubInfoAgg", title: "Hub Information Aggregation", install: false, uninstall: false) {
	  section("Aggregation Detail"){
        String htmlWork = '<table><tr><th></th>'
        for(i=0;i<qryDevice.size();i++){
              htmlWork+='<th>'+qryDevice[i].currentValue('locationName')+'</th>'
        }
        htmlWork+='</tr>'
        def attrList=[]
        qryDevice[0].supportedAttributes.each{
            if(settings["$it"] && "$it"!="locationName") attrList.add(it.toString())
            sortedList=attrList.sort()
        }
        sortedList.each{
            htmlWork+='<tr><td>'+it+'</td>'
            for(i=0;i<qryDevice.size();i++){
                htmlWork += '<td>'+qryDevice[i].currentValue("$it")+'</td>'
            }
            htmlWork+='</tr>'
        }
        htmlWork+='</table>'           
        paragraph htmlWork
      }
    }
}


def appButtonHandler(btn) {
    switch(btn) {
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

def intialize() {

}
