/*
 * 
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
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
*/
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
static String version()	{  return '0.0.2'  }

definition (
	name: 			"App and Device ID Logger", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Keeps a running list of every App and Dev ID used, even after the device/app has been deleted",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/appDevIdLogger.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"

}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        section("<h4>Settings</h4>", hideable:true, hidden: true){
			input "debugEnabled", "bool", title: "<b>Enable Debug Logging</b>", defaultValue: false, submitOnChange:true
			input "nameOverride", "text", title: "<b>New Name for Application</b>", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
			if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            input "snapFreq", "number", title:"<b>Minutes between snapshots</b>", defaultValue:60, submitOnChange:true
		}        
        section("") {
            if(minVerCheck("2.4.0.0")) {
                input "startSnaps", "button", title:"Start/Restart"
                if(state.startPressed) {
                    state.startPressed = false
                    paragraph "Start requested"
                    getSnapshot()
                }               
            } else paragraph "Must be on HE v2.4.0.0 or Higher"
        }
        section(title: "<b><u>Find App/Dev</u></b>"){
            input "aOrD", "enum", title:"App or Device", options: ["APP","DEV"], submitOnChange:true, width:4
            input "iKey", "number", title:"ID number", submitOnChange:true, width:4
            input "srch", "button", title:"Search"
            if(state.srch) {
            	state.srch = false
				try {
					buData = downloadHubFile('appDevID.json') 
			    } catch (ignore) {
        			buData = ''
                    paragraph "App/Device Data File not Found"
    			}
    			if(buData.size() > 0){
					jSlurp = new JsonSlurper()
                    foundIt = false
					oldData = jSlurp.parseText(new String(buData, "UTF-8"))                    
                    oldData.each {
                        if(debugEnabled) log.debug "$it"
                        if("${it.key}" == "$aOrD-$iKey"){
                        	paragraph "<b>Found:</b> ${it.value}      <b>Last Seen: </b>${it.lastSeen}"
                        	foundIt = true
                        }
                    }
                    if(!foundIt) paragraph "$aOrD$iKey - not found"
                }
            }
            
        }
    }
}

String getName(keyVal, appList){
    rVal = ''
    appList.each{
        if(debugEnabled) log.debug "$keyVal ${it.key}"
        if(it.key == keyVal)
        	rVal = it.value
    }
    return rVal
}

void getSnapshot() {
    if(!snapFreq) snapFreq = 60
    idList = getAppsList()
    dList = getDevList()
    idList+=dList
    try {
    	buData = downloadHubFile('appDevID.json') 
    } catch (ignore) {
        buData = ''        
    }
    if(buData.size() < 1){
        buData = JsonOutput.toJson(idList)
        uploadHubFile('appDevID.json',buData.getBytes())
        runIn(snapFreq*60, 'getSnapshot')
        return
    }
	def jSlurp = new JsonSlurper()
	oldData = jSlurp.parseText(new String(buData, "UTF-8"))
    //idL = JsonOutput.toJson(idList) //jSlurp.parseText(idList)
    idList.each{ idl ->
        foundIt = false
        oldData.each{ od ->
            if(od.key == idl.key){
                //log.debug "${od.key}<br><b>O:</b>${od.lastSeen}<br> <b>I:</b>${idl.lastSeen}"
                od.lastSeen = idl.lastSeen
                od.value = idl.value
                foundIt = true
            }
        }
        if(!foundIt)
        	oldData.add(idl)       
    }
    mergedData = JsonOutput.toJson(oldData)
	uploadHubFile('appDevID.json',mergedData.toString().getBytes())
    runIn(snapFreq*60, 'getSnapshot')
    
}

ArrayList getAppsList() {
    Map requestParams =
	[
        uri:  "http://127.0.0.1:8080",
        path:"/hub2/appsList"
	]

    httpGet(requestParams) { resp ->
        wrkList = []
        resp.data.apps.each{
            wrkMap =[key:"${it.key}",id:"${it.data.id}",value:"${it.data.name}",lastSeen:"${new Date()}"]
            wrkList.add(wrkMap)
            it.children.each{
                wrkMap =[key:"${it.key}",id:"${it.data.id}",value:"${it.data.name}",lastSeen:"${new Date()}"]
	            wrkList.add(wrkMap)
            }
        }
        
        return wrkList.sort{it.key}
    }
}

ArrayList getDevList() {
    Map requestParams =
	[
        uri:  "http://127.0.0.1:8080",
        path:"/hub2/devicesList"
	]

    httpGet(requestParams) { resp ->
        wrkList = []
        resp.data.devices.each{
            wrkMap =[key:"${it.key}",id:"${it.data.id}",value:"${it.data.name}",lastSeen:"${new Date()}"]
            wrkList.add(wrkMap)
            it.children.each{
                wrkMap =[key:"${it.key}",id:"${it.data.id}",value:"${it.data.name}",lastSeen:"${new Date()}"]
	            wrkList.add(wrkMap)
            }
        }
        
        return wrkList.sort{it.key}
    }
}


def readJsonPage(fName){
    def params = [
        uri: fName,
        contentType: "application/json",
        //textParser: false,
        headers: [
            "Connection-Timeout":600
        ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return resp.data
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read JFile Error: ${exception.message}"
        return null
    }
     
}

Boolean minVerCheck(vStr){  //check if HE is >= to the requirement
    fwTokens = location.hub.firmwareVersionString.split("\\.")
    vTokens = vStr.split("\\.")
    if(fwTokens.size() != vTokens.size())
        return false
    rValue =  true
    for(i=0;i<vTokens.size();i++){
        if(vTokens[i].toInteger() < fwTokens[i].toInteger())
           i=vTokens.size()+1
        else
        if(vTokens[i].toInteger() > fwTokens[i].toInteger())
            rValue=false
    }
    return rValue
}

def appButtonHandler(btn) {
    switch(btn) {
		case "startSnaps":
        	state.startPressed = true
        	break
        case "srch":
        	state.srch = true
        	break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
