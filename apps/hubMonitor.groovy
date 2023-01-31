/* Hub Monitor
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
 *     Date              Who           Description
 *    ===========       ===========   =====================================================
 *    2021-10-08        thebearmay    New code
 *                                    0.1.1 fix null condition coming from hub information device
 *                                    0.1.2 add debugging logic
 *    2021-11-02        thebearmay    add monitoring for hubUpdateStatus
 *    2021-12-27        thebearmay    169.254.x.x reboot option
 *    2021-12-28        thebearmay    bug fix
 *    2022-03-23        thebearmay    remove the second auth requirement to reboot
 *    2022-04-12        thebearmay    typo in memory warning
 *    2022-06-10        thebearmay    pull in hubAlerts attribute
 *    2023-01-15        thebearmay    Allow v3 
 */

static String version()	{  return '1.0.12'  }


definition (
	name: 			"Hub Monitor", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides a utility to set alert levels based on a Hub Information Device",
	category: 		"Utility",
	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubMonitor.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "hubAlerts"
}

void installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
    if(debugEnable) {
        unschedule(logsOff)
        runIn(1800,logsOff)
    }
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
                input "qryDevice", "device.HubInformationDriverv3", title: "Hubs to Monitor:", multiple: true, required: true, submitOnChange: true
                input "debugEnable", "bool", title: "Enable Debugging:", submitOnChange: true
                if (qryDevice != null){
                    hubDevCheck = true
                    
                    if(hubDevCheck) {
                        unsubscribe()
		        	    qryDevice.each{
                            	subscribe(it, "uptime", "refreshDevice")
			            }
                        refreshDevice()
                        href "hubAlerts", title: "Configure Hub Alerts", required: false                  
                    } else
                        paragraph "Invalid Device Selected"
                }
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def hubAlerts(){
    dynamicPage (name: "hubAlerts", title: "Hub Alerts", install: false, uninstall: false) {
	  section(""){
          input "notifyDevice", "capability.notification", title: "Notification Devices:", multiple:true, required:true
          int numHub=0
          qryDevice.each{
              paragraph "<b><u>${it.currentValue('locationName')}</u></b>"
              input "maxTemp$numHub", "number", title:"Max Temperature (0..200)",range:"0..200", required: true, defaultValue:200, width:6
              input "maxDb$numHub", "number", title:"Max DB Size (0..1000)", range:"0..1000", required:true, defaultValue:1000, width:6
              input "minMem$numHub", "number", title:"Min Free Memory (0..600000)", range:"0..600000", required:true,defaultValue:0, width:6
              input "trackIp$numHub", "bool", title:"Alert on IP Change", required:true, submitOnChange:true
              if(settings["trackIp$numHub"]){
                  app.updateSetting("ip$numHub",[value: it.currentValue('localIP'), type:"string"])
                  input "rebootRequested$numHub", "bool", title:"Reboot hub if IP = 169.254.x.x", required:true, submitOnChange:true, width:6
                  if("rebootRequested$numHub") {
                      app.updateSetting("rbAttempts$numHub",[value: 0, type:"number"])
                      input "rebootInterval$numHub", "number", title:"Number of seconds to delay reboot (30..360)", range:"0..360",submitOnChange:true,required:true, defaultValue:60, width:6
                      input "rebootMax$numHub", "number", title: "Maximum Reboot Attempts", defaultValue: 1//, width:6
                  }
              }
              input "trackUpdStat$numHub", "bool", title:"Alert on Hub Update Status Change", required:true, submitOnChange:true, width:6
              if(settings["trackUpdStat$numHub"]) app.updateSetting("updStat$numHub",[value: it.currentValue('hubUpdateStatus'), type:"string"])
              input "hub2Alerts$numHub", "bool", title:"Alert on HE UI alerts", required:true, submitOnChange:true, width:6
              numHub++
          }
      }
    }
}

def refreshDevice(evt = null){
    if(notifyDevice){
        int numHub=0
        qryDevice.each{
            if(debugEnable){
                log.debug "${it.currentValue('locationName')} Temperature reported: ${it.currentValue("temperature")} Alert Level: ${settings["maxTemp$numHub"]}"
                log.debug "${it.currentValue('locationName')} Database Size reported: ${it.currentValue("dbSize")} Alert Level: ${settings["maxDb$numHub"]}"
                log.debug "${it.currentValue('locationName')} Free Memory reported: ${it.currentValue("freeMemory")} Alert Level: ${settings["minMem$numHub"]}"
                if(settings["ip$numHub"])log.debug "${it.currentValue('locationName')} IP reported: ${it.currentValue("localIP")} Previous Value: ${settings["ip$numHub"]}"
            }
            if(it.currentValue("temperature",true)?.toFloat() >= settings["maxTemp$numHub"]?.toFloat() && it.currentValue("temperature",true) != null ){
                notifyStr = "Hub Monitor Temperature Warning on ${it.currentValue('locationName')} - ${it.currentValue("temperature",true)}°"
                sendNotification(notifyStr)
            }
            if(it.currentValue("dbSize",true)?.toInteger() >= settings["maxDb$numHub"]?.toInteger() && it.currentValue("dbSize",true) != null ){
                notifyStr = "Hub Monitor DB Size Warning on ${it.currentValue('locationName')} - ${it.currentValue("dbSize",true)}"
                sendNotification(notifyStr)
            }
            if(it.currentValue("freeMemory",true)?.toFloat() <= settings["minMem$numHub"]?.toFloat() && it.currentValue("freeMemory",true) != null ){
                notifyStr = "Hub Monitor Free Memory Warning on ${it.currentValue('locationName')} - ${it.currentValue("freeMemory")}"
                sendNotification(notifyStr)
            }
            if((it.currentValue("localIP",true) != settings["ip$numHub"] && settings["ip$numHub"]) || (it.currentValue("localIP",true).startsWith("169.254") && settings["ip$numHub"])) {
                notifyStr = "Hub Monitor - Hub IP Address for ${it.currentValue('locationName')} has changed to ${it.currentValue("localIP",true)}"
                sendNotification(notifyStr)
		        app.updateSetting("ip$numHub",[value: it.currentValue('localIP',true), type:"string"])
                String ip = it.currentValue('localIP',true)
                if(!ip.startsWith("169.254")) app.updateSetting("rbAttempts$numHub",[value: 0, type:"number"])
                if(settings["rebootRequested$numHub"] && ip.startsWith("169.254")) {
                    if(settings["rbAttemptsMade$numHub"] < settings["rebootMax$numHub"]) {
                        app.updateSetting("rbAttempts$numHub",[value: settings["rbAttemptsMade$numHub"].toInteger + 1, type:"number"])
                        notifyStr = "Hub Monitor - ${it.currentValue('locationName')} will be rebooted in $rebootInterval seconds"
                        sendNotification(notifyStr)
                        runIn(settings["rebootInterval$numHub"],"rebootHub", [data:[ipAddress:ip],overwrite:false]) 
                    }
                } 
            }
            if(it.currentValue("hubUpdateStatus",true) != settings["updStat$numHub"] && settings["updStat$numHub"] && it.currentValue("hubUpdateStatus",true) != null) {
                notifyStr = "Hub Update Status for ${it.currentValue('locationName')} has changed to ${it.currentValue("hubUpdateStatus",true)}"
                sendNotification(notifyStr)
		        app.updateSetting("updStat$numHub",[value: it.currentValue('hubUpdateStatus',true), type:"string"])
            }
            if(settings["hub2Alerts$numHub"] && it.currentValue("hubAlerts",true)?.length() > 2) {
                notifyStr = "Hub Monitor - ${it.currentValue('locationName')} has raised alerts for: ${it.currentValue("hubAlerts",true)} "
                sendNotification(notifyStr)
            }
            numHub++
         }
    
    }
 }

void rebootHub(data){
    qryDevice.each{ 
        if(it.currentValue('localIP',true) == data.ipAddress) {
            try{
                httpGet("http://${data.ipAddress}:8080/api/hubitat.xml") { res ->
                    it.reboot()
                }        
            } catch(ignore) {}
        }
    }
}

void sendNotification(notifyStr){
    notifyDevice.each { 
      if(debugEnable) log.debug "Sending notification to $it, text: $notifyStr"
      it.deviceNotification(notifyStr)  
    }   
}

def appButtonHandler(btn) {
    switch(btn) {
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}
