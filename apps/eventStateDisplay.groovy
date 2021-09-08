/*
 * Event / State Display
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
 *    Date          Who           What
 *    ----          ---           ----
 *    2021-08-31    thebearmay    Original version 0.1.0
 */

import java.text.SimpleDateFormat
import groovy.transform.Field
static String version()	{  return '0.1.0'  }
@Field attrList = []


definition (
	name:		"Event-State Display", 
	namespace:	"thebearmay", 
	author:		"Jean P. May, Jr.",
	description: 	"Display all events or states for a given attribute",
	category:	"Utility",
	importUrl:	"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/eventStateDisplay.groovy",
	oauth:		false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "showItems"
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
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: false, required: true, submitOnChange: true
                if(qryDevice!=null) buildAttrList()
                input "attrSelected", "enum", title: "Attribute to pull events/states for:", required: true, options: attrList
                if (qryDevice != null && attrSelected != null) href "showItems", title: "Event/State Information", required: false
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def showItems(){
    dynamicPage (name: "showItems", title: "", install: false, uninstall: false) {
	  section(""){
            dispTable = 
                "<style type='text/css'>div{overflow:auto;} .mtable {border:1px black solid;padding:0px;width:100%;} th {border:1px black solid; min-width:16em;} td {border-bottom:1px black solid;border-left:1px black solid;vertical-align:top;}</style><div class='.mdiv'>"
            evtList = {}
            dName = qryDevice.displayName 
            dispTable+="<table class='mtable'><tr><th>$dName State Changes</th><th>Attribute:$attrSelected</th></tr>"
            dispTable+="<tr><th>Date-Time</th><th>Value</th></tr>"
            evtList=qryDevice.statesSince(attrSelected.toString(),Date.parse("yyyy-MM-dd hh:mm", "1970-01-01 00:00:00"), [max:1000000])
            if(dwnldState) statePipe="$dName State Changes|Attribute:$attrSelected|[crlf]"
            evtList.each {
                dispTable += "<tr><td>${it.date}</td><td>${it.value}</td></tr>"
                if(dwnldState) statePipe+="$it.date|$it.value|[crlf]" 
            }  
            dispTable += "</tr></table></div>"
          
            dispTable2="<table class='mtable'><tr><th>$dName Events</th><th>Attribute:$attrSelected</th></tr><tr>"
            dispTable2+="<tr><th>Date-Time</th><th>Value</th><th>Type</th></tr>"
            evtList=qryDevice.eventsSince(Date.parse("yyyy-MM-dd hh:mm", "1970-01-01 00:00:00"), [max:10000000])
            if(dwnldEvent) eventPipe="$dName Events|Attribute:$attrSelected||[crlf]"          
            evtList.each {
                if(it.properties.name == attrSelected){
                    if(it.properties.type == null || it.properties.type == "null") itType = "NA"
                    else itType = it.properties.type
                    dispTable2 += "<tr><td>$it.properties.date</td><td>$it.properties.value</td><td>$itType</td></tr>"
                    if(dwnldEvent) eventPipe+="$it.properties.date|$it.properties.value|$itType|[crlf]" 
                }
            }
            dispTable2 += "</tr></table></div>"
           
            section ("States", hideable: true, hidden: false) {  
                paragraph  "$dispTable"
                input "dwnldState", "bool", title:"Download State Data", submitOnChange:true
                if(dwnldState) paragraph downloadFile("state.txt",statePipe)
            }
            section ("Events", hideable: true, hidden: true) {  
                paragraph  "$dispTable2"
                input "dwnldEvent", "bool", title:"Download Event Data", submitOnChange:true
                if(dwnldEvent) paragraph downloadFile("event.txt",eventPipe)
            }
      }
    }
}

def buildAttrList(){
    qryDevice.supportedAttributes.each{
        attrList.add(it.toString())
    }
}

String downloadFile(fName="download.txt",fContent="no content supplied"){
    // Display the resultant string in a paragraph to execute
    fileContent = fContent.replace('"','\"')
    String jsStr = "<script type='text/javascript'>const downloadToFile = (content, filename, contentType) => {const a = document.createElement('a');const file = new Blob([content], {type: contentType});a.href= URL.createObjectURL(file);a.download = filename;a.click();URL.revokeObjectURL(a.href);};downloadToFile('$fileContent', '$fName', 'text/plain');</script>"
    return jsStr
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
