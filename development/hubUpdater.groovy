/*
 * Hub Updater
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
 *    Date         Who           What
 *    ----         ---           --------------------------------------
 *
 */
import groovy.transform.Field
import groovy.json.JsonSlurper

static String version()	{  return '0.0.1'  }

metadata {
    definition (
		name: "Hub Updater", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/hubUpdater.groovy"
	) {
        capability "Actuator"
        capability "Momentary"
       
        attribute "msg", "string"
                                
    
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
    input("updMesh","bool",title: "Update all meshed hubs", width:4)
}


def installed() {
    log.trace "Hub Updater v${version()} installed()"
}

def configure() {
    if(debugEnabled) log.debug "configure()"

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def test(){
    log.debug "Test $debugEnabled"
}

def push(){
    log.info "Firmware Update Requested"
    updateAttr ("msg", "Update Requested at ${new Date()}")
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/cloud/checkForUpdate",
        timeout: 10
    ]
    asynchttpGet("getUpdateCheck", params)    
}

void getUpdateCheck(resp, data) {
    if(debugEnable) log.debug "update check: ${resp.status}"
    try {
        if (resp.status == 200) {
            def jSlurp = new JsonSlurper()
            log.debug "${resp.data}"
            Map resMap = (Map)jSlurp.parseText((String)resp.data)
            if(resMap.status == "NO_UPDATE_AVAILABLE")
                updateAttr("msg","Hub is Current")
            else {
                updateAttr("msg","${resMap.version} requested")
                if(updMesh) {
                    updateMesh()
                    pauseExecution(1000)
                }
                httpGet("http://127.0.0.1:8080/hub/cloud/updatePlatform"){ response -> 
                    updateAttr("msg", "${response.data}")
                }
            }
        }
    }catch(ignore) {
        updateAttr("msg", "Hub is Current")
    }
}

void updateMesh(){
    params =  [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubMeshJson",
        headers: [
            "Connection-Timeout":600
        ]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getHubMesh", params)
}

@SuppressWarnings('unused')
void getHubMesh(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable)
                log.debug resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
           if (debugEnable)
                log.debug "${h2Data.hubList}"
            h2Data.hubList.each{
                log.info "Requesting update of ${it.ipAddress}"
                httpGet("http://${it.ipAddress}/hub/cloud/updatePlatform"){}
            } 
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on Hubmesh request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}
    
def updated(){
	log.trace "updated()"
	if(debugEnabled) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
