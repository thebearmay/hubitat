/*
 * Device Data Item Display
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
 *    Date        Who           What
 *    ----        ---           ----
 *    03Mar2022   thebearmay    JSON/CSV download
 *    11Mar2022   thebearmay    Add State Data option
 *                              Hub WriteFile option for JSON
 *    12Mar2022   thebearmay    Make ID the key for JSON
 *    01Apr2022   thebearmay    add optional tile device
 *    05Apr2022   thebearmay    split html > 1024
 *    06Apr2022   thebearmay    allow multiple instances via rename
 *    20Jun2022   thebearmay    embedded section correction
 *    10Jul2022   jtp10181      CSV changes
 *	  10Jul2025	  thebearmay	Additional fields
 *	  11Jul2025					Add Driver Name
 *	  08Dec2025	  				Add a CSV/JSON display/download endpoints
*/

import java.text.SimpleDateFormat
import java.net.URLEncoder
static String version()	{  return '1.3.7'  }


definition (
	name: 			"Device Data Item Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Display selected items out of the device data area for one or more devices.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devDataDisp.groovy",
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "deviceData"
   page name: "jsonDown"
   page name: "csvDown"

}

mappings {
    path("/csvDisp") {
        action: [GET: "csvDisp"]
    }
	path("/jsonDisp") {
        action: [GET: "jsonDisp"]
    }
    path("/csvDown") {
        action: [GET: "csvEpDown"]
    }
	path("/jsonDown") {
        action: [GET: "jsonEpDown"]
    }
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
      	if (app.getInstallationState() == 'COMPLETE') {   
//	    	section("Main")
//		    {
              section("Selection Criteria", hideable: true, hidden: true){
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null) {
                    dataList = buildDataList()
                    input "varList", "enum", title: "Select data items to display", options: dataList, multiple: true, required: false, submitOnChange: true
                    stateList = buildStateList()
                    input "stList", "enum", title: "Select states to display", options: stateList, multiple: true, required: false, submitOnChange: true
                    input "otherElem", "bool", title: "Add Room, DNI, etc.", submitOnChange:true
                }
              }
              section(""){
                  if(varList !=null || stateList!= null) {
                    href "deviceData", title: "Display Data", required: false
                    href "jsonDown", title: "Download JSON Data", required: false
                    href "csvDown", title: "Download CSV Data", required: false
                  }
              }
              section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
              }
			  section("Endpoint Information", hideable: true, hidden: true){
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
                paragraph "<b>Cloud Request Formats: </b><br>${getFullApiServerUrl()}/csvDisp?access_token=${state.accessToken}<br>${getFullApiServerUrl()}/jsonDisp?access_token=${state.accessToken}<br>${getFullApiServerUrl()}/csvDown?access_token=${state.accessToken}<br>${getFullApiServerUrl()}/jsonDown?access_token=${state.accessToken}"
                paragraph "<b>Local Request Formats: </b><br>${getFullLocalApiServerUrl()}/csvDisp?access_token=${state.accessToken}<br>${getFullLocalApiServerUrl()}/jsonDisp?access_token=${state.accessToken}<br>${getFullLocalApiServerUrl()}/csvDown?access_token=${state.accessToken}<br>${getFullLocalApiServerUrl()}/jsonDown?access_token=${state.accessToken}"
              }
//		    }
	    } else {
              section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
              }   
            section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def buildDataList(){
    List dataList = []
    qryDevice.each {
        it.properties.data.each {
            dataList.add(it.key)
        }
    }
    dataList = dataList.sort().unique()
    return dataList
}

def buildStateList() {
    List stateList = []
    qryDevice.each {
        it.properties.currentStates.each {
            stateList.add(it.name)
        }
    }
    stateList = stateList.sort().unique()
    return stateList    
    
}

def deviceData(){
    dynamicPage (name: "deviceData", title: "", install: false, uninstall: false) {
	  section("Device Data"){
          qryDevice.sort({m1, m2 -> m1.displayName <=> m2.displayName})
          qryDevice.each{ x->
              paragraph "<span style='font-weight:bold;text-decoration:underline'>$x.displayName</span> ($x.name)"
              varOut = ""
              varList.each {
                  if(x.properties.data["$it"]) varOut+= "$it: ${x.properties.data["$it"]}<br>"
              }
              stList.each { s->
                 x.properties.currentStates.each {
                    if(it.name == s) varOut+= "$s: ${it.value}<br>"
                 }
              }
			  if(otherElem) {
				othData = getOthData(x)
				othData.each { o ->
					varOut+= "$o.key: $o.value<br>"
				}
        	  }              
              paragraph varOut
            }
            input "createChild", "bool", title: "Create a child device for dashboard use", required: false, submitOnChange: true
            if (createChild) {
                if(getChildDevice("ddd${app.id}-01"))
                     return
                childDev = createChildDev()
                refreshDevice()
                subscribe(qryDevice, "refreshDevice", [filterEvents:true])
            } else if(getChildDevice("ddd${app.id}-01")){
                unsubscribe()
                deleteChildDevice("ddd${app.id}-01")
            }          
       }
    }
}

def refreshDevice(evt=null){
	inx = 0
	html = []
    qryDevice.sort({m1, m2 -> m1.displayName <=> m2.displayName})
    html[inx] = "<div id='devDataDisp'>"
    qryDevice.each{ x->
        varOut= "<p style='font-weight:bold;text-decoration:underline'>$x.displayName</p>"
        varList.each {
            if(x.properties.data["$it"]) varOut+= "<p>$it: ${x.properties.data["$it"]}</p>"
        }
        stList.each { s->
            x.properties.currentStates.each {
                if(it.name == s) varOut+= "<p>$s: ${it.value}</p>"
            }
        }
		if(otherElem) {
			othData = getOthData(x)
			othData.each { o ->
				varOut+= "<p>$o.key: $o.value</p>"
			}
        }
		if(varOut.length() + html[inx].length() + 6 > 1024){
			html[inx]+="</div>"
			inx++
			html[inx] = "<div id='devDataDisp'>"
		}
		html[inx]+=varOut
    }
    html[inx]+="</div>"
	
	chd = getChildDevice("ddd${app.id}-01")
	chd.sendEvent(name:"html",value:html[0].toString())
    if(debugEnabled) log.debug "inx = $inx"
	if(inx > 0) {
		for (int i = 1;i <= inx; i++){
            if(debugEnabled) log.debug "i=$i"
            chd.sendEvent(name:"html${i+1}",value:html[i].toString())
		}
	}
		
}

def createChildDev(){
    if(getChildDevice("ddd${app.id}-01"))
        return
    cd = addChildDevice("thebearmay", "Generic HTML Device", "ddd${app.id}-01", [name:"${app.label} Tile Device"])
    return cd
}

def getOthData(dev) {
    String drName
    httpGet([uri:"http://127.0.0.1:8080/device/fullJson/${dev.id}"]) { resp ->
        drName = "${resp.data.device.deviceTypeName}"
    }
    return [driverType:"${dev.driverType}",driverName:"${drName}",dni:"${dev.deviceNetworkId}",zgId:"${dev.zigbeeId}",roomName:"${dev.roomName}",controllerType:"${dev.controllerType}"]
}

def jsonDown(){
    dynamicPage (name: "jsonDown", title: "", install: false, uninstall: false) {
	  section("<b><u>JSON Data</u></b>"){
          jData = buildJson()
          oData = "<script type='text/javascript'>function download() {var a = document.body.appendChild( document.createElement('a') );a.download = 'deviceData.json';a.href = 'data:text/json,' + encodeURIComponent(document.getElementById('jData').innerHTML);a.click();}</script>"
          oData +="<button onclick='download()'>Download JSON</button><hr /><div id='jData'>$jData</div><hr />"
          paragraph oData  
          input "lFileName", "string", title:"Hub File Name (optional)", submitOnChange: true
          if(lFileName != null) {
              atomicState.jData = jData
              input "wFile", "button", title:"Write to Hub File", submitOnChange:true
          }
      }        

  }
}

def buildJson(){
	jData = "["
	qryDevice.sort({m1, m2 -> m1.displayName <=> m2.displayName})
	qryDevice.each{ x->
		jData +=  "{\"$x.id\": {\"displayName\": \"$x.displayName\","
		varList.each {
			if(x.properties.data["$it"]) 
				jData += "\"$it\": \"${x.properties.data["$it"]}\","
		}
		stList.each { s->
			x.properties.currentStates.each {
				if(it.name == s) 
                    jData += "\"$s\": \"$it.value\","
            }
        }
        if(otherElem) {
            othData = getOthData(x)
            othData.each { o ->
                jData += "\"$o.key\": \"$o.value\","
            }
        }
        jData = jData.substring(0,jData.length()-1)
        jData += "}},"
	}
	jData = jData.substring(0,jData.length()-1)
	jData += "]"    
}

def jsonDisp(){
	contentBlock = [
		contentType: "application/json",
		gzipContent: true,
		data: buildJson(),
		status:200
	]
    if(debugEnabled)
        log.debug "$contentBlock"
    render (contentBlock)
}

def jsonEpDown(){
	jData = buildJson()//.getBytes("UTF-8")
    tNow=new Date()
    fName = "${toCamelCase(location.hub.name)}Json${new Date().getTime()}.json"

    contentBlock = [
        contentType: "application/octet-stream", 
        contentLength:jData.size(),
        contentDisposition: "attachment; filename=$fName",
		gzipContent: true,
        data:jData,
		status:200
    ]
    //log.debug "$contentBlock"
    render(contentBlock)
}

def csvDown(){
    dynamicPage (name: "csvDown", title: "", install: false, uninstall: false) {
      section("CSV Data"){
	  jData = buildCsv()
      oData = "<script type='text/javascript'>function download() {var a = document.body.appendChild( document.createElement('a') );a.download = 'deviceData.csv';a.href = 'data:text/plain,' + encodeURIComponent(document.getElementById('jData').innerHTML);a.click();}</script>"
      oData +="<button onclick='download()'>Download CSV</button><hr /><div id='jData'>$jData</div><hr />"
      paragraph oData    
    }
  }
}

def csvDisp(){
	contentBlock = [
		contentType: "text/csv",
		gzipContent: true,
		data: buildCsv(),//.replace("\n","<br>"),
		status:200
	]
    if(debugEnabled)
        log.debug "$contentBlock"
    render (contentBlock)
}

def csvEpDown(){
	jData = buildCsv()//.getBytes("UTF-8")
    fName = "${toCamelCase(location.hub.name)}Csv${new Date().getTime()}.csv"

    contentBlock = [
        contentType: "application/octet-stream", 
        contentLength:jData.size(),
        contentDisposition: "attachment; filename=$fName",
		gzipContent: true,
        data:jData,
		status:200
    ]
    
    render(contentBlock)
}

def buildCsv(){
	jData="\"id\",\"displayName\",\"name\""
    varList.each { jData += ",\"$it\"" }
	stList.each { jData += ",\"$it\"" }
	if(otherElem) {
		othData = getOthData(qryDevice[0])
		othData.each {jData += ",\"$it.key\"" } 
	}
	jData += "\n"  

	qryDevice.sort({m1, m2 -> m1.displayName <=> m2.displayName})
	qryDevice.each{ x->
		jData += "\"$x.id\",\"$x.displayName\",\"$x.name\""
		varList.each {
			jData += (x.properties.data["$it"] ? ",\"${x.properties.data["$it"]}\"" : ",")
		}
		stList.each { s->
			x.properties.currentStates.each {
				jData += (it.name == s ? ",\"$it.value\"" : ",")
			}
		}
		if(otherElem) {
			othData = getOthData(x)
			othData.each { o ->
				jData += ",\"$o.value\"" 
            }
        }
		jData += "\n"
	}
    return jData
}

def appButtonHandler(btn) {
    switch(btn) {
        case "wFile":
            writeFile("$lFileName", "${atomicState.jData}")
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
      }
}

def intialize() {

}

void uninstalled(){
    if(getChildDevice("ddd${app.id}-01")){
        unsubscribe()
        deleteChildDevice("ddd${app.id}-01")
    }          
}


Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();    
	uploadHubFile(fName,fData.getBytes("UTF-8"))
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
