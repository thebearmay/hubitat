/*
 * Govee Effects Player 
 *  
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
 *	Date               Who                    Description
 *	----------	-----------	---------------------------------------------------------
 *	18Dec2024	thebearmay	Add overRide processing, check for stop during effect list processing, kill switch
 *	19Dec2024				Add debug logging, additional kill switch checking
 *	20Dec2024				UI Cleanup
 *    
 */

import java.time.*
import java.time.format.DateTimeFormatter
static String version()	{  return '0.0.8'  }


import groovy.transform.Field


definition (
	name:		"Govee Effect Player", 
	namespace:	"thebearmay", 
	author:		"Jean P. May, Jr.",
	description: 	"Play back Govee Effects",
	category:	"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/gEffects.groovy",
	installOnOpen:  true,
	oauth:		false,
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
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   

         section(name:"Effect_Files",title:"<span style='font-weight:bold;color:blue;text-decoration:underline;'>Select Effect List</span>", hideable: false, hidden: false){
             fileList = getFiles()
             effList = []
             fileList.each {
                 if("$it".contains('.txt'))
                     effList.add("$it")                     
             }
             input("effFile","enum", title:"Name of Local File for Effects", options:effList, width: 4, submitOnChange:true)
         }
		 section(name:"Effect_Specifics", title:"<span style='font-weight:bold;color:blue;text-decoration:underline;'>Effect Specifics</span>"){
             
			 input("minEff","number",title:"Default Effect Time (Minutes)", width:4, submitOnChange: true, defaultValue: 5)
             input("devList","capability.*", title:"Lights to Send Effects to", width: 4, submitOnChange:true, multiple:true)
         }
         section(name:"timeBased", title:"<span style='font-weight:bold;color:blue;text-decoration:underline;'>Time Based Effects</span>"){
             input("startDate","date",title:"Start Date", width: 4, submitOnChange:true)
             input("startTime","time",title:"Start Time", width: 4, submitOnChange:true)
             paragraph "&nbsp;"
             input("endDate","date",title:"End Date", width: 4, submitOnChange:true)
             input("endTime","time",title:"End Time", width: 4, submitOnChange:true)
			 paragraph "&nbsp;"
			 input("goBtn","button",title:"Start Timer", width: 4, submitOnChange:true)
             if(endTime < startTime) paragraph "<span style = 'color:red;font-weight:bold'>End Time less than Start Time</span>"
             if(endDate < startDate) paragraph "<span style = 'color:red;font-weight:bold'>End Date less than Start Date</span>"
             if(state.goBtn){
             	state.goBtn = false
             	runEffectList()
             }
         }
         section(name:"switchBased", title:"<span style='font-weight:bold;color:blue;text-decoration:underline;'>Switch Based Effects</span>"){
			 input("overRide","capability.switch", title:"Time Overide Switch", width:4, submitOnChange:true)
			 if(overRide)
				subscribe(overRide,"switch","overRideEffectRun")
			 else
				unsubscribe()             
         }
         section("<span style='font-weight:bold;color:blue;text-decoration:underline;'>Settings</span>", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            input("killSw", "bool",title:"Stop/Block Effects from Running",width:4,submitOnChange:true)
			input("debugEnable", "bool",title:"Debug",width:4,submitOnChange:true)
         }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

void setNextRun(){
    unschedule()
    if(LocalDate.parse(endDate) < LocalDate.now())
    	return
	if(LocalDate.parse(startDate) <= LocalDate.now()){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")
        sTime = LocalTime.parse(startTime, formatter)
		if(sTime > LocalTime.now())
			tDate = LocalDate.now()
		else
			tDate = LocalDate.now().plusDays(1)
		if(debugEnable)
			log.debug "${tDate.getYear()} ${tDate.getMonthValue()} ${new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0)}"
		runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0), "runEffectList")
	} else {
		tDate = LocalDate.ofEpochDay(startDate.toLong())
		if(debugEnable)
			log.debug "${tDate.getYear()} ${tDate.getMonthValue()}<br>${new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0)}"		
        runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0), "runEffectList")
    }  
}

void runEffectList(){
    if (!startTime || !endTime || !startDate || !endDate){
        log.error "Missing date or time information"
    	return
    }
    if(killSw == null) app.updateSetting('killSw',[type:'bool',value:false])
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")
    sTime = LocalTime.parse(startTime, formatter)
    if(LocalDate.parse(startDate) > LocalDate.now() || LocalTime.now() < sTime){
        setNextRun()
        return
    }
	if((overRide && overRide.currentValue("switch",true) == 'on') || app.getSetting('killSw')==true) {
		if(debugEnable)
			log.debug " $overRide:${overRide.currentValue("switch")} kill:$killSw"
		return
	}
    fileRecords = (new String (downloadHubFile("${effFile}"))).split("\n")
    
    eTime = LocalTime.parse(endTime, formatter)
    while (LocalTime.now() < eTime && app.getSetting('killSw')==false){
        fileRecords.each {
			if(debugEnable) 
            log.debug "time:${LocalTime.now() < eTime } kill:${app.getSetting('killSw')}"
			if(LocalTime.now() < eTime && app.getSetting('killSw')==false) {
				flds = it.split(":")
				devList.each {
					if(app.getSetting('killSw')==false){
						if(debugEnable) 
							log.debug "set effect ${flds[0]} on ${it.displayName}"
						it.setEffect(flds[0])
					}
				}
				if(flds.size() > 2){
					if(debugEnable)
						log.debug "pausing ${flds[2].toInteger()} minutes"
					pauseExecution(flds[2].toInteger() * 60 * 1000)
				} else {
					if(!minEff) minEff = 5
					if(debugEnable)
						log.debug "pausing ${flds[2].toInteger()} minutes"
					pause(minEff * 60 * 1000)
				}
			}
        }
    }
    setNextRun()
}

void overRideEffectRun(evt){
    if(killSw == null) app.updateSetting('killSw',[type:'bool',value:false])
    fileRecords = (new String (downloadHubFile("${effFile}"))).split("\n")
    while (overRide.currentValue("switch", true) == 'on' && app.getSetting('killSw')==false){
        fileRecords.each {
			if(debugEnable) 
            log.debug "overRide:${overRide.currentValue("switch", true)} kill:${app.getSetting('killSw')}"		
			if(overRide.currentValue("switch", true) == 'on' && app.getSetting('killSw')==false) {
				flds = it.split(":")
				devList.each {
					if(app.getSetting('killSw')==false){			
						if(debugEnable) 
							log.debug "set effect ${flds[0]} on ${it.displayName}"				
						it.setEffect(flds[0])
					}
				}
				if(flds.size() > 2){
					if(debugEnable)
						log.debug "pausing ${flds[2].toInteger()} minutes"				
					pauseExecution(flds[2].toInteger() * 60 * 1000)
				} else {
					if(!minEff) minEff = 5
					if(debugEnable)
						log.debug "pausing ${flds[2].toInteger()} minutes"					
					pause(minEff * 60 * 1000)
				}
			}
        }
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

    if(debugEnable) log.debug "toCamelCase return $ret"
    return ret;
}

def appButtonHandler(btn) {
    switch(btn) {
        case "goBtn":
        	state.goBtn = true
        	break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
