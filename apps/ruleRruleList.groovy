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
 *    05Jan2025        thebearmay             capture privateF and actRuleMain
*/
import groovy.transform.Field
import groovy.json.JsonSlurper
static String version()	{  return '0.0.11'  }

definition (
	name: 			"Rule References Rule Table", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Simple Table Display for Rules referencing other Rules",
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
        section("<h4>Settings</h4>", hideable:true, hidden: true){
			input "debugEnabled", "bool", title: "<b>Enable Debug Logging</b>", defaultValue: false, submitOnChange:true
			input "nameOverride", "text", title: "<b>New Name for Application</b>", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
			if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)				
		}        
        section("") {
            if(debugEnabled) runIn(1800,logsOff)
            if(minVerCheck("2.4.0.0")) {
                input "getRules", "button", title: "Create Tables"
                if(state.getRules){
                    state.getRules = false
                	childApps = getRuleList()
	                rule2Runner=[]
     	           oTable = "$tableStyle<table class='mdl-data-table tstat-col'><tr><th colSpan='2' style='text-align:center;border-bottom:1px solid;'><b>Rule Affects</b></th></tr><tr><th>Rule Name(id)</th><th>Rules Affected(id)</th></tr>"//<th>Related Rules</th></tr>"
        	        childApps.each { ca ->
		    	        jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${ca.key}")
                	    aSHold=[]
                    	oTable += "<tr><td><a href='http://${location.hub.localIP}/installedapp/configure/${ca.key}'>${ca.value}(${ca.key})</a></td><td>"
	                    jData.appSettings.each { aS ->
    	                    if(aS.name.toString().contains('ruleAct') || aS.name.toString().contains('pauseRule.') || aS.name.toString().contains('valFunction.') || aS.name.toString().contains('privateT') || aS.name.toString().contains('privateF') || aS.name.toString().contains('stopAct')){
        	                    vSplit = aS.value.toString().replace('\"','').replace('[','').replace(']','').split(',')
                            //log.debug "${aS.value} ${vSplit}"
            	                vSplit.each{
                	                if(ca.key != it)
									   aSHold.add("\"$it\"")
                        	    }
                        	}
	                    }
    	                if(debugEnabled) 
        	            	log.debug "$aSHold"
            	        i=0
                	    aSHold.sort().unique().each {
                    	    sInx = it.toString().indexOf('\"')+1
	                        eInx = it.toString().indexOf('\"',sInx)
    	                    aKey=it.toString().substring(sInx,eInx)
        	                if(aKey != 'null' && aKey != ca.key ){
            	            	if(i>0) oTable += ",<br> "
                	        	i++
                    	    	aName=getName(aKey,childApps)
                        		oTable+= "<a href='http://${location.hub.localIP}/installedapp/configure/${aKey}'>${aName}(${aKey})</a>"
                        		rule2Runner.add([key:"$aKey", value:"${ca.key}", name:"$aName"])
	                        }
    	                }
        	            oTable += '</td></tr>'
            	    }
                	oTable += '</table>'
	                paragraph oTable
    	            r2r = rule2Runner.sort{it.name+it.key}.unique()
        	        
					oTable = "$tableStyle<table class='mdl-data-table tstat-col'><th colSpan='2' style='text-align:center;border-bottom:1px solid;'><b>In Use</b></th></tr><tr><th>Rule Name(id)</th><th>In Use By(id)</th></tr>"
					lastKey = 0
	                holdR=[]
    	            r2r.each { r ->
        	            if(lastKey != r.key && r.key !='null' ){
            	            i=0
                	        holdR.sort().each{
                    	        if(i>0)
                        			oTable+= ',<br>'
								oTable+="<a href='http://${location.hub.localIP}/installedapp/configure/${it}'>${getName(it,childApps)}(${it})"
	                            i++
    	                    }  
        	                if(i>0){
            	                oTable+='</td></tr>'
                	        }
                    	    oTable+="<tr><td><a href='http://${location.hub.localIP}/installedapp/configure/${r.key}'>${r.name}(${r.key})</a></td><td>"
                        	holdR=[]
	                        holdR.add(r.value)
    	                    lastKey = r.key
        	            } else if(r.key != 'null'){
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
                }
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
                    if(it.data.disabled)
                    	wrkMap =[key:"${it.data.id}",value:"<s>${it.data.name}</s>"]
                    else
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
		case "getRules":
        	state.getRules = true
        	break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th {padding:8px 8px;text-align:left;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;}</style>"
