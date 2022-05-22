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
 *    17May22      thebearmay    Original Code
 *    18May22      thebearmay    Add PresenceSensor capability
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.1.5"}

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
        capability "PresenceSensor"
        
        attribute "longitude", "number"
        attribute "latitude", "number"
        attribute "deviceId", "string"
        attribute "deviceName", "string"
        attribute "currAddr", "JSON"
        attribute "mapUrl", "string"
        attribute "name", "string"
        attribute "updateReqTS", "string"
        attribute "lastUpdTS", "string"
        attribute "metersFromHub", "number"
        attribute "feetFromHub", "number"
 
        command "reqUpd"
        command "updFromNr",[[name:"updateStr",type:"string"]]
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("presenceRadius", "number", title: "GeoFence Radius", defaultValue:150)
    input("radiusMeasure", "enum", title:"GeoFence Measurement Unit", options:["meters","feet"], defaultValue:"meters")
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
            updateAttr("$it.key", "<a href='$it.value' target='_blank'>Click to Display Map</a>")
    }
    updateAttr("lastUpdTS", new Date())
    hubDist = dist(jsonData.longitude.toDouble(), jsonData.latitude.toDouble(), this.location.longitude.toDouble(), this.location.latitude.toDouble()) 
    updateAttr("metersFromHub", hubDist.round(2))
    updateAttr("feetFromHub", (hubDist*3.28084).round(2))
    
    if(radiusMeasure == null) device.updateSetting("radiusMeasure",[value:"meters",type:"text"])
    if(presenceRadius == null) device.updateSetting("presenceRadius",[value:150,type:"number"])
    if(radiusMeasure == "meters")
        rDist = hubDist
    else
        rDist = hubDist*3.28084
       
    if(rDist <= presenceRadius) 
        updateAttr("presence","present")
    else
        updateAttr("presence","not present")
    
}

double dist(long1, lat1, long2, lat2) {
  R = 6371; // Radius of the earth in km
  dLat = java.lang.Math.toRadians(lat2-lat1) 
  dLon = java.lang.Math.toRadians(long2-long1) 
  a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(java.lang.Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2)
  c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  d = R * c; // Distance in km
  return d*1000;    
}
          
@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
