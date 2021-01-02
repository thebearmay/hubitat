 /*
 *  Virtual Switch with Presence
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
 *    2021-01-03  thebearmay	 Original version 0.1.0
 * 
 */

static String version()	{  return '0.1.0'  }

metadata {
    definition (
		name: "Virtual Switch with Presence", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/dashVariable.groovy"
	) {
        	capability "Actuator"
        	capability "Switch"
        	capability "PresenceSensor"
        	
		    command "arrived"
            command "departed"

    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
    on()
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

def on() {
    if(debugEnable) log.debug "switch on"
    sendEvent(name:"switch", value:"on")
    sendEvent(name:"presence",value:"present")
}

def arrived(){
    if(debugEnable) log.debug "arrived"
    on()
}    

def off() {
    if(debugEnable) log.debug "switch off"
    sendEvent(name:"switch", value:"off")
    sendEvent(name:"presence",value:"not present")
}

def departed(){
    if(debugEnable) log.debug "departed"
    off()
}    
    
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
