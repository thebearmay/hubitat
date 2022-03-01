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
 *    01Mar2022     thebearmay    Add message for any hub mesh device a note is attached to (meshed devices won't retain note)
 */

static String version()	{  return '1.0.1'  }


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
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        if (app.getInstallationState() == 'COMPLETE') {   
            section("Main"){
                input "qryDevice", "capability.*", title: "Devices to Add Notes to:", multiple: true, required: false, submitOnChange: true
                if(qryDevice){ 
					input "custNote", "text", title: "Custom Note Text", required: false, submitOnChange: true
                    input "noteName", "text", title: "Custom Note Name (no special characters)", required: false, submitOnChange:true
                    if(noteName != null) checkName()
					if(custNote && checkName)
                        input "addNote", "button", title: "Update Note", width:4
					input "remNote", "button", title: "Remove Note", width:4
                    input "debugEnabled", "bool", title: "Enable Debug", defaultValue: false, submitOnChange:true                  
		        }
				
		    }
            section("Update Messages", hideable:true, hidden: false){
                paragraph "$atomicState.meshedDeviceMsg"
            }
            section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
            }            
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

boolean checkName() {
    if(noteName == null) app.updateSetting("noteName",[value:"customNote",type:"text"])
    if(debugEnabled) log.debug toCamelCase(noteName)
    app.updateSetting("noteName",[value:toCamelCase(noteName),type:"text"])
    return true
}

def toCamelCase(init) {
    if (init == null)
        return null;

    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

def appButtonHandler(btn) {
    switch(btn) {
	case "addNote":
	    if(!custNote) break
        atomicState.meshedDeviceMsg = ""
		qryDevice.each{
            it.updateDataValue(noteName, custNote)
            if(it.driverType == "link") {
                atomicState.meshedDeviceMsg+="<span style='background-color:red;font-weight:bold;color:white;'>$it is a Hub Mesh Device, note must be added to the <i>REAL</i> device to be retained</span><br>"
            }
		}
        if(atomicState.meshedDeviceMsg == "") atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
		break
	case "remNote":
		qryDevice.each{
			it.removeDataValue(noteName)
		}
		break	
    default: 
		log.error "Undefined button $btn pushed"
		break
	}
}
def intialize() {

}
