/*
 * Dexcom Glucose Monitor Master
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
 *    Date        Who           What
 *    ----        ---           ----
 *
*/
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '0.0.5'  }

definition (
	name: 			"Dexcom Master", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Dexcom Glucose Monitor Integration",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/dexcom/dexMaster.groovy",
	oauth: 			true,
    installOnOpen:  true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}

mappings {
    path("/"){
        action: [POST: "authReturn",
                 GET: "authReturn"]
    }
    path("/a"){
        action: [POST: "authReturn",
                 GET: "authReturn"]
    }
    path(""){
        action: [POST: "authReturn",
                 GET: "authReturn"]
    }    
}

void installed() {
	if(debugEnabled) log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 

            section("Main") {

                input "apiUri", "enum", title: "<b>API URI</b>", options:[["https://api.dexcom.eu":"EU"],["https://api.dexcom.jp":"Japan"],["https://api.dexcom.com":"US"],["https://sandbox-api.dexcom.com":"Sandbox"]], description:"Select API Location", submitOnChange: true, width:4
                input "dexClient", "string", title:"<b>App Client Id</b><br />Obtain an App Client Id from Dexcom by <a href='https://developer.dexcom.com/user/register' target='_blank'>registering</a> as a developer", submitOnChange: true, width:4
                input "dexSecret", "password", title:"<b>App Client Secret</b>", description:"Enter Secret obtained from the Dexcom App registration", submitOnChange:true, width:4
                input "debugEnabled", "bool", title:"<b>Enable Debug</b>", submitOnChange:true, width:4
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
            
            }
            section("<h2>Dexcom Authorization</h2>", hideable: false, hidden: false){
                if(state.accessToken == null) createAccessToken()
                    paragraph "<b>Access Token: </b>${state.accessToken}"
                input "resetToken", "button", title:"Reset Hubitat Token"
                paragraph "<b>Redirect URL</b>: ${getFullApiServerUrl()}/a?access_token=${state.accessToken}&"
                paragraph "<small>${"${getFullApiServerUrl()}/a?access_token=${state.accessToken}&".size()} characters</small>"
                input "tinyUrl", "string", title:"<b>Redirect Override</b>", description:"Use TinyUrl or similar if redirect exceeds 128 characters", submitOnChange: true, width:4
                input "initAuth", "button", title: "Get Dexcom Auth Token"
                if (state.iAuthReq){
                    state.iAuthReq = false
                    redirect = URLEncoder.encode("${getFullApiServerUrl()}/a?access_token=${state.accessToken}&", "UTF-8")                
                    
                    state.redirect =  tinyUrl ? URLEncoder.encode("$tinyUrl","UTF-8") : redirect 
                    
                    iaUri = "$apiUri/v2/oauth2/login?client_id=$dexClient&redirect_uri=${tinyUrl ? URLEncoder.encode("$tinyUrl","UTF-8") : redirect }&response_type=code&scope=offline_access"
                    if(debugEnabled) paragraph iaUri
                    paragraph "<script>window.open('$iaUri',target='_blank');</script>"
                }
                /*
                if(state.dexAuthCode){ 
                    input "secondAuth", "button", title: "Get Dexcom Authorization"
                    if(state.secAuth == true || state.secAuth == 'true'){
                        state.secAuth = false
                        getSecondAuth()
                    }
                }
                if(state.dexAccessToken){
                    input "getDevs", "button", title: "Get Devices"
                    if(state.reqDev == true || state.reqDev == 'true') {
                        state.reqDev = false
                        getDevices()
                    }
                    if (state.devCount)
                    paragraph "${state.devCount} Device(s) Created"
                }*/
                if (state.devCount)
                    paragraph "Authorization Received - ${state.devCount} Device(s) Created"
            }
            section("Reset Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
           }

	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def getSecondAuth(){
    bodyText = "client_id=$dexClient&client_secret=$dexSecret&code=${state.dexAuthCode}&grant_type=authorization_code&redirect_uri=${state.redirect}"
    Map requestParams =
	[
        uri: "$apiUri/v2/oauth2/token",
        //textParser: true,
        headers: [
		    "Content-Type" : 'application/x-www-form-urlencoded'
            //"Accept" : 'application/json'
        ],
		body: "$bodyText"
	]
    
    if(debugEnabled) 
        log.debug "$requestParams"
    try {
        httpPost(requestParams) { resp ->
            log.debug 'secondAuth'
            state.dexAccessToken = resp.data.access_token
            state.refreshToken = resp.data.refresh_token
            lastRefresh = new Date().getTime()
            state.lastRefresh = lastRefresh
            state.tExpire = lastRefresh + 7190000 // actual refresh interval is 7200 seconds
            getDevices()
        }
    } catch (ex) {
        log.error "secondAuth: $ex"
    }
}

def getRefresh(){
    bodyText = "client_id=$dexClient&client_secret=$dexSecret&refresh_token=${state.refreshToken}&grant_type=refresh_token&redirect_uri=${state.redirect}"
    Map requestParams =
	[
        uri: "$apiUri/v2/oauth2/token",
        //textParser: true,
        headers: [
		    "Content-Type" : 'application/x-www-form-urlencoded'
            //"Accept" : 'application/json'
        ],
		body: "$bodyText"
	]
    
    if(debugEnabled) 
        log.debug "$requestParams"
    try {
        httpPost(requestParams) { resp ->
            log.debug 'getRefresh'
            state.dexAccessToken = resp.data.access_token
            state.refreshToken = resp.data.refresh_token
            lastRefresh = new Date().getTime()
            state.lastRefresh = lastRefresh
            state.tExpire = lastRefresh + 7190000 // actual refresh interval is 7200 seconds
        }
    } catch (ex) {
        log.error "getRefresh: $ex"
    }
}

def authReturn(){
    log.debug params
    pMap = (HashMap) params
    if (params['?code']) //sandbox
        state.dexAuthCode = params['?code']
    else if (params['code']) //live
        state.dexAuthCode = params['code']
    else 
        log.error "Unknown response with parameters $params"
    runIn(5,'getSecondAuth')
    render contentType:'application/json', data:'{"status":200}',status:200
}

def getDevices(){
   // /v3/users/self/devices
    if(new Date().getTime() > (Long)state.tExpire)
        getRefresh()
    
    Map requestParams =
	[
        uri: "$apiUri/v3/users/self/devices",
        headers: [
            "Authorization" : "Bearer $state.dexAccessToken",
            "Accept" : 'application/json'
        ]
	]
    
    asynchttpGet("processDevices",requestParams)
}

def processDevices(resp, data){
    if(debugEnabled) log.debug resp.json
    rCount = 0
    resp.json.records.each {
        if(debugEnabled) log.debug "${it.transmitterId}, ${it.transmitterGeneration}"
        if(it.transmitterId != null) {
            rCount++
            chd = addChildDevice("thebearmay","Dexcom Glucose Monitor","DGM${app.id}-$rCount", [name: "Dexcom ${app.id}-$rCount", isComponent: false, label:"Dexcom ${app.id}-$rCount ${it.transmitterGeneration}"])
            chd.updateDataValue("dexUserId", "${resp.json.userId}")
            chd.updateDataValue("dexTransmitId", "${it.transmitterId}")
            chd.updateDataValue("dexGen", "${it.transmitterGeneration}")
            if (it.softwareVersion) chd.updateDataValue("dexSoftVersion", "${it.softwareVersion}")
            if (it.softwareNumber) chd.updateDataValue("dexSoftNumber", "${it.softwareNumber}")
        }
        state.devCount = rCount
    }
    
}

void getGlucose(DNI, glucoseRange) {
   // /v3/users/self/egvs
    if(new Date().getTime() > (Long)state.tExpire)
        getRefresh()
    if(glucoseRange == null || glucoseRange < 1)
        glucoseRange = 600
    Long eDate = new Date().getTime()
    Long sDate = eDate - (Long) glucoseRange
    
    sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
    
    startDate = URLEncoder.encode(sdf.format(new Date(sDate)),"UTF-8")
    endDate = URLEncoder.encode(sdf.format(new Date(eDate)), "UTF-8")
    
    if(debugEnabled) log.debug "start: $startDate end: $endDate"
    
    Map requestParams =
	[
        uri: "$apiUri/v3/users/self/egvs?startDate=$startDate&endDate=$endDate",
        headers: [
            "Authorization" : "Bearer $state.dexAccessToken",
            "Accept" : 'application/json'
        ]
	]
    log.debug "$requestParams"
    asynchttpGet("processGlucose",requestParams, [dni:DNI])    
}

def processGlucose(resp, data){
    if(debugEnabled) 
        log.debug resp.properties
    try { 
        DNI = data['dni']
        chd = getChildDevice("$DNI")
        if (resp?.json?.records) {
            chd.updateAttr("glucose", resp.json.records[0].value, resp.json.records[0].unit)
            chd.updateAttr("glucoseStatus", resp.json.records[0].status)
            chd.updateAttr("glucoseTrend", resp.json.records[0].trend)
            chd.updateAttr("glucoseRate", resp.json.records[0].trendRate, resp.json.records[0].rateUnit)
        } else {
            chd.updateAttr("glucoseStatus", "unknown")
        }
            
    } catch (ex) {
        log.error "Get Glucose: $ex <br> ${resp?.properties}"
    }
    
}

void getAlert(DNI, alertRange) {
   // /v3/users/self/alerts
    if(new Date().getTime() > (Long)state.tExpire)
        getRefresh()
    if(alertRange == null || alertRange < 1)
        alertRange = 600
    Long eDate = new Date().getTime()
    Long sDate = eDate - (Long) alertRange
    
    sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
    
    startDate = URLEncoder.encode(sdf.format(new Date(sDate)),"UTF-8")
    endDate = URLEncoder.encode(sdf.format(new Date(eDate)), "UTF-8")
    
    if(debugEnabled) log.debug "start: $startDate end: $endDate"
    
    Map requestParams =
	[
        uri: "$apiUri/v3/users/self/alerts?startDate=$startDate&endDate=$endDate",
        headers: [
            "Authorization" : "Bearer $state.dexAccessToken",
            "Accept" : 'application/json'
        ]
	]
    log.debug "$requestParams"
    asynchttpGet("processAlerts",requestParams, [dni:DNI])    
}

def processAlerts(resp, data){
    if(debugEnabled) 
        log.debug resp.properties
    try { 
        DNI = data['dni']
        chd = getChildDevice("$DNI")
        if (resp?.json?.records) {
            alertList = []
            resp.json.records.each{
                alertList.add(["${it.alertName}":"${it.alertStatus}"])
            }
            chd.updateAttr("alertJson", JsonOutput.toJson(alertList))
        } else {
            alertJson = JsonOutput.toJson(["noAlerts":"unknown"])
            chd.updateAttr("alertJson", alertJson)
        }            
    } catch (ex) {
        log.error "Get Alerts: $ex <br> ${resp?.properties}"
    }
    
}

void appButtonHandler(btn) {
    switch(btn) {
        case "initAuth":
            state.iAuthReq = true
            break
        case "resetToken":
            createAccessToken()
            break
        case "secondAuth":
            state.secAuth = true
            break
        case "getDevs":
            state.reqDev = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
