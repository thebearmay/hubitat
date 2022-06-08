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
 */

static String version()	{  return '0.0.2'  }


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
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        if (app.getInstallationState() == 'COMPLETE') {   
            section("Main"){
                app.removeSetting("noteName")
                app.removeSetting("custNote")
                if(state.updFlag || state.lastDev != qryDevice?.displayName){
                    settings.each{
                        if("$it.key".indexOf("dv") == 0) {
                            app.removeSetting("$it.key")
                        }
                    }
                    state.updFlag = false
                }
                input "qryDevice", "capability.*", title: "Device to Update Data Items:", multiple: false, required: false, submitOnChange: true

                state.lastDev = qryDevice?.displayName
                if(qryDevice){ 
                    qryDevice.properties.data.each{
                        input "dv$it.key", "text",title:"<b>$it.key</b>", defaultValue: "$it.value", submitOnChange:true
                    }
                    input "updBtn", "button", title: "Update Data Items"
		        }
                if(atomicState.meshedDeviceMsg != null)
                    paragraph "$atomicState.meshedDeviceMsg"
                
                href "addNote", title: "Add new data note", required: false
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
            if(custNote  && checkName )
                input "addNote", "button", title: "Update Note", width:4            
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
		break
	case "addNote":
	    if(!custNote) break
        atomicState.meshedDeviceMsg = ""
		qryDevice.updateDataValue(noteName, custNote)

        if(qryDevice.controllerType == "LNK") {
            atomicState.meshedDeviceMsg="<span style='background-color:red;font-weight:bold;color:white;'>$it is a Hub Mesh Device, note must be added to the <i>REAL</i> device to be retained</span><br>"
		} else
            atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
		break        
    default: 
		log.error "Undefined button $btn pushed"
		break
	}
}

def intialize() {

}
