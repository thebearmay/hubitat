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
*/

static String version()	{  return '0.0.0' }

definition (
	name: 			"Power Outage Manager", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides an interface to define actions to take when power goes down and is later restored.",
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
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
            section("<h3>Main</h3>"){
                
                input "triggerDevs", "capability.powerSource", title:"Devices with PowerSource to act as Triggers", submitOnChange:true
                if(triggerDevs != null) {
                    unsubscribe()
                    triggerDevs.each{
                        subscribe(it, "powerSource", "triggerOccurrence")
                    }
                    subscribe(location, "systemStart", "systemStartCheck")
                } else
                    unsubscribe()

                input "triggerDelay", "number", title:"Number of minutes to delay before taking action", defaultValue:0, width:3, submitOnChange:true
                input "agreement", "number", title: "Number of devices that must agree before taking action", defaultValue:1, width:3, submitOnChange:true
                input "notifyDev", "capability.notification", title: "Send notifications to", submitOnChange:true
                input "notifyMsgOut", "string", title: "Notification Message - Power Out", defaultValue: "${app.getLabel()} - Power Outage Detected", submitOnChange:true
                input "notifyMsgUp", "string", title: "Notification Message - Power Restored", defaultValue: "${app.getLabel()} - Power Restored", submitOnChange:true
                
                href "outAction", title: "Power Outage Actions", required: false, width:6, submitOnChange:true
                href "upAction", title: "Power Restored Actions", required: false, width:6, submitOnChange:true
                input "debugEnabled", "bool", title: "Turn on Debug Logging", submitOnChange:true
                input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
                if (security) { 
                    input("username", "string", title: "Hub Security Username", required: false, submitOnChange: true)
                    input("password", "password", title: "Hub Security Password", required: false, submitOnChange: true)
                    login = getCookie()
                    paragraph "Login successful: ${login.result}\n${login.cookie}"
                }
            }

            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            }            
        }
    }
}

def outAction(){
    dynamicPage (name: "outAction", title: "<h2>Power Outage Actions</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3></h3>"){
            input "oaDelay1", "number", title:"Minutes before executing actions selected for Outage Response Queue 1", submitOnChange:true, width:4
            input "oaDelay2", "number", title:"Minutes before executing actions selected for Outage Response Queue 2", submitOnChange:true, width:4
            input "oaDelay3", "number", title:"Minutes before executing actions selected for Outage Repsonse Queue 3", submitOnChange:true, width:4
            
            paragraph "<b>Assign each of the below to a Response Queue, items assigned to Queue 0 will not be scheduled</b>"
            
            input "zbDisable", "enum", title: "Turn off the ZigBee Radio", options: [0,1,2,3], submitOnChange:true, width:4
            input "zwDisable", "enum", title: "Turn off the ZWave Radio", options: [0,1,2,3], submitOnChange:true, width:4
            input "appDisable", "enum", title: "Disable all Rules/Apps (except this one)", options: [0,1,2,3], submitOnChange:true, width:4
            input "shutdownHub", "enum", title: "Shut down the hub", options: [0,1,2,3], submitOnChange:true, width:4
           
        }           
    }
}

def upAction(){
    dynamicPage (name: "upAction", title: "<h2>Power Restore Actions</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3></h3>"){
            input "zbEnable", "bool", title: "Turn on the ZigBee Radio", submitOnChange:true, width:4
            input "zwEnable", "bool", title: "Turn on the ZWave Radio", submitOnChange:true, width:4
            input "appEnable", "bool", title: "Enable all Rules/Apps (except this one)", submitOnChange:true, width:4
            input "rebootHub", "bool", title: "Reboot the hub", submitOnChange:true, width:4
        }           
    }
}

void triggerOccurrence(evt){
    log.debug "Time: ${evt.unixTime} Device: ${evt.deviceId}:${evt.displayName} Value: ${evt.value}"
    if(state.onMains == null) state.onMains = [:]  
    if(state.onBattery == null) state.onBattery = [:]
    
    if(evt.value.toString().trim() == "battery") {
        state.onBattery["dev${evt.deviceId}"] = true
        mainsTemp = [:]
        state.onMains.each{
            if(it.key != "dev${evt.deviceId}")
                mainsTemp[it.key] = state.onMains[it.key]
        }
        state.onMains = mainsTemp       
        log.debug "${state.onMains} <br> ${state.onBattery}"
        if(state.onBattery.size() >= agreement) startOutActions()
    } else if(evt.value.toString().trim() == "mains") {
        state.onMains["dev${evt.deviceId}"] = true
        batteryTemp = [:]
        state.onBattery.each{
            if(it.key != "dev${evt.deviceId}")
                mainsTemp[it.key] = state.onBattery[it.key]
        }
        state.onBattery = batteryTemp
           
        log.debug "${state.onMains} <br> ${state.onBattery}"
        if(state.onMains.size() >= agreement) startUpActions()
    }
}

void systemStartCheck(evt){
    unschedule("reboot")
    unschedule("shutdown")
    pollDevices() // verify the powerSource value in case it changed
    if(state.onBattery.size() >= agreement) startOutActions() // should only occur if something else triggered a reboot or an outage occurred during reboot
    
}

void pollDevices(){
    triggerDevs.each { dev ->
        log.debug dev.currentValue("powerSource")
        switch (dev.currentValue("powerSource")){
            case "mains":
                state.onMains["dev${dev.id}"] = true
                batteryTemp = [:]
                state.onBattery.each{
                    if(it.key != "dev${dev.id}")
                        batteryTemp[it.key] = state.onMains[it.key]
                }
                state.onBattery = batteryTemp
                break
            case "battery":
                state.onBattery["dev${dev.id}"] = true
                mainsTemp = [:]           
                state.onMains.each{
                    log.debug "${dev.id} ${it.key}"
                    if(it.key != "dev${dev.id}"){
                        mainsTemp[it.key] = state.onBattery[it.key]
                    }
                }
                state.onMains = mainsTemp
                break
            default:
                log.error "Invalid Value"
                break
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
    delayList[1] = oaDelay1.toInteger()*60
    delayList[2] = oaDelay2.toInteger()*60
    delayList[3] = oaDelay3.toInteger()*60
    if(zbDisable > 0) runIn(delayList[zbDisable.toInteger()], "disableZb")
    if(zwDisable > 0) runIn(delayList[zwDisable.toInteger()], "disableZw")
    if(appDisable > 0) runIn(delayList[appDisable.toInteger()], "disableApps")
    if(shutdownHub > 0) runIn(delayList[shutdownHub.toInteger()], "shutdown")
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

void startUpActions(){
    if(state.outage == false) return //already started processing
    state.outage = false
    runIn(triggerDelay.toInteger()*60, "startRecover")
}

void startRecover(){
    if(state.outage) return //check if conditions have changed to outage
    notifyDev.each { 
      it.deviceNotification(notifyMsgUp)  
    } 
    if(zbEnable) zbPost("enabled")
    if(zwEnable) zwPost("enabled")
    if(appEnable) appsPost("enable")
    if(rebootHub) runIn(120,"reboot")//allow time for the other actions to complete
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
    
    appList = getAppsList() 
    
    appList.each(){
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

HashMap [] getAppsList() { 

	def params = [
		uri: "http://127.0.0.1:8080/installedapp/list",
		textParser: true
	  ]
	
	def allAppsList = []
    def allAppNames = []
	try {
		httpGet(params) { resp ->    
			def matcherText = resp.data.text.replace("\n","").replace("\r","")
			def matcher = matcherText.findAll(/(<tr class="app-row" data-app-id="[^<>]+">.*?<\/tr>)/).each {
				def allFields = it.findAll(/(<td .*?<\/td>)/) // { match,f -> return f } 
				def id = it.find(/data-app-id="([^"]+)"/) { match,i -> return i.trim() }
				def title = allFields[0].find(/data-order="([^"]+)/) { match,t -> return t.trim() }
				allAppsList += [id:id,title:title]
                allAppNames << title
			}

		}
	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    return allAppsList
}
