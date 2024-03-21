/*
 * Power Outage Manager 
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
 *    Date         Who           What
 *    ----         ---           ----
 *    07Jan2023    thebearmay    v0.1.3 - Add trigger device refresh option on system restart
 *                               v0.1.4 - Fix Security Prompt
 *    18Jan2023                  v0.1.5 - Add presence sensors as trigger
 *    21Feb2023                  v0.2.0 - Add device on/off capabilities
 *                                        Add RM interface 
 *    05Mar2023                  v0.2.1 - Fix typo
 *    20Mar2024                  v0.2.2 - unschedule pending actions if power is restored
 *                                        update Apps List logic to reflect new UI
 *    20Mar2024                  v0.2.3 - Add PowerMeter with healthStatus offline/online
 * 				 v0.2.4 - handle null queue selections
*/

import hubitat.helper.RMUtils
static String version()	{  return '0.2.4' }

definition (
	name: 			"Power Outage Manager", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides an interface to define actions to take when power goes down and when it is later restored.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/powOutMgr.groovy",
    installOnOpen:  true,
	oauth: 			false,
    singleThreaded: false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "outAction"
    page name: "upAction"
}


def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Power Outage Manager</h2><p style='font-size:small;color:navy'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
            section("<h3>Main</h3>"){
                
                input "triggerDevs", "capability.powerSource,capability.presenceSensor,capability.powerMeter", title:"Devices with PowerSource/Presence to act as Triggers", submitOnChange:true, multiple:true
                if(triggerDevs != null) {
                    unsubscribe()
                    state.onMains=[:]
                    state.onBattery=[:]
                    triggerDevs.each{
                       if(it.hasCapability("PowerSource"))
                            subscribe(it, "powerSource", "triggerOccurrence")
                       else if(it.hasCapability("PresenceSensor"))
                            subscribe(it, "presence", "triggerOccurrence")
                       else if(it.hasCapability("PowerMeter"))
                            subscribe(it, "healthStatus", "triggerOccurrence")
                    }
                    subscribe(location, "systemStart", "systemStartCheck")
                    pollDevices()
                    paragraph "<b>On Mains:</b> ${state.onMains.size()} <b>On Battery:</b> ${state.onBattery.size()}"
                } else {
                    unsubscribe()
                    state.onMains = [:]
                    state.onBattery = [:]
                }
                input "triggerDelay", "number", title:"<b>Number of minutes to delay before taking action</b>", defaultValue:0, width:3, submitOnChange:true
                input "agreement", "number", title: "<b>Number of devices that must agree before taking action</b>", defaultValue:1, width:3, submitOnChange:true
                if(agreement != null && (triggerDevs == null || agreement.toInteger() > triggerDevs.size())){
                    paragraph "<span style='font-weight:bold;background-color:red'>Agreement count ($agreement) exceeds number of devices (${triggerDevs==null ? 0:triggerDevs.size()}) - trigger will never occur</span>"
                    state.outage = false
                }
                input "refreshOnStart", "bool", title: "<b>Refresh Trigger Devices on System Start</b>", width:3, submitOnChange:true
                input "notifyDev", "capability.notification", title: "Send notifications to", submitOnChange:true, multiple:true
                input "notifyMsgOut", "text", title: "<b>Notification Message - Power Out</b>", defaultValue: "${app.getLabel()} - Power Outage Detected", submitOnChange:true
                input "notifyMsgUp", "text", title: "<b>Notification Message - Power Restored</b>", defaultValue: "${app.getLabel()} - Power Restored", submitOnChange:true
                
                href "outAction", title: "<b>Power Outage Actions</b>", required: false, width:6, submitOnChange:true
                href "upAction", title: "<b>Power Restored Actions</b>", required: false, width:6, submitOnChange:true
                input "debugEnabled", "bool", title: "<b>Turn on Debug Logging</b>", submitOnChange:true
                input("security", "bool", title: "<b>Hub Security Enabled</b>", defaultValue: false, submitOnChange: true)
                if (security) { 
                    input("username", "text", title: "<b>Hub Security Username</b>", required: false, submitOnChange: true)
                    input("password", "password", title: "<b>Hub Security Password</b>", required: false, submitOnChange: true)
                    if(username != null && password != null){
                        login = getCookie()
                        if(login.cookie != null)
                            paragraph "Login successful: ${login.result}\n${login.cookie}"
                    }
                }
                if(debugEnabled) runIn(1800,"logsOff") 
                else unschedule("logsOff")
            }

            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            }            
        }
    }
}

def outAction(){
    dynamicPage (name: "outAction", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Power Outage Actions</h2><p style='font-size:small;color:navy'>v${version()}</p>", install: false, uninstall: false) {
        section(title:"<h3>General Information<h3>",hideable:true,hidden:true){
            paragraph "<p>Outage actions have 3 Outage Response Queues available. These queues allow for the desired actions to staggered to accommodate an ongoing outage. For example, when the outage is detected you may want to disable integrations that no longer are available.  Later you may want to disable other apps, or turn off the  radios, and if theoutage goes on long enough, you may want to shutdown the hub gracefully before the UPS runs out of power.</p><p>First step is to decide what your checkpoints are (how many minutes before taking each set of actions) and enter those. Then go to the bottom of the screen and assign actions to the queues - if you don't want to take an action noted, set its queue number to zero.  If you chose to disable apps, assigning the action to a queue will give you the option to select All apps or individal ones.</p>"
        }
        section("<h3>Queue Timers</h3>"){
            input "oaDelay1", "number", title:"<b>Minutes before executing actions selected for Outage Response Queue 1</b>", submitOnChange:true, width:4
            input "oaDelay2", "number", title:"<b>Minutes before executing actions selected for Outage Response Queue 2</b>", submitOnChange:true, width:4
            input "oaDelay3", "number", title:"<b>Minutes before executing actions selected for Outage Repsonse Queue 3</b>", submitOnChange:true, width:4
            
        }
        section ("<h3>Queue Actions</h3>"){
            paragraph "<b>Assign each of the below to a Response Queue, items assigned to Queue 0 will not be scheduled</b>"
            
            appsList = [0:"All"]
            getAppsList().each{
                appsList["$it.id"]=it.title
            }

            
            input "zbDisable", "enum", title: "Turn off the ZigBee Radio", options: [0,1,2,3], submitOnChange:true, width:4
            input "zwDisable", "enum", title: "Turn off the ZWave Radio", options: [0,1,2,3], submitOnChange:true, width:4
            input "appDisable", "enum", title: "Disable Rules/Apps", options: [0,1,2,3], submitOnChange:true, width:4
            if(appDisable == "0" || appDisable == null){
                app.updateSetting("appDisableList",[value:"",type:"enum"])
            }else {
                input "appDisableList", "enum", title: "Select Rules/Apps", options: appsList, multiple:true, width:4, submitOnChange:true
            }
            input "turnOffDevs","enum", title: "Turn off Devices", options: [0,1,2,3], submitOnChange:true, width:4
            if(turnOffDevs == "0" || turnOffDevs == null){
                app.updateSetting("turnOffDevsList",[value:"-1",type:"capability.switch"])
                app.updateSetting("turnOnDevsList1",[value:"-1",type:"capability.switch"])
                app.updateSetting("turnOnDevsList2",[value:"-1",type:"capability.switch"])
            }else {
                input "turnOffDevsList", "capability.switch", title: "Devices to turn off", multiple: true, width:4, submitOnChange:true
            }
            input "rmRuleO", "enum", title:"Run a RM Rule", options: [0,1,2,3], submitOnChange:true,width:4
            if(rmRuleO) {
                rList = RMUtils.getRuleList("5.0")
                input "rmRuleOList", "enum", title: "Rule(s) to run", options: rList, submitOnChange:true, width:4, multiple:true
            }
            input "rebootHubO", "enum", title: "Reboot the hub", options: [0,1,2,3], submitOnChange:true, width:4
            input "shutdownHub", "enum", title: "Shutdown the hub", options: [0,1,2,3], submitOnChange:true, width:4
        }           
    }
}

def upAction(){
    dynamicPage (name: "upAction", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Power Restore Actions</h2><p style='font-size:small;color:navy'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3></h3>"){
            input "zbEnable", "bool", title: "<b>Turn on the ZigBee Radio</b>", submitOnChange:true, width:4
            input "zwEnable", "bool", title: "<b>Turn on the ZWave Radio</b>", submitOnChange:true, width:4
            input "appEnable", "bool", title: "<b>Enable all Rules/Apps</b>", submitOnChange:true, width:4
            input "rebootHub", "bool", title: "<b>Reboot the hub (2 minutes after trigger delay)</b>", submitOnChange:true, width:4
            if(turnOffDevs != "0" && turnOffDevs != null){
                if(turnOnDevsList1 == null){
                    app.updateSetting("turnOnDevsList1",[value:turnOffDevsList,type:"capability.switch"])
                    app.updateSetting("onDelay1",[value:0,type:"number"])
                }
                if(turnOnDevsList2 == null){
                    app.updateSetting("turnOnDevsList2",[value:turnOffDevsList,type:"capability.switch"])
                    app.updateSetting("onDelay2",[value:0,type:"number"])
                }
                input("onDelay1", "number", title:"<b>Delay in minutes before turning on first set of devices, zero to disable</b>", submitOnChange:true, width:4)
                if(onDelay1)
                    input("turnOnDevsList1", "capability.switch",title:"<b>First set of Devices to Turn On</b>", constraints:turnOffDevsList, multiple:true, submitOnChange:true, width:4)
                input("onDelay2", "number", title:"<b>Delay in minutes before turning on second set of devices, zero to disable</b>", submitOnChange:true, width:4)
                if(onDelay2)
                    input("turnOnDevsList2", "capability.switch",title:"<b>Second set of Devices to Turn On</b>", multiple:true, submitOnChange:true, width:4)
            }
            input "rmRuleR", "number", title:"<b>Delay in minutes before running RM Rule(s), zero to disable", submitOnChange:true,width:4
            if(rmRuleR) {
                rList = RMUtils.getRuleList("5.0")
                input "rmRuleRList", "enum", title: "Rule(s) to run", options: rList, submitOnChange:true, multiple:true
            }
        }           
    }
}

void triggerOccurrence(evt){
    if(debugEnabled) log.debug "Time: ${evt.unixTime} Device: ${evt.deviceId}:${evt.displayName} Value: ${evt.value}"
    if(state.onMains == null) state.onMains = [:]  
    if(state.onBattery == null) state.onBattery = [:]
    
    if(evt.value.toString().trim() == "battery" || evt.value.toString().trim() == "not present" || evt.value.toString().trim() == "offline") {
        state.onBattery["dev${evt.deviceId}"] = true
        mainsTemp = [:]
        state.onMains.each{
            if(it.key != "dev${evt.deviceId}")
                mainsTemp[it.key] = state.onMains[it.key]
        }
        state.onMains = mainsTemp       
        if(debugEnabled) log.debug "${state.onMains} <br> ${state.onBattery}"
        if(state.onBattery.size() >= agreement) startOutActions()
    } else if(evt.value.toString().trim() == "mains" || evt.value.toString().trim() == "present"|| evt.value.toString().trim() == "online") {
        state.onMains["dev${evt.deviceId}"] = true
        batteryTemp = [:]
        state.onBattery.each{
            if(it.key != "dev${evt.deviceId}")
                batteryTemp[it.key] = state.onBattery[it.key]
        }
        state.onBattery = batteryTemp
           
        if(debugEnabled) log.debug "${state.onMains} <br> ${state.onBattery}"
        if(state.onMains.size() >= agreement) startUpActions()
    }
}

void systemStartCheck(evt){
    unschedule("reboot")
    unschedule("shutdown")
    if(refreshOnStart){
        refreshTriggers()
        pauseExecution(3000)
    }
    pollDevices() // verify the powerSource value in case it changed
    if(state.onBattery.size() >= agreement) startOutActions() // should only occur if something triggered a reboot during the outage or an outage occurred during reboot
    
}

void refreshTriggers(){
    triggerDevs.each { dev ->
        if(dev.hasCommand("refresh"))
            dev.refresh()
    }    
}

void pollDevices(){
    if(state.onMains == null) state.onMains = [:]  
    if(state.onBattery == null) state.onBattery = [:]
    triggerDevs.each { dev ->
        if(debugEnabled) log.debug dev.currentValue("powerSource")
        if(dev.currentValue("powerSource") == "mains" || dev.currentValue("presence") == "present"|| dev.currentValue.toString().trim() == "online"){
                state.onMains["dev${dev.id}"] = true
                batteryTemp = [:]
                state.onBattery.each{
                    if(it.key != "dev${dev.id}")
                        batteryTemp[it.key] = state.onBattery[it.key]
                }
                state.onBattery = batteryTemp
        } else if(dev.currentValue("powerSource") == "battery" || dev.currentValue("presence") == "not present" || dev.currentValue.toString().trim() == "offline"){
                state.onBattery["dev${dev.id}"] = true
                mainsTemp = [:]           
                state.onMains.each{
                    if(debugEnabled) log.debug "${dev.id} ${it.key}"
                    if(it.key != "dev${dev.id}"){
                        mainsTemp[it.key] = state.onMains[it.key]
                    }
                }
                state.onMains = mainsTemp
        }
        
    }
}

void startOutActions(){
    if(state.outage == true) return// already processed
    state.outage = true
    runIn(triggerDelay.toInteger()*60, "startOutage")
}

void startOutage(){
    if(!state.outage) return // check if conditions have changed to recovered
    notifyDev.each { 
      it.deviceNotification(notifyMsgOut)  
    }     
    delayList = []
    if(oaDelay1 == null) oaDelay1 = 0
    if(oaDelay2 == null) oaDelay2 = 0
    if(oaDelay3 == null) oaDelay3 = 0
    delayList[1] = oaDelay1.toInteger()*60
    delayList[2] = oaDelay2.toInteger()*60
    delayList[3] = oaDelay3.toInteger()*60
    if(zbDisable == null) zbDisable = 0
    if(zwDisable == null) zwDisable = 0
    if(appDisable == null) appDisable = 0
    if(turnOffDevs == null) turnOffDevs = 0
    if(rebootHubO == null) rebootHubO = 0 
    if(shutdownHub == null) shutdownHub = 0    
    if(rmRuleO == null) rmRuleO = 0
    
    
    if(zbDisable.toInteger() > 0) runIn(delayList[zbDisable.toInteger()], "disableZb")
    if(zwDisable.toInteger() > 0) runIn(delayList[zwDisable.toInteger()], "disableZw")
    if(appDisable.toInteger() > 0) runIn(delayList[appDisable.toInteger()], "disableApps")
    if(turnOffDevs.toInteger() > 0) runIn(delayList[turnOffDevs.toInteger()], "devsOff")
    if(rebootHubO.toInteger() > 0) runIn(delayList[rebootHub.toInteger()], "reboot")
    if(shutdownHub.toInteger() > 0) runIn(delayList[shutdownHub.toInteger()], "shutdown")
    if(rmRuleO.toInteger()) runIn(delayList[rmRuleO.toInteger()], "outageRunRM")
}

void disableZb(){
    zbPost("disabled")
}

void disableZw(){
    zwPost("disabled")
}

void disableApps(){
    appsPost("disable")
}

void devsOff() {
    turnOffDevsList.each {
        it.off()
    }
}

void outageRunRM(){
    rmRuleOList.each{
        RMUtils.sendAction([it.toInteger()],"runRuleAct", app.getLabel(), "5.0")
    }
}


void startUpActions(){
    if(state.outage == false) return //already started processing
    unschedule() //stop any pending shutdown activity
    state.outage = false
    runIn(triggerDelay.toInteger()*60, "startRecover")
}

void startRecover(){
    if(state.outage) return //check if conditions have changed to outage
    

    notifyDev.each { 
      it.deviceNotification(notifyMsgUp)  
    } 
    
    if(zbEnabled) zbPost("enabled")
    if(zwEnabled) zwPost("enabled")
    if(appEnabled) appsPost("enable")
    if(rebootHub) runIn(120,"reboot")//allow time for the other actions to complete
    if(onDelay1)  runIn(onDelay1.toInteger()*60,"devicesOn1")
    if(onDelay2)  runIn(onDelay2.toInteger()*60,"devicesOn2")
    if(rmRuleR)   runIn(rmRuleR.toInteger()*60,"restoreRunRM")
}

@SuppressWarnings('unused')
HashMap getCookie(){
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
	return [result: result, cookie: cookie]
}

def appButtonHandler(btn) {
    switch(btn) {
        case "saveAction":
            state.saveActions = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}               

def zwPost(eOrD){//"enabled"/"disabled"
    try{
        params = [
					uri: "http://127.0.0.1:8080/hub/zwave/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zwaveStatus:"$eOrD"],
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
}

def zbPost(eOrD){"enabled"/"disabled"
    try{
        params = [
                    uri: "http://127.0.0.1:8080/hub/zigbee/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zigbeeStatus:"$eOrD"], 
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
}

void reboot(){
    String cookie=(String)null
    if(security) cookie = getCookie().cookie
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/reboot",
			headers:[
				"Cookie": cookie
			]
		]
	) {		resp ->	} 
}

@SuppressWarnings('unused')
void shutdown() {
    String cookie=(String)null
    if(security) cookie = getCookie().cookie
	httpPost(
		[
			uri: "http://127.0.0.1:8080",
			path: "/hub/shutdown",
			headers:[
				"Cookie": cookie
			]
		]
	) {		resp ->	} 
}

def appsPost(String eOrD){
    if(eOrD == "enable") tOrF = false
    else tOrF = true
    if(appDisableList.size() < 1)
        return
    if(appDisableList[0] == 0 || appDisableList[0] == "0")
        appList = appDisableList
    else
        appList = getAppsList().id
    
    
    appList.each(){
        if(it.id != this.getId()){
        try{
            params = [
                uri: "http://127.0.0.1:8080/installedapp/disable",
                headers: [
                    "Content-Type": "application/x-www-form-urlencoded",
                    Accept: "application/json"
                ],
                body:[id:"${it.id}", disable:"$tOrF"], //
                followRedirects: true
            ]
            if(debugEnabled) log.debug "$params"
            httpPost(params){ resp ->
                if(debugEnabled) log.debug "appsPost response: $resp.data"
    		}
        }catch (e){
        }
        }
    }
}

void restoreRunRM(){
    rmRuleRList.each{
        RMUtils.sendAction([it.toInteger()],"runRuleAct", app.getLabel(), "5.0")
    }
}

HashMap [] getAppsList() { 
	def params = [
		uri: "http://127.0.0.1:8080/hub2/appsList",
        headers: [
            accept:"application/json"
        ],
		textParser: false
	  ]
	
	def allAppsList = []
    def allAppNames = []
	try {
		httpGet(params) { resp ->   
            resp.data.apps.data.each {
				allAppsList.add([id:it.id,title:it.name])
                allAppNames.add( it.name )               
            }

		}
	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    
    return allAppsList
}

void devicesOn1(){
    turnOnDevsList1.each{
        it.on()
    }
}

void devicesOn2(){
    turnOnDevsList2.each{
        it.on()
    }
}
