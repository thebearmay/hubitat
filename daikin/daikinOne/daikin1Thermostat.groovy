/*
 * Daikin One Thermostat - for use as a child device of the Daikin Master
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
 *    17Nov2024    thebearmay    Fix display issue with the cooling and heating setpoints
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.6"}

metadata {
    definition (
        name: "Daikin One Thermostat", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/daikin/daikinOne/daikin1Thermostat.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Thermostat"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatFanMode"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatMode"
        capability "ThermostatOperatingState"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
        capability "Sensor"
        
        attribute "equipmentStatus","number" //???HE - thermostatOperatingState ENUM ["heating", "pending cool", "pending heat", "vent economizer", "idle", "cooling", "fan only"]        
        attribute "fan","number"
        attribute "fanCirculateSpeed","number"
        attribute "setpointDelta","number"
        attribute "setpointMinimum","number"
        attribute "setpointMaximum","number"
        attribute "tempOutdoor","number"
        attribute "humidity", "number"
        attribute "humidOutdoor", "number"

        
        command "refresh"


                                  
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", defaultValue:false)
    input("useFahrenheit", "bool", title: "Use Fahrenheit", defaultValue:false)
    input("pollRate", "number", title: "Thermostat Polling Rate (minutes)\nZero for no polling:", defaultValue:5)
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
        device.updateSetting("pollRate",[value:5,type:"number"])
    if(pollRate > 0){
        runIn(pollRate*60,"refresh")
    } else
        unschedule("refresh")
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"
    log.debug device.properties
    device.supportedAttributes.each{
        log.debug "${it}"
        updateAttr("${it}","0")
    }
    updateAttr("supportedThermostatFanModes",'["auto", "circulate", "on"]')
    updateAttr("supportedThermostatModes",'["heat", "cool", "emergency heat", "auto", "off"]')              
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

void auto(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[mode:3])
    updateAttr("thermostatMode","auto")
}

void cool(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[mode:2])
    updateAttr("thermostatMode","cool")
}

void emergencyHeat(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[mode:4])
    updateAttr("thermostatMode","emergency heat")
}

void fanAuto(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[fanCirculate:0])
    updateAttr("thermostatFanMode","auto")
}

void fanCirculate(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[fanCirculate:2])
    updateAttr("thermostatFanMode","circulate")
}

void fanOn(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[fanCirculate:1])
    updateAttr("thermostatFanMode","on")
}

void heat(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[mode:1])
    updateAttr("thermostatMode","heat")
}

void off(){
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[mode:0])
    updateAttr("thermostatMode","off")
}

void setCoolingSetpoint(temp){
    if(useFahrenheit) {
        temp2 = fahrenheitToCelsius(temp).toFloat().round(0)
        cOrF = "°F"
    } else {
        cOrF = "°C"
        temp2 = temp
    }
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[cspHome:temp2])
    updateAttr("thermostatSetPoint",temp,cOrF)
    updateAttr("coolingSetPoint",temp,cOrF)                   
}

void setHeatingSetpoint(temp){
    if(useFahrenheit) {
        temp2 = fahrenheitToCelsius(temp).toFloat().round(0)
        cOrF = "°F"
    } else {
        cOrF = "°C"
        temp2 = temp
    }
    parent.sendPut("/deviceData/${device.deviceNetworkId}",[hspHome:temp2])
    updateAttr("thermostatSetPoint",temp,cOrF)
    updateAttr("heatingSetPoint",temp,cOrF)   
}

void setThermostatFanMode(fanmode){
    if(fanmode=="on") 
       fanOn()
    else if (fanmode == "circulate")
       fanCirculate()
    else
       fanAuto()    
}

void setThermostatMode(tmode){
    if(tmode == "auto")
        auto()
    else if(tmode == "heat")
        heat()
    else if(tmode == "cool")
        cool()
    else if(tmode == "off")
        off()
    else        
        emergencyHeat()
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
