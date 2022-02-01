/* Contact Sensor Close

 */

static String version()	{  return '0.0.1'  }


definition (
	name: 			"Custom Device Note", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Force/Send close event with optional button.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/custDevNote.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
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
                input "qryDevice", "capability.*", title: "Devices to Add Notes to:", multiple: true, required: true, submitOnChange: true
                if(qryDevice){ 
					input "custNote", "text", title: "Custom Note Text", required: true, submitOnChange: true
					input "addNote", "button", title: "Update Note"
		}
				
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



def appButtonHandler(btn) {
    switch(btn) {
		case "addNote":
			qryDevice.each{
				it.state.customNote = custNote
			}
			break
        default: 
			log.error "Undefined button $btn pushed"
            break
	}
}
def intialize() {

}
