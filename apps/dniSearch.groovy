/*
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date          Who          Description
 *    ----------   ------------  ------------------------------------------------
 */
    
static String version()	{  return '0.0.1'  }

import groovy.transform.Field
import groovy.json.JsonSlurper

definition (
	name: 			"DNI Search", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Search Devices for port 39501 using IP and MAC",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/apps/dniSearch.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 
preferences {
    page name: "mainPage"

}
mappings {
/*   path("/store") {
        action: [POST: "store",
                 GET: "store"]
    }
*/
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:''){
            input "ipAddr", "string", title:"IP Address to Search for", submitOnChange: true
            input "macAddr", "string", title:"MAC Address to Search for", submitOnChange: true
            if(ipAddr || macAddr){
				input "srch", "button", title: "Search"
            }
            if(state?.srchBtnPush) {
                state.srchBtnPush = false
				paragraph "<b>Matching Devices</b>"
                getDevices().each{
                    paragraph "$it"
                }
            }
        }
    }
}

String getHostAddress(ip=''){
    if(!ip)
        ipTokens = location.hub.localIP.split('\\.')
    else
        ipTokens = ip.split('\\.')     
    hexStr=''
    ipTokens.each{
        wStr=Integer.toString(it.toInteger(),16).toUpperCase()
        if(wStr.size() == 1)
            hexStr+="0$wStr"
        else
            hexStr+="$wStr"        
    }
    
    return hexStr
}

def appButtonHandler(btn) {
	switch(btn) {
		case "srch":
			state.srchBtnPush = true
            break
		default: 
			log.error "Undefined button $btn pushed"
            break
	}
}

ArrayList getDevices(){
	params = [
		uri    : "http://127.0.0.1:8080",
		path   : "/hub2/devicesList",
		headers: [
			"Connection-Timeout":600,
            Accept:"application/json"
		]           
	]
	ArrayList devMatch = []
    String ip
    if(ipAddr) 
    	ip = getHostAddress(ipAddr)
    else 
        ip = 'not provided'
    if(macAddr && macAddr.contains(":"))
       macAddr = macAddr.replace(":","")
    else 
        macAddr = 'not provided'
    
    httpGet(params) { resp ->
        devJson = resp.data 
        devJson.devices.each{
            if(it.data.dni == ip || it.data.dni == macAddr){
            	if(it.data.name)
            		dName = it.data.name
            	else
                    dName = it.data.secondaryName
            	if(it.data.id) 
            		devId = it.data.id
            	else 
                    devId = "ID error"
            	devMatch.add("ID: ${devId} Name: ${dName}")
            }
        }
	}
    return devMatch
}
            
