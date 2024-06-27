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
 *    25Jun2024                  add Restart button, Max/Min/Avg options
 *    26Jun2024                  Fix bug in computed reporting
 *    27Jun2024                  Changes to interface with the UI app, Value interval, purge unused preferences
 */
    


static String version()	{  return '0.0.5'  }

//import groovy.json.JsonSlurper
//import groovy.json.JsonOutput
//import groovy.transform.Field


definition (
	name: 			"Device Attribute Iterative Storage - Acquisition", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Store a set number of attribute values based on pre-determined cycle.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devAttIterSto.groovy",
    parent:         "thebearmay:Device Attribute Iterative Storage - UI",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "attrSelect"
    page name: "compAttr"

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
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: false) {
      	if (app.getInstallationState() == 'COMPLETE') {   
          section(name:"itDetail",title:"Iteration Retention Details", hideable: true, hidden: false){
             input "qryDevice", "capability.*", title: "Device Selection:", multiple: false, required: true, submitOnChange: true
             if (qryDevice != null) {
                 href "attrSelect", title: "Attribute Selection", required: false
                 href "compAttr", title: "Computed Values (Max/Min/Avg)", required: false
                 input ("numIter","number",title:"Number of Iterations to Retain", submitOnChange: true, width:4)
                 input ("intType", "enum", title:"Reporting Interval Type", options: ["Minutes", "Hours", "Days","Value Change"], submitOnChange: true, width:4)
                 input ("intVal", "number", title: "Reporting Interval Length", submitOnChange: true, width:4)
                 
                 input ("stoLocation","string",title: "Local file name to use for Storage (will add .CSV)", submitOnChange:true, width:4)
                 
                 if(stoLocation != null) {
                     input ("createFile", "button", title: "Create File", width:4)
                     if(state.fileCreateReq) {
                         purgeOldStates()
                         checkSubscriptions()
                         state.fileCreateReq = false
                         fName = toCamelCase(stoLocation)+".csv"
                         app.updateSetting("stoLocation",[value:"${fName}",type:"string"])
                         initString = "\"timeStamp\""
                         valString ="\"${new Date().getTime()}\""
                         state.sort().each {
                             if(it.key != "isInstalled" && it.key != "fileCreateReq" && it.key != "rptRestart" && !it.key.contains('count')){
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
                     input ("restart", "button", title: "Restart Reporting", width:4)
                     if(state.rptRestart) {
                         state.rptRestart = false
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
              input "da-$it", "bool", title: "$it", required: false, defaultValue: false, width:4
          }


          paragraph "<p>$strWork</p>"
       }
    }
}

def compAttr(){
    dynamicPage (name: "compAttr", title: "Computed Values (Max/Min/Avg)", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          dev = qryDevice
          def attrList=[]
          dev.supportedAttributes.each{
              if(it.dataType == "NUMBER")
                  attrList.add(it.toString())
          }
          sortedList=attrList.sort()
          sortedList.each{
              input "ca-max-$it", "bool", title: "$it Max", required: false, defaultValue: false, width:4
              input "ca-min-$it", "bool", title: "$it Min", required: false, defaultValue: false, width:4
              input "ca-avg-$it", "bool", title: "$it Avg", required: false, defaultValue: false, width:4
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
            state["${it.key.substring(3,)}"] = qryDevice.currentValue("${it.key.substring(3,)}")
        }
        if(it.value==true && it.toString().substring(0,3) == 'ca-'){
            subscribe(qryDevice,"${it.key.substring(7,)}","holdValue")
            state["${it.key.substring(7,)}"] = qryDevice.currentValue("${it.key.substring(7,)}")
            state["${it.key.substring(3,)}"] = qryDevice.currentValue("${it.key.substring(7,)}")
            if("${it.key.substring(3,6)}" == "avg") {
                state["${it.key.substring(3,)}-count"] = 1
            }
        }
    }
    subscribe(location, "systemStart","scheduleReport")
}

void holdValue(evt) {
    if(degubEnabled) log.debug "entering holdValue<br>${evt.properties}"
    state["${evt.name}"] = evt.value
    
    if(debugEnabled) log.debug "1 ${state["min-${evt.name}"]} ${evt.getNumericValue()}"
    if(state["min-${evt.name}"] && evt.getNumericValue() < state["min-${evt.name}"]) 
        state["min-${evt.name}"] = evt.getNumericValue()
    
    if(debugEnabled) log.debug "2"
    if(state["max-${evt.name}"] && evt.getNumericValue() > state["max-${evt.name}"]) 
        state["max-${evt.name}"] = evt.getNumericValue()
    
    if(debugEnabled) log.debug "3"
    if(state["avg-${evt.name}"] ) {
        state["avg-${evt.name}"] += evt.getNumericValue()
        state["avg-${evt.name}-count"]++        
    }
    if(debugEnabled) log.debug "finished holdValue"
    if(intType == "Value Change") reportAttr()
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
    if(intType != "Value Change")
        runIn(mult*intVal, reportAttr)
}

def getPref(keyVal){
    return settings["$keyVal"]
}

void reportAttr(){
    valString ="\"${new Date().getTime()}\""
    state.sort()each {
        if(it.key != "isInstalled" && it.key != "fileCreateReq" && it.key != "rptRestart"){
            if(!it.key.contains('avg') && !it.key.contains('count')) {
                valString+=","
                valString+="\"${it.value}\""
            } else if(it.key.contains('avg-') && !it.key.contains('count')) {
                valString+=","
                valString+="\"${(it.value/state["${it.key}-count"]).toFloat().round(2)}\""
                state["${it.key}-count"] = 1  
                state["${it.key}"] = state["${it.key.substring(4,)}"].toFloat()
            }
            if(it.key.contains('min-') || it.key.contains('max-') )
               state["${it.key}"] = state["${it.key.substring(4,)}"].toFloat()
               
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

void purgeOldStates(){
    oldState = state
    state = [:]
    oldState.each{
        if(it.key == "isInstalled" || it.key == "fileCreateReq" || it.key == "rptRestart" || settings["da-${it.key}"] || settings["ca-${it.key}"] ){
            state.put(it.key, it.value)
        }
    }
    settings.each{
        if(!(it.value))
            app.removeSetting("$it.key")
    }
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
        case "restart":
            state.rptRestart = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
