/*
 *  Node Red Motion
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
 *    2021-03-31  thebearmay	 Original version 0.1.0
 * 
 */

static String version()	{  return '0.3.0'  }

metadata {
    definition (
		name: "Node Red Motion", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	    importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/nodeRedMotion.groovy"
	) 
    {
        	capability "Actuator"
        	capability "Switch"
        	capability "MotionSensor"
            capability "Sensor"
            capability "Initialize"
            capability "Configuration"
        
            attribute "battery", "number"
            attribute "nodeName", "string"
        	attribute "lastUpdate", "number"
            attribute "syncPending", "bool"
		    command "updateStatus", [[name:"statusString*", type:"STRING", description:"JSON string containing nodeName, motion, battery and switch attributes"]]  

    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
    on()
}

def configure() {
    syncNR()
}

def initialize(){
    runIn(30,syncNR)
}

def syncNR(){
    updateAttr("syncPending", true)
    // Requires a check on the NR side for a change in lastSyncReq to inject 
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

def updateStatus(sStr) {
    if(debugEnable) log.debug "updateStatus $sStr"
    updateAttr("lastUpdate", new Date())
    updateAttr("syncPending", false)
    sStr = sStr.replaceAll("\"","")
    sStr = sStr.replace("{","")
    sStr = sStr.replace("}","")
    strArr = sStr.split(",")

    strArr.each {
        nodeArr = it.split(":")
        if(nodeArr[0] == "battery")
            updateAttr(nodeArr[0],nodeArr[1],"%")
        else    
            updateAttr(nodeArr[0],nodeArr[1])
    }
    if(device.currentValue("motion") == "active") runIn(300,checkSync)
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def on() {
    if(debugEnable) log.debug "switch on"
    updateAttr("switch","on")
}

def off() {
    if(debugEnable) log.debug "switch off"
    updateAttr("switch","off")
}

def checkSync() {
    if(device.currentValue("motion") == "active") {
        if(device.currentValue("switch") == "on") {
            syncNR()
            runIn(300,checkSync)
            log.warn "Sync lost with ${device.currentValue('nodeName')}, retrying..."
        } else updateAttr("motion","inactive")
    }
}
    
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
