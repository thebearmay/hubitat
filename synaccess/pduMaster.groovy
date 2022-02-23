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

        attribute "numBanks", "number"
        attribute "numOutlets", "number"
        attribute "numInlets", "number"
        attribute "uptime", "number" 
    }   
}

preferences {
    input("serverAddr", "text", title:"IP address for PDU:", required: true, submitOnChange:true)
    input("token", "text", title:"Personal Access Token:", required: true, submitOnChange:true)
    //input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true)

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
    reqPduData()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

//{"numBanks": 3, "numOutlets": 36, "numInlets": 1, "inletPlug": "L21-30P", "outletPwrMeasurementsSupported": true, "outletSwitchingSupported": true, "enclosureSerialNumber": 1024122712, "modelNumber": "SP-3001CA-HA", "inletConfig": "standard", "formFactor": "0U", "controllerSerialNumber": 1360604425, "controllerFirmwareVersion": "1.0.3", "phase": "Single Phase", "controllerHardwareVersion": "1.0.0", "circuitBreakerProtection": true, "uptime": 19284619}
void reqPduData() {
    Map params = [
        uri    : serverAddr,
        Authorization: "Bearer $token"
    ]
    if (debugEnable) log.debug params
    asynchttpGet("getPduData", params)
}

void getPduData(resp, data) {
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            pduData = resp.data
            updateAttr("numBanks", pduData.numBanks)
            updateAttr("numInlets", pduData.numInlets)
            updateAttr("numOutLets", pduData.numOutlets)
            
            updateDataValue("inletPlug", pdu.inletPlug)
            updateDataValue("outletPwrMeasurementsSupported", pdu.outletPwrMeasurementsSupported)
            updateDataValue("outletSwitchingSupported", pdu.outletSwitchingSupported)
            updateDataValue("enclosureSerialNumber", pdu.enclosureSerialNumber)
            updateDataValue("modelNumber", pdu.modelNumber)
            updateDataValue("inletConfig", pdu.inletConfig)
            updateDataValue("formFactor", pdu.formFactor)
            updateDataValue("controllerSerialNumber", pdu.controllerSerialNumber)
            updateDataValue("controllerFirmwareVersion", pdu.controllerFirmwareVersion)
            updateDataValue("phase", pdu.phase)
            updateDataValue("controllerHardwareVersion", pdu.controllerHardwareVersion)
            updateDataValue("circuitBreakerProtection", pdu.circuitBreakerProtection)            
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
    
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
