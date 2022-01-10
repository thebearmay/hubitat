/*
* Notify nodeRed
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

*/
import java.text.SimpleDateFormat
import groovy.transform.Field
import java.net.URLEncoder
static String version()	{  return '0.0.1'  }

metadata {
	definition (
			name: "Notify Node Red", 
			namespace: "thebearmay", 
			description: "Simrple driver to act as a destination for notifications to be forwarded to nodeRed",
			author: "Jean P. May, Jr.",
			importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifyNodeRed.groovy",
            singleThreaded: true
		) {
			capability "Notification"
            capability "Configuration"

			}   
		}

	preferences {
		input("debugEnabled", "bool", title: "Enable debug logging?")
            input "nrServer","text", title:"<b>NodeRed Server path</b> (i.e. http://192.168.x.x:1880)", submitOnChange: true
            input "nrPath", "text", title:"<b>NodeRed Endpoint path</b> (i.e. /notifications)", submitOnChange:true
            input "nrEnabled", "bool", title:"Enable Send to NodeRed", submitOnChange:true, defaultValue:false

	}

	void installed() {
		if (debugEnabled) log.trace "installed()"
		configure()
	}

	void updated(){
        if (debugEnabled) log.trace "updated()"
		if(debugEnabled) runIn(1800,logsOff)

    }

	void configure() {
		if (debugEnabled) log.trace "configure()"

	}

void deviceNotification(notification){
	if (debugEnabled) log.debug "deviceNotification entered: ${notification}" 
    if(nrEnabled){
        //notification = URLEncoder.encode(notification, "UTF-8")
        sendNR(notification)
    }

}    

void sendNR(notifyStr){

	Map requestParams =
	[
       uri: "$nrServer",
       path: "$nrPath",// /$notifyStr",
       contentType: "application/json", 
       body: [notification: notifyStr]
	]

    if(debugEnabled) log.debug "$requestParams"

    asynchttpPost("getResp", requestParams, [src:"nrNotify"])     
}

void getResp(resp, data) {
    try {
        if(debugEnabled) log.debug "$resp.properties - ${data['src']} - ${resp.getStatus()}"
        if(resp.getStatus() == 200 || resp.getStatus() == 207){
            if(resp.data) 
                returnString = resp.data
            else returnString = "status:${resp.getStatus()}"
        } else 
            log.error "Bad status returned ${resp.getStatus()}"
    } catch (Exception ex) {
        log.error ex.message
    }


}

	void logsOff(){
		device.updateSetting("debugEnable",[value:"false",type:"bool"])
	}

	void push() {
        	configure()
	}
