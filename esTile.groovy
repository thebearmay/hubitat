 /*  Echo Speaks Tile
 *
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
 *    28Aug2023    thebearmay    HE 2.3.6.x changes
 *    11Sep2023    thebearmay    Add server attribute option
 *    18Sep2023    thebearmay    Add Debug Logging option
 *    23Oct2023    thebearmay    Add serverIp as an atttribute
 *    18Nov2024    thebearmay    HE 2.4.0.x changes
 */

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
@Field static final String okSymFLD       = "\u2713"
@Field static final String notOkSymFLD    = "<span style='color:red'>\u2715</span>"
@Field static final String sBLANK         = ''
@Field static String minFwVersion = "2.3.6.121"


@SuppressWarnings('unused')
static String version() {return "0.0.6"} 

metadata {
    definition (
        name: "ES Tile", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/esTile.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        
        attribute "cookieRefreshDays", "number"
        attribute "serverData","string"
        attribute "cookieData","string"
        attribute "csrf","string"
        attribute "amazonDomain","string"
        attribute "tm2NewAtRfrsh", "string"
        attribute "tmFromAtRrsh", "string"
        attribute "serverLocation", "string"
        attribute "anError","string"
        attribute "serverIp","string"
        
        attribute "html","string"
        attribute "htmlAlt", "string"
       
        //command "processPage"
        command "refreshHTML"
    }   
}

preferences {
    input("vDisp", "hidden", title:"Driver Version",description:"<b>v${version()}</b>")
    if(location.hub.firmwareVersionString < minFwVersion){
        input("errMsg", "hidden", title:"<b>Minimum Version Error</b>",description:"<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>", width:8)
    }
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
    input("pollRate","number", title:"Poll rate (in minutes) Disable:0):", defaultValue:720, submitOnChange:true, width:4)
    input("debugEnabled","bool", title:"Enable Debug Logging", defaultValue:false, submitOnChange:true, width:4)

}

@SuppressWarnings('unused')
def installed() {
    if(location.hub.firmwareVersionString < minFwVersion){
        updateAttr("anError","<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
    } else 
        device.deleteCurrentState("anError")
        
}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh(){
    if(location.hub.firmwareVersionString < minFwVersion){
        updateAttr("anError","<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
        return
    } else
        device.deleteCurrentState("anError")
    processPage()
    refreshHTML()
    if(pollRate == null)
        device.updateSetting("pollRate",[value:720,type:"number"])   
    if(pollRate > 0) {
        runIn(pollRate*60,"refresh")
    }

}

def updated(){
    if(pollRate == null)
        device.updateSetting("pollRate",[value:720,type:"number"])   
    if(pollRate > 0) {
        runIn(pollRate*60,"refresh")
    }
    if(debugEnabled)
        runIn(1800,"logsOff")
}
void processPage(){
    app = findPage()
    if(app==-1) {
        log.error "Echo Speaks not Installed"
        return
    }
    if(minVerCheck("2.4.0.0")) 
        processJsonData(app)
    else{    
        pData=readExtPage("http://127.0.0.1:8080/installedapp/status/$app")
        dWork = pData.substring(pData.indexOf('refreshCookieDays'),pData.indexOf('refreshCookieDays')+500)
        if(debugEnabled) "Refresh Days Work: <br> $dWork"
        dWork.replace('<','')
        dWork=dWork.split(' ')
        dWork.each{
            if(debugEnable) "Refresh Split Each: $it"
            if(it.isNumber()) updateAttr("cookieRefreshDays",it.toInteger())
        }
        dWork = pData.substring(pData.indexOf('serverDataMap'),pData.indexOf('serverDataMap')+800)
        dWork = dWork.substring(dWork.indexOf('{'),dWork.indexOf('}')+1)
   
        createServerMap(dWork)
        if(pData.indexOf("cookieData") >-1){
            if(debugEnabled) log.debug "Found Cookie Data"
            updateAttr("cookieData",true)
            if(pData.indexOf("csrf") > -1){
                updateAttr("csrf", true)
                if(debugEnabled) log.debug "Found csrf"
            }
        }
        dWork = pData.substring(pData.indexOf('amazonDomain'),pData.indexOf('amazonDomain')+300)
        if(debugEnabled) log.debug "Amazon Domain Raw: $dWork"
        dWork.replace('<','')
        dWork=dWork.split(' ')
        dWork.each{
            if(it.contains(".")){
                updateAttr("amazonDomain",it.trim())
                if(debugEnabled) log.debug "Amazon Domain: ${it.trim()}"
            }
        }
    }
}

void processJsonData(app){
    jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/$app") 
    cookieRefreshDays = 0
    jData.appSettings.each {
        if(it.name == "cookieRefreshDays"){
            cookieRefreshDays = it.value.toInteger()
            updateAttr("cookieRefreshDays",cookieRefreshDays)
        }
    }
    def cookieData = ''
    jData.appState.each{
        if(it.name == 'serverDataMap'){
            updateAttr("serverData", JsonOutput.toJson(it.value))
        }
        if(it.name == 'cookieData'){
            cookieData = it.value
            updateAttr("cookieData",true)
            if(cookieData.csrf){
                updateAttr("csrf", true)
                if(debugEnabled) log.debug "Found csrf"
            }
        }
        if(it.name == 'amazonDomain'){
            updateAttr("amazonDomain",it.value.trim())
        }
    }
}


Integer findPage(){

	def params = [
		uri: "http://127.0.0.1:8080/hub2/appsList",
	    contentType: "application/json",
        followRedirects: false,
        textParser: false
    ]
	
    appId = -1
	try {
		httpGet(params) { resp ->  
            appId = -1
            if(debugEnabled) log.debug "GET: ${resp.data.apps}"
            resp.data.apps.each {a ->
                if(debugEnabled) log.debug "${a.data.type}"
                if("${a.data.type}" == "Echo Speaks") {
                    appId = a.data.id
                    if(debugEnabled) log.debug "Found it ${a.data.id}"
                }
            }
        }

	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    
    return appId
}

void createServerMap(sData){
    if(debugEnabled)
        log.debug "Server Data Raw: $sData"
    sWork = sData.replace("&#x3D;",'\":\"')
    sWork = sWork.replace(', ','","')
    sWork = sWork.replace('{','{\"')
    sWork = sWork.replace('}','\"}')
    if(debugEnabled)  log.debug "Server Information: $sWork"
    updateAttr("serverData", sWork)
}

void refreshHTML(){
    if(debugEnabled)  log.debug "Refreshing HTML"
    Long tNow = new Date().getTime()
    if(minVerCheck("2.4.0.0")) {
        def jSlurp = new JsonSlurper()
        serverData = jSlurp.parseText(device.currentValue("serverData",true))
    } else {
        serverData = device.currentValue("serverData",true)
    }
    nextCookieRefreshDur()
    wkStr = "<table style='color:mediumblue;font-size:small'><tr><th>Auth Status: "
    if(device.currentValue("csrf",true) == "true" && device.currentValue("cookieData",true) == "true")
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</th></tr><tr><td>&nbsp;&nbsp;Cookie: "
    if(device.currentValue("cookieData",true) == "true")
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</td></tr><tr><td>&nbsp;&nbsp;CSRF: "
    if(device.currentValue("csrf",true) == "true")
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</td></tr><tr><th>Cookie Data</th></tr>"
    wkStr2 = wkStr
    wkStr+="<tr><td>Last Refresh: ${serverData.lastCookieRrshDt}</td></tr>"
    startDate = Date.parse("E MMM dd HH:mm:ss z yyyy", serverData.lastCookieRrshDt).getTime()
    nextDate = startDate + (86400000 * device.currentValue("cookieRefreshDays").toInteger())
    //log.debug "$tNow $nextDate"
    SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(nextDate > tNow){
        wkStr+="<tr><td>Next Refresh: ${sdf.format(nextDate)}</td></tr>"
    } else {
        wkStr+="<tr><td style='color:red;font-weight:bold'>Missed Refresh: ${sdf.format(nextDate)}</td></tr>"
    }
    wkStr+="</td></tr><tr><th>Server Data</th></tr>"
    wkStr+="<tr><td>Heroku: "
    if(serverData.onHeroku == "true" || serverData.onHeroku){
        wkStr+=okSymFLD
        updateAttr("serverLocation","Heroku")
    } else {
        wkStr+=notOkSymFLD
        updateAttr("serverLocation","Local")
    }
    wkStr+="</td></tr><tr><td>Local Server: "
    if(serverData.isLocal == "true"  || serverData.isLocal)
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</td></tr><tr><td>Server IP: ${serverData.serverHost}</td></tr>"
    updateAttr("serverIp","${serverData.serverHost}".substring(7))
    wkStr+="<tr><td>Domain: ${device.currentValue("amazonDomain",true)}</td></tr>"
    
    wkStr+="</table>"
    updateAttr("html",wkStr)
    
    wkStr2+="<tr><td>Last Refresh: ${device.currentValue("tmFromAtRrsh",true)} ago</td></tr>"
    if(nextDate > tNow){
        wkStr2+="<tr><td>Next Refresh: ${device.currentValue("tm2NewAtRfrsh",true)}</td></tr>"
    } else {
        wkStr2+="<tr><td style='color:red;font-weight:bold'>Missed Refresh: ${sdf.format(nextDate)}</td></tr>"
    }
    wkStr2+="</td></tr><tr><th>Server Data</th></tr>"
    wkStr2+="<tr><td>Heroku: "
    if(serverData.onHeroku == "true" || serverData.onHeroku)
       wkStr2+=okSymFLD
    else
        wkStr2+=notOkSymFLD
    wkStr2+="</td></tr><tr><td>Local Server: "
    if(serverData.isLocal == "true" || serverData.isLocal)
       wkStr2+=okSymFLD
    else
        wkStr2+=notOkSymFLD
    wkStr2+="</td></tr><tr><td>Server IP: ${serverData.serverHost}</td></tr>"
    wkStr2+="<tr><td>Domain: ${device.currentValue("amazonDomain",true)}</td></tr>"
    wkStr2+="</table>"
    updateAttr("htmlAlt",wkStr2)
    if(debugEnabled)  log.debug "HTML Refresh complete"    
}

String nextCookieRefreshDur() {
    Long tNow = new Date().getTime()
    def jSlurp = new JsonSlurper()
    serverData = jSlurp.parseText(device.currentValue("serverData",true))
    Integer days = device.currentValue("cookieRefreshDays").toInteger()
    String lastCookieRfsh = serverData.lastCookieRrshDt
    if(!lastCookieRfsh) { return "Not Sure"}
    Date lastDt = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(Date.parse("E MMM dd HH:mm:ss z yyyy", lastCookieRfsh)))   
                                                                             
    String dMinus = seconds2Duration(((tNow-lastDt.getTime())/1000).toInteger(),false,3)
    updateAttr("tmFromAtRrsh", dMinus)                                                                        
                                                                             
    Date nextDt = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(lastDt + days))
    Integer diff = ( (nextDt.getTime() - wnow()) / 1000) as Integer
    String dur = seconds2Duration(diff, false, 3)
    // log.debug "now: ${now} | lastDt: ${lastDt} | nextDt: ${nextDt} | Days: $days | Wait: $diff | Dur: ${dur}"
    updateAttr("tm2NewAtRfrsh", dur)
}

String formatDt(Date dt, Boolean tzChg=true) {
    def tf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(tzChg) { if(location.timeZone) { tf.setTimeZone((TimeZone)location?.timeZone) } }
    return (String)tf.format(dt)
}

private Long wnow(){ return (Long)now() }

@SuppressWarnings('GroovyAssignabilityCheck')
static String seconds2Duration(Integer itimeSec, Boolean postfix=true, Integer tk=2) {
    Integer timeSec=itimeSec
    Integer years = Math.floor(timeSec / 31536000); timeSec -= years * 31536000
    Integer months = Math.floor(timeSec / 31536000); timeSec -= months * 2592000
    Integer days = Math.floor(timeSec / 86400); timeSec -= days * 86400
    Integer hours = Math.floor(timeSec / 3600); timeSec -= hours * 3600
    Integer minutes = Math.floor(timeSec / 60); timeSec -= minutes * 60
    Integer seconds = Integer.parseInt((timeSec % 60) as String, 10)
    Map d = [y: years, mn: months, d: days, h: hours, m: minutes, s: seconds]
    List l = []
    if(d.d > 0) { l.push("${d.d} ${pluralize(d.d, "day")}") }
    if(d.h > 0) { l.push("${d.h} ${pluralize(d.h, "hour")}") }
    if(d.m > 0) { l.push("${d.m} ${pluralize(d.m, "min")}") }
    if(d.s > 0) { l.push("${d.s} ${pluralize(d.s, "sec")}") }
    return l.size() ? "${l.take(tk ?: 2)?.join(", ")}${postfix ? " ago" : sBLANK}".toString() : "Not Sure"
}

static String pluralize(Integer itemVal, String str) { return (itemVal > 1) ? str+"s" : str }

def readJsonPage(fName){
    def params = [
        uri: fName,
        contentType: "application/json",
        //textParser: false,
        headers: [
            "Connection-Timeout":600
        ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                //def jSlurp = new JsonSlurper()
                //Map jData = (Map)jSlurp.parseText((String)resp.data)
                //log.debug "${resp.properties}"
                return resp.data
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
     
}


String readExtPage(fName){  
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [
            "Connection-Timeout":600
        ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return """${resp.data}"""
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
}

Boolean minVerCheck(vStr){  //check if HE is >= to the requirement
    fwTokens = location.hub.firmwareVersionString.split("\\.")
    vTokens = vStr.split("\\.")
    if(fwTokens.size() != vTokens.size())
        return false
    rValue =  true
    for(i=0;i<vTokens.size();i++){
        if(vTokens[i].toInteger() < fwTokens[i].toInteger())
           i=vTokens.size()+1
        else
        if(vTokens[i].toInteger() > fwTokens[i].toInteger())
            rValue=false
    }
    return rValue
}


@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
