/*
 * Daikin DKN Cloud Interface
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
*/

static String version()	{  return '0.0.0'  }
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition (
	name: 			"Daikin DKN Cloud", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Daikin DKN Cloud interface - handles all communication with the Daikin Cloud.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubVarSync.groovy",
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "localInfo"
   page name: "dknCredentials"
}

mappings {
    path("/dnkAuth") {
        action: [POST: "authRequest"]
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

                href "localInfo", title: "Setup Server Information", required: false
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
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

def localInfo(){
    dynamicPage (name: "localInfo", title: "", install: false, uninstall: false) {
        section("Local Hub Information", hideable: false, hidden: false){
            //paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
            paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
            if(state.accessToken == null) createAccessToken()
            paragraph "<b>Access Token: </b>${state.accessToken}"
            input "resetToken", "button", title:"Reset Token"

            paragraph """<b>From the Daikin DNK Cloud Setup</b><hr /><p>Before using Oauth, the third party client application must be registered with the Open API service,
where the following details of the application must be provided:
• Application Name
• List of Redirect URI or Callback URL</p>
<p>The redirect URI is where the Open API will redirect the user after they authorize (or deny) the
application, and therefore the part of the application that will handle authorization codes or access
tokens. This must be seen as a list of valid URLs. As we will see next, on the authentication
process using a web interface based flow, the application must specify a redirect URL. This URL
must match one of the registered URLS. This is a safety measure used to ensure that the user will
only be directed to appropriate locations.</p>
<p><b><u>As of now, this registration is done exclusively on demand, and requires manual interaction
of the system administrator of the DKN Cloud NA ecosystem.</u></b></p>
<hr />"""
            
            paragraph "<b>Redirect URL path for Daikin:</b> ${getFullApiServerUrl()}/dnkAuth?access_code=${state.accessToken}"
            href "dknCredentials",title: "<b>AFTER</b> you have registered this application with the Daikin DKN administrator go here to enter all credentials"
            
            
        }
    }
}

def dknCredentials(){
    dynamicPage (name: "dknCredentials", title: "", install: false, uninstall: false, nextPage:"mainPage") {
        section("Application Credentials", hideable: false, hidden: false){  
            input "dknUserName", "text", title: "Daikin User Name:" 
            input "dknPassword", "password", title: "Daikin Password:" 
            input "dknAppName", "text", title: "Daikin Registered App Name:"
            input "dknCode", "text", title: "Daikin Client Code:"
            input "dknClientId", "text", title: "Daikin Client ID:"
            input "dknClientSecret", "text", title: "Daikin Client Secret:"
            input "authBtn", "button", title: "Create Initial Authorization"
        }
    }
}


//Begin App Authorization 
def authRequest() {
    log.error "Unexpected endpoint access: ${request.data}"
}

void intialAuth(){
    command = "auth/login/dknUsa"
    bodyMap = [email:"$dknUserName", password:"$dknPassword"]

    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://www.dkncloudna.com/api/v1/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"${command}"]) 
}


void authReq2(command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://www.dkncloudna.com/api/v1/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        Authorization: "Bearer $state.temp_token",
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"${command}"]) 
}

void tokenRequest(command, bodyMap){
    def bodyText = ""
    bodyMap.each {
        if(bodyText !="") bodyText += "&"
        bodyText+=URLEncoder.encode(it.key, "UTF-8")
        bodyText+="="
        bodyText+=URLEncoder.encode(it.value, "UTF-8")
    }
    Map requestParams =
	[
        uri:  "https://www.dkncloudna.com/api/v1/$command",
        requestContentType: 'application/json',
		contentType: 'application/x-www-form-urlencoded',
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"${command}"]) 
}

void tokenRefresh() {
    tokenRequest("open/oauth2/token",[client_id:"$dknClientId",client_secret:"$dknClientSecret",grant_type:"refresh_token",code:"$dknCode"])
}

void getResp(resp, data) {
    try {
        if(debugEnabled) log.debug "$resp.properties - ${data['cmd']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data){
                if(data.cmd == "auth/login/dknUsa"){ 
                    // response from initial auth request
                    jsonData = (HashMap) resp.JSON
                    state.temp_token = jsonData.token
                    authReq2("users/oauth2/authorize",[entityName:"$dknAppName",client_id:"$dknClientId", scopes:"devices, installations"])
                } else if (data.cmd =="users/oauth2/authorize"){
                    jsonData = (HashMap) resp.JSON
                    state.dknAuthCode = jsonData.redirectUri.substring(jsonData.redirectUri.indexOf("=")+1)
                    tokenRequest("open/oauth2/token",[client_id:"$dknClientId",client_secret:"$dknClientSecret",grant_type:"authorization_code",code:"$dknCode"])
                } else if (data.cmd =="users/oauth2/token"){
                    jsonData = (HashMap) resp.JSON
                    state.dknAccessToken = jsonData.access_token
                    state.dknRefreshToken = jsonData.refresh_token
                    state.dknTokenTimeStamp = new Date()
                    // Now get the device list
                    apiGet("devices",[])
                }
            } else 
                atomicState.returnString =  "{\"status\":\"${resp.getStatus()}\"}"
        }
    } catch (Exception ex) {
            log.error "getResp - $ex.message"
    } 
}
// End App Authorization
         
// Begin API

void apiGet (command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://dkncloudna.com/api/v1/open/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        Authorization: "Bearer $state.dknAccessToken",
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    asynchttpGet("getApi", requestParams, [cmd:"${command}"]) 
}

void getApi(resp, data){
    try {
        if(debugEnabled) log.debug "$resp.properties - ${data['cmd']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data){
                if(data.cmd == "devices"){
                    jsonData = (HashMap) resp.JSON
                    state.siteId = jsonData._id
                    state.siteName = jsonData.name 
                    unitTran = ['C', 'F']
                    state.siteUnit = unitTran[jsonData.Units.toInteger()]
                    jsonData.devices.each{
                        createChildDev(it.name, it.mac)
                    }
                } else if(data.cmd.indexOf("$state.siteId")!= -1) {
                    mac = data.cmd.substring(data.cmd.lastIndexOf("/")+1)
                    macStrip = mac.replace(":","")
                    cd = getChildDevice("${device.deviceNetworkId}-${macStrip}")
                    cd.updState("${resp.JSON}")
                }
            }
        }
    } catch (Exception e) {
        log.error "getApi - $e.message"        
    }
}
                   

void apiPut (command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://dkncloudna.com/api/v1/open/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        Authorization: "Bearer $state.dknAccessToken",
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    httpPut("getApi", requestParams, [cmd:"${command}"]) {resp -> 
        
    }
}

// End API

void createChildDev(name, mac){
    macStrip = mac.replace(":","")
    cd = addChildDevice("thebearmay", "Daikin Cloud Device", "${device.deviceNetworkId}-$macStrip", [name: "${name}", isComponent: true, mac:"$mac", label:"dcd$name"])
    apiGet("${state.siteId}/${cd.properties.data["${mac}"]}")
}

void appButtonHandler(btn) {
    switch(btn) {
          case ("authBtn"):
              initialAuth()
              break
          default: 
              if(debugEnabled) log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}

void uninstalled(){

}
