 /*
 * Dexcom Glucose Monitor
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
 *    Date        Who            What
 *    ----        ---            ----
*/
@SuppressWarnings('unused')
static String version() {return "0.0.2"}

metadata {
    definition (
        name: "Dexcom Glucose Monitor", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/dexcom/dexGluMon.groovy"
    ) {
        capability "Actuator"
        capability "Initialize"
        
        attribute "glucose", "number"
        attribute "glucoseTrend", "string"
        attribute "glucoseRate", "string"
        attribute "glucoseStatus", "string"
        attribute "alertJson", "string"
        
        command "getGlucose"
        command "getAlert"
        
    }
}
preferences {
    input("glucoseRange", "number", title: "Glucose Data Poll Interval in Seconds", submitOnChange: true, width:4)
    input("alertRange", "number", title: "Alert Poll Interval in Seconds", submitOnChange: true, width:4)
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
}
                         
@SuppressWarnings('unused')
void installed() {

}

void initialize() {

}

void configure() {
   
}

void updated(){
    unschedule("getGlucose")
    if(glucoseRange > 0) 
        runIn(glucoseRange, "getGlucose")
    unschedule("getAlert")
    if(alertRange > 0) 
        runIn(alertRange, "getGlucose")
    unschedule("logsOff")
    if(debugEnabled)
        runIn(1800,"logsOff")
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.contains("Your hub is starting up"))
       return
    sendEvent(name:aKey, value:aValue, unit:aUnit)
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void getGlucose() {
    if(debugEnabled) log.debug "glucose range: $glucoseRange"
    if(glucoseRange == null || glucoseRange == 0) 
        gRange = 600
    else
        gRange = glucoseRange
    parent.getGlucose(device.deviceNetworkId, gRange)
    if(glucoseRange > 0) 
        runIn(glucoseRange, "getGlucose")
}

void getAlert() {
    if(debugEnabled) log.debug "alert range: $alertRange"
    if(alertRange == null || alertRange == 0) 
        aRange = 600
    else
        aRange = alertRange
    parent.getAlert(device.deviceNetworkId, aRange)
    if(alertRange > 0) 
        runIn(alertRange, "getAlert")
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
