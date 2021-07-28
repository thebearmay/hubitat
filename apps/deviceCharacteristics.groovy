/*
 * Device Details Display
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
 *    Date        Who           What
 *    ----        ---           ----
 *    2021-03-11  thebearmay	Original version 0.1.0
 *    2021-03-15  thebearmay    Release Candidate version 1.0.0
 *    2021-04-14  thebearmay    pull in last attribute state change
 *    2021-04-25  thebearmay    error when attribute has never been set
 *    2021-05-04  thebearmay    hub 2.2.7x changes
 *	  2021-07-28  thebearmay	add .properties expansion
 */

static String version()	{  return '1.3.0'  }


definition (
	name: 			"Device Details Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Display the capabilities, attributes, commands and device data for devices selected.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/deviceCharacteristics.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
   page name: "mainPage"
   page name: "deviceCharacteristics"
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
                input "qryDevice", "capability.*", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null) href "deviceCharacteristics", title: "Device Characteristics", required: false
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def deviceCharacteristics(){
    dynamicPage (name: "deviceCharacteristics", title: "", install: false, uninstall: false) {
	  section("Device Characteristics"){
          def strWork = ""
          for(i=0;i<qryDevice.size();i++){
            if (qryDevice[i].label) qryName= qryDevice[i].label
            else qryName = qryDevice[i].name
            def qryDeviceState = ""
            def nl = false
            qryDevice[i].supportedAttributes.each {
                if (nl) qryDeviceState += "\n"
                def tempValue = qryDevice[i].currentValue("$it")
               
             
                def tempDisp = qryDevice[i].currentState("$it").toString() 
                Integer start = tempDisp.indexOf('[')+1
                Integer end = tempDisp.length() - 1
                tempDisp = tempDisp.substring(start, end)
                stateParts = tempDisp.split(',')
                if(stateParts.size() > 1){
                    if(location.hub.firmwareVersionString <= "2.2.7.0") {
                        unt = stateParts[3]?.trim()
                        ts = stateParts[0]
                        dsc= stateParts[1]
                    }else {
                        unt = stateParts[4]?.replace("unit=","")
                        unt =  unt.trim()
                        ts = stateParts[1]?.replace("date=","")
                        dsc = stateParts
                    }
                    tempDisp = "\n\t\t<b>Activity Timestamp: </b>${ts}\n\t\t<b>Event Desc: </b>${dsc}\n\t\t<b>Unit:</b>$unt"
		            // [2] - value, [4] - data type
                } else tempDisp = "N/A"
                qryDeviceState += "<span style='font-weight:bold'>$it </span>(${it.dataType}): $tempValue  \n<span style='font-weight:bold'>\tLast State Change</span> $tempDisp"
                nl = true
            }
            def devAttr = qryDevice[i].supportedAttributes
            def devCap = qryDevice[i].capabilities
            def devCmd = qryDevice[i].supportedCommands
            def devData = qryDevice[i].getData()
            def devId = qryDevice[i].getId()
            //def devRoom = qryDevice[i].room
            def devType = qryDevice[i].getTypeName()
 
            pStr1 = "<h2 style='border-top: 3px blue solid'>$qryName</h2><span style='font-weight:bold'>ID:</span> $devId \n\n"// <span style='font-weight:bold'>Room:</span> $devRoom \n\n"
            pStr1 += "<span style='font-weight:bold'>Type: </span> $devType\n\n"
            pStr1 += "<span style='font-weight:bold'>Capabilities:</span> $devCap\n\n"
            pStr1 += "<span style='font-weight:bold'>Attributes:</span>$devAttr"
              pStr2 = "<span style='font-weight:bold'>Device Data:</span> $devData"
            section ("") {
                paragraph pStr1
            }
            section ("Attribute Details", hideable: true, hidden: true) {  
                paragraph   "<span style='font-weight:bold;'>Current Values:</span>\n$qryDeviceState"
            }
            section {
                for (n=0;n<devCmd.size; n++){
                    strWork += "\n<span style='font-weight:bold'>"+devCmd.name[n] + "</span>(" +  devCmd.parameters[n] + ")"
                }
                paragraph "<span style='font-weight:bold'>Commands:</span>$devCmd"
            }   
            section ("Command Details", hideable: true, hidden: true) {  
                paragraph  "<span> $strWork </span>"
            }
            section (""){    
                paragraph pStr2    
            }
			section ("Properties Details", hideable: true, hidden: true) { 
				propStr =""
				qryDevice[i].properties.each {
					propStr+="$it<br>"
				}
				paragraph propStr
			}			
          }
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

def intialize() {

}
