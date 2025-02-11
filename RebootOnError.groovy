/*
 * 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
*/
//import groovy.transform.Field
import groovy.json.JsonSlurper
import hubitat.device.HubAction
import hubitat.device.Protocol
static String version()	{  return '0.0.1'  }

metadata {
    definition (
        name: "Reboot on Error", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/RebootOnError.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        
        command "startMonitoring"
        command "stopMonitoring"
    }
}

preferences {
			input "debugEnabled", "bool", title: "<b>Enable Debug Logging</b>", defaultValue: false, submitOnChange:true
            input "numErr", "number", title:"<b>Number of Errors to Trigger</b>", defaultValue:10, submitOnChange:true
            input "timeInterval", "number", title:"<b>Time Interval (seconds) to Reach Trigger</b>", defaultValue:60, submitOnChange:true
            input "errMsgs","string",title:"<b>Error messages that will trigger (comma separated)</b>", defaultValue: 'Exception queue is full', submitOnChange:true
    		input "msgLvl", "enum", title:"<b>Minimum Log Level to examine</b>", options: ['Error', 'Warn', 'Info'], defaultValue: "Error", submitOnChange:true
            input "allowReboot","bool",title:"<b>Allow app to reboot the hub</b>", defaultValue: "false", submitOnChange:true
}

def configure(){
    initialize()
}

def installed() {
    initialize()
}

def initialize(){
    if(state.monitoring || state.monitoring == 'true')
       startMonitoring()    
}

def updated(){
	if(debugEnabled) runIn(1800,logsOff)
}


void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

void startMonitoring(){
    state.monitoring = true
	if(!allowReboot || allowReboot == null)
		log.warn "<b style='color:red'>Monitoring Enabled but Allow Reboot is False</b>"
	connect()
}

void stopMonitoring(){
	state.monitoring = false
	disconnect()
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
    if (debugEnable) { log.debug "webSocket status: ${message}" }
    if (message.startsWith("failure")) {
        runIn(5, connect)
    }
}

void parse(String description) {

    def descData = new JsonSlurper().parseText(description)
    String message = escapeStringHTMlforMsg(descData.msg)
    String containsStr = 'error'
    if(msgLvl == 'Warn')
    	containsStr+=', warn'
    if(msgLvl == 'Info')
    	containsStr+=', warn, info'
    
    
    if(!containsStr.contains("${descData.level}"))  // don't remove this without putting in at least a filter of debug entries
    	return
    if(state.errStart == null)
    	state.errStart = 0
	errIntervalEnd = state.errStart + (timeInterval * 1000)
    
   	if(errIntervalEnd < tNow  && state.errStart > 0) {
		if(debugEnabled)
			log.debug "Interval Expired"
		state.errStart = 0
    }
    
    if(errMsgs == null) {
    	errMsgs = 'Exception queue is full'
        device.updateSetting('errMsgs',[value:'Exception queue is full', type:'string'])
    }
    msgArr = errMsgs.split(',')
    foundIt = false
    msgArr.each {
        if(it.contains(message)) 
        	foundIt = true
    }
    if(debugEnabled){
        log.debug "MsgArr: $msgArr<br>Log Message: $message<br>Found: $foundIt"
    }
    if(foundIt) {
        tNow = new Date().getTime()
		if(state.errStart == null || state.errStart == 0)
        	state.errStart = tNow
        if(!state.errCount) 
        	state.errCount = 0
        state.errCount++
        if(state.errCount >= numErr){
            state.errCount = 0
            rebootHub()
        }
    }
}

private String escapeStringHTMlforMsg(String str) {
    if (str) {
        str = str.replaceAll("&lt;", "<") // Escape commas.
        str = str.replaceAll("&gt;", ">") // Escape equal signs.
        str = str.replaceAll("&#027;", "'") // Escape double quotes.
        str = str.replaceAll("&#039;", "'")  // Replace apostrophes with underscores.
        str = str.replaceAll("&apos;", "'")
    }
    else {
        str = 'null'
    }
    return str
}

void reboot() {
    if(!allowReboot || allowReboot == null){
        log.error "Reboot was requested, but allowReboot was set to false"
        return
    }
    log.info "Hub Reboot requested"
    
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot"
		]
	) {		resp ->	} 
}
