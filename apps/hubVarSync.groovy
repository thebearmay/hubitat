/*
 * Hub Variable Synchronizer
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
 *    17Dec21    thebearmay    Add a debug shutoff timer
 *                             Add uninstalled()
 *    20Dec21    thebearmay    Add HSM and Mode options
 *    06Jan22    thebearmay    Add option to send variable updates to nodeRed
 *    10Jan22    thebearmay    change nodeRed to Post instead of Get
 *    15Jan22    thebearmay    Fix 1st time issue
 *    18Jan22    thebearmay    408 on 2-way HSM exchange
 *    19Jan22    thebearmay    Don't update HSM Status if already in desired state (debounce)
 */

static String version()	{  return '0.1.10'  }
import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition (
	name: 			"Hub Variable Sync", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Keep Hub Variables in Sync across multiple hubs",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubVarSync.groovy",
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "localInfo"
   page name: "remoteInfo"
   page name: "hsmMode"
   page name: "nodeRed"
}

mappings {
    path("/ping") {
        action: [POST: "connectPing",
                 GET: "connectPing"]
    }
    path("/setVar/:varName/:varValue") {
        action: [POST: "setVar",
                 GET: "setVar"]
    }
    path("/getVar/:varName"){
        action: [POST: "getVar",
                 GET: "getVar"]
    }
    path("/hsmStat/:varValue"){
        action: [POST: "hsmStat",
                 GET: "hsmStat"]
    }    
    path("/modeStat/:varValue"){
        action: [POST: "modeStat",
                 GET: "modeStat"]
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
                HashMap varMap = getAllGlobalVars()
                List varListIn = []
                
                varMap.each {
                    varListIn.add("$it.key")
                }
                input "varList", "enum", title: "Select variables to sync:", options: varListIn.sort(), multiple: true, required: false, submitOnChange: true
                List<String> l1 = varList
                List<String> l2 = atomicState.priorList?.value
                if((varList != null && l2 !=null && !listsEqual(l1, l2)) || (varList != null && l2 == null)){
                    manageSubscriptions()
                    atomicState.priorList = varList
                }
                if(varList) input "sendUpd", "button", title:"Send Update"
                href "remoteInfo", title: "Remote Server Information", required: false
                href "localInfo", title: "Local Server Information", required: false
                href "hsmMode", title: "HSM and Mode Settings", required: false
                href "nodeRed", title: "NodeRed Settings", required: false
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
     	    }
            section("Optional Controller Device", hideable: true, hidden: true){
                input "qryDevice", "device.VariableControllerDevice", title: "Select Controller Device:", multiple: true, required: false, submitOnChange: true
                if (qryDevice != null) 
                    ccSubscribe()
                else 
                    unsubscribe(qryDevice)               
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
            paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
            paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
            if(state.accessToken == null) createAccessToken()
            paragraph "<b>Access Token: </b>${state.accessToken}"
            input "resetToken", "button", title:"Reset Token"
        }
    }
}

def remoteInfo(){
    dynamicPage (name: "remoteInfo", title: "", install: false, uninstall: false) {
        section("Remote Hub Information", hideable: true, hidden: false){
            input "remoteAPI", "text", title:"<b>Remote Server API:</b>",submitOnChange:true
            input "token","text", title:"<b>Remote Access Token:</b>",submitOnChange:true
            if (remoteAPI != null) {
                input "checkConnection", "button", title:"Check Connection"
                try {
                    JsonSlurper jSlurp = new JsonSlurper()
                    Map resMap = (Map)jSlurp.parseText((String)atomicState.returnString)
                    paragraph "<b>Connection response status:</b>$atomicState.lastStatus $resMap.status"
                } catch (ignore) {
                    paragraph "<b>Connection parse error - response:</b>$atomicState.lastStatus $atomicState.returnString"
                }
            }
        }        
    }
}

def hsmMode(){
    dynamicPage (name: "hsmMode", title: "", install: false, uninstall: false) {
        section("HSM and Mode Settings", hideable: false, hidden: false){
            input "hsmSend", "bool", title:"Send HSM status to remote hub"
            if(hsmSend) 
                subscribe(location, "hsmStatus", "hsmSend")
            else 
                unsubscribe(location, "hsmStatus")
            input "hsmRec", "bool", title:"Allow remote hub to update HSM Status"
            input "modeSend", "bool", title: "Send Hub Mode to remote hub"
            if(modeSend) 
                subscribe(location, "mode", "modeSend")
            else
                unsubscribe(location, "mode")
            input "modeRec", "bool", title: "Allow remote hub to update Hub Mode"
            
        }
    }
}

def nodeRed(){
    dynamicPage (name: "nodeRed", title:"", install: false, uninstall: false) {
        section ("Node Red Settings", hideable: false, hidden: false){
            input "nrServer","text", title:"<b>NodeRed Server path</b> (i.e. http://192.168.x.x:1880)", submitOnChange: true
            input "nrPath", "text", title:"<b>NodeRed Endpoint path</b> (i.e. /hubVariable)", submitOnChange:true
            input "nrEnabled", "bool", title:"Enable Variable Send to NodeRed", submitOnChange:true, defaultValue:false
            input "nrTest", "button", title: "Send Test"
        }
    }
}

// Begin Device Interface
void ccSubscribe(){
    unsubscribe(qryDevice)
    subscribe(qryDevice,"varName",ccProcess)
    subscribe(qryDevice,"varCmd",ccProcess)
}

void ccProcess(evt=null) {
    dev = this.getSubscribedDeviceById(evt.deviceId)
    vName = dev.currentValue("varName")
    vCmd = dev.currentValue("varCmd")
    vValue = dev.currentValue("varValue")
    vType = dev.currentValue("varType")
    
    if (vCmd == "set"){
        success = this.setGlobalVar(vName, vValue)
        dev.varReturn(success)
    }else if (vCmd == "get"){
        if(getGlobalVar(vName)) dev.varReturn(this.getGlobalVar(vName).value)
        else dev.varReturn("Variable not found")
    }
}
//End Device Interface
         
//Begin App Communication         
void sendRemote(command) {

	Map requestParams =
	[
        uri:  "$remoteAPI$command?access_token=$token",
        requestContentType: 'application/json',
		contentType: 'application/json',
        body: []
	]

    if(debugEnabled) log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"${command.substring(1)}"]) 
}

void sendNR(vName, vValue, vType){

	Map requestParams =
	[   
       uri: "$nrServer",
       path: "$nrPath",
       contentType: "application/json", 
       body: [varName: vName, varValue: vValue, varType: vType]        
	]

    if(debugEnabled) log.debug "$requestParams"
    atomicState.debugS = "$requestParams"
    asynchttpPost("getResp", requestParams, [cmd:"$vName/$vValue/$vType"])     
}

void getResp(resp, data) {
    try {
        if(debugEnabled) log.debug "$resp.properties - ${data['cmd']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data) 
                atomicState.returnString = resp.data
            else atomicState.returnString = "{\"value\":\"Null Data Set\", \"status\":\"${resp.getStatus()}\"}"
        } else 
            atomicState.returnString =  "{\"status\":\"${resp.getStatus()}\"}"
    } catch (Exception ex) {
        atomicState.returnString = ex.message
        log.error ex.message
    }
    atomicState.lastStatus = resp.getStatus()

}

void jsonResponse(retData){
    render (contentType: 'application/json', body: JsonOutput.toJson(retData) )
}                                                                  

void connectPing() {
    if(debugEnabled) log.debug "Ping received"
    jsonResponse(status: "acknowledged")
}

void getVar() {
    if(debugEnabled) log.debug "getVar $params.varName"
    if(getGlobalVar(params.varName)) 
        jsonResponse(value: "${this.getGlobalVar(params.varName).value}")
    else
        jsonResponse(value: "Invalid variable name: $params.varName")
}

void setVar() {
    if(debugEnabled) log.debug "setVar $params.varName, $params.varValue"
    varValue = URLDecoder.decode(params.varValue)
    success = this.setGlobalVar(params.varName, varValue)
    jsonResponse(successful:"$success")
}

void hsmStat(){
    if(debugEnabled) log.debug "hsmStat $params.varValue"
    if(hsmRec && location.hsmStatus != params.varValue) {
        sendLocationEvent(name: "hsmSetArm", value: params.varValue.replace("armed","arm"), descriptionText:"Hub Variable Sync:v{$version()}")
        jsonResponse(armStatus:"$params.varValue")
    } else if(hsmRec && location.hsmStatus == params.varValue) {
        jsonResponse(armStatus:"$params.varValue")
    } else
    	jsonResponse(armStatus:"Not Authorized")
}

void modeStat(){
    if(debugEnabled) log.debug "modeStat $params.varValue"
    if(modeRec) {
        location.setMode(params.varValue)
    	jsonResponse(modeStatus:"$params.varValue")
    } else
	jsonResponse(modeStatus:"Not Authorized")
	
}

// End App Communication
         
void manageSubscriptions(){
    atomicState.priorList?.value.each{
        if(debugEnabled) log.debug "unsub $it"
        unsubscribe(location, it.toString())
    }
    removeAllInUseGlobalVar()
    varList.each{
        if(debugEnabled) log.debug "sub $it"
        var="variable:$it"
        if(debugEnabled) log.debug var
        subscribe(location,"$var", "variableSync")
        success = addInUseGlobalVar(it.toString())
        if(debugEnabled) log.debug "Added $it $success"
    }  
}

void variableSync(evt){
    if(debugEnabled) log.debug evt
    vName = evt.name.substring(evt.name.indexOf(":")+1,evt.name.length())
    varName = URLEncoder.encode(vName, "UTF-8")
    if(debugEnabled) log.debug varName
    vValue = this.getGlobalVar(vName).value.toString()
    varValue = URLEncoder.encode(vValue, "UTF-8")
    if (remoteAPI != null){
        sendRemote("/setVar/$varName/$varValue")
        pauseExecution(100)
    }
    if(nrEnabled)
        sendNR(vName, vValue, this.getGlobalVar(vName).type.toString())
}
   
Boolean listsEqual(l1,l2){
    if(debugEnabled) log.debug "L1: $l1"
    if(debugEnabled) log.debug "L2: $l2"
    if(l1.size() != l2.size()){
        if(debugEnabled) log.debug "Failed size"
        return false
    }
    for (i=0; i<l1.size(); i++) {
        if(l1[i].toString() != l2[i].toString()){
            if(debugEnabled) log.debug "Failed on item $i"
            return false
        }
    }
    if(debugEnabled) log.debug "Lists equal"
    return true    
}

void manualSend() {
    if(debugEnabled) log.debug "Manual Send"
    varList.each {
        varName = it.toString()
        if(debugEnabled) log.debug varName
        varValue = URLEncoder.encode(this.getGlobalVar(varName).value.toString(), "UTF-8")
        sendRemote("/setVar/$varName/$varValue")
        pauseExecution(100)
    }
}

void sendTest2NR(){
    sendNR("testName", "testValue")
}

void hsmSend(evt) {
    if(debugEnabled) log.debug "hsmSend $evt.value"
    sendRemote("/hsmStat/$evt.value")  
}

void modeSend(evt){
    if(debugEnabled) log.debug "hsmSend $evt.value"
    sendRemote("/modeStat/$evt.value")  
}

void appButtonHandler(btn) {
    switch(btn) {
          case "checkConnection":
              if(debugEnabled) log.debug "Ping requested"
              sendRemote("/ping")
              break
          case "sendUpd":
              manualSend()
              break
          case "resetToken":
              createAccessToken()
              break
          case "nrTest":
              sendTest2NR()
              break
          default: 
              if(debugEnabled) log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}

void uninstalled(){
	removeAllInUseGlobalVar()
}
