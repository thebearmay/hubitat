/*
 * Motion Timing Comparison
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
 *    2021-04-16    thebearmay    Original version 0.1.0
 *    2021-05-04    thebearmay	  2.2.7.x changes
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.2.0'  }


definition (
	name:		"Motion Timing", 
	namespace:	"thebearmay", 
	author:		"Jean P. May, Jr.",
	description: 	"Capture the motion events from multiple devices and compare them for timing difference",
	category:	"Utility",
	importUrl:	"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/motionTiming.groovy",
	oauth:		false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "motionTiming"
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
	    	section("Main")
		    {
                input "qryDevice", "capability.motionSensor", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                input "motionDate", "string", title: "Pull motion data from this date forward (yyyy-MM-dd hh:mm):", required: true, submitOnChange: true
                if (qryDevice != null && motionDate != null) href "motionTiming", title: "Motion Information", required: false
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def motionTiming(){
    dynamicPage (name: "motionTiming", title: "", install: false, uninstall: false) {
	  section("Motion Stats"){
            dispTable = 
                "<style type='text/css'>div{overflow:auto;} .mtable {border:1px black solid;padding:0px;width:100%;}.mth {border:1px black solid; min-width:16em;} .mtd {border:1px black solid;vertical-align:top;}</style><div class='.mdiv'><table class='mtable'><tr>"
            evtList = {}
            evtArr = []
            for(i=0;i<qryDevice.size();i++){
                dName = qryDevice[i].label ?: qryDevice[i].name 
                dispTable+="<th class='mth'> $dName </th>"
                evtList=qryDevice[i].statesSince("motion",Date.parse("yyyy-MM-dd hh:mm", motionDate),[max:100])
                evtArr[i] = evtList
            }
            dispTable += "</tr><tr>"
            for(i=0;i<qryDevice.size();i++){
                tempStr =""
                evtArr[i].each {
                    stateParts = parseState(it.toString())
                    stateParts[1] = stateParts[1].replace("date=","")
                    stateParts[5] = stateParts[5].replace("value=","")
                    tempStr+="${stateParts[1]} ${stateParts[5]}\n"
                }                   
                dispTable += "<td class='mtd'>$tempStr</td>"
            }
            dispTable += "</tr></table></div>"
          
          
            section ("Motion Details", hideable: true, hidden: false) {  
                paragraph  "$dispTable"
            }
            section (""){    
                paragraph pStr2    
                href "mainPage", title: "Return", required: false
            }
      }
    }
}

def parseState(stateStr){ //returns array of the elements in the string [0] - Timestamp, [1] - Event ID, [2] - Event Name, [3] - Event Description, [4] - Event Value. [5] - Unit
    start = stateStr.indexOf('[')+1
    end = stateStr.length() - 1
    stateStr = stateStr.substring(start, end)
    return stateStr.split(',')
}

def appButtonHandler(btn) {
    switch(btn) {
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

def intialize() {

}
