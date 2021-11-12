 /*
 * Notification Switch
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 * 
 */
static String version()	{  return '0.0.2'  }

metadata {
    definition (
		name: "Notification Switch", 
		namespace: "thebearmay", 
		description: "Simple Switch that is turned on via a Notfication.",
		author: "Jean P. May, Jr.",
	    	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifySwitch.groovy"
	) {
       		capability "Notification"
			capability "Switch"
            capability "Configuration"
            attribute "notificationText", "STRING"

    }   
}

preferences {
	input("debugEnabled", "bool", title: "Enable debug logging?")
    input("toggleDelay","number", title:"Seconds before turning off switch (Zero=off)",defaultValue:0)
    input("onText","string",title:"If notification contains this text, turn on")
    input("offText","string",title:"If notification contains this text, turn off")
}

def installed() {
    log.trace "${device.displayName} installed using ${device.typeName} v${version()} "
    configure()
}

def updated(){
	log.trace "${device.displayName} preferences updated"
	if(debugEnabled) runIn(1800,logsOff)
}

def deviceNotification(notification){
    sendEvent(name:"notificationText", value: notification)
    if((!onText && !offText) || (onText && notification.indexOf(onText) > -1))
        on()
    else if(offText && notification.indexOf(offText) > -1)
        off()
}

def on(){
    sendEvent(name:"switch",value:"on")
    if(toggleDelay) runIn(toggleDelay,"off")
}    
              
def off(){  
    sendEvent(name:"switch",value:"off")
}

def configure() {
    off()
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
