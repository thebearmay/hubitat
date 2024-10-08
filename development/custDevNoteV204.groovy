/* Device Custom Note
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
 *    Date          Who           What
 *    ----          ---           ----
 *    01Mar2022     thebearmay    1.0.1 - Add message for any hub mesh device a note is attached to (meshed devices won't retain note)
 *                                1.0.2 - Use controllerType to determine Mesh status
 *    02Mar2022     thebearmay    1.0.3 - Add warning message for missing note text
 *    01Oct2024     thebearmay    2.0.0..2.0.4 - Rewrite of the UI
 */
import groovy.transform.Field
static String version()	{  return '2.0.4'  }
String appLocation() { return "http:${location.hub.localIP}/installedapp/configure/${app.id}/mainPage" }


definition (
	name: 			"Custom Device Note", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Add a custom note to any device.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/custDevNote.groovy",
	oauth: 			false,
    installOnOpen: true,
    singleThreaded: true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "noteMaint"
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
    dynamicPage (name: "mainPage", title: "<h2 style='color:blue'>Custom Note <span style='font-size: x-small'>v${version()}</span></h2>", install: true, uninstall: true) {
        if (app.getInstallationState() == 'COMPLETE') { 
            section("<h4>Configuration</h4>", hideable:true, hidden: true){
                input "qryDevice", "capability.*", title: "Populate Device Table:", multiple: true, required: false, submitOnChange: true
				input "debugEnabled", "bool", title: "Enable Debug", defaultValue: false, submitOnChange:true
				input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
				if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)				
            }
            section(""){
                fileList = getFiles()
                if(!fileList.contains('iconify'))
                   fetchJS()
                settings.each{
                    if("${it.key}".contains("sdKey")){
                        app.removeSetting("${it.key}")
                    }
                }                
                state.sdSave = false
                paragraph buildDeviceTable() 
                if(state.singleDev){
                    state.singleDev = false
                    paragraph "<script>window.location.replace('${appLocation()}/noteMaint')</script>"
                }                    
                
                //href name: "noteMaintHref", page: "noteMaint",title: "${btnIcon('pi-pencil')} Note Maint", description: "", width: 4, newLine: false//, params:[did: 30]
                                

	       }        
	    } else {
    		section("") {
	    	   paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

String btnIcon(String name) {
    return "<span class='p-button-icon p-button-icon-left pi " + name + "' data-pc-section='icon'></span>"
}

String buildDeviceTable(){
    ArrayList<String> tHead = ["","Sel","Device","Notes"]
    String X = "<i class='he-checkbox-checked'></i>"
    String O = "<i class='he-checkbox-unchecked'></i>"
    String settingsIcon = "<i class='material-icons app-column-info-icon' style='font-size: 24px;'>settings_applications</i>"
    String removeIcon = "<i class='material-icons he-bin'></i>"


    String str = "<script src='/local/iconify-icon.min.js'></script>$ttStyleStr"
    str += "$tableStyle<div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style='border-left:2px solid black;border-top:2px solid black;'>" +
            "<thead><tr style='border-bottom:2px solid black'>"
    tHead.each { str += "<th><strong>${it}</strong></th>" }
    str += "</tr></thead>" 
    if(qryDevice)
        devSort = qryDevice.sort { a, b -> a.displayName <=> b.displayName }
    devSort.each{
        noteMap = it.getData()
        noteList = ''
		hoverList = "<div class='tTipText'><ul>"
        i=0
        noteMap.sort().each {
            if ( i > 0 ) noteList += ", "
            noteList += "<b>${it.key}</b>" 
            i++
			hoverList += "<li><b>${it.key}:</b>${it.value}</li>"
        }
		hoverList += "</ul></div>"
        //str += "<tr><td><a href='${appLocation}/singleDevice?did=${it.id}' target='_self'>$settingsIcon</a></td>"
        if(it.controllerType == "LNK") 
            singleDev = "<b style='color:#700000'>&#x26D4;</b>"
        else
            singleDev = buttonLink("singleDev${it.id}", "$settingsIcon", "#000000", "12px")
        str += "<tr><td>$singleDev</td>"
        if(it.controllerType == "LNK") 
            devSel = "<b style='color:#700000'>&#x26D4;</b>"
        else
            devSel = buttonLink("devSel${it.id}", "${state["devSel${it.id}"]? X : O}", "#000000", "12px")
        if(it.controllerType == "LNK")
            devName = "${it.displayName} <span style='color:red'>(HubMesh Device)</span>"
        else
            devName = it.displayName
        str += "<td>$devSel</td><td><a href='http://${location.hub.localIP}/device/edit/${it.id}' target='_self'>${devName}</a><td class='tTip'>$hoverList$noteList</td>"
        str += "</tr>"
    }
    String addNote = "<a href='${appLocation()}/noteMaint' target='_self' style='color:#007009;font-size:25px;'><b>+</b></a>"
    //buttonLink("addNote", "<b>＋</b>", "#007009", "25px")
    str += "<tr style='border-top:2px solid black;border-right:none'><td title='Add/Edit/Remove Note' style='padding:0px 0px;border:2px solid black'>$addNote</td><td colspan='6' style='color:#007009;font-weight:bold;border:none;text-align:left'>&larr;Add/Edit/Remove Note</td></tr>"
    str += "</table></div>"
    return str
}

def noteMaint(){
    dynamicPage (name:"noteMaint", title: "<h2 style='color:blue'>Custom Note <span style='font-size: x-small'>v${version()}</span></h2>", install: false, uninstall: false, nextPage:mainPage) {
        dList = getDevList()
        if(dList.devList.size() <= 0){
            section("") {
                paragraph "<h1>No devices selected</h1>"
            }
        }else if(dList.devList.size() == 1) {
            section("<h4>Single Device Maintenance<h4>", hideable:false, hidden: false){
                paragraph "<span style='background-color:yellow;font-weight:bold'>Selected device: ${dList.devListName[0]}</span>"
                input "sdSave", "button", title:"<b>Save</b>", width:2, backgroundColor:'#007000',textColor:'#ffffff'
                input "sdRem", "button", title:"<b>Remove</b>", width:2, backgroundColor:'#700000',textColor:'#ffffff'
                input "hidden","hidden", title:"", width:8
                input "newKey", "text", title:"<b style='background-color:#87CECB'>New/Remove Key</b>",submitOnChange:true, width:6
                if(newKey) app.updateSetting("newKey",[value:"${toCamelCase(newKey)}",type:"text"])
                input "newVal", "text", title:"<b style='background-color:#87CECB'>New Note</b>",submitOnChange:true, width:6                               
                qryDevice.each{
                    if("${it.id}" == dList.devList[0]){ 
                        noteMap = it.getData()
                        noteMap.sort().each {
                            input "sdKey${it.key}", "text", title:"<b style='background-color:#87CECB'>${it.key}</b>", defaultValue:"${it.value}", submitOnChange:true, width:6
                        }
                    }
                }

                if (state.sdSave) {
                    state.sdSave = false
                    qryDevice.each{ dev ->
                        if("${dev.id}" == dList.devList[0]){
                            if(newKey && newVal){
                                dev.updateDataValue("$newKey", "$newVal")
                                app.removeSetting("newKey")
                                app.removeSetting("newVal")
                            }
                            settings.each{
                                if("${it.key}".contains("sdKey")){
                                    dev.updateDataValue(it.key.substring(5,),it.value)
                                    app.removeSetting("${it.key}")
                                }
                            }
                        }
                    }
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"
                }

                if (state.sdRem) {
                    state.sdRem = false
                    qryDevice.each{ dev ->
                        if("${dev.id}" == devList[0]){
                            dev.removeDataValue(newKey)
                            app.removeSetting("sdKey$newKey")
                            app.removeSetting("newKey")
                            app.removeSetting("newVal")
                        }
                    }
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"                    
                }
                input "mainPage", "button", title:"Return" 
                if(state.mainPg){
                    state.mainPg = false
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"
                }                
            }
        } else {
            section("<h4>Multi-Device Maintenance</h4>", hideable:false, hidden: false){
                paragraph "<span style='background-color:yellow;font-weight:bold'>Selected devices: ${dList.devListName.sort()}</span>"
                input "mdSave", "button", title:"<b>Save</b>", width:2, backgroundColor:'#007000',textColor:'#ffffff'
                input "mdRem", "button", title:"<b>Remove</b>", width:2, backgroundColor:'#700000',textColor:'#ffffff'
                input "hidden","hidden", title:"", width:8
                input "newKey", "text", title:"<b style='background-color:#87CECB'>New/Remove/Update Key</b>",submitOnChange:true, width:6
                if(newKey) app.updateSetting("newKey",[value:"${toCamelCase(newKey)}",type:"text"])
                input "newVal", "text", title:"<b style='background-color:#87CECB'>New/Updated Note</b>",submitOnChange:true, width:6                               
              
                if (state.mdSave) {
                    state.mdSave = false
                    qryDevice.each{ dev ->
                        if(state["devSel${dev.id}"]){
                            if(newKey && newVal){
                                dev.updateDataValue("$newKey", "$newVal")                                
                            }
                        }
                    }
                    app.removeSetting("newKey")
                    app.removeSetting("newVal")
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"
                }

                if (state.mdRem) {
                    state.mdRem = false
                    qryDevice.each{ dev ->
                        if(state["devSel${dev.id}"]){
                            dev.removeDataValue(newKey)
                        }
                    }
                    app.removeSetting("newKey")
                    app.removeSetting("newVal")
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"                    
                }
                 
                
                input "mainPage", "button", title:"Return" 
                if(state.mainPg){
                    state.mainPg = false
                    paragraph "<script>window.location.replace('${appLocation()}')</script>"
                }
            }
        }
    }
}

def getDevList(){
    devList = []
    devListName = []
    qryDevice.each {
        if(state["devSel${it.id}"]){
            devList.add("${it.id}") 
            devListName.add("${it.displayName}")
        }
    }
    return [devList:devList, devListName:devListName]
}

           

def toCamelCase(init) {
    if (init == null)
        return null;   
	
    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

def appButtonHandler(btn) {
    log.debug "$btn pressed"
    switch(btn) {
	case "addNote":
/*            if(it.controllerType == "LNK") {
                atomicState.meshedDeviceMsg+="<span style='background-color:red;font-weight:bold;color:white;'>$it is a Hub Mesh Device, note must be added to the <i>REAL</i> device to be retained</span><br>"
            }
		}
        if(atomicState.meshedDeviceMsg == "") atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
*/
        state.addNote = true
		break
	case "remNote":
		qryDevice.each{
			it.removeDataValue(noteName)
		}
		break
    case "sdSave":
        state.sdSave = true
        break
    case "sdRem":
        state.sdRem = true
        break
    case "mdSave":
        state.mdSave = true
        break
    case "mdRem":
        state.mdRem = true
        break
    case "mainPage":
        state.mainPg = true
        break
    default:
        if (btn.contains("devSel")){
            if(state?."$btn")
                state."$btn" = false
            else
                state."$btn" = true
        }
        else if (btn.contains("singleDev")){
            singleDev = "$btn".substring(9,)
            qryDevice.each{
                if("${it.id}" != singleDev)
                    state["devSel${it.id}"] = false
                else
                    state["devSel${it.id}"] = true
            }
            state.singleDev = true
            
        }
        else 
		    log.error "Undefined button $btn pushed"
		break
	}
}

ArrayList getFiles(){
    fileList =[]
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/fileManager/json",
        headers: [
            accept : "application/json"
        ],
    ]
    httpGet(params) { resp ->
        resp.data.files.each {
            fileList.add(it.name)
        }
    }
    
    return fileList.sort()
}

String readExtFile(fName){  
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true  
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return """${resp.data}"""
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
}


def fetchJS(){
    jsFile = readExtFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/iconify-icon.min.js")
    if(jsFile){
        bArray = (jsFile.getBytes("UTF-8"))
	    uploadHubFile("iconify-icon.min.js",bArray)
    } else
        log.error "iconify-icon.min.js not found"
}

def intialize() {

}

@Field static String ttStyleStr = "<style>.tTip {display:inline-block;border-bottom: 1px dotted black;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;text-align:left;}</style>"
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;} tr {border-right:2px solid black;}</style>"
