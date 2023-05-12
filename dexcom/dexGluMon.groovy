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
static String version() {return "0.0.1"}

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
        
        command "getGlucose"
        
    }
}
preferences {
    input("glucoseRange", "number", title: "Glucose Data Lookback Seconds", defaultValue: 600, submitOnChange: true, width:4)

}
                         
@SuppressWarnings('unused')
void installed() {

}

void initialize() {

}

void configure() {
   
}

void updated(){
    updateAttr("dbg", apiUri)   
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.contains("Your hub is starting up"))
       return
    sendEvent(name:aKey, value:aValue, unit:aUnit)
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void getGlucose() {
    parent.getGlucose(device.deviceNetworkId, glucoseRange)
}
