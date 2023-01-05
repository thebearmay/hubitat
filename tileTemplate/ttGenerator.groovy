/* Tile Template Generator
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
 *     Date              Who           Description
 *    ===========       ===========   =============================================================
 *    2022-08-26        thebearmay    add a check for saveAs not null before displaying save button
 *    2022-08-30        thebearmay    add option to generate multi-device templates
 *    2023-01-05        thebearmay    check for invalid characters in save file name
 */

static String version()	{  return '0.0.4'  }


definition (
	name: 			"Tile Template Generator", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Allows for the creation of a custom tile template for any device.",
	category: 		"Utility",
	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/tileTemplate/ttGenerator.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "attrSelect"
    page name: "attrRepl"
    page name: "templatePreview"
}

void installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main") {
                state.saveReq = false
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                
                if (qryDevice){        
                        href "attrSelect", title: "Select Attributes", required: true
                        href "attrRepl", title: "Alternate Text for Attributes", required: false
                        href "templatePreview", title: "Show Template", required: false
                }
                input "clearSettings", "button", title: "Clear previous settings"
                if(state?.clearAll == true) {
                    settings.each {
                        if(it.key != 'isInstalled') {
                            app.removeSetting("${it.key}")
                        }
                    }
                    state.clearAll = false
                }
             }
             section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
             }  
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def attrSelect(){
    dynamicPage (name: "attrSelect", title: "Attribute Selection", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          //log.debug "$qryDevice ${qryDevice.size()}"
          def attrList=[]
          qryDevice.each { dev ->
              dev.supportedAttributes.each{
                  if(qryDevice.size() == 1){
                      attrList.add(it.toString())
                  }else{                     
                      attrList.add("${dev.id}:$it")
                  }
              }
          }
          sortedList=attrList.sort()
          sortedList.each{
            input "$it", "bool", title: "$it", required: false, defaultValue: false
          }

          paragraph "<p>$strWork</p>"
       }
    }
}

def attrRepl(){
    dynamicPage (name: "attrRepl", title: "Alternate Attribute Descriptions", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          def attrList=[]

          qryDevice.each{ dev->
            dev.supportedAttributes.each{
                if(qryDevice.size() == 1) {
                    if(settings["$it"]) attrList.add(it.toString())
                } else {
                    if(settings["${dev.id}:$it"]) attrList.add("${dev.id}:$it")
                }
            }
          }          
          sortedList=attrList.sort()
          sortedList.each{
            input "repl$it", "string", title: "$it", required: false
          }

          paragraph "<p>$strWork</p>"          
      }
    }
}

def templatePreview(){
    dynamicPage (name: "templatePreview", title: "Template Preview", install: false, uninstall: false) {
	  section(""){
          retMap = buildTable()
          htmlWork = retMap.preview
          paragraph "<div style='overflow:auto'>$htmlWork<p style='font-size:xx-small'>${htmlWork.size()} characters</p></div>"
          paragraph "<textarea disabled='true' cols='70' rows='15'>${retMap.template}</textarea>"
          input "saveAs", "text", title: "Enter Name for Template", multiple: false, required: false, submitOnChange: true, width:4
          if(saveAs != null) {
              app.updateSetting("saveAs",[value:toCamelCase(saveAs),type:"text"])
              input "saveTemplate", "button", title:"Save Template"
              if(state.saveReq == true) {
                  writeFile("$saveAs","${retMap.template}")
                  state.saveReq = false
              }
          }
      }
    }
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


HashMap buildTable() {
    String htmlWork = '<table>\n'
    templateWork = htmlWork
    def attrList=[]
    qryDevice.each{ dev->
        dev.supportedAttributes.each{
            if(qryDevice.size() == 1) {
                if(settings["$it"]) attrList.add(it.toString())
            } else {
                if(settings["${dev.id}:$it"]) attrList.add("${dev.id}:$it")
            }
        }
    }
    sortedList=attrList.sort()
       
    sortedList.each{
        replacement = "repl$it"
        if(settings["$replacement"]) {
            htmlWork+="<tr><th>${settings[replacement]}</th>"
            templateWork += "<tr><th>${settings[replacement]}</th>"
        } else {
            htmlWork+="<tr><th>$it</th>"
            templateWork += "<tr><th>$it</th>"
        }
        if(qryDevice.size() == 1){
            dev = qryDevice[0]
            aVal = dev.currentValue("$it",true)
            String attrUnit = dev.currentState("$it")?.unit
            if (attrUnit != null) aVal+=" $attrUnit"            
            htmlWork += "<td>$aVal</td></tr>"
        }else {
            //log.debug "$it"
            devId = it.substring(0,it.indexOf(":")).toLong()
            qryDevice.each{ qDev->
                //log.debug "${qDev.id} $devId"
                if(qDev.id.toLong() == devId) dev=qDev
            }
            vName = it.substring(it.indexOf(":")+1)
            aVal = dev.currentValue("$vName",true)
            String attrUnit = dev.currentState("$vName")?.unit
            if (attrUnit != null) aVal+=" $attrUnit"            
            htmlWork += "<td>$aVal</td></tr>"
                
        }
        templateWork += "<td><%$it%></td></tr>\n"
    }
    htmlWork+='</table>'
    templateWork+='</table>\n'
    return [preview:htmlWork, template:templateWork]
}


@SuppressWarnings('unused')
Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString(); 
    
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString;text/html; charset=utf-8"
			],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}

def appButtonHandler(btn) {
    switch(btn) {
        case "clearSettings":
            state.clearAll = true
            break
        case "saveTemplate":
            if(saveAs == null) break
            state.saveReq = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}

void intialize() {

}
