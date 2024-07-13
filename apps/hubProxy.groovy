/*
 *
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
 *    12Jul2024    thebearmay    Add alternate read for extended characters
 *    13Jul2024                  charset = UTF-8 for HTML
 */
    


static String version()	{  return '0.0.1'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field


definition (
	name: 			"Hub Proxy Server", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Hub Proxy Image Server",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubProxy.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"

}
mappings {
    path("/getImage") {
        action: [POST: "serveImage",
                 GET: "serveImage"]
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
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
         section(name:"lhi",title:"Local Hub Information", hideablel: true, hidden: false){
             
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
                paragraph "<b>Cloud Request Format: </b><br> ${getFullApiServerUrl()}/getImage?access_token=${state.accessToken}&fName=yourFileName.fileExtension"
        }
        section(name:"opt",title:"Options", hideablel: true, hidden: false){
            input("debugEnabled","bool",title:"Enable Debug Logging", width:4)
            input("altRead","bool",title:"Use alternate read for text files", width:4)
        }

	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}


def toCamelCase(init) {
    if (init == null)
        return null;
    init = init.replaceAll("[^a-zA-Z0-9]+","")
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
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}


def jsonResponse(retMap){     
    return JsonOutput.toJson(retMap)
}


def serveImage(){

    if(debugEnabled)
        log.debug "serveImage called: $params source: ${request.requestSource}"

    fileExt = params.fName.substring(params.fName.lastIndexOf(".")+1)
    if(!(fileExt in imageType)  && altRead) {
        imageFile = readFile("${params.fName}")
    } else
        imageFile = downloadHubFile("${params.fName}")        
    if(debugEnabled) log.debug fileExt
    switch (fileExt) {
        case 'gif':
            fType = 'image/gif'
            break     
        case 'htm':
        case 'html':
            fType = 'text/html; charset=UTF-8'
            break
        case 'js':
            fType = 'text/javascript'
            break
        case 'json':
            fType = 'application/json'
            break
        case 'jpeg':
        case 'jpg':
            fType = 'image/jpg'
            break  
        case 'png':
            fType = 'image/png'
            break           
        case 'svg':
            fType = 'image/svg+xml'
            break
        case 'txt':
            fType = 'text/plain'
            break
        default:
            fType = 'text/plain'
            break
    }
    if(imageFile.size() < 131072) {
        if(fileExt in imageType) {
            if(debugEnabled) log.debug "Image Type"
            contentBlock = [
                //contentDisposition: "attachment; fileName:${params.fName}",
                contentType: fType,
                contentLength:imageFile.size(),
                gzipContent: true,
                data: imageFile,
                status:200
            ]
        } else {
            if(debugEnabled) log.debug "Text Type"
            contentBlock = [
                //contentDisposition: "attachment; fileName:${params.fName}",
                contentType: fType,
                contentLength:imageFile.size(),
                gzipContent: true,
                data: new String(imageFile),
                status:200
            ]
        }
    } else {
        contentBlock = [
            contentType: 'text/plain',
            data: "File Length Error - ${imageFile.size()} exceeds maximum of 131072",
            status:413
        ]        
    }

    if(debugEnabled) {
        log.debug "Content Length: ${imageFile.size()}"
        log.debug contentBlock
    }
    render(contentBlock)
    
}

String readFile(fName){ 
    if(debugEnabled) log.debug "Alternate Read"
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                if(!resp.contentType.contains("text") && !resp.contentType.contains("json"))
                    return "File type is not text -> $resp.contentType"
                return """${resp.data}"""
            } else {
                log.error "Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Alt Read Error: ${exception.message}"
        return null
    }
}
@Field imageType=['gif','jpeg','jpg','png','svg']
