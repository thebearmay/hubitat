/*
 * Open Garage
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

static String version()	{  return '0.0.1'  }

metadata {
    definition (
		name: "Open Garage", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/openGarage.groovy"
	) {
        capability "Actuator"
        capability "Polling"
        capability "GarageDoorControl"
        
        attribute "door", "string"
        attribute "distance", "number"
        attribute "vehStatus", "string"
        attribute "rssi","number"
        
        //command "close"
        command "toggleDoor"
        command "rebootDevice"
    }   
}

preferences {
    input("devIP", "string", title: "IP of the Open Garage Device", width:4)
    input("devPwd", "password", title: "Device Password", width:4)
    input("pollRate","number", title: "Polling interval in seconds (0 to disable)", width:4)
	input("debugEnable", "bool", title: "Enable debug logging?",width:4)
}

def installed() {
    log.trace "Open Garage v${version()} installed()"
    device.updateSetting("devIP",[value:"192.168.4.1",type:"string"])
    device.updateSetting("devPwd",[value:"opendoor",type:"password"]) 
    device.updateSetting("pollRate",[value:0,type:"number"]) 
}

def updated(){
    log.trace "updated()"
    if(debugEnable) 
        runIn(1800,"logsOff")
    else 
        unschedule("debugEnable")
    if(pollRate > 0)
        runIn(pollRate, "poll")
    else
        unschedule("poll")
    
}

void open() {
    if(debugEnable) log.debug "opening.."
    httpGet(
        [
            uri: "http://$devIP",
            path: "/cc?dkey=$devPwd&open=1",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    ) { resp -> }
    runIn(10,"poll")
  
}

void close() {
    if(debugEnable) log.debug "closing.."
    httpGet(
        [
            uri: "http://$devIP",
            path: "/cc?dkey=$devPwd&close=1",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    ) { resp -> }
    runIn(10,"poll")
  
}

void toggleDoor() {
    if(debugEnable) log.debug "toggle door.."
    httpGet(
        [
            uri: "http://$devIP",
            path: "/cc?dkey=$devPwd&click=1",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    ) { resp -> }
    runIn(10,"poll")
  
}

void rebootDevice() {
    if(debugEnable) log.debug "rebooting.."
    httpGet(
        [
            uri: "http://$devIP",
            path: "/cc?dkey=$devPwd&reboot=1",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    ) { resp -> }
    runIn(30,"poll")
  
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void poll(){
    httpGet(
        [
            uri: "http://$devIP",
            path: "/jc",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    ) { resp ->
        try{
            if (resp.getStatus() == 200){
                if (debugEnable) 
                    log.debug resp.data
                try {
                    processJc((Map)resp.data)
                } catch (e) {
                    processJc(respToMap(resp.data.toString()))
                }
            }
        }catch (ex) {
            log.error "$ex"
        }
    }
}

HashMap respToMap(String rData){
    if(debugEnable) log.debug rData
    rList = rData.substring(1,rData.size()-1).split(",")
    if(debugEnable) log.debug rList
    rMap = [:]
    rList.each{
        rMap["${it.substring(0,it.indexOf(":")).trim()}"] = it.substring(it.indexOf(":")+1)
    }
    if(debugEnable) log.debug rMap
    
    return rMap
}

void processJc(dMap){
    updateAttr("distance", dMap.dist, "cm")
    if(dMap.door.toInteger() == 1)
        updateAttr("door","open")
    else
        updateAttr("door", "closed")
    if(dMap.vehicle.toInteger() == 1)
        updateAttr("vehStatus", "present")
    else 
        updateAttr("vehStatus", "not present")
    updateAttr("rssi", dMap.rssi, "dBm")
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
