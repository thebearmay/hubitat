/* Tile Multi-Device Template Manager
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
 *     Date              Who           Description
 *    ===========       ===========   =====================================================
 *    2022-08-30        thebearmay    add file list and template checking
 *    2022-09-05        thebearmay    add @room
 *    2022-09-18        thebearmay    handle template read error
 *    2023-01-05        thebearmay    add a filter for template selection
 *    2023-02-07        thebearmay    Allow the use of Hub Variables
*/

static String version()	{  return '0.1.0'  }


definition (
	name: 			"Tile Multi-Device Template Manager", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Use a template file to generate an HTML element for multiple named devices.",
	category: 		"Utility",
	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/tileTemplate/ttMultiDevMgr.groovy",
    installOnOpen:  true,
	oauth: 			false,
    singleThreaded: true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "previewTemplate"
}

void installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main") {
                
                state.validTemplate = false
                List<String> fList 
                input "mustContain", "string", title:"Filter to Templates that contain", required:false, submitOnUpdate: true, width:4
                input "applyFilter", "button", title: "Apply Filter"                
                if(state.afPushed) {
                    state.afPushed = false
                }
             
                if(mustContain != null)
                    fList = listFiles("$mustContain")
                else
                    fList = listFiles()
                
                input "templateName", "enum", title: "<b>Template to Process</b>", required: true, width:5, submitOnUpdate:true, options:fList
                input "templateCheck", "button", title:"Check Template"
                if(templateName != null && state?.tCheck == true) {
                    List devList = templateScan()
                    if (devList.size > 0){
                        devListE = []
                        devList.each {
                            if("$it".isNumber())
                                devListE.add("<a href='http://${location.hub.localIP}:8080/device/edit/$it' target='_blank'>$it</a>")
                            else
                                devListE.add("variable:$it")
                        }
                        state.validTemplate = true
                    } else devListE = 'No devices found <b>**Invalid Template**</b>'
                    paragraph "The following devices are required for this template: $devListE"
                    state.tCheck = false
                }
                if (state.validTemplate)
                    input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: false, submitOnChange: true
                if(qryDevice) {
                    unsubscribe()
                    qryDevice.each{
                        subscribe(it, "altHtml", [filterEvents:true])
                    }
                    href "previewTemplate", title: "Template Preview", required: false
                }
                HashMap varMap = getAllGlobalVars()
                List varListIn = []
                
                varMap.each {
                    varListIn.add("$it.key")
                }
                input "varList", "enum", title: "Select variables to monitor:", options: varListIn.sort(), multiple: true, required: false, submitOnChange: true
                if(varList != null) {
                    removeAllInUseGlobalVar()
                    varlist.each {
                        var="variable:$it"
                        subscribe(location,"$var", "altHtml")
                        success = addInUseGlobalVar(it.toString())
                    }                   
                }

                input "clearSettings", "button", title: "Clear previous settings"
                if(state?.clearAll == true) {
                    unsubscribe()
                    settings.each {
                        if(it.key != 'isInstalled') {
                            app.removeSetting("${it.key}")
                        }
                    }
                    state.clearAll = false
                }
                if(!this.getChildDevice("ttdm${app.id}"))
                    addChildDevice("thebearmay","Generic HTML Device","ttdm${app.id}", [name: "HTML Tile Device${app.id}", isComponent: true, label:"HTML Tile Device${app.id}"]) 
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
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def previewTemplate(){
    dynamicPage (name: "previewTemplate", title: "Template Preview", install: false, uninstall: false) {
	  section(""){
          html = altHtml()
          paragraph "${html}"      
      }
    }
}

List templateScan() {
    if(templateName == null) return []
    String fContents = readFile("$templateName")
    List fRecs=fContents.split("\n")
    List devList =[]
    fRecs.each {
        int vCount = it.count("<%")
        if(vCount > 0){
            recSplit = it.split("<%")
            recSplit.each {
                if(it.indexOf("%>") > -1){
                    vEnd = it.indexOf("%>")
//                    log.debug "${it.indexOf(":")}"
                    if(it.indexOf(":") > -1 && it.indexOf(":") < vEnd){ //format of <%devId:attribute%>
                        devId = it.substring(0,it.indexOf(":"))
                        if(devId != "var")
                            devList.add(devId.toLong())
                        else
                            devList.add(it.substring(it.indexOf(":"),vEnd))
                    }
                }
            }
        }
    }
    
    return devList.unique()
}

String altHtml(evt = "") {
    //log.debug "altHtml $evt.properties"
    String fContents = readFile("$templateName")
    if (fContents == null) return
    List fRecs=fContents.split("\n")
    String html = ""
    fRecs.each {
        int vCount = it.count("<%")
        if(vCount > 0){
            recSplit = it.split("<%")
            recSplit.each {
                if(it.indexOf("%>") == -1)
                    html+= it
                else {
                    vEnd = it.indexOf("%>")
                    if(it.indexOf(":") > -1 && it.indexOf(":") < vEnd){
//                    if(it.indexOf(":") > -1){ //format of <%devId:attribute%>
                        devId = it.substring(0,it.indexOf(":"))
                        if(devId != "var") {
                            devId = devId.toLong()
                            qryDevice.each{
                                if(it.deviceId == devId) dev=it
                            }
                        }
                        vName = it.substring(it.indexOf(":")+1,it.indexOf('%>'))
                        //log.debug "$devId $vName"
                    } else
                        vName = it.substring(0,it.indexOf('%>'))

                    if(vName == "@date")
                        aVal = new Date()
                    else if (vName == "@version")
                        aVal = version()
                    else if (vName == "@name" && dev != null)// requires a format of <%devId:attribute%>
                        aVal = dev.properties.displayName
                    else if (vName == "@room" && dev != null)
                        aVal = dev.properties.roomName
                    else if(devId=="var") {
                        aVal = getGlobalVar("$vName").value
                    }
                    else if(dev != null) {
                        aVal = dev.currentValue("$vName",true)
                        String attrUnit = dev.currentState("vName")?.unit
                        if (attrUnit != null) aVal+=" $attrUnit"
                    }
                    html+= aVal
                    if(it.indexOf("%>")+2 != it.length()) {
                        html+=it.substring(it.indexOf("%>")+2)
                    }
                }                 
            }
        }
        else html += it
    }
    if (!evt) return html
        
    chd = getChildDevice("ttdm${app.id}")
    chd.sendEvent(name:"html1", value:html)
    return null
}

void refreshSlot(sNum) {
    altHtml([refresh:true])
}

@SuppressWarnings('unused')
String readFile(fName){
    if(security) cookie = getCookie()
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
List<String> listFiles(filt = null){
    if(security) cookie = getCookie()
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
                    if(filt != null){
                        if(rec.name.contains("$filt")){
                            fileList << rec.name
                        }
                    } else
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

@SuppressWarnings('unused')
String getCookie(){
    try{
  	  httpPost(
		[
		uri: "http://127.0.0.1:8080",
		path: "/login",
		query: [ loginRedirect: "/" ],
		body: [
			username: username,
			password: password,
			submit: "Login"
			]
		]
	  ) { resp -> 
		cookie = ((List)((String)resp?.headers?.'Set-Cookie')?.split(';'))?.getAt(0) 
        if(debugEnable)
            log.debug "$cookie"
	  }
    } catch (e){
        cookie = ""
    }
    return "$cookie"

}

def appButtonHandler(btn) {
    switch(btn) {
        case "clearSettings":
            state.clearAll = true
            break
        case "templateCheck":
            state.tCheck = true
            break
        case "applyFilter":
            state.afPushed = true
            mainPage()
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}

void intialize() {

}
