/*
 * Daikin Thermostat 
 *
 * API document: https://synaccess.com/support/webapi#table-of-contents
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
        name: "Daikin Thermostat", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/daikin/daikinThermostat.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        
        attribute "equipmentStatus","number"
        attribute "mode","number"
        attribute "modeLimit","number"
        attribute "modeEmHeatAvailable","number"
        attribute "fan","number"
        attribute "fanCirculate","number"
        attribute "fanCirculateSpeed","number"
        attribute "heatSetpoint","number"
        attribute "coolSetpoint","number"
        attribute "setpointDelta","number"
        attribute "setpointMinimum","number"
        attribute "setpointMaximum","number"
        attribute "tempIndoor","number"
        attribute "humIndoor","number"
        attribute "tempOutdoor","number"
        attribute "humOutdoor","number"
        attribute "scheduleEnabled","string"
        attribute "geofencingEnabled","string"
        
//        command "setEquipmentStatus", [[name:"equipStatus", type:"ENUM", constraints:["No Selection","1: cool","2: overcool for dehum","3: heat","4: fan","5: idle","6: waiting to cool","7: waiting to heat","8: aux humidifier","9: aux dehumidifier"]]]
        command "setMode",[[name:"mode", type:"ENUM", constraints:["No Selection","0: off","1: heat","2: cool","3: auto","4: emergency heat"]]]
//        command "setModeLimit",[[name:"modeLimit", type:"ENUM", constraints:["No Selection","0: none","1: all","2: heat only","3: cool only"]]]
//        command "setFan",[[name:"fan", type:"ENUM", constraints:["No Selection","0: auto","1: on"]]]
        command "setFanCirculate",[[name:"fanCirculate", type:"ENUM", constraints:["No Selection","0: off","1: always on","2: on a schedule"]]]
        command "setFanSpeed", [[name:"fanSpeed", type:"ENUM", constraints:["No Selection","0: low","1: medium","2: high"]]]
        command "setHeatPoint", [[name:"heatPoint", type: "number"]]
        command "setCoolPoint", [[name:"coolPoint", type: "number"]]
        command "useThermostatSchedule", [[name:"thermoSchedule", type:"ENUM", constraints:["No Selection","true", "false"]]]
                                  
                                  
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "${device.displayLabel} v${version} installed()"
    initialize()
}

def initialize(){

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

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void setMode(tMode){
}

void setFanCirculate(sel) {
}

void setFanSpeed(sel) {
}

void setHeatPoint(temp) {
}

void setCoolPoint(temp) {
}

void useThermostatSchedule(tOrF) {
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
