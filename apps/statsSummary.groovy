/* 

 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import java.text.DecimalFormat 
static String version()	{  return '0.0.1'  }


definition (
	name: 			"Stats Summary", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides an alternate view of the log data",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/statsSummary.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
}

def installed() {
    log.info "${app.name} v${version()} installed()"
    state?.isInstalled = true
    state?.singleThreaded = true
    initialize()
}

def uninstalled() {
    log.info "${app.name} v${version()} uninstalled()"
    removeDevice()
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
    dynamicPage (name: "mainPage", title: "<h2 style='color:darkBlue'>${app.getLabel()}</h2><p style='font-size:small;color:navy'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("<b style='color:darkBlue'>Summary</b>")
		    {
                def df = new DecimalFormat("#0.000")
                logData = getLogs()
                devHtml = "<p><b>Total Device Uptime:</b> ${logData.devicesUptime}</p>"
                devHtml += '<table><tr><th>Name</th><th>% Total</th><th>Minutes</th></tr>'
                logData.deviceStats.each {
                    tMin = it.total.toInteger()/1000/60
                    if(tMin > 0) {
                        devHtml+="<tr><td>${it.name}</td><td>${it.formattedPctTotal}</td><td>${df.format(tMin)}</td></tr>"
                    }
                }
                devHtml+='</table>'
                appHtml = "<p><b>Total App Uptime:</b> ${logData.appsUptime}</p>"
                appHtml += '<table><tr><th>Name</th><th>% Total</th><th>Minutes</th></tr>'
                logData.appStats.each {
                    tMin = it.total.toInteger()/1000/60
                    if(tMin > 0) {
                        appHtml+="<tr><td>${it.name}</td><td>${it.formattedPctTotal}</td><td>${df.format(tMin)}</td></tr>"
                    }
                }
                appHtml+='</table>'
                paragraph "<style>td{padding:3px}.row {display: flex;}.column { flex: 50%;}</style><div class='row'><div class='column'>${devHtml}</div><div class='column'>${appHtml}</div></div>" 
                
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

HashMap getLogs(){
    def params = [
        uri: 'http://127.0.0.1:8080/logs/json',
        contentType: "application/json",
        textParser: false,
        headers: [
				"Cookie": cookie
            ]        
    ]

    try {
        httpGet(params) { resp ->
            //log.debug resp.data
            return resp.data
        }
    }catch (ex) {
        log.error "$ex"
    }
}


def appButtonHandler(btn) {
    switch(btn) {
        case "button1":
            state.buttonPushed = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
      }
}
