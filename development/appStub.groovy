 /*
 * Application Name
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
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-01-04  thebearmay	 Original version 0.1.0
 * 
 */

static String version()	{  return '0.1.0'  }


definition (
	name: 			"Application Name", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Application description",
	category: 		"My Apps",
	importURL:		"https://raw.githubusercontent.com/thebearmay/hubitat/main/appName.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {

   page name: "mainPage"
   page name: "nameNextPage"
}

def installed() {
	log.trace "installed()"
    atomicState?.isInstalled = true
    initialize()
}

def updated(){
	log.trace "updated()"
    if(!atomicState?.isInstalled) { atomicState?.isInstalled = true }
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
	    	section("Section Header")
		    {
			    input "debugEnable", "bool", title: "Enable debug logging?", required: false, defaultValue: false
			    input "switchName", "bool", title: "boolean siwtch", required: false, defaultValue: false
    			input "textField", "text", title: "some message text", description: "displayed in field", required: false, defaultValue: "", submitOnChange: true
                input "linkStyle", "enum", title: "Link Style", required: true, submitOnChange: true, options: ["embedded":"Same Window", "external":"New Window"], image: ""
		    	href "nameNextPage", title: "page navigation", required: false
			    paragraph "some text to display"
                input "sendPushMessage", "capability.notification", title: "Notification Devices: Hubitat PhoneApp", multiple: true, required: false
                notify("Test Message")

                
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def nameNextPage() {
    dynamicPage (name: "nameNextPage", title: "", install: false, uninstall: false) {
        section(""){
            paragraph "Second page"
            href "mainPage", title:"go back", required:false
        }
    }
}

def notify(message){
    if(!sendPushMessage) return
    if(message == null) message = "Test notification, parameter fail"
    sendPushMessage.deviceNotification(message)   
}
