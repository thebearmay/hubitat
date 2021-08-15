/* Hub Information Aggregation
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
 */

static String version()	{  return '0.0.2'  }


definition (
	name: 			"Hub Information Aggregation", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides a utility to compare multiple Hub Information devices, and customize the html attribute",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubInfoAggregation.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "hubAttrSelect"
   page name: "attrRepl"
   page name: "hubInfoAgg"
}

void installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
                input "qryDevice", "capability.initialize", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null){
                    hubDevCheck = true
                    qryDevice.each{
                        if(it.typeName != 'Hub Information') hubDevCheck = false
                    }
                    if(hubDevCheck) {
                        href "hubAttrSelect", title: "Select Attributes", required: true
                        href "attrRepl", title: "Alternate Text for Attributes", required: false
                        href "hubInfoAgg", title: "Show Hubs", required: false
                        input "createChild", "bool", title: "Create child device for HTML attribute?", defaultValue: false, submitOnChange: true
                        if(createChild) {
                            addDevice()
                        } else {
                            removeDevice()
                        }
                        input "overwrite", "bool", title:"Overwrite Hub Info (2.6.0+ required) html attribute(s)", defaultValue: false
                        if(createChild || overwrite){
                            subscribe(qryDevice[0], "uptime", "refreshDevice")
                            refreshDevice()
                        } else unsubscribe
                    } else
                        paragraph "Invalid Device Selected"
                }
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def hubAttrSelect(){
    dynamicPage (name: "hubAttrSelect", title: "Hub Attribute Selection", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          dev = qryDevice[0]
          def attrList=[]
          dev.supportedAttributes.each{
              attrList.add(it.toString())
          }
          sortedList=attrList.sort()
          sortedList.each{
            input "$it", "bool", title: "$it", required: false, defaultValue: false
          }


          paragraph "<p>$strWork</p>"
       }
    }
}

def attrRepl(){
    dynamicPage (name: "attrRepl", title: "Alternate Attribute Descriptions", install: false, uninstall: false) {
	  section(""){
          String strWork = ""
          dev = qryDevice[0]
          def attrList=[]
          dev.supportedAttributes.each{
              attrList.add(it.toString())
          }
          sortedList=attrList.sort()
          sortedList.each{
            input "repl$it", "string", title: "$it", required: false
          }


          paragraph "<p>$strWork</p>"          
      }
    }
}

def hubInfoAgg(){
    dynamicPage (name: "hubInfoAgg", title: "Hub Information Aggregation", install: false, uninstall: false) {
	  section("Aggregation Detail"){
          htmlWork = buildTable()
          paragraph "<div style='overflow:auto'>$htmlWork<p style='font-size:xx-small'>${htmlWork.size()} characters</p></div>"
      }
    }
}

String buildTable() {
        String htmlWork = '<table id=\'hia\'><tr><th></th>'
        for(i=0;i<qryDevice.size();i++){
              htmlWork+='<th>'+qryDevice[i].currentValue('locationName')+'</th>'
        }
        htmlWork+='</tr>'
        def attrList=[]
        qryDevice[0].supportedAttributes.each{
            if(settings["$it"] && "$it"!="locationName") attrList.add(it.toString())
            sortedList=attrList.sort()
        }
        sortedList.each{
            replacement = "repl$it"
            if(settings["$replacement"]) htmlWork+="<tr><th>${settings[replacement]}</th>"
            else htmlWork+="<tr><th>$it</th>"
            for(i=0;i<qryDevice.size();i++){
                htmlWork += '<td>'+qryDevice[i].currentValue("$it")+'</td>'
            }
            htmlWork+='</tr>'
        }
        htmlWork+='</table>' 
    return htmlWork
}

def addDevice() {
    if(!this.getChildDevice("hiad001"))
        addChildDevice("thebearmay","Hub Information Aggregation Device","hiad001")
}

def removeDevice(){
    unschedule("refreshDevice")
    deleteChildDevice("hiad001")
}

def refreshDevice(evt = null){
    String htmlStr = buildTable()
    if(htmlStr.size() > 1024) htmlStr="<b>Attribute Size Exceeded - ${htmlStr.size()} Characters</b>"
    if(createChild){
        dev = getChildDevice("hiad001")
        dev.sendEvent(name:"html",value:htmlStr)
    }
    if(overwrite){
        qryDevice.each{      
            it.hiaUpdate(htmlStr,it.currentValue("macAddr"))
        }
    }
 }

def appButtonHandler(btn) {
    switch(btn) {
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

void intialize() {

}
