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
 *    26Jul2024                  add getURL endpoint,  remove alternate read
 *    16Nov2024                  add device attribute render 
 */
    


static String version()	{  return '0.0.4'  }

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
    path("/getImage") {  //?fName=
        action: [POST: "serveImage",
                 GET: "serveImage"]
    }
    path("/getURL") { //?url=
        action: [POST: "serveURL",
                 GET: "serveURL"]
    }
    path("/getAttribute") { //?dev=xxxx,attr=xxxx
        action: [POST: "serveAttr",
                GET: "serveAttr"]
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
                paragraph "<b>Cloud Request Formats: </b><br>${getFullApiServerUrl()}/getImage?access_token=${state.accessToken}&fName=yourFileName.fileExtension<br><br>${getFullApiServerUrl()}/getURL?access_token=${state.accessToken}&url=urlEncodedURL<br><br>${getFullApiServerUrl()}/getAttribute?access_token=${state.accessToken}&dev=deviceID&attr=attributeName"
        }
        section(name:"opt",title:"Options", hideable: true, hidden: false){
            input("debugEnabled","bool",title:"Enable Debug Logging", width:4)
        }
        section(name:"devs",title:"Authorized Devices", hideable:true, hidden:true){
            input "devlist","capability.*",title:"Select Devices to Authorized",multiple:true
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
    log.debug "serveImage called: ${params} source: ${request.requestSource}"
    renderFile(params.fName)
}

def serveURL(){
    if(debugEnabled)
        log.debug "serveURL called: ${params.url}"
    fContent = readFile(params.url)

    uploadHubFile("proxyWork${app.id}.htm",fContent.getBytes("UTF-8"))
    renderFile("proxyWork${app.id}.htm")
}

def serveAttr(){
    if(debugEnabled)
        log.debug "serveAttr called: ${params.dev} ${params.attr}"
    devlist.each{
        if ("${it.id}" == "${params.dev}"){
            //uploadHubFile("proxyWork${app.id}.htm",it.currentValue("${params.attr}").getBytes("UTF-8"))
            //renderFile("proxyWork${app.id}.htm")
            contentBlock = [
                contentType: "text/html; charset:utf-8 ",
                gzipContent: true,
                data: it.currentValue("${params.attr}").replace("°","&deg;"),
                status:200
            ]
        }
    }
    if(debugEnabled)
        log.debug "$contentBlock"
    render (contentBlock)
}

void delWork(){
    deleteHubFile("proxyWork${app.id}.htm")
}
    
def renderFile(fName) {

    fileExt = fName.substring(fName.lastIndexOf(".")+1)
    imageFile = downloadHubFile("${fName}")        
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
    if(imageFile.size() < 9999999){// limit was originally 131072 
        if(fileExt in imageType) {
            if(debugEnabled) log.debug "Image Type"
            contentBlock = [
                contentType: fType,
                contentLength:imageFile.size(),
                gzipContent: true,
                data: imageFile,
                status:200
            ]
        } else {
            if(debugEnabled) log.debug "Text Type"
            contentBlock = [
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
    delWork()
    render(contentBlock)
    
}

String readFile(fName){ 
    if(debugEnabled) log.debug "Alternate Read"
    uri = "${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        ignoreSSLIssues: true
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
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
