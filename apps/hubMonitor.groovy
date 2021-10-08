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
 */

static String version()	{  return '0.1.0'  }


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
    page name: "hubAttrSelect"
    page name: "attrRepl"
    page name: "hubInfoAgg"
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
	if(debugEnable) runIn(1800,logsOff)
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
                input "qryDevice", "capability.initialize", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null){
                    hubDevCheck = true
                    qryDevice.each{
                        if(it.typeName != 'Hub Information') hubDevCheck = false
                    }
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
          input "notifyDevice", "capability.notification", title: "Notification Devices:", multiple:true
          int numHub=0
          qryDevice.each{
              paragraph "<b><u>${it.currentValue('locationName')}</u></b>"
              input "maxTemp$numHub", "number", title:"Max Temperature (0..200)",range:"0..200", required: true, defaultValue:200
              input "maxDb$numHub", "number", title:"Max DB Size (0..1000)", range:"0..1000", required:true, defaultValue:1000
              input "minMem$numHub", "number", title:"Min Free Memory (0..600000)", range:"0..600000", required:true,defaultValue:0
              input "trackIp$numHub", "bool", title:"Alert on IP Change", required:true, submitOnChange:true
              if(settings["trackIp$numHub"]) app.updateSetting("ip$numHub",[value: it.currentValue('localIP'), type:"string"])
              numHub++
          }
      }
    }
}

def refreshDevice(evt = null){
    if(notifyDevice){
        int numHub=0
        qryDevice.each{
            if(it.currentValue("temperature") >= settings["maxTemp$numHub"]){
                notifyStr = "HIA Temperature Warning on ${it.currentValue('locationName')} - ${it.currentValue("temperature")}°"
                sendNotification(notifyStr)
            }
            if(it.currentValue("dbSize") >= settings["maxDb$numHub"]){
                notifyStr = "HIA DB Size Warning on ${it.currentValue('locationName')} - ${it.currentValue("dbSize")}"
                sendNotification(notifyStr)
            }
            if(it.currentValue("freeMemory") <= settings["minMem$numHub"]){
                notifyStr = "HIA Free Memory Warning on ${it.currentValue('locationName')} - ${it.currentValue("freeMem")}"
                sendNotification(notifyStr)
            }
            if(it.currentValue("localIP") != settings["ip$numHub"] && settings["ip$numHub"]) {
                notifyStr = "Hub IP Address for ${it.currentValue('locationName')} has changed to ${it.currentValue("localIP")}"
                sendNotification(notifyStr)           
            }
            numHub++
         }
    
    }
 }

void sendNotification(notifyStr){
    notifyDevice.each { 
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
