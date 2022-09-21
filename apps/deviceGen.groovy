/*
 * Device Generator
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
 *    Date        Who           What
 *    ----        ---           ----
 *    
*/

static String version()	{  return '0.0.4'}



definition (
	name: 			"Device Generator", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Device Generator",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/deviceGen.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "options"
}



void installed() {
    if(debugEnabled) log.trace "${app.getLabel()} installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "${app.getLabel()} updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 
            section("") {
                input "devDriver", "text", title: "<b>Driver Name</b>", submitOnChange:true, required:false, width:4
                input "nameSpace", "text", title: "<b>Driver Name Space</b>", submitOnChange:true, required:false, width:4
                input "baseName", "text", title: "Base Name for devices", submitOnChange:true, required:false, width:4
                input "numDev", "number", title: "Number of Devices to Create", width:4, submitOnChange:true

                input "devCheck", "button", title:"Add Devices"
                if(state?.getDev == true){
                    createDevices()
                    state.getDev = "false"                    
                }

            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }        
    }
}


def createDevices() {
    log.debug "$nameSpace $devDriver $baseName $numDev"
    for(int i=0;i<numDev;i++){
        cd = addChildDevice("$nameSpace", "$devDriver", "AGen${app.id}-$i", [name: "${baseName}-$i", isComponent: false])
    }
}  

def appButtonHandler(btn) {
    switch(btn) {
        case "devCheck":
            state.getDev = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
