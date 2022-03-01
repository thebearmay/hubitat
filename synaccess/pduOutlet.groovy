/*
 * Synaccess PDU Outlet 
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
        name: "Synaccess Outlet", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/synaccess/pduOutlet.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Outlet"

        command "refresh" 
        command "reboot"
        attribute "lastRefresh", "STRING"
    }   
}

preferences {
    //input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true)

    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){
    runIn(2,"refresh")
}

def refresh(){
    parent.getState("${device.deviceNetworkId}","${getDataValue('name')}")
    updateAttr("lastRefresh", new Date())
}

def reboot(){
    parent.rebootOutlet("${getDataValue('name')}")
    runIn(60,"refresh")
}

def on(){
    parent.powerMode("${getDataValue('name')}","on")
    updateAttr("switch", "on")
    updateAttr("lastRefresh", new Date())
}

def off(){
    parent.powerMode("${getDataValue('name')}","off")
    updateAttr("switch", "off")
    updateAttr("lastRefresh", new Date())
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
    intialize()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
