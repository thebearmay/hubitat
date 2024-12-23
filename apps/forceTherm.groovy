/* 

 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

static String version()	{  return '0.0.4'  }

import groovy.transform.Field
@Field oStateOpts = ["heating", "pending cool", "pending heat", "vent economizer", "idle", "cooling", "fan only"]

definition (
	name: 			"Force Thermostat Modes", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Sends mode values to selected thermostat.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/forceTherm.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}

def installed() {
    log.info "${app.name} v${version()} installed()"
    state?.isInstalled = true
    state?.singleThreaded = true
    initialize()
}

def uninstalled() {
    log.info "${app.name} v${version()} uninstalled()"
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
                input "qryDevice", "capability.thermostat", title: "Thermostat(s) Selected:", multiple: true, required: false, submitOnChange: true
				input "dState", "enum",title:"Desired Operating State", options:oStateOpts
                input "sendBtn","button",title:"Send Update"
                if (state.sendBtn){
                    state.sendBtn = false
                    qryDevice.each {
                        it.sendEvent(name:"supportedThermostatModes", value:'["off","cool","heat","auto"]' , isStateChange:true)
    					it.sendEvent(name:"supportedThermostatFanModes", value:'["on","auto"]' , isStateChange:true)
    					it.sendEvent(name:"thermostatFanMode", value:"auto" , isStateChange:true)
    					it.sendEvent(name:"thermostatOperatingState", value:dState , isStateChange:true)
                        paragraph "Events sent to $it" 
                    }
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
        case "sendBtn":
        	state.sendBtn = true
        	break
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}
def intialize() {

}
