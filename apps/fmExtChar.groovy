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
 *    05Oct2022   thebearmay    code clean up
 *	  27Dec2024	  				Handle small screens better
 */
import java.text.SimpleDateFormat
static String version()	{  return '0.0.4'  }


definition (
	name: 			"File Manager - Extended Characters", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Overcomes the Extended Character display issue with File Manager and allows download of the selected file.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/apps/fmExtChar.groovy",
    installOnOpen: 	true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "fileDisp"

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
	    	section("")
		    {
              section("File List", hideable: true, hidden: false){
                  fileList = listFiles()
                  SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")         
                  oData = "<style>table{overflow:auto;display:block;width:100%} td {word-wrap:break-word;padding:5px;}</style><table><tr><th></th><th>Name</th><th>Size</th><th>Date</th><th></th></tr>"
                  int i = 0
                  fileList.each {
                      if(it.date)                   
                          dDate = sdf.format(new Date(Long.parseLong(it.date)))
                      else
                          dDate = "Date not Available"
                      oData +="<tr><td>${buttonLink("fDispBtn$i","FM Ext View")}</td>"
                      oData +="<td><a href='http://${location.hub.localIP}:8080/local/${it.name}' target='_blank'>${it.name}</a></td>"
                      oData +="<td style='text-align:right'>${String.format('%,.0f',it.size.toDouble())}</td>"
                      oData +="<td>$dDate&nbsp;</td></td>"
	                  oData +="<td><button><a href='http://${location.hub.localIP}:8080/local/${it.name}?raw=true'>Download</a></button></td></tr>"
	                  if(state["btnPush$i"] || state["btnPush$i"] == "true"){
                          state["btnPush$i"] = false
                          fContent = readFile("${it.name}")
                          fContent = fContent.replace('<','&lt;')
                          paragraph "<pre style='border:1px solid blue;border-radius:10px'>${fContent}</pre>"
                          input "clear", "button", title:"Close", backgroundColor:"yellow"
					  }
                 
                      i++
                  }
                  paragraph oData+"</table>"

                  input "debugEnabled", "bool", title: "Enable Debug Logging", required: false, defaultValue:false, submitOnChange:true, width:4
                  input "security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4
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

String buttonLink(String btnName, String linkText, color = "#FFFFFF", bkColor = "green", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='border-radius:25px;color:$color;background-color:$bkColor;cursor:pointer;font-size:$font; border-style:outset;width:8em;text-align:center;'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

def fileDisp(params){
    dynamicPage (name: "fileDisp", title: "File Display",nextPage: "mainPage",  install: false, uninstall:false) {
        section("File Contents") {
            
            paragraph "params ${params}" //getQueryStringParams()
            if(params != null){
                fContent = readFile("${it.name}")
                fContent = fContent.replace('<','&lt;')
                paragraph "<pre style='border:1px solid blue;overflow:auto'>${fContent}</pre>" 
            }
        }
    }
}

def appButtonHandler(btn) {
    switch(btn) {  
        case {it.contains('fDispBtn')}:
            bNum=btn.substring(8)
            state["btnPush$bNum"] = true
            break
        case "clear":
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
      }
}

def intialize() {

}

void uninstalled(){
        
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
                //log.debug "${resp.contentType}"
               if(!resp.contentType.contains("text") && !resp.contentType.contains("json"))
                   return "File type is not text -> $resp.contentType"
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               }
               if(logResponses) log.info "File Read Data: $delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null
    }
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
