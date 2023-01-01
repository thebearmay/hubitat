 /*  Echo Speaks Tile

 */

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.transform.Field
@Field static final String okSymFLD       = "\u2713"
@Field static final String notOkSymFLD    = "<span style='color:red'>\u2715</span>"
@Field static final String sBLANK         = ''



@SuppressWarnings('unused')
static String version() {return "0.0.1"}

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
        
        attribute "html","string"
        attribute "htmlAlt", "string"
       
        //command "processPage"
        command "refreshHTML"
    }   
}

preferences {
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
    input("pollRate","number", title:"Poll rate (in minutes) Disable:0):", defaultValue:720, submitOnChange:true, width:4)

}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh(){
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
}
void processPage(){
    app = findPage()
    if(app==-1) {
        log.error "Echo Speaks not Installed"
        return
    }
    pData=readPage("http://127.0.0.1:8080/installedapp/status/$app")
    dWork = pData.substring(pData.indexOf('refreshCookieDays'),pData.indexOf('refreshCookieDays')+500)
    dWork.replace('<','')
    dWork=dWork.split(' ')
    dWork.each{
        if(it.isNumber()) updateAttr("cookieRefreshDays",it.toInteger())
    }
    dWork = pData.substring(pData.indexOf('serverDataMap'),pData.indexOf('serverDataMap')+800)
    dWork = dWork.substring(dWork.indexOf('{'),dWork.indexOf('}')+1)
   
    createServerMap(dWork)
    if(pData.indexOf("cookieData") >-1){
        updateAttr("cookieData",true)
        if(pData.indexOf("csrf") > -1)
            updateAttr("csrf", true)
    }
    dWork = pData.substring(pData.indexOf('amazonDomain'),pData.indexOf('amazonDomain')+300)
    dWork.replace('<','')
    dWork=dWork.split(' ')
    dWork.each{
        if(it.contains(".")) updateAttr("amazonDomain",it.trim())
    }

}

Integer findPage(){

	def params = [
		uri: "http://127.0.0.1:8080/installedapp/list",
		textParser: true
	  ]
	
	def allAppsList = []
    def allAppNames = []
	try {
		httpGet(params) { resp ->    
			def matcherText = resp.data.text.replace("\n","").replace("\r","")
			def matcher = matcherText.findAll(/(<tr class="app-row" data-app-id="[^<>]+">.*?<\/tr>)/).each {
				def allFields = it.findAll(/(<td .*?<\/td>)/) // { match,f -> return f } 
				def id = it.find(/data-app-id="([^"]+)"/) { match,i -> return i.trim() }
				def title = allFields[0].find(/data-order="([^"]+)/) { match,t -> return t.trim() }
				allAppsList += [id:id,title:title]
                allAppNames << title
			}

		}
	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    
    for(i=0;i < allAppsList.size();i++) { 
        if(allAppsList[i].title == 'Echo Speaks') {
            //log.debug "Found it"
            return allAppsList[i].id.toInteger()
            break
        }
    }
    return -1
}

void createServerMap(sData){
    sWork = sData.replace("&#x3D;",'\":\"')
    sWork = sWork.replace(', ','","')
    sWork = sWork.replace('{','{\"')
    sWork = sWork.replace('}','\"}') 
    updateAttr("serverData", sWork)
}

void refreshHTML(){
    Long tNow = new Date().getTime()
    def jSlurp = new JsonSlurper()
    serverData = jSlurp.parseText(device.currentValue("serverData",true))
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
    if(serverData.onHeroku == "true")
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</td></tr><tr><td>Local Server: "
    if(serverData.isLocal == "true")
       wkStr+=okSymFLD
    else
        wkStr+=notOkSymFLD
    wkStr+="</td></tr><tr><td>Server IP: ${serverData.serverHost}</td></tr>"
    wkStr+="<tr><td>Domain: ${device.currentValue("amazonDomain",true)}</td></tr>"
    
    wkStr+="</table>"
    updateAttr("html",wkStr)
    
    wkStr2+="<tr><td>Last Refresh: ${device.currentValue("tmFromAtRrsh",true)} ago</td></tr>"
    if(nextDate > tNow){
        wkStr2+="<tr><td>Next Refresh: ${device.currentValue("tm2NewAtRfrsh")}</td></tr>"
    } else {
        wkStr2+="<tr><td style='color:red;font-weight:bold'>Missed Refresh: ${sdf.format(nextDate)}</td></tr>"
    }
    wkStr2+="</td></tr><tr><th>Server Data</th></tr>"
    wkStr2+="<tr><td>Heroku: "
    if(serverData.onHeroku == "true")
       wkStr2+=okSymFLD
    else
        wkStr2+=notOkSymFLD
    wkStr2+="</td></tr><tr><td>Local Server: "
    if(serverData.isLocal == "true")
       wkStr2+=okSymFLD
    else
        wkStr2+=notOkSymFLD
    wkStr2+="</td></tr><tr><td>Server IP: ${serverData.serverHost}</td></tr>"
    wkStr2+="<tr><td>Domain: ${device.currentValue("amazonDomain",true)}</td></tr>"
    wkStr2+="</table>"
    updateAttr("htmlAlt",wkStr2)
    
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

String readPage(fName){
    if(security) cookie = securityLogin().cookie
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
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}
HashMap securityLogin(){
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
	return [result: result, cookie: cookie]
}
