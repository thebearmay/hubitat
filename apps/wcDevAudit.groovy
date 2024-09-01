/*
 * 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
 *    31Aug2024        thebearmay            add a try..catch around the piston processing
 *    01Sep2024                              split the try..catch into three separate iterations
*/

static String version()	{  return '0.0.8'  }
import java.security.MessageDigest

definition (
	name: 			"webCoRE Device Audit", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Examines Pistons for broken device links",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/wcDevAudit.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"

}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "webCoRE Device Audit", install: true, uninstall: true) {
        section("") {
            input("runList", "button", title:"Generate List")
            if(state.getErr){
                state.getErr = false
                paragraph "<h3>Error List</h3><p>${getErrors()}</p>"
                if(errList.size() < 1) paragraph "No Missing Devices Found"
            }

        }
        section("Exclusions Maintenance",hideable:true,hidden:true){
            if(!state.exclude) state.exclude = [:]
            else{
                paragraph "<h3>Excluded Hashes</h3>"
                state.exclude.each {
                    paragraph "${it.key}::${it.value}"
                }
            }
            input("excName", "text", title:"<b>Name of Exclusion</b>",size:6)
            input("excHash", "text", title:"<b>Hash Value to Exclude</b>",size:6)
            input("addExc","button",title:"Add", size:4)
            if(state.addExclusion) {
                state.addExclusion = false
                excName = toCamelCase(excName)
                state.exclude["$excName"]="$excHash"
                app.removeSetting("excName")
                app.removeSetting("excHash")
            }
            input("delExc","button",title:"Delete",size:4)
            if(state.delExclusion) {
                state.delExclusion = false
                excName = toCamelCase(excName)
                wMap = state.exclude
                state.exclude = [:]
                wMap.each{
                    if(it.key != excName)
                    state.exclude["${it.key}"]="${it.value}"
                }
                app.removeSetting("excName")
            }
        }
    }
}

String readPage(pId){
    def params = [
        uri: "http://127.0.0.1:8080/installedapp/status/$pId",
        contentType: "text/html",
        textParser: true          
    ]
    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return """${resp.data}"""
            }
            else {
                log.error "Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

String getErrors(){
    childApps = getPistonList()
    //log.debug "Child apps:${childApps}"
    wcApp = readPage(state.wcID)
    //uploadHubFile("wcaWork.txt",wcApp.getBytes('UTF-8'))
    parDevList = []
    parDevList2 = []
    devStart = wcApp.indexOf('/device/edit/')//,wcApp.indexOf('dev:all'))
    devEnd = wcApp.indexOf('"', devStart)
    //paragraph "$devStart, $devEnd"
    i=0
    while (devEnd > -1 && devStart > -1){
        if(devStart > -1 && devEnd > -1) {
            devNum = wcApp.substring(devStart+13,devEnd).toInteger()
            parDevList.add(devNum)
            hDnum = md5("core.$devNum")
            parDevList2.add(hDnum)
        }
        devStart = wcApp.indexOf('/device/edit/',devEnd)
        devEnd = wcApp.indexOf('"', devStart)
        i++
    }
    if(state.exclude) {
        state.exclude.each{
            //parDevList2.add('ff91564bbd8403bb7f9e5043bb7a9e82')//hsmStatus
            parDevList2.add(it.value.toString())
        }
    }
    parDevList = parDevList.unique().sort()
    parDevList2 = parDevList2.unique().sort()
    //paragraph "Parent List $parDevList"
    //log.debug "Parent List2 $parDevList2"
    
    errList = ''
    childApps.each {
        i=0
        //log.debug it.key
        chdApp = readPage(it.key)
        //uploadHubFile("wcaWork2.txt",chdApp.getBytes('UTF-8'))
        devStart = chdApp.indexOf('/device/edit/')
        devEnd = chdApp.indexOf('"', devStart)
        devList = []
        try{
            while (devEnd > -1 && devStart > -1 && devStart > devEnd){
                if(devStart > -1 && devEnd > -1) {
                    devChk = chdApp.substring(devStart+13,devEnd).toInteger()
                    if(!parDevList.contains(devChk)){
                        dName = chdApp.substring(devEnd+2,chdApp.indexOf('<',devEnd+2))
                        errList +="Piston <a href='http://${location.hub.localIP}/installedapp/status/${it.key}'>${it.value}</a> is missing device <b>$dName</b>\n"
                    }
                }
                devList.add(devChk)
                devStart = chdApp.indexOf('/device/edit/',devEnd)
                devEnd = chdApp.indexOf('"', devStart)
                i++
            }
        } catch (ex) {
            log.error "Pass 1: ${ex.message}"
        }
        try{
            devEnd = chdApp.indexOf('not found}')
            devStart = chdApp.indexOf('Device', devEnd-70)
            i=0
            while (devEnd > -1 && devStart > -1 && devStart > devEnd){
                dMsg = 'd'+chdApp.substring(devStart+1,devEnd+9)
                errList +="Piston <a href='http://${location.hub.localIP}/installedapp/status/${it.key}'>${it.value}</a> <b>$dMsg</b>\n"
                devEnd = chdApp.indexOf('not found}',devStart+71)
                devStart = chdApp.indexOf('Device', devEnd-70)
                i++
            }
            devStart=chdApp.indexOf('d&#x3D;:')
            devEnd=chdApp.indexOf(':',devStart+8)
        } catch (ex) {
            log.error "Pass 2: ${ex.message}"
        }
        //if(devEnd > -1 && devStart > -1)
        //    log.debug "${it.value}: ${chdApp.substring(devStart+8,devEnd)}"
        try{
            i=0
            while (devEnd > -1 && devStart > -1 && devStart > devEnd){
                hashVal = chdApp.substring(devStart+8,devEnd)
                if(!parDevList2.contains(hashVal))
                    errList +="Piston <a href='http://${location.hub.localIP}/installedapp/status/${it.key}'>${it.value}</a> contains unknown device <b>':$hashVal:'</b>\n"
                devStart=chdApp.indexOf('d&#x3D;:',devEnd)
                devEnd=chdApp.indexOf(':',devStart+8)
                i++
            }                
        } catch (ex) {
            log.error "Pass 3: ${ex.message}"
        }
    }
    return errList
}

ArrayList getPistonList() {
    Map requestParams =
	[
        uri:  "http://127.0.0.1:8080",
        path:"/hub2/appsList"
	]

    httpGet(requestParams) { resp ->
        wrkList = []
        resp.data.apps.each{
            if(it.data.type == "webCoRE"){
                state.wcID = it.data.id
                it.children.each{
                    if(it.data.type == 'webCoRE Piston'){
                        wrkMap =[key:"${it.data.id}",value:"${it.data.name}"]
                        wrkList.add(wrkMap)
                    }
                }
            }
        }
        
        return wrkList
    }
}

String md5(String md5){ 
	MessageDigest md=MessageDigest.getInstance('md5')
	byte[] array=md.digest(md5.getBytes())   
	String r= ''
	Integer l=array.size()
	for(Integer i=0; i<l; ++i){
		r+=Integer.toHexString((array[i] & 0xFF)| 0x100).substring(1,3)
	}
	return r
}

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
    
def appButtonHandler(btn) {
    switch(btn) {
        case "runList":
            state.getErr = true
            break
        case "addExc":
            state.addExclusion = true
            break
        case "delExc":
            state.delExclusion = true
            break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
