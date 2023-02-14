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
 *    ..........
 *    2023-01-10  thebearmay     version 3 rewrite ** minFwVersion = "2.2.8.141" **
 *    2023-01-11                 v3.0.1 - Poll 4 error
 *    2023-01-12                 v3.0.2 - Zigbee status/status2 disagreement handler (happens when radio is shut off without a reboot)
 *                                        Turn off Debug Logs after 30 minutes
 *                                        Add removeUnused method, command and preference
 *                               v3.0.3 - Add Uptime Descriptor 
 *    2023-01-13                 v3.0.4 - Select format for restart formatted attribute
 *                               v3.0.5 - Missing zigbeeStatus generating warning message
 *    2023-01-14                 v3.0.6 - Delay baseData() on Initialize to cpature state correctly
 *                               v3.0.7 - hubversion to v2Cleanup, 
 *                    					  FreeMemoryUnit option
 *					                      Add Update Check to Initialize if polled
 *                               v3.0.8 - Fix 500 Error on device create
 *    2023-01-16                 v3.0.9 - Delay initial freeMemory check for 8 seconds
 *    2023-01-21                 v3.0.10 - lastUpdated conflict, renamed lastPollTime
 *    2023-01-23                 v3.0.11 - Change formatting date formatting description to match lastPollTime
 *    2023-02-01                 v3.0.12 - Add a try around the time zone code
 *    2023-02-02                 v3.0.13 - Add a null character check to time zone formatting
 *                               v3.0.14 - US/Arizona timezone fix
 *    2023-02-13                 v3.0.15 - check for null SSID when hasWiFi true
*/
import java.text.SimpleDateFormat
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "3.0.15"}

metadata {
    definition (
        name: "Hub Information Driver v3", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoV3.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"
        
        attribute "latitude", "string"
        attribute "longitude", "string"
        //attribute "hubVersion", "string"
        attribute "id", "string"
        attribute "name", "string"
        //attribute "data", "string"
        attribute "zigbeeId", "string"
        attribute "zigbeeEui", "string"
        attribute "hardwareID", "string"
        attribute "type", "string"
        attribute "localIP", "string"
        attribute "localSrvPortTCP", "string"
        attribute "uptime", "number"
        attribute "lastPollTime", "string"
        attribute "lastPoll", "string"
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
        
        attribute "cpu5Min", "number"
        attribute "cpuPct", "number"
        attribute "dbSize", "number"
        attribute "publicIP", "string"
        attribute "zigbeeChannel","string"
        attribute "maxEvtDays", "number"
        attribute "maxStateDays", "number"
        attribute "zwaveVersion", "string"
        attribute "zwaveSDKVersion", "string"        
        //attribute "zwaveData", "string"
        attribute "hubModel", "string"
        attribute "hubUpdateStatus", "string"
        attribute "hubUpdateVersion", "string"
        attribute "currentMode", "string"
        attribute "currentHsmMode", "string"
        attribute "ntpServer", "string"
        attribute "ipSubnetsAllowed", "string"
        attribute "zigbeeStatus", "string"
        attribute "zigbeeStatus2", "string"
        attribute "zigbeeStack", "string"
        attribute "zwaveStatus", "string"
        attribute "hubAlerts", "string"
        attribute "hubMeshData", "string"
        attribute "hubMeshCount", "number"
        attribute "securityInUse", "string"
		attribute "sunrise", "string"
		attribute "sunset", "string"
        attribute "nextPoll", "string"
        //HE v2.3.4.126
        attribute "connectType", "string" //Ethernet, WiFi, Dual
        attribute "dnsServers", "string"
        attribute "staticIPJson", "string"
        attribute "lanIPAddr", "string"
        attribute "wirelessIP", "string"
        attribute "wifiNetwork", "string"
        

        command "hiaUpdate", ["string"]
        command "reboot"
        command "shutdown"
        command "updateCheck"
        command "removeUnused"
    }   
}
preferences {
    if(state?.errorMinVersion || state?.errorMinVersion == "true") 
        input("errMsg", "string", title:"<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
    input("quickref","string", title:"$ttStyleStr<a href='https://htmlpreview.github.io/?https://github.com/thebearmay/hubitat/blob/main/hubInfoQuickRef3.html' target='_blank'>Quick Reference v${version()}</a>")
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("warnSuppress", "bool", title: "Suppress Warn Level Logging", width:4)

	prefList.each { l1 ->
        l1.each{
		    pMap = (HashMap) it.value
            input ("${it.key}", "enum", title: "<div class='tTip'>${pMap.desc}<span class='tTipText'>${pMap.attributeList}</span></div>", options:pollList, submitOnChange:true, width:4, defaultValue:"0")
        }
	}
    input("remUnused", "bool", title: "Remove unused attributes", defaultValue: false, submitOnChange: true, width:4)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("alternateHtml", "string", title: "Template file for HTML attribute", submitOnChange: true, defaultValue: "hubInfoTemplate.res", width:4)
    input("attrLogging", "bool", title: "Log all attribute changes", defaultValue: false, submitOnChange: true, width:4)
    input("allowReboot","bool", title: "Allow Hub to be shutdown or rebooted", defaultValue: false, submitOnChange: true, width:4)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
    input("freeMemUnit", "enum", title: "Free Memory Unit", options:["KB","MB"], defaultValue:"KB", width:4)
    input("sunSdfPref", "enum", title: "Date/Time Format for Sunrise/Sunset", options:sdfList, defaultValue:"HH:mm:ss", width:4)
    input("updSdfPref", "enum", title: "Date/Time Format for Last Poll Time", options:sdfList, defaultValue:"Milliseconds", width:4)
    input("rsrtSdfPref", "enum", title: "Date/Time Format for Hub Restart Formatted", options:sdfList, defaultValue:"yyyy-MM-dd HH:mm:ss", width:4)  
    input("upTimeSep", "string", title: "Separator for Formatted Uptime", defaultValue: ", ", width:4)
    input("upTimeDesc", "enum", title: "Uptime Descriptors", defaultValue:"d/h/m/s", options:["d/h/m/s"," days/ hrs/ min/ sec"," days/ hours/ minutes/ seconds"])
	input("pollRate1", "number", title: "Poll Rate for Queue 1 in minutes", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate2", "number", title: "Poll Rate for Queue 2 in minutes", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate3", "number", title: "Poll Rate for Queue 3 in minutes", defaultValue:0, submitOnChange: true, width:4) 
    input("pollRate4", "number", title: "Poll Rate for Queue 4 in <b style='background-color:red'>&nbsp;hours&nbsp;</b>", defaultValue:0, submitOnChange: true, width:4) 
}
@SuppressWarnings('unused')
void installed() {
    log.trace "installed()"
    xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
    initialize()
    configure()
}

void initialize() {
    log.info "Hub Information v${version()} initialized"
    restartCheck()
    updated()
    runIn(8,"initMemory")
    runIn(5,"baseData")
    if (settings["parm12"] != 0)
        runIn(30,"updateCheck")
    if(!state?.v2Cleaned)
        v2Cleanup()
}

void initMemory(){
    if(security) cookie = getCookie()
    freeMemoryReq(cookie)    
}

void configure() {
    updated()
    baseData()
    if(!state?.v2Cleaned)
        v2Cleanup()    
}

void updated(){
    if(debugEnable) log.debug "updated"
	unschedule()
	state.poll1 = []
	state.poll2 = []
	state.poll3 = []
    state.poll4 = []
	prefList.each{ l1 ->
        l1.each{
            if(settings["${it.key}"] != null && settings["${it.key}"] != "0") {
                pMap = (HashMap) it.value
                if(debugEnable) log.debug "poll${settings["${it.key}"]} ${pMap.method}" 
                state["poll${settings["${it.key}"]}"].add("${pMap.method}")
            }
        }
    }  
    //Enforce the integer value 
    try{
        if(pollRate1.toString().contains(".")){
            pollRate1 = pollRate1.toString().substring(0,pollRate1.toString().indexOf(".")).toInteger()
            device.updateSetting("pollRate1",[value:pollRate1,type:"number"])
        }       
        if(pollRate2.toString().contains(".")){
            pollRate2 = pollRate2.toString().substring(0,pollRate2.toString().indexOf(".")).toInteger()
            device.updateSetting("pollRate2",[value:pollRate2,type:"number"])
        }
        if(pollRate3.toString().contains(".")){
            pollRate3 = pollRate3.toString().substring(0,pollRate3.toString().indexOf(".")).toInteger()
            device.updateSetting("pollRate3",[value:pollRate3,type:"number"])
        }
        if(pollRate4.toString().contains(".")){
            pollRate4 = pollRate4.toString().substring(0,pollRate4.toString().indexOf(".")).toInteger()
            device.updateSetting("pollRate4",[value:pollRate4,type:"number"])
        }        
    } catch (ex) {
        log.error ex
    }
    
	if(pollRate1 > 0)
		runIn(pollRate1*60, "poll1")
	if(pollRate2 > 0)
		runIn(pollRate2*60, "poll2")
	if(pollRate3 > 0)
		runIn(pollRate3*60, "poll3")		
    if(pollRate4 > 0)
		runIn(pollRate4*60*60, "poll4")	
    if(debugEnable)
        runIn(1800,"logsOff")

    if(remUnused) removeUnused()
}

void refresh(){
    baseData()
    poll1()
    poll2()
    poll3()
    poll4()
}

void v2Cleanup() {
    device.deleteCurrentState("data")
    device.deleteCurrentState("zwaveData")
    device.deleteCurrentState("nextPoll")
    device.deleteCurrentState("hubVersion")
    state.v2Cleaned = true
}


void poll1(){
    if(security) cookie = getCookie()
	state.poll1.each{
		this."$it"(cookie)
	}
	if(pollRate1 > 0)
		runIn(pollRate1*60, "poll1")
    everyPoll("poll1")
}

void poll2(){
    if(security) cookie = getCookie()
	state.poll2.each{
		this."$it"(cookie)
	}
	if(pollRate2 > 0)
		runIn(pollRate2*60, "poll2")
    everyPoll("poll2")
}

void poll3(){
    if(security) cookie = getCookie()
	state.poll3.each{
		this."$it"(cookie)
	}
	if(pollRate3 > 0)
		runIn(pollRate3*60, "poll3")
    everyPoll("poll3")
}

void poll4(){
    if(security) cookie = getCookie()
	state.poll4.each{
		this."$it"(cookie)
	}
	if(pollRate4 > 0)
		runIn(pollRate4*60*60, "poll4")
    everyPoll("poll4")
}

void baseData(dummy=null){
    String model = getHubVersion() // requires >=2.2.8.141
    updateAttr("hubModel", model)
    
    List locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    locProp.each{
        if(it != "timeZone")
            updateAttr(it, location["${it}"])
        else {
            try {
                tzWork=location["timeZone"].toString()
                if(tzWork.indexOf("TimeZone") > -1)
                    tzWork=tzWork.substring(tzWork.indexOf("TimeZone")+8)
                else // US/Arizona uses a shorter format
                    tzWork=tzWork.substring(tzWork.indexOf("ZoneInfo")+8).replace("\"","")
                if(debugEnable)
                    log.debug "1) $tzWork"
                tzWork=tzWork.replace("=",":\"")
                if(debugEnable)
                    log.debug "2) $tzWork"
                tzWork=tzWork.replace(",","\",")
                if(debugEnable)
                    log.debug "3) $tzWork"
                tzWork=tzWork.replace("]]","\"]")
                if(debugEnable)
                    log.debug "4) $tzWork"
                tzWork=tzWork.replace("null]","\"]")
                if(debugEnable)
                    log.debug "5) $tzWork"
                tzMap= (Map) evaluate(tzWork)
                updateAttr("timeZone",JsonOutput.toJson(tzMap))
            } catch (e) {
                log.error "Time zone format error: ${location["timeZone"]}<br>$e" 
            }
        }
    }
    
    def myHub = location.hub
    List hubProp = ["id","name","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    hubProp.each {
        updateAttr(it, myHub["${it}"])
    }
    
    if(location.hub.firmwareVersionString < minFwVersion) {
        state.errorMinVersion = true
    } else
        state.errorMinVersion = false
    

    if(location.hub.properties.data.zigbeeChannel != null)
        updateAttr("zigbeeChannel",location.hub.properties.data.zigbeeChannel)
    else
        updateAttr("zigbeeChannel","Not Available")
                   
    if(location.hub.properties.data.zigbeeChannel != null){
        updateAttr("zigbeeStatus", "enabled")
    }else
        updateAttr("zigbeeStatus", "disabled")
    
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)

    everyPoll("baseData")
}

void everyPoll(whichPoll=null){
    updateAttr("currentMode", location.properties.currentMode)
    updateAttr("currentHsmMode", location.hsmStatus)
    
    SimpleDateFormat sdfIn = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    sunrise = sdfIn.parse(location.sunrise.toString())
    sunset = sdfIn.parse(location.sunset.toString())
    
	if(sunSdfPref == null) device.updateSetting("sunSdfPref",[value:"HH:mm:ss",type:"enum"])
    if(sunSdfPref != "Milliseconds") {
        SimpleDateFormat sdf = new SimpleDateFormat(sunSdfPref)
        updateAttr("sunrise", sdf.format(sunrise))
	    updateAttr("sunset", sdf.format(sunset))
    } else {
        updateAttr("sunrise", sunrise.getTime())
	    updateAttr("sunset", sunset.getTime())  
    }
    updateAttr("localIP",location.hub.localIP)

    if(updSdfPref == null) device.updateSetting("updSdfPref",[value:"Milliseconds",type:"string"])
    if(updSdfPref == "Milliseconds" || updSdfPref == null) 
        updateAttr("lastPollTime", new Date().getTime())
    else {
        SimpleDateFormat sdf = new SimpleDateFormat(updSdfPref)
        updateAttr("lastPollTime", sdf.format(new Date().getTime()))
    }
    if(whichPoll != null)
        updateAttr("lastPoll", whichPoll)
    
    formatUptime()
      
    if (attribEnable) createHtml()
    
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
/*    if(aValue.length() > 1024) {
        log.error "Attribute value for $aKey exceeds 1024, current size = ${aValue.length()}, truncating to 1024..."
        aValue = aValue.substring(0,1023)
    }*/
    sendEvent(name:aKey, value:aValue, unit:aUnit)
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void cpuTemperatureReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/internalTempCelsius",
        headers: ["Cookie": cookie]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getCpuTemperature", params)    
}

void getCpuTemperature(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Double tempWork = new Double(resp.data.toString())
            if(tempWork > 0) {
                if(debugEnable) log.debug tempWork
                if (location.temperatureScale == "F")
                    updateAttr("temperature",String.format("%.1f", celsiusToFahrenheit(tempWork)),"°F")
                else
                    updateAttr("temperature",String.format("%.1f",tempWork),"°C")

                updateAttr("temperatureF",String.format("%.1f",celsiusToFahrenheit(tempWork))+ " °F")
                updateAttr("temperatureC",String.format("%.1f",tempWork)+ " °C")
            }
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getTemp httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}


void freeMemoryReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemory",
        headers: ["Cookie": cookie]
    ]
    if (debugEnable) log.debug params
        asynchttpGet("getFreeMemory", params)    
}

@SuppressWarnings('unused')
void getFreeMemory(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer memWork = new Integer(resp.data.toString())
            if(debugEnable) log.debug memWork
            if(freeMemUnit == "MB")
                updateAttr("freeMemory",(new Float(memWork/1024).round(2)), "MB")
            else
                updateAttr("freeMemory",memWork, "KB")
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getFreeMem httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
}

void cpuLoadReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemoryLast",
        headers: ["Cookie": cookie]
    ]
    if (debugEnable) log.debug params
    asynchttpGet("getCpuLoad", params)    
}

void getCpuLoad(resp, data) {
    String loadWork
    List<String> loadRec = []
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            loadWork = resp.data.toString()
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getCpuLoad httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
    if (loadWork) {
        Integer lineCount = 0
        loadWork.eachLine{
            lineCount++
        }
        Integer lineCount2 = 0
        loadWork.eachLine{
            lineCount2++
            if(lineCount==lineCount2)
                workSplit = it.split(",")
        }
        if(workSplit.size() > 1){
            Double cpuWork=workSplit[2].toDouble()
            updateAttr("cpu5Min",cpuWork.round(2))
            cpuWork = (cpuWork/4.0D)*100.0D //Load / #Cores - if cores change will need adjusted to reflect
            updateAttr("cpuPct",cpuWork.round(2),"%")            
        }
    }
}
void dbSizeReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/databaseSize",
        headers: ["Cookie": cookie]
    ]

    if (debugEnable) log.debug params
    asynchttpGet("getDbSize", params)    
}

@SuppressWarnings('unused')
void getDbSize(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer dbWork = new Integer(resp.data.toString())
            if(debugEnable) log.debug dbWork
            updateAttr("dbSize",dbWork,"MB")
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getDb httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

void publicIpReq(cookie){
    params = [
        uri: "https://api.ipify.org?format=json",
        headers: [            
            Accept: "application/json"
        ]
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getPublicIp", params)
}

@SuppressWarnings('unused')
void getPublicIp(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.debug resp.data
            def jSlurp = new JsonSlurper()
            Map ipData = (Map)jSlurp.parseText((String)resp.data)
            updateAttr("publicIP",ipData.ip)
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching Public IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

void evtStateDaysReq(cookie){
    //Max State Days
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/maxDeviceStateAgeDays",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getStateDays", params)
     
     //Max Event Days
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/maxEventAgeDays",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getEvtDays", params)
}

@SuppressWarnings('unused')
void getEvtDays(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer evtDays = new Integer(resp.data.toString())
            if(debugEnable) log.debug "Max Event Days $evtDays"

            updateAttr("maxEvtDays",evtDays)
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getEvtDays httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void getStateDays(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer stateDays = new Integer(resp.data.toString())
            if(debugEnable) log.debug "Max State Days $stateDays"

            updateAttr("maxStateDays",stateDays)
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getStateDays httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

void zwaveVersionReq(cookie){
    if(!isCompatible(7)) {
        if(!warnSuppress) log.warn "ZWave Version information not available for this hub"
        return
    }
    param = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/zwaveVersion",
        headers: ["Cookie": cookie]
    ]
    if (debugEnable) log.debug param
    asynchttpGet("getZwaveVersion", param)
}

@SuppressWarnings('unused')
void getZwaveVersion(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            String zwaveData = resp.data.toString()
            if(debugEnable) log.debug resp.data.toString()
            if(zwaveData.length() < 1024){
                //updateAttr("zwaveData",zwaveData)
                parseZwave(zwaveData)
            }
            else if (!warnSuppress) log.warn "Invalid data returned for Zwave, length = ${zwaveData.length()} will retry"
        }
    } catch(ignored) {
        if (!warnSuppress) log.warn "getZwave Parsing Error"    
    } 
}

@SuppressWarnings('unused')
void parseZwave(String zString){
    Integer start = zString.indexOf('(')
    Integer end = zString.length()
    String wrkStr
    
    if(start == -1 || end < 1 || zString.indexOf("starting up") > 0 ){ //empty or invalid string - possibly non-C7
        //updateAttr("zwaveData",null)
        if(!warnSuppress) log.warn "Invalid ZWave Data returned"
    }else {
        wrkStr = zString.substring(start,end)
        wrkStr = wrkStr.replace("(","[")
        wrkStr = wrkStr.replace(")","]")

        HashMap zMap = (HashMap)evaluate(wrkStr)
        
        updateAttr("zwaveSDKVersion","${((List)zMap.targetVersions)[0].version}.${((List)zMap.targetVersions)[0].subVersion}")
        updateAttr("zwaveVersion","${zMap?.firmware0Version}.${zMap?.firmware0SubVersion}.${zMap?.hardwareVersion}")
    }
}

void ntpServerReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/ntpServer",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getNtpServer", params)    
}

@SuppressWarnings('unused')
void getNtpServer(resp, data) {
    try {
        if (resp.status == 200) {
            ntpServer = resp.data.toString()
            if(ntpServer == "No value set") ntpServer = "Hub Default(Google)"
            updateAttr("ntpServer", ntpServer)
        } else {
            if(!warnSuppress) log.warn "NTP server check returned status: ${resp.status}"
        }
    }catch (ignore) {
    }
}

void ipSubnetsReq(cookie){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/allowSubnets",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getSubnets", params)
}

@SuppressWarnings('unused')
void getSubnets(resp, data) {
    try {
        if (resp.status == 200) {
            subNets = resp.data.toString()
            if(subNets == "Not set") subNets = "Hub Default"
            updateAttr("ipSubnetsAllowed", subNets)
        } else {
            if(!warnSuppress) log.warn "Subnet check returned status: ${resp.status}"
        }
    }catch (ignore) {
    }
}

void hubMeshReq(cookie){
    params =  [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubMeshJson",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getHubMesh", params)
}

@SuppressWarnings('unused')
void getHubMesh(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
            i=0
            subMap2=[:]
            jStr="["
            h2Data.hubList.each{
                if(i>0) jStr+=","
                jStr+="{\"hubName\":\"$it.name\","
                jStr+="\"active\":\"$it.active\","
                jStr+="\"offline\":\"$it.offline\","
                jStr+="\"ipAddress\":\"$it.ipAddress\","
                jStr+="\"meshProtocol\":\"$h2Data.hubMeshProtocol\"}"          
                i++
            }
            jStr+="]"
            updateAttr("hubMeshData", jStr)
            updateAttr("hubMeshCount",i)

        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on H2 request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

void extNetworkReq(cookie){
    if(location.hub.firmwareVersionString < "2.3.4.126"){
        if(!warnSuppress) log.warn "Extend Network Data not available for HE v${location.hub.firmwareVersionString}"
        return
    }
        
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/networkConfiguration",
        headers: ["Cookie": cookie]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getExtNetwork", params)
}

void hub2DataReq(cookie) {
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubData",
        headers: ["Cookie": cookie]                   
    ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getHub2Data", params)
}

@SuppressWarnings('unused')
void getHub2Data(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.debug resp.data
            try{
				def jSlurp = new JsonSlurper()
			    h2Data = (Map)jSlurp.parseText((String)resp.data)
            } catch (eIgnore) {
                if (debugEnable) log.debug "H2: $h2Data <br> ${resp.data}"
                return
            }
            
            hubAlerts = []
            h2Data.alerts.each{
                if(it.value == true){
                    if("$it.key".indexOf('Database') > -1)
                        hubAlerts.add("hubDatabaseSize")
                    else if("$it.key".indexOf('Load') > -1)
                        hubAlerts.add("hubLoad")
                    else if("$it.key" != "runAlerts")
                        hubAlerts.add(it.key)
                }
            }
            updateAttr("hubAlerts",hubAlerts)
            if(h2Data?.baseModel == null) {
                if (debugEnable) log.debug "baseModel is missing from h2Data, ${device.currentValue('hubModel')} ${device.currentValue('firmwareVersionString')}<br>$h2Data"
                return
            }
            if(h2Data.baseModel.zwaveStatus == "false") 
                updateAttr("zwaveStatus","enabled")
            else
                updateAttr("zwaveStatus","disabled")
            if(h2Data.baseModel.zigbeeStatus == "false"){
                updateAttr("zigbeeStatus2", "enabled")
                if (device.currentValue("zigbeeStatus", true) != null && device.currentValue("zigbeeStatus", true) != "enabled" && !state.errorZigbeeMismatch ){
                    log.warn "Zigbee Status has opposing values - radio was either turned off or crashed"
                    state.errorZigbeeMismatch = true
                } else state.errorZigbeeMismatch = false
            } else {
                updateAttr("zigbeeStatus2", "disabled")
                if (device.currentValue("zigbeeStatus", true) != null && device.currentValue("zigbeeStatus", true) != "disabled" && !state.errorZigbeeMismatch){
                    log.warn "Zigbee Status has opposing values - radio was either turned off or crashed."
                    state.errorZigbeeMismatch = true
                } else state.errorZigbeeMismatch = false                    
            }
            if(debugEnable) log.debug "securityInUse"
            updateAttr("securityInUse", h2Data.baseModel.userLoggedIn)
            if(debugEnable) log.debug "h2 security check"
            if((!security || password == null || username == null) && h2Data.baseModel.userLoggedin == true){
                log.error "Hub using Security but credentials not supplied"
                device.updateSetting("security",[value:"true",type:"bool"])
            }
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on H2 request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

@SuppressWarnings('unused')
void getExtNetwork(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
            if(!h2Data.usingStaticIP)
            updateAttr("staticIPJson", "{}")
            else {
                jMap = [staticIP:"${h2Data.staticIP}", staticGateway:"${h2Data.staticGateway}", staticSubnetMask:"${h2Data.staticSubnetMask}",staticNameServers:"${h2Data.staticNameServers}"]
                updateAttr("staticIPJson",JsonOutput.toJson(jMap))
            }
            if(h2Data.hasEthernet && h2Data.hasWiFi && h2Data.wifiNetwork != null ){
                updateAttr("connectType","Dual")
                updateAttr("lanIPAddr", h2Data.lanAddr)    
            } else if(h2Data.hasEthernet){
                updateAttr("connectType","Ethernet")
                updateAttr("lanIPAddr", h2Data.lanAddr)
            } else if(h2Data.hasWiFi)
                updateAttr("connectType","WiFi")
            if(h2Data.hasWiFi){
				if(h2Data.wifiNetwork != null)
					updateAttr("wifiNetwork", h2Data.wifiNetwork)
				else 
					updateAttr("wifiNetwork", "None")
				if(h2Data.wlanAddr != null) 
					updateAttr("wirelessIP",h2Data.wlanAddr)
				else
					updateAttr("wirelessIP", "None")
            } else {
                updateAttr("wifiNetwork", "None")
                updateAttr("wirelessIP", "None")
            }
            updateAttr("dnsServers", h2Data.dnsServers)
            updateAttr("lanIPAddr", h2Data.lanAddr)                           
        }
    }catch (ex) {
        if (!warnSuppress) log.warn ex
    }
}

void updateCheck(){
    if(security) cookie = getCookie()
    updateCheckReq(cookie)
}

void updateCheckReq(cookie){
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/cloud/checkForUpdate",
        timeout: 10,
        headers:["Cookie": cookie]
    ]
    asynchttpGet("getUpdateCheck", params)
}

@SuppressWarnings('unused')
void getUpdateCheck(resp, data) {
    if(debugEnable) log.debug "update check: ${resp.status}"
    try {
        if (resp.status == 200) {
            def jSlurp = new JsonSlurper()
            Map resMap = (Map)jSlurp.parseText((String)resp.data)
            if(resMap.status == "NO_UPDATE_AVAILABLE")
                updateAttr("hubUpdateStatus","Current")
            else
                updateAttr("hubUpdateStatus","Update Available")
            if(resMap.version)
                updateAttr("hubUpdateVersion",resMap.version)
            else updateAttr("hubUpdateVersion",location.hub.firmwareVersionString)
        }
    } catch(ignore) {
       updateAttr("hubUpdateStatus","Status Not Available")
    }

}

void zigbeeStackReq(cookie){
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/currentZigbeeStack",
        headers:["Cookie": cookie]
    ]
        asynchttpGet("getZigbeeStack",params) 
}

void getZigbeeStack(resp, data) {
    try {
        if(resp.data.toString().indexOf('standard') > -1)
            updateAttr("zigbeeStack","standard")
        else
            updateAttr("zigbeeStack","new")      
    } catch(ignore) { }
}
                     

@SuppressWarnings('unused')
boolean isCompatible(Integer minLevel) { //check to see if the hub version meets the minimum requirement
    String model = getHubVersion()
    String[] tokens = model.split('-')
    String revision = tokens.last()
    return (Integer.parseInt(revision) >= minLevel)
}

@SuppressWarnings('unused')
String getCookie(){
    try{
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
	  ) { resp -> 
		cookie = ((List)((String)resp?.headers?.'Set-Cookie')?.split(';'))?.getAt(0) 
        if(debugEnable)
            log.debug "$cookie"
	  }
    } catch (e){
        cookie = ""
    }
    return "$cookie"
}

@SuppressWarnings('unused')
Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    if(logResponses) log.info "File xFer Status: $retStat"
    return retStat
}

@SuppressWarnings('unused')
String readExtFile(fName){
    if(security) cookie = getCookie()    
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
            ]        
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
               if(debugEnable) log.info "Read External File result: delim"
               return delim
            }
            else {
                log.error "Read External - Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
Boolean fileExists(fName){
    if(fName == null) return false
    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri          
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                if(debugEnable) log.debug "File Exist: true"
                return true;
            } else {
                if(debugEnable) log.debug "File Exist: true"
                return false
            }
        }
    } catch (exception){
        if (exception.message == "status code: 404, reason phrase: Not Found"){
            if(debugEnable) log.debug "File Exist: false"
        } else if (resp.getStatus() != 408) {
            log.error "Find file $fName :: Connection Exception: ${exception.message}"
        }
        return false
    }
}

@SuppressWarnings('unused')
void hiaUpdate(htmlStr) {
	updateAttr("html",htmlStr)
}

void createHtml(){
    if(alternateHtml == null || fileExists("$alternateHtml") == false){
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"hubInfoTemplate.res", type:"string"])
    }
    String fContents = readFile("$alternateHtml")
    if(fContents == 'null' || fContents == null) {
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"hubInfoTemplate.res", type:"string"]) 
        fContents = readFile("$alternateHtml")
    }
    List fRecs=fContents.split("\n")
    String html = ""
    fRecs.each {
        int vCount = it.count("<%")
        if(debugEnable) log.debug "variables found: $vCount"
        if(vCount > 0){
            recSplit = it.split("<%")
            if(debugEnable) log.debug "$recSplit"
            recSplit.each {
                if(it.indexOf("%>") == -1)
                    html+= it
                else {
                    vName = it.substring(0,it.indexOf('%>'))
                    if(debugEnable) log.debug "${it.indexOf("5>")}<br>$it<br>${it.substring(0,it.indexOf("%>"))}"
                    if(vName == "date()" || vName == "@date")
                        aVal = new Date()
                    else if (vName == "@version")
                        aVal = version()
                    else {
                        aVal = device.currentValue("$vName",true)
                        String attrUnit = getUnitFromState("$vName")
                        if (attrUnit != null) aVal+=" $attrUnit"
                    }
                    html+= aVal
                    if(it.indexOf("%>")+2 != it.length()) {
                        if(debugEnable) log.debug "${it.substring(it.indexOf("%>")+2)}"
                        html+=it.substring(it.indexOf("%>")+2)
                    }
                }                 
            }
        }
        else html += it
    }
    if (debugEnable) log.debug html
    updateAttr("html", html)
}

@SuppressWarnings('unused')
String readFile(fName){
    if(security) cookie = getCookie()
    uri = "http://${location.hub.localIP}:8080/local/${fName}"

    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie,
                "Accept": "application/octet-stream"
            ]
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
               if(debugEnabled) log.info "File Read Data: $delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null;
    }
}
@SuppressWarnings('unused')
Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString(); 
    
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString;text/html; charset=utf-8"
			],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}


@SuppressWarnings('unused')
void reboot() {
    if(!allowReboot){
        log.error "Reboot was requested, but allowReboot was set to false"
        return
    }
    log.info "Hub Reboot requested"
    // start - Modified from dman2306 Rebooter app
    String cookie=(String)null
    if(security) cookie = getCookie()
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot",
			headers:[
				"Cookie": cookie
			]
		]
	) {		resp ->	} 
    // end - Modified from dman2306 Rebooter app
}

@SuppressWarnings('unused')
void shutdown() {
    if(!allowReboot){
        log.error "Shutdown was requested, but allowReboot/Shutdown was set to false"
        return
    }
    log.info "Hub Reboot requested"
    // start - Modified from dman2306 Rebooter app
    String cookie=(String)null
    if(security) cookie = getCookie()
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/shutdown",
			headers:[
				"Cookie": cookie
			]
		]
	) {		resp ->	} 
    // end - Modified from dman2306 Rebooter app
}
                   
void formatUptime(){
    updateAttr("uptime", location.hub.uptime)
    try {
        Long ut = location.hub.uptime.toLong()
        Integer days = Math.floor(ut/(3600*24)).toInteger()
        Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
        Integer min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
        Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()
        if(upTimeSep == null){
            device.updateSetting("upTimeSep",[value:",", type:"string"])
            upTimeSep = ","
        }
        utD=upTimeDesc.split("/")
        dayD = (days==1) ? utD[0].replace("s",""):utD[0]
        hrD = (hrs==1) ? utD[1].replace("s",""):utD[1]
        minD = (min==1) ? utD[2].replace("s",""):utD[2]
        if(utD[3] == " seconds")
            secD = (sec==1) ? " second":utD[3]
        else 
            secD = utD[3]
        
        String attrval = "${days.toString()}${dayD}$upTimeSep${hrs.toString()}${hrD}$upTimeSep${min.toString()}${minD}$upTimeSep${sec.toString()}${secD}"
        updateAttr("formattedUptime", attrval) 
    } catch(ignore) {
        updateAttr("formattedUptime", "")
    }
}

@SuppressWarnings('unused')
void restartCheck() {
    if(debugEnable) log.debug "$rsDate"
    Long ut = new Date().getTime().toLong() - (location.hub.uptime.toLong()*1000)
    Date upDate = new Date(ut)
    if(debugEnable) log.debug "RS: $rsDate  UT:$ut  upTime Date: $upDate   upTime: ${location.hub.uptime}"
    
    updateAttr("lastHubRestart", ut)
    
    if(rsrtSdfPref == null){
        device.updateSetting("rsrtSdfPref",[value:"yyyy-MM-dd HH:mm:ss",type:"string"])
        rsrtSdfPref="yyyy-MM-dd HH:mm:ss"
    }
    if(rsrtSdfPref == "Milliseconds") 
        updateAttr("lastHubRestartFormatted", upDate.getTime())
    else {
        SimpleDateFormat sdf = new SimpleDateFormat(rsrtSdfPref)
        updateAttr("lastHubRestartFormatted", sdf.format(upDate.getTime()))
    }
    
//    SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//    updateAttr("lastHubRestartFormatted",sdf.format(upDate))
}

void removeUnused() {
	prefList.each{ l1 ->
        l1.each{
            if(it.key.contains("parm")  && (settings["${it.key}"] == null || settings["${it.key}"] == "0")) {
                pMap = (HashMap) it.value
                if(debugEnable) log.debug "${it.key} poll${settings["${it.key}"]} ${pMap.attributeList}" 
                aList = pMap.attributeList.split(",")
                aList.each{
                    if(debugEnable) log.debug "device.deleteCurrentState(\"${it.trim()}\")"
                    device.deleteCurrentState("${it.trim()}")
                }
            }
        }
    }
    v2Cleanup()
}

@SuppressWarnings('unused')
String getUnitFromState(String attrName){
   	return device.currentState(attrName)?.unit
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

@Field static String minFwVersion = "2.2.8.141"
@Field static List <String> pollList = ["0", "1", "2", "3", "4"]
@Field static prefList = [[parm01:[desc:"CPU Temperature Polling", attributeList:"temperatureF, temperatureC, temperature", method:"cpuTemperatureReq"]],
[parm02:[desc:"Free Memory Polling", attributeList:"freeMemory", method:"freeMemoryReq"]],
[parm03:[desc:"CPU Load Polling", attributeList:"cpuLoad, cpuPct", method:"cpuLoadReq"]],
[parm04:[desc:"DB Size Polling", attributeList:"dbSize", method:"dbSizeReq"]],
[parm05:[desc:"Public IP Address", attributeList:"publicIP", method:"publicIpReq"]],
[parm06:[desc:"Max Event/State Days Setting", attributeList:"maxEvtDays,maxStateDays", method:"evtStateDaysReq"]], 
[parm07:[desc:"ZWave Version", attributeList:"zwaveVersion, zwaveSDKVersion", method:"zwaveVersionReq"]],
[parm08:[desc:"Time Sync Server Address", attributeList:"ntpServer", method:"ntpServerReq"]],
[parm09:[desc:"Additional Subnets", attributeList:"ipSubnetsAllowed", method:"ipSubnetsReq"]],
[parm10:[desc:"Hub Mesh Data", attributeList:"hubMeshData, hubMeshCount", method:"hubMeshReq"]],
[parm11:[desc:"Expanded Network Data", attributeList:"connectType (Ethernet, WiFi, Dual), dnsServers, staticIPJson, lanIPAddr, wirelessIP, wifiNetwork", method:"extNetworkReq"]],
[parm12:[desc:"Check for Firmware Update",attributeList:"hubUpdateStatus, hubUpdateVersion",method:"updateCheckReq"]],
[parm13:[desc:"Zwave Status & Hub Alerts",attributeList:"hubAlerts,zwaveStatus, zigbeeStatus2, securityInUse", method:"hub2DataReq"]],
[parm14:[desc:"Base Data",attributeList:"firmwareVersionString, hardwareID, id, latitude, localIP, localSrvPortTCP, locationId, locationName, longitude, name, temperatureScale, timeZone, type, uptime, zigbeeChannel, zigbeeEui, zigbeeId, zigbeeStatus, zipCode",method:"baseData"]]]
@Field static String ttStyleStr = "<style>.tTip {display:inline-block;border-bottom: 1px dotted black;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;}</style>"
@Field sdfList = ["yyyy-MM-dd","yyyy-MM-dd HH:mm","yyyy-MM-dd h:mma","yyyy-MM-dd HH:mm:ss","ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "HH:mm", "H:mm","h:mma", "HH:mm:ss", "Milliseconds"]
