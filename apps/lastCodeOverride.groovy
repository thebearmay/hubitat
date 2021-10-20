/* Last Code Name Override
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

static String version()	{  return '0.0.2'  }


definition (
	name: 			"Last Code Name Override", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Over write the lastCodeName state when a lock opens to allow triggering of another event if the same code opens it consecutively",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/lastCodeOverride.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "sendEvent"
}

def installed() {
    log.info "${app.name} v${version()} installed()"
    state?.isInstalled = true
    state?.singleThreaded = true
    initialize()
}

def uninstalled() {
    log.info "${app.name} v${version()} uninstalled()"
    removeDevice()
    unsubscribe()
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
          input "qryDevice", "capability.lock", title: "Contact Sensors Selected:", multiple: true, required: false, submitOnChange: true
          input "createChild", "bool", title: "Create Button Device?", defaultValue: false, submitOnChange: true
          if(createChild) {
					   addDevice()
          } else {
             removeDevice()
          }				
          if (qryDevice != null){
            href "sendCodeEvent", title: "Send Close-Inactive Event", required: false
            unsubscribe()
            subscribe(qryDevice,"lastCodeName","nameOverride")
          }
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def sendCodeEvent(){
    dynamicPage (name: "sendCodeEvent", title: "", install: false, uninstall: false) {
	    section(""){
		    nameOverride()
        qryDevice.each{
          paragraph "<p>${it.displayName} lastCodeName: <b>${it.currentValue('lastCodeName')}</b></p>"
        }
      }
    }
}

def nameOverride(evt = "pushed"){
	qryDevice.each{
		it.sendEvent(name:"lastCodeName",value:"None",isStateChange:true)
	}

}

def addDevice() {
    if(!this.getChildDevice("lcnOvrButton001")){
        addChildDevice("hubitat","Virtual Button","cscButton001",[name:"Close Contact Button"])
        chDev = this.getChildDevice("lcnOvrButton001")
        subscribe(chDev, "pushed", "nameOverride")
    }
}

def removeDevice(){
	unsubscribe()
  deleteChildDevice("lcnOvrButton001")
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
