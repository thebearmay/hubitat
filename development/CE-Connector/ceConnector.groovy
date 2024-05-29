/*
 * CE Connector
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
 *    31Jul2023    thebearmay    New Code
 *    
*/

static String version()	{  return '1.0.14'  }
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.security.MessageDigest

definition(
    name:             "CE Connector", 
    namespace:        "thebearmay", 
    author:           "Jean P. May, Jr.",
    description:      "Config sync, connector and companion App Community Edition",
    importUrl:        "",
    installOnOpen:    true,
    oauth:            true,
    iconUrl:          "",
    iconX2Url:        ""
)

preferences {
    page name: "mainPage"
    page name: "deviceMgmt"
}

mappings {
    path("/saveconfig") {
        action: [POST: "saveConfig"]
    }
    path("/getconfig") {
        action: [POST: "getConfig"]
    }
    path("/checkconfighash"){
        action: [POST: "checkHash"]
    }
    path("/getguestlist") {
        action: [POST: "getGuestList"]
    }
    path("/updateguest"){
        action: [POST: "updateGuest"]
    }
    path("/deleteguest"){
        action: [POST: "deleteGuest"]
    }
    path("/saveconfigbackuplist"){
        action: [POST: "createBackup"]
    }
    path("/getconfigbackuplist"){
        action: [POST: "getBackup"]
    }
    path("/devices/all"){
        action: [POST: "deviceListDetailed"]
    }
    path("/devices/:devId"){
        action: [POST: "deviceDetails"]
    } 
    path("/devices/:devId/commands"){
        action: [POST: "deviceCommandList"]
    }
    path("/devices/:devId/events"){
        action: [POST: "deviceEventList"]
    }
    path("/devices/:devId/capabilities"){
        action: [POST: "deviceCapabilities"]
    }
    path("/devices/:devId/:cmd/delay/:seconds"){
        action: [POST: "deviceIssueCommandDelay"]
    }
    path("/devices/:devId/:cmd/:secValue/delay/:seconds"){
        action: [POST: "deviceIssueCommandDelay"]
    }
    path("/devices/:devId/:cmd/:secValue"){
        action: [POST: "deviceIssueCommand"]
    }  
    path("/devices/:devId/:cmd"){
        action: [POST: "deviceIssueCommand"]
    } 
    path("/devices"){
        action: [POST: "deviceList", GET: "deviceList"]
    }
    path("/hsm"){
        action: [POST: "getHsm"]
    }
    path("/modes"){
        action: [POST: "getMode"]
    }    
}

void installed() {
	if(debugEnabled) log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 
            section("<span style='color:blue;font-weight:bold;font-size:x-Large'>${app.getLabel()}  <span style='font-size:x-small'>v${version()}</span></span>") {
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false, width:4
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
            }
          
            section("<span style='color:blue;font-weight:bold'>User Tokens</span>", hideable: true, hidden: true) {          
                if(state.admToken == null) state.admToken = generateRandomToken(6)
                
                paragraph "<b>'Admin' User Token: </b>${state.admToken}"
                input ("genUT", "button", title:"Reset 'Admin' User Token")
                if(state.getUT == true){
                    state.getUT = false
                    state.admToken = generateRandomToken(6)
                }
                int countOfGuestTokens = 0
                htmlOut = "<style>tr:nth-child(odd){background-color: #D6EEEE;}th{color:white;background-color:blue;}table,th,td{padding:.25em;border: 1px solid black;border-collapse: collapse;}</style><table><tr><th>Token</th><th>Name</th><th>Permissions</th><th>Dash ID</th></tr>"
                if(state?.guest == null) state.guest = []
                sortedGuest = state.guest.sort { e1, e2 -> e1.key <=> e2.key }*.key
                sortedGuest.each{
                    countOfGuestTokens++
                    htmlOut+="<tr><td>${it.value}</td><td>${state.guest[it].name}</td><td>${state.guest[it].permissions}</td><td>${state.guest[it].defaultDashId}</td></tr>"                       
                }
                htmlOut+="</table>"
                paragraph "<b>'Guest' User Token/s: </b>${countOfGuestTokens < 1 ? 'none' : countOfGuestTokens}"
                if(countOfGuestTokens > 0) paragraph "$htmlOut"
            }
            
            section("<span style='color:blue;font-weight:bold'>Hub Security</span>", hideable: true, hidden: true){
                if(state.accessToken == null) createAccessToken()
                tDefault = state.accessToken
                aDefault = getFullLocalApiServerUrl()
                
                input "sourceSecurity", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4
                if (sourceSecurity) { 
                    input("sUsername", "string", title: "Hub Security Username", required: false)
                    input("sPassword", "password", title: "Hub Security Password", required: false)
                }
            }
            
            section("<span style='color:blue;font-weight:bold'>Endpoint Information</span>", hideable: true, hidden: true){
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>AppID: </b>${app.id}"
                paragraph "<b>Access Token: </b>${state.accessToken}"
                paragraph "<b>Cloud Token: </b>${app.getHubUID()}"
                createQR()
                paragraph "<iframe width='300' height='300' src = '/local/ceConnQr.html'></iframe>"
                paragraph "<hr>"
                input "resetToken", "button", title:"Reset Access Token"
                paragraph "<b>Local Server:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server: </b>${getFullApiServerUrl()}"
            }    
            
            section(""){
                href "deviceMgmt", title: "<span style='color:blue;font-weight:bold'>Device Management</span>", required: false
            }
            
            section("<span style='color:blue;font-weight:bold'>Reset Application Name</span>", hideable: true, hidden: true){
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

def deviceMgmt(){
    dynamicPage (name: "deviceMgmt", title: "<span style='color:blue;font-weight:bold'>Device Management</span>", install: false, uninstall: false) {
        section(""){
            input "assignedDevices", "capability.*", title: "Available Devices:", multiple: true, required: false, submitOnChange: true
        }
    }
}

def saveConfig(){
    jsonData = (HashMap) request.JSON
    if(debugEnabled) log.debug "saveConfig - ${jsonData}"
    if(getPermissions(jsonData?.token) == "rw"){
        fData = JsonOutput.toJson(jsonData).getBytes("UTF-8")
        uploadHubFile("ceConnConf.txt",fData)
        retHash = sha256("${jsonData.jsonCustomThemes}${jsonData.jsonDashCfg}","s4ltyS3cre7K3y")
        state.lastHash=retHash
        jsonText = JsonOutput.toJson([isError:false, staus:"ok", message:"success",confighash:"$retHash"])
        render contentType:'application/json', data: "$jsonText", status:200 
    } else {
        def bodyText = JsonOutput.toJson([isError:false, staus:"fail", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:200    
    }               
}

def createBackup(){
    jsonData = (HashMap) request.JSON
    if(getPermissions(jsonData?.token) == "rw"){
        fData = downloadHubFile("ceConnConf.txt")
        uploadHubFile("ceConnConfBck.txt",fData)
        jsonText = JsonOutput.toJson([isError:false, staus:"ok", message:"success"])
        render contentType:'application/json', data: "$jsonText", status:200 
    } else {
        def bodyText = JsonOutput.toJson([isError:false, staus:"fail", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:200   
    }               
}

def getConfig(){
    jsonData = (HashMap) request.JSON
    if(debugEnabled) log.debug "getConfig - ${jsonData}"
    
    if(getPermissions(jsonData?.token) == "rw" || getPermissions(jsonData?.token) == "ro") {
        Map rMap = [jsonCustomThemes:[:],jsonDashCfg:[:]]
        try {
            byte[] rData = downloadHubFile("ceConnConf.txt")                                
            String rFile = new String(new String(rData))
            JsonSlurper jSlurp = new JsonSlurper()
            rMap = (Map)jSlurp.parseText(rFile)
        } catch (Exception e) {}
        rMap.put("permissions",getPermissions(jsonData?.token))
        rMap.put("confighash",sha256("${rMap.jsonCustomThemes}${rMap.jsonDashCfg}","s4ltyS3cre7K3y"))    
        rMap.put("isError",false)
        rMap.put("status","ok")
        rMap.put("message","success")
        def bodyText = JsonOutput.toJson(rMap)
        render contentType:'application/json', data: "$bodyText", status:200 
    } else {     
        def bodyText = JsonOutput.toJson([isError:false, staus:"fail", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:200   
    } 
}

def getBackup(){
    jsonData = (HashMap) request.JSON
    if(getPermissions(jsonData?.token) == "rw") {
        Map rMap = [configbackuplist:[:]]
        try {
            byte[] rData = downloadHubFile("ceConnConfBck.txt")                                
            String rFile = new String(new String(rData))
            JsonSlurper jSlurp = new JsonSlurper()
            rMap = (Map)jSlurp.parseText(rFile)
        } catch (Exception e) {}        
        rMap.put("isError",false)
        rMap.put("status","ok")
        rMap.put("message","success")
        def bodyText = JsonOutput.toJson(rMap)
        render contentType:'application/json', data: "$bodyText", status:200 
    } else {
        def bodyText = JsonOutput.toJson([isError:false, staus:"fail", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:200   
    }      
}

def checkHash(){
    rMap = [status:"ok", confighash:"$state.lastHash"]
    def bodyText = JsonOutput.toJson(rMap)
    render contentType:'application/json', data: "$bodyText", status:200 
}

def updateGuest(){
    jsonData = (HashMap) request.JSON
    if(debugEnabled) 
        if(debugEnabled) log.debug "updateGuest - ${jsonData}"
    if(getPermissions(jsonData?.token) == "rw"){
        if(!state.guest) state.guest = [:]
        if(jsonData.guesttoken) {
            state.guest["${jsonData.guesttoken}"]=[:]
            state.guest["${jsonData.guesttoken}"]?.name = jsonData.name
            state.guest["${jsonData.guesttoken}"]?.defaultDashId = jsonData.defaultDashId
            state.guest["${jsonData.guesttoken}"]?.permissions = jsonData.permissions
            def bodyText = JsonOutput.toJson([status:"ok"])
            render contentType:'application/json', data: "$bodyText", status:200 
        } else {
            def bodyText = JsonOutput.toJson([staus:"fail", message:"Guest Token Missing"])
            render contentType:'application/json', data: "$bodyText", status:400 
        }
    } else {
        def bodyText = JsonOutput.toJson([staus:"fail", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:403   
    }
}

def getGuestList(){
    rMap = [:]
    if(!state.guest) state.guest = [:]
    state.guest.each{
        if(debugEnabled) log.debug "${it.properties}"
        tMap = [:]
        tMap.put("name","${it.value.name}")
        tMap.put("permissions","${it.value.permissions}")
        tMap.put("defaultDashId","${it.value.defaultDashId}")
        rMap.put("${it.key}",tMap)                 
    }
    zMap = [status:"ok"]
    zMap+=[guestList:rMap]
    def bodyText = JsonOutput.toJson(zMap)
    render contentType:'application/json', data: "$bodyText", status:200 
}

def deleteGuest(){
    jsonData = (HashMap) request.JSON
    if(getPermissions(jsonData?.token) == "rw"){
        tempMap = state.guest
        state.guest = [:]
        tempMap.each{
            if(jsonData.guesttoken != it.key){
                state.guest["${it.key}"]=[:]
                state.guest["${it.key}"].name = it.value.name
                state.guest["${it.key}"].defaultDashId = it.value.defaultDashId
                state.guest["${it.key}"].permissions = it.value.permissions 
            }
        }
        def bodyText = JsonOutput.toJson([status:"ok"])
        render contentType:'application/json', data: "$bodyText", status:200 
    } else {
        def bodyText = JsonOutput.toJson([staus:"invalid", message:"Access Denied"])
        render contentType:'application/json', data: "$bodyText", status:403   
    }
}

def deviceList(){
    devMap = []
    assignedDevices.each{
        devMap=devMap+[id:"${it.properties.id}",name:"${it.properties.name}",label:"${it.properties.label}",room:"${it.properties.roomName}"]   
    }
    def bodyText = JsonOutput.toJson(devMap)
    render contentType:'application/json', data: "$bodyText", status:200 
}

def deviceListDetailed(){
    devMap = []
    assignedDevices.each{
        tMap=[id:"${it.properties.id}",name:"${it.properties.name}",label:"${it.properties.label}",type:"${it.device.deviceTypeName}", date:"${it.properties.lastActivity}",model:"${it.device.properties.data.model}",manufacturer:"${it.device.properties.data.manufacturer}",room:"${it.properties.roomName}"]
        aMap=[:]
        it.currentStates.each{
            aMap.put("${it.name}","${it.value}")
        }
        cMap=[]
        it.properties.supportedCommands.each{
            cMap+=[command:"${it}"]
        }
        tMap+=[attributes:aMap]+[commands:cMap]           
        devMap=devMap+tMap   
    }
    def bodyText = JsonOutput.toJson(devMap)
    render contentType:'application/json', data: "$bodyText", status:200     
}

def deviceDetails(){
    assignedDevices.each{
        if(it.properties.id == params.devId) {
            tMap=[id:"${it.properties.id}",name:"${it.properties.name}",label:"${it.properties.label}",type:"${it.device.deviceTypeName}",room:"${it.properties.roomName}",date:"${it.properties.lastActivity}",model:"${it.device.properties.data.model}",manufacturer:"$it.device.properties.data.manufacturer}"]
            aMap=[:]
            it.currentStates.each{
                aMap.put("${it.name}","${it.value}")
            }
            cMap=[]
            it.properties.supportedCommands.each{
                cMap+=[command:"${it}"]
            }
            tMap+=[attributes:aMap]+[commands:cMap]           
        }
    }
    def bodyText = JsonOutput.toJson(tMap)
    render contentType:'application/json', data: "$bodyText", status:200     
}

def deviceCommandList(){
    assignedDevices.each{
        if(it.properties.id == params.devId) {
            cMap=[]
            it.properties.supportedCommands.each{
                cMap+=[command:"${it}"]
            }         
        }
    }
    def bodyText = JsonOutput.toJson(cMap)
    render contentType:'application/json', data: "$bodyText", status:200    
}

def deviceEventList(){
    assignedDevices.each{d->
        if(d.properties.id == params.devId) { 
            eList = d.events()
            eMap = []
            eList.each {
                eMap += [device_id:"${d.properties.id}",name:"${d.properties.name}",label:"${d.properties.label}",stateName:"${it.name}",value:"${it.value}",date:"${it.getDate()}",isStateChange:"${it.isStateChange}",source:"${it.source}"]
            }
        }
    }
    def bodyText = JsonOutput.toJson(eMap)
    render contentType:'application/json', data: "$bodyText", status:200    
}

def deviceCapabilities(){
    if(debugEnabled) log.debug "Capabilities"
    assignedDevices.each{
        if(it.properties.id == params.devId) {
            tMap=[capabilities:"${it.capabilities}"]//+[attributes:aMap]          
        }
    }
    def bodyText = JsonOutput.toJson(tMap)
    render contentType:'application/json', data: "$bodyText", status:200     
}


def deviceIssueCommandDelay(){
    assignedDevices.each{d->
        if(d.properties.id == params.devId) {
            if(debugEnabled) log.debug "$d ${params.cmd}"
            runIn(params.seconds.toInteger(),"commandDelay",[overwrite:false,data:[id:"${params.devId}",cmd:"${params.cmd}",secVal:"${params.secValue}"]])
        }
    }
    def bodyText = JsonOutput.toJson([status:"ok"])
    render contentType:'application/json', data: "$bodyText", status:200 
}

def commandDelay(data) {
    if(debugEnabled) log.debug "${data} ${data['id']} ${data['cmd']} ${data['secVal']}"
    assignedDevices.each{d->
        if(debugEnabled) log.debug "${d.properties.id} == ${data['id']}"
        if(d.properties.id == data.id) {
            if(debugEnabled) log.debug "$d ${data.cmd}"
            if(data.secVal == "null") data.secVal = null
            d."${data.cmd}"(data.secVal)
        }
    }
}

def deviceIssueCommand(){
    assignedDevices.each{d->
        if(d.properties.id == params.devId) {
            if(debugEnabled) log.debug "$d ${params.cmd}"
            d."${params.cmd}"(params.secValue)
        }
    }
    def bodyText = JsonOutput.toJson([status:"ok"])
    render contentType:'application/json', data: "$bodyText", status:200 
}

def getHsm(){
    def bodyText = JsonOutput.toJson([hsm:"${location.hsmStatus}"])
    render contentType:'application/json', data: "$bodyText", status:200 
}

def getMode(){
    locationMap =[]
    location.modes.each{
        if("$it" == location.mode) 
            actMode = "true"
        else
            actMode = "false"
        locationMap+=[name:"$it", active:"$actMode"]
    }
    def bodyText = JsonOutput.toJson(locationMap)
    render contentType:'application/json', data: "$bodyText", status:200     
}

private generateRandomToken(int length) {
    def characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    def token = ""

    // Create a new instance of Random class
    def random = new Random()

    // Generate each character of the password
    (1..length).each {
        def randomIndex = random.nextInt(characters.length())
        def randomCharacter = characters.charAt(randomIndex)
        token += randomCharacter
    }

    return token
}

private String getPermissions(token) {
    if(token == state.admToken)
        return "rw"
    else
    if(state.guest["$token"])
        return state.guest["$token"]?.permissions
    else
        return "fail"
}

private String sha256(String message, String salt) {
    MessageDigest md = MessageDigest.getInstance("SHA-256")
    md.update(salt.getBytes());
    byte[] digest = md.digest(message.getBytes())
    StringBuffer hexString = new StringBuffer()
    for (int i = 0;i<digest.length;i++) {
        hexString.append(Integer.toHexString(0xFF & digest[i]))
    }
    
    return hexString.toString()
}

void createQR(){
    String ipAddr = location.hub.localIP
    String appId = app.id
    String userToken = state.admToken
    String apiAccessToken = state.accessToken
    String cloudAccessToken = app.getHubUID()
    String qrJson = JsonOutput.toJson([ip:"${ipAddr}", id:"${appId}",ut:"${userToken}",at:"${apiAccessToken}",ct:"${cloudAccessToken}"])
    String qrFile = "<script src='https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js'></script><canvas id='qrcode'></canvas><script type='text/javascript'>new QRious({element: document.getElementById('qrcode'), size: 250,value: '${qrJson}'});</script>"
    fData = qrFile.getBytes("UTF-8")
    uploadHubFile("ceConnQr.html",fData)
    //hubitat.helper.QRHelper.generateQR(qrFile, "ceConnQr.png")
}

void appButtonHandler(btn) {
    switch(btn) { 
        case "resetToken":
            createAccessToken()
            break
        case "genUT":
            state.getUT = true
            break
        default: 
            if(debugEnabled) log.error "Undefined button $btn pushed"
            break
    }
}

void intialize() {

}

void uninstalled(){

}
