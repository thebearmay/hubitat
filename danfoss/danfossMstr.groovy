/*
 * Danfoss
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
 *    26Jul2022   thebearmay    additional verifications
*/

static String version()	{  return '0.0.1'  }

import java.text.SimpleDateFormat
import java.util.Date
import groovy.json.JsonSlurper
import groovy.transform.Field

definition (
	name: 			"Danfoss Master", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Danfoss Intefration ",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/apps/xxxx.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "options"
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
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 
            section("") {
                input "apiKey", "text", title: "<b>Danfoss API Key</b>", submitOnChange:true, required:false, width:4
                input "apiSecret", "text", title: "<b>Danfoss API Secret</b>", submitOnChange:true, required:false, width:4
                input "checkCred", "button", title: "Check Credentials"
                if(state?.loginCheck == true) {
                    danfossLogin()
                    state.loginCheck = "false"
                }
                paragraph state.authToken
                input "devCheck", "button", title:"Get Devices"
                if(state?.getDev == true){
                    getDevices()
                    state.getDev = "false"                    
                }
                paragraph state.devResp
            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }        
    }
}

def danfossLogin(){
    String encodedString = "$apiKey:$apiSecret".bytes.encodeBase64().toString();
    log.debug "$encodedString"
    try{
        params = [
					uri: "https://api.danfoss.com/oauth2/token",
                    headers: [
                        Authorization: "Basic $encodedString",
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[grant_type:"client_credentials"]
				]
        log.debug "$params"
        httpPost(params){ resp ->
            log.debug "$resp.data"

            state.authToken = resp.data.access_token
		}
    }catch (e){
        log.error "Error logging in: ${e}"
    }
}

def getDevices() {
    danfossLogin()
    try{
        params = [
					uri: "https://api.danfoss.com/ally/devices",
                    headers: [
                        Authorization: "Bearer ${state.authToken}",
                        Accept: "application/json"
                        ]

				]
        log.debug "$params"
        httpGet(params){ resp ->
            log.debug "$resp.data"
            state.devResp = resp.data
		}
    }catch (e){
        log.error "Error getting devices: ${e}"
    }
}


def appButtonHandler(btn) {
    switch(btn) {
        case "checkCred":
            state.loginCheck = true
            break
        case "devCheck":
            state.getDev = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
