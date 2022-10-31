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
 *
 */

static String version()	{  return '0.2.0'  }


metadata {
    definition (
		name: "HttpGetPost Tester", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:""
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
    params = [
        uri: path,
        contentType: "$contentT",
        requestContentType: contentR ?"$contentR" : "*/*",
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
    		//if(debugEnable)
            log.debug strWork
	        sendEvent(name:"respReturn",value:strWork)
  	    }
    } catch(Exception ex) { 
        log.error "$ex"
    } 

}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
