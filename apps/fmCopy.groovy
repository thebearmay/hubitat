/*
 * File Manager Copy/Rename 
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


static String version()	{  return '1.0.0' }

definition (
	name: 			"File Manager Copy / Rename", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"File Manager Copy / Rename -  Utility to copy or rename files in File Manager",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/fmCopy.groovy",
    installOnOpen:  true,
	oauth: 			false,
    singleThreaded: true,
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
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<h2>File Manager Copy /  Rename</h2><p style='font-size:small'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
            section("<h3>Main</h3>"){
                fList = listFiles().fList
                i=0
                paragraph "<span style='font-weight:bold;color:blue'>Enter a new name for each file to be copied or renamed</span>"
                fList.each{
                    i++
                    input "fAlt$i", "string", title:"<b>$it</b>", width:8, submitOnChange:true
                }
                input "renameOnly", "bool", title:"<b>Turn on to rename, off to copy</b>", submitOnChange:true
                input "execFunc", "button", title: "Execute Rename/Copy", width:2
                if(state.exec) {
                    state.exec = false
                    i=0
                    fList.each{
                        i++
                        if(settings["fAlt$i"]){
                            fBuff = downloadHubFile("$it")
                            uploadHubFile(settings["fAlt$i"],fBuff)
                            if(renameOnly) deleteHubFile("$it")
                            app.removeSetting("fAlt$i")
                        }
                    }
                    paragraph "<script type='text/javascript'>location.reload()</script>"
                }

            }    
            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            }            
        }
    }
}

@SuppressWarnings('unused')
HashMap listFiles(retType='nameOnly'){
    fileList = []
    json=getHubFiles("")
    //for (rec in json) {
    json.each {
        if(it.type == 'file')
            fileList << it.name.trim()
    }
    if(debugEnabled) log.debug fileList.sort()
    if(retType == 'json') 
        return [jStr: json]
    else
        return [fList: fileList.sort()]
}

def appButtonHandler(btn) {
    switch(btn) {
        case "execFunc":
            state.exec = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}               
