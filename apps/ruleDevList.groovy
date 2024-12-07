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
 *		07Dec2024		thebearmay				eliminate duplicates
*/
import groovy.transform.Field
static String version()	{  return '0.0.2'  }

definition (
	name: 			"Rule Device Table", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Simple Table Display for Devices in use by Rules",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/ruleDevList.groovy",
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
    dynamicPage (name: "mainPage", title: "Rule Devices List", install: true, uninstall: true) {
        section("") {
            if(minVerCheck("2.4.0.0")) {
                childApps = getRuleList()
                oTable = "$tableStyle<table class='mdl-data-table tstat-col'><tr><th>Rule Name</th><th>Devices</th></tr>"
                childApps.each { ca ->
		            jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${ca.key}")
                    oTable += "<tr><td><a href='http://${location.hub.localIP}/installedapp/configure/${ca.key}'>${ca.value}</a></td><td>"
                    dCount = 0
                    aSHold=[]
                    jData.appSettings.each { aS ->
                        aS.deviceList.each{
                            if(!aSHold.contains(it.key)){ 
                            	if (dCount > 0) oTable += ', '
                            	oTable += "<a href='http://${location.hub.localIP}/device/edit/${it.key}'>${it.value}</a>"
                            	dCount++
                                aSHold.add(it.key)
                        	}
                        }
                    }
                    oTable += '</td></tr>'
                }
                oTable += '</table>'
                paragraph oTable
            } else paragraph "Must be on HE v2.4.0.0 or Higher"
        }     
    }
}

ArrayList getRuleList() {
    Map requestParams =
	[
        uri:  "http://127.0.0.1:8080",
        path:"/hub2/appsList"
	]

    httpGet(requestParams) { resp ->
        wrkList = []
        resp.data.apps.each{
            if(it.data.type.contains("Rule")){
                it.children.each{
                    wrkMap =[key:"${it.data.id}",value:"${it.data.name}"]
                    wrkList.add(wrkMap)
                }
            }
        }
        
        return wrkList
    }
}

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
                return resp.data
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read JFile Error: ${exception.message}"
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

def appButtonHandler(btn) {
    switch(btn) {
       
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:left;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;}</style>"
