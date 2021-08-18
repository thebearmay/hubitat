/* Room Temperatures
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
 *    2021-08-17  thebearmay    Original Version
 */

static String version()	{  return '1.0.0'  }


definition (
	name: 			"Room Temperatures", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Displays sorted list of temperature sensors and allows output of the maximum and minimum sensor names to hub variables.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/roomTemperatures.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "currentTemps"
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
                input "qryDevice", "capability.temperatureMeasurement", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null){
                    href "currentTemps", title: "Current Room Tempertaures", required: false
                    input "warmDev", "capability.variable", title: "Hub Variable Connector for Warmest", multiple: false, submitOnChange:true
                    input "coldDev", "capability.variable", title: "Hub Variable Connector for Coldest", multiple: false, submitOnChange:true
                    input "pollInterval", "number", title: "Polling Interval in Seconds (Zero to disable)", submitOnChange: true, defaultValue:0
                    if(pollInterval>0){
                      unschedule()
                      runIn(pollInterval, "currentTemps")
                    }
                }
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def currentTemps(){
    dynamicPage (name: "currentTemps", title: "", install: false, uninstall: false) {
	  section("Rooms by Temperature"){
          LinkedHashMap devTable = [:]
          qryDevice.each{
              devTable.put(it.displayName, it.currentValue("temperature"))
          }
          wrkStr = "<table>"
          tempSorted = devTable.sort { -it.value }
          tempSorted.each{
              wrkStr+="<tr><td>${it.key}</td><td>${it.value}</td></tr>"
          }
          wrkStr+="</table>"
          maxValue = devTable.max{it.value}
          minValue = devTable.min{it.value}

          paragraph "Warmest $maxValue<br>Coolest $minValue<br><br>$wrkStr"
          if(warmDev) warmDev.setVariable(maxValue.toString().substring(0,maxValue.toString().indexOf('=')))
          if(coldDev) coldDev.setVariable(minValue.toString().substring(0,minValue.toString().indexOf('=')))
          if(pollInterval>0){
              unschedule()
              runIn(pollInterval, "currentTemps")
          }
       }
    }
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
