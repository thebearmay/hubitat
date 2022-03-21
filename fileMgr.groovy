/*
 * File Manager Device
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
 *    Date         Who           What
 *    ----         ---           ----
 *    21Mar2022    thebearmay    take the text file methods and place them into a simple to use device driver   
*/


import java.net.URLEncoder

import groovy.json.JsonSlurper
import groovy.transform.Field




@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "File Manager Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        description: "Device (or child device) that allows string data to be save and retrieved from HE Local File Storage",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/fileMgr.groovy"
    ) {
        
      
        capability "Actuator"
 
        command "writeFile",[[name:"fileName", type:"STRING", description:"File Manager Destination Name"],[name:"writeString", type:"STRING", description:"String to Store"]]
        command "xferFile",[[name:"inputURL", type:"STRING", description:"Input URL"],[name:"fileName", type:"STRING", description:"File Manager Destination Name"]]
        command "appendFile",[[name:"fileName", type:"STRING", description:"File Manager Destination Name"],[name:"appendString", type:"STRING", description:"String to Append"]]
        command "readFile",[[name:"fileName", type:"STRING",description:"Local File to Read"]]
        command "fileExists",[[name:"fileName", type:"STRING",description:"File to Look for"]]               



    }   
}

preferences {
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("logResponses", "bool", title: "Log Responses to Commands")
 
}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
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


Boolean fileExists(fName){

    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                if(logResponses) log.info "File Exist: true"
                return true;
            } else {
                if(logResponses) log.info "File Exist: true"
                return false
            }
        }
    } catch (exception){
        if (exception.message == "Not Found"){
            if(logResponses) log.info "File Exist: false"
        } else {
            log.error("Find file $fName) :: Connection Exception: ${exception.message}")
        }
        return false;
    }

}

String readFile(fName){
    if(security) cookie = securityLogin().cookie
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
            ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {       
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               }
               if(logResponses) log.info "File Read Data: $delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null;
    }
}



Boolean appendFile(fName,newData){
    try {
        fileData = (String) readFile(fName)
        if(fileData.length()>0) 
            fileData = fileData.substring(0,fileData.length()-1)
        else fileData = " "
        return writeFile(fName,fileData+newData)
    } catch (exception){
        if (exception.message == "Not Found"){
            writeStatus =  writeFile(fName, newData)
            if(logResponses) log.info "Append Status: $writeStatus"
            return writeStatus
        } else {
            log.error("Append $fName Exception: ${exception}")
            return false
        }
    }
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

Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    if(logResponses) log.info "File xFer Status: $retStat"
    return retStat
}

String readExtFile(fName){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               if(logResponses) log.info "Read External File result: delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
