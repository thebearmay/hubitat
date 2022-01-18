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
 *    18Jan2022	  thebearmay	Add a forced reconnect option 
 */


@SuppressWarnings('unused')
static String version() {return "0.1.8"}

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
    input(name: "startInput", type: "number", title: "Input at Power On (0 to use last value)", defaultValue: 0, range:"0..9", submitOnChange:true)
    input(name: "keepAlive", type: "bool", title: "Use device keep alive", defaultValue: false, sibmitOnChange: true)
	input(name: "forceConnect", type: "bool", title: "Re-establish Connect with each Command", defaultValue: false, sibmitOnChange: true)
}

@SuppressWarnings('unused')
def installed() {

}

def updated(){
    if(volume == null) device.updateSetting("volume",[value:50,type:"number"])
    if(startInput == null) device.updateSetting("startInput",[value:0,type:"number"])
    if(keepAlive) 
        runIn(120, "sendReset")
    else 
        unschedule()
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
	if(forceConnect) connectTelnet()
    sendHubCommand(new hubitat.device.HubAction("""$message\r""", hubitat.device.Protocol.TELNET))
}

def on(){
    connectTelnet()
    pauseExecution(100)
    sendMsg("-p.1")
    updateAttr("switch", "on")
    setVolume(volume)
    updateAttr("mute","off")
    updateAttr("networkStatus", "online")
    if(startInput > 0){     
        pauseExecution(100)
        setInput(startInput)
    } else {
        pauseExecution(100)
        setInput(device.currentValue("input", true))        
    }
    if(keepAlive){
        pauseExecution(100)
        runIn(120, "sendReset")
    }
}

def off(){
	if(forceConnect) connectTelnet()
    sendMsg("-p.0")
    pauseExecution(100)
    unschedule()
    sendMsg("-r.~")
    pauseExecution(100)
    disconnectTelnet()
    updateAttr("switch", "off")
    updateAttr("networkStatus", "offline")
}

def powerToggle(){
	if(forceConnect) connectTelnet()
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
	if(forceConnect) connectTelnet()
    sendMsg("-m.1")
    updateAttr("mute", "on")
}

def muteOff(){
	if(forceConnect) connectTelnet()
    sendMsg("-m.0")
    updateAttr("mute", "off")
}

def muteToggle(){
	if(forceConnect) connectTelnet()
    sendMsg("-m.t")
    if(device.currentValue("mute") == "on")
        updateAttr("mute", "off")
    else        
        updateAttr("mute", "on")
}

def volUp() {
	if(forceConnect) connectTelnet()
    sendMsg("-v.u")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger() + 1)
}

def volDown() {
	if(forceConnect) connectTelnet()
    sendMsg("-v.d")
    updateAttr("lastVolume", device.currentValue("lastVolume").toInteger() - 1)
}

def setVolume(level){
	if(forceConnect) connectTelnet()
    level = level.toInteger()
    if(level < 0 || level > 100) level = 50
    sendMsg("-v.$level")
    updateAttr("lastVolume",level)
}

def setInput(inputNum){
	if(forceConnect) connectTelnet()
    inputNum = inputNum.toInteger()
    if(inputNum < 1 || inputNum> 9) inputNum = 1

    iVals = ['Balanced','Analog 1','Analog 2','Coaxial','Optical 1','Optical 2','Optical 3','USB','Network']
    updateAttr("input", "${iVals[(Integer)inputNum-1]}")

    sendMsg("-i.$inputNum")
        
}

def sendReset(){
	if(forceConnect) connectTelnet()
    sendMsg("-r.3")
    runIn(120,"sendReset")
}

def parse(message) {
    updateAttr("lastParseMessage", message)
}

def telnetStatus(message){
    updateAttr("lastStatusMessage", message)
}
