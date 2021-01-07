 /*
 * Notify Tile Device
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
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-01-06  thebearmay	 Original version 0.1.0
 *    2021-01-07  thebearmay     Fix condition causing a loss notifications if they come in rapidly
 *    2021-01-07  thebearmay     Add alternative date format
 * 
 */
import java.text.SimpleDateFormat
static String version()	{  return '0.5.0'  }

metadata {
    definition (
		name: "Notify Tile Device", 
		namespace: "thebearmay", 
		description: "Simple driver to act as a destination for notifications, and provide an attribute to display the last 5 on a tile.",
		author: "Jean P. May, Jr.",
	    importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifyTile.groovy"
	) {
       		capability "Notification"
			
			attribute "last5", "STRING"
            attribute "notify1", "STRING"
            attribute "notify2", "STRING"
            attribute "notify3", "STRING"
            attribute "notify4", "STRING"
            attribute "notify5", "STRING"
			attribute "notificationText", "STRING"

			command "configure"//, [[name:"notification*", type:"STRING", description:"Notification Text"]]   
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
    input("dfEU", "bool", title: "Use Date Format dd/MM/yyyy", defaultValue:false)
}

def installed() {
	log.trace "installed()"
    configure()
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

def deviceNotification(notification){
    sendEvent(name:"notificationText", value: notification)
    updateLast5(notification)
}

def updateLast5(notification){
    dateNow = new Date()
    if (dfEU)
        sdf= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    else
        sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
    notification += " " + sdf.format(dateNow)
    last5 = notification+"<br />"+device.currentValue("notify1")+"<br />"+device.currentValue("notify2")+"<br />"+device.currentValue("notify3")+"<br />"+device.currentValue("notify4")
    sendEvent(name:"notify5", value:device.currentValue("notify4"))
    sendEvent(name:"notify4", value:device.currentValue("notify3"))
    sendEvent(name:"notify3", value:device.currentValue("notify2"))
    sendEvent(name:"notify2", value:device.currentValue("notify1"))
    sendEvent(name:"last5", value:last5)
    sendEvent(name:"notify1",value:notification)

}    

def configure() {
    sendEvent(name:"notificationText", value:' ')
    sendEvent(name:"last5", value:" ")
    sendEvent(name:"notify1", value:" ")
    sendEvent(name:"notify2", value:" ")
    sendEvent(name:"notify3", value:" ")
    sendEvent(name:"notify4", value:" ")
    sendEvent(name:"notify5", value:" ")
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
