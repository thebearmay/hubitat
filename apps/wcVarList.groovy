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

static String version()	{  return '0.0.4'  }
import java.security.MessageDigest
import groovy.json.JsonSlurper

definition (
	name: 			"webCoRE Variable List", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Examines Pistons for Variables in use",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/wcVarList.groovy",
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
        section("") {
            input("runList", "button", title:"Generate List")
            //input("createCSV", "button", title:"Generate CSV")

            if(state.getVar){
                varList = getVars()
                state.getVar = false
	  			csvData = varList[1]
      			oData = """<script type='text/javascript'>function download() { var csvContent = document.getElementById('cData').innerHTML;var blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });var a = document.createElement('a');var url = URL.createObjectURL(blob);a.href = url;a.download = 'wcVariables.csv';
  document.body.appendChild(a);a.click();}</script><button onclick='download()'>Download CSV</button><div id='cData' style='display:none'>$csvData</div>"""
      			paragraph oData
                paragraph "<h3><b><u>Piston to Variable List</u></b></h3><p>${varList[0]}</p>"
            }
         


        }
    }
}


ArrayList getVars(disp){
    varDispList = ""
    childApps = getPistonList()
    jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${state.wcID}")
    varList = getVarsJ(jData, childApps)
    vNamePrev = ''
    varList.each { varE ->
        if(vNamePrev != varE.pName) {
            vNamePrev = varE.pName
            varDispList += "<h4><u>${varE.pName}</u></h4>"
        }
        //log.debug "$varE"
        if(varE.value != null)
        	varDispList += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${varE.value.key} = ${varE.value.value}<br />"   
    }
    
    rVarList = varList.sort { it.value.key }
    //log.debug "$rVarList"
    varDispList += "<br><h3><b><u>Variable to Piston List</u></b></h3><p>"
    vPrev = ''
    rVarList.each{ varR ->
        if(varR.value != null && vPrev != varR.value.key) {
            vPrev = varR.value.key
            varDispList += "<h4><u>${varR.value.key}</u> = ${varR.value.value}</h4>"
        }
        varDispList += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${varR.pName}<br />"   
    }  
    varDispList += "</p>"
    
    String csv = '"App Number","App Name","Var Name","Var Value"\n'
    varList.each{
        //log.debug "${it.properties}"
        csv+= "\"${it.key}\",\"${it.pName}\",\"${it.value.key}\",\"${it.value.value}\"\n"
    }
    
	return [varDispList, csv]

}

def getVarsJ(wcData, childApps){
	varList = []
    varList2 = []
	childApps.each{ ca ->
        //log.debug "$ca"
		jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${ca.key}")
        //log.debug "${jData}"
        if("${ca.key}" == "${state.wcID}") {
	        jData.appState.each { aS ->
            //log.debug "${aS.name}"
	            if(aS.name == 'vars'){
    	            //log.debug "${aS.value}"
        	        aS.value.each {
            	        //key:value
                	    //log.debug "${ca.key} ${state.wcID} ${"${ca.key}" == "${state.wcID}"}"
                    	if(aS.value != null){
                            	if(it.value.v == null || it.value.v == 'null')
                            		it.value.v = '[Dynamic Value]'
        	                    vMap = [key:it.key, value:it.value.v]
            	                tMap = [key:ca.key, value:vMap, pName:ca.value]
                	    		varList.add(tMap)
                    	        varList2.add(tMap)
    	                }
        	        }
            	}
            }
        }

        String sChunk = ''
        jData.appSettings.sort{ it.name }.each {s ->
        	if(s.name.contains('chunk')){
               // log.debug "${s.name}"
            	sChunk += new String(s.value.decodeBase64(), 'UTF-8')
	        }
    	}

        if(sChunk) {

        	def jSlurp = new JsonSlurper()
        	jChunk = jSlurp.parseText(sChunk)
        	jChunk.each { chunk ->
            	chunk.each {
                    //log.debug "$it.properties"
                    if(it.key == 'v') {
                        it.value.each { vars ->
                            //log.debug "$it"
                            if("${ca.key}" != "${state.wcID}") {
                                if(vars?.v?.c == null || vars?.v?.c == 'null')
                            		tVal =  '[Dynamic Value]'
                                else 
                                    tVal = vars.v.c
                                kMap = [key:vars.n, value:tVal]
                    			tMap = [key:ca.key, value:kMap, pName:ca.value]
                                varList.add(tMap)  
                            }
                        }
                    }
                
                    if(it.key.toString().contains("@")){
                        if(it.value?.v == null || it.value?.v == 'null')
                        	it.value.v = '[Dynamic Value]'
                        kMap = [key:it.key, value:it.value?.v]
                        tMap = [key:ca.key, value:kMap, pName:ca.value]
                    	varList.add(tMap)
                    }
                	if(it.value.toString().contains("@")) {
                		sPos = it.value.toString().indexOf("@")
                		ePos = it.value.toString().indexOf("]",sPos)
            			vName = it.value.toString().substring(sPos,ePos)
                        tVal = 'TBD'
                        if(vName.contains("@@")){
                            gVar = getGlobalVar(vName.substring(2,))
                            tVal =  gVar.value                          
                        } else {
                            varList2.each{vl2 ->
                                if(vName == vl2.value.key)
                                	tVal = vl2.value.value
                            }
                            if(tVal == null || tVal == 'null')
                            	tVal = '[Dynamic Value]'
                        }
                        vMap = [key:vName, value:tVal]
                    	tMap = [key:ca.key, value:vMap, pName:ca.value]
                    	varList.add(tMap)
                	}
            	}
        	}
        }

	}
    
    return varList
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
				wrkMap =[key:"${it.data.id}",value:"${it.data.name}"]
                wrkList.add(wrkMap)              
                it.children.each{
                    if(it.data.type == 'webCoRE Piston'){
                        wrkMap =[key:"${it.data.id}",value:"${it.data.name}"]
                        wrkList.add(wrkMap)
                    }
                }
            }
        }
        
        return wrkList.sort { it.value }
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
            state.getVar = true
            break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
