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

static String version()	{  return '0.0.1'  }
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition (
	name: 			"Air Things Cloud", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Air Things Allview Cloud Interface",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/xxx.groovy",
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
                paragraph "<a href='https://dashboard.airthings.com/integrations/api-integration/add-api-client', target='_blank'>Link to AirThings for ID/Secret</a>"
                input "userName", "text", title: "Airthings API ID:",width:4
                input "pwd", "password", title: "Airthings Secret:", width:4
                input "authBtn", "button", title: "Create Initial Authorization"
                if(state?.authBtnPushed) {
                    state.authBtnPushed = false
                    getAuth("initialAuth")
                }
                paragraph "Token: ${state.temp_token.toString().substring(0,50)}..."
                if(state?.temp_token != null){
                    input "devBtn", "button", title: "Get Devices"
                    if(state?.devBtnPushed) {
                        state.devBtnPushed = false
                        apiGet("/devices")
                    } 
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

void getAuth(command){
    bodyMap = [grant_type:"client_credentials",client_id:"$userName", client_secret:"$pwd"]

    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://accounts-api.airthings.com/v1/token",
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
                if(data.cmd == "initialAuth"){
                    jsonData = (HashMap) resp.json
                    state.temp_token = jsonData.access_token
                } else if(data.cmd == "reAuth") {
                    jsonData = (HashMap) resp.json
                    state.temp_token = jsonData.access_token
                }
   
            } 
        }
    } catch (Exception ex) {
            log.error "getResp - $ex.message"
    } 
}
// End App Authorization
         
// Begin API

void apiGet (command){
    getAuth("reAuth")
    // commands should take the form "devices/${devId}/optionalParam"
    //def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://ext-api.airthings.com/v1/$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        Authorization: "Bearer $state.temp_token"
        //body: "$bodyText"
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
                    jsonData = (HashMap) resp.json
                    jsonData.devices.each{
                        if(debugEnabled) log.debug "${it.id}, ${it.deviceType}, ${it.segment.name}"
                        //createChildDev(it.id, it.deviceType, it.segment.name)
                    }
                } else if(data.cmd == ""){
                }
            }
        }
    } catch (Exception e) {
        log.error "getApi - $e.message"        
    }
}
                   

// End API

void createChildDev(devId, devType, devName){
    cd = addChildDevice("thebearmay", "Air Things Device", "${device.deviceNetworkId}-$devId", [name: "${devName}", isComponent: true, deviceId:"$devId", label:"$devName"])
    apiGet("devices/${devId}/samples")
}

void appButtonHandler(btn) {
    switch(btn) {
          case ("authBtn"):
              state.authBtnPushed = true
              break
          case ("devBtn"):
              state.devBtnPushed = true
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
