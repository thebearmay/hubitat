/* Contact Sensor Close / Motion Sensor Inactive / Switch-Bulb Off

 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Date		Who			Description
 *	---------	----------	-----------------------------------------------
 *	25Aug2025	thebearmay	added switch/bulbs off command
 */

static String version()	{  return '0.0.3'  }


definition (
	name: 			"Force Sensor Close-Inactive", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Sends inactive/close event to designated sensors.",
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
    log.info "${app.name} v${version()} installed()"
    state?.isInstalled = true
    state?.singleThreaded = true
    initialize()
}

def uninstalled() {
    log.info "${app.name} v${version()} uninstalled()"
    removeDevice()
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
                input "qryDevice", "capability.contactSensor", title: "Contact Sensors Selected:", multiple: true, required: false, submitOnChange: true
                input "qryDevice2", "capability.motionSensor", title: "Motion Sensors Selected:", multiple: true, required: false, submitOnChange: true
                input "qryDevice3", "capability.switch", title: "Bulbs/Switches Selected:", multiple: true, required: false, submitOnChange: true
                input "createChild", "bool", title: "Create Button Device?", defaultValue: false, submitOnChange: true
                if(createChild) {
					addDevice()
                } else {
                    removeDevice()
                }				
                if (qryDevice != null || qryDevice2 != null || qryDevice3 != null) href "deviceCharacteristics", title: "Send Close-Inactive-Off Event", required: false
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
        qryDevice2.each{
          paragraph "<p>${it.displayName} ${it.currentValue('motion')}</p>"
        }
        qryDevice3.each{
          paragraph "<p>${it.displayName} ${it.currentValue('switch')}</p>"
        }   
      }
    }
}

def closeContacts(evt = "pushed"){
	qryDevice.each{
		it.sendEvent(name:"contact",value:"closed",isStateChange:true)
	}
    qryDevice2.each{
		it.sendEvent(name:"motion",value:"inactive",isStateChange:true)
	}
	qryDevice3.each{
		it.sendEvent(name:"switch",value:"off",isStateChange:true)
	}
}

def addDevice() {
    if(!this.getChildDevice("cscButton001")){
        addChildDevice("hubitat","Virtual Button","cscButton001",[name:"Close Contact Button"])
        chDev = this.getChildDevice("cscButton001")
        subscribe(chDev, "pushed", "closeContacts")
    }
}

def removeDevice(){
    unsubscribe()
    if(this.getChildDevice("cscButton001")) deleteChildDevice("cscButton001")
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
