 /*
 * Dynamic Attribute Device
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
 *    2021-01-04  thebearmay	 Original version 0.1.0
 *    2021-01-05  thebearmay     v0.5.0 add JSON format, getValue, and getEntryValue
 * 
 */

static String version()	{  return '0.5.0'  }

metadata {
    definition (
		name: "Dynamic Attribute Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/dynAttribute.groovy"
	) {
        	capability "Actuator"
		
    attribute "attrRet", "JSON"
    attribute "semaphor", "STRING"
  
    command "setAttribute", [[name:"attrName*", type:"STRING", description:"Attribute Name/Key"],[name:"attrValue*", type:"STRING", description:"Attribute Value, \\0 to remove key"]]   
    command "clearStateVariables"
    command "getValue", [[name:"lookupKey*", type:"STRING", description:"Key to retrieve value for"]]
    command "getEntryValue", [[name:"lookupKey*", type:"STRING", description:"Key to retrieve the Key:Value pair for"]]
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

def setAttribute(attrName,attrValue) {
    if(debugEnable) log.debug "setAttribute($attrName, $attrValue)"
    if (attrValue == "\\0")
        state.remove(attrName)
    else
        state[attrName]=attrValue
    attrRet = formatMap(state)
    
    if(debugEnable) log.debug attrRet
    sendEvent(name:"attrRet", value:attrRet)
}

def formatMap(mapIn){
    formatJSON = "{"
    for (entry in mapIn) {
        formatJSON += "\""+entry.key+"\":\""+entry.value+"\","
    }
    if(formatJSON > "{")
        formatJSON = formatJSON[0..-2]+"}"
    else
        formatJSON += " }"

    return formatJSON
}

def getValue(key) {
    if(debugEnable) log.debug "getValue($key)"
    semaphor=state.get(key)
    sendEvent(name:"semaphor",value:semaphor)
}

def getEntryValue(key) {
    if(debugEnable) log.debug "getEntryValue($key)"
    semaphor=state.get(key)
    sendEvent(name:"semaphor",value:"$key:\"$semaphor\"")
}

def clearStateVariables(){
     state.clear()   
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
