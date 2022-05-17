/* Apple FindMe Driver
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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "Apple FindMe", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        description: "Recieves data from the Node Red Apple FindMe nodes, Apple account information is stored in NR",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/appleFindMe.groovy"
    ) {
 
        capability "Actuator"
        capability "Initialize"
        
        attribute "longitude", "number"
        attribute "latitude", "number"
        attribute "deviceId", "string"
        attribute "deviceName", "string"
        attribute "currAddr", "JSON"
        attribute "mapUrl", "string"
        attribute "name", "string"
        attribute "updateReqTS", "string"
        attribute "lastUpdTS", "string"
 
        command "reqUpd"
        command "updFromNr",[[name:"updateStr",type:"string"]]
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {

}
@SuppressWarnings('unused')
def updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void initialize() {
    updated()
}

void updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,"logsOff")
    } 
}

void updFromNr(jsonIn){
    if(debugEnabled) updateAttr("dataChk",jsonIn)
    def slurper = new groovy.json.JsonSlurper()
    jsonData = slurper.parseText(jsonIn)
    setVariables(jsonData)    
}

void reqUpd(){
    updateAttr("updateReqTS", new Date())
}

void setVariables(jsonData){
    if(debugEnabled) log.debug "${jsonData}"
    jsonData.each{
        if(debugEnabled) log.debug "${it.key} : ${it.value}"
        if(it.key != "mapUrl")
            updateAttr("$it.key", "$it.value")
        else 
            updateAttr("$it.key", "<a href='$it.value'>Click to Display Map</a>")
    }
    updateAttr("lastUpdTS", new Date())
}
    
          
@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
