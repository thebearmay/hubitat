/* Device Custom Note
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date          Who           What
 *    ----          ---           ----
 */

static String version()	{  return '0.0.3'  }


definition (
	name: 			"Custom Device Note", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Add a custom note to any device.",
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
                input "qryDevice", "capability.*", title: "Devices to Add Notes to:", multiple: true, required: false, submitOnChange: true
                if(qryDevice){ 
					input "custNote", "text", title: "Custom Note Text", required: false, submitOnChange: true
					input "addNote", "button", title: "Update Note"
					input "remNote", "button", title: "Remove Note"
		}
				
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def appButtonHandler(btn) {
    switch(btn) {
	case "addNote":
		qryDevice.each{
			it.updateDataValue('customNote', custNote)
		}
			break
	case "remNote":
		qryDevice.each{
			it.removeDataValue('customNote')
		}
		break	
    default: 
		log.error "Undefined button $btn pushed"
		break
	}
}
def intialize() {

}
