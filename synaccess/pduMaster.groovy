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
static String version() {return "0.0.1"}

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

    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("simulated", "bool", title: "Use Similated Data")
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
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"
    reqPduData()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void reqPduData() {
    Map params = [
        uri    : serverAddr,
        path: "/api/device",
        Authorization: "Bearer $token"
    ]
    if (debugEnabled) log.debug params
    if(simulated)
        getPduData("a","b")
    else
        asynchttpGet("getPduData", params)
    
}

void getPduData(resp, data) {
    if(simulated) {
        simData = '{"numBanks": 3, "numOutlets": 2, "numInlets": 1, "inletPlug": "L21-30P", "outletPwrMeasurementsSupported": true, "outletSwitchingSupported": true, "enclosureSerialNumber": 1024122712, "modelNumber": "SP-3001CA-HA", "inletConfig": "standard", "formFactor": "0U", "controllerSerialNumber": 1360604425, "controllerFirmwareVersion": "1.0.3", "phase": "Single Phase", "controllerHardwareVersion": "1.0.0", "circuitBreakerProtection": true, "uptime": 19284619}'
        def jSlurp = new JsonSlurper()
        HashMap pduData = (HashMap)jSlurp.parseText((String) simData)
        storeConfig(pduData)
    } else {
        try{
            if (resp.getStatus() == 200){
                if (debugEnabled) log.info resp.data
                pduData = (HashMap) resp.JSON
                storeConfig(pduData)
            } else {
                if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
            } 
        } catch (Exception ex){
            if (!warnSuppress) log.warn ex
        }
    }
}

void storeConfig(pduData){
    updateAttr("numBanks", pduData.numBanks)
    updateAttr("numInlets", pduData.numInlets)
    updateAttr("numOutlets", pduData.numOutlets)
    
    pduData.each {
        if(it.key != "numBanks" && it.key != "numInlets" && it.key != "numOutlets" && it.key != "uptime") {
            updateDataValue("$it.key", "$it.value")
        }
    }
    createChildDev(pduData.numOutlets)
   
}

void createChildDev(numDev){
    if(debugEnabled) log.debug "Expecting $numDev child devices to be created"

    if(simulated) 
        outlets = getOutletData ("a", "b")
    else
        outlets = reqOutlets()
    if(debugEnabled) log.debug "Query returned ${outlets.size()} nodes"
    def i = 0
    outlets.each {
        i++
        addChildDevice("thebearmay", "Synaccess Outlet", "${device.deviceNetworkId}:${i}", [name:"$it.id",label:"$it.outletName",isComponent:true])
    }
}

def reqOutlets() {
    Map params = [
        uri    : serverAddr,
        path: "/api/outlets",
        Authorization: "Bearer $token"
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getOutletData", params)
}

def getOutletData(resp, data) {
    if(simulated) {
        simData = '[{ "id": "1-286331153", "outletName": "Outlet 1", "pwrOnState": "PREV", "outletIndex": 1, "receptacle": "IEC 60320 C19", "currentRms": 1.1, "state": "ON", "bankId": "286331153", "customRebootTimeEnabled": false, "customRebootTime": 12, "rebootStatus": "none", "voltageDetection": true, "relayHealth": "OK"  },  { "id": "2-286331153", "outletName": "Outlet 2", "pwrOnState": "PREV", "outletIndex": 2, "receptacle": "IEC 60320 C19", "currentRms": 1.1, "state": "ON", "bankId": "286331153", "customRebootTimeEnabled": false, "customRebootTime": 12, "rebootStatus": "none", "voltageDetection": true, "relayHealth": "OK"  }]'
        def jSlurp = new JsonSlurper()
        return jSlurp.parseText((String) simData)
    }
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
           return (HashMap) resp.JSON
    } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }

}   

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
