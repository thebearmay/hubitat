/*
 * Hub Variable Synchronizer
 */

static String version()	{  return '0.0.0'  }
import groovy.transform.Field
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition (
	name: 			"Hub Variable Sync", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Keep Hub Variables in Sync across Hub Mesh",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubVarSync.groovy",
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
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
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
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
                if(!listsEqual(l1, l2)){
//                    paragraph "${atomicState.priorList?.value} <br/> $varList"
                    manageSubscriptions()
                    atomicState.priorList = varList
                } 

     	    }
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
            section("Local Hub Information", hideable: true, hidden: true){
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
                input "resetToken", "button", title:"Reset Token"
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

def ccSubscribe(){
    unsubscribe(qryDevice)
    subscribe(qryDevice,"varName",ccProcess)
    subscribe(qryDevice,"varCmd",ccProcess)
}
// Begin Device Interface
def ccProcess(evt=null) {
    dev = this.getSubscribedDeviceById(evt.deviceId)
    vName = dev.currentValue("varName")
    vCmd = dev.currentValue("varCmd")
    vValue = dev.currentValue("varValue")
    vType = dev.currentValue("varType")
/*
    if(vCmd == "create") {
        if (!this.getGlobalVar(vName))
            success = this.createGlobalVar(vName, vValue, vType)
        else
            success = "Already existed"
        dev.varReturn(success)
    }else if (vCmd == "delete"){
        success = this.deleteGlobalVar(vName)
        dev.varReturn(success)
    }else */
    
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

    log.debug "$requestParams"
    asynchttpPost("getResp", requestParams, [hubName:"$location.hub.name"]) 
}

void getResp(resp, data) {
    try {
        log.debug "$resp.properties - $resp.data - ${data['hubName']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data != null || resp.data <= " ") 
                atomicState.returnString = resp.data
            else atomicState.returnString = "Null Data Set Returned"
        } else 
            atomicState.returnString =  "Error: ${resp.getStatus()}"
    } catch (Exception ex) {
        atomicState.returnString = ex.message
    }
    atomicState.lastStatus = resp.getStatus()

}

void jsonResponse(retData){
    render (contentType: 'application/json', text: JsonOutput.toJson(retData) )
}                                                                  

void connectPing() {
    log.debug "Ping received"
    jsonResponse(status: "acknowledged")
}

void getVar() {
    log.debug "getVar $params.varName"
    if(getGlobalVar(params.varName)) 
        jsonResponse(value: "${this.getGlobalVar(params.varName).value}")
    else
        jsonResponse(value: "Invalid variable name: $params.varName")
}

void setVar() {
    log.debug "setVar $params.varName, $params.varValue"
    success = this.setGlobalVar(params.varName, params.varValue)
    jsonResponse(successful:"$success")
}
// End App Communication
         
void manageSubscriptions(){
    atomicState.priorList.value.each{
//        log.debug "unsub $it"
        unsubscribe(location, it.toString())
    }
    removeAllInUseGlobalVar()
    varList.each{
//        log.debug "sub $it"
        var="variable:$it"
        log.debug var
        subscribe(location,"$var", "variableSync")
        success = addInUseGlobalVar(it.toString())
//        log.debug "Added $it $success"
    }  
}

void variableSync(evt){
    log.debug evt
    varName = evt.name.substring(evt.name.indexOf(":")+1,evt.name.length())
    log.debug varName
    sendRemote("/setVar/$varName/${this.getGlobalVar(varName).value}")
}
   
Boolean listsEqual(l1,l2){
//    log.debug "L1: $l1"
//    log.debug "L2: $l2"
    if(l1.size() != l2.size()){
//        log.debug "Failed size"
        return false
    }
    for (i=0; i<l1.size(); i++) {
        if(l1[i].toString() != l2[i].toString()){
//            log.debug "Failed on item $i"
            return false
        }
    }
//    log.debug "Lists equal"
    return true    
}

def appButtonHandler(btn) {
    switch(btn) {
          case "checkConnection":
              log.debug "Ping requested"
              sendRemote("/ping")
              break
          case "resetToken":
              createAccessToken()
              break
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

def intialize() {

}
