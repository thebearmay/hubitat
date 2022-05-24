/*
 * File Manager Extended Characters
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
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.0.1'  }


definition (
	name: 			"File Manager - Extended Characters", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Overcomes the Extended Character display issue with File Manager and allows download of the selected file.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/fmExtChar.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "fileDispDown"

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
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
              section("File List", hideable: true, hidden: false){
                  fileList = listFiles()
                  SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                  oData = "<table><tr><th style='text-align:center;width:20em'>Name</th><th style='text-align:center;width:2em'>Size</th><th style='width:3em'></th><th style='text-align:center;width:10em'>Date</th></tr>"
                  fileList.each {
                      if(it.date)                   
                          dDate = sdf.format(new Date(Long.parseLong(it.date)))
                      else
                          dDate = "Date not Available"
                      oData += "<tr><td><a href='http://${location.hub.localIP}:8080/local/${it.name}?raw=true'>${it.name}</a></td>"
                      oData += "<td style='text-align:right'>${it.size}</td><td></td>"
                      oData += "<td>$dDate</td></tr>"
                  }
                  oData += "</table>"
                  paragraph oData
                  input "debugEnabled", "bool", title: "Enable Debug Logging", required: false, defaultValue:false, submitOnChange:true
                  input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
                  if (security) { 
                    input("username", "string", title: "Hub Security Username", required: false)
                    input("password", "password", title: "Hub Security Password", required: false)
                  }
              }
              section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
              }   
		    }
	    } else {
              section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
              }   
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def appButtonHandler(btn) {
    switch(btn) {
        default: 
            log.error "Undefined button $btn pushed"
            break
      }
}

def intialize() {

}

void uninstalled(){
    if(getChildDevice("ddd${app.id}-01")){
        unsubscribe()
        deleteChildDevice("ddd${app.id}-01")
    }          
}

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
//			log.debug resp.data?.text
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

List<String> listFiles(){
     if(security) cookie = securityLogin().cookie
    // Adapted from BptWorld's Community Post 89466/4
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
                if(debugEnabled) log.debug "Found the files"
                def json = resp.data
                for (rec in json.files) {
                    fileList.add([name:rec.name, size:rec.size, date:rec.date])//rec.name
                }
            } else {
                //
            }
        }
        if(debugEnabled) log.debug fileList.sort()
        return fileList.sort{it.name}
    } catch (e) {
        log.error e
    }
}
