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
 *    Date         Who           What
 *    ----         ---           ----
 *    16Oct2022    thebearmay    Code cleanup
 *    16Nov2023    thebearmay    Allow removal of a child device outside of app uninstall
*/

static String version()	{  return '0.1.5'  }
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

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
                input "userName", "text", title: "<b>Airthings API ID:</b>",width:4
                input "pwd", "password", title: "<b>Airthings Secret:</b>", width:4
                input "authBtn", "button", title: "Create Initial Authorization"
                if(state?.authBtnPushed) {
                    state.authBtnPushed = false
                    authToken = getAuth("initialAuth")
                    apiGet("devices")
                }
                if(state.temp_token != null) {
                    def eos = state.temp_token.size() > 50 ? 50 : state.temp_token.size()
                    paragraph "<b>Token:</b> ${state.temp_token.toString().substring(0,eos)}. . . . ."
                    paragraph "<b>Expires:</b> ${state?.tokenExpiresDisp}"
                }
                if(state?.temp_token != null){
                    input "devBtn", "button", title: "Get Devices"
                    if(state?.devBtnPushed) {
                        state.devBtnPushed = false
                        apiGet("devices")
                    }
                    if (state.numberDevices == null) state.numberDevices = 0
                    paragraph "Found ${state.numberDevices} devices"
                }
            } 
            section("<b><u>Child Device Maintenance</u></b>"){
                /*input "forceCreate","button", title: "Force Device Creation"
                if(state?.forceCreatePushed) {
                    state.forceCreatePushed = false
                    createChildDev("Air-FC001", "Forced", "Air-FC001", "Virtual")
                    createChildDev("Air-FC002", "Forced", "Air-FC002", "Virtual")
                    if(!state.numberDevices) 
                        state.numberDevices = 2
                    else
                        state.numberDevices+=2
                }*/
                if (state?.numberDevices > 0){
                    def chdList = []
                    getChildDevices().each{
                        chdList.add("$it")
                    }
                    //log.debug chdList
                    input "removeChild", "enum", title:"<b>Remove child device</b>", defaultValue:"None", submitOnChange:true, options:chdList
                    if(removeChild ){
                        getChildDevices().each{
                            if("$it" == "$removeChild")
                                deleteChildDevice(it.getDeviceNetworkId())
                        }
                        app.updateSetting("removeChild",[value:null,type:"enum"])
                    }
                    
                }
            }

           section("Reset Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
           }

	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}


//Begin App Authorization 

void getAuth(command){
    def bodyMap = [grant_type:"client_credentials",client_id:"$userName", client_secret:"$pwd","scope": ["read:device:current_values"]]

    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "https://accounts-api.airthings.com/v1/token",
        requestContentType: 'application/json',
		contentType: 'application/json',
        body: "$bodyText"
	]

    if(debugEnabled) 
        log.debug "$requestParams"
    httpPost (requestParams) { resp ->
        if(debugEnabled) 
        	log.debug "${resp.properties} - ${command} - ${resp.getStatus()} "
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data){
                    Map jsonData = (HashMap) resp.data             
                    state.temp_token = jsonData.access_token
                    if(debugEnabled) log.debug "Token: ${jsonData.access_token}"
                    state.tokenExpires = (jsonData.expires_in.toLong()*1000) + new Date().getTime().toLong()
                    SimpleDateFormat sdf= new SimpleDateFormat("HH:mm:ss yyyy-MM-dd")
                    state.tokenExpiresDisp = sdf.format(new Date(state.tokenExpires))
            } 
        }
    }
}
// End App Authorization
         
// Begin API

def apiGet (command){
    if(!state.tokenExpires)
        return
    if(new Date().getTime().toLong() >= state?.tokenExpires?.toLong() - 3000) //if token has expired or is within 3 seconds of expiring
        getAuth("reAuth")
    // commands should take the form "devices/${devId}/optionalParams
                           
	Map requestParams =
	[
        uri: "https://ext-api.airthings.com/v1/$command",
        headers: [
            Authorization: "Bearer ${state.temp_token}",
            requestContentType: 'application/json',
		    contentType: 'application/json'
        ]
	]

    if(debugEnabled) 
        log.debug "$requestParams"
    asynchttpGet("getApi", requestParams, [cmd:"${command}"])
    
}

def getApi(resp, data){   
    try {
        if(debugEnabled) 
            log.debug "$resp.properties - $data.cmd - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data){
                if(data.cmd == "devices"){
                    def jsonData = (HashMap) resp.json
                    def numDev = 0
                    jsonData.devices.each{
                        if(debugEnabled) log.debug "${it.id}, ${it.deviceType}, ${it.segment.name}"
                        createChildDev(it.id, it.deviceType, it.segment.name, it.location.name)
                        numDev++
                    }
                    state.numberDevices = numDev
                } else if(data.cmd.contains("latest-samples")) {
                    def start = data.cmd.indexOf('/')+1
                    def end = data.cmd.indexOf('/',start)
                    def devId = data.cmd.substring(start,end)
                    def cd = getChildDevice("${app.id}-$devId")
                    def jsonData = (HashMap) resp.json
                    cd.dataRefresh(jsonData)
                } else {
                    log.error "Unhandled Command: '${data.cmd}'"
                }
            }
        } else if(resp.getStatus() == 401) {
            apiGet("${data.cmd}")
        }
    } catch (Exception e) {
        log.error "getApi - $e.message"        
    }
}
                   

// End API

void createChildDev(devId, devType, devName, devLoc){
    def cd
    if(!this.getChildDevice("${app.id}-$devId"))
        cd = addChildDevice("thebearmay", "Air Things Device", "${app.id}-$devId", [name: "${devName}", isComponent: true, deviceId:"$devId", label:"$devName"])
    else
        cd = getChildDevice("${app.id}-$devId")
    cd.updateDataValue("deviceId","$devId")
    cd.updateDataValue("deviceType","$devType")
    cd.updateDataValue("location","$devLoc")
    
}

void updateChild(devId){
    apiGet("devices/${devId}/latest-samples")
}

void appButtonHandler(btn) {
    switch(btn) {
          case ("authBtn"):
              state.authBtnPushed = true
              break
          case ("devBtn"):
              state.devBtnPushed = true
              break
          case ("forceCreate"):
              state.forceCreatePushed = true
              break
          default: 
              if(debugEnabled) log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}

void uninstalled(){
    def chdList = getChildDevices()
    chdList.each{
        deleteChildDevice(it.getDeviceNetworkId())
    }
}
