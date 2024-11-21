/*
 * Hub Updater
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
 *    ----         ---           --------------------------------------
 *    06Aug2024    thebearmay    v2.0.5 code reversion issue
 *                               v2.0.6 send request even if publisher is current, fix notes link
 *    09Aug2024                  v2.0.7 update msg to show current HE version at initialize (5min delay)
 *    06Nov2024					 v2.0.8 add "Switch" capability to enable HomeKit usage
 *    21Nov2024                  v2.0.9 add capability to use a list of hubs for updates
 *
 */
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

static String version()	{  return '2.0.8'  }

metadata {
    definition (
		name: "Hub Updater v2", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/hubUpdaterV2.groovy"
	) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Momentary"
		capability "Switch"
       
        attribute "msg", "string"
        attribute "notesUrl", "string"
        attribute "hubList", "string"
        
        command "sendTestMsg"
        command "subscribe",[[type:"string",description:"IP of Publisher Hub"]]
        command "unsubscribe"
        command "updateHubList",[[type:"string",description:"Comma separated list of hubs to update"]]    
    }   
}

preferences {
    input("termsAccepted","bool",title: "By using this driver you are agreeing to the <a href='https://hubitat.com/terms-of-service'>Hubitat Terms of Service</a>",required:true)
    input("updMesh","bool", title: "Push Update Request to All HubMeshed Hubs")
    input("useHubList","bool",title: "Push Update Request using Hub List Attribute")
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
}

void installed() {
    log.trace "Hub Updater v${version()} installed()"
    configure()   
}

void configure() {
    if(debugEnabled) log.debug "configure()"
    device.updateDataValue('publishingDNI', getHostAddress())
	off()
}

void initialize() {
    runIn(300,"verCheck")
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void verCheck() {
    updateAttr('msg',"Hub is on v${location.hub.firmwareVersionString}")
}

void test(){
    log.debug "Test $debugEnabled"
}

void updateHubList(hList){
    if(hList == null) {
        device.deleteCurrentState('hubList')
        return
    }
    updateAttr("hubList",hList.replace(" ",""))
}

def on(){
    push()
    updateAttr("switch","on")
    off()
}

def off(){
    updateAttr("switch","off")
}

def push(){
    log.info "Firmware Update Requested"
    if(!termsAccepted) {
        updateAttr("msg", "Please accept terms and conditions first")
        return
    }    
    updateAttr ("msg", "Update Requested at ${new Date()}")
    params = [
        uri: "http://127.0.0.1:8080",
        path:"/hub/cloud/checkForUpdate",
        timeout: 10
    ]
    asynchttpGet("getUpdateCheck", params)    
}

void getUpdateCheck(resp, data) {
    if(updMesh) {
        updateMesh()
    } else if(useHubList) {
        updateFromList()
    }

    if(debugEnable) log.debug "update check: ${resp.status}"
    try {
        if (resp.status == 200) {
            def jSlurp = new JsonSlurper()
            if (debugEnabled) log.debug "${resp.data}"
            Map resMap = (Map)jSlurp.parseText((String)resp.data)
            if(resMap.status == "NO_UPDATE_AVAILABLE")
                updateAttr("msg","Hub is Current")
            else {
                updateAttr("msg","${resMap.version} requested")
                updateAttr("notesUrl","<a href='${resMap.releaseNotesUrl}'>Release Notes</a>")
                httpGet("http://127.0.0.1:8080/hub/cloud/updatePlatform"){ response -> 
                    updateAttr("msg", "${response.data}")
                }
            }
        }
    }catch(ignore) {
        updateAttr("msg", "Hub is Current")
    }
}

void updateMesh(){
    params =  [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubMeshJson",
        headers: [
            "Connection-Timeout":600
        ]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getHubMesh", params)
}

@SuppressWarnings('unused')
void getHubMesh(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable)
                log.debug resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
            if (debugEnable)
                log.debug "${h2Data.hubList}"
            h2Data.hubList.each{
                log.info "Requesting update of ${it.ipAddress}"
                sendMsg(it.ipAddress,"Update Requested")
            } 
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on Hubmesh request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

void updateFromList(){
    hubList = device.currentValue("hubList")
    hList = hubList.split(',')
    hList.each{
        log.info "Requesting update of ${it}"
        sendMsg(it,"Update Requested")
    } 
}
    
def updated(){
	log.trace "updated()"
	if(debugEnabled) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def subscribe(ip){
    hostHex = getHostAddress(ip)
    device.setDeviceNetworkId(hostHex)
    device.updateDataValue('subscriptionDNI',hostHex)
}

def unsubscribe(){
    newDNI=UUID.randomUUID().toString()
    device.removeDataValue('subscriptionDNI')
    device.setDeviceNetworkId(newDNI)
}


def getHostAddress(ip=''){
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

def sendTestMsg(){
    if(useHubList){
        hubList = device.currentValue("hubList")
        hList = hubList.split(',')
        hList.each{
            log.info "Testing Message to ${it}"
            sendMsg(it,"Test Message")
        }       
        return
    }
    params =  [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub2/hubMeshJson",
        headers: [
            "Connection-Timeout":600
        ]           
    ]
    
    if(debugEnable)log.debug params
    asynchttpGet("getHubMeshData", params)   
}

@SuppressWarnings('unused')
void getHubMeshData(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable)
                log.debug resp.data
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
           if (debugEnable)
                log.debug "${h2Data.hubList}"
            h2Data.hubList.each{
                log.info "Testing Message to ${it.ipAddress}"
                sendMsg(it.ipAddress,"Test Message")
            } 
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} on Hubmesh request"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}

def sendMsg(ip, msg){
    def bodyText = JsonOutput.toJson(['msg':"$msg"])
	Map requestParams =
	[
        uri:  "http://$ip:39501",
		contentType: 'application/json',
        body: "$bodyText"
	]

    if(debugEnabled)
        log.debug "$requestParams"
    asynchttpPost("getResp", requestParams) 
}

def getResp(resp, data){
    if(debugEnabled) log.debug "Send response: ${resp.properties}"
}

void parse(String msgIn) {
  // Called everytime a POST message is received from Port 39501 if DNI matches Hex of sender
    try {
        Map data = parseLanMessage(msgIn);
        if(data.json.msg =="Update Requested" )
            push()
        else
            updateAttr("msg", data.json.msg)
    } catch (e) {
        log.error "$e"
   
    }
}
