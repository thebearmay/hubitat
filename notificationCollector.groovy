/*
* Notification Collector Device
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
*    Date        Who            What
*    ----        ---            ----
*/

import groovy.transform.Field
static String version()	{  return '0.0.1'  }

metadata {
	definition (
			name: "Notification Collector", 
			namespace: "thebearmay", 
			description: "Simple driver to act as a destination for notifications for the Notification Forwarder to pickup",
			author: "Jean P. May, Jr.",
			importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notificationCollector.groovy",
            singleThreaded: true
		) {
			capability "Notification"

			attribute "lastNotification", "STRING"

			}   
}

preferences {
}

void installed() {
	if (debugEnable) log.trace "installed()"
}

void updated(){
	if (debugEnable) log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void configure() {
}

void deviceNotification(notification){
	sendEvent(name:"lastNotification", value: notification, isStateChange:true)
}    

void logsOff(){
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
