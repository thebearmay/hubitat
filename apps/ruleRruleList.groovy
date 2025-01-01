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
import groovy.transform.Field
import groovy.json.JsonSlurper
static String version()	{  return '0.0.6'  }

definition (
	name: 			"Rule Runs Rule Table", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Simple Table Display for Devices in use by Rules",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/ruleRruleList.groovy",
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
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
        section("") {
            input "debugEnabled", "bool", title:"Enable Debug Logging"
            if(debugEnabled) runIn(1800,logsOff)
            if(minVerCheck("2.4.0.0")) {
                childApps = getRuleList()
                rule2Runner=[]
                oTable = "$tableStyle<table class='mdl-data-table tstat-col'><tr><th colSpan='2' style='text-align:center;border-bottom:1px solid;'><b>Runs</b></th></tr><tr><th>Rule Name</th><th>Rules Run/Paused</th></tr>"//<th>Related Rules</th></tr>"
                childApps.each { ca ->
		            jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${ca.key}")
                    aSHold=[]
                    oTable += "<tr><td><a href='http://${location.hub.localIP}/installedapp/configure/${ca.key}'>${ca.value}(${ca.key})</a></td><td>"
                    jData.appSettings.each { aS ->
                        if(aS.value == 'getRuleActions'){
                            suffix = aS.name.substring(aS.name.indexOf('.')+1,)
                            if(debugEnabled) log.debug "$suffix"
                            jData.appSettings.each { aS2 ->
                                if(aS2.name == "ruleAct.$suffix")
                            		aSHold.add(aS2.value)
                            }
                        }
                        if(aS.name.toString().contains('pauseRule.') || aS.name.toString().contains('valFunction.') || aS.name.toString().contains('privateT')){
                            vSplit = aS.value.toString().replace('\"','').replace('[','').replace(']','').split(',')
                            //log.debug "${aS.value} ${vSplit}"
                            vSplit.each{
								aSHold.add("\"$it\"")
                            }
                        }
                    }
                    if(debugEnabled) log.debug "$aSHold"
                    i=0
                    aSHold.sort().unique().each {
                        sInx = it.toString().indexOf('\"')+1
                        eInx = it.toString().indexOf('\"',sInx)
                        if(i>0) oTable += ", "
                        i++
                        oTable+= "<a href='http://${location.hub.localIP}/installedapp/configure/${it.toString().substring(sInx,eInx)}'>${getName(it.toString().substring(sInx,eInx),childApps)}(${it.toString().substring(sInx,eInx)})</a>"
                        rule2Runner.add([key:"${it.toString().substring(sInx,eInx)}", value:"${ca.key}"])
                    }
  /*                  oTable+= "</td><td>"
                    jData.appState.each{
                    	if(it.name == 'installedRules'){
                            i=0
							itVal = (ArrayList) it.value
							if(debugEnabled)
                            	log.debug "${itVal}"
                            itVal.each{
                                it = it.toString()
                                //log.debug "${it}"
                                s1=it.indexOf('{')+1
                                e1=it.indexOf('=')
                                e2=it.indexOf('}')
                                itK=it.substring(s1,e1)
                                itV=it.substring(e1+1,e2)
                                
                        		if(i>0) oTable += ", "
                               	oTable+="<a href='http://${location.hub.localIP}/installedapp/configure/${itK}'>${itV}(${itK})</a>"
                                i++
                            }
                    	}
                    } */
                    oTable += '</td></tr>'
                }
                oTable += '</table>'
                paragraph oTable
                r2r = rule2Runner.sort{it.key}.unique()
				oTable = "$tableStyle<table class='mdl-data-table tstat-col'><th colSpan='2' style='text-align:center;border-bottom:1px solid;'><b>Run By</b></th></tr><tr><th>Rule Name</th><th>Run/Paused By</th></tr>"
				lastKey = 0
                holdR=[]
                r2r.each { r ->
                    if(lastKey != r.key ){
                        i=0
                        holdR.sort().each{
                            if(i>0)
                        		oTable+= ', '
							oTable+="<a href='http://${location.hub.localIP}/installedapp/configure/${it}'>${getName(it,childApps)}(${it})"
                            i++
                        }  
                        if(i>0){
                            oTable+='</td></tr>'
                        }
                        oTable+="<tr><td><a href='http://${location.hub.localIP}/installedapp/configure/${r.key}'>${getName(r.key,childApps)}(${r.key})</a></td><td>"
                        holdR=[]
                        holdR.add(r.value)
                        lastKey = r.key
                    } else {
                        holdR.add(r.value)
                    }
                }
                i=0
                holdR.sort().each{
                	if(i>0)
						oTable+= ', '
                    oTable+="<a href='http://${location.hub.localIP}/installedapp/configure/${it}'>${getName(it,childApps)}(${it})"
                	i++
                }  
                oTable+='</td></tr></table>' 
				paragraph oTable
            } else paragraph "Must be on HE v2.4.0.0 or Higher"
        }     
    }
}

String getName(keyVal, appList){
    rVal = ''
    appList.each{
        if(debugEnabled) log.debug "$keyVal ${it.key}"
        if(it.key == keyVal)
        	rVal = it.value
    }
    return rVal
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
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th {padding:8px 8px;text-align:left;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;}</style>"
