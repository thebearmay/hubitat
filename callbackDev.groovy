/*
 * Callback Device
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
 *    Date         Who           What
 *    ----         ---           --------------------------------------
 *	  
 *
 */
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.*

static String version()	{  return '0.0.1'}

metadata {
    definition (
		name: "Callback Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/callbackDev.groovy"
	) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"

       
        attribute "msg", "string"
        attribute "dest", "string"
        attribute "cbMsg", "string"

        
        command "subscribe",[[type:"string",description:"IP of Publisher Hub"]]
        command "unsubscribe"

    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
}

void installed() {
    log.trace "Callback Device v${version()} installed()"
    configure()   
}

void configure() {

}

void initialize() {

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateParAttr(aKey, aValue){
    parent.sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def subscribe(ip){
    hostHex = getHostAddress(ip)
    device.setDeviceNetworkId(hostHex)
    device.updateDataValue('subscriptionDNI',hostHex)
}

def unsubscribe(){
    newDNI=UUID.randomUUID().toString()
    device.removeDataValue('subscriptionDNI')
    device.setDeviceNetworkId(newDNI)
}

def getHostAddress(ip=''){
    if(!ip)
        ipTokens = location.hub.localIP.split('\\.')
    else
        ipTokens = ip.split('\\.')  
    hexStr=''
//    log.debug "$ipTokens"
    ipTokens.each{
        wStr=Integer.toString(it.toInteger(),16).toUpperCase()
        if(wStr.size() == 1)
            hexStr+="0$wStr"
        else
            hexStr+="$wStr"        
    }
//    log.debug "$hexStr"
    return hexStr
}

void parse(String msgIn) {
  // Called everytime a POST message is received from Port 39501 if DNI matches Hex of sender
    try {
        Map data = parseLanMessage(msgIn);
        //log.debug "${data.json}"
        updateAttr("msg", data.json.msg)
        if(data.json.dest)
        	updateAttr("dest",data.json.dest)
        else 
            device.deleteCurrentState('dest')
        if(getParent()) {
            //log.debug "Parent Available"
            if (getParent() instanceof com.hubitat.app.InstalledAppWrapper) {
    			//log.info "The parent is an App: ${device.parent.getName()}"
			} else if (getParent() instanceof com.hubitat.app.ParentDeviceWrapper) {
    			updateParAttr("cbMsg",data.json.msg)
        	} else log.debug "Unknown parent type"
        }
    } catch (e) {
        log.error "$e"
   
    }
}
