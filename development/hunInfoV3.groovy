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


    }   
}
preferences {
    input("quickref","href", title:"<style>.tooltip {display:inline-block;border-bottom: 1px dotted black;.tooltip .tooltiptext {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tooltip:hover .tooltiptext {
display:inline-block;background-color:yellow;color:black;}</style><a href='https://htmlpreview.github.io/?https://github.com/thebearmay/hubitat/blob/main/hubInfoQuickRef.html' target='_blank'>Quick Reference v${version()}</a>")
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("warnSuppress", "bool", title: "Suppress Warn Level Logging", width:4)
	prefList.each {
		input ("${it.key}", "ENUM", title: "<span class='tooltip'>${it.desc}<span class='tooltiptext'>${it.attributes}</span></span>", options:pollList, submitOnChange:true, width:4)
	}
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("alternateHtml", "string", title: "Template file for HTML attribute", submitOnChange: true, defaultValue: "hubInfoTemplate.res", width:4)
	input("remUnused", "bool", title: "Remove unused attributes (Requires HE >= 2.2.8.141", defaultValue: false, submitOnChange: true, width:4)
    input("attrLogging", "bool", title: "Log all attribute changes", defaultValue: false, submitOnChange: true, width:4)
    input("allowReboot","bool", title: "Allow Hub to be shutdown or rebooted", defaultValue: false, submitOnChange: true, width:4)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
	input("pollRate1", "number", title: "Poll Rate 1", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate2", "number", title: "Poll Rate 2", defaultValue:0, submitOnChange: true, width:4) 
	input("pollRate3", "number", title: "Poll Rate 3", defaultValue:0, submitOnChange: true, width:4) 
}
@SuppressWarnings('unused')
void installed() {
    log.trace "installed()"
    xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
    initialize()
}

void updated(){
	unschedule()
	state.poll1 = []
	state.poll2 = []
	state.poll3 = []
	prefList.each{
		if(settings["${it.key}"] != null)
			state["poll${it.key}"].add("${it.method}")
	}
	if(pollRate1 > 0)
		runIn(pollRate1, "poll1")
	if(pollRate2 > 0)
		runIn(pollRate2, "poll2")
	if(pollRate3 > 0)
		runIn(pollRate3, "poll3")		
}

void poll1(){
	state.poll1.each{
		this."$it".call()
	}
	if(pollRate1 > 0)
		runIn(pollRate1, "poll1")
}

void poll2(){
	state.poll2.each{
		this."$it".call()
	}
	if(pollRate2 > 0)
		runIn(pollRate2, "poll2")
}

void poll3(){
	state.poll3.each{
		this."$it".call()
	}
	if(pollRate3 > 0)
		runIn(pollRate3, "poll3")	
}


void cpuTemperatureReq(){}
void freeMemoryReq(){}
void cpuLoad(){}
void dbSizeReq(){}
void publicIpReq(){}
void evtStateDaysReq(){}
void zwaveVersionReq(){}
void ntpServerReq(){}
void ipSubnetsReq(){}
void hubMeshReq(){}
void exNetworkReq(){}
void updateCheckReq(){}
	
@Field static prefList = [[parm01:[desc:"CPU Temperature Polling", attributeList:"temperatureF, temperatureC, temperature", method:"cpuTemperatureReq"],
[parm02:[desc:"Free Memory Polling", attributeList:"freeMemory", method:"freeMemoryReq"],
[parm03:[desc:"CPU Load Polling", attributeList:"cpuLoad, cpuPct", method:"cpuLoad"],
[parm04:[desc:"DB Size Polling", attributeList:"dbSize", method:"dbSizeReq"],
[parm05:[desc:"Public IP Address", attributeList:"publicIP", method:"publicIpReq"],
[parm06:[desc:"Max Event/State Days Setting", attributeList:"maxEvtDays,maxStateDays", method:"evtStateDaysReq"] 
[parm07:[desc:"ZWave Version", attributeList:"zwaveVersion, zwaveSDKVersion", method:"zwaveVersionReq"],
[parm08:[desc:"Time Sync Server Address", attributeList:"ntpServer", method:"ntpServerReq"]
[parm09:[desc:"Additional Subnets", attributeList:"ipSubnetsAllowed", method:"ipSubnetsReq"],
[parm10:[desc:"Hub Mesh Data", attributeList:"hubMeshData, hubMeshCount", method:"hubMeshReq"],
[parm11:[desc:"Expanded Network Data", attributeList:"connectType (Ethernet, WiFi, Dual), dnsServers, staticIPJson, lanIPAddr, wirelessIP, wifiNetwork"], method:"exNetworkReq"],
[parm12:[desc:"Check for Firmware Update",attributeList:"hubUpdateStatus, hubUpdateVersion",method:"updateCheckReq"]]

@Field static List <String> pollList = ["0", "1", "2", "3"]
