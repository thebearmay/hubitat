 /*
 * Hub Info
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
 *    2020-12-07  thebearmay	 Original version 0.1.0
 *    2021-01-30  thebearmay     Add full hub object properties
 *    2021-01-31  thebearmay     Code cleanup, release ready
 *    2021-01-31  thebearmay     Putting a config delay in at initialize to make sure version data is accurate
 *    2021-02-16  thebearmay     Add text date for restart
 *    2021-03-04  thebearmay     Added CPU and Temperature polling 
 *    2021-03-05  thebearmay     Merged CSteele additions and added the degree symbol and scale to the temperature attribute 
 *    2021-03-05  thebearmay	 Merged addtions from LGKhan: Added new formatted uptime attr, also added an html attr that stores a bunch of the useful 
 *					                info in table format so you can use on any dashboard
 *    2021-03-06  thebearmay     Merged security login from BPTWorld (from dman2306 rebooter app)
 *    2021-03-06  thebearmay     Change numeric attributes to type number
 *    2021-03-08  thebearmay     Incorporate CSteele async changes along with some code cleanup and adding tags to the html to allow CSS overrides
 *    2021-03-09  thebearmay     Code tightening as suggested by CSteele, remove state variables, etc.
 *    2021-03-11  thebearmay     Add Sensor capability for Node-Red/MakerAPI 
 *    2021-03-11  thebearmay     Security not set right at initialize, remove state.attrString if it exists (now a local variable)
 *    2021-03-19  thebearmay     Add attributes for JVM Total, Free, and Free %
 *                               Add JVM info to HTML
 *                               Fix for exceeded 1024 attr limit
 *    2021-03-20  thebearmay     Firmware 2.2.6.xxx support, CPU 5min Load
 *    2021-03-23  thebearmay     Add DB Size
 *    2021-03-24  thebearmay     Calculate CPU % from load 
 *    2021-03-28  thebearmay     jvmWork.eachline error on reboot 
 *    2021-03-30  thebearmay     Index out of bounds on reboot
 *    2021-03-31  thebearmay 	 jvm to HTML null error (first run)
 *    2021-04-13  thebearmay     pull in suggested additions from lgkhan - external IP and combining some HTML table elements
 *    2021-04-14  thebearmay     add units to the HTML
 *    2021-04-20  thebearmay     provide a smooth transition from 1.8.x to 1.9.x
 *    2021-04-26  thebearmay     break out polls as separate preference options
 */
import java.text.SimpleDateFormat
static String version()	{  return '2.0.0'  }

metadata {
    definition (
		name: "Hub Information", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfo.groovy"
	) {
        capability "Actuator"
        capability "Configuration"
	    capability "Initialize"
        capability "Sensor"
	    capability "TemperatureMeasurement"
        
	    attribute "latitude", "string"
	    attribute "longitude", "string"
        attribute "hubVersion", "string"
        attribute "id", "string"
        attribute "name", "string"
        attribute "data", "string"
        attribute "zigbeeId", "string"
        attribute "zigbeeEui", "string"
        attribute "hardwareID", "string"
        attribute "type", "string"
        attribute "localIP", "string"
        attribute "localSrvPortTCP", "string"
        attribute "uptime", "number"
        attribute "lastUpdated", "string"
        attribute "lastHubRestart", "string"
	    attribute "firmwareVersionString", "string"
        attribute "timeZone", "string"
        attribute "temperatureScale", "string"
        attribute "zipCode", "string"
        attribute "locationName", "string"
        attribute "locationId", "string"
        attribute "lastHubRestartFormatted", "string"
        attribute "freeMemory", "number"
	    attribute "temperatureF", "string"
        attribute "temperatureC", "string"
        attribute "formattedUptime", "string"
        attribute "html", "string"
        attribute "jvmTotal", "number"
        attribute "jvmFree", "number"
        attribute "jvmFreePct", "number"
        attribute "cpu5Min", "number"
        attribute "cpuPct", "number"
        attribute "dbSize", "number"
        attribute "publicIP", "string"

            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("tempPollEnable", "bool", title: "Enable Temperature Polling")
    input("freeMemPollEnabled", "bool", title: "Enable Free Memory Polling")
    input("cpuPollEnabled", "bool", title: "Enable CPU & JVM Polling")
    input("dbPollEnabled","bool", title: "Enable DB Size Polling")
    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable) 
        input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", default:300, submitOnChange: true)
    input("publicIPEnable", "bool", title: "Enable Querying the cloud \nto obtain your Public IP Address?", default: false, required: true, submitOnChange: true)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", default: false, required: false, submitOnChange: true)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }

}

def installed() {
	log.trace "installed()"
    initialize()
}

def initialize(){
    log.trace "Hub Information initialize()"
// psuedo restart time - can also be set at the device creation or by a manual initialize
    restartVal = now()
    updateAttr("lastHubRestart", restartVal)	
    sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(restartVal))
    if (!security)  device.updateSetting("security",[value:"false",type:"bool"])

    runIn(30,configure)
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
    if(tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable){
        unschedule()
        getPollValues()
    }
    
    if (attribEnable) 
        formatAttrib() 
    else 
        sendEvent(name: "html", value: "<table></table>", isChanged: true); 
}

def configure() {
    if(debugEnable) log.debug "configure()"
    locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    def myHub = location.hub
    hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        updateAttr(hubProp[i], myHub["${hubProp[i]}"])
    }
    for(i=0;i<locProp.size();i++){
        updateAttr(locProp[i], location["${locProp[i]}"])
    }
    formatUptime()
    updateAttr("hubVersion", location.hub.firmwareVersionString) //retained for backwards compatibility
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)
    updateAttr("lastUpdated", now())
    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable) getPollValues()
    if (attribEnable) formatAttrib()
}

def updateAttr(aKey, aValue, aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def formatUptime(){
    String attrval 
    try {
        Integer ut = device.currentValue("uptime").toDouble()
        Integer days = (ut/(3600*24))
        Integer hrs = (ut - (days * (3600*24))) /3600
        Integer min =  (ut -  ((days * (3600*24)) + (hrs * 3600))) /60
        Integer sec = ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))
    
        attrval = days.toString() + " days, " + hrs.toString() + " hrs, " + min.toString() + " min, " + sec.toString() + " sec"
        updateAttr("formattedUptime", attrval) 
    } catch(Exception ex) { 
        updateAttr("formattedUptime", "")
    }
}

def formatAttrib(){ 
	if(debugEnable) log.debug "formatAttrib"
	def attrStr = "<table id='hubInfoTable'>"
	
	attrStr += addToAttr("Name","name")
	attrStr += addToAttr("Version","hubVersion")
    if(publicIPEnable) {
        def combine = ["localIP", "publicIP"]        
        attrStr += combineAttr("IP Local/Public", (String[])combine)        
    } else
	    attrStr += addToAttr("Address","localIP")
    if(cpuPollEnabled) {
    	attrStr += addToAttr("Free Memory","freeMemory","int")
        if(device.currentValue("cpu5Min")){
            def combine = ["cpu5Min", "cpuPct"]        
            attrStr += combineAttr("CPU Load/Load%", (String[])combine)
        }

        def combine = ["jvmTotal", "jvmFree", "jvmFreePct"]
        attrStr += combineAttr("JVM Total/Free/%", (String[])combine)
    }
    
    if(device.currentValue("dbSize")) attrStr +=addToAttr("DB Size","dbSize")
    
	attrStr += addToAttr("Last Restart","lastHubRestartFormatted")
	attrStr += addToAttr("Uptime","formattedUptime")
    
    if(tempPollEnable) {
    	def tempAttrib = location.temperatureScale=="C" ? "temperatureC" : "temperatureF"
	    attrStr += addToAttr("Temperature",tempAttrib)
    }
	attrStr += "</table>"

	if (debugEnable) log.debug "after calls attr string = $attrStr"
	updateAttr("html", attrStr)
}

def combineAttr(name, String[] keys){
    if(enableDebug) log.debug "adding $name, $keys.length"

    retResult = '<tr><td align="left">'
    retResult += name + '</td><td align="left">'
    
    keyResult = ""
    for (i = 0;i < keys.length; i++) {
        keyResult+= device.currentValue(keys[i])
        attrUnit = getUnitFromState(keys[i])
        if (attrUnit != "null") keyResult+=attrUnit
        if (i < keys.length - 1) keyResult+= " / "
    }
            
    retResult += keyResult+'</td></tr>'
}

def addToAttr(name, key, convert = "none")
{
    if(enableDebug) log.debug "adding $name, $key"
    retResult = '<tr><td align="left">'
    retResult += name + '</td><td align="left">'

    attrUnit = getUnitFromState(key)
    if (attrUnit == "null") attrUnit =""
    
    if(device.currentValue(key)){
        if (convert == "int"){
            retResult += device.currentValue(key).toInteger().toString()+attrUnit
        } else if (name=="Temperature"){
            // span uses integer value to allow CSS override 
            retResult += "<span class=\"temp-${device.currentValue('temperature').toInteger()}\">" + device.currentValue(key)+attrUnit + "</span>"
        } else retResult += device.currentValue(key)+attrUnit
    }
    retResult += '</td></tr>'
}

def getPollValues(){
    // start - Modified from dman2306 Rebooter app
    if(security) {
        httpPost(
            [
                uri: "http://127.0.0.1:8080",
                path: "/login",
                query: [ loginRedirect: "/" ],
                body: [
                    username: username,
                    password: password,
                    submit: "Login"
                ]
            ]
        ) { resp -> cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0) }
    }
    // End - Modified from dman2306 Rebooter app
    
    // get Temperature
    if(tempPollEnable) {
        params = [
            uri: "http://${location.hub.localIP}:8080",
            path:"/hub/advanced/internalTempCelsius",
            headers: [ "Cookie": cookie ]
        ]
        if(debugEnable)log.debug params
        asynchttpGet("getTempHandler", params)
    }
    
    // get Free Memory
    if(freeMemPollEnabled) {
        params = [
            uri: "http://${location.hub.localIP}:8080",
            path:"/hub/advanced/freeOSMemory",
            headers: [ "Cookie": cookie ]
        ]
        if(debugEnable)log.debug params
        asynchttpGet("getFreeMemHandler", params)
    }
    
    // get Free JVM & CPU
    if(cpuPollEnabled) {
        if (location.hub.firmwareVersionString <= "2.2.5.131") {
            params = [
                uri: "http://${location.hub.localIP}:8080",
                path:"/hub/advanced/freeOSMemoryHistory",
                headers: [ "Cookie": cookie ]
            ]
        } else {
            params = [
                uri: "http://${location.hub.localIP}:8080",
                path:"/hub/advanced/freeOSMemoryLast",
                headers: [ "Cookie": cookie ]
            ]
        }
        if(debugEnable)log.debug params
        asynchttpGet("getJvmHandler", params)
    }
    
    //Get DB size
    if(dbPollEnabled){
        params = [
            uri: "http://${location.hub.localIP}:8080",
            path:"/hub/advanced/databaseSize",
            headers: [ "Cookie": cookie ]
        ]
	
        if(debugEnable)log.debug params
        asynchttpGet("getDbHandler", params)
    }
    
    //get Public IP 
    if(publicIPEnable) {
        params =
	    [
		    uri:  "https://ifconfig.co/",
            headers: [ 
                   Host: "ifconfig.co",               
                   Accept: "application/json"
            ]
	    ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getIfHandler", params)
    }
    
    //End Pollable Gets
    
    updateAttr("uptime", location.hub.uptime)
	formatUptime()
    
    if (debugEnable) log.debug "tempPollRate: $tempPollRate"
    
    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable) {
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,getPollValues)
        }else {
            runIn(tempPollRate,getPollValues)
        }
    }
}


def getTemp(){  // this is to handle the upgrade path from >= 1.8.x
    log.info "Upgrading HubInfo polling from 1.8.x"
    unschedule(getTemp)
    getPollValues()
}


def getTempHandler(resp, data) {
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    tempWork = new Double(resp.data.toString())
    		if(debugEnable) log.debug tempWork
	    	if (location.temperatureScale == "F")
		        updateAttr("temperature",celsiusToFahrenheit(tempWork),"°F")
		    else
		        updateAttr("temperature",tempWork,"°C")

		    updateAttr("temperatureF",celsiusToFahrenheit(tempWork)+ " °F")
    		updateAttr("temperatureC",tempWork+ " °C")
	    }
    } catch(Exception ex) { 
        respStatus = resp.getStatus()
        log.warn "getTemp httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

def getFreeMemHandler(resp, data) {
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    memWork = new Integer(resp.data.toString())
		    if(debugEnable) log.debug memWork
            updateAttr("freeMemory",memWork)
	    }
    } catch(Exception ex) { 
        respStatus = resp.getStatus()
        log.warn "getFreeMem httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
}


def getJvmHandler(resp, data) {
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            jvmWork = resp.data.toString()
        }
        if (attribEnable) runIn(5,formatAttrib) //allow for events to register before updating - thebearmay 210308
    } catch(Exception ex) { 
        respStatus = resp.getStatus()
        log.warn "getJvm httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
    if (jvmWork) {
        lineCount = 0
        jvmWork.eachLine{
            lineCount++
        }
        lineCount2 = 0
        jvmWork.eachLine{
            lineCount2++
            if(lineCount==lineCount2)
                jvmArr = it.split(",")
        }
        if(jvmArr.length > 1){
            jvmTotal = jvmArr[2].toInteger()
            jvmFree = jvmArr[3].toInteger()
            Double jvmFreePct = (jvmFree/jvmTotal)*100
            updateAttr("jvmTotal",jvmTotal)
            updateAttr("jvmFree",jvmFree)
            updateAttr("jvmFreePct",jvmFreePct.round(1),"%")
            if(jvmArr.length > 4) {
                cpuWork=jvmArr[4].toDouble()
                updateAttr("cpu5Min",cpuWork.round(2))
                cpuWork = (cpuWork/4)*100  //Load / #Cores - if cores change will need adjusted to reflect
                updateAttr("cpuPct",cpuWork.round(2),"%")
            }
        }
    }
}

def getDbHandler(resp, data) {
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    dbWork = new Integer(resp.data.toString())
    		if(debugEnable) log.debug dbWork
    		updateAttr("dbSize",dbWork,"MB")
	    }
    } catch(Exception ex) { 
        respStatus = resp.getStatus()
        log.warn "getDb httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

def getIfHandler(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            ipData = parseJson(resp.data)
			updateAttr("publicIP",ipData.ip)
		} else {
			log.warn "Status ${resp.getStatus()} while fetching Public IP"
		} 
    } catch (Exception ex){
        log.info ex
    }
}   

def getUnitFromState(attrName){
    updateAttr("debug", attrName)
    def wrkStr = device.currentState(attrName).toString()
    start = wrkStr.indexOf('(')+1
    end = wrkStr.length() - 1
    wrkStr = wrkStr.substring(start,end)
    stateParts = wrkStr.split(',')
    if(stateParts.size()>=4)
        return stateParts[3].trim()
    else 
        return
}

// Begin JSON Parser

def getNearestEnd(String json, int start, String head, String tail) {
    def end = start
    def count = 1
    while (count > 0) {
        end++
        def c = json.charAt(end)
        if (c == head) {
            count++
        } else if (c == tail) {
            count--
        }
    }
    return end;
}

//  Parse JSON Object
def parseObject(String json) {
    def map = [:]
    def length = json.length()
    def index = 1
    def state = 'none' // none, string-value, other-value
    def key = ''
    while (index < length -1) {
        def c = json.charAt(index)
        switch(c) {
            case '"':
                if (state == 'none') {
                    def keyStart = index + 1;
                    def keyEnd = keyStart;
                    while (json.charAt(keyEnd) != '"') {
                        keyEnd++
                    }
                    index = keyEnd
                    def keyValue = json[keyStart .. keyEnd - 1]
                    key = keyValue
                } else if (state == 'value') {
                    def stringStart = index + 1;
                    def stringEnd = stringStart;
                    while (json.charAt(stringEnd) != '"') {
                        stringEnd++
                    }
                    index = stringEnd
                    def stringValue = json[stringStart .. stringEnd - 1]
                    map.put(key, stringValue)
                }
                break

            case '{':
                def objectStart = index
                def objectEnd = getNearestEnd json, index, '{', '}'
                def objectValue = json[objectStart .. objectEnd]
                map.put(key, parseObject(objectValue))
                index = objectEnd
                break

            case '[':
                def arrayStart = index
                def arrayEnd = getNearestEnd(json, index, '[', ']')
                def arrayValue = json[arrayStart .. arrayEnd]
                map.put(key, parseArray(arrayValue))
                index = arrayEnd
                break

            case ':':
                state = 'value'
                break

            case ',':
                state = 'none'
                key = ''
                break;

            case ["\n", "\t", "\r", " "]:
                break

            default:
                break
        }
        index++
    }

    return map
}

// Parse JSON Array
def parseArray(String json) {
    def list = []
    def length = json.length()
    def index = 1
    def state = 'none' // none, string-value, other-value
    while (index < length -1) {
        def c = json.charAt(index)
        switch(c) {
            case '"':
                def stringStart = index + 1;
                def stringEnd = stringStart;
                while (json.charAt(stringEnd) != '"') {
                    stringEnd++
                }
                def stringValue = json[stringStart .. stringEnd - 1]
                list.add(stringValue)
                index = stringEnd
                break

            case '{':
                def objectStart = index
                def objectEnd = getNearestEnd(json, index, '{', '}')
                def objectValue = json[objectStart .. objectEnd]
                list.add(parseObject(objectValue))
                index = objectEnd
                break

            case '[':
                def arrayStart = index
                def arrayEnd = getNearestEnd(json, index, '[', ']')
                def arrayValue = json[arrayStart .. arrayEnd]
                list.add(parseArray(arrayValue))
                index = arrayEnd
                break

            case ["\n", "\t", "\r", " "]:
                break

            case ',':
                state = 'none'
                key = ''
                break;

            default:
                break
        }
        index++
    }

    return list
}

//  Parse the JSON - can be Object or Array
def parseJson(String json) {
    def start = json[0]
    if (start == '[') {
        return parseArray(json)
    } else if (start == '{') {
        return parseObject(json)
    } else {
        return null
    }
}

//End JSON Parser


void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
