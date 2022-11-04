/*
 * Hub File Manager Sychronization
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
 *    18Oct2022    thebearmay    New Code
*/

static String version()	{  return '1.0.0'  }
import groovy.json.JsonOutput
import java.text.SimpleDateFormat
#include thebearmay.localFileMethods
/* *********************************************************************************************
 * https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/localFileMethods.groovy *
 ***********************************************************************************************/
import groovy.transform.Field


definition (
	name: 			"Hub File Sync", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Keep a current copy of File Manager Files on a second hub",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/heHa/hubFileSync.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}

mappings {
    path("/remLogin") {
        action: [POST: "remLogin"]
    }
    path("/getList") {
        action: [POST: "getList"]
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
            section("<span style='color:blue;font-weight:bold'>Synchronization Settings</span>", hideable: true, hidden: false){
                input "syncMaster", "string", title:"IP Address of Source Hub", width:5, submitOnChange:true
                input "syncDestination", "string", title: "IP Address of Destination Hub", width:5, submitOnChange:true
                roleList = ["Unknown", "Source", "Destination"]
                roleIndex = ["","$syncMaster", "$syncDestination"].indexOf("${location.hub.localIP}")
                if(roleIndex < 0) roleIndex = 0
                paragraph "This hub's role is <span style='background-color:yellow;font-weight:bold'>${roleList[roleIndex]}</span>"
                state.hubRole = "${roleList[roleIndex]}"
                if(state.hubRole == "Destination"){
                    input "syncIntervalUnit", "enum", title: "Sync Interval Units", options: ["Minutes", "Hours", "Days", "Weeks"], width:4, submitOnChange:true
                    input "syncInterval", "number", title: "Sync Interval", width:4, constraints:["NUMBER"], submitOnChange:true
                    input "firstSync", "time", title: "Time for First Sync", width:4, submitOnChange:true
                    input "checkRead", "button", title:"Test 1 File", width:4
                    if(state.checkRead) {
                        fileSync()
                        paragraph "${state.lastSyncResult}"
                        state.checkRead = false
                    }
                   
                    input "syncEnabled", "bool", title: "Enable File Synchronization", width: 4, submitOnChange:true, defaultValue: false
                    
                    if(state.currentSe != syncEnabled){
                        state.currentSe = settings["syncEnabled"]
                        if(syncEnabled){
                            log.debug firstSync
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                            fSync = sdf.parse(firstSync)
                            if(fSync.getTime() < new Date().getTime()) {
                                log.debug "Time in the Past adding 24 hours"
                                newTime = new Date(fSync.getTime() + 86400000)
                                log.debug "New time: $newTime"
                                runOnce(newTime, 'fileSync')
                            } else 
                                runOnce(fSync, 'fileSync')
                        } else
                            unschedule('fileSync')
                    }
                }

            }
            section("<span style='color:blue;font-weight:bold'>Source Hub Security</span>", hideable: true, hidden: true){
                if(state.accessToken == null) createAccessToken()
                if(state.hubRole == "Source"){
                    tDefault = state.accessToken
                    aDefault = getFullLocalApiServerUrl()
                }
                input "remoteAPI", "text", title:"<b>Source Server API:</b>",submitOnChange:true, defaultValue: aDefault
                input "token","text", title:"<b>Source Access Token:</b>",submitOnChange:true, defaultValue: tDefault
                input "sourceSecurity", "bool", title: "Source Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4
                if (sourceSecurity) { 
                    input("sUsername", "string", title: "Source Hub Security Username", required: false)
                    input("sPassword", "password", title: "Source Hub Security Password", required: false)
                }
            }
            
            section("<span style='color:blue;font-weight:bold'>Local Hub Information</span>", hideable: true, hidden: true){
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
                input "resetToken", "button", title:"Reset Token"
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

def fileSync(){
    fileList = listRemoteFiles()
    i=0
	fileList.each{
       if((state.checkRead && i < 1) || !state.checkRead) 
		    copyFile("http://$syncMaster/local/$it")
       i++
	}
    if(state.checkRead){
        log.info "1 file copied from $syncMaster, $i files in list" 
        state.lastSyncResult = "1 file copied from $syncMaster, $i files in list" 
    } else {
        log.info "$i files copied from $syncMaster"
        state.lastSyncResult = "$i files copied from $syncMaster" 
        if(syncEnabled) {
            // "Minutes", "Hours", "Days", "Weeks"
            switch (syncIntervalType) {
                case "Minutes":
                    multiplier = 60
                    break
                case "Hours":
                    multiplier = 3600
                    break
                case "Days":
                    multiplier = 86400
                    break
                case "Weeks":
                    multiplier = 604800
                    break
                default:
                    log.error "Invalid Interval - $syncIntervalType, scheduling daily"
                    multiplier = 86400
                    break                    
            }
            Long compSeconds = syncInterval.toLong() * mulitplier
            runIn(compSeconds, 'fileSync')
        }
    }
}

void copyFile(fPath){
    fExist = extFileExists("$fPath")
    if(fExist.exist == "false")
        return
	oName = fPath.substring(fPath.lastIndexOf("/")+1)
    if(!fExist.fType.contains("text") && !fExist.fType.contains("json")) {
		imageData = readImage(fPath)
		writeImageFile(oName, imageData.iContent, imageData.iType)
    } else {
		fData = readExtFile(fPath)
		writeFile("$oName", "$fData")
	}    
}

List<String> listRemoteFiles(){
    if(sourceSecurity){
        sendRemote("/remLogin",[username:"$sUsername", password:"$sPassword"])
    }
    sendRemote("/getList", [:])
    if(debugEnabled) log.debug state.fList
    return state.fList.files
}

HashMap remoteLogin(rName, rPwd){
    if(debugEnabled) log.debug "Remote login requested: $rName:$rPwd"
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: rName,
						password: rPwd,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
    if(result){
        app.updateSetting("security",[value:"true",type:"bool"])
        app.updateSetting("username",[value:"$rName",type:"string"])
        app.updateSetting("password",[value:"$rPwd",type:"password"])
    }
	return [result: result, cookie: cookie]
}

def remLogin() { //request from Destination Server
    jsonData = (HashMap) request.JSON
    if(debugEnabled) log.debug "${jsonData.username}, ${jsonData.password}"
    
    jsonText = JsonOutput.toJson(remoteLogin("${jsonData.username}", "${jsonData.password}"))
    if(debugEnabled) log.debug "rendering $jsonText"
    render contentType:'application/json', data: "$jsonText", status:200    
}

def getList() {//request from Destination Server
    jsonText = JsonOutput.toJson(files:listFiles())
    if(debugEnabled) log.debug "rendering $jsonText"
    render contentType:'application/json', data: "$jsonText", status:200     
}

def sendRemote(command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "$remoteAPI$command?access_token=$token",
        requestContentType: 'application/json',
		contentType: 'application/json',
        body: "$bodyText"
	]

    if(debugEnabled) log.debug "$requestParams"
    httpPost(requestParams) { resp ->
        try {
            if(debugEnabled) log.debug "$resp.properties ${resp.getStatus()}"
            if(resp.getStatus() == 200 || resp.getStatus() == 207){
                if(resp.data)
                    if(debugEnabled) log.debug "$resp.data"
                    if(command == '/remLogin')
                        state.cookie =  (HashMap) resp.data
                    else if(command == '/getList')
                        state.fList = (HashMap) resp.data
            }
        } catch (Exception ex) {
            log.error ex
        }
    }
}

@SuppressWarnings('unused')
List<String> listFiles(){
    if(security) cookie = securityLogin().cookie
    if(debugEnabled) log.debug "Getting list of files"
    uri = "http://${location.hub.localIP}:8080/hub/fileManager/json";
    def params = [
        uri: uri,
        headers: [
				"Cookie": cookie
            ]        
    ]
    try {
        fileList = []
        httpGet(params) { resp ->
            if (resp != null){
                if(logEnable) log.debug "Found the files"
                def json = resp.data
                for (rec in json.files) {
                    fileList << rec.name
                }
            } else {
                //
            }
        }
        if(debugEnabled) log.debug fileList.sort()
        return fileList.sort()
    } catch (e) {
        log.error e
    }
}


void appButtonHandler(btn) {
    switch(btn) { 
        case "checkRead":
            state.checkRead = true
            break
        case "resetToken":
            createAccessToken()
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
