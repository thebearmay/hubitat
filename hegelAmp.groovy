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
static String version() {return "0.1.5"}

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
        attribute "lastStatusMessage", "STRING"
        attribute "lastParseMessage", "STRING"
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
    input(name: "volume", type: "number", title: "Starting Volume Level", defaultValue: 50, range:"0..100", submitOnChange: true)
}

@SuppressWarnings('unused')
def installed() {

}

def updated(){
    if(volume == null) device.updateSetting("volume",[value:50,type:"number"])
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def connectTelnet(){
    try{
        telnetConnect([termChars:[13]], ipAddr, (int)portNum, null, null)
    } catch (ex) {
        updateAttr("lastMessage", ex)
    }
}

def disconnectTelnet() {
    telnetClose()
}

def sendMsg(message) {
    sendHubCommand(new hubitat.device.HubAction("""$message\r""", hubitat.device.Protocol.TELNET))
}

def on(){
    connectTelnet()
    sendMsg("-p.1")
    updateAttr("switch", "on")
    setVolume(volume)
    updateAttr("mute","off")
    updateAttr("networkStatus", "online")
}

def off(){
    sendMsg("-p.0")
    pauseExecution(100)
    disconnectTelnet()
    updateAttr("switch", "off")
    updateAttr("networkStatus", "offline")    
}

def powerToggle(){
    sendMsg("-p.t")
    if(device.currentValue("switch") == "on")
        updateAttr("switch", "off")
    else {
        updateAttr("switch", "on")
        pauseExecution(100)
        setVolume(volume)
        updateAttr("mute","off")
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
        updateAttr("mute", "on")
}

def volUp() {
    sendMsg("-v.u")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger() + 1)
}

def volDown() {
    sendMsg("-v.d")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger() - 1)
}

def setVolume(level){
    level = level.toInteger()
    if(level < 0 || level > 100) level = 50
    sendMsg("-v.$level")
    updateAttr("lastVolume",level)
}

def setInput(inputNum){
    inputNum = inputNum.toInteger()
    if(inputNum < 1 || inputNum> 9) inputNum = 1

    iVals = ['Balanced','Analog 1','Analog 2','Coaxial','Optical 1','Optical 2','Optical 3','USB','Network']
    updateAttr("input", "${iVals[(Integer)inputNum-1]}")

    sendMsg("i.$inputNum")
        
}

def parse(message) {
    updateAttr("lastParseMessage", message)
}

def telnetStatus(message){
    updateAttr("lastStatusMessage", message)
}
