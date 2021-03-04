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
 *    2021-01-31  thebearmay     Code cleanup, release ready
 *    2021-01-31  thebearmay     Putting a config delay in at initialize to make sure version data is accurate
 *    2021-02-16  thebearmay     Add text date for restart
 *    2021-03-05  thebearmay     Added CPU and Temperature polling
 */
import java.text.SimpleDateFormat
static String version()	{  return '1.3.2'  }

metadata {
    definition (
		name: "Hub Information", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfo.groovy"
	) {
        capability "Actuator"
	    capability "Initialize"
		capability "TemperatureMeasurement"
        
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
	    attribute "firmwareVersionString", "string"
        attribute "timeZone", "string"
        attribute "temperatureScale", "string"
        attribute "zipCode", "string"
        attribute "locationName", "string"
        attribute "locationId", "string"
        attribute "lastHubRestartFormatted", "string"
        attribute "freeMemory", "string"
		command "configure"
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
    input("tempPollEnable", "bool", title: "Enable Temperature Polling")
    input("tempPollRate", "number", title: "Temperature Polling Rate (seconds)\nDefault:300", default:300, submitOnChange: true)
}

def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    def myHub = location.hub
    hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        updateAttr(hubProp[i], myHub["${hubProp[i]}"])
    }
    for(i=0;i<locProp.size();i++){
        updateAttr(locProp[i], location["${locProp[i]}"])
    }
    updateAttr("hubVersion", location.hub.firmwareVersionString) //retained for backwards compatibility
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)
    updateAttr("lastUpdated", now())
    if (tempPollEnable) getTemp()
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def initialize(){
    log.trace "Hub Information initialize()"
// psuedo restart time - can also be set at the device creation or by a manual initialize
    restartVal = now()
    updateAttr("lastHubRestart", restartVal)	
    sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(restartVal))
    runIn(30,configure)
}

def getTemp(){
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/internalTempCelsius"
    ]
    if(debugEnable)log.debug params
    httpGet(params, {response -> 
        if(debugEnable) {
            response.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug response.data
        }
        tempWork = new Double(response.data.toString())
        if(debugEnable) log.debug tempWork
        if (location.temperatureScale == "F")
            updateAttr("temperature",celsiusToFahrenheit(tempWork))
        else
            updateAttr("temperature",tempWork)
    })
    
    // get Free Memory
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/freeOSMemory"
    ]
    if(debugEnable)log.debug params
    httpGet(params, {response -> 
        if(debugEnable) {
            response.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug response.data
        }
        memWork = new Double(response.data.toString())
        if(debugEnable) log.debug memWork
        
            updateAttr("freeMemory",memWork)
    })
    if(tempPollRate == null)  device.updateSetting("tempPollRate",[value:300,type:"number"])
    if (debugEnable) log.debug tempPollRate
    if (tempPollEnable) runIn(tempPollRate,getTemp)
}


def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
