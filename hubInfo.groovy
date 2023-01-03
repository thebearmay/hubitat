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
 *                               2.4.4/5 restrict Zwave Version query to C7
 *    2021-06-17  thebearmay     2.4.8-10 - add MAC address and hub model, code cleanup, better compatibility check, zwaveVersion check override
 *    2021-06-17  thebearmay     freeMemPollEnabled was combined with the JVM/CPU polling when creating the HTML
 *    2021-06-19  thebearmay     fix the issue where on a driver update, if configure isn't a hubModel and macAddr weren't updated
 *    2021-06-29  thebearmay     2.2.8.x removes JVM data -> v2.5.0
 *    2021-06-30  thebearmay     clear the JVM attributes if >=2.2.8.0, merge pull request from nh.schottfam (stronger typing)
 *    2021-07-01  thebearmay     allow Warn level logging to be suppressed
 *    2021-07-02  thebearmay	    fix missing formatAttrib call
 *    2021-07-15  thebearmay     attribute clear fix
 *    2021-07-22  thebearmay     prep work for deleteCurrentState() with JVM attributes
 *                               use the getHubVersion() call for >=2.2.8.141 
 *    2021-07-23  thebearmay     add remUnused preference to remove all attributes that are not being polled 
 *    2021-08-03  thebearmay     put back in repoll on invalid zigbee channel
 *    2021-08-14  thebearmay     add html update from HIA
 *    2021-08-19  thebearmay     zwaveSDKVersion not in HTML
 *    2021-08-23  thebearmay     simplify unit retrieval
 *    2021-09-16  thebearmay     add localIP check into the polling cycle instead of one time check
 *    2021-09-29  thebearmay     suppress temperature event if negative
 *    2021-10-21  thebearmay     force a read against the database instead of cache when building html
 *    2021-11-02  thebearmay     add hubUpdateStatus
 *    2021-11-05  thebearmay     add hubUpdateVersion
 *    2021-11-09  thebearmay     add NTP Information
 *    2021-11-24  thebearmay     remove the hub update response attribute - release notes push it past the 1024 size limit.
 *    2021-12-01  thebearmay     add additional subnets information
 *    2021-12-07  thebearmay     allow data attribute to be suppressed if zigbee data is null, remove getMacAddress() as it has been retired from the API
 *    2021-12-08  thebearmay     fix zigbee channel bug
 *    2021-12-27  thebearmay     169.254.x.x reboot option
 *    2022-01-17  thebearmay     allow reboot to be called without Hub Monitor parameter
 *    2022-01-21  thebearmay     add Mode and HSM Status as a pollable attribute
 *    2022-03-03  thebearmay     look at attribute size each poll and enforce 1024 limit
 *    2022-03-09  thebearmay     fix lastUpdated not always updated
 *    2022-03-17  thebearmay     add zigbeeStatus
 *    2022-03-18  thebearmay     add zwaveStatus
 *    2022-03-23  thebearmay     code cleanup
 *    2022-03-27  thebearmay     fix zwaveStatus with hub security
 *    2022-03-28  thebearmay     add a try..catch around the zwaveStatus
 *    2022-05-17  thebearmay     enforce 1 decimal place for temperature
 *    2022-05-20  thebearmay     remove a check/force remove for hubUpdateResp
 *    2022-06-10  thebearmay     add hubAlerts, change source for zwaveStatus
 *    2022-06-20  thebearmay     trap login error
 *    2022-06-24  thebearmay     add hubMesh data
 *    2022-06-30  thebearmay     add shutdown command
 *    2022-08-11  thebearmay     add attribute update logging
 *    2022-08-15  thebearmay     add zigbeeStatus2 from the hub2 data
 *    2022-08-19  thebearmay     allow for a user defined HTML attribute using a file template
 *    2022-08-24  thebearmay     switch all HTML attribute processing to the template
 *    2022-09-18  thebearmay     add a security in use attribute
 *    2022-09-29  thebearmay     handle null or 'null' html template
 *	  2022-10-20  thebearmay	 add sunrise sunset
 *    2022-10-21  thebearmay     add format option for lastUpdated
 *    2022-10-25  thebearmay     handle a 408 in fileExists() 
 *    2022-10-26  thebearmay     fix a typo
 *    2022-10-28  thebearmay     add a couple of additional dateTime formats, add traps for null sdf selection
 *    2022-11-18  thebearmay     add an attribute to display next poll time, add checkPolling method instead of forcing a poll at startup
 *    2022-11-22  thebearmay     catch an error on checking poll times
 *	  2022-11-22  thebearmay	 correct stack overflow
 *    2022-11-23  thebearmay     change host for publicIP
 *    2022-11-25  thebearmay     log.warn instead of log.warning
 *    2022-12-09  thebearmay     fix timing issue with Next Poll Time
 *    2022-12-23  thebearmay     use the loopback address for shutdown and reboot
 *    2022-12-29  thebearmay     more hub2 data with HEv2.3.4.126
 *    2023-01-03  thebearmay     minor cosmetic fixes
*/
import java.text.SimpleDateFormat
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "2.7.22"}

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
        capability "Refresh"
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


    }   
}

preferences {
    input("quickref","href", title:"<a href='https://htmlpreview.github.io/?https://github.com/thebearmay/hubitat/blob/main/hubInfoQuickRef.html' target='_blank'>Quick Reference v${version()}</a>")
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("warnSuppress", "bool", title: "Suppress Warn Level Logging", width:4)
    input("tempPollEnable", "bool", title: "Enable Temperature Polling", width:4)
    input("freeMemPollEnabled", "bool", title: "Enable Free Memory Polling", width:4)
    input("cpuPollEnabled", "bool", title: "Enable CPU Load Polling", width:4)
    input("dbPollEnabled","bool", title: "Enable DB Size Polling", width:4)
    input("publicIPEnable", "bool", title: "Enable Querying the cloud \nto obtain your Public IP Address?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("evtStateDaysEnable", "bool", title:"Enable Display of Max Event/State Days Setting", width:4)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("alternateHtml", "string", title: "Template file for HTML attribute", submitOnChange: true, defaultValue: "hubInfoTemplate.res")
    input("checkZwVersion","bool",title:"Force Update of ZWave Version Attribute", defaultValue: false, submitOnChange: true, width:4)
    input("zwLocked", "bool", title: "Never Run ZWave Version Update", defaultValue:false, submitOnChange: true, width:4)
    input("ntpCkEnable","bool", title: "Check NTP Server on Poll", defaultValue:false,submitOnChange: true, width:4)
    input("subnetEnable", "bool", title: "Check for additional Subnets on Poll",defaultValue:false,submitOnChange: true, width:4)
    input("hubMeshPoll", "bool", title: "Include Hub Mesh Data", defaultValue:false, submitOnChange:true, width:4)
    input("extNetData", "bool", title: "Include Expanded Network Data", defaultValue: false, submitOnChange:true, width:4)
    input("sunSdfPref", "enum", title: "Date/Time Format for Sunrise/Sunset", options:sdfList, defaultValue:"HH:mm:ss", width:4)
    input("updSdfPref", "enum", title: "Date/Time Format for Last Updated", options:sdfList, defaultValue:"Milliseconds", width:4)
    input("upTimeSep", "string", title: "Separator for Formatted Uptime", defaultValue: ",", width:4)
    input("suppressData", "bool", title: "Suppress <i>data</i> attribute if Zigbee is null", defaultValue:false, submitOnChange: true, width:4)
	input("remUnused", "bool", title: "Remove unused attributes (Requires HE >= 2.2.8.141", defaultValue: false, submitOnChange: true, width:4)
    input("attrLogging", "bool", title: "Log all attribute changes", defaultValue: false, submitOnChange: true, width:4)
    input("allowReboot","bool", title: "Allow Hub to be shutdown or rebooted", defaultValue: false, submitOnChange: true, width:4)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
    if(pollingCheck())
        input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)    
    input("fwUpdatePollRate","number", title:"Poll rate (in seconds) for FW Update Check (Default:6000, Disable:0):", defaultValue:6000, submitOnChange:true, width:4)
    if(fwUpdatePollRate != null && fwUpdatePollRate.toInteger() > 0)
        input("rawUpdateNotice", "bool", title:"Use raw update notice", defaultValue:true, width:4, submitOnChange:true)
}

boolean pollingCheck(){
    if(tempPollEnable||freeMemPollEnabled||cpuPollEnabled||dbPollEnabled||publicIPEnable||evtStateDaysEnable||checkZwVersion||ntpCkEnable||subnetEnable||hubMeshPoll||extNetData)
        return true
    else
        return false
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
    initialize()
}

def initialize(){
    log.trace "Hub Information Driver ${version()} initialized"
    if (!security)  device.updateSetting("security",[value:"false",type:"bool"])
    
    // will additionally be checked before execution to determine if C-7 or above
    if(!zwLocked)
        device.updateSetting("checkZwVersion",[value:"true",type:"bool"])
    
    pollHub2()
    
    runIn(45,"configure")
    restartCheck() //set Restart Time using uptime and current timeatamp
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    if(pollingCheck()){
        unschedule("getPollValues")
        getPollValues()
    }else {
        unschedule("getPollValues")
        updateAttr("nextPoll","None")
    }
    if(fwUpdatePollRate == null) 
        device.updateSetting("fwUpdatePollRate",[value:6000,type:"number"])
    if(fwUpdatePollRate>0){
        unschedule("updateCheck")
        updateCheck()
    }
    if(warnSuppress == null) device.updateSetting("warnSuppress",[value:"false",type:"bool"])
    if (attribEnable)
        altHtml()
    else if(location.hub.firmwareVersionString < "2.2.8.141")
        sendEvent(name: "html", value: "<table></table>", isChanged: true)
		
	if(remUnused && location.hub.firmwareVersionString >= "2.2.8.141") {
		if(location.hub.firmwareVersionString >= "2.2.8.0") {
            device.deleteCurrentState("jvmFree")
            device.deleteCurrentState("jvmTotal")
            device.deleteCurrentState("jvmFreePct")
		}
		if(!tempPollEnable) {
		    device.deleteCurrentState("temperatureC")
			device.deleteCurrentState("temperatureF")
			device.deleteCurrentState("temperature")
		}
		if(!freeMemPollEnabled){
		    device.deleteCurrentState("freeMemory")
		}
		if(!cpuPollEnabled){
		    device.deleteCurrentState("cpu5Min")
			device.deleteCurrentState("cpuPct")
		}
		if(!dbPollEnabled){
		    device.deleteCurrentState("dbSize")
		}
		if(!publicIPEnable){
		    device.deleteCurrentState("publicIP")
		}
		if(!checkZwVersion){
		    device.deleteCurrentState("zwaveSDKVersion")
		    device.deleteCurrentState("zwaveVersion")
		}
		if(!attribEnable){
			device.deleteCurrentState("html")
		}
        if(!evtStateDaysEnable){
			device.deleteCurrentState("maxStateDays")
			device.deleteCurrentState("maxEvtDays")
        } 
        if(!ntpCkEnable){
			device.deleteCurrentState("ntpServer")
        }
        if(!subnetEnable){
            device.deleteCurrentState("ipSubnetsAllowed")
        }
        if(!hubMeshPoll){
            device.deleteCurrentState("hubMeshData")
            device.deleteCurrentState("hubMeshCount")
        }
        if(!extNetData) {
            device.deleteCurrentState("connectType")
            device.deleteCurrentState("dnsServers")
            device.deleteCurrentState("staticIPJson")
            device.deleteCurrentState("lanIPAddr")
            device.deleteCurrentState("dnsServers")
            device.deleteCurrentState("wirelessIP")
            device.deleteCurrentState("wifiNetwork")
        }
        device.deleteCurrentState("hubUpdateResp")
	}
    
    if(attribEnable && fileExists("$alternateHtml") != true){
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"hubInfoTemplate.res",type:"string"])
    }
				
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnable) log.debug "configure()"
    List locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    def myHub = location.hub
    List hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        if(hubProp[i] != "data")
            updateAttr(hubProp[i], myHub["${hubProp[i]}"])
        else if(location.hub.properties.data.zigbeeChannel != null || suppressData == false)
            updateAttr(hubProp[i], myHub["${hubProp[i]}"])
        else if(location.hub.firmwareVersionString >= "2.2.8.0") {
            device.deleteCurrentState("data")
            device.deleteCurrentState("zigbeeChannel")
        }
    }
    for(i=0;i<locProp.size();i++){
        updateAttr(locProp[i], location["${locProp[i]}"])
    }
    if(!suppressData || location.hub.properties.data.zigbeeChannel != null)
        updateAttr("zigbeeChannel",location.hub.properties.data.zigbeeChannel)
    if(location.hub.properties.data.zigbeeChannel != null)
        updateAttr("zigbeeStatus", "enabled")
    else
        updateAttr("zigbeeStatus", "disabled")
    
    formatUptime()
    updateAttr("hubVersion", location.hub.firmwareVersionString) //retained for backwards compatibility
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)

    updateAttr("hubModel", getModel())
    if(updSdfPref == null) device.updateSetting("updSdfPref",[value:"Milliseconds",type:"string"])
    if(updSdfPref == "Milliseconds" || updSdfPref == null) 
        updateAttr("lastUpdated", new Date().getTime())
    else {
        SimpleDateFormat sdf = new SimpleDateFormat(updSdfPref)
        updateAttr("lastUpdated", sdf.format(new Date().getTime()))
    }

    
    if (pollingCheck()) 
        checkPolling()
    else 
        updateAttr("nextPoll","None")
    if (attribEnable) altHtml()
    if(fwUpdatePollRate == null) 
        device.updateSetting("fwUpdatePollRate",[value:6000,type:"number"])
    if(fwUpdatePollRate > 0 ) updateCheck()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.length() > 1024) {
        log.error "Attribute value for $aKey exceeds 1024, current size = ${aValue.length()}, truncating to 1024..."
        aValue = aValue.substring(0,1023)
    }
    sendEvent(name:aKey, value:aValue, unit:aUnit)
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

void formatUptime(){
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
        String attrval = "${days.toString()}d$upTimeSep${hrs.toString()}h$upTimeSep${min.toString()}m$upTimeSep${sec.toString()}s"
        updateAttr("formattedUptime", attrval) 
    } catch(ignore) {
        updateAttr("formattedUptime", "")
    }
}

String getModel(){
    try{
        String model = getHubVersion() // requires >=2.2.8.141
    } catch (ignore){
        try{
            httpGet("http://${location.hub.localIP}:8080/api/hubitat.xml") { res ->
                model = res.data.device.modelName
            return model
            }        
        } catch(ignore_again) {
            return ""
        }
    }
}

void checkZigStack(){
    if(!beta)
        return
    try{
        httpGet("http://127.0.0.1:8080/hub/currentZigbeeStack") { resp ->
            if(resp.data.toString().indexOf('standard') > -1)
                updateAttr("zigbeeStack","standard")
            else
                updateAttr("zigbeeStack","new")
            }       
       } catch(ignore) { }
}

boolean isCompatible(Integer minLevel) { //check to see if the hub version meets the minimum requirement
    String model = device.currentValue("hubModel",true)
    if(!model){
        model = getModel()
        updateAttr("hubModel", model)
    }
    String[] tokens = model.split('-')
    String revision = tokens.last()
    return (Integer.parseInt(revision) >= minLevel)

}

void pollHub2() {
        Map params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub2/hubData"        
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getHub2", params)
}    

void refresh() {
    getPollValues()
}

void getPollValues(){

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
	
    String cookie=(String)null
    if(security) cookie = getCookie()
    if(debugEnable) log.debug "Cookie = $cookie"

    // repoll zigbee channel if invalid
	
    if (device.currentValue("zigbeeChannel") == "NA") { 
        updateAttr("zigbeeChannel",location.hub.properties.data.zigbeeChannel)
    }


    if(location.hub.properties.data.zigbeeChannel != null)
        updateAttr("zigbeeStatus", "enabled")
    else
        updateAttr("zigbeeStatus", "disabled")
        
 
    //verify localIP in case of change
    updateAttr("localIP", location.hub.localIP)
    
    //Hub Mode & HSM Status
    updateAttr("currentMode", location.properties.currentMode)
    updateAttr("currentHsmMode", location.hsmStatus)
    
    // Zwave Version
    if(checkZwVersion == null && isCompatible(7))
        device.updateSetting("checkZwVersion",[value:"true",type:"bool"])
    else if(checkZwVersion == null)
        device.updateSetting("checkZwVersion",[value:"false",type:"bool"])
    if(zwLocked == null) device.updateSetting("zwLocked",[value:"false",type:"bool"])

    if(checkZwVersion && isCompatible(7) && !zwLocked){
        Map paramZ = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveVersion",
            headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug paramZ
        asynchttpGet("getZwave", paramZ)
    } else if (checkZwVersion) {
        device.updateSetting("checkZwVersion",[value:"false",type:"bool"])
        updateAttr("zwaveData",null)
    }
    //Zwave Status - enabled/disabled & hubAlerts
    pollHub2()

    // get Temperature
    if(tempPollEnable) {
        params = [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/internalTempCelsius",
                headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug params
        asynchttpGet("getTempHandler", params)
    }

    // get Free Memory
    if(freeMemPollEnabled) {
        params = [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/freeOSMemory",
                headers: ["Cookie": cookie]
        ]
        if (debugEnable) log.debug params
        asynchttpGet("getFreeMemHandler", params)
    }
    
    // get Free JVM & CPU
    if(cpuPollEnabled) {
        if (location.hub.firmwareVersionString <= "2.2.5.131") {
            params = [
                    uri    : "http://127.0.0.1:8080",
                    path   : "/hub/advanced/freeOSMemoryHistory",
                    headers: ["Cookie": cookie]
            ]
        } else {
            params = [
                    uri    : "http://127.0.0.1:8080",
                    path   : "/hub/advanced/freeOSMemoryLast",
                    headers: ["Cookie": cookie]
            ]
        }
        if (debugEnable) log.debug params
        asynchttpGet("getJvmHandler", params)
    }

    //Get DB size
    if(dbPollEnabled) {
        params = [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/databaseSize",
                headers: ["Cookie": cookie]
        ]

        if (debugEnable) log.debug params
        asynchttpGet("getDbHandler", params)
    }

    //get Public IP 
    if(publicIPEnable) {
        params =
        [
            uri: "https://api.ipify.org?format=json",
            headers: [            
                   Accept: "application/json"
            ]
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getIfHandler", params)
    }
 
    //Max State Days
    if(evtStateDaysEnable) {
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/maxDeviceStateAgeDays",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getStateDaysHandler", params)
     
     //Max Event Days
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/maxEventAgeDays",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getEvtDaysHandler", params)
     
     
    } 
    
    // NTP Server
    if(ntpCkEnable){
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/advanced/ntpServer",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getNtpServer", params)
    }
    // Additional Subnets 
    if(subnetEnable) {
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub/allowSubnets",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getSubnets", params)    
    
    }
    
    //HubMesh Data
    if(hubMeshPoll) {
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub2/hubMeshJson",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getHubMesh", params)
    }
    
    //v2.3.4.126 data
    if(location.hub.firmwareVersionString >= "2.3.4.126" && extNetData){
        params =
        [
                uri    : "http://127.0.0.1:8080",
                path   : "/hub2/networkConfiguration",
                headers: ["Cookie": cookie]           
        ]
    
        if(debugEnable)log.debug params
        asynchttpGet("getNetworkConfig", params)        
        
    }
    
    //End Pollable Gets
	
    if(!suppressData || location.hub.properties.data.zigbeeChannel != null)
        updateAttr("zigbeeChannel",location.hub.properties.data.zigbeeChannel) 
		
    updateAttr("uptime", location.hub.uptime)
    formatUptime()
	
    if (attribEnable) altHtml()
    
    if (debugEnable) log.debug "tempPollRate: $tempPollRate"
    if(location.hub.firmwareVersionString >= "2.3.3.120") checkZigStack()
    if (pollingCheck()) {
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,"getPollValues")
        }else {
            runIn(tempPollRate,"getPollValues")
        }
    }
    
    getNextPoll()
    
    if(updSdfPref == null) device.updateSetting("updSdfPref",[value:"Milliseconds",type:"string"])
    if(updSdfPref == "Milliseconds") 
        updateAttr("lastUpdated", new Date().getTime())
    else {
        SimpleDateFormat sdf = new SimpleDateFormat(updSdfPref)
        updateAttr("lastUpdated", sdf.format(new Date().getTime()))
    }
    
}
@SuppressWarnings('unused')
def getTemp(){  // this is to handle the upgrade path from >= 1.8.x
    log.info "Upgrading HubInfo polling from 1.8.x"
    unschedule("getTemp")
    getPollValues()
}

@SuppressWarnings('unused')
void getTempHandler(resp, data) {
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

@SuppressWarnings('unused')
void getZwave(resp, data) {
    try {
        if(resp.getStatus() == 200 || resp.getStatus() == 207) {
            String zwaveData = resp.data.toString()
            if(debugEnable) log.debug resp.data.toString()
            if(zwaveData.length() < 1024){
                updateAttr("zwaveData",zwaveData)
                parseZwave(zwaveData)
                device.updateSetting("checkZwVersion",[value:"false",type:"bool"])
            }
            else if (!warnSuppress) log.warn "Invalid data returned for Zwave, length = ${zwaveData.length()} will retry"
        }
    } catch(ignored) {
        if (!warnSuppress) log.warn "getZwave Parsing Error"    
    }
 
    
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
        if (!warnSuppress) log.warn "getFreeMem httpResp = $respStatus but returned invalid data, will retry next cycle"    
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
        if (attribEnable) runIn(5,"altHtml") //allow for events to register before updating - thebearmay 210308
    } catch(ignored) {
        def respStatus = resp.getStatus()
        if (!warnSuppress) log.warn "getJvm httpResp = $respStatus but returned invalid data, will retry next cycle"    
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
            if(location.hub.firmwareVersionString <= "2.2.8.0"){
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
            } else {
                Double cpuWork=jvmArr[2].toDouble()
                updateAttr("cpu5Min",cpuWork.round(2))
                cpuWork = (cpuWork/4.0D)*100.0D //Load / #Cores - if cores change will need adjusted to reflect
                updateAttr("cpuPct",cpuWork.round(2),"%")
                if(device.currentValue("jvmFree")) {
                    try { // requires >= 2.2.8.141
                        device.deleteCurrentState("jvmFree")
                        device.deleteCurrentState("jvmTotal")
                        device.deleteCurrentState("jvmFreePct")
                    } catch (ignore) {
                        updateAttr("jvmFree","\0")
                        updateAttr("jvmTotal","\0")
                        updateAttr("jvmFreePct","\0") 
                    } 
                }         
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
        if (!warnSuppress) log.warn "getDb httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
}

@SuppressWarnings('unused')
void getIfHandler(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) 
                log.debug resp.data
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
        if (!warnSuppress) log.warn "getStateDays httpResp = $respStatus but returned invalid data, will retry next cycle"
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
        if (!warnSuppress) log.warn "getEvtDays httpResp = $respStatus but returned invalid data, will retry next cycle"
    } 
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

@SuppressWarnings('unused')
void parseZwave(String zString){
    Integer start = zString.indexOf('(')
    Integer end = zString.length()
    String wrkStr
    
    if(start == -1 || end < 1 || zString.indexOf("starting up") > 0 ){ //empty or invalid string - possibly non-C7
        updateAttr("zwaveData",null)
    }else {
        wrkStr = zString.substring(start,end)
        wrkStr = wrkStr.replace("(","[")
        wrkStr = wrkStr.replace(")","]")

        HashMap zMap = (HashMap)evaluate(wrkStr)
        
        updateAttr("zwaveSDKVersion","${((List)zMap.targetVersions)[0].version}.${((List)zMap.targetVersions)[0].subVersion}")
        updateAttr("zwaveVersion","${zMap?.firmware0Version}.${zMap?.firmware0SubVersion}")
    }
}

String getUnitFromState(String attrName){
   	return device.currentState(attrName)?.unit
}

void restartCheck() {
    if(debugEnable) log.debug "$rsDate"
    Long ut = new Date().getTime().toLong() - (location.hub.uptime.toLong()*1000)
    Date upDate = new Date(ut)
    if(debugEnable) log.debug "RS: $rsDate  UT:$ut  upTime Date: $upDate   upTime: ${location.hub.uptime}"
    
    updateAttr("lastHubRestart", ut)
    SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(upDate))
}

@SuppressWarnings('unused')
void getHub2(resp, data){
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
                if (device.currentValue("zigbeeStatus", true) != "enabled") log.warn "Zigbee Status has opposing values - may have crashed."
            } else {
                updateAttr("zigbeeStatus2", "disabled")
                if (device.currentValue("zigbeeStatus", true) != "disabled") log.warn "Zigbee Status has opposing values - may have crashed."
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

@SuppressWarnings('unused')
void getNetworkConfig(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
            if(!h2Data.usingStaticIP)
                updateAttr("staticIPJson", "[]")
            else {
                jMap = [staticIP:"${h2Data.staticIP}", staticGateway:"${h2Data.staticGateway}", staticSubnetMask:"${h2Data.staticSubnetMask}",staticNameServers:"${h2Data.staticNameServers}"]
                updateAttr("staticIPJson",JsonOutput.toJson(jMap))
            }
            if(h2Data.hasEthernet && h2Data.hasWiFi ){
                updateAttr("connectType","Dual")
                updateAttr("lanIPAddr", h2Data.lanAddr)    
            } else if(h2Data.hasEthernet){
                updateAttr("connectType","Ethernet")
                updateAttr("lanIPAddr", h2Data.lanAddr)
            } else if(h2Data.hasWiFi)
                updateAttr("connectType","WiFi")
            if(h2Data.hasWiFi){
                updateAttr("wifiNetwork", h2Data.wifiNetwork)
                updateAttr("wirelessIP",h2Data.wlanAddr)
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

@SuppressWarnings('unused')
void updateCheck(){
	if(security) cookie = getCookie()
    if(fwUpdatePollRate == 0) {
        unschedule("updateCheck")
        return
    }
    params = [
            uri: "http://${location.hub.localIP}:8080",
            path:"/hub/cloud/checkForUpdate",
            timeout: 10,
			headers:[
				"Cookie": cookie
			]
        ]
   asynchttpGet("updChkCallback", params)
   runIn(fwUpdatePollRate,"updateCheck")
}

@SuppressWarnings('unused')
void updChkCallback(resp, data) {
    if(rawUpdateNotice == null){
        rawUpdateNotce = true
        device.updateSetting("rawUpdateNotice",[value:"true",type:"bool"])
    }
    try {
        if (resp.status == 200) {
            def jSlurp = new JsonSlurper()
            Map resMap = (Map)jSlurp.parseText((String)resp.data)
            if(rawUpdateNotice) 
                updateAttr("hubUpdateStatus",resMap.status)
            else if(resMap.status == "NO_UPDATE_AVAILABLE")
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

@SuppressWarnings('unused')
void hiaUpdate(htmlStr) {
	updateAttr("html",htmlStr)
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
void altHtml(){
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
@Field beta = false
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
                updateAttr("exist","true")
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
void checkPolling(){
    if(debubEnable)log.debug "checkPolling"
	getNextPoll()
    nextPoll = device.currentValue('nextPoll',true)
	sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")
    if(nextPoll != null)
	    Long nextPollTime = sdf.parse("${nextPoll}").getTime()
    else
        nextPollTime = 0
	if(nextPollTime > new Date().getTime())
		pollFound = true
	if(!pollFound && pollingCheck())
        getPollValues()

}

@SuppressWarnings('unused')
void getNextPoll(){
    pollFound = false
    if(security) cookie = getCookie()    
    params = [
                uri: "http://127.0.0.1:8080",
                path: "/logs/json",
                headers: [
				    "Cookie": cookie
                ]
            ]
    
    try{
     httpGet(params) { resp ->
        if(debubEnable)log.debug resp.properties
        mapData = (HashMap) resp.data
        myJobs = [:]
        devID = device.getId().toString()
        if(debubEnable)log.debug "${mapData.jobs}"
        mapData.jobs.each {
            if(it.link.contains('/device/edit') && it.link.endsWith(devID)){
                if("$it.methodName" == 'getPollValues'){
                    updateAttr("nextPoll",it.nextRun)
                    pollFound = true
                }
            }
        }

     }
    } catch (ignore) {
    }
}


@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

@Field sdfList = ["yyyy-MM-dd","yyyy-MM-dd HH:mm","yyyy-MM-dd h:mma","yyyy-MM-dd HH:mm:ss","ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "HH:mm", "H:mm","h:mma", "HH:mm:ss", "Milliseconds"]
