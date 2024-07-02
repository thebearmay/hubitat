/*
 * Device Attribute Iterative Storage - UI
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date          Who          Description
 *    ----------   ------------  ------------------------------------------------
 *    27Jun2024    thebearmay    v0.0.1 Original Code
 *    28Jun2024                  v0.0.2 Small UI tweaks
 *                               v0.0.3 Make device reference a link
 *    02Jul2024                  v0.0.4 Use same window when creating child, presentation cleanup
 *                               v0.0.5 Disable/Enable logic
 */
    


static String version()	{  return '0.0.5'  }

//import groovy.json.JsonSlurper
//import groovy.json.JsonOutput
import groovy.transform.Field


definition (
	name: 			"Device Attribute Iterative Storage - UI", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"User Interface/Parent for Device Attribute Iterative Storage",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devAttIterStoUI.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"

}
mappings {

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
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
          section(name:"userInterface",title:"Iteration Storage UI", hideable: true, hidden: false){
            paragraph listTable()
            //input "addSto", "button", title:"Create new Item"
            if(state.addReq) {
                state.addReq = false
                chd = addChildApp("thebearmay", "Device Attribute Iterative Storage - Acquisition", "DAIS ${new Date().getTime()}")
                paragraph "<script>location.href='http://${location.hub.localIP}/installedapp/configure/${chd.id}/mainPage';setTimeout(() => {window.close()},1000);</script>"
            }
           
             
          }
          section("Change Application Name", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
          }
            section("", hideable:false){
                input "debugEnabled", "bool", title:"Enable Debug Logging"
            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

String listTable() {
    ArrayList<String> tHead = ["","Disable","Name","Device","Attributes","Interval","Output File","<i style='font-size:1.125rem' class='material-icons he-bin'></i>"]
    String X = "<i class='he-checkbox-checked'></i>"
    String O = "<i class='he-checkbox-unchecked'></i>"
    String settingsIcon = "<i class='material-icons app-column-info-icon' style='font-size: 24px;'>settings_applications</i>"
    String removeIcon = "<i class='material-icons he-bin'></i>"


    String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
    str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px } tr {border-right:2px solid black;}" +
            "</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style='border-left:2px solid black;border-top:2px solid black;'>" +
            "<thead><tr style='border-bottom:2px solid black'>"
    tHead.each { str += "<th><strong>${it}</strong></th>" }
    str += "</tr></thead>"
    
    getChildApps().each{      
        str += "<tr><td><a href='http://${location.hub.localIP}/installedapp/status/${it.id}' target='_self'>$settingsIcon</a></td>"
        String appDis = buttonLink("appDis${it.id}", "${it.getPref('appDisable')? X : O}", "#000000", "12px")
        str += "<td>$appDis</td>"
        str += "<td><a href='http://${location.hub.localIP}/installedapp/configure/${it.id}/mainPage', target='_self'>${it.label}</a></td>"
        str += "<td><a href='http://${location.hub.localIP}/device/edit/${it.getPref('qryDevice')?.id}' target='_blank'>${it.getPref('qryDevice')}</a></td>"
        attrList = ""
        i=0
        it.state.sort().each {
            if(!ignoreState.toString().contains("${it.key}") && !it.key.contains("-count")){
                if(i>0) attrList+= ", "
                attrList += it.key
                i++
            }
        }
        str += "<td>${attrList}</td>"
        if(!it.getPref('intType')?.contains("Value"))
            wkStr = "${it.getPref('intVal')}${it.getPref('intType')?.substring(0,1)}"
        else
            wkStr = "Value"
        str += "<td>$wkStr</td>"
        str += "<td><a href='http://${location.hub.localIP}/local/${it.getPref('stoLocation')}'>${it.getPref('stoLocation')}</a></td>"
        String remSto = buttonLink("remSto${it.id}", "$removeIcon", "#ff0000", "6px")
        str += "<td>$remSto</td></tr>"
    }

    String addSto = buttonLink("addSto", "<b>＋</b>", "#007009", "25px")
    str += "<tr style='border-top:2px solid black;border-right:none'><td title='Add new storage definition' style='padding:0px 0px;border:2px solid black'>$addSto</td><td colspan='6' style='color:#007009;font-weight:bold;border:none;text-align:left'>&larr;Add new storage definition</td></tr>"
    str += "</table></div>"


    return str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

String toCamelCase(init) {
    if (init == null)
        return null;
    init = init.replaceAll("[^a-zA-Z0-9]+","")
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
    switch(btn) {    
        case "addSto":
            state.addReq = true
            break
        default: 
            if (btn.contains("remSto")){
                cid=btn.substring(6,).toInteger()
                deleteChildApp(cid)
                break
            }
            if (btn.contains("appDis")){
                cid=btn.substring(6,).toLong()
                chd=app.getChildAppById(cid)
                if(chd.getPref('appDisable')){
                    chd.setPref('appDisable','false','bool')
                    chd.scheduleReport()
                } else {
                    chd.setPref('appDisable','true','bool')
                    chd.unschedule('reportAttr')
                }
                break
            }       
            log.error "Undefined button $btn pushed"
            break
    }
}
@Field ignoreState = ["isInstalled","fileCreateReq","rptRestart","returnReq","appDisable"]
