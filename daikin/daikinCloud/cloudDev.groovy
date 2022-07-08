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
        
        command "updState",[["jsonString"]]
        
        attribute "temporary", "string"

    }   
}

preferences {    
    input("debugEnabled", "bool", title: "Enable debug logging?")
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
    updateAttr("temporary", "$jStr")
}

def clearMsg(){
  
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
