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
 *    22Aug2023    thebearmay    Enable multi-attribute and channel selection
 *    24Aug2023    thebearmay    Remove the use of the substring, reset attributes & channels when device list changes
 *                               Refined the reset logic for attributes and channels
 *                               Send Channel Name (attribute name) and Device number
 *
*/
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '0.0.7'  }

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
                input "apiKey", "password", title: "<b>API Key from <a href='https://www.constantgraph.com/account' target='_blank'>ConstantGraph</a></b>", description:"Enter API Key", submitOnChange: true, width:4
                input "debugEnabled", "bool", title:"<b>Enable Debug</b>", submitOnChange:true, width:4
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
            
            }
            section("<h3>Device Selection</h3>", hideable: true, hidden: false){
                if(!state.devHold) {
                    if(devSelected) 
                        state.devHold = "$devSelected"
                    else
                        state.devHold=[]
                }
                input "devSelected", "capability.*",title:"Select device to share", submitOnChange:true,multiple:true
                unsubscribe()
                if(devSelected){
                    devSelected.each{ d ->
                        input "attrib-${d.name}", "enum", title: "Select Attribute to report for $d", options: d.properties.currentStates.name.sort(), submitOnChange: true, multiple: true, width:4
                    }
                    if("$devSelected"!="${state.devHold}"){
                        state.devHold = "$devSelected"
                        settings.each{ s1 ->
                            if("$s1".indexOf("attrib-") >  -1 ) {
                                devName = "${s1.key}".split("-")[1]
                                validDev = false
                                devSelected.each { d2 -> 
                                    if ("${d2.name}" == "$devName") {
                                        validDev = true
                                    }
                                }
                                if(!validDev) app.removeSetting(s1.key)
                            }
                            if("$s1".indexOf("channel-") >  -1){
                                devID = "${s1.key}".split("-")[1]
                                validDev = false
                                devSelected.each { d2 -> 
                                    if ("${d2.id}" == "$devID") {
                                        validDev = true
                                    }
                                }
                                if(!validDev) app.removeSetting(s1.key)                                
                            }
                        }
                    }
                    settings.each{ s ->
                        //log.debug "$s"
                        if("$s".indexOf("attrib-") >  -1) {
                            dName = "${s.key}".split("-")[1]
                            //dName= dName.substring(0,dName.indexOf("="))
                            devSelected.each { d -> 
                                if ("${d.name}" == "$dName") {
                                    s.value.each { v ->
                                        subscribe(d,v,"sendDataEvt")
                                        input "channel-${d.id}-$v","number", title:"Select Channel for $d-$v",submitOnChange:true, width:4
                                    }
                                }
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
                        if("$s".indexOf("attrib-") >  -1 || "$s".indexOf("channel-") >  -1 ) {
                            app.removeSetting(s.key)
                        }
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

void sendDataEvt(evt){
    if(debugEnabled) log.debug "evt : <br>${evt.properties}"
    channel = 0
    settings.each{ s ->
        if("$s".indexOf("channel-") >  -1) {
            sSplit = "${s.key}".split("-")
            if(debugEnabled) log.debug "$sSplit"
            if(sSplit[1] == "${evt.deviceId}" && sSplit[2] == "${evt.name}")
                channel = s.value            
        }
    }
    if(debugEnabled)log.debug "Channel: $channel Attribute: ${evt.name}"
    dataMap = [app:"${app.getLabel()}", version: "${version()}", channels:[[id:"$channel", v:"${evt.value}", Name:"${evt.name}", Device:"${evt.deviceId}"]]]
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
    asynchttpPost("dataReturn",requestParams)    
}

def dataReturn(resp, data){
    if(debugEnabled) 
    	log.debug "dataReturn:<br>${resp.properties}}"
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
