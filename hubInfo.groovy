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
 *    2021-03-20  thebearmay     Firmware 2.2.6.xxx support, CPU 5min %
 *                               DB Size
 */
import java.text.SimpleDateFormat
static String version()	{  return '1.8.1'  }

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
        attribute "dbSize", "number"

            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("tempPollEnable", "bool", title: "Enable Temperature/Memory/HTML Polling")
    if (tempPollEnable) input("tempPollRate", "number", title: "Temperature/Memory Polling Rate (seconds)\nDefault:300", default:300, submitOnChange: true)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
    input("attribEnable", "bool", title: "Enable Info attribute?", default: false, required: false, submitOnChange: true)
}

def installed() {
	log.trace "installed()"
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
    if (tempPollEnable) getTemp()
    if (attribEnable) formatAttrib()
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){
    log.trace "Hub Information initialize()"
// psuedo restart time - can also be set at the device creation or by a manual initialize
    restartVal = now()
    updateAttr("lastHubRestart", restartVal)	
    sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(restartVal))
    if (!security)  device.updateSetting("security",[value:"false",type:"bool"])
    if(state.attrString) state.remove("attrString")
    runIn(30,configure)
}

def formatUptime(){
    String attrval 
    try {
        Integer ut = device.currentValue("uptime").toDouble()
        Integer days = (ut/(3600*24))
        Integer hrs = (ut - (days * (3600*24))) /3600
        Integer min =  (ut -  ((days * (3600*24)) + (hrs * 3600))) /60
        Integer sec = ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))
    
        attrval = days.toString() + " days, " + hrs.toString() + " hrs, " + min.toString() + " min, " + sec.toString() + " sec."
        sendEvent(name: "formattedUptime", value: attrval, isChanged: true) 
    } catch(Exception ex) { 
        sendEvent(name: "formattedUptime", value: "", isChanged: true)
    }
}

def formatAttrib(){ 
	if(debugEnable) log.debug "formatAttrib"
	def attrStr = "<table id='hubInfoTable'>"
	
	attrStr += addToAttr("Name","name")
	attrStr += addToAttr("Version","hubVersion")
	attrStr += addToAttr("Address","localIP")
	attrStr += addToAttr("Free Memory","freeMemory","int")
    if(device.currentValue("cpu5Min")) attrStr +=addToAttr("CPU 5min %","cpu5Min")
    attrStr += addToAttr("JVM Total Memory", "jvmTotal", "int")    
    attrStr += addToAttr("JVM Free Memory", "jvmFree", "int")
    attrStr += addToAttr("JVM Free %", "jvmFreePct")
    if(device.currentValue("dbSize")) attrStr +=addToAttr("DB Size (MB)","dbSize")
	attrStr += addToAttr("Last Restart","lastHubRestartFormatted")
	attrStr += addToAttr("Uptime","formattedUptime")
	def tempAttrib = location.temperatureScale=="C" ? "temperatureC" : "temperatureF"
	attrStr += addToAttr("Temperature",tempAttrib)
	attrStr += "</table>"

	if (debugEnable) log.debug "after calls attr string = $attrStr"
	sendEvent(name: "html", value: attrStr, isChanged: true)
}

def addToAttr(String name, String key, String convert = "none")
{
    if(enableDebug) log.debug "adding $name, $key"
    String retResult = '<tr><td align="left">'
    retResult += name + '</td><td align="left">'
   
    if (convert == "int"){
        retResult += device.currentValue(key).toInteger().toString()
    } else if (name=="Temperature"){
        // span uses integer value to allow CSS override 
        retResult += "<span class=\"temp-${device.currentValue('temperature').toInteger()}\">" + device.currentValue(key) + "</span>"
    } else retResult += device.currentValue(key)
    
    retResult += '</td></tr>'
}

//start CSteele changes 210307
def getTemp(){
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
    
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/internalTempCelsius",
        headers: [ "Cookie": cookie ]
    ]
    if(debugEnable)log.debug params
    asynchttpGet("getTempHandler", params)
    
    // get Free Memory
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/freeOSMemory",
        headers: [ "Cookie": cookie ]
    ]
    if(debugEnable)log.debug params
    asynchttpGet("getFreeMemHandler", params)
    
    // get Free JVM & CPU Avg

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
    
    //Get DB size
        params = [
            uri: "http://${location.hub.localIP}:8080",
            path:"/hub/advanced/databaseSize",
            headers: [ "Cookie": cookie ]
        ]
	
    if(debugEnable)log.debug params
    asynchttpGet("getDbHandler", params)
    
    updateAttr("uptime", location.hub.uptime)
	formatUptime()
    
    if (debugEnable) log.debug "tempPollRate: $tempPollRate"
    
    if (tempPollEnable) {
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,getTemp)
        }else {
            runIn(tempPollRate,getTemp)
        }
    }
}


def getTempHandler(resp, data) {
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    tempWork = new Double(resp.data.toString())
    		if(debugEnable) log.debug tempWork
	    	if (location.temperatureScale == "F")
		        updateAttr("temperature",celsiusToFahrenheit(tempWork),"°${location.temperatureScale}")
		    else
		        updateAttr("temperature",tempWork,"°${location.temperatureScale}")

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
// end CSteele changes 210307

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
    jvmTotal = jvmArr[2].toInteger()
    jvmFree = jvmArr[3].toInteger()
    Double jvmFreePct = (jvmFree/jvmTotal)*100
    updateAttr("jvmTotal",jvmTotal)
    updateAttr("jvmFree",jvmFree)
    updateAttr("jvmFreePct",jvmFreePct.round(3),"%")
    if(jvmArr.length > 4) {
        updateAttr("cpu5Min",jvmArr[4].toDouble()*100,"%")
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

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
    if (attribEnable) 
        formatAttrib() 
    else 
        sendEvent(name: "html", value: "<table></table>", isChanged: true); 
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
