/*
 * 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
*/

static String version()	{  return '0.0.0'  }

definition (
	name: 			"Page Save", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Reads and saves HTML Page to the local file system - used for debugging screen scrapes",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/pageSave.groovy",
    installOnOpen:  true,
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
    dynamicPage (name: "mainPage", title: "Page Save <span style='font-size:smaller'>v${version()}</span>", install: true, uninstall: true) {
        section("") {
            input("url","string", title:"URL to save to File Manager")
            input("runScrape", "button", title:"Save Page")
            if(state.getPage){
                state.getPage = false
                pageSrc = readPage("$url")
                uploadHubFile("PS${new Date().getTime()}.txt",pageSrc.getBytes('UTF-8'))
            }

        }
    }
}

String readPage(url){
    def params = [
        uri: "$url",
        contentType: "text/html",
        textParser: true          
    ]
    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return """${resp.data}"""
            }
            else {
                log.error "Null Response"
                return " "
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return " ";
    }
}

def appButtonHandler(btn) {
    switch(btn) {
        case "runScrape":
            state.getPage = true
            break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}
