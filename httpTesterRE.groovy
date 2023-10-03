/*
 * HTTP Get/Post Test - Rule Edition
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
 *    2023-10-03  thebearmay    Split off from HttpGetPost Tester 
 *
 */
import groovy.transform.Field
import groovy.json.JsonOutput


static String version()	{  return '0.0.1'  }

metadata {
    definition (
		name: "HttpGetPost Tester Rule Edition", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/httpTesterRE.groovy"
	) {
        capability "Actuator"
       
        attribute "respReturn", "string"
        
        
        command "postUrl", [[name:"url*", type:"STRING", description:"URL to send POST", title:"URL"],
                         [name:"contentT", type:"STRING", description:"Content-Type", title:"Content-Type"],
                         [name:"contentR", type:"STRING", description:"Content Type Requested", title:"Content Type Requested"],
                         [name:"headerBlock", type:"STRING", description:"Headers, comma separated", title:"Headers"],
                         [name:"bodyText", type:"STRING", description:"Body Text (usually JSON)", title:"Body Text"]
                         ]
                          
        command "getUrl", [[name:"url*", type:"STRING", description:"URL to send POST", title:"URL"],
                        [name:"contentT", type:"STRING", description:"Content-Type", title:"Content-Type"],
                        [name:"contentR", type:"STRING", description:"Content Type Requested", title:"Content Type Requested"],
                        [name:"headerBlock", type:"STRING", description:"Headers, comma separated", title:"Headers"],
                        [name:"bodyText", type:"STRING", description:"Body Text (usually JSON)", title:"Body Text"]
                        ]
        command "test"
    
    }   
}

preferences {
    input("errMsg", "hidden", title:"<b>Version Information</b>",description:"<span style='font-weight:bold;color:blue;'>v${version()}</span>")
    input "tParser", "bool", title: "Text Parser", submitOnChange: true, defaultValue:true
    input("debugEnabled", "bool", title: "Enable debug logging?")
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

def test(){
    log.debug "Test $debugEnabled"
}

def postUrl(url,contentT='text/plain',contentR='',headerBlock='',bodyText=''){
    if(debugEnabled)
        log.debug "POST,$url,$contentT,$contentR,$headerBlock,$bodyText"
    send("POST",url,contentT,contentR,headerBlock,bodyText)
}

def getUrl(url,contentT='text/plain',contentR='',headerBlock='',bodyText=''){
    if(debugEnabled)
        log.debug "GET,$url,$contentT,$contentR,$headerBlock,$bodyText"
    send("GET",url,contentT,contentR,headerBlock,bodyText)
}

def send(postGet, path,contentT,contentR,headerBlock,bodyText){
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
    if(headerBlock != null && headerBlock != ""){
        headerList = headerBlock.split(",") 
        headerList.each{ 
            itList = it.split(":")
            params.headers.put("${itList[0]}", "${itList[1]}")
        }
    }
    if(bodyText) params.put("body", "$bodyText")
    
    if(debugEnabled) log.debug "$postGet $params"
    if(postGet == "POST")
        asynchttpPost("sendHandler", params)
    else
        asynchttpGet("sendHandler", params)
    
}

def sendHandler(resp, data) {
    if(debugEnabled) log.debug "Response: ${resp.properties}"
    
    def xmlStr="""<Response Status="OK">
	<Item Name="Guid">{adasdasdasdae-3421-dase-1dd}</Item>
	<Item Name="Version">324234</Item>
</Response>"""    
    
    try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    strWork = resp.data.toString()
    	    if(debugEnable) log.debug "$strWork<br>Type:${resp.headers["Content-Type"]}"
            if(resp.headers["Content-Type"].contains("xml")) {
            //strWork = xmlStr
                xmlNodes= new XmlParser().parseText(strWork)//.children()

                mapVal=[:]
                if(xmlNodes.attributes())
                    mapVal+=xmlNodes.attributes()
                xmlNodes.each {
                    aMap=[:]
                    cMap=[:]
                    it.children().each {
                        if(!it.class.toString().contains('String')) 
                            cMap.put(it?.name(),it.value().toString().replace("[","").replace("]",""))
                        else 
                            cMap.put("value",it.value.toString().replace("[","").replace("]",""))                  
                    }
                    if(it.attributes()){
                        sWork = it?.name()+it.attributes().toString().replace("[","_").replace(":","_").replace("]","")
                        mapVal.put(sWork,cMap)
                    } else 
                    if(it?.name()) 
                        mapVal.put(it?.name(),cMap)
                    else
                        mapVal+=cMap                        
                    }

                if (debugEnabled) log.debug "${mapVal}"
                strWork = JsonOutput.toJson(mapVal)
            }
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
