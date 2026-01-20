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
 *    2023-02-14                 v3.0.16 - add connectCapable
 *                               v3.0.17 - Check for html conflict at startup
 *    2023-02-23                 v3.0.18 - Add html attribute output file option
 *    2023-02-27                 v3.0.19 - Add 15 minute averages for CPU Load, CPU Percentage, and Free Memory
 *    2023-03-09                 v3.0.20 - Add cloud connection check
 *                               v3.0.21 - Modify the cloud check to allow a user specified device
 *    2023-03-10                 v3.0.22 - Add dnsStatus check
 *    2023-03-14                 v3.0.23 - Change Font to red/bold if Cloud URL is blank or does not contain cloud.hubitat
 *    2023-03-25                 v3.0.24 - Add Zigbee Stack check back in
 *    2023-03-28                 v3.0.25 - Check attribute values for startup message
 *    2023-03-29                 v3.0.26 - Remove Zigbee Stack check as the endpoint is no longer available
 *    2023-10-13                 v3.0.27 - add lanSpeed attribute
 *    2023-10-20                 v3.0.28 - add zigbeeInfo endpoint data if HE>= 2.3.6.1
 *    2023-10-24                 v3.0.29 - HE 2.3.7.x zigbee endpoint change
 *    2023-10-24                 v3.0.30 - Add Matter attributes
 *    2023-11-14                 v3.0.31 - Suppress error on extended Zigbee/Matter reads if hub not ready
 *    2023-11-27                 v3.0.32 - Reboot with Rebuild Option
 *    2024-01-05                 v3.0.33 - Use file methods instead of endpoints if available
 *                               v3.0.34 - Reboot with Log Purge, Rebuild changes
 *    2024-01-09                 v3.0.35 - Allow Matter attributes for C-5, C-7, and C-8
 *				                 v3.0.36 - Allow C-8 Pro to pass Compatibility checks
 *    2024-03-07                 v3.0.37 - add /hub/advanced/zipgatewayVersion endpoint
 *    2024-03-19                 v3.0.38 - add pCloud (passive cloud check)
 *    2024-03-28                 v3.0.39 - add GB option for memory display
 *                               v3.0.40 - Dynamic unit option for memory display
 *    2024-04-16                 v3.0.41 - lanspeed source change
 *.   2024-05-07                 v3.0.42 - fix C8 Pro failing Matter compatibility check
 *    2024-05-10                 v3.0.43 - Add a delayed base data check on initialization
 *    2024-07-12                 v3.1.0/v3.1.1 - 127.0.0.1 replacement *** best using 2.3.9.159+
 *    2024-07-22                 v3.1.2 - Added accessList attribute
 *    2024-07-23                 v3.1.3 - streamline the firmware version checks
 *    2024-07-24		         v3.1.4 - correct an issue with blank headers and endpoints
 *                               v3.1.5 - reboot and shutdown headers issue
 *    2024-07-30                 v3.1.6 - add security information back in for hub2 data
 *                               v3.1.7 - alternate method to detect security in use
 *    2024-07-31                 v3.1.8 - split securityInUse check out into its own option, code cleanup
 *    2024-08-06                 v3.1.9 - add a notification URL for hub shutdown
 *    2024-11-08                 v3.1.10 - Add capability URL and attributes to allow display on Easy Dash
 *                               v3.1.11 - Fix degree symbol when using File Manager output
 *    2024-11-16                 v3.1.12 - fix min version check
 *    2025-01-31				 v3.1.13 - add zwaveJS(enabled/disabled), zwaveRegion, zwaveUpdateAvail(true/false), zigbeeUpdateAvail(true/false)
 *    2025-04-06				 v3.1.14 - Add jvmSize, jvmFree, zwHealthy, zbHealthy
 *	  2025-05-02				 v3.1.15 - Trap file write attempt without data 
 *	  2025-06-24				 v3.1.16 - Add sunriseTomorrow and sunsetTomorrow
 *	  2025-06-26				 v3.1.17 - negative hour fix for sunriseTomorrow
 *	  2025-09-23				 v3.1.18 - Add app state compression attribute
 *	  2025-10-02				 v3.1.19 - fix typo on zbHealthy
 *	  2025-11-25				 v3.1.20 - Extend H2 data timeout to 1500
 *	  2025-11-27				 v3.1.21 - change H2 to httpGet
 *	  2025-12-07				 v3.1.22 - add javaDirect
 *	  2025-12-23				 v3.1.23 - move driver version update code
 *	  2026-01-13				 v3.1.24 - h2Data issue
 *	  2023-01-20				 v3.1.25 - make freeMem15 unit agree with freeMemory
*/
import java.text.SimpleDateFormat
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

@SuppressWarnings('unused')
static String version() {return "3.1.24"}

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
        capability "URL" //Virtual URL Device for Easy Dash
        
        attribute "latitude", "string"
        attribute "longitude", "string"
        attribute "id", "string"
        attribute "name", "string"
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
        //attribute "zigbeeStack", "string"
        attribute "zwaveStatus", "string"
        attribute "hubAlerts", "string"
        attribute "hubMeshData", "string"
        attribute "hubMeshCount", "number"
        attribute "securityInUse", "string"
		attribute "sunrise", "string"
		attribute "sunset", "string"
        attribute "nextPoll", "string"
        //HE v2.3.4.126
        attribute "connectType", "string" //Ethernet, WiFi, Dual, Not Connected
        attribute "dnsServers", "string"
        attribute "staticIPJson", "string"
        attribute "lanIPAddr", "string"
        attribute "wirelessIP", "string"
        attribute "wifiNetwork", "string"
        attribute "connectCapable", "string" //Ethernet, WiFi, Dual
        attribute "cpu15Min", "number"
        attribute "cpu15Pct", "number"
        attribute "freeMem15", "number"
        attribute "cloud", "string"
        attribute "pCloud", "string"
        attribute "dnsStatus", "string"
        attribute "lanSpeed", "string"
        attribute "zigbeePower", "number"
        attribute "zigbeePan", "string"
        attribute "zigbeeExtPan", "string"
        attribute "matterEnabled", "string"
        attribute "matterStatus", "string"
        attribute "releaseNotesUrl", "string"
        attribute "accessList","string"
        attribute "sunriseTomorrow","string"
        attribute "sunsetTomorrow","string"
		//HE v2.7.3.1
		attribute "zwaveJS", "string"
        attribute "zwaveRegion","string"
		attribute "zwaveUpdateAvail", "string"
		attribute "zigbeeUpdateAvail", "string"
        // HE v2.4.1.154
        attribute "jvmFree", "number"
        attribute "jvmSize", "number"
        attribute "zbHealthy", "string"
        attribute "zwHealthy", "string"
        // Virtual URL Device attributes
        attribute "URL", "string"
        attribute "type", "string"//iframe, image, link, or video
        //HE v2.4.3.121
        attribute "appStateCompression", "string"
		attribute "javaDirect","number"
        command "hiaUpdate", ["string"]
        command "reboot"
        command "rebootW_Rebuild"
        command "rebootPurgeLogs"
        command "shutdown"
        command "updateCheck"
        command "removeUnused"
    }   
}
preferences {
    if(state?.errorMinVersion || state?.errorMinVersion == "true") 
        input("errMsg", "hidden", title:"<b>Minimum Version Error</b>",description:"<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
    input("quickref","hidden", title:"$ttStyleStr<a href='https://htmlpreview.github.io/?https://github.com/thebearmay/hubitat/blob/main/hubInfoQuickRef3.html' target='_blank'>Quick Reference v${version()}</a>")
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("warnSuppress", "bool", title: "Suppress Warn Level Logging", width:4)

	prefList.each { l1 ->
        l1.each{
		    pMap = (HashMap) it.value
            input ("${it.key}", "enum", title: "<div class='tTip'>${pMap.desc}<span class='tTipText'>${pMap.attributeList}</span></div>", options:pollList, submitOnChange:true, width:4, defaultValue:"0")
        }
	}
    if(parm16 != null && parm16 != 0 && parm16 != "0")
        input("makerInfo", "string", title: "<span style='$cloudFontStyle'>MakerApi or Dashboard URL string</span>", submitOnChange: true)
    input("remUnused", "bool", title: "Remove unused attributes", defaultValue: false, submitOnChange: true, width:4)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("alternateHtml", "string", title: "Template file for HTML attribute", submitOnChange: true, defaultValue: "hubInfoTemplate.res", width:4)
    input("htmlOutput", "string", title: "HTML Attribute Output for > 1024 characters", submitOnChange:true, defaultValue:"hubInfoOutput.html", width:4)
    input("forceFileOutput","bool", title:"Always use Output file for HTML Attribute", submitOnChange:true, width:4)
    input("attrLogging", "bool", title: "Log all attribute changes", defaultValue: false, submitOnChange: true, width:4)
    input("allowReboot","bool", title: "Allow Hub to be shutdown or rebooted", defaultValue: false, submitOnChange: true, width:4)
    input("freeMemUnit", "enum", title: "Free Memory Unit", options:["KB","MB","GB","Dynamic"], defaultValue:"KB", width:4)
    input("sunSdfPref", "enum", title: "Date/Time Format for Sunrise/Sunset", options:sdfList, defaultValue:"HH:mm:ss", width:4)
    input("updSdfPref", "enum", title: "Date/Time Format for Last Poll Time", options:sdfList, defaultValue:"Milliseconds", width:4)
    input("rsrtSdfPref", "enum", title: "Date/Time Format for Hub Restart Formatted", options:sdfList, defaultValue:"yyyy-MM-dd HH:mm:ss", width:4)  
    input("upTimeSep", "string", title: "Separator for Formatted Uptime", defaultValue: ", ", width:4)
    input("upTimeDesc", "enum", title: "Uptime Descriptors", defaultValue:"d/h/m/s", options:["d/h/m/s"," days/ hrs/ min/ sec"," days/ hours/ minutes/ seconds"],width:4)
    input("onShutdownUrl","string", title: "URL to notify when hub receives a shutdown request", width:4)
	input("pollRate1", "number", title: "Poll Rate for Queue 1 in minutes", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate2", "number", title: "Poll Rate for Queue 2 in minutes", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate3", "number", title: "Poll Rate for Queue 3 in minutes", defaultValue:0, submitOnChange: true, width:4) 
    input("pollRate4", "number", title: "Poll Rate for Queue 4 in <b style='background-color:red'>&nbsp;hours&nbsp;</b>", defaultValue:0, submitOnChange: true, width:4) 
}
@SuppressWarnings('unused')
void installed() {
    log.trace "installed()"
    xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/hubInfoTemplate.res","hubInfoTemplate.res")
    initialize()
    configure()
}

void initialize() {
    restartCheck()
    updated()
    runIn(30,"initMemory")
    if (settings["parm12"] != 0)
        runIn(30,"updateCheck")
    if(!state?.v2Cleaned)
        v2Cleanup()
    if(driverVersionCheck())
    	runIn(5,"updated")
    log.info "Hub Information v${version()} initialized"
    runIn(120,"baseData")

}

void initMemory(){
    
    freeMemoryReq()    
}

void configure() {
    updated()
    baseData()
    if(!state?.v2Cleaned)
        v2Cleanup()    
}

void updated(){
    baseData()
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
    
    if(htmlOutput == null) 
        device.updateSetting("htmlOutput",[value:"hubInfoOutput.html",type:"string"])
    device.updateSetting("htmlOutput",[value:toCamelCase(htmlOutput),type:"string"])
    if(makerInfo == null || !makerInfo.contains("https://cloud.hubitat.com/"))
        cloudFontStyle = 'font-weight:bold;color:red'
    elseversion
        cloudFontStyle = ''

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

boolean driverVersionCheck(){
    if(version() != getDataValue('driverVersion')){
    	updateDataValue('driverVersion', "${version()}")
        return true
    } else
        return false
}


void poll1(){
    
	state.poll1.each{
		this."$it"()
	}
	if(pollRate1 > 0)
		runIn(pollRate1*60, "poll1")
    everyPoll("poll1")
}

void poll2(){
    
	state.poll2.each{
		this."$it"()
	}
	if(pollRate2 > 0)
		runIn(pollRate2*60, "poll2")
    everyPoll("poll2")
}

void poll3(){
    
	state.poll3.each{
		this."$it"()
	}
	if(pollRate3 > 0)
		runIn(pollRate3*60, "poll3")
    everyPoll("poll3")
}

void poll4(){
    
	state.poll4.each{
		this."$it"()
	}
	if(pollRate4 > 0)
		runIn(pollRate4*60*60, "poll4")
    everyPoll("poll4")
}

void baseData(dummy=null){
	if(driverVersionCheck())
    	runIn(5,"updated")
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
    
    if(!minVerCheck(minFwVersion)) {
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

    if(minVerCheck("2.3.6.1"))
        extendedZigbee()
    if(minVerCheck("2.4.1.103"))
    	zwaveJsStat()
    if(minVerCheck("2.4.3.121"))
    	checkAppComp()
    everyPoll("baseData")
}

void everyPoll(whichPoll=null){
    updateAttr("currentMode", location.properties.currentMode)
    updateAttr("currentHsmMode", location.hsmStatus)
    
    SimpleDateFormat sdfIn = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    sunrise = sdfIn.parse(location.sunrise.toString())
    sunset = sdfIn.parse(location.sunset.toString())
    
    int yearST=new Date().getYear() + 1900
    int monthST=new Date().getMonth() + 1
    int dayST=new Date().getDate() + 1
    
    try {
    	LocalDate.of(yearST,monthST,dayST)
    } catch (dCheck){
    	monthST++
        dayST = 1
        if(monthST > 12) 
            monthST = 1
    }
    
    ZonedDateTime ssTom = calculateSunset(yearST, monthST, dayST)
    ZonedDateTime srTom = calculateSunrise(yearST, monthST, dayST)
       
	if(sunSdfPref == null) device.updateSetting("sunSdfPref",[value:"HH:mm:ss",type:"enum"])
    	
    if(sunSdfPref != "Milliseconds") {
        SimpleDateFormat sdf = new SimpleDateFormat(sunSdfPref)
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(sunSdfPref)
        updateAttr("sunrise", sdf.format(sunrise))
	    updateAttr("sunset", sdf.format(sunset))
        updateAttr("sunsetTomorrow",ssTom.format(dtf))
    	updateAttr("sunriseTomorrow",srTom.format(dtf))
    } else {
        updateAttr("sunrise", sunrise.getTime())
	    updateAttr("sunset", sunset.getTime())
        updateAttr("sunsetTomorrow",ssTom.toInstant().toEpochMilli())
    	updateAttr("sunriseTomorrow",srTom.toInstant().toEpochMilli())
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
    if(aValue.contains("Your hub is starting up"))
       return

    sendEvent(name:aKey, value:aValue, unit:aUnit, descriptionText:"$aKey : $aValue$aUnit")
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void cpuTemperatureReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/internalTempCelsius",
        headers: [
            "Connection-Timeout":600
        ]
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


void freeMemoryReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemory",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnable) log.debug params
        asynchttpGet("getFreeMemory", params)    
    jvmReq()
}

@SuppressWarnings('unused')
void getFreeMemory(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            Integer memWork = new Integer(resp.data.toString())
            if(debugEnable) 
                log.debug "Free Memory $memWork"
            if(freeMemUnit == "Dynamic"){
                if(memWork > 1048575){ 
                    freeMemUnit = "GB"
                    if(debugEnable) log.debug "unit is $freeMemUnit"
                }else if(memWork > 150000){
                    freeMemUnit = "MB"
                    if(debugEnable) log.debug "unit is $freeMemUnit"
                }
                else freeMemUnit = "KB"
            }
                               
            if(freeMemUnit == "GB")
                updateAttr("freeMemory",(new Float(memWork/1024/1024).round(2)), "GB")
            else
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

void jvmReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemoryHistory",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnable) log.debug params
        asynchttpGet("getJvm", params)    
}

@SuppressWarnings('unused')
void getJvm(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            //log.debug "${resp.data}"
            ArrayList memRecs = resp.data.toString().split('\n')
            String memRec = memRecs[memRecs.size()-1]
            ArrayList memWork = memRec.split(',')
            if(debugEnable) 
            	log.debug "JVM record ${memRecs.size()} ${memWork.size()} $memRec<br> $memWork"
                           
        	if(memWork.size() >= 5){
               updateAttr("jvmSize",memWork[3], "KB")
               updateAttr("jvmFree",memWork[4], "KB")
            }
            if(memWork.size >= 6) {
                updateAttr("javaDirect",memWork[5], "KB")
            }
    	}
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getJvm httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
}

void fifteenMinute(){
    if(location.hub.uptime < 900) { //if the hub hasn't been up 15 minutes use current 5 min values
        updateAttr("cpu15Min",device.currentValue("cpu5Min",true))
        updateAttr("cpu15Pct",device.currentValue("cpuPct",true),"%")
        updateAttr("freeMem15",device.currentValue("freeMemory",true))        
        return
    }
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemoryHistory",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnable) log.debug params
    asynchttpGet("get15Min", params)        
}

void get15Min(resp, data){
    String loadWork
    List<String> loadRec = []
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            loadWork = resp.data.toString()
        }
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "get15min httpResp = $respStatus but returned invalid data, will retry next cycle"    
    }
    if (loadWork) {
        Integer lineCount = 0
        loadWork.eachLine{
            lineCount++
        }
        Integer lineCount2 = 0
        Double cpuWork = 0.0
        Long memWork = 0
        loadWork.eachLine{
            lineCount2++
            if(lineCount2 > lineCount-3){
                workSplit = it.split(",")
                cpuWork+=workSplit[2].toDouble()
                memWork+=workSplit[1].toLong()
            }
        }
        memWork/=3
        cpuWork/=3                   
        updateAttr("cpu15Min",cpuWork.round(2))
        cpuWork = (cpuWork/4.0D)*100.0D //Load / #Cores - if cores change will need adjusted to reflect
        updateAttr("cpu15Pct",cpuWork.round(2),"%")
        
		if(freeMemUnit == "Dynamic"){
			if(memWork > 1048575){ 
				freeMemUnit = "GB"
			}else if(memWork > 150000){
				freeMemUnit = "MB"
            }else 
                freeMemUnit = "KB"
        }
    	if(freeMemUnit == "GB")
			updateAttr("freeMem15",(new Float(memWork/1024/1024).round(2)), "GB")
		else
		if(freeMemUnit == "MB")
			updateAttr("freeMem15",(new Float(memWork/1024).round(2)), "MB")
		else
			updateAttr("freeMem15",memWork, "KB")
        }
}

void cpuLoadReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/freeOSMemoryLast",
        headers: [
            "Connection-Timeout":600
        ]
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
void dbSizeReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/databaseSize",
        headers: [
            "Connection-Timeout":600
        ]
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

void publicIpReq(){
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

void evtStateDaysReq(){
    //Max State Days
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/maxDeviceStateAgeDays",
        headers: [
            "Connection-Timeout":600
        ]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getStateDays", params)
     
     //Max Event Days
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/maxEventAgeDays",
        headers: [
            "Connection-Timeout":600
        ]           
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

void zwaveVersionReq(){
    if(!isCompatible(7)) {
        if(!warnSuppress) log.warn "ZWave Version information not available for this hub"
        return
    }
    param = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/zwaveVersion",
        headers: [
            "Connection-Timeout":600
        ]
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
    if(device.currentValue('zwaveJS',true) != 'true' && zString.length() != 4){
    	if(start == -1 || end < 1 || zString.indexOf("starting up") > 0 ){ //empty or invalid string - possibly non-C7
        	//updateAttr("zwaveData",null)
        	if(!warnSuppress) log.warn "Invalid ZWave Data returned ($zString) "
    	}else {
        	wrkStr = zString.substring(start,end)
        	wrkStr = wrkStr.replace("(","[")
        	wrkStr = wrkStr.replace(")","]")

        	HashMap zMap = (HashMap)evaluate(wrkStr)
        	updateAttr("zwaveVersion","${zMap?.firmware0Version}.${zMap?.firmware0SubVersion}.${zMap?.hardwareVersion}")
        }
    }else
       updateAttr("zwaveVersion","$zString")
                  
    if(!minVerCheck("2.3.8.124"))
        updateAttr("zwaveSDKVersion","${((List)zMap.targetVersions)[0].version}.${((List)zMap.targetVersions)[0].subVersion}")
    else {
            params = [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/zipgatewayVersion",
                headers: [
                    "Connection-Timeout":600
                ]           
            ]
            httpGet(params) { resp ->
                updateAttr("zwaveSDKVersion",resp.data)
            }
    }
            
}

void ntpServerReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/ntpServer",
        headers: [
            "Connection-Timeout": 600
        ]           
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

void ipSubnetsReq(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/allowSubnets",
        headers: [
            "Connection-Timeout": 600
        ]           
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

void hubMeshReq(){
    params =  [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubMeshJson",
        headers: [
            "Connection-Timeout":600
        ]           
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
            Map hmData = (Map)jSlurp.parseText((String)resp.data)
            i=0
            subMap2=[:]
            jStr="["
            hmData.hubList.each{
                if(i>0) jStr+=","
                jStr+="{\"hubName\":\"$it.name\","
                jStr+="\"active\":\"$it.active\","
                jStr+="\"offline\":\"$it.offline\","
                jStr+="\"ipAddress\":\"$it.ipAddress\"}"        
                i++
            }
            jStr+="]"
            updateAttr("hubMeshData", jStr)
            updateAttr("hubMeshCount",i)

        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on Hubmesh request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

void extNetworkReq(){
    if(!minVerCheck("2.3.4.126")){
        if(!warnSuppress) log.warn "Extend Network Data not available for HE v${location.hub.firmwareVersionString}"
        return
    }
        
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/networkConfiguration",
        headers: [
            "Connection-Timeout":600
        ]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getExtNetwork", params)
}

void hub2DataReq() {

    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubData",
        timeout: 300,
        headers: [
            "Connection-Timeout": 1500
        ]                   
    ]
    
	if(debugEnable)
    	log.debug params
    httpGet(params) {resp ->
	    try{
            if (debugEnable) 
        		log.debug resp.data
            try{
//				def jSlurp = new JsonSlurper()
//			    h2Data = (Map)jSlurp.parseText((String)resp.data)
            	h2Data = (Map)resp.data
            } catch (eMsg) {
                if (debugEnable) 
                	log.debug "H2: $eMsg<br>$h2Data <br> ${resp.data}"
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
            } else {
                updateAttr("zigbeeStatus2", "disabled")                 
            }
            /*******************************************************************************************************
            * userLoggedIn is ONLY true if security is in use and the user has provided credentials to this driver * 
			* use /logout endpoint to check instead                                                                *
            *******************************************************************************************************/
            if(!h2Data.baseModel.cloudDisconnected){
                updateAttr("pCloud", "connected")
            } else {
                updateAttr("pCloud", "not connected")
            }
	    } catch (Exception ex){
    	    if (!warnSuppress) log.warn ex
    	}  
    }

    checkSecurity()
    zHealthReq()

}

void checkSecurity(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/logout",
        followRedirects: false,
        headers: [
            "Connection-Timeout": 300
        ]                   
    ]
    asynchttpGet("getSecurity", params)
    
}
@SuppressWarnings('unused')
void getSecurity(resp, data){

    if(resp.headers.Location == 'http://127.0.0.1:8080/login')
        updateAttr("securityInUse",'true')
    else
        updateAttr("securityInUse",'false')
    
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
            
            if(h2Data.hasEthernet && h2Data.hasWiFi)
                updateAttr("connectCapable","Dual")
            else if(h2Data.hasEthernet)
                updateAttr("connectCapable","Ethernet")
            else if (h2Data.hasWiFi)
                updateAttr("connectCapable","WiFi")
            else
                updateAttr("connectCapable", "Unknown")
            
            if(lanIPAddr != null)
                updateAttr("lanIPAddr", h2Data.lanAddr)
            else
                updateAttr("lanIPAddr", "None")
            
            if(h2Data.wlanAddr != null) 
                updateAttr("wirelessIP",h2Data.wlanAddr)
            else
                updateAttr("wirelessIP","None")
            
            if(h2Data.wifiNetwork != null)
					updateAttr("wifiNetwork", h2Data.wifiNetwork)
				else 
					updateAttr("wifiNetwork", "None")
            
            if(h2Data.wifiNetwork && h2Data.wlanAddr && h2Data.lanAddr)
                updateAttr("connectType","Dual")
            else if(h2Data.wifiNetwork && h2Data.wlanAddr)
                updateAttr("connectType", "WiFi")
            else if(h2Data.lanAddr)
                updateAttr("connectType","Ethernet")
            else
                updateAttr("connectType","Not Connected")                
            
            updateAttr("lanIPAddr", h2Data.lanAddr)
            
            dnsList = []
            if(h2Data.usingStaticIP){
                h2Data.staticNameServers.each{
                    dnsList.add("$it")
                }
            }else {
                h2Data.dhcpNameServers.each{
                    dnsList.add("$it")
                }
            }
            h2Data.dnsServers.each{
                dnsList.add("$it")
            }
            dnsList = dnsList.unique()
            checkDns(dnsList)
            updateAttr("dnsServers", dnsList)
            if(h2Data.lanAutoneg == 'AUTONEG')
                updateAttr("lanSpeed","Auto","mbps")
            else
                updateAttr("lanSpeed", "100","mbps")

        }
    }catch (ex) {
        if (!warnSuppress) log.warn ex
    }
}

void checkDns(dnsList) {
    if(dnsList == null){
        updateAttr("dnsStatus","inactive")
        return
    }
        
    for(i=0;i<dnsList.size();i++){
        hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(dnsList[i],1)
        int pTran = pingData.packetsTransmitted.toInteger()
        if (pTran == 0){ // 2.2.7.121 bug returns all zeroes on not found
            pingData.packetsTransmitted = numPings
            pingData.packetLoss = 100
        }
        if (pingData.packetLoss < 100){               
            updateAttr("dnsStatus","active")
            i=dnsList.size()
        } else {         
            updateAttr("dnsStatus","inactive")
        }
    }
}

void updateCheck(){
    
    updateCheckReq()
}

void updateCheckReq(){
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/cloud/checkForUpdate",
        timeout: 10
    ]
    asynchttpGet("getUpdateCheck", params)
}

@SuppressWarnings('unused')
void getUpdateCheck(resp, data) {
    if(debugEnable) log.debug "update check: ${resp.status}"
    try {
        if (resp.status == 200) {
            def jSlurp = new JsonSlurper()
            /*/Temporary capture
            tempStr = readFile("updateLog.txt")
            tempStr+="\n${resp.data}"
            writeFile("updateLog.txt",tempStr)
            /*/
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

void zHealthReq(){
    if(!minVerCheck("2.4.1.154"))
    	return
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/zigbee/healthStatus"
    ]
        asynchttpGet("getZbHealth",params) 
    
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/zwave/healthStatus"
    ]
        asynchttpGet("getZwHealth",params) 
}

void getZbHealth(resp, data) {
    try {
        updateAttr("zbHealthy",resp.data)
      
    } catch(ignore) { }
}

void getZwHealth(resp, data) {
    try {
        updateAttr("zwHealthy",resp.data)
      
    } catch(ignore) { }
}

void checkCloud(){
    if(makerInfo == null || !makerInfo.contains("https://cloud.hubitat.com/")) {
        updateAttr("cloud", "invalid endpoint")
        cloudFontStyle = 'font-weight:bold;color:red'
        return
    }
    if(makerInfo.contains("Device ID"))
      makerInfo=makerInfo.replace("[Device ID]","${device.deviceId}")
   
    if(!makerInfo.contains("dashboard")){
        cType="maker"
        dId=makerInfo.substring(makerInfo.lastIndexOf('/')+1,makerInfo.indexOf('?'))

    } else {
        cType="dashboard"
        dId=makerInfo.substring(makerInfo.lastIndexOf('/')+1,makerInfo.indexOf('?'))
    }
    params = [
       uri    : makerInfo,
       headers: [Accept:"application/json"]
    ]
    //log.debug "$params"
    asynchttpGet("getCloudReturn", params, [cType:"$cType",dId:"$dId"]) 
}

void getCloudReturn(resp, data){
    try{
        if(resp.status == 200 && makerInfo.substring(makerInfo.lastIndexOf('/')+1,makerInfo.indexOf('?')) == "${data["dId"]}") {
            updateAttr("cloud", "connected")
        } else {
            updateAttr("cloud", "not connected")
        } 
    } catch (EX) {
        updateAttr("cloud", "not connected")
    }
        
}

void extendedZigbee(){
    
    if(minVerCheck("2.3.7.1"))
        zPath = "/hub/zigbeeDetails/json"
    else
        zPath = "/hub2/zigbeeInfo"
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : zPath,
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getExtendedZigbee", params)    
}    

void getExtendedZigbee(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map zbData = (Map)jSlurp.parseText((String)resp.data)
        updateAttr("zigbeeStatus","${zbData.networkState}".toLowerCase())
        updateAttr("zigbeePower",zbData.powerLevel)
		updateAttr("zigbeeUpdateAvail", zbData.firmwareUpdateAvailable)
        if(zbData?.pan) updateAttr("zigbeePan",zbData.pan)
        if(zbData?.epan) updateAttr("zigbeeExtPan",zbData.epan)
    } catch (EX) {
        //log.error "$EX"
    }
        
}

void extendedZwave(){
    
    if(!minVerCheck("2.3.7.1"))
        return
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/zwaveDetails/json",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getExtendedZwave", params)    
}    

void getExtendedZwave(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map zwData = (Map)jSlurp.parseText((String)resp.data)
        updateAttr("zwaveUpdateAvail","${zwData.isRadioUpdateNeeded}")
        updateAttr("zwaveRegion","${zwData.region}")
//        updateAttr("zwaveStatus",zwData.enabled) //already caught in hub2 data
    } catch (EX) {
        //log.error "$EX"
    }
    zwaveJsStat()
        
}

void zwaveJsStat(){
	params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/zwave2/status",
        headers: [
            "Connection-Timeout":600
        ]
    ]
	if (debugEnabled) log.debug params
    asynchttpGet("getZwaveJsStat", params) 
}

void getZwaveJsStat(resp, data){
    try{
        updateAttr("zwaveJS","${resp.data}")
    } catch (EX) {
        //log.error "$EX"
    }
        
}


void checkMatter(){
    hubModel = getHubVersion()
    if(!(isCompatible(5)))
        return
    
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/matterDetails/json",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getMatter", params) 
    
}

void getMatter(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map mData = (Map)jSlurp.parseText((String)resp.data)
        updateAttr("matterStatus","${mData.networkState}".toLowerCase())
        updateAttr("matterEnabled",mData.enabled)
    } catch (EX) {
        //log.error "$EX"
    }
        
}

void checkAccess(){
    if(!minVerCheck("2.3.9.159")){
        updateAttr('accessList','[]')
        return
    }
        
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/getLimitedAccessAddresses",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getAccess", params) 
}

void getAccess(resp, data) {
    try{
        if(resp.data.toString().contains('no limit set'))
            updateAttr('accessList','[]')
        else{
            aList = resp.data.toString().replace('<br>',',').replace('<br/>',',')
            updateAttr('accessList',aList.split(','))
        }
    } catch (e) {    
    }
}

void checkAppComp(){
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/advanced/stateCompressionStatus",
        headers: [
            "Connection-Timeout":600
        ]
    ]
    if (debugEnabled) log.debug params
    asynchttpGet("getCompressStatus", params)     
}

void getCompressStatus(resp, data) {
    try{
		updateAttr('appStateCompression',"${resp.data.toString()}")
    } catch (e) {    
    }
}

@SuppressWarnings('unused')
boolean isCompatible(Integer minLevel) { //check to see if the hub version meets the minimum requirement
    String model = getHubVersion()
    String[] tokens = model.split('-')
    String revision = tokens.last()
    if(revision.contains('Pro')) revision = 9
    return (Integer.parseInt(revision) >= minLevel)
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
        
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,   
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
    try{
        if(minVerCheck("2.3.4.134")){
            byte[] rData = downloadHubFile("$fName")
            fContent = new String(rData, "UTF-8")
            if(fContent.size() > 0) {
                if(debugEnable) log.debug "$fName File Exist: true"
                return true;
            } else {
                if(debugEnable) log.debug "$fName File Exist: false"
                return false;                
            }
        }
    } catch (ex) {
        log.error "$fName - $ex"
        return false
    }    
    
    uri = "http://127.0.0.1:8080/local/${fName}";

     def params = [
        uri: uri          
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                if(debugEnable) log.debug "$fName File Exist: true"
                return true;
            } else {
                if(debugEnable) log.debug "$fName File Exist: false"
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
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/hubInfoTemplate.res","hubInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"hubInfoTemplate.res", type:"string"])
    }
    String fContents = readFile("$alternateHtml")
    if(fContents == 'null' || fContents == null) {
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/hubInfoTemplate.res","hubInfoTemplate.res")
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
    if(!html.contains("support.hubitat.com")){
        updateAttr("html", html)
        state.htmlError = false
        if(html.size() > 1024 || forceFileOutput){
            if(htmlFileOutput == null) htmlFileOutput = "hubInfoOutput.html"
            html = html.replace("°","&deg;")
            writeFile(htmlFileOutput, html)
            updateAttr("html","<a href='http://${location.hub.localIP}:8080/local/$htmlFileOutput'>Link to attribute data</a>")
            updateAttr("URL","http://${location.hub.localIP}:8080/local/$htmlFileOutput")
            updateAttr("type","iframe")
        } else
            updateAttr("html", html)
    }else {
        updateAttr("html", "<h2>Hub Not Ready</h2><p>Please hit Initialize, or wait for next poll</p><p style='font-size:smaller'>${new Date()}</p>")
        if("${state.htmlError}" != "true"){
            state.htmlError = true
            runIn(20,"initialize")
        }
    }
}

@SuppressWarnings('unused')
String readFile(fName){
    try{
        if(minVerCheck("2.3.4.134")){
            byte[] rData = downloadHubFile("$fName")
            return new String(rData, "UTF-8")
        }
    } catch (ex) {
        log.error "$fName - $ex"
    }
    
    
    uri = "http://127.0.0.1:8080/local/${fName}"

    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				
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
    if(fData.size() < 1) {
        log.error "$fName cannot be created with size ${fData.size()}"
        return false
    }
    if(minVerCheck("2.3.4.134")){
        wData = fData.getBytes("UTF-8")
        uploadHubFile("$fName",wData)
        return true
    }
    
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
    
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot"
		]
	) {		resp ->	} 
}

@SuppressWarnings('unused')
void rebootW_Rebuild() {
    if(!allowReboot){
        log.error "Reboot was requested, but allowReboot was set to false"
        return
    }
    if(!minVerCheck("2.3.7.122")){
        log.error "Reboot with rebuild was requested, but failed HE min version."
        return        
    }
    log.info "Hub Reboot with Rebuild requested"
    
    if(!minVerCheck("2.3.7.14")){
    	httpPost(
	    	[
		    	uri: "http://127.0.0.1:8080",
			    path: "/hub/rebuildDatabaseAndReboot"
    		]
	    ) {		resp ->	} 
    } else {
        httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot",
			headers:[
                "Content-Type": "application/x-www-form-urlencoded"
			],
            body:[rebuildDatabase:"true"] 
	    ]
    	) {		resp ->	} 
    }
}

void rebootPurgeLogs() {
    if(!allowReboot){
        log.error "Reboot was requested, but allowReboot was set to false"
        return
    }
    if(!minVerCheck("2.3.7.140")){
        log.error "Reboot with Purge was requested, but failed HE min version."
        return        
    }
    log.info "Hub Reboot & Log Purge requested"
    
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot",
			headers:[
                "Content-Type": "application/x-www-form-urlencoded"
			],
            body:[purgeLogs:"true"] 
		]
	) {		resp ->	} 
}

@SuppressWarnings('unused')
void shutdown() {
    if(!allowReboot){
        log.error "Shutdown was requested, but allowReboot/Shutdown was set to false"
        return
    }
    log.info "Hub Shutdown requested"
    if(onShutdownUrl) 
        sendShutdownUrl()

	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/shutdown"
		]
	) {		resp ->	} 

}

@SuppressWarnings('unused')
void sendShutdownUrl(){
    if(debugEnabled) log.debug "Shutdown is sending request to $onShutdownUrl"
    params = [
			uri: onShutdownUrl,
			headers:[
                "Connection-Timeout":600
			]
		]
    asynchttpGet("ssdResp",params)    
}

void ssdResp(resp, data){
    if(debugEnabled) 
        log.debug "${resp.properties}"
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
String toCamelCase(init) {
    if (init == null)
        return null;

    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

@SuppressWarnings('unused')
Boolean minVerCheck(vStr){  //check if HE is >= to the requirement
    fwTokens = location.hub.firmwareVersionString.split("\\.")
    vTokens = vStr.split("\\.")
    if(fwTokens.size() != vTokens.size())
        return false
    rValue =  true
    for(i=0;i<vTokens.size();i++){
        if(vTokens[i].toInteger() < fwTokens[i].toInteger())
           i=vTokens.size()+1
        else
        if(vTokens[i].toInteger() > fwTokens[i].toInteger())
            rValue=false
    }
    return rValue
}

ZonedDateTime calculateSunrise(int year=new Date().getYear() + 1900, int month=new Date().getMonth() + 1, int day=new Date().getDate()) {
    final double ZENITH = 90.83333 // Official zenith for sunrise/sunset
    LocalDate date = LocalDate.of(year, month, day)
    double latitude = location.latitude
    double longitude = location.longitude
    int dayOfYear = date.dayOfYear
    
    double lngHour = longitude / 15
    double t = dayOfYear + ((6 - lngHour) / 24)

    // Mean anomaly
    double M = (0.9856 * t) - 3.289
    
    // Sun's true longitude
    double L = (M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(Math.toRadians(2 * M))) + 282.634) % 360

    // Right ascension
    double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))))
    RA = RA % 360

    // Adjust RA to be in the same quadrant as L
    double Lquadrant = (Math.floor(L / 90)) * 90
    double RAquadrant = (Math.floor(RA / 90)) * 90
    RA = RA + (Lquadrant - RAquadrant)

    // Convert RA into hours
    RA = RA / 15

    // Calculate declination of the sun
    double sinDec = 0.39782 * Math.sin(Math.toRadians(L))
    double cosDec = Math.cos(Math.asin(sinDec))

    // Calculate the sun's local hour angle
    double cosH = (Math.cos(Math.toRadians(ZENITH)) - (sinDec * Math.sin(Math.toRadians(latitude)))) / (cosDec * Math.cos(Math.toRadians(latitude)))

    if (cosH > 1) {
        return -1 //no sunrise at this location for this date 
    }

    // Calculate H and convert into hours
    double H = 360 - Math.toDegrees(Math.acos(cosH))
    H = H / 15

    // Calculate local mean time
    double T = H + RA - (0.06571 * t) - 6.622

    // Adjust time back to UTC
    double UT = (T - lngHour) % 24
    if (UT < 0) UT += 24
    
    // Convert UT to Local Time Zone
    ZoneId zone = ZoneId.systemDefault()
    ZonedDateTime utcTime = date.atTime((int) UT, (int) ((UT % 1) * 60)).atZone(ZoneId.of("UTC"))
    ZonedDateTime localTime = utcTime.withZoneSameInstant(zone)

    // Return the local sunrise time
    return localTime//.toLocalTime()
}
    
ZonedDateTime calculateSunset(int year=new Date().getYear() + 1900, int month=new Date().getMonth() + 1, int day=new Date().getDate()) {
    final double ZENITH = 90.83333 // Official zenith for sunrise/sunset
    day++
    
    try {
    	LocalDate.of(year,month,day)
    } catch (dCheck){
    	month++
        day = 1
        if(month > 12) 
            month = 1
    }
    
    LocalDate date=LocalDate.of(year,month,day)
    int dayOfYear = date.dayOfYear
    double latitude = location.latitude
    double longitude = location.longitude 

    // Approximate time in hours
    double lngHour = longitude / 15
    double t = dayOfYear + ((18 - lngHour) / 24)

    // Sun's mean anomaly
    double M = (0.9856 * t) - 3.289

    // Sun's true longitude
    double L = M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(Math.toRadians(2 * M))) + 282.634
    L = (L + 360) % 360

    // Sun's right ascension
    double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))))
    RA = (RA + 360) % 360

    // Right ascension value needs to be in the same quadrant as L
    double Lquadrant = (Math.floor(L / 90)) * 90
    double RAquadrant = (Math.floor(RA / 90)) * 90
    RA = RA + (Lquadrant - RAquadrant)

    // Convert RA into hours
    RA = RA / 15

    // Sun's declination
    double sinDec = 0.39782 * Math.sin(Math.toRadians(L))
    double cosDec = Math.cos(Math.asin(sinDec))

    // Sun's local hour angle
    double cosH = (Math.cos(Math.toRadians(ZENITH)) - (sinDec * Math.sin(Math.toRadians(latitude)))) / (cosDec * Math.cos(Math.toRadians(latitude)))
    if (cosH > 1) {
        return -1  // Sun never sets
    } else if (cosH < -1) {
        return -1  // Sun never rises
    }
    
    // H = local hour angle in degrees
    double H = Math.toDegrees(Math.acos(cosH)) / 15

    // Local mean time of sunset
    double T = H + RA - (0.06571 * t) - 6.622

    // Adjust back to UTC
    double UT = (T - lngHour) % 24
    if (UT < 0) UT += 24

    // Convert UT to Local Time Zone
    ZoneId zone = ZoneId.systemDefault()
    ZonedDateTime utcTime = date.atTime((int) UT, (int) ((UT % 1) * 60)).atZone(ZoneId.of("UTC"))
    ZonedDateTime localTime = utcTime.withZoneSameInstant(zone)

    // Return the local sunset time
    return localTime//.toLocalTime()
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

@Field static String cloudFontStyle = ''
@Field static String minFwVersion = "2.2.8.141"
@Field static List <String> pollList = ["0", "1", "2", "3", "4"]
@Field static prefList = [
[parm01:[desc:"CPU Temperature Polling", attributeList:"temperatureF, temperatureC, temperature", method:"cpuTemperatureReq"]],
[parm02:[desc:"Free Memory Polling", attributeList:"freeMemory,jvmSize,jvmFree", method:"freeMemoryReq"]],
[parm03:[desc:"CPU Load Polling", attributeList:"cpuLoad, cpuPct", method:"cpuLoadReq"]],
[parm04:[desc:"DB Size Polling", attributeList:"dbSize", method:"dbSizeReq"]],
[parm05:[desc:"Public IP Address", attributeList:"publicIP", method:"publicIpReq"]],
[parm06:[desc:"Max Event/State Days Setting", attributeList:"maxEvtDays,maxStateDays", method:"evtStateDaysReq"]], 
[parm07:[desc:"ZWave Version", attributeList:"zwaveVersion, zwaveSDKVersion", method:"zwaveVersionReq"]],
[parm08:[desc:"Time Sync Server Address", attributeList:"ntpServer", method:"ntpServerReq"]],
[parm09:[desc:"Additional Subnets", attributeList:"ipSubnetsAllowed", method:"ipSubnetsReq"]],
[parm10:[desc:"Hub Mesh Data", attributeList:"hubMeshData, hubMeshCount", method:"hubMeshReq"]],
[parm11:[desc:"Expanded Network Data", attributeList:"connectType (Ethernet, WiFi, Dual, Not Connected), connectCapable (Ethernet, WiFi, Dual), dnsServers, staticIPJson, lanIPAddr, wirelessIP, wifiNetwork, dnsStatus, lanSpeed", method:"extNetworkReq"]],
[parm12:[desc:"Check for Firmware Update",attributeList:"hubUpdateStatus, hubUpdateVersion",method:"updateCheckReq"]],
[parm13:[desc:"Z Status, Hub Alerts, Passive Cloud Check",attributeList:"hubAlerts,zwaveStatus, zigbeeStatus2, pCloud, zbHealthy, zwHealthy", method:"hub2DataReq"]],
[parm14:[desc:"Base Data",attributeList:"appStateCompression, firmwareVersionString, hardwareID, id, latitude, localIP, localSrvPortTCP, locationId, locationName, longitude, name, temperatureScale, timeZone, type, uptime, zigbeeChannel, zigbeeEui, zigbeeId, zigbeeStatus, zigbeePower, zigbeeUpdateAvail, zipCode",method:"baseData"]],
[parm15:[desc:"15 Minute Averages",attributeList:"cpu15Min, cpu15Pct, freeMem15", method:"fifteenMinute"]],
[parm16:[desc:"Active Cloud Connection Check",attributeList:"cloud", method:"checkCloud"]],
[parm17:[desc:"Matter Status (C-5 and > only)",attributeList:"matterEnabled, matterStatus", method:"checkMatter"]],
[parm18:[desc:"Restricted Access List",attributeList:"accessList", method:"checkAccess"]],
[parm19:[desc:"Hub Security Active",attributeList:"securityInUse", method:"checkSecurity"]],
[parm20:[desc:"Extended ZWave",attributeList:"zwaveUpdateAvail, zwaveJS, zwaveRegion", method:"extendedZwave"]]     
]    
@Field static String ttStyleStr = "<style>.tTip {display:inline-block;border-bottom: 1px dotted black;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;}</style>"
@Field sdfList = ["yyyy-MM-dd","yyyy-MM-dd HH:mm","yyyy-MM-dd h:mma","yyyy-MM-dd HH:mm:ss","ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "HH:mm", "H:mm","h:mma", "HH:mm:ss", "Milliseconds"]
