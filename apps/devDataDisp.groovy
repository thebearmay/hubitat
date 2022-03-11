/*
 * Device Data Display
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
 */
import java.text.SimpleDateFormat
import java.net.URLEncoder
static String version()	{  return '1.2.1'  }


definition (
	name: 			"Device Data Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Display selected items out of the device data area for one or more devices.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/devDataDisp.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "deviceData"
   page name: "jsonDown"
   page name: "csvDown"

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
	    	section("Main")
		    {
              section("Selection Criteria", hideable: true, hidden: true){
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null) {
                    dataList = buildDataList()
                    input "varList", "enum", title: "Select data items to display", options: dataList, multiple: true, required: false, submitOnChange: true
                    stateList = buildStateList()
                    input "stList", "enum", title: "Select states to display", options: stateList, multiple: true, required: false, submitOnChange: true
                }
              }
              section(""){
                  if(varList !=null || stateList!= null) {
                    href "deviceData", title: "Display Data", required: false
                    href "jsonDown", title: "Download JSON Data", required: false
                    href "csvDown", title: "Download CSV Data", required: false
                  }
              }
		    }
	    } else {
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
          qryDevice.each{ x->
              paragraph "<p style='font-weight:bold;text-decoration:underline'>$x.displayName</p>"
              varOut = ""
              varList.each {
                  if(x.properties.data["$it"]) varOut+= "$it: ${x.properties.data["$it"]}<br>"
              }
              stList.each { s->
                 x.properties.currentStates.each {
                    if(it.name == s) varOut+= "$s: ${it.value}<br>"
                 }
              }
              paragraph varOut
          }
       }
    }
}

def jsonDown(){
    dynamicPage (name: "jsonDown", title: "", install: false, uninstall: false) {
	  section("<b><u>JSON Data</u></b>"){
        jData = "["
        qryDevice.each{ x->
            jData += "{\"$x.displayName\": {"
            varList.each {
                if(x.properties.data["$it"]) 
                    jData += "\"$it\": \"${x.properties.data["$it"]}\","
            }
            stList.each { s->
                x.properties.currentStates.each {
                    if(it.name == s) 
                    jData += "\"$s\": \"$it.value\","
                    //varOut+= "$s: ${it.value}<br>"
                }
             }
            jData = jData.substring(0,jData.length()-1)
            jData += "}},"
      }
          jData = jData.substring(0,jData.length()-1)
          jData += "]"
          oData = "<script type='text/javascript'>function download() {var a = document.body.appendChild( document.createElement('a') );a.download = 'deviceData.json';a.href = 'data:text/json,' + encodeURIComponent(document.getElementById('jData').innerHTML);a.click();}</script>"
          oData +="<button onclick='download()'>Download JSON</button><div id='jData'><hr />$jData<hr /></div>"
          paragraph oData  
          input "lFileName", "string", title:"Hub File Name (optional)", submitOnChange: true
          if(lFileName != null) {
              atomicState.jData = jData
              input "wFile", "button", title:"Write to Hub File", submitOnChange:true
          }
      }        
        section("Hub Security", hideable: true, hidden: true){
            input "username", "string", title:"Hub User ID", submitOnChange: true
            input "password", "string", title:"Hub Password", submitOnChange: true
        }

  }
}

def csvDown(){
    dynamicPage (name: "csvDown", title: "", install: false, uninstall: false) {
      section("CSV Data"){
        jData=""
        qryDevice.each{ x->
            jData += "\"$x.displayName\"\n"
            varList.each {
                if(x.properties.data["$it"]) 
                    jData += ",\"$it\",\"${x.properties.data["$it"]}\"\n"
            }
            stList.each { s->
                x.properties.currentStates.each {
                    if(it.name == s) 
                    jData +=",\"$s\",\"$it.value\"\n"
                }
             }            
      }
      oData = "<script type='text/javascript'>function download() {var a = document.body.appendChild( document.createElement('a') );a.download = 'deviceData.csv';a.href = 'data:text/plain,' + encodeURIComponent(document.getElementById('jData').innerHTML);a.click();}</script>"
      oData +="<button onclick='download()'>Download CSV</button><div id='jData'>$jData</div>"
      paragraph oData    
    }
  }
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
//			log.debug resp.data?.text
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
				'Content-Type': "multipart/form-data; boundary=$encodedString"
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
