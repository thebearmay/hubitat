/* HomeKit Restart Button
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
 *	Date		Who		        Description
 *	----------	-------------	----------------------------------------------------------------------------
 *  16OCt2023    thebearmay     Initial code
*/
import groovy.json.JsonSlurper
@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "HomeKit Restart", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hkRestart.groovy"
    ) {
        capability "Actuator"
        capability "Momentary"
        capability "Configuration"
        
        attribute "lastStatus", "string"
        attribute "lastStatusTime", "string"

    }   
}

preferences {
    if(!state.hkApp || state.hkApp == -1 ) 
        input("ErrMessage", "hidden", title: "<span style='font-weight:bold;color:red;background-color:yellow;font-size:larger'>HomeKit Integration has not been set</span>")
    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("rStartRate", "number", title: "Restart Interval (minutes)\nDefault:480", defaultValue:480, submitOnChange: true)
    if(rStartRate > 0){
        runIn(rStartRate*60, "push")
    } else
        unschedule("push")
}

def configure(){
    def params = [
		uri: "http://127.0.0.1:8080/hub2/appsList",
	    contentType: "application/json",
        followRedirects: false,
        textParser: false
    ]
	
    appId = -1
	try {
		httpGet(params) { resp ->  
            appId = -1
            if(debugEnabled) log.debug "GET: ${resp.data.apps}"
            resp.data.apps.each {a ->
                if(debugEnabled) log.debug "${a.data.type}"
                if("${a.data.type}" == "HomeKit Integration") {
                    appId = a.data.id
                    if(debugEnabled) log.debug "Found it ${a.data.id}"
                }
            }
        }

	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    
    state.hkApp = appId
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def restart(){
    log.debug "restart"
    push()
}

void push(){
    if(debugEnabled) log.debug "button pushed"
    try{
        params = [
            uri: "http://127.0.0.1:8080/installedapp/btn",
            headers: [
                "Content-Type": "application/x-www-form-urlencoded",
                Accept: "application/json"
            ],
            body:[id:"${state.hkApp}",name:"reloadDevices",undefined:"clicked","reloadDevices.type":"button"],
            followRedirects: false
        ]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
            updateAttr("lastStatus","$resp.data")
            updateAttr("lastStatusTime",new Date())
		}
    }catch (e){
        log.error e.message
    }
        
    if(rStartRate == null) 
        device.updateSetting("rStartRate",[value:480,type:"number"])
    if(rStartRate > 0){
        runIn(rStartRate*60, "push")
    }
}

def installed() {
    initialize()
}

def uninstalled(){
    unschedule()
}

def initialize() {
    configure()
    if(rStartRate && rStartRate > 0){
        runIn(rStartRate*60, "push")
    }    
}  
    
def updated(){
	initialize()
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
