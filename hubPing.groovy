/*
 * Hubitat Ping
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
 *    2021-03-13  thebearmay	 Original version 0.1.0
 *                               add responseReady, additional minor fixes v0.5.0
 *                               add PresenceSensor capability v0.6.0
 *    2021-03-14  thebearmay     Add repeat value, tighten to release v1.0.0
 *    2021-03-15  thebearmay     Add lastIpAddress, leave presence at last value when starting new ping
 *    2021-05-04  thebearmay     Use 2.2.7.x ping instead of http call if available
 *    2021-05-06  thebearmay     2.2.7.121 returns all zeroes on ping not found 
 *    2021-05-10  thebearmay	   Fix the scheduler option under the new method
 *    2021-05-14  thebearmay     add option to use old method if desired
 *    2021-06-21  thebearmay	   add a dummy refresh method to deal with phantom command
 *    2021-06-22  thebearmay     code for null return
 *                               add regEx pattern to check address format validity
 *    2021-06-23  thebearmay     HTTP endpoint method returns status 408 when pinging 8.8.8.8 and 8.8.4.4, place message in return attribute instead
 *                                of suppressing
 *    2021-08-25  thebearmay     Add restart of scheduled ping on reboot
 *    2021-08-26  thebearmay	 data.ipAddress was truncate to data.ip 
 *                               IP not passing when using old method and repeating ping
 *    2021-08-28  thebearmay	 Attributes can't be bool
 *    2021-10-11  thebearmay	 Restrict to one instance running at a time
 *    2021-11-05  thebearmay     Add Text Logging option
 *    2022-06-07  thebearmay     Add preference to reduce number of attributes returned.
 */

static String version()	{  return '2.1.11'  }

metadata {
    definition (
		name: "Hubitat Ping", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubPing.groovy",
	    	singleThreaded: true
	) {
        capability "Actuator"
        capability "Configuration"
        capability "PresenceSensor"
        capability "Initialize"
       
        attribute "pingReturn", "string"
        attribute "percentLoss", "number"
        attribute "max", "number"
        attribute "avg", "number"
        attribute "min", "number"
        attribute "mdev", "number"
        attribute "pingStats", "string"
	    attribute "responseReady", "string"
        attribute "lastIpAddress", "string"
        
        
        command "sendPing", [[name:"ipAddress*", type:"STRING", description:"IP Address (IPv4) for the hub to ping"]]   
            
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
    input("numPings", "number", title: "Number of pings to issue", defaultValue:3, required:true, submitOnChange:true, range: "1..5", width:4)
    input("pingPeriod", "number", title: "Ping Repeat in Seconds\n Zero to disable", defaultValue: 0, required:true, submitOnChange: true, width:4)
    input("useOldMethod", "bool", title: "Use HTTP endpoint to issue request", width:4)
    input("textLoggingEnabled", "bool", title: "Enable Text Logging", defaultValue:false, width:4)
    input("presenceOnly", "bool", title: "Only report presence attribute", , width:4)
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
}

def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    updateAttr("percentLoss"," ")
    updateAttr("pingStats"," ") 
    updateAttr("min"," ")
    updateAttr("avg"," ")
    updateAttr("max"," ")
    updateAttr("mdev"," ")
    updateAttr("pingReturn"," ")
    updateAttr("responseReady","false")
    if (device.currentValue("presence") == null) updateAttr("presence","not present")

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){
  if(pingPeriod > 0 && numPings > 0 && device.currentValue("lastIpAddress") != null){
    unschedule()
    sendPing(device.currentValue("lastIpAddress"))
  }
}

def refresh() {
	unschedule(refresh)
}

def sendPing(ipAddress){
    if(ipAddress == null) ipAddress = data.ipAddress
    if(numPings == null) numPings = 3
    configure()
    updateAttr("lastIpAddress", ipAddress)
    if(textLoggingEnabled) log.debug "Ping initiated for $ipAddress"
    if(!validIP (ipAddress)) {
        if(textLoggingEnabled) log.debug "IP address $ipAddress failed pattern check - ping request terminated"
        updateAttr("pingReturn", "IP address format invalid")
        updateAttr("presence","not present")
        updateAttr("responseReady","true")
    } else {
        if (location.hub.firmwareVersionString > "2.2.6.140" && !useOldMethod){
            if(textLoggingEnabled) log.debug "Hub internal ping method selected"
            updateAttr("responseReady",false)
            if(!presenceOnly)
                updateAttr("pingReturn","Pinging $ipAddress") 
            hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(ipAddress, numPings.toInteger())
            int pTran = pingData.packetsTransmitted.toInteger()
            if (pTran == 0){ // 2.2.7.121 bug returns all zeroes on not found
                pingData.packetsTransmitted = numPings
                pingData.packetLoss = 100
            }
            if(!presenceOnly)                
                updateAttr("percentLoss", pingData.packetLoss,"%")
            String pingStats = "Transmitted: ${pingData.packetsTransmitted}, Received: ${pingData.packetsReceived}, %Lost: ${pingData.packetLoss}"
            if(textLoggingEnabled) log.debug "Ping Stats: $pingStats"
            if(!presenceOnly){
                updateAttr("pingStats", pingStats) 
                updateAttr("min",pingData.rttMin,"ms")
                updateAttr("avg",pingData.rttAvg,"ms")
                updateAttr("max",pingData.rttMax,"ms")
            //mdev not returned, calculate using min, max and avg (accuracy decreases proportionally to the number of pings)
                Double mdev = ((pingData.rttAvg - pingData.rttMin) + (pingData.rttMax - pingData.rttAvg))/2
                updateAttr("mdev",mdev.round(3))
                updateAttr("pingReturn",pingData)
            }
            if (pingData.packetLoss < 100) {
                if(textLoggingEnabled) log.debug "Presence set to 'present' for $ipAddress"
                updateAttr("presence","present")
            } else {
                if(textLoggingEnabled) log.debug "Presence set to 'not present' for $ipAddress"
                updateAttr("presence","not present")
            }
            updateAttr("responseReady","true")
	        if(pingPeriod > 0) runIn(pingPeriod, "sendPing", [data:ipAddress])
            if(textLoggingEnabled && pingPeriod > 0) log.debug "Next ping for $ipAddress scheduled in $pingPeriod seconds"
        } else {
            if(textLoggingEnabled) log.debug "Hub Endpoint ping method selected"
            if(security) {
                httpPost(
                    [
                        uri: "http://127.0.0.1:8080",
                        path: "/login",
                        query: [ loginRedirect: "/" ],
                        body: [
                            username: username,
                            password: password,
                            submit: "Login"
                        ]
                    ]
                ) { resp -> cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0) }
            }    
            params = [
                uri: "http://${location.hub.localIP}:8080",
                path:"/hub/networkTest/ping/"+ipAddress,
                headers: [ "Cookie": cookie ]
            ]
            if(debugEnable)log.debug params
            updateAttr("responseReady",false)
            updateAttr("pingReturn","Pinging $ipAddress")  
	
            asynchttpGet("sendPingHandler", params)
        }
    }
    
}

def sendPingHandler(resp, data) {
    def errFlag = 0
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    strWork = resp.data.toString()
    		if(debugEnable) log.debug strWork
	        updateAttr("pingReturn",strWork)
        } else updateAttr("pingReturn", "Ping Error - Status ${resp.getStatus()}")
    } catch(Exception ex) { 
        errFlag = 1
        respStatus = resp.getStatus()
        sendEvent(name:"pingReturn", value: "httpResp = $respStatus but returned invalid data")
        updateAttr("presence","not present")
        log.warn "sendPing httpResp = $respStatus but returned invalid data"
        
    } 
    if (!errFlag) extractValues(strWork)
    ipAddress = device.currentValue("lastIpAddress")
    if(pingPeriod > 0) runIn(pingPeriod, "sendPing", [data:ipAddress])
    if(textLoggingEnabled && pingPeriod > 0) log.debug "Next ping scheduled for $ipAddress in $pingPeriod seconds"

}

def extractValues(strWork) {
    if(strWork == null)
        startInx = -1
    else
        startInx = strWork.indexOf("%")
    if(debubEnable)log.debug startInx
    if (startInx == -1){
        if(textLoggingEnabled) log.debug "Invalid response received from endpoint"
        if(!presenceOnly){
            updateAttr("percentLoss",100,"%")
            updateAttr("pingStats"," ") 
            updateAttr("min"," ")
            updateAttr("avg"," ")
            updateAttr("max"," ")
            updateAttr("mdev"," ")
        }
        percentLoss = 100
    } else {
        startInx -=3
        strWork=strWork.substring(startInx)
        if(strWork.substring(0,1)==","){
            percentLoss = strWork.substring(1,3).toInteger()
        } else
            percentLoss = strWork.substring(0,3).toInteger()
        if(!presenceOnly)
            updateAttr("percentLoss",percentLoss,"%")
        
        startInx = strWork.indexOf("=")
        pingStats= strWork.substring(startInx+2,strWork.length()-4).tokenize("/")
        if(textLoggingEnabled) log.debug "Ping Stats: $pingStats"
        if(!presenceOnly){
            updateAttr("pingStats",pingStats) 
            updateAttr("min",pingStats[0]," ms")
            updateAttr("avg",pingStats[1]," ms")
            updateAttr("max",pingStats[2]," ms")
            updateAttr("mdev",pingStats[3], " ms")
        }
    }
    if (percentLoss < 100 ) {
        updateAttr("presence","present")
        if(textLoggingEnabled) log.debug "Presence set to 'present' for $ipAddress"
    } else {
        updateAttr("presence","not present")
        if(textLoggingEnabled) log.debug "Presence set to 'not present' for $ipAddress"
    }
    updateAttr("responseReady", "true")
}
       
def validIP(ipAddress){
    regxPattern =/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    boolean match = ipAddress ==~ regxPattern
    return match
}

def updated(){
	if(debugEnable) log.trace "updated()"
    if(!(pingPeriod > 0)) unschedule()
	if(debugEnable) runIn(1800,logsOff)
    updateAttr("percentLoss"," ")
    updateAttr("pingStats"," ") 
    updateAttr("min"," ")
    updateAttr("avg"," ")
    updateAttr("max"," ")
    updateAttr("mdev"," ")
    updateAttr("pingReturn"," ")
    updateAttr("responseReady","false")
    updateAttr("pingReturn"," ")
    if (device.currentValue("presence") == null) updateAttr("presence","not present")       
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
