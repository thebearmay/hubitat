/*
 * Air Things Device
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
static String version() {return "0.0.4"}

metadata {
    definition (
        name: "Air Things Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/airthings/airthingsDevice.groovy"
    ) {
        capability "Actuator"
        capability "Initialize"
        //capability "Battery"


        attribute "radonShortTermAvg", "number"
        attribute "humidity", "number"
        attribute "pressure", "number"
        attribute "co2", "number"
        attribute "voc", "number"
        attribute "temperature", "number"
        attribute "battery", "number"
        
        command "refresh"                                  
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", defaultValue:false)
    input("useFahrenheit", "bool", title: "Use Fahrenheit", defaultValue:false)
    input("usePicoC", "bool", title: "Use pCi/L for Radon", defaultValue:false)
    input("pollRate", "number", title: "Sensor Polling Rate (minutes)\nZero for no polling:", defaultValue:0)
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
    refresh()
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh() {
    parent.updateChild(device.data.deviceId)
    if(pollRate > 0)
        runIn(pollRate*60,"refresh")
}

void dataRefresh(retData){
    retData.data.each{
        switch (it.key){
            case("temp"):
                unit="°"
                if(useFahrenheit) it.value = celsiusToFahrenheit(it.value)
                updateAttr("temperature", it.value, unit)
                break
            case("radonShortTermAvg"):
                if(usePicoC){
                    it.value = (it.value/37).round(1)
                    unit="pCi/L"
                }else
                    unit="Bq/m<sup>3</sup>"
                break
            case("humidity"):
                unit="%"
                break
            case("co2"):
                unit="ppm"
                break
            case("pressure"):
                unit="mBar"
                break
            case("voc"):
                unit="ppb"
                break
            case("battery"):
                unit="%"
                break
            default:
                unit=""
                break
        }
        if(it.key != "temp" && unit != null) //unit will be null for any values not tracked
            updateAttr(it.key, it.value,unit)
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
