/*
 * NodeRed Server Information
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
 *    Date         Who            What
 *    ----         ---            ----
 *    23Mar2023    thebearmay     Original version 0.0.1
 *                                v0.0.2 split out cpuLoad into 5, 10 & 15min
*/

import java.text.SimpleDateFormat
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "0.0.2"}

metadata {
    definition (
        name: "NodeRed Server Information Driver", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/nrServerInfo.groovy",
        description:"Accepts input from node-red-contrib-device-stat, node-red-contrib-cpu and stores it in a virtual device"
    ) {
        capability "Actuator"
        capability "Initialize"
        capability "Sensor"
        capability "TemperatureMeasurement"
       
        attribute "cpuUsage", "number"
        attribute "temperature", "number"
        attribute "uptime", "number"
        attribute "memUsed", "number"
        attribute "memFree", "number"
        attribute "swapUsed", "number"
        attribute "swapFree", "number"
        attribute "cpuAvgLoad", "number"
        attribute "cpu5mLoad", "number"
        attribute "cpu10mLoad", "number"
        attribute "cpu15mLoad", "number"
        attribute "tx", "number"
        attribute "rx", "number"
        attribute "formattedUptime", "string"
        attribute "lastRestart", "string"
        attribute "lastRestartFormatted", "string"
        attribute "html", "string"
        
        
        command "setUsage", ["number"]
        command "setTemp", ["number"]
        command "jsonUpdate", ["string"]

    }   
}

preferences {
//    input("debugEnable", "bool", title: "Enable debug logging?", width:4)
//    input("warnSuppress", "bool", title: "Suppress Warn Level Logging", width:4)
    input("upTimeSep", "string", title: "Separator for Formatted Uptime", defaultValue: ", ", width:4)
    input("upTimeDesc", "enum", title: "Uptime Descriptors", defaultValue:"d/h/m/s", options:["d/h/m/s"," days/ hrs/ min/ sec"," days/ hours/ minutes/ seconds"])
    input("rsrtSdfPref", "enum", title: "Date/Time Format for Restart Formatted", options:sdfList, defaultValue:"yyyy-MM-dd HH:mm:ss", width:4)
    input("attribEnable", "bool", title: "Enable HTML Attribute Creation?", defaultValue: false, required: false, submitOnChange: true, width:4)
    input("alternateHtml", "string", title: "Template file for HTML attribute", submitOnChange: true, defaultValue: "nrsInfoTemplate.res", width:4)

}
@SuppressWarnings('unused')
void installed() {
    log.info "NodeRed Information v${version()} installed()"
    xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/nrsInfoTemplate.res","nrsInfoTemplate.res")
    initialize()
    configure()
}

void initialize() {
    log.info "NodeRed Information v${version()} initialized"
}

void updated(){
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}



void setUsage(usageAmt){
    updateAttr("cpuUsage", usageAmt, "%")
    if(attribEnable) 
        createHtml()        
}

void setTemp(tempCel){
    tempCel = tempCel.toFloat()
    if (location.temperatureScale == "F")
        updateAttr("temperature",String.format("%.1f", celsiusToFahrenheit(tempCel)),"°F")
    else
        updateAttr("temperature",String.format("%.1f",tempCel),"°C")
    if(attribEnable) 
        createHtml()     
}

void jsonUpdate(String jsonString){
    def jSlurp = new JsonSlurper()
    Map nrData = (Map)jSlurp.parseText(jsonString)
    uptime = nrData.uptime.toInteger()
    updateAttr("uptime",uptime)
    formatUptime(uptime)
    updateAttr("memUsed", nrData.mem.used.toInteger()/1024,"MB")
    updateAttr("memFree", nrData.mem.free.toInteger()/1024,"MB")
    updateAttr("swapUsed", nrData.mem.swapused.toInteger()/1024,"MB")
    updateAttr("swapFree", nrData.mem.swapfree.toInteger()/1024,"MB")
    updateAttr("rx", nrData.nw.eth0.rx)
    updateAttr("tx", nrData.nw.eth0.tx)
    
    Float aLoad=(nrData.load[0]+nrData.load[1]+nrData.load[2])/3
    updateAttr("cpu5mLoad",nrData.load[0])
    updateAttr("cpu10mLoad",nrData.load[1])
    updateAttr("cpu15mLoad",nrData.load[2])
    updateAttr("cpuAvgLoad", aLoad.round(2))
    if(attribEnable) 
        createHtml()         
}

void formatUptime(uptime){
    Long ut = uptime
    Integer days = Math.floor(ut/(3600*24)).toInteger()
    Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
    Integer min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
    Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()
    if(upTimeSep == null){
        device.updateSetting("upTimeSep",[value:",", type:"string"])
        upTimeSep = ","
    }
    utD=upTimeDesc.split("/")
    dayD = (days==1) ? utD[0].replace("s",""):utD[0]
    hrD = (hrs==1) ? utD[1].replace("s",""):utD[1]
    minD = (min==1) ? utD[2].replace("s",""):utD[2]
    if(utD[3] == " seconds")
        secD = (sec==1) ? " second":utD[3]
    else 
        secD = utD[3]
        
    String attrval = "${days.toString()}${dayD}$upTimeSep${hrs.toString()}${hrD}$upTimeSep${min.toString()}${minD}$upTimeSep${sec.toString()}${secD}"
    updateAttr("formattedUptime", attrval) 
    
    Long upt = new Date().getTime().toLong() - (uptime.toLong()*1000)
    
    Date upDate = new Date(upt)    
    updateAttr("lastRestart", upt)
    
    if(rsrtSdfPref == null){
        device.updateSetting("rsrtSdfPref",[value:"yyyy-MM-dd HH:mm:ss",type:"string"])
        rsrtSdfPref="yyyy-MM-dd HH:mm:ss"
    }
    if(rsrtSdfPref == "Milliseconds") 
        updateAttr("lastRestartFormatted", upDate.getTime())
    else {
        SimpleDateFormat sdf = new SimpleDateFormat(rsrtSdfPref)
        updateAttr("lastRestartFormatted", sdf.format(upDate.getTime()))
    }
}

@SuppressWarnings('unused')
Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    if(logResponses) log.info "File xFer Status: $retStat"
    return retStat
}

@SuppressWarnings('unused')
String readExtFile(fName){
    if(security) cookie = getCookie()    
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
            ]        
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               if(debugEnable) log.info "Read External File result: delim"
               return delim
            }
            else {
                log.error "Read External - Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
String toCamelCase(init) {
    if (init == null)
        return null;

    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

void createHtml(){
    byte[] rData = downloadHubFile("$alternateHtml")
    String fContents = new String(new String(rData))

    if(fContents == 'null' || fContents == null) {
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/nrsInfoTemplate.res","nrsInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"nrsInfoTemplate.res", type:"string"]) 
        fContents = downloadHubFile("nrsInfoTemplate.res")
    }
    List fRecs=fContents.split("\n")
    String html = ""
    fRecs.each {
        int vCount = it.count("<%")
        if(debugEnable) log.debug "variables found: $vCount"
        if(vCount > 0){
            recSplit = it.split("<%")
            if(debugEnable) log.debug "$recSplit"
            recSplit.each {
                if(it.indexOf("%>") == -1)
                    html+= it
                else {
                    vName = it.substring(0,it.indexOf('%>'))
                    if(debugEnable) log.debug "${it.indexOf("5>")}<br>$it<br>${it.substring(0,it.indexOf("%>"))}"
                    if(vName == "date()" || vName == "@date")
                        aVal = new Date()
                    else if (vName == "@version")
                        aVal = version()
                    else {
                        aVal = device.currentValue("$vName",true)
                        String attrUnit = device.currentState(vName)?.unit
                        if (attrUnit != null) aVal+=" $attrUnit"
                    }
                    html+= aVal
                    if(it.indexOf("%>")+2 != it.length()) {
                        if(debugEnable) log.debug "${it.substring(it.indexOf("%>")+2)}"
                        html+=it.substring(it.indexOf("%>")+2)
                    }
                }                 
            }
        }
        else html += it
    }
    updateAttr("html", html)

}

@Field sdfList = ["yyyy-MM-dd","yyyy-MM-dd HH:mm","yyyy-MM-dd h:mma","yyyy-MM-dd HH:mm:ss","ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "HH:mm", "H:mm","h:mma", "HH:mm:ss", "Milliseconds"]
