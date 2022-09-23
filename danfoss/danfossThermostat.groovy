/*
 * Danfoss Thermostat - for use as a child device of the Danfoss Master
 * 
 *
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
static String version() {return "1.0.0"}

metadata {
    definition (
        name: "Danfoss Thermostat", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/danfoss/danfossThermostat.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
        capability "Battery"
        
        attribute "heatingSetpoint", "number"
        attribute "thermostatSetpoint", "number"
        attribute "thermostatMode", "string"
        attribute "online", "string"
        
        
        command "refresh"
        command "setMode",[[name:"tMode",type:"STRING",description:"Thermostat Mode"]]


                                  
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", defaultValue:false)
    input("useFahrenheit", "bool", title: "Use Fahrenheit", defaultValue:false)
    input("pollRate", "number", title: "Thermostat Polling Rate (minutes)\nZero for no polling:", defaultValue:0)
}

@SuppressWarnings('unused')
def installed() {
    log.trace "${device.displayName} v${version()} installed()"
    initialize()
}

def initialize(){
   updated()    
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    if(pollRate == null)
        device.updateSetting("pollRate",[value:0,type:"number"])
    if(pollRate > 0){
        runIn(pollRate*60,"refresh")
    } else
        unschedule("refresh")
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh() {
    if(useFahrenheit) cOrF = "F"
    else  cOrF = "C"
    parent.updateChild(device.deviceNetworkId, cOrF)
    if(pollRate > 0)
        runIn(pollRate*60,"refresh")
}

void setHeatingSetpoint(temp){
    if(useFahrenheit)
        temp = fahrenheitToCelsius(temp).toFloat().round(0)
    temp = temp*10
    parent.sendCmd("${device.deviceNetworkId}","temp_set","$temp")
    updateAttr("thermostatSetpoint",temp,cOrF)
    updateAttr("heatingSetpoint",temp,cOrF)   
}

void setMode(tMode) {
    parent.sendCmd("${device.deviceNetworkId}","mode","$tMode")
    updateAttr("thermostatMode", "$tMode")
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
