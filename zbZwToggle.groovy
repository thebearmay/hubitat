/* Zigbee/Zwave toggle buttons
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
 *	Date		Who		        Description
 *	----------	-------------	----------------------------------------------------------------------------
 *  18Apr2023    thebearmay      change zigbee "enabled" to "on"
*/

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "0.0.2"}

metadata {
    definition (
        name: "ZB-ZW Radio Toggle", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/zbZwToggle.groovy"
    ) {
        capability "Actuator"
        capability "PushableButton"
        capability "Configuration"
        capability "Refresh"
        
        attribute "btnDescription", "string"
        attribute "zwaveStatus", "string"
        attribute "zigbeeStatus", "string"
        
        command "push", [[name:"buttonNumber*", type:"NUMBER", description:"1-Refresh 2-Zigbee Enable 3-Zigbee Disable 4-ZWave Enable 5-ZWave Disable"]]

    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
    if(pollRate > 0){
        refresh()
        runIn(pollRate, "refresh")
    } else
        unschedule("refresh")
}

def configure(){
    updateAttr("numberOfButtons",5)
    updateAttr("btnDescription","<pre rows='5'>1-Refresh\n2-Zigbee Enable\n3-Zigbee Disable\n4-ZWave Enable\n5-ZWave Disable</pre>")
    refresh()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh(){
    pollHub2()
    if(pollRate == null) 
        device.updateSetting("pollRate",[value:300,type:"number"])
    if(pollRate > 0){
        runIn(pollRate, "refresh")
    }
}

void push(btnNum){
    updateAttr("pushed",btnNum)
    switch (btnNum) {
        case 1://refresh
            refresh()
            break
        case 2://Zigbee Enable
            zbPost("on")//("enabled")
            break
        case 4://Zwave Enable
            zwPost("enabled")
            break
        case 3: //Zigbee Disable
            zbPost("disabled")
            break
        case 5:// ZWave Disable
            zwPost("disabled")
            break
        default:
            updateAttr("pushed", "Invalid")
    }
}

def installed() {
    initialize()
}

def uninstalled(){
    unschedule()
}

def initialize() {

}  
    
def updated(){
	initialize()
}

void pollHub2() {
        Map params =
        [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub2/hubData"        
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getHub2", params)
} 

@SuppressWarnings('unused')
void getHub2(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.debug resp.data
            try{
				def jSlurp = new JsonSlurper()
			    h2Data = (Map)jSlurp.parseText((String)resp.data)
            } catch (eIgnore) {
                if (debugEnable) log.debug "H2: $h2Data <br> ${resp.data}"
                return
            }
            if(h2Data?.baseModel == null) {
                if (debugEnable) log.debug "baseModel is missing from h2Data<br>$h2Data"
                return
            }
            if(h2Data.baseModel.zwaveStatus == "false") 
                updateAttr("zwaveStatus","enabled")
            else
                updateAttr("zwaveStatus","disabled")
            if(h2Data.baseModel.zigbeeStatus == "false"){
                updateAttr("zigbeeStatus", "enabled")
            } else {
                updateAttr("zigbeeStatus", "disabled")
            }
      } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on H2 request"
      } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

def zwPost(eOrD){
    try{
        params = [
					uri: "http://127.0.0.1:8080/hub/zwave/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zwaveStatus:"$eOrD"],
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
    refresh()
}

def zbPost(eOrD){
    try{
        params = [
                    uri: "http://127.0.0.1:8080/hub/zigbee/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zigbeeStatus:"$eOrD"], 
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
    refresh()
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
