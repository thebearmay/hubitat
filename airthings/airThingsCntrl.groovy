/*
 * Air Things Allview Cloud Interface
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
	name: 			"Air Things Cloud", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Air Things Allview Cloud Interface",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/airthings/airThingsCntrl.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "cloudCredentials"
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
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
     	    }
            
            section("<span style='text-decoration:underline;font-weight:bold'>Application Credentials</span>", hideable: false, hidden: false){  
                input "userName", "text", title: "Airthings User Name:",width:4
                input "pwd", "password", title: "Airthings Password:", width:4
                input "authBtn", "button", title: "Create Initial Authorization"
                if(state?.authBtnPushed) {
                    state.authBtnPushed = false
                    initialAuth()
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



def cloudCredentials(){
    dynamicPage (name: "cloudCredentials", title: "", install: false, uninstall: false, nextPage:"mainPage") {

    }
}


//Begin App Authorization 

void initialAuth(){
    command = "initialAuth"
    bodyMap = [email:"$userName", password:"$pwd", grant_type:"read:device"]

    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://accounts-api.airthings.com/v1/token", //"https://accounts.airthings.com/authorize",
        requestContentType: 'application/json',
		contentType: 'application/json',
        body: "$bodyText"
	]

    //if(debugEnabled) 
    log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"${command}"]) 
}




void getResp(resp, data) {
    try {
        //if(debugEnabled) 
        log.debug "$resp.properties - ${data['cmd']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data){
   
            } 
        }
    } catch (Exception ex) {
            log.error "getResp - $ex.message"
    } 
}
// End App Authorization
         
// Begin API

void apiGet (command, bodyMap){
    // commands should take the form "devices/{serialNumber}/{subType}"
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://ext-api.airthings.com/v1/$command",
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
        uri:  "https://dkncloudna.com/api/v1/open/${state.siteId}/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        Authorization: "Bearer $state.dknAccessToken",
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    httpPut(requestParams, [cmd:"${command}",bMap:bodyMap]) {resp -> 
        if (resp.getStatus() == 401){
            tokenRefresh()
            pauseExecution(500)
            apiPut(data.cmd, data.bMap)
        } else if (resp.getStatus == 400) {
            log.error "${resp.JSON}"        
        }else {
            mac = data.cmd.substring(0,17)
            macStrip = mac.replace(":","")
            cd = getChildDevice("${device.deviceNetworkId}-${macStrip}")
            cd.updState("${resp.JSON}")
        }
        
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
              state.authBtnPushed = true
              break
          default: 
              if(debugEnabled) log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}

void uninstalled(){
    chdList = getChildDevices()
    chdList.each{
        deleteChildDevice(it.getDeviceNetworkId())
    }
}
