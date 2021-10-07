/* Contact Sensor Close

 */

static String version()	{  return '0.0.1'  }


definition (
	name: 			"Contact Sensor Close with Button", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Force/Send close event with optional button.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/contactCloseButton.groovy",
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
	    	section("Main")
		    {
                input "qryDevice", "capability.contactSensor", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                input "createChild", "bool", title: "Create Button Device?", defaultValue: false, submitOnChange: true
                if(createChild) {
					addDevice()
                } else {
                    removeDevice()
                }				
                if (qryDevice != null) href "deviceCharacteristics", title: "Send Close Event", required: false
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
		    closeContacts()
        qryDevice.each{
          paragraph "<p>${it.displayName} ${it.currentValue('contact')}</p>"
        }
      }
    }
}

def closeContacts(evt = "pushed"){
	qryDevice.each{
		it.sendEvent(name:"contact",value:"closed",isStateChange:true)
	}
}

def addDevice() {
    if(!this.getChildDevice("cscButton001")){
      	addChildDevice("hubitat","Virtual Button","cscButton001",[name:"Close Contact Button"])
	subscribe(this.getChildDevice("cscButton001"), "pushed", "closeContacts")
    }
}

def removeDevice(){
	unsubscribe()
  deleteChildDevice("cscButton001")
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
