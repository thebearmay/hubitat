/*
 * Constant Graph Demo
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
 *    21Aug2023    thebearmay    Enable multi-device
 *
*/
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '0.0.3'  }

definition (
	name: 			"ConstantGraph Demo", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Constantgraph Demo",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/constantGraphDemo.groovy",
	oauth: 			true,
    installOnOpen:  true,
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
                input "apiKey", "string", title: "<b>API Key from <a href='https://www.constantgraph.com/account' target='_blank'>ConstantGraph</a></b>", description:"Enter API Key", submitOnChange: true, width:4
                input "debugEnabled", "bool", title:"<b>Enable Debug</b>", submitOnChange:true, width:4
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
            
            }
            section("<h3>Device Selection</h3>", hideable: true, hidden: false){
                input "devSelected", "capability.*",title:"Select device to share", submitOnChange:true,multiple:true
                unsubscribe()
                if(devSelected){                    
                    devSelected.each{ d ->
                        input "attrib-${d.name}", "enum", title: "Select Attribute to report for $d", options: d.properties.currentStates.name.sort(), submitOnChange: true, multiple: false, width:4
                    }
                    settings.each{ s ->
                        //log.debug "$s"
                        if("$s".indexOf("attrib-") >  -1) {
                            dName = "$s".split("-")[1]
                            dName= dName.substring(0,dName.indexOf("="))
                            devSelected.each { d -> 
                                if ("${d.name}" == "$dName")
                                    subscribe(d,s.value,"sendDataEvt")
                            }
                        }
                    }
/*                    input "test", "button", title:"Test Send Data"
                    if(state.testSend == true){
                        state.testSend == false
                        sendData([evt:[d)
                    }
*/
                } else {
                    settings.each{ s ->
                        if("$s".indexOf("attrib-") >  -1) {
                            app.removeSetting(s.key)
                        }
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

void sendDataEvt(evt){
    if(debugEnabled) log.debug "evt : <br>${evt.properties}"
    dataMap = [app:"Hubitat Demo", version: "${version()}", channels:[[id:"${evt.deviceId}", v:"${evt.value}", name:"${evt.name}"]]]
    def bodyText = JsonOutput.toJson(dataMap)
    if(debugEnabled) log.debug "$bodyText"
    Map requestParams =
	[
        uri: "https://data.mongodb-api.com/app/constantgraph-iwfeg/endpoint/http/data",
        "requestContentType" : "application/json",
        "contentType": "application/json",
        headers: [
            "X-Api-Key" :"$apiKey"
        ],
        body: "$bodyText",
        timeout:100
	]
    if(debugEnabled) 
        log.debug "$requestParams"
    asynchttpPost("dataReturn",requestParams,[retry:retry])    
}

def dataReturn(resp, data){
    if(debugEnabled) 
    	log.debug "dataReturn:<br>${resp.properties}<br>${data['retry']}"
}


void appButtonHandler(btn) {
    switch(btn) {
        case "test":
            state.testSend = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
