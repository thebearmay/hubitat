/*
 * HPM Manifest Generator
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
 *    23Nov2022    thebearmay    Fix Repository Entry
*/

static String version()	{  return '1.0.2'  }

import java.text.SimpleDateFormat
#include thebearmay.localFileMethods
import groovy.transform.Field
@Field hpmTags = [
		"Alarm Systems",
		"Appliances",
		"Automations & Groups",
		"Bathroom",
		"Buttons",
		"Cleaning Devices",
		"Climate Control",
		"Cloud",
		"Dashboards",
		"Doors & Windows",
		"Energy Monitoring",
		"Garage Doors",
		"Health & Fitness",
		"IR & RF",
		"Irrigation",
		"LAN",
		"Lights & Switches",
		"Locks",
		"Misc. Devices",
		"Monitoring",
		"Motion Control",
		"Multimedia",
		"Multi Sensors",
		"Notifications",
		"Pets & Animals",
		"Pools & Spas",
		"Presence & Location",
		"Repeaters & Extenders",
		"Safety & Security",
		"Scales",
		"Shower",
		"Sleep",
		"Speakers",
		"Temperature & Humidity",
		"Timers",
		"Tools & Utilities",
		"Valves",
		"Vehicles & Transportation",
		"Voice Assistants",
		"Water Heater",
		"Water",
		"Weather",
		"Window Coverings",
		"Zigbee",
		"ZWave"
	]


definition (
	name: 			"HPM Manifest Generator", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Air Things Allview Cloud Interface",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hpmManifestGen.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}


void installed() {
	if(debugEnabled) log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "updated()"
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
            section("<span style='color:blue;font-weight:bold;font-size:x-Large'>${app.getLabel()}  <span style='font-size:x-small'>v${version()}</span></span>") {
				input "debugEnabled", "bool", title:"Enable Debug Logging:", submitOnChange:true, required:false, defaultValue:false, width:4
                if(debugEnabled) {
                    unschedule()
                    runIn(1800,logsOff)
                }
                input "retainRepo", "bool", title:"Retain Repository Information", width:4
                input "clearAll", "button",title:"Clear Workspace"
                if(state.clearReq) {
                    state.clearReq = flase
                    settings.each{
                        if("$it.key" != "nameOveride" && "$it.key" != "debugEnabled" && "$it.key" != "retainRepo" && !("$it.key" == "reposLoc" && retainRepo)) {
                            app.removeSetting("$it.key")
                        }
                    }
                    state.remove("aText")
                    state.remove("bText")
                    state.remove("dText")
                    state.remove("manifest")

                }
                
            }
            section("<span style='color:blue;font-weight:bold'>Header Information</span>", hideable: true, hidden: false){
                sdf = new SimpleDateFormat("yyyy-MM-dd")
                cDate = new Date()
                                       
                input "packageName", "string", title: "Package Name", required: true, submitOnChange: true
                input "author", "string", title: "Author", required: true, submitOnChange: true
                input "versionStr", "string", title: "Version", required: true, submitOnChange: true, width:4
                input "minimumHEVersion", "string", title: "Minimum HE Version", submitOnChange: true, defaultValue:"${location.hub.firmwareVersionString}", width:4
                input "dateReleased", "string", title: "Date Released (yyyy-mm-dd)", submitOnChange: true, defaultValue: "${sdf.format(cDate)}", width:4
                input "releaseNotes", "string", title: "Release Notes", submitOnChange: true
                input "documentationLink", "string", title: "Documentation Link", submitOnChange: true
                input "communityLink", "string", title: "Link to the Community Thread", submitOnChange: true

            }
            
            section("<span style='color:blue;font-weight:bold'>Bundles</span>", hideable: true, hidden: false){  
                input "newBundle", "button", title: "+", width:1
                input "clearBundle", "button", title: "Clear", width:1
                if(state.newBundleReq){
                    input "bName", "string", title:"Bundle Name",submitOnChange:true
                    input "bNameSp", "string", title:"Name Space",submitOnChange:true
                    input "bLoc", "string", title:"Location",submitOnChange:true
                    input "bReq", "bool", title: "Required",submitOnChange:true, defaultValue: true, width:4
                    input "bundleSave", "button", title: "Save"
                }
                if(state.bText != null) paragraph "<pre>${state.bText}</pre>"
                if(state.clearBundle) {
                    state.clearBundle = false
                    settings.each{
                        if("$it.key".startsWith("b")) {
                            app.removeSetting("$it.key")
                        }
                    }
                    state.remove("bText")
                }

            } 
            
            section("<span style='color:blue;font-weight:bold'>Apps</span>", hideable: true, hidden: false){  
                input "newApp", "button", title: "+", width:1
                input "clearApp", "button", title: "Clear", width:1
                if(state.newAppReq){
                    input "aName", "string", title:"App Name",submitOnChange:true
                    input "aNameSp", "string", title:"Name Space",submitOnChange:true
                    input "aLoc", "string", title:"Location",submitOnChange:true
                    input "aReq", "bool", title: "Required",submitOnChange:true, defaultValue: true, width:4
                    input "aOauth", "bool", title: "Uses oAuth",submitOnChange:true, defaultValue:false, width:4
                    input "aPrimary", "bool", title: "Primary",submitOnChange:true, defaultValue: true, width:4
                    input "appSave", "button", title: "Save"
                }
                if(state.aText != null) paragraph "<pre>${state.aText}</pre>"
                
                if(state.clearApp) {
                    state.clearApp = false
                    settings.each{
                        if("$it.key".startsWith("a")) {
                            app.removeSetting("$it.key")
                        }
                    }
                    state.remove("aText")
                }                

            }            
           
            section("<span style='color:blue;font-weight:bold'>Drivers</span>", hideable: true, hidden: false){  
                input "newDriver", "button", title: "+", width:1
                input "clearDriver", "button", title: "Clear", width:1
                if(state.newDriverReq){
                    input "dName", "string", title:"Driver Name",submitOnChange:true
                    input "dNameSp", "string", title:"Name Space",submitOnChange:true
                    input "dLoc", "string", title:"Location",submitOnChange:true
                    input "dReq", "bool", title: "Required",submitOnChange:true, defaultValue: true, width:4
                    input "driverSave", "button", title: "Save"
                }
                if(state.dText != null) paragraph "<pre>${state.dText}</pre>"

                if(state.clearDriver) {
                    state.clearDriver = false
                    settings.each{
                        if("$it.key".startsWith("d")) {
                            app.removeSetting("$it.key")
                        }
                    }
                }                

            } 
            
            section("<span style='color:blue;font-weight:bold'>Generate</span>", hideable: true, hidden: false){            
                input "genManifest", "button", title: "Generate Manifest"
                if(state.genManifest) {
                    man="{\n \"packageName\":\"$packageName\",\n"
                    man+=" \"author\":\"$author\",\n"
                    man+=" \"version\":\"$versionStr\",\n"
                    man+=" \"minimumHEVersion\":\"$minimumHEVersion\",\n"
                    man+=" \"dateReleased\":\"$dateReleased\""
                    if(releaseNotes != null) man+=",\n \"releaseNotes\":\"$releaseNotes\""
                    if(documentationLink != null) man+=",\n \"documentationLink\":\"$documentationLink\""
                    if(communityLink != null) man+=",\n \"communityLink\":\"$communityLink\""
                    if(state.bText != null) man+= ",\n \"bundles\": [\n$state.bText\n  ]"
                    if(state.aText != null) man+= ",\n \"apps\": [\n$state.aText\n  ]"  
                    if(state.dText != null) man+= ",\n \"drivers\": [\n$state.dText\n  ]"   
                    man+="\n}"
                    state.manifest = man
                    state.genManifest = false
                }
                if(state.manifest) paragraph "<pre>$state.manifest</pre>"
            }
            section("<span style='color:blue;font-weight:bold'>Add Repository Entry</span>", hideable: true, hidden: false){            
                input "reposLoc", "string", title: "Location of Repository", submitOnChange:true
                input "reposCat", "enum", title: "Category", submitOnChange:true, options:["Control","Convenience","Integrations","Notifications","Security","Utility"], width:4
                input "reposTag", "enum", title: "Tags", submitOnChange:true, options: hpmTags, multiple:true, width:4
                input "reposManLoc", "string", title: "Manifest Location"
                input "reposDesc", "string", title: "Description for HPM Search Display"
                
                input "genRepos", "button", title: "Generate Merged Repository"                
                if(state.genRepos) {
                    reposWork = readExtFile("$reposLoc")
                    reposWork = reposWork.substring(0,reposWork.size()-3)
                    reposWork = reposWork.substring(0,reposWork.lastIndexOf("}")+1)
                    reposWork +=",\n    {\n"
                    reposWork += "      \"id\":\"${GUID()}\",\n"
                    reposWork += "      \"name\":\"$packageName\",\n"
                    reposWork += "      \"category\":\"$reposCat\",\n"
                    reposWork += "      \"location\":\"$reposManLoc\",\n"
                    reposWork += "      \"description\":\"$reposDesc\""
                    if(reposTag != null) {
                        firstTag = true
                        reposWork += ",\n      \"tags\":["
                        reposTag.each{
                            if(firstTag) {
                                firstTag = false
                                reposWork += "\n        \"$it\""
                            } else
                                reposWork +=",\n        \"$it\""
                        }
                        reposWork +="\n       ]\n"
                    }
                    else
                        reposWork += "\n"                    
                    reposWork += "    }\n  ]\n}"
                    paragraph "<pre>$reposWork</pre>"
                    state.genRepos = false
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

String GUID(){
    UUID uuid = UUID.randomUUID()
    return uuid.toString()
}

void appButtonHandler(btn) {
    switch(btn) {
        case ("newBundle"):
            state.newBundleReq = true
            break
        case ("bundleSave"):
            if(state.bText != null) bText=",\n"
            else bText = ""
            bText+="  {\n     \"id\":\"${GUID()}\",\n"
            bText+="     \"name\":\"$bName\",\n"
            bText+="     \"location\":\"$bLoc\",\n"
            bText+="     \"required\":$bReq\n  }"
            if(state.bText != null) state.bText += bText
            else state.bText = bText
            state.newBundleReq = false
            break
        case ("newApp"):
            state.newAppReq = true
            break        
        case ("appSave"):
            if(state.aText != null) aText=",\n"
            else aText = ""
            aText+="  {\n     \"id\":\"${GUID()}\",\n"
            aText+="     \"name\":\"$aName\",\n"
            aText+="     \"location\":\"$aLoc\",\n"
            aText+="     \"required\":$aReq,\n"
            aText+="     \"oauth\":$aOauth,\n"
            aText+="     \"primary\":$aPrimary\n  }"
            if(state.aText != null) state.aText += aText
            else state.aText = aText
            state.newAppReq = false
            break  
        case ("newDriver"):
            state.newDriverReq = true
            break
        case ("driverSave"):
            if(state.dText != null) dText=",\n"
            else dText = ""
            dText+="  {\n     \"id\":\"${GUID()}\",\n"
            dText+="     \"name\":\"$dName\",\n"
            dText+="     \"location\":\"$dLoc\",\n"
            dText+="     \"required\":$dReq\n  }"
            if(state.dText != null) state.dText += dText
            else state.dText = dText
            state.newDriverReq = false
            break          
        case ("clearAll"):
            state.clearReq = true
            break
        case ("clearBundle"):
            state.remove("bText")
            state.clearBundle = true
            break        
        case ("clearApp"):
            state.remove("aText")
            state.clearApp = true
            break        
        case ("clearDriver"):
            state.remove("dText")
            state.clearDriver = true
            break        
        case ("genManifest"):
            state.genManifest= true
            break        
        case ("genRepos"):
            state.genRepos= true
            break        
        default: 
            if(debugEnabled) log.error "Undefined button $btn pushed"
            break
      }
}

void intialize() {

}

void uninstalled(){
    chdList = getChildDevices()
    chdList.each{
        deleteChildDevice(it.getDeviceNetworkId())
    }
}
