 /*
 * Hegel Amp Telnet Driver
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
        name: "Hegel Amp", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hegelAmp.groovy"
    ) {
        
        capability "Telnet"
        capability "Switch"
        
        attribute "mute", "ENUM", ["on", "off"]
        attribute "input", "STRING"
        attribute "lastMessage", "STRING"
        attribute "lastVolume", "NUMBER"

        command "connectTelnet"
        command "disconnectTelnet"
        command "powerToggle"
        command "volUp"
        command "volDown"
        command "setVolume", [[name:"level*", type:"NUMBER", description:"Level to set volume to, % of Max", range:"0..100"]] 
        command "muteOn"
        command "muteOff"
        command "muteToggle"
        command "setInput", [[name:"inputNum*", type:"NUMBER", description:"Input Number", range:"1..9"]]


    }   
}

preferences {

    input(name: "ipAddr", type: "string", title:"IP Address", required: true)
    input(name: "portNum", type: "number", title: "Port Number", required: true)
    input(name: "volume", type: "number", title: "Starting Volume Level", defaultValue: 50, submitOnChange: true)
}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def connectTelnet(){
    try{
        telnetConnect([termChars:[10]], ipAddr, (int)portNum, null, null)
    } catch (ex) {
        updateAttr("lastMessage", ex)
    }
}

def disconnectTelnet() {
    telnetClose()
}

def sendMsg(message) {
    sendHubCommand(new hubitat.device.HubAction("""$message\r\n""", hubitat.device.Protocol.TELNET))
}

def on(){
    connectTelnet()
    sendMsg("-p.1")
    updateAttr("switch", "on")
    setVolume(volume)
}

def off(){
    sendMsg("-p.0")
    disconnectTelnet()
    updateAttr("switch", "off")
}

def powerToggle(){
    sendMsg("-p.t")
    if(device.currentValue("switch") == "on")
        updateAttr("switch", "off")
    else {
        updateAttr("switch", "off")
        setVolume(volume)
    }
}

def muteOn(){
    sendMsg("-m.1")
    updateAttr("mute", "on")
}

def muteOff(){
    sendMsg("-m.0")
    updateAttr("mute", "off")
}

def muteToggle(){
    sendMsg("-m.t")
    if(device.currentValue("mute") == "on")
        updateAttr("mute", "off")
    else        
        updateAttr("mute", "off")
}

def volUp() {
    sendMsg("-v.u")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger + 1)
}

def volDown() {
    sendMsg("-v.d")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger - 1)
}

def setVolume(level=50){
    if(level == null) level = 50
    sendMsg("-v.${level.toInteger()}")
    updateAttr("lastVolume",level)
}

def setInput(inputNum=1){
    if(inputNum.toInteger() < 1 || inputNum.toInteger() > 9) inputNum = 1
    sendMsg("i.${inputNum.toInteger()}")
    iVals = ["1": "Balanced","2": "Analog 1","3": "Analog 2","4": "Coaxial","5": "Optical 1","6": "Optical 2","7": "Optical 3","8": "USB","9": "Network"]
    updateAttr("input", iVals["$input"])
}

def parse(message) {
    updateAttr("lastMessage", message)
}

def telnetStatus(message){
    updateAttr("lastMessage", message)
}
