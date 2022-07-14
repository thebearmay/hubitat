/*
 * Daikin Cloud Device 
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
import groovy.transform.Field

@Field modeStr = ["auto", "cool", "heat", "fan", "dry"]
@Field fanMode = ["auto", "low", "medium", "high"]

@SuppressWarnings('unused')
static String version() {return "0.0.0"}

metadata {
    definition (
        name: "Daikin Cloud Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/daikin/daikinCloud/cloudDev.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        
        command "updState",[[name:"stateStr", type:"STRING", description:"JSON string containing state updates"]]
        command "on"
        command "off"
        command "setSetpoint", [[name:"temp",type:"NUMBER", description:"Temperature to set the Set Point to"]]
        command "setMode", [[name:"temp",type:"ENUM", constraints: modeStr, description:"Value to set the Mode to "]] 
        command "setFan", [[name:"fMode",type:"ENUM", constraints: fanMode, description:"Fan Speed"]]

        attribute  "lastUpdateStr", "string"
        attribute  "mode", "number"
        attribute  "modeTxt", "string"
        attribute  "power", "string"
        attribute  "setpoint", "number"
        attribute  "temperature", "number"
        attribute  "speed", "number" 
        attribute  "fanSpeedTxt", "string"
        attribute  "isConnected", "string"
                                 
    }   
}

preferences {    
    input("debugEnabled", "bool", title: "Enable debug logging?")
    	input("useFahrenheit", "bool", title: "Use Fahrenheit", defaultValue:false)
}

@SuppressWarnings('unused')
def installed() {
    log.trace "Daikin Cloud Device v${version} installed()"
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

void updState(jStr){
    updateAttr("lastUpdateStr", "$jStr")
    def jSlurp = new JsonSlurper()
    Map jsonData = (Map)jSlurp.parseText((String)jStr)
    jsonData.each {
        if(it.key != "temperature")
            updateAttr("${it.key}","${it.value}")
        else if (useFahrenheit) {
            temp = celsiusToFahrenheit(it.value).toFloat().round(0)
            updateAttr("${it.key}","$temp", "°F")
        } else
            updateAttr("${it.key}","${it.value}", "°C")
        if(it.key == "mode") updateAttr("modeTxt", modeStr[it.value.toInteger()])
        if(it.key == "speed") updateAttr("fanSpeedTxt", fanMode[(it.value.toInteger()/2).toInteger()])
    }

}

void on(){
    parent.apiPut("${device.properties.data['${mac}']}/true")
}

void off(){
    parent.apiPut("${device.properties.data['${mac}']}/false")
}

void setMode(modeVal) {
    Integer i = 0
    modeStr.each{
        if(modeStr(i) == modeVal) modeInx = i*2
        i++
    }
    parent.apiPut("${device.properties.data['${mac}']}/$modeInx")
}

void setFan(modeVal) {
    Integer i = 0
    modeStr.each{
        if(modeStr(i) == modeVal) modeInx = i
        i++
    }
    parent.apiPut("${device.properties.data['${mac}']}/$modeInx")
}

void setSetPoint(temp){
    if(useFahrenheit){
        hold = temp
        temp = fahrenheitToCelsius(temp).toFloat().round(1)
        temp = checkForAdj(hold, temp)
    } else
        temp = normalizeTemp(temp)
    if(temp >= 18 && temp <=30)    
        parent.apiPut("${device.properties.data['${mac}']}/$temp")
    else 
        log.error "Setpoint temperature out of range(18-30°C): $temp"
}
                  
Float normalizeTemp(temp) { //limits to x.5 or x.0
    Float nTemp =  ((int) (temp*2 + 0.5))/2.0
    return nTemp
}

Float checkForAdj(hold, temp) {
    temp = normalizeTemp(temp)
    if(celsiusToFahrenheit(temp).toFloat().round(0) < hold)
        temp += 0.5
    return temp
}            
                  
def clearMsg(){
  
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
