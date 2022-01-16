 /*
 *  Telnet Test Driver
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


@SuppressWarnings('unused')
static String version() {return "0.0.0"}

metadata {
    definition (
        name: "Telnet Test", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/telnet.groovy"
    ) {
        
        capability "Telnet"

        command "connectTelnet"
        command "disconnectTelnet"

    }   
}

preferences {

    input(name: "ipAddr", type: "string", title:"IP Address", required: true)
    input(name: "portNum", type: "number", title: "Port Number", required: true)
}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def connectTelnet(){
    try{
        telnetConnect([termChars:[13]], ipAddr, (int)portNum, null, null)
    } catch (ex) {
        updateAttr("error", ex)
    }
}

def disconnectTelnet() {
    telnetClose()
}

def sendMsg(message) {
    sendHubCommand(new hubitat.device.HubAction("""$message\r""", hubitat.device.Protocol.TELNET))
}

def parse(message) {
    updateAttr("parsedMessage", message)
}

def telnetStatus(message){
        updateAttr("statusMessage", message)
}
