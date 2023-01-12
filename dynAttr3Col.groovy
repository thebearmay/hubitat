 /*
 * Dynamic Attribute 3 Column Device
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
 * 
 */

static String version()	{  return '0.1.0'  }

metadata {
    definition (
		name: "Dynamic 3 Column Attribute Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/dynAttr3Col.groovy"
	) {
        	capability "Actuator"
		
    attribute "attrRet", "JSON"
    attribute "key", "STRING"
    attribute "v1", "STRING"
    attribute "v2", "STRING"
  
    command "setAttribute", [[name:"attrName*", type:"STRING", description:"Attribute Name/Key"],[name:"attrValue1*", type:"STRING", description:"Attribute Value, \\0 to remove key"],[name:"attrValue2*", type:"STRING", description:"Attribute Value 2"]]
    command "setEntry", [[name:"entryStr*", type:"STRING", description: "Key:Value:Value"]]
    command "clearStateVariables"
    command "getValue", [[name:"lookupKey*", type:"STRING", description:"Key to retrieve values for"]]
    command "getEntryValue", [[name:"lookupKey*", type:"STRING", description:"Key to retrieve the Key:Value:Value set for"]]
    command "parseFile"
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("filePath", "string", title: "Path to file", width:4)
    input("pollInt", "number", title: "Polling Interval in Seconds", width: 4)
}

def installed() {
	log.trace "installed()"
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
    if(pollInt == 0 || pollInt == null || filePath == null)
        unschedule("parseFile")
    else
        parseFile()
}

def setAttribute(attrName,attrValue,attrValue2) {
    if(debugEnable) log.debug "setAttribute($attrName, $attrValue, $attrValue2)"
    if (attrValue == "\\0")
        state.remove(attrName)
    else
        state[attrName]="$attrValue, $attrValue2"
    attrRet = formatMap(state)
    
    if(debugEnable) log.debug attrRet
    sendEvent(name:"attrRet", value:attrRet)
}

def setEntry(entryStr){
    entrySplit = entryStr.split(":")
    if (entrySplit.size() == 1){
        sendEvent(name:"attrRet", value:"Error - Expected : separated string")
        return
    }
    setAttribute(entrySplit[0], entrySplit[1], entrySplit[2])
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
    semaphor=state["$key"]
    sList = semaphor.split(",")
    sendEvent(name:"key",value:key)
    sendEvent(name:"v1",value:sList[0])
    sendEvent(name:"v2",value:sList[1])
}

def getEntryValue(key) {
    if(debugEnable) log.debug "getEntryValue($key)"
    semaphor=state.get(key)
    attrRet = formatMap(["$key":"$semaphor"])
    sendEvent(name:"attrRet", value:attrRet)
}

def clearStateVariables(){
     state.clear()   
}

void parseFile(){
    fData = readExtFile(filePath)
    if(fData == null) return
    sendEvent(name:"semaphor",value:"$fData")
    fRecs=fData.split("\n")
    fRecs.each{
        item=it.split(",")
        if (item.size() == 3)
            setAttribute(item[0],item[1],item[2])
    }
    if(pollInt > 0)
        runIn(pollInt, "parseFile")
}

String readExtFile(fName){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}


void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
