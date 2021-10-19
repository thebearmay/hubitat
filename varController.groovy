/*
    Don't save after 2.2.9.134 or functionality may break
 */

static String version()	{  return '0.0.0'  }
#include hubitat.bb2Html

definition (
	name: 			"Hub Variable Controller", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Capture 2.2.9.134 Variable Capability",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/varController.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
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
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: false, submitOnChange: true
                if (qryDevice != null) 
                    ccSubscribe()
                else 
                    unsubscribe()
		    }

	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def ccSubscribe(){
    unsubscribe()
    subscribe(qryDevice,"varName",ccProcess)
}

def ccProcess(evt=null) {
    dev = this.getSubscribedDeviceById(evt.deviceId)
    vName = dev.currentValue("varName")
    vCmd = dev.currentValue("varCmd")
    vValue = dev.currentValue("varValue")
    vType = dev.currentValue("varType")
    if(vCmd == "create") {
        if (!this.getGlobalVar(vName))
            success = this.createGlobalVar(vName, vValue, vType)
        else
            success = "Already existed"
        dev.varReturn(success)
    }else if (vCmd == "delete"){
        success = this.deleteGlobalVar(vName)
        dev.varReturn(success)
    }else if (vCmd == "set"){
        success = this.setGlobalVar(vName, vValue)
        dev.varReturn(success)
    }else if (vCmd == "get"){
        if(getGlobalVar(vName)) dev.varReturn(this.getGlobalVar(vName).value)
        else dev.varReturn("Variable not found")
    }
}

def appButtonHandler(btn) {
    switch(btn) {
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

def intialize() {

}
