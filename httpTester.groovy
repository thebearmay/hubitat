/*
 * HTTP Get/Post Test
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
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-05-21  thebearmay	 Original version 0.1.0
 *    2022-10-31  thebearmay     add more options for headers, etc.
 *    2023-09-27                 Add text=parser option
 *    2023-09-28                 XML to JSON
 *
 */
import groovy.transform.Field
import groovy.json.JsonOutput


static String version()	{  return '0.2.2'  }

metadata {
    definition (
		name: "HttpGetPost Tester", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/httpTester.groovy"
	) {
        capability "Actuator"
       
        attribute "respReturn", "string"
        
        
        command "send"
    
    }   
}

preferences {
    input "postGet", "enum", title: "POST or GET", options:["POST","GET"], submitOnChange: true
    input "path", "string", title: "Server Path (including port)", submitOnChange: true
    input "contentT", "string", title: "Content Type", required:true, submitOnChange: true
    input "contentR", "string", title: "Request Content Type", submitOnChange: true
    input "headerBlock", "string", title: "Headers, comma separated", submitOnChange: true
    input "bodyText", "string", title: "Body text (usually JSON)"
    input "tParser", "bool", title: "Text Parser", submitOnChange: true, defaultValue:true
    input("debugEnable", "bool", title: "Enable debug logging?")
        input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
}


def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    updateAttr("getReturn"," ")

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}


def send(){
    if(security) {
        httpPost(
            [
            uri: "http://127.0.0.1:8080",
            path: "/login",
            query: [ loginRedirect: "/" ],
            body: [
                username: username,
                password: password,
                submit: "Login"
                ]
            ]
        ) { resp -> cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0) }
    }
    sendEvent(name:"respReturn",value:".")
    params = [
        uri: path,
        contentType: "$contentT",
        requestContentType: contentR ?"$contentR" : "*/*",
        textParser: tParser ? "true":"$tParser",
        headers: [:]
    ]
    if(cookie) params.headers.put("Cookie", cookie)
    if(headerBlock != null){
        headerList = headerBlock.split(",") 
        headerList.each{ 
            itList = it.split(":")
            params.headers.put("${itList[0]}", "${itList[1]}")
        }
    }
    if(bodyText) params.put("body", "$bodyText")
    
    log.debug "$postGet $params"
    if(postGet == "POST")
        asynchttpPost("sendHandler", params)
    else
        asynchttpGet("sendHandler", params)
    
}

def sendHandler(resp, data) {
    log.debug "Response: ${resp.properties}"
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    strWork = resp.data.toString()
    	    if(debugEnable)
                log.debug "$strWork<br>Type:${resp.headers["Content-Type"]}"
           if(resp.headers["Content-Type"].contains("xml")) {
                strWork = JsonOutput.toJson(convertToMap(new XmlSlurper().parseText(strWork)))
            }
	        sendEvent(name:"respReturn",value:strWork)
  	    }
    } catch(Exception ex) { 
        log.error "$ex"
    } 

}

def convertToMap(nodes) {
    nodes.children().collectEntries { 
        [ it.name(), it.childNodes() ? convertToMap(it) : it.text() ] 
    }
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
