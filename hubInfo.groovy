 /*
 * Hub Info
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
 *    2020-12-07  thebearmay	 Original version 0.1.0
 *    2021-01-30  thebearmay     Add full hub object properties
 */

static String version()	{  return '0.7.0'  }

metadata {
    definition (
		name: "Hub Information", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfo.groovy"
	) {
        	capability "Actuator"
	    	capability "Initialize"
		
		attribute "latitude", "string"
		attribute "longitude", "string"
        attribute "hubVersion", "string"
        attribute "id", "string"
        attribute "name", "string"
        attribute "data", "string"
        attribute "zigbeeId", "string"
        attribute "zigbeeEui", "string"
        attribute "hardwareID", "string"
        attribute "type", "string"
        attribute "localIP", "string"
        attribute "localSrvPortTCP", "string"
        attribute "uptime", "string"
        attribute "lastUpdated", "string"
        attribute "lastHubRestart", "string"
		command "configure"
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}



def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    sendEvent(name:"latitude", value:location.getLatitude())
	sendEvent(name:"longitude", value:location.getLongitude())
    sendEvent(name:"hubVersion", value:location.hub.firmwareVersionString)
    def myHub = location.hub
    hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        updateAttr(hubProp[i], myHub["${hubProp[i]}"])
    }
    updateAttr("lastUpdated", now())
}

def restartDetected(){
    updateAttr("lastHubRestart", now())
    configure()
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def initialize(){
    if(debugEnable) log.debug "initialize()"
    restartDetected()	
    configure()    
}


def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
