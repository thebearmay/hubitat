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
 *    26Jul2022   thebearmay    additional verifications
 *    11Nov2022   thebearmay    suppress CSS option
*/

static String version()	{  return '0.1.2'  }

import java.text.SimpleDateFormat
import java.util.Date
import groovy.transform.Field

@Field sdfList = ["yyyyMMdd","ddMMYYYY","MMddyyyy","dd/MM/yyyy","MM/dd/yyyy"]
@Field sdfList2 = ["dd MMM", "ddMMM", "MMM dd", "ddMMMyyyy", "MM/dd", "dd/MM","[None]"]


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
    page name: "options"
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
            section("<b><u>Main Page</u></b><br /><small>v${version()}</small>") {
                input "fileIn", "text", title: "<b>Input File</b>", submitOnChange:true, required:false, defaultValue:"yourFileName.txt", width:4
                
                if(fileExists("$fileIn")) {
                    paragraph "<span style='color:white;background-color:green'>&nbspInput File Present&nbsp</span>"
                    s1 = true                    
                } else {
                    paragraph "<span style='color:black;background-color:red'>&nbspInput File Not Present&nbsp</span>"
                    s1 = false
                }
                input "varOut", "text", title: "<b>Variable to update:</b>", submitOnChange:true, required:false, defaultValue:"varName", width:4
                input "noVarOut", "bool", title: "<b>Run without a Hub Variable</b>", submitOnChange:true, required:false, defaultValue:false, width:4
                removeAllInUseGlobalVar()
                if((varOut != null && variableExists("$varOut")) || noVarOut){
                    paragraph "<span style='color:white;background-color:green'>&nbsp;Variable Present or Overriden&nbsp;</span>"
                    s2 = true                  
                } else{
                    paragraph "<span style='color:black;background-color:red'>&nbsp;Variable Not Present&nbsp;</span>"
                    s2= false
                }
                
                input "dateFmt", "enum", title: "<b>Date format used in file:</b>", submitOnChange:true, required:false, options:sdfList, width:4
                
                input "tOnly", "bool", title:"<b>Today's events only</b>", submitOnChange:true, required:false, defaultValue:false, width:4
                href "options", title:"Additional Options", width:4
                
                if(s1 && s2 && dateFmt != null)
                input "loadFile", "button", title:"Load File and Start Daily Processing", backgroundColor:"light-gray",textColor:"green",borderColor:"black"

                input("security", "bool", title: "<b>Hub Security Enabled?</b>", defaultValue: false, submitOnChange: true)
                if (security) { 
                    input("username", "string", title: "<b>Hub Security Username</b>", required: false)
                    input("password", "password", title: "<b>Hub Security Password</b>", required: false)
                }
                
				input "debugEnabled", "bool", title:"<b>Enable Debug Logging?</b>", submitOnChange:true, required:false, defaultValue:false
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

def options(){
    dynamicPage (name: "options", title: "", install: false, uninstall: false) {
        section("<u><b>Options</b></u>"){
            input "dateFmt2", "enum", title: "<b>Date format to prepend note:</b>", submitOnChange:true, required:false, options:sdfList2, defaultValue:"dd MMM", width:4
            input "noCSS", "bool", title: "<b>No CSS in Output</b>", submitOnChange:true, defaultValue:false, width:4
            input "swDev", "capability.switch", title:"<b>Optional switch to turn on, on the event date</b>", submitOnChange:true, required:false
            input "notifDev", "capability.notification", title:"<b>Optional notification device(s)</b>", submitOnChange:true, required:false, multiple:true
            if(notifDev) {
                input "notifTime", "time", title:"<b>Time to send Notification</b>", submitOnChange:true, require:false, defaultValue:"07:00", width:4
            }
           
        }
    }
}

void processFile(){
    String fContent=readFile(fileIn)
    List fRecs=fContent.split("\n")
    String today = (new SimpleDateFormat("yyyyMMdd")).format(new Date())
    sdf = new SimpleDateFormat("$dateFmt")
    if(!noCSS)
        dailyMessage = "<span style='color:black;background-color:red'>&nbsp;No future events to display&nbsp;</span>"
    else
        dailyMessage = "No future events to display"
    boolean procFlag = true
    if(swDev) swDev.off()
    fRecs.each {
        if(!procFlag) return
        if(debugEnabled) log.debug(it)
        
        int firstSpace = it.indexOf(' ')
        datePart = (new SimpleDateFormat("yyyyMMdd")).format(sdf.parse(it.substring(0,firstSpace)))
        if(dateFmt2 && dateFmt2 != "[None]")
            noteDate = (new SimpleDateFormat(dateFmt2)).format(sdf.parse(it.substring(0,firstSpace)))
        else
            noteDate = ""
                       
        notePart = it.substring(firstSpace+1)
        if(debugEnabled) log.debug "$datePart $notePart"
        if(datePart == today) {
            dailyMessage = "$noteDate $notePart"
            procFlag = false
            if(swDev) swDev.on()
            if(notifDev) schedNotify(dailyMessage)
        } else if (!tOnly && datePart > today) {
            dailyMessage = "$noteDate $notePart"
            procFlag = false
        } else if (tOnly && datePart > today) {
            dailyMessage = "$noteDate Nothing for Today"
            proceFlag = false
        }
    }
    if(!noVarOut) this.setGlobalVar("$varOut", "$dailyMessage")
}

void schedNotify(msg) {
    if(debugEnabled) log.debug "Schedule $msg"
    //Extract the time from the preference setting and append to the current date
    String wDateStr = "${(new SimpleDateFormat('yyyy-MM-dd')).format(new Date())}T${notifTime.substring(notifTime.indexOf('T')+1)}"
    //Subtract current date/time from notification date/time to determine runin offset
    long notifSec = (((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).parse(wDateStr)).getTime()-((new Date()).getTime()))/1000;
    runIn(notifSec, "sendNotif", [data:msg])
}

void sendNotif(msg){
    if(msg == null) msg = data.msg
    notifDev.each { 
        if(debugEnabled) log.debug "Sending notification to $it, text: $msg"
        it.deviceNotification(msg)  
    }
}

void appButtonHandler(btn) {
    switch(btn) {
        case "loadFile":
            if(debugEnabled) log.debug "loadFile Called"
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
