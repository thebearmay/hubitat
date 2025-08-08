/*
 * Lock Event History 
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
 *    2021-04-29    thebearmay    Original version 0.1.0
 *    2021-04-30    thebearmay    Add alternate code names
 *    2021-05-04    thebearmay    2.2.7.x changes
 *    2021-12-28    thebearmay    return State as a map
 *    2022-06-20    thebearmay    remove embedded sections
 *    2023-05-16    thebearmay    allow leading zeros in code
 *	  2025-07-10	thebearmay	  allow alternate names for lock devices
 *	  2025-08-06	thebearmay	  fix the ignorePhysical logic
 *	  2025-08-08	thebearmay	  digital event not being sent for keypad/fingerprint 
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.2.6'  }


definition (
	name: 			"Lock History", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Display a history of the events for locks, allows side-by-side display of multiple devices for comparisons",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/lockHistory.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "lockHistory"
   page name: "altName"
   page name: "altLockName"
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    state.altNames = [:]
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
	    	section("Main")
		    {
                input "qryDevice", "capability.lockCodes", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                input "qryDate", "string", title: "Pull event data from this date forward (yyyy-MM-dd hh:mm):", required: true, submitOnChange: true
                if (qryDevice != null && qryDate != null) href "lockHistory", title: "Lock History", required: false
                href "altName", title: "Maintain Alternate Names for events that return 'unknown codeNumber:x' or 'code #x'", required: false
                href "altLockName", title: "Maintain Alternate Names for lock devices", required: false
                input "notifyDevice", "capability.notification", title: "Notification Devices:", multiple: true, submitOnChange: true
                input "ignorePhysical", "bool", title: "Only report unlock events from digital events"
                if(notifyDevice?.size() > 0) 
                    lockSubscribe()
                else
                    unsubscribe()
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def lockSubscribe(){
    unsubscribe()
    qryDevice.each {
        if(debugEnabled) log.debug "Subscribing to lock for $it"
        subscribe(it, "lock", "lockHandler")
    }
}

def lockHandler(evt){ 
    if(evt.value == "unlocked") {
        notifyDevice.each {
			if(ignorePhysical && evt.descriptionText.indexOf(' by ') < 0) {
                log.info "$evtDev physical unlock event notification skipped"
            } else {
	            evtDev = evt.getDevice()
    	        if(settings["lock${evt.deviceId}"]) evtDev = settings["lock${evt.deviceId}"]
        	    if(evt.descriptionText.contains("unknown codeNumber:") || evt.descriptionText.contains("code #")){
            	    altNam = findAltName(evt.descriptionText)
                	it.deviceNotification("$evtDev was unlocked by $altNam")
                } else {
                	evtDesc = evt.descriptionText.substring(evt.descriptionText.indexOf("was"))
                	it.deviceNotification("$evtDev ${evtDesc}")
                }
            }
        }
    }
}

def lockHistory(){
    dynamicPage (name: "lockHistory", title: "", install: false, uninstall: false, nextPage:"mainPage") {
        section("Lock History"){          
          dispTable = buildTable()
          paragraph  "$dispTable"  
        }
 
        section ("Lock Details", hideable: true, hidden: false) {  
                input  "codeRec", "bool", title:"Last Code Records", defaultValue: true, submitOnChange:true, width:4
                input  "unlockRec", "bool", title:"Unlock Records", defaultValue: true, submitOnChange: true, width:4
                input  "lockRec", "bool", title:"Lock Records", defaultValue: true, submitOnChange: true, width:4
                input "qryDate", "string", title: "Pull event data from this date forward (yyyy-MM-dd hh:mm):", required: true, submitOnChange: true, width:8
                input "refreshTable", "button", title:"Refresh Table"
            }
      
    }
}

String buildTable(){
    dispTable = "<style type='text/css'>.mtable {border:1px black solid;padding:0px;width:100%;}.mth {border:1px black solid; min-width:16em;} .mtd {border:1px black solid;vertical-align:text-top;}</style><div class='.mdiv'><table class='mtable'><tr>"
    evtList = {}
    evtArr = []
    for(i=0;i<qryDevice.size();i++){
        evtArr[i]=[]     
        dName = qryDevice[i].displayName
        if(settings["lock${qryDevice[i].deviceId}"]) 
        	dName = settings["lock${qryDevice[i].deviceId}"]
        dispTable+="<th class='mth'> $dName </th>"
        def evtList = [] 
        if(codeRec) evtList+=qryDevice[i].statesSince("lastCodeName",Date.parse("yyyy-MM-dd hh:mm", qryDate),[max:100])
        if(unlockRec || lockRec) evtList+=qryDevice[i].statesSince("lock",Date.parse("yyyy-MM-dd hh:mm", qryDate),[max:100])
        evtList.each {        
      //      paragraph it.toString()
            stateParts = parseState(it.toString())
 
            pDate=stateParts["date"]
            p4Trim = stateParts["value"].trim()

            if(p4Trim.contains("unknown codeNumber:") || p4Trim.contains("code #")) p4Trim = findAltName(p4Trim)
            if(pDate.length() < 23) pDate = pDate + "0"
            
            if (p4Trim == "unlocked" && unlockRec){
                evtArr[i].add("$pDate\t\t   [Unlock Event]")
            } else if (p4Trim == "locked" && lockRec){
                evtArr[i].add("$pDate\t\t   [Lock Event]")
            }else if (p4Trim != "locked" && p4Trim != "unlocked")
               evtArr[i].add("$pDate\t [Unlock w/Code] $p4Trim")            
        }
        evtArr[i].sort()
    }

    dispTable += "</tr><tr>"
    for(i=0;i<qryDevice.size();i++){
        tempStr =""
        evtArr[i].each {
            tempStr+="$it \n"
        }                    
        dispTable += "<td class='mtd'>$tempStr</td>"
    }


        
    dispTable += "</tr></table></div>"

        
    return dispTable
}
      
String findAltName(unkStr){
    startPos = unkStr.indexOf(":")+1
    if (startPos == 0) startPos = unkStr.indexOf("#")+1
    endPos = unkStr.size()
    result = unkStr.substring(startPos, endPos).trim()
    aName = state.altNames.get(result)
    if(aName) return aName
    else return "Key Not Found: $result"
}

def altLockName(){
    dynamicPage (name: "altLockName", title: "", install: false, uninstall: false, nextPage:"mainPage") {
        section("Alternate Lock Names"){   
            qryDevice.each {
                input "lock${it.deviceId}", "string", title:"Alternate for <b>${it.displayName}</b>", defaultValue:"$it.displayName", width:4, submitOnChange:true
            }
        }
    }
}


def altName(){
    dynamicPage (name: "altName", title: "", install: false, uninstall: false, nextPage:"mainPage") {
        section("Alternate Names"){   
            dispTable = buildNames()
                paragraph dispTable
        }            
        section ("Alternate Names Details", hideable: false, hidden: false) { 
                input "slotNum", "string", title: "Slot Number:", submitOnChange:true
                input "slotName", "string", title: "Name to Display:", submitOnChange:true
                if(slotName && slotNum) input "saveName", "button", title:"Save"
                if(slotNum) input "deleteName", "button", title:"Delete"
            if(state.saveNm){
                state.saveNm = false
                state.altNames["$slotNum"]="$slotName"
                if(slotNum) app.updateSetting("slotNum",[type:"string",value:""])
                if(slotName) app.updateSetting("slotName",[type:"string",value:""])
                paragraph "<script>location.reload();</script>"
            }
            if(state.delNm) {
                state.delNm = false
                state.altNames.remove(slotNum)
                if(slotNum) app.updateSetting("slotNum",[type:"string",value:""])
                paragraph "<script>location.reload();</script>"
            }
                    
        }
    }
}

String buildNames(){
    if(state.altNames) return state.altNames
    else return "Empty List"
}
               
def parseState(stateStr){ //returns array of the elements in the string [0] - Timestamp, [1] - Event ID, [2] - Event Name, [3] - Event Description, [4] - Event Value. [5] - Unit
    Integer start = stateStr.indexOf('[')+1
    if(location.hub.firmwareVersionString <= "2.2.7.0") 
         start = stateStr.indexOf('(')+1

    end = stateStr.length() - 1
    stateStr = stateStr.substring(start, end)
    stateStr=stateStr.split(',')
    HashMap stateMap = [:]
    stateStr.each {
        tempList = it.split("=")
        stateMap.put(tempList[0].trim(),tempList[1].trim())
    }

    return stateMap
}

def appButtonHandler(btn) {
    switch(btn) {
        case "refreshTable":
            lockHistory()
            break
        case "saveName":
            state.saveNm = true
            break
        case "deleteName":
            state.delNm = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
      }
}
