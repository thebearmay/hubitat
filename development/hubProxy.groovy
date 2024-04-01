/*

 */
    


static String version()	{  return '0.0.0'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field


definition (
	name: 			"Hub Proxy Server", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Hub Proxy Image Server",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/xxxxx.groovy",
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

    //if(debugEnabled)
        log.debug "serveImage called: $params"
    imageFile = downloadHubFile("${params.fName}")
    fileExt = params.fName.substring(params.fName.lastIndexOf(".")+1)
    if(debugEnabled) log.debug fileExt
    switch (fileExt) {
        case 'gif':
            fType = 'image/gif'
            break     
        case 'htm':
        case 'html':
            fType = 'text/html'
            break
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
        contentBlock = [
            contentDisposition: "attachment; fileName:${params.fName}",
            contentType: fType,
            contentLength:imageFile.size(),
            data: imageFile,
            status:200
        ]
    } else {
        contentBlock = [
            contentType: 'text/plain',
            data: "File Length Error - ${imageFile.size()} exceeds maximum of 131072",
            status:413
        ]        
    }

//    if(debugEnabled) {
        log.debug "Content Length: ${imageFile.size()}"
        log.debug contentBlock
//    }
    render(contentBlock)
    
}
