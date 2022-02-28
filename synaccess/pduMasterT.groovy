/*
 * Synaccess PDU Master 
 *
 * API document: https://synaccess.com/support/webapi#table-of-contents
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
 *    ----         ---           ----
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.6"}

metadata {
    definition (
        name: "Synaccess PDU Master Telnet", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/synaccess/pduMasterT.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Telnet"
       
        attribute "numOutlets", "number"        
        attribute "lastStatusMessage", "STRING"
        attribute "lastParseMessage", "STRING"
        attribute "lastMessage", "STRING" 
        attribute "lastCommand", "STRING"
        attribute "outletString", "JSON_OBJECT"
        
        command "masterPowerOff"
        command "masterPowerOn"
        command "connectTelnet"
    }   
}

preferences {
    input("ipAddr", "text", title:"IP address for PDU:", required: true, submitOnChange:true)
    input(name: "portNum", type: "number", title: "Port Number:", required: true)
    input("userId", "text", title:"User Name:", required: true, submitOnChange:true)
    input("pwd", "text", title:"Password:", required: true, submitOnChange:true) 
    input("pollEnabled","bool", title:"Enable Outlet Polling?", submitOnChange:true)
    if (pollEnabled)
        input("pollInterval", "number", title:"Polling Interval:", submitOnChange:true)
    
    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){
    if(pollEnabled && pollInterval > 0)
        runIn(pollInterval, "outletPoll")
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    if(pollEnabled && pollInterval > 0)
        runIn(pollInterval, "outletPoll")
    else unschedule("outletPoll")
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"
    if(ipAddr){
        reqPduData()
        runIn(5,"createChildDev")
    }
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def clearMsg(){
    updateAttr("lastMessage","-")
    updateAttr("lastStatusMessage","-")
    updateAttr("lastParseMessage","-")    
}

def connectTelnet(){
    try{
        telnetConnect([termChars:[13]], ipAddr, (int)portNum, null, null)
        pauseExecution(100)
        sendMsg("\$A1,$userId,$pwd")
    } catch (ex) {
        updateAttr("lastMessage", ex)
    }
}

def disconnectTelnet() {
    sendMsg("logout")
    pauseExecution(100)
    telnetClose()
}

def sendMsg(message) {
    updateAttr("lastCommand",message)
    sendHubCommand(new hubitat.device.HubAction("""$message\r""", hubitat.device.Protocol.TELNET))
}

def parse(message) {
    if(message.indexOf("Goodbye!") != -1 || message == "Synaccess Telnet V6.2"){
        clearMsg()
        updateAttr("lastCommand","autoLogoff")
    } else if(device.currentValue("lastCommand") == "pshow" && message.indexOf('|') > 0) {
        outletParse = message.replace(' |', ',').split(',')
        if(device.currentValue("numOutlets") == null) {
            numOutlets = outletParse.size()/3
            updateAttr("numOutlets", numOutlets)
        } else numOutlets = device.currentValue("numOutlets")
        jsonStr = "["
        if(debugEnabled) "parse numOutlets = $numOutlets outletParse = $outletParse.size()"
        for (i = 0; i < numOutlets; i++){
            if(i>0) jsonStr+=","
            jsonStr+="{\"oNum\":\"${outletParse[i*3].toString().trim()}\",\"oName\":\"${outletParse[i*3+1].toString().trim()}\",\"oStatus\":\"${outletParse[i*3+2].toString().trim()}\"}"
        }
        jsonStr+="]"
        state.outletString = jsonStr           
    } else if(device.currentValue("lastCommand") == "ver") {
        updateDataValue("pduVersion", "$message")
    } else if(device.currentValue("lastCommand") == "mac") {
        updateDataValue("mac", "$message")
    }
    updateAttr("lastParseMessage", message)
}

def telnetStatus(message){
    updateAttr("lastStatusMessage", message)
}

void reqPduData() {
    connectTelnet()
    pauseExecution(100)
    sendMsg("pshow")
    runIn(30,"pduExtended")
}

void pduExtended(){
    connectTelnet()
    pauseExecution(100)    
    sendMsg("ver")
    pauseExecution(100)
    sendMsg("mac")               
}

void createChildDev(){
    int i=0
    def jSlurp = new JsonSlurper()
    outlets = jSlurp.parseText((String) state.outletString)

    outlets.each {
        i++
        addChildDevice("thebearmay", "Synaccess Outlet", "${device.deviceNetworkId}:${i}", [name:"$device.displayName-$it.oNum",label:"$it.oName",isComponent:true])
        chd = getChildDevice("${device.deviceNetworkId}:${i}")
        chd.updateAttr("switch",it.oStatus.toLowerCase())
    }
}

def reqOutlets(){   
    def jSlurp = new JsonSlurper()
    return jSlurp.parseText((String) state.outletString)
}

def outletPoll(){
    connectTelnet()
    pauseExecution(100)
    sendMsg("pshow")
    pauseExecution(100)
    outlets = reqOutlets()
    int i = 0
    outlets.each{
        i++
        chd = getChildDevice("${device.deviceNetworkId}:${i}")
        chd.updateAttr("switch",it.oStatus.toLowerCase())
        chd.updateAttr("lastRefresh",new Date())
    }
    if(pollEnabled && pollInterval > 0)
        runIn(pollInterval, "outletPoll")
    
}

void powerMode(outletID, pMode) {
    if(debugEnabled) log.debug "powerMode $outletID $pMode"
    oNum = outletID.substring(outletID.indexOf("-")+1)
    if(pMode == "on") v=1
    else v=0
    connectTelnet()
    pauseExecution(100)    
    sendMsg("pset $oNum $v")    
}

void masterPowerOff(){
    if(debugEnabled) log.debug "Master Power Off"
    connectTelnet()
    pauseExecution(100)    
    sendMsg("ps 0")
    children = getChildDevices()
    children.each {
        it.updateAttr("switch","off")
        it.updateAttr("lastRefresh", new Date())
        pauseExecution(50)
    }
}

void masterPowerOn(){
    if(debugEnabled) log.debug "Master Power On"
    connectTelnet()
    pauseExecution(100)    
    sendMsg("ps 1")
    children = getChildDevices()
    children.each {
        it.updateAttr("switch","on")
        it.updateAttr("lastRefresh", new Date())
        pauseExecution(50)
    }
}

void rebootOutlet(outletID) {
    if(debugEnabled) log.debug "reboot for $outletID requested"
    oNum = outletID.substring(outletID.indexOf("-")+1)
    connectTelnet()
    pauseExecution(100)    
    sendMsg("rb $oNum")    
}

void getState(netID, outletID) {
    chd = getChildDevice(netID)
    connectTelnet()
    pauseExecution(100)    
    sendMsg("pshow")
    pauseExecution(100)
    outlets = reqOutlets()
    outlets.each{
        if("$device.displayName-$it.oNum" == outletID) {
            chd.updateAttr("switch",it.oStatus.toLowerCase())
        }
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
