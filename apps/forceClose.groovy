/*

 */

static String version()	{  return '0.0.0'  }


definition (
	name: 			"Force Close", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Logic Check .",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/forceClose.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "deviceCharacteristics"
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
	   section("Main") {
                input "qryDevice", "capability.contactSensor", title: "Contact Sensors of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null) href "deviceCharacteristics", title: "Send Close Event", required: false
                input "qryDevice2", "capability.motionSensor", title: "Motion Sensors of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice2 != null) href "deviceCharacteristics", title: "Send Inactive Event", required: false
	   }
	} else {
	   section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
	   }
	}
    }
}

def deviceCharacteristics(){
    dynamicPage (name: "deviceCharacteristics", title: "", install: false, uninstall: false) {
	  section(""){
	  	if (qryDevice != null) {
          		qryDevice.each{
              			it.sendEvent(name:"contact",value:"closed",isStateChange:true)
              			paragraph "<p>${it.displayName} ${it.currentValue('contact')}</p>"
          		}
		}
	  	if (qryDevice2 != null) {
			qryDevice2.each{
              			it.sendEvent(name:"motion",value:"inactive",isStateChange:true)
              			paragraph "<p>${it.displayName} ${it.currentValue('motion')}</p>"
          		}
		}
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
