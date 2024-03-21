/*
 * Hub Failover
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
 *	Date        Who           What
 * 	----        ---           ----
 *	21Mar2024   thebearmay    Update the applist endpoint for the new UI	
*/

static String version()	{  return '1.0.1'}

import java.text.SimpleDateFormat
import java.util.Date
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

definition (
	name: 			"Hub Failover", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Monitors Production Hub heartbeat, and turns on the radios if the production hub does not respond.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/heHa/heFailover.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
}

mappings {
    path("/heartbeat") {
        action: [POST: "beatCheck"]
    }
    path("/shutdown") {
        action: [POST: "hubShutdown"]
    }
}

void installed() {
    if(debugEnabled) log.trace "${app.getLabel()} installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "${app.getLabel()} updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<span style='color:blue;font-weight:bold;font-size:x-Large'>${app.getLabel()}  <span style='font-size:x-small'>v${version()}</span></span>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 
            section("") {
				input "debugEnabled", "bool", title:"Enable Debug Logging", submitOnChange:true, required:false, defaultValue:false, width:4
                if(debugEnabled) {
                    unschedule("logsOff")
                    runIn(1800,logsOff)
                }
            }
            section("<span style='color:blue;font-weight:bold'>Failover Settings</span>", hideable: true, hidden: false){
                input "prodHub", "string", title:"IP Address of Production Hub", width:5, submitOnChange:true
                input "heartbeatInterval", "number", title: "Hearbeat Interval", width:2, constraints:["NUMBER"], submitOnChange:true, defaultValue:1
                input "heartbeatUnit", "enum", title: "Heartbeat Interval Units", options: ["Seconds","Minutes"], width:4, submitOnChange:true, defaultValue:"Minutes"
                input "missed", "number", title: "How many check-ins can be missed", constraints: ["NUMBER"], width:2, submitOnChange:true, defaultValue:5
                if(location.hub.localIP != prodHub){
                    state.hubRole = "Monitor"
                    input "pauseApps", "button", title: "Disable/Enable Apps", width:2
                    if(state.pauseApps == true){
                        toggleApps()
                        state.pauseApps = false
                    }
                    
                    paragraph "Apps state is showing: ${state.appToggle}", width:2
                    
                    input "hbEnabled", "bool", title: "Turn off radios and start heartbeat monitoring", defaultValue:false, submitOnChange: true, width:4
                    input "monitorOnly", "bool", title: "Leave radios on and monitor heartbeat <br><span style='font-size:x-small'>(could cause a conflict if Hub Protect™ restore has been done on this hub)</span>", defaultValue:false, submitOnChange: true, width:4
                    if(heartbeatUnit.toString() == null) app.updateSetting("hearbeatUnit",[value:"Minutes",type:"enum"])
                    if(monitorOnly){
                        if(hbEnabled) {
                            app.updateSetting("hbEnabled",[value:"false",type:"bool"])
                            zwPost("enabled")
                            zbPost("enabled")
                        }  
                        if(heartbeatUnit == "Minutes") multiplier = 60
                        else multiplier = 1
                        state.hbMissed = 0
                        runIn(heartbeatInterval.toInteger()*multiplier,"heartbeat")                        
                    } else if(hbEnabled) {
                        app.updateSetting("monitorOnly",[value:"false",type:"bool"])
                        zwPost("disabled")
                        zbPost("disabled")
                        if(heartbeatUnit == "Minutes") multiplier = 60
                        else multiplier = 1
                        state.hbMissed = 0
                        runIn(heartbeatInterval.toInteger()*multiplier,"heartbeat")
                    } else
                        unschedule("heartbeat")
                    
                } else {
                    state.hubRole = "Source"
                    unschedule("heartbeat")
                }
                input "notifyDev", "capability.notification", title: "Device(s) to notify", multiple:true, width:6, submitOnChange:true
            }
            section("<span style='color:blue;font-weight:bold'>Monitored Hub Security</span>", hideable: true, hidden: true){
                if(state.accessToken == null) createAccessToken()
                if(state.hubRole == "Source"){
                    tDefault = state.accessToken
                    aDefault = getFullLocalApiServerUrl()
                }
                input "remoteAPI", "text", title:"<b>Source Server API:</b>",submitOnChange:true, defaultValue: aDefault
                input "token","text", title:"<b>Source Access Token:</b>",submitOnChange:true, defaultValue: tDefault
                input "sourceSecurity", "bool", title: "Source Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4
                if (sourceSecurity) { 
                    input("sUsername", "string", title: "Source Hub Security Username", required: false)
                    input("sPassword", "password", title: "Source Hub Security Password", required: false)
                }
            }
            section("<span style='color:blue;font-weight:bold'>Local Hub Information</span>", hideable: true, hidden: true){
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
                input "resetToken", "button", title:"Reset Token"
            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }        
    }
}

def zwPost(eOrD){
    try{
        params = [
					uri: "http://127.0.0.1:8080/hub/zwave/update",
                    //uri: "http://127.0.0.1:8080/hub/zigbee/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zwaveStatus:"$eOrD"],//[zigbeeStatus:"disabled"], //
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            //if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
}

def zbPost(eOrD){
    try{
        params = [
                    uri: "http://127.0.0.1:8080/hub/zigbee/update",
                    headers: [
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[zigbeeStatus:"$eOrD"], //
                    followRedirects: false
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            //if(debugEnabled) log.debug "$resp.data"
		}
    }catch (e){
        
    }
}

def heartbeat(){
    jsonText = JsonOutput.toJson([heartbeat:"${location.hub.localIP}"])
	Map requestParams =
	[
        uri:  "$remoteAPI/heartbeat?access_token=$token",
        requestContentType: 'application/json',
		contentType: 'application/json',
       body: "$jsonText"
	]
    if(debug.enabled) log.debug "HB: $requestParams"
    httpPost(requestParams) { resp ->
        try {
            if(debugEnabled) 
                log.debug "$resp.properties ${resp.getStatus()}"
            if(resp.getStatus() == 200 || resp.getStatus() == 207){
                if(resp.data) {
                    respMap =  (HashMap) resp.data
                    state.zwave = respMap.zwave
                    state.zigbee = respMap.zigbee
                    state.alive = respMap.alive
                    state.hbMissed = 0
                } else {
                    state.hbMissed = state.hbMissed.toInteger + 1
                    state.alive = "unknown"
                    state.zigbee = "unknown"
                    state.zwave = "unknown"
                    sendNotice("$prodHub missed heartbeat check, count = $state.missed")
                }
            }
        } catch (Exception ex) {
            log.error "$ex<br>$ex.getResponse()"
        }
    }    
    if(state.hbMissed.toInteger() > missed.toInteger() && !monitorOnly){
        zwPost("enabled")
        zbPost("enabled")
        if(state.appToggle == "disabled")
            toggleApps()
        sendNotice("Hub Failover for $prodHub is ACTIVE")
        return
    }
         
    if(hbEnabled || monitorOnly) {
        if(heartbeatUnit == "Minutes") multiplier = 60
        else multiplier = 1
        runIn(heartbeatInterval.toInteger()*multiplier,"heartbeat")
    } else
        unschedule("heartbeat")
}

def sendNotice(msg){
    notifyDev.each { 
      if(debugEnable) log.debug "Sending notification to $it, text: $msg"
      it.deviceNotification("$msg")  
    }     
}

def beatCheck(){
    if (debugEnabled) log.debug "beatCheck()"
    Map params =
        [
                uri    : "http://${location.hub.localIP}:8080",
                path   : "/hub2/hubData"        
        ]
    
    httpGet(params) { resp ->
        try{
            if(debugEnabled) log.debug resp.data
            h2Data = (Map) resp.data
            if(h2Data.baseModel.zwaveStatus == "false") 
                state.hbZwStatus="enabled"
            else
                state.hbZwStatus="disabled"
            if(h2Data.baseModel.zigbeeStatus == "false")
                state.hbZbStatus="enabled"
            else 
                state.hbZbStatus="disabled"            
        } catch (e) {
            log.error e
        }

    }
    jsonText = JsonOutput.toJson([alive:true, zwave:"${state.hbZwStatus}", zigbee:"${state.hbZbStatus}"] )
    if(debugEnabled) log.debug "rendering $jsonText"
    render contentType:'application/json', data: "$jsonText", status:200 
}

def toggleApps(){
    getAppsList()
    state.appsList.each{
        if(it.id != app.id){
            if(state.appToggle == "disabled"){
                appsPost("enable", "${it.id}")
                if(debugEnabled) log.debug "enable, $it.id"
            }else{
                appsPost("disable", "${it.id}")
                if(debugEnabled) log.debug "disable, $it.id"
            }          
        }
    }     
    if(state.appToggle == "disabled") state.appToggle = "enabled"
    else state.appToggle = "disabled"
}

def appsPost(String eOrD, String aId){
    if(eOrD == "enable") tOrF = false
    else tOrF = true
    try{
        params = [
            uri: "http://127.0.0.1:8080/installedapp/disable",
            headers: [
                "Content-Type": "application/x-www-form-urlencoded",
                Accept: "application/json"
            ],
            body:[id:"$aId", disable:"$tOrF"], //
            followRedirects: true
        ]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "appsPost response: $resp.data"
		}
    }catch (e){
        
    }
}

def getAppsList() { 
 //   if (security)
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
    state.appsList = allAppsList.sort { a, b -> a.title <=> b.title }
}

def hubShutdown(){
    log.info "Hub Reboot requested"

    String cookie=(String)null
    if(sourceSecurity) cookie = getCookie()
	httpPost(
		[
			uri: "http://${location.hub.localIP}:8080",
			path: "/hub/shutdown",
			headers:[
				"Cookie": cookie
			]
		]
	) {		resp ->	} 
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

def appButtonHandler(btn) {
    switch(btn) {
        case "pauseApps":
            state.pauseApps = true
            break
        case "resetToken":
            createAccessToken()
            break        
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
