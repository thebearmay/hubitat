/*
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
 *    24Jun2024    thebearmay    Original Code
 */
    


static String version()	{  return '0.0.2'  }

//import groovy.json.JsonSlurper
//import groovy.json.JsonOutput
//import groovy.transform.Field


definition (
	name: 			"Device Attribute Iteration Storage", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Store a set number of attribute values based on pre-determined cycle.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devAttIterSto.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "attrSelect"

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
          section(name:"itDetail",title:"Iteration Retention Details", hideable: true, hidden: false){
             input "qryDevice", "capability.*", title: "Device Selection:", multiple: false, required: true, submitOnChange: true
             if (qryDevice != null) {
                 href "attrSelect", title: "Attribute Selection", required: false
                 input ("numIter","number",title:"Number of Iterations to Retain", submitOnChange: true, width:4)
                 input ("intType", "enum", title:"Reporting Interval Type", options: ["Minutes", "Hours", "Days"], submitOnChange: true, width:4)
                 input ("intVal", "number", title: "Reporting Interval Length", submitOnChange: true, width:4)
                 checkSubscriptions()
                 input ("stoLocation","string",title: "Local file name to use for Storage (will add .CSV)", submitOnChange:true, width:4)
                 if(stoLocation != null) {
                     input ("createFile", "button", title: "Create File")
                     if(state.fileCreateReq) {
                         state.fileCreateReq = false
                         fName = toCamelCase(stoLocation)+".csv"
                         app.updateSetting("stoLocation",[value:"${fName}",type:"string"])
                         initString = "\"timeStamp\""
                         valString ="\"${new Date().getTime()}\""
                         state.each {
                             if(it.key != "isInstalled" && it.key != "fileCreateReq"){
                                 initString+=","
                                 valString+=","
                                 initString+= "\"${it.key}\""
                                 valString+="\"${it.value}\""
                             }
                         }
                         bArray = (initString+"\n"+valString+"\n").getBytes("UTF-8")                       
                         uploadHubFile("${fName}",bArray)
                         scheduleReport()                         
                     }
                 }
             }
             
          }
          section("Change Application Name", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
          }
            section("Debug", hideable:false){
                input "debugEnabled", "bool", title:"Enable Debug Logging"
            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def attrSelect(){
    dynamicPage (name: "attrSelect", title: "Attribute Selection", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          dev = qryDevice
          def attrList=[]
          dev.supportedAttributes.each{
              attrList.add(it.toString())
          }
          sortedList=attrList.sort()
          sortedList.each{
              input "da-$it", "bool", title: "$it", required: false, defaultValue: false
          }


          paragraph "<p>$strWork</p>"
       }
    }
}

void checkSubscriptions(){
    unsubscribe()
    settings.each{
        if(it.value==true && it.toString().substring(0,3) == 'da-'){
            subscribe(qryDevice,"${it.key.substring(3,)}","holdValue")
            if(!state["${it.key.substring(3,)}"]) state["${it.key.substring(3,)}"] = qryDevice.currentValue("${it.key.substring(3,)}")
        }
    }
    subscribe("location", "systemStart","scheduleReport")
}

void holdValue(evt) {
    state["${evt.name}"] = evt.value
}

void scheduleReport(){
    switch(intType) {
        case "Minutes":
            mult = 60
            break
        case "Hours":
            mult = 60*60
            break
        case "Days":
            mult = 60*60*24
            break
    }
    runIn(mult*intVal, reportAttr)
}

void reportAttr(){
    valString ="\"${new Date().getTime()}\""
    state.each {
        if(it.key != "isInstalled" && it.key != "fileCreateReq"){
            valString+=","
            valString+="\"${it.value}\""
        }
    }
    fileRecords = (new String (downloadHubFile("${stoLocation}"))).split("\n")
    if(debugEnabled){
        fileRecords.each{
            log.debug "$it<br>"
        }
    }
    fileContents = ""
    if(fileRecords.size() <= numIter - 1)
        inx = 0
    else
        inx = fileRecords.size() - numIter 
    i=0
    fileRecords.each {
        if(debugEnabled) log.debug "$i $inx"
        if(i > inx)
            fileContents+="${it}\n"
        i++
    }
    bArray = (fileRecords[0]+"\n"+fileContents+valString+"\n").getBytes("UTF-8")                       
    uploadHubFile("${stoLocation}",bArray)
    scheduleReport()
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
        case "createFile":
            state.fileCreateReq = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
