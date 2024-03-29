/* Device Data Edit
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
 *    08Jun2022    thebearmay    clear state when selected device changes
 *    15Jun2022    thebearmay    add the ability to remove a note
 *    14Oct2022    thebearmay    address some data retention issues when editting multiple devices
 */

static String version()	{  return '0.0.4'  }


definition (
	name: 			"Device Data Edit", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Edit the data items on a device",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devDataEdit.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "addNote"
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
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        if (app.getInstallationState() == 'COMPLETE') {   
            section("Main <p style='text-align:right;font-size:small'>Device Data Edit v${version()}</p>"){
                app.removeSetting("noteName")
                app.removeSetting("custNote")
                input "qryDevice", "capability.*", title: "Device to Update Data Items:", multiple: false, required: false, submitOnChange: true
                if(state.updFlag || state.lastDev != qryDevice?.deviceId){
                    settings.each{
                        if("$it.key".indexOf("dv") == 0) {
                            app.removeSetting("$it.key")
                        }
                    }
                    state.updFlag = false
                }
                state.lastDev = qryDevice?.deviceId
                if(qryDevice){ 
                    qryDevice.properties.data.each{
                        input "dv$it.key", "text",title:"<b>$it.key</b>", defaultValue: "$it.value", submitOnChange:true
                    }
                    input "updBtn", "button", title: "Update Data Items"
		        }
                if(atomicState.meshedDeviceMsg != null)
                    paragraph "$atomicState.meshedDeviceMsg"
                
                href "addNote", title: "Add/Remove data note", required: false
                input "debugEnabled", "bool", title: "Enable Debug", defaultValue: false, submitOnChange:true  
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

def addNote(){
    dynamicPage (name: "addNote", title: "", install: false, uninstall: false) {
        section{
            input "custNote", "text", title: "Custom Note Text", required: false, submitOnChange: true
            input "noteName", "text", title: "Custom Note Name (no special characters)", required: false, submitOnChange:true
            if(noteName != null) checkName()
            if(custNote  && checkName() ){
                input "addNote", "button", title: "Update Note", width:4
                input "remNote", "button", title: "Remove Note", width:4
            } else if (checkName()) 
                input "remNote", "button", title: "Remove Note", width:4
        }
    }
}

boolean checkName() {
    if(noteName == null) return false
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

def clearMessage(){
    atomicState.meshedDeviceMsg="."
}


def appButtonHandler(btn) {
    switch(btn) {
	case "updBtn":

		settings.each{
            if("$it.key".indexOf("dv") == 0) {
                itemKey = "$it.key".substring(2)
                itemValue = "$it.value"
                qryDevice.updateDataValue(itemKey, itemValue)
            }
		}
        state.updFlag = true
        if(qryDevice.controllerType == "LNK") {
            atomicState.meshedDeviceMsg="<span style='background-color:red;font-weight:bold;color:white;'>$qryDevice is a Hub Mesh Device, notes must be updated on the <i>REAL</i> device to be retained</span><br>"
        } else 
             atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
        runIn(30,"clearMessage")
        break
	case "addNote":
	    if(!custNote) break
        atomicState.meshedDeviceMsg = ""
		qryDevice.updateDataValue(noteName, custNote)

        if(qryDevice.controllerType == "LNK") {
            atomicState.meshedDeviceMsg="<span style='background-color:red;font-weight:bold;color:white;'>$qryDevice is a Hub Mesh Device, note must be added to the <i>REAL</i> device to be retained</span><br>"
		} else
            atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
		break
	case "remNote":
        if(!noteName) break
		qryDevice.removeDataValue(noteName)
        app.removeSetting("dv$noteName")
		break        
    default: 
		log.error "Undefined button $btn pushed"
		break
	}
}

def intialize() {

}
