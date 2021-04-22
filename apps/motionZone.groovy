/*
 * Motion Zone
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
 *    Date        Who           What
 *    ----        ---           ----
 *    2021-04-21  thebearmay	Original version 0.1.0
 */

static String version()	{  return '0.1.0'  }


definition (
	name: 			"Motion Zone", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Control devices based on motion events.",
	category: 		"Utility",
	importUrl:		"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/motionZone.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "controlPage"
    page name: "addControl"
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
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main") {
                input "motionDevice", "capability.motionSensor", title: "Motion Sensor(s):", multiple: true, required: true, submitOnChange: true
                input "debugEnabled", "bool", title: "Enbable Debug", default: false, submitOnchange: true
                if (motionDevice != null) href "controlPage", title: "Define Controls", required: false
                if (motionDevice != null) motionSubscribe()
		    }
	    } else {
		    section("") {
                input "appName", "string", title:"Name for this Instance", default:"Motion Zone", required: true, submitOnChange:true
                app.updateLabel(appName)
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def controlPage(){
    dynamicPage (name: "controlPage", title: "", install: false, uninstall: false) {
	  section("Define Controls"){
          if(state.timeControl == null) state.timeControl=[:] // [name:{start, end, delay, device, on/off}]
          state.message = " "
        section ("") {
            paragraph ""
        }                                                                                           
        section ("Time Based Control", hideable: true, hidden: false) {
            tcntrlStr = "<style type='text/css'>td, th{border-right: 1px solid black;border-bottom:1px solid black;}}</style><table><tr><th>ID</th><th>Trigger</th><th>Start Time</th><th>End Time</th><th>Device</th><th>Delay</th><th>Actions</th></tr>"
            if(state.timeControl){
                state.timeControl.each{
                    wrkStr = it.toString()
                    key = wrkStr.substring(0,wrkStr.indexOf("="))
                    node = state.timeControl[key]
                    tcntrlStr+="<tr><td>$key</td><td>${node.trigger}</td><td>${node.sTime}</td><td>${node.eTime}</td><td>${node.device}</td><td>${node.delay}</td><td>${node.action}</td></tr>"
                }
            }
            tcntrlStr +="</table>"
            paragraph tcntrlStr
            
            href "addControl", title: "Add Control", required: false
            
        }
        section {       
            paragraph "<span style='font-weight:bold'>Commands:</span>"
        }   
        section ("Command Details", hideable: true, hidden: true) {  
                paragraph  "<span>  </span>"
        }
        section (""){    
                href "mainPage", title: "Return", required: false
        }
      }
    }
}

def motionSubscribe(){
    unsubscribe()
    motionDevice.each {
        if(debugEnabled) log.debug "Subscribing to motion for $it"
        subscribe(it, "motion", "motionHandler")
    }
}

def motionHandler(evt){
    if(evt.value == "active") checkZoneActive(evt)
    else if (evt.value == "inactive") checkZoneInactive(evt)
    else log.error "Invalid motion event value ${evt.value}"
}

def checkZoneActive(evt){
    if(debugEnabled) log.debug "Motion active received for ${evt.getDevice()}"
    if(state.timeControl){
        state.timeControl.each{
            wrkStr = it.toString()
            key = wrkStr.substring(0,wrkStr.indexOf("="))
            node = state.timeControl[key]
            if(timeOfDayIsBetween(timeToday(node.sTime),timeToday(node.eTime),new Date()) && node.trigger == "active"){
                dev = node.device.substring(1,node.device.length())
                devList = dev.split(",")
                devList.each{
                    devId = it.substring(0,it.indexOf(":")).trim()
                    if(debugEnabled) log.debug "Active $devId"
                    switchDevice.each{
                        if (it.getId() == devId) it.on()
                        else log.debug "Device not found"
                    }
                }
            }
        }
    }
           
    
}

def checkZoneInactive(evt){
    if(debugEnabled) log.debug "Motion inactive received for ${evt.getDevice()}"
    if(state.timeControl){
        state.timeControl.each{
            wrkStr = it.toString()
            key = wrkStr.substring(0,wrkStr.indexOf("="))
            node = state.timeControl[key]
            if(timeOfDayIsBetween(timeToday(node.sTime),timeToday(node.eTime),new Date()) && node.trigger == "inactive"){
                dev = node.device.substring(1,node.device.length())
                devList = dev.split(",")
                devList.each{
                    devId = it.substring(0,it.indexOf(":")).trim()
                    if(debugEnabled) log.debug "Inactive $devId"
                    switchDevice.each{
                        if (it.getId() == devId) it.off()
                        else log.debug "Device not found"
                    }
                }
            }
        }
    }
     
}

def addControl() {
    dynamicPage (name: "addControl", title: "", install: false, uninstall: false) {
	  section("Add Control"){
          //Not using required so that we can clear the values after each successful update
          if(state.message == " "  || state.message == "Control Saved"){
              app.clearSetting("cname")
              app.clearSetting("trigger")
              app.clearSetting("sTime")
              app.clearSetting("eTime")
              app.clearSetting("delay")
              app.clearSetting("switchDevice")
              app.clearSetting("action")
          }
          if(state.timeControl == null) state.timeControl=[:] // [name:{start, end, delay, device, on/off}]
          input "cname", "string", title:"Control Name"
          input "trigger", "enum", title:"Motion Triger Condition", options:["active", "inactive"]
          input "sTime", "time", title:"Start Time", width:6
          input "eTime", "time", title:"End Time", width:6
          input "delay", "number", title:"Minutes to delay the action", range:"0..60"
          input "switchDevice", "capability.switch", title:"Switches to Control", multiple:true, submitOnChange: true
          input "action", "enum", title:"Switch Action", options:["on", "off", "ignore"], default: "on"
          input "saveControl", "button", title:"Save"
      }
      section ("") {
          paragraph "${state.message}"
      }                                                                                           
    }
    
}

def saveControl() {
    if(debugEnabled) log.debug "$cname $sTime $eTime $delay $switchDevice $action"
    if(cname == null) 
       state.message = "Missing Name - cannot save"
    else if (trigger == null)
        state.message = "Missing Trigger - cannot save"
    else if(sTime == null)
        state.message = "Missing Start Time - cannot save"
    else if(eTime == null)
        state.message = "Missing End Time - cannot save"
    else if(action == null)
        state.message = "Missing Action - cannot save"
    else if(switchDevice == null) 
        state.message = "Missing Device - cannot save"
    else if(delay == null)
        state.message = "Missing Delay - cannot save"
    else {
        sTime = sTime.substring(sTime.indexOf("T")+1,sTime.indexOf("T")+6)
        eTime = eTime.substring(eTime.indexOf("T")+1,eTime.indexOf("T")+6)
        switchList = []
        switchDevice.each{
            switchList.add("${it.getId()}:${it}")
        }
        state.timeControl["$cname"] = [trigger:"$trigger",sTime:"$sTime", eTime:"$eTime", action:"$action", device:"${switchList}", delay:delay*60]
        state.message = "Control Saved"
    }
}


def appButtonHandler(btn) {
    switch(btn) {
        case "saveControl":
            saveControl()
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
   }
}
