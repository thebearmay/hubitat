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
 *    2020-12-07  thebearmay     Original version 0.1.0
 *    2021-01-30  thebearmay     Add full hub object properties
 *    2021-01-31  thebearmay     Code cleanup, release ready
 *    2021-01-31  thebearmay     Putting a config delay in at initialize to make sure version data is accurate
 *    2021-02-16  thebearmay     Add text date for restart
 *    2021-03-04  thebearmay     Added CPU and Temperature polling 
 *    2021-03-05  thebearmay     Merged CSteele additions and added the degree symbol and scale to the temperature attribute 
 *    2021-03-05  thebearmay     Merged addtions from LGKhan: Added new formatted uptime attr, also added an html attr that stores a bunch of the useful 
 *                                    info in table format so you can use on any dashboard
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
 *    2021-03-31  thebearmay      jvm to HTML null error (first run)
 *    2021-04-13  thebearmay     pull in suggested additions from lgkhan - external IP and combining some HTML table elements
 *    2021-04-14  thebearmay     add units to the HTML
 *    2021-04-20  thebearmay     provide a smooth transition from 1.8.x to 1.9.x
 *    2021-04-26  thebearmay     break out polls as separate preference options
 *    2021-04-27  thebearmay     replace the homegrown JSON parser, with groovy's JsonSluper
 *    2021-04-29  thebearmay     merge pull request from nh.schottfam, clean up/add type declarations, optimize code and add local variables
 *    2021-05-03  thebearmay     add nonPolling zigbee channel attribute, i.e. set at hub startup
 *    2021-05-04  thebearmay     release 2.2.7.x changes (v2.2.0 - v2.2.2)
 *    2021-05-06  thebearmay     code cleanup from 2.2.2, now 2.2.3
 *    2021-05-09  thebearmay     return NA when zigbee channel not valid
 *    2021-05-25  thebearmay     use upTime to recalculate system start when initialize called manually
 *    2021-05-25  thebearmay     upTime display lagging by 1 poll
 *    2021-06-11  thebearmay     add units to the jvm and memory attributes
 *    2021-06-12  thebearmay     put a space between unit and values
 *    2021-06-14  thebearmay     add Max State/Event days, required trimming of the html attribute
 *    2021-06-15  thebearmay     add ZWave Version attribute
 *                               2.4.1 temporary version to stop overflow on reboot
 *    2021-06-16  thebearmay     2.4.2 overflow trap/retry
 *                               2.4.3 firmware0Version and subVersion is the radio firmware. target 1 version and subVersion is the SDK
 */
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "2.4.2"}

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
        attribute "zigbeeChannel","string"
        attribute "maxEvtDays", "number"
        attribute "maxStateDays", "number"
        attribute "zwaveVersion", "string"
        attribute "zwaveSDKVersion", "string"        
        attribute "zwaveData", "string"
        
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("tempPollEnable", "bool", title: "Enable Temperature Polling")
    input("freeMemPollEnabled", "bool", title: "Enable Free Memory Polling")
    input("cpuPollEnabled", "bool", title: "Enable CPU & JVM Polling")
    input("dbPollEnabled","bool", title: "Enable DB Size Polling")
    input("publicIPEnable", "bool", title: "Enable Querying the cloud \nto obtain your Public IP Address?", default: false, required: false, submitOnChange: true)
    input("evtStateDaysEnable", "bool", title:"Enable Display of Max Event/State Days Setting")
    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable || evtStateDaysEnable)
        input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", default:300, submitOnChange: true)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", default: false, required: false, submitOnChange: true)
    input("checkZwVersion","bool",title:"Update ZWave Version Attribute", default: true, submitOnChange: true)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){
    log.trace "Hub Information Driver ${version()} initialized"
// psuedo restart time - gets reset by calculating from uptime vs current time below
    Long restartVal = now()
    updateAttr("lastHubRestart", restartVal)
    def sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(restartVal))
    if (!security)  device.updateSetting("security",[value:"false",type:"bool"])
    device.updateSetting("checkZwVersion",[value:"true",type:"bool"])
    runIn(30,configure)
    restartCheck() //reset Restart Time using uptime and current timeatamp
}

@SuppressWarnings('unused')
def updated(){
    log.trace "updated()"
    if(debugEnable) runIn(1800,logsOff)
    if(tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable || checkZwVersion){
        unschedule()
        getPollValues()
    }

    if (attribEnable)
        formatAttrib()
    else
        sendEvent(name: "html", value: "<table></table>", isChanged: true)
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnable) log.debug "configure()"
    List locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    def myHub = location.hub
    List hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        updateAttr(hubProp[i], myHub["${hubProp[i]}"])
    }
    for(i=0;i<locProp.size();i++){
        updateAttr(locProp[i], location["${locProp[i]}"])
    }
    myHubData = parseHubData()
    updateAttr("zigbeeChannel",myHubData.zigbeeChannel)
    
    formatUptime()
    updateAttr("hubVersion", location.hub.firmwareVersionString) //retained for backwards compatibility
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)
    updateAttr("lastUpdated", now())
    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable || checkZwVersion) 
        getPollValues()
    if (attribEnable) formatAttrib()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}
        
HashMap parseHubData() {    
    String dataWork = location.hub.data.toString()
    dataWork = dataWork.substring(1,dataWork.size()-1)
    List <String> dataMapPre = dataWork.split(",")
    HashMap dataMap = [:]    

    dataMapPre.each() {
        List dSplit= it.split(":")
        dataMap.put(dSplit[0].trim(),dSplit[1].trim())
    }
    if (dataMap.zigbeeChannel.trim() == "0x00 (0)") dataMap.zigbeeChannel = "NA"
    // Invalid zigbee response - possibly timeout issue
    return dataMap
}


void formatUptime(){
    try {
        Long ut = location.hub.uptime.toLong()
        Integer days = Math.floor(ut/(3600*24)).toInteger()
        Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
        Integer min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
        Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()
    
        String attrval = days.toString() + "d," + hrs.toString() + "h," + min.toString() + "m," + sec.toString() + "s"
        updateAttr("formattedUptime", attrval) 
    } catch(ignore) {
        updateAttr("formattedUptime", "")
    }
}

void formatAttrib(){
    if(debugEnable) log.debug "formatAttrib"
    String attrStr = "<style>td{text-align:left;}</style><table id='hubInfoTable'>"
    
    attrStr += addToAttr("Name","name")
    attrStr += addToAttr("Version","hubVersion")
    if(publicIPEnable) {
        List combine = ["localIP", "publicIP"]
        attrStr += combineAttr("IP Local/Public", combine)
    } else
        attrStr += addToAttr("IP Addr","localIP")
    if(cpuPollEnabled) {
        attrStr += addToAttr("Free Mem","freeMemory","int")
        if(device.currentValue("cpu5Min")){
            List combine = ["cpu5Min", "cpuPct"]
            attrStr += combineAttr("CPU Load/Load%", combine)
        }

        List combineA = ["jvmTotal", "jvmFree", "jvmFreePct"]
        attrStr += combineAttr("JVM Tot/Free/%", combineA)
    }

    if(device.currentValue("dbSize")) attrStr +=addToAttr("DB Size","dbSize")
    if(evtStateDaysEnable){
        List combine = ["maxEvtDays", "maxStateDays"]
        attrStr += combineAttr("Max Evt/State Days", combine)
    }

    attrStr += addToAttr("Last Restart","lastHubRestartFormatted")
    attrStr += addToAttr("Uptime","formattedUptime")

    if(tempPollEnable) {
        String tempAttrib = location.temperatureScale=="C" ? "temperatureC" : "temperatureF"
        attrStr += addToAttr("Temperature",tempAttrib)
    }
    
    attrStr += addToAttr("ZB Channel","zigbeeChannel")
    
    if (device.currentValue("zwaveVersion")){
        List combine = ["zwaveVersion","zwaveVersion"]
        attrStr += combineAttr("ZW Radio/SDK", combine)   
    }
    
    attrStr += "</table>"

    if (debugEnable) log.debug "after calls attr string = $attrStr"
    updateAttr("html", attrStr)
    //updateAttr("htmlLength",attrStr.length())
    if (attrStr.length() > 1024) updateAttr("html", "Max Attribute Size Exceeded: ${attrStr.length()}")
}

String combineAttr(String name, List<String> keys){
    if(enableDebug) log.debug "adding $name, $keys.length"

    String retResult = '<tr><td align="left">'
    retResult += name + '</td><td align="left">'
    
    String keyResult = ""
    for (i = 0;i < keys.size(); i++) {
        keyResult+= device.currentValue(keys[i])
        String attrUnit = getUnitFromState(keys[i])
        if (attrUnit != "null") keyResult+=" "+attrUnit
        if (i < keys.size() - 1) keyResult+= " / "
    }
            
    retResult += keyResult+'</td></tr>'
    return retResult
}

String addToAttr(String name, String key, String convert = "none") {
    if(enableDebug) log.debug "adding $name, $key"
    String retResult = '<tr><td>'
    retResult += name + '</td><td>'

    String attrUnit = getUnitFromState(key)
    if (attrUnit == "null") attrUnit =""

    def curVal = device.currentValue(key)
    if(curVal){
        if (convert == "int"){
            retResult += curVal.toInteger().toString()+" "+attrUnit
/*        } else if (name=="Temperature"){
            // span uses integer value to allow CSS override 
            retResult += "<span class=\"temp-${device.currentValue('temperature').toInteger()}\">" + curVal.toString() + attrUnit + "</span>"
*/
        } else retResult += curVal.toString() + " "+attrUnit
    }
    retResult += '</td></tr>'
    return retResult
}

void getPollValues(){
    // start - Modified from dman2306 Rebooter app
    String cookie
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
        ) { resp -> cookie = ((List)((String)resp?.headers?.'Set-Cookie')?.split(';'))?.getAt(0) }
    }
    // End - Modified from dman2306 Rebooter app
    // Zwave Version
    if(checkZwVersion == null) device.updateSetting("checkZwVersion",[value:"true",type:"bool"])
    if(checkZwVersion){
        Map paramZ = [
            uri    : "http://${location.hub.localIP}:8080",
            path   : "/hub/zwaveVersion",
            headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug paramZ
        asynchttpGet("getZwave", paramZ)
    }
 
    // get Temperature
    if(tempPollEnable) {
        Map params = [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub/advanced/internalTempCelsius",
                headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug params
        asynchttpGet("getTempHandler", params)
    }

    // get Free Memory
    if(freeMemPollEnabled) {
        Map params = [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub/advanced/freeOSMemory",
                headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug params
        asynchttpGet("getFreeMemHandler", params)
    }
    
    // get Free JVM & CPU
    if(cpuPollEnabled) {
        Map params
        if (location.hub.firmwareVersionString <= "2.2.5.131") {
            params = [
                    uri    : "http://${location.hub.localIP}:8080",
                    path   : "/hub/advanced/freeOSMemoryHistory",
                    headers: ["Cookie": cookie]
            ]
        } else {
            params = [
                    uri    : "http://${location.hub.localIP}:8080",
                    path   : "/hub/advanced/freeOSMemoryLast",
                    headers: ["Cookie": cookie]
            ]
        }
        if (debugEnable) log.debug params
        asynchttpGet("getJvmHandler", params)
    }

    //Get DB size
    if(dbPollEnabled) {
        Map params = [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub/advanced/databaseSize",
                headers: ["Cookie": cookie]
        ]

        if (debugEnable) log.debug params
        asynchttpGet("getDbHandler", params)
    }

    //get Public IP 
    if(publicIPEnable) {
        Map params =
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
 
    //Max State Days
    if(evtStateDaysEnable) {
        Map params =
        [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub/advanced/maxDeviceStateAgeDays",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getStateDaysHandler", params)
     
     //Max Event Days
        params =
        [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub/advanced/maxEventAgeDays",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getEvtDaysHandler", params)
     
     
    } 
    
    //End Pollable Gets
    
    def myHubData = parseHubData()
    updateAttr("zigbeeChannel",myHubData.zigbeeChannel)    
    updateAttr("uptime", location.hub.uptime)
    formatUptime()
    
    if (debugEnable) log.debug "tempPollRate: $tempPollRate"

    if (tempPollEnable || freeMemPollEnabled || cpuPollEnabled || dbPollEnabled || publicIPEnable || checkZwVersion) {
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,getPollValues)
        }else {
            runIn(tempPollRate,getPollValues)
        }
    }
}

@SuppressWarnings('unused')
def getTemp(){  // this is to handle the upgrade path from >= 1.8.x
    log.info "Upgrading HubInfo polling from 1.8.x"
    unschedule(getTemp)
    getPollValues()
}

@SuppressWarnings('unused')
void getTempHandler(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Double tempWork = new Double(resp.data.toString())
            if(debugEnable) log.debug tempWork
            if (location.temperatureScale == "F")
                updateAttr("temperature",celsiusToFahrenheit(tempWork),"°F")
            else
                updateAttr("temperature",tempWork,"°C")

            updateAttr("temperatureF",celsiusToFahrenheit(tempWork)+ " °F")
            updateAttr("temperatureC",tempWork+ " °C")
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getTemp httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void getZwave(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            zwaveData = resp.data.toString()
            if(debugEnable) log.debug resp.data.toString()
            if(zwaveData.length() < 1024){
                updateAttr("zwaveData",zwaveData)
                device.updateSetting("checkZwVersion",[value:"false",type:"bool"])
            }
            else log.warn "Invalid data returned for Zwave, length = ${zwaveData.length()} will retry"

        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getZwave httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
    
    if(zwaveData && zwaveData.length() < 1024) parseZwave(zwaveData)
    
}

@SuppressWarnings('unused')
void getFreeMemHandler(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer memWork = new Integer(resp.data.toString())
            if(debugEnable) log.debug memWork
            updateAttr("freeMemory",memWork, "KB")
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getFreeMem httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
}

@SuppressWarnings('unused')
void getJvmHandler(resp, data) {
    String jvmWork
    List<String> jvmArr = []
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            jvmWork = resp.data.toString()
        }
        if (attribEnable) runIn(5,formatAttrib) //allow for events to register before updating - thebearmay 210308
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getJvm httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
    if (jvmWork) {
        Integer lineCount = 0
        jvmWork.eachLine{
            lineCount++
        }
        Integer lineCount2 = 0
        jvmWork.eachLine{
            lineCount2++
            if(lineCount==lineCount2)
                jvmArr = it.split(",")
        }
        if(jvmArr.size() > 1){
            Integer jvmTotal = jvmArr[2].toInteger()
            updateAttr("jvmTotal",jvmTotal, "KB")
            Integer jvmFree = jvmArr[3].toInteger()
            updateAttr("jvmFree",jvmFree, "KB")
            Double jvmFreePct = (jvmFree/jvmTotal)*100
            updateAttr("jvmFreePct",jvmFreePct.round(1),"%")
            if(jvmArr.size() > 4) {
                Double cpuWork=jvmArr[4].toDouble()
                updateAttr("cpu5Min",cpuWork.round(2))
                cpuWork = (cpuWork/4.0D)*100.0D //Load / #Cores - if cores change will need adjusted to reflect
                updateAttr("cpuPct",cpuWork.round(2),"%")
            }
        }
    }
}

@SuppressWarnings('unused')
void getDbHandler(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer dbWork = new Integer(resp.data.toString())
            if(debugEnable) log.debug dbWork
            updateAttr("dbSize",dbWork,"MB")
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getDb httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void getIfHandler(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            def jSlurp = new JsonSlurper()
            ipData = jSlurp.parseText (resp.data)
            updateAttr("publicIP",ipData.ip)
        } else {
            log.warn "Status ${resp.getStatus()} while fetching Public IP"
        } 
    } catch (Exception ex){
        log.info ex
    }
}   

@SuppressWarnings('unused')
void getStateDaysHandler(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer stateDays = new Integer(resp.data.toString())
            if(debugEnable) log.debug "Max State Days $stateDays"

            updateAttr("maxStateDays",stateDays)
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getStateDays httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void getEvtDaysHandler(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer evtDays = new Integer(resp.data.toString())
            if(debugEnable) log.debug "Max Event Days $evtDays"

            updateAttr("maxEvtDays",evtDays)
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        log.warn "getEvtDays httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void parseZwave(zString){
/*
    Integer start = zString.indexOf('(')+1
    Integer end = zString.length()-1  
    
    if(start == -1 || end == -1) return //empty or invalid string - possibly non-C7
    
    wrkStr = zString.substring(start,end)

    List<String> wrkStrPre = wrkStr.split(",")
    HashMap zMap = [:]    
    wrkStrPre.each() {
        List dSplit= it.split(":")
        if(dSplit.size()>1)
            zMap.put(dSplit[0].trim(),dSplit[1].trim())
        else
           zMap.put(dSplit[0].trim(),null)
    }
    updateAttr("zwaveSDKVersion","${zMap?.zWaveProtocolVersion}.${zMap?.zWaveProtocolSubVersion}")
    updateAttr("zwaveVersion","${zMap?.firmware0Version}.${zMap?.firmware0SubVersion}")
*/
    Integer start = zString.indexOf('(')
    Integer end = zString.length()  
    
    if(start == -1 || end < 1) return //empty or invalid string - possibly non-C7
  
    wrkStr = zString.substring(start,end)
    wrkStr = wrkStr.replace("(","[")
    wrkStr = wrkStr.replace(")","]")
    
    HashMap zMap = evaluate(wrkStr)

    updateAttr("zwaveSDKVersion","${zMap?.zWaveProtocolVersion}.${zMap?.zWaveProtocolSubVersion}")
    updateAttr("zwaveVersion","${zMap?.firmware0Version}.${zMap?.firmware0SubVersion}")
}

String getUnitFromState(String attrName){
    String wrkStr = device.currentState(attrName).toString()
    if(location.hub.firmwareVersionString <= "2.2.7.0") {
        Integer start = wrkStr.indexOf('(')+1    
        Integer end = wrkStr.length() - 1
        wrkStr = wrkStr.substring(start,end)

        if (debugEnabled) log.debug "$wrkStr"
        List<String> stateParts = wrkStr.split(',')
        return stateParts[3]?.trim()
    } else {
        String unt = altGetUnitProc(wrkStr) 
        return unt
    }

}

String altGetUnitProc(String wrkStr) {
    Integer start = wrkStr.indexOf('[')+1
    Integer end = wrkStr.length()-1    
    wrkStr = wrkStr.substring(start,end)
    wrkStr = wrkStr.replace("=",":")

    List<String> wrkStrPre = wrkStr.split(",")
    HashMap statePartsMap = [:]    
    wrkStrPre.each() {
        List dSplit= it.split(":")
        if(dSplit.size()>1)
            statePartsMap.put(dSplit[0].trim(),dSplit[1].trim())
        else
           statePartsMap.put(dSplit[0].trim(),null)
    }
    return statePartsMap.unit    
}

void restartCheck() {
    Long rsDate = Long.parseLong(device.currentValue('lastHubRestart'))
    if(debugEnable) log.debug "$rsDate"
    Long ut = now() - (location.hub.uptime.toLong()*1000)
    Date upDate = new Date(ut)
    if(debugEnable) log.debug "RS: $rsDate  UT:$ut  upTime Date: $upDate   upTime: ${location.hub.uptime}"
    if(rsDate > ut){
        updateAttr("lastHubRestart", ut)
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        updateAttr("lastHubRestartFormatted",sdf.format(upDate))
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
