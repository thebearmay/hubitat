/*
* LimitExceed Monitor Device
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
*	2025-08-09	thebearmay		Fixed run away error condition due to missing data field
*/
import java.text.SimpleDateFormat
import groovy.transform.Field
//Logger imports
import groovy.json.JsonSlurper
import hubitat.device.HubAction
import hubitat.device.Protocol


static String version()	{  return '0.0.2'  }

metadata {
	definition (
			name: "Limit Exceeded Monitor", 
			namespace: "thebearmay", 
			description: "Simple driver to trap a limit exceeded exception in the log.",
			author: "Jean P. May, Jr.",
			importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/limitExceeded.groovy",
            singleThreaded: true
		) 
	{

		capability "Actuator" 
		capability "Initialize"

		attribute "limitExceededFlag", "STRING"
       
        command "disconnect"
        command "reset"

	}   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
	input("msgCount", "number", title:"Number of messages during the interval containing 'LimitExceeded' needed to raise flag", defaultValue:5)
    input("resetInterval", "number", title:"Interval in Minutes for counter reset", defaultValue:5)

}

void installed() {
	if (debugEnable) log.trace "installed()"
	configure()
}

void updated(){
	if (debugEnable) log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
    configure()
}

void configure() {
	log.trace "configure()"
	if(msgCount == null) device.updateSetting("nmsgCount",[value:5,type:"number"])
    if(resetInterval == null) device.updateSetting("resetInterval",[value:5,type:"number"])
    reset()
	runIn(5, "connect")
    runIn(resetInterval*60,"reset")
}

void initialize(){
    reset()
    runIn(30, "connect")
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.contains("Your hub is starting up"))
       return

    sendEvent(name:aKey, value:aValue, unit:aUnit, descriptionText:"$aKey : $aValue$aUnit")
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void reset(){
    state.flagCount = 0
    updateAttr("limitExceededFlag","false")
    runIn(resetInterval*60,"reset")
}

void logsOff(){
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void push() {
 	configure()
}

void parse(String description) {

    def descData = new JsonSlurper().parseText(description)
    //log.debug "${descData.properties}"
    if(descData.msg.indexOf('imitExceeded') > 0) {
        state.flagCount++
        if(msgCount < state.flagCount) 
           updateAttr("limitExceededFlag",'true')
    } 
}
    
void connect() {
    if (debugEnable) { log.debug "attempting connection" }
    try {
        interfaces.webSocket.connect("http://localhost:8080/logsocket")
        pauseExecution(1000)
    } catch (e) {
        log.error "initialize error: ${e.message}"
    }

}

void disconnect() {
    interfaces.webSocket.close()
}

void webSocketStatus(String message) {
    // handle error messages and reconnect
    if (debugEnable) { log.debug "Got status ${message}" }
    if (message.startsWith("failure")) {
        // reconnect in a little bit
        runIn(5, connect)
    }
}
