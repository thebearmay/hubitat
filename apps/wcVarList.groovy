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

static String version()	{  return '0.0.3'  }
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
            if(state.getVar){
                state.getVar = false
                paragraph "<h3><b><u>Piston to Variable List</u></b></h3><p>${getVars()}</p>"
                if(varList.size() < 1) paragraph "No Variables Found"
            }


        }
    }
}


String getVars(){
    varDispList = ""
    childApps = getPistonList()
    jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${state.wcID}")
    varList = getVarsJ(jData, childApps)
    vNamePrev = ''
    //log.debug "$varList"
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
    
    return varDispList

}

def getVarsJ(wcData, childApps){
	varList = []
    varList2 = []
	childApps.each{ ca ->
        //log.debug "$ca"
		jData=readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/${ca.key}")
        //log.debug "${jData}"
        jData.appState.each { aS ->
            //log.debug "${aS.name}"
            if(aS.name == 'vars'){
                //log.debug "${aS.value}"
                aS.value.each {
                    //key:value
                    //log.debug "${ca.key} ${state.wcID} ${"${ca.key}" == "${state.wcID}"}"
                    if(aS.value != null){
                        if("${ca.key}" != "${state.wcID}") {
                    		tMap = [key:ca.key, value:it, pName:ca.value]
                    		varList.add(tMap)
                        } else {
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
        jData.appSettings.each {s ->
            if(s.name.contains('chunk')){
                sChunk+= new String(s.value.decodeBase64(), 'UTF-8')
            }
        }

        if(aChunk) {
          
        	def jSlurp = new JsonSlurper()
        	jChunk = jSlurp.parseText(sChunk)
        	jChunk.each { chunk ->
            	chunk.each {
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
