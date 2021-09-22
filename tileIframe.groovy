 /*
 * Tile iFrame Device
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
 * 
 */

static String version()	{  return '0.2.0'  }

metadata {
    definition (
		name: "Tile iFrame Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/tileIframe.groovy"
	) {
        	capability "Actuator"
	    	capability "Sensor"
  //          capability "Variable"
		
		attribute "html", "string"
        attribute "url", "string"
        command "setSource", [[name:"url", type:"STRING", description:"URL to display"]]   
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
    	setVariable("installed")
}

def updated(){
    log.trace "updated()"
    if(debugEnable) runIn(1800,logsOff)
}

def setSource(url) {
    if(debugEnable) log.debug "setSource $url"
    sendEvent(name:"html", value:"<iframe src='$url' style='width:100%;height:100%;border:none;'></iframe>")
    sendEvent(name:"url",value:url)
}


void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
