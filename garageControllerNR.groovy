 /*
 * NR Garage Controller
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
 *    2021-09-08  thebearmay	 Original version 0.1.0
 * 
 */

static String version()	{  return '0.0.1'  }

metadata {
    definition (
		name: "NR Garage Controller", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/garageControllerNR.groovy"
	) {
        capability "GarageDoorControl"
        command ("setStatus",[["name":"Set Door Status","type":"ENUM", "constraints":["unknown", "open", "closing", "closed", "opening"]]])
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
}

def updated(){
    log.trace "updated()"
    if(debugEnable) runIn(1800,logsOff)
}

def open() {
    if(debugEnable) log.debug "opening.."
    setStatus("opening")

}

def close() {
    if(debugEnable) log.debug "closing.."
    setStatus("closing")
  
}

void setStatus(dStat){
    sendEvent(name:"door", value:dStat)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
