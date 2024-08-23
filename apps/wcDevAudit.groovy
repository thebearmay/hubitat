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
 */


static String version()	{  return '0.0.0'  }
import groovy.json.JsonOutput

import groovy.json.JsonSlurper
import java.net.URLEncoder
import groovy.transform.Field

@Field btnBckGrd 
@Field btnTxColor

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
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        section("Errors Found") {
            childApps = getPistonList()
            //paragraph "${childApps}"
            wcApp = readPage(state.wcID)
            //uploadHubFile("wcaWork.txt",wcApp.getBytes('UTF-8'))
            parDevList = []
            devStart = wcApp.indexOf('/device/edit/',wcApp.indexOf('dev:all'))
            devEnd = wcApp.indexOf('"', devStart)
            //paragraph "$devStart, $devEnd"
            i=0
            while (devEnd > -1 && devStart > -1){
                if(devStart > -1 && devEnd > -1) {
                    parDevList.add(wcApp.substring(devStart+13,devEnd).toInteger())
                }
                devStart = wcApp.indexOf('/device/edit/',devEnd)
                devEnd = wcApp.indexOf('"', devStart)
                i++
            }
            //paragraph "$parDevList"
            childApps.each {
                i=0
                chdApp = readPage(it)
                devStart = chdApp.indexOf('/device/edit/')
                devEnd = chdApp.indexOf('"', devStart)
                errList = ''
                while (devEnd > -1 && devStart > -1 && i<15){
                    if(devStart > -1 && devEnd > -1) {
                        devChk = chdApp.substring(devStart+13,devEnd).toInteger()
                        if(parDevList.indexOf(devChk) == -1)
                        dName = chdApp.substring(devEnd+2,chdApp.indexOf('<',devEnd+2))
                        errList +="<a href='http://${location.hub.localIP}/installedapp/status/$it'>Piston #$it</a> is missing device $dName\n"
                    }
                    devStart = chdApp.indexOf('/device/edit/',devEnd)
                    devEnd = chdApp.indexOf('"', devStart)
                    i++
                }
               
            }
            paragraph errList

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
/*               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                       char c =  (char) i
                       delim+=c
                       i = resp.data.read()
                       if(i < 0 || i > 255) return delim
               } 
               if(debugEnabled) log.info "Read Page result: delim"
               return delim.toString()
*/
                return """${resp.data}"""//resp.data.toString()
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
                    wrkList.add(it.data.id)
                }
            }
        }
        
        return wrkList
    }
}
