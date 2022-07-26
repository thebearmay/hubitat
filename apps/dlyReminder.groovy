/*
 * Daily Reminder
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

static String version()	{  return '0.1.0'  }

import java.text.SimpleDateFormat
import groovy.transform.Field

@Field sdfList = ["yyyyMMdd","dd/MM/yyyy","MM/dd/yyyy"]

definition (
	name: 			"Daily Reminder", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Reads a file to generate a reminder variable update .",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/apps/dlyReminder.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}



void installed() {
    if(debugEnabled) log.trace "${app.getLabel()} installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "${app.getLabel()} updated()"
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
                input "fileIn", "text", title: "Input File", submitOnChange:true, required:false, defaultValue:"yourFileName.txt"
                
                if(fileExists("$fileIn")) {
                    paragraph "<span style='color:white;background-color:green'>&nbspInput File Present&nbsp</span>"
                    s1 = true                    
                } else {
                    paragraph "<span style='color:black;background-color:red'>&nbspInput File Not Present&nbsp</span>"
                    s1 = false
                }
                input "varOut", "text", title: "Variable to update:", submitOnChange:true, required:false, defaultValue:"varName"
                removeAllInUseGlobalVar()
                if(varOut != null && variableExists("$varOut")){
                    paragraph "<span style='color:white;background-color:green'>&nbsp;Variable Present&nbsp;</span>"
                    s2 = true                  
                } else{
                    paragraph "<span style='color:black;background-color:red'>&nbsp;Variable Not Present&nbsp;</span>"
                    s2= false
                }
                
                input "dateFmt", "enum", title: "Date format used in file:", submitOnChange:true, required:false, options:sdfList
                
                input "tOnly", "bool", title:"Today's events only:", submitOnChange:true, required:false, defaultValue:false
                if(s1 && s2)
                    input "loadFile", "button", title:"Load File and Start Daily Processing"

                input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
                if (security) { 
                    input("username", "string", title: "Hub Security Username", required: false)
                    input("password", "password", title: "Hub Security Password", required: false)
                }
                
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
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

void processFile(){
    String fContent=readFile(fileIn)
    List fRecs=fContent.split("\n")
    String today = (new SimpleDateFormat("yyyyMMdd")).format(new Date())
    sdf = new SimpleDateFormat("$dateFmt")
    dailyMessage = "<span style='color:black;background-color:red'>&nbsp;No future events to display&nbsp;</span>"
    boolean procFlag = true
    fRecs.each {
        if(!procFlag) return
        if(debugEnabled) log.debug(it)
        
        int firstSpace = it.indexOf(' ')
        datePart = (new SimpleDateFormat("yyyyMMdd")).format(sdf.parse(it.substring(0,firstSpace)))
        noteDate = (new SimpleDateFormat("dd MMM")).format(sdf.parse(it.substring(0,firstSpace)))
        notePart = it.substring(firstSpace+1)
        //if(debugEnabled) 
        log.debug "$datePart $notePart"
        if(datePart == today) {
            dailyMessage = "$noteDate $notePart"
            procFlag = false
        } else if (!tOnly && datePart > today) {
            dailyMessage = "$noteDate $notePart"
            procFlag = false
        } else if (tOnly && datePart > today) {
            dailyMessage = "$noteDate Nothing for Today"
            proceFlag = false
        }
    }
    this.setGlobalVar("$varOut", "$dailyMessage")
}

void appButtonHandler(btn) {
    switch(btn) {
        case "loadFile":
            log.debug "loadFile Called"
            processFile()
            schedule("0 5 0 ? * * *", "processFile")
            break            
        default: 
            if(debugEnabled) log.error "Undefined button $btn pushed"
            break
    }
}

void intialize() {

}

void uninstalled(){
    unschedule()
    removeAllInUseGlobalVar()
}

//variable methods
boolean variableExists(vName) {
    Map varList = getAllGlobalVars()
    boolean found = false
    varList.each{
        if(it.key == vName) found = true
    }
    if(found) addInUseGlobalVar("$vName")
    else removeInUseGlobalVar("$vName")
    return found
}

//file methods
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

@SuppressWarnings('unused')
String readFile(fName){
    if(security) cookie = securityLogin().cookie
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie,
                "Accept": "application/octet-stream"
            ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {       
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               }
               if(debugEnabled) log.info "File Read Data: $delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
Boolean fileExists(fName){
    if(debugEnabled) log.debug "fileExists($fName)"
    if(fName == null) return false
    
    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri          
    ]

    try {
        httpGet(params) { resp ->
            if(debugEnabled) log.debug "${resp.properties}"
            if (resp != null){
                if(debugEnabled) log.info "File Exist: true"
                return true;
            } else {
                if(debugEnabled) log.info "File Exist: false"
                return false
            }
        }
    } catch (exception){
        if (exception.statusCode == 404){
            if(debugEnabled) log.info "File Exist: false"
        } else {
            log.error "Find file $fName :: Connection Exception: ${exception.message}"
        }
        return false;
    }
}
