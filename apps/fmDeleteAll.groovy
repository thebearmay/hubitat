/*
 * File Manager Delete All 
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
 */
import java.util.zip.*
import java.util.zip.ZipOutputStream    
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '0.0.0' }

definition (
	name: 			"File Manager Delete", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Logic Check .",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/fmDeleteAll.groovy",
    installOnOpen:  true,
	oauth: 			false,
    singleThreaded: true,
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
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<h2>File Manager Delete</h2><p style='font-size:small'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
            section("<h3>Main</h3>"){
        
                input "purgeAll", "button", title: "Delete all file in FM"
                if(state.purgeReq) {
                    state.purgeReq = false
                    deleteFM()
                }
            }
            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
            }            
        }
    }
}


void deleteFM() {
    fList = listFiles().fList
    fList.each{
        deleteHubFile("${it.trim()}")
    }
}


@SuppressWarnings('unused')
HashMap listFiles(retType='nameOnly'){
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
        json = ''
        httpGet(params) { resp ->
            if (resp != null){
                if(logEnable) log.debug "Found the files"
                json = resp.data
                if(debugEnabled) log.debug "$json"
                for (rec in json.files) {
                    if(rec.type == 'file')
                        fileList << rec.name.trim()
                }
            } else {
                //
            }
        }
        if(debugEnabled) log.debug fileList.sort()
        if(retType == 'json') 
            return [jStr: json]
        else
            return [fList: fileList.sort()]
    } catch (e) {
        log.error e
    }
}

@SuppressWarnings('unused')
HashMap securityLogin(){
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
						username: username,
						password: password,
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
	return [result: result, cookie: cookie]
}

Boolean fileExists(fName){

    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                return true;
            } else {
                return false;
            }
        }
    } catch (exception){
        if (exception.message.toLowerCase().contains("not found")){
            if(debugEnabled) log.debug "File DOES NOT Exists for $fName"
        } else {
            log.error "Find file $fName :: Connection Exception: ${exception.message}"
        }
        return false;
    }

}


def appButtonHandler(btn) {
    switch(btn) {
        case "purgeAll":
            state.purgeReq = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}               
