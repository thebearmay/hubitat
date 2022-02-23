/*
 * Synaccess PDU Master 
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
 *    ----         ---           ----
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.1.5"}

metadata {
    definition (
        name: "Synaccess PDU Master", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/synaccess/pduMaster.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"

    }   
}

preferences {
    input("serverAddr", "text", title:"IP address and path to Poll:", required: true, submitOnChange:true)
    input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true)

    input("debugEnable", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){
    if (serverAddr != null){
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,"getPollValues")
        }else if(tempPollRate > 0){
            runIn(tempPollRate,"getPollValues")
        }
    }
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    getPollValues()
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnable) log.debug "configure()"
    getPollValues()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void getPollValues(){

    Map params = [
        uri    : serverAddr,
    ]
    if (debugEnable) log.debug params
    asynchttpGet("getPollData", params)

    if (debugEnable) log.debug "tempPollRate: $tempPollRate"


    if(tempPollRate == null){
        device.updateSetting("tempPollRate",[value:300,type:"number"])
        runIn(300,"getPollValues")
    }else if(tempPollRate > 0){
        runIn(tempPollRate,"getPollValues")
    }
}


@SuppressWarnings('unused')
void getPollData(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            dataIn = resp.data.toString()
 
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}   



@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
