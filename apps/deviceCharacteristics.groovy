 /*
 * Device Characteristics 
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
 *    2021-03-11  thebearmay	Original version 0.1.0
 * 
 */

static String version()	{  return '0.1.0'  }


definition (
	name: 			"Device Characteristics", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Display the capabilities and attributes for devices selected.",
	category: 		"Utility",
	importUrl:		"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/deviceCharacteristics.groovy",
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
    atomicState.runEffect = false
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null) href "deviceCharacteristics", title: "Device Characteristics", required: false
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
	  section("Device Characteristics"){
          
          for(i=0;i<qryDevice.size();i++){
            if (qryDevice[i].label) qryName= qryDevice[i].label
            else qryName = qryDevice[i].name
            def qryDeviceState = ""
            def nl = false
            qryDevice[i].supportedAttributes.each {
                if (nl) qryDeviceState += "\n"
                def tempValue = qryDevice[i].currentValue("$it")
    	        qryDeviceState += "<span style='font-weight:bold'>$it:</span> $tempValue"
                nl = true
            }
            def devAttr = qryDevice[i].supportedAttributes
            def devCap = qryDevice[i].capabilities
            def devCmd = qryDevice[i].supportedCommands
            paragraph "<h2>$qryName</h2><span style='font-weight:bold'>Attributes:</span>$devAttr\n\n<span style='font-weight:bold'>Current Values:</span>\n$qryDeviceState\n\n<span style='font-weight:bold'>Capabilities:</span> $devCap\n\n<span style='font-weight:bold'>Commands:</span> $devCmd"    
            href "mainPage", title: "Return", required: false
          }
       }
    }
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
