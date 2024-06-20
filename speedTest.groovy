/*
 * Download Speed Test
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
 *    
 *
 */
import groovy.transform.Field

static String version()	{  return '0.0.1'  }

metadata {
    definition (
		name: "Download Speed Test", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/speedTest.groovy"
	) {
        capability "Actuator"
       
        attribute "result", "string"
        
                          
        command "checkSpeed", [[name:"url*", type:"STRING", description:"URL to download from", title:"URL"]]
        command "test"
    
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
}


def installed() {
    log.trace "Speed Test v${version()} installed()"
}

def configure() {
    if(debugEnabled) log.debug "configure()"

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def test(){
    log.debug "Test $debugEnabled"
}

def checkSpeed(url){
    if(debugEnabled)
        log.debug "checking speed for $url"
    updateAttr("result","Test Running")
    try {
        tStart = new Date().getTime()
        httpGet(url) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
                   if(delim.size()%1000 == 0) updateAttr("result","Still Running - Downloaded ${delim.size()}")
               } 
               if(debugEnabled) log.debug "${delim.size()} bytes read" 
               tEnd = new Date().getTime()
               result = "Start Time: $tStart<br>End Time: $tEnd<br>File Size: ${delim.size()} bytes<br>Download Speed: ${((delim.size()*8)/(tEnd-tStart))} mbps"
               updateAttr("result", result)
            }
            else {
                updateAttr("result", "Null Response")
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        updateAttr("result", exception.message)
    }
}
    
def updated(){
	log.trace "updated()"
	if(debugEnabled) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
