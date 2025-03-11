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
 *    2021-07-28  thebearmay	add .properties expansion
 *    2021-11-19  thebearmay    add report option ->v2.0.0
 *    2021-11-22  thebearmay    clean up last activity date on report and add commands
 *    2022-02-07  thebearmay    add Capabilities to the report
 *    2022-06-20  thebearmay    embedded section error
 *	  2025-03-11  thebearmay	line 243 bool value generating error correction
 */
import java.text.SimpleDateFormat
static String version()	{  return '2.0.5'  }


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
   page name: "deviceReport"
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
                if (qryDevice != null) {
                    href "deviceCharacteristics", title: "Device Characteristics", required: false
                    href "deviceReport", title: "Device Report", required: false
                }
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
//	  section("Device Characteristics"){
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
       //}
    }
}

def deviceReport(){
    dynamicPage (name: "deviceReport", title: "", install: false, uninstall: false) {
	  section("Device Characteristics Report"){
          rpt=buildReport()
          String rptHTML = """
        <script type='text/javascript'>
            winObj=window.open('','_blank');
            winObj.document.body.innerHTML='$rpt';
        winObj.focus();
        </script>
        """
         paragraph "$rptHTML"
         paragraph "<div id='devRpt'>$rpt</div>"
      }
    }
}

def buildReport() {
     hh = hubitat.helper.HexUtils

    String html = "<style>div{overflow:auto;}th, td{text-align:left;border:solid 1px blue;vertical-align:top;}</style>"
    html += "<table><tr><th>Name</th><th>Type</th><th>Status</th><th>Disabled</th><th>Controller</th><th>Capabilities</th><th>Attributes</th><th>Commands</th><th>Data</th><th>DNI</th><th>Last Activity</th></tr>"
    qdSorted = qryDevice.displayName.sort()
    for(i=0; i<qdSorted.size(); i++) {   
        qryDevice.each {
            if (it.displayName == qdSorted[i]){
                html += "<tr><td>${it.displayName}</td>"
                html += "<td>${it.typeName}</td>"
                html += "<td>${it.status}</td>"
                html += "<td>${it.disabled}</td>"
                if(it.controllerType == "LNK")
                    cType = "Mesh Link"
                else if (it.controllerType == "ZGB")
                    cType = "Zigbee"
                else if (it.controllerType == "ZWV")
                    cType = "Z-Wave"
                else
                    cType = "Other"
                html += "<td>$cType</td>"
                capString = ""
                it.capabilities.each {
                    capString+="$it<br />"
                }
                html += "<td>$capString</td>"
                attrString = ""
                it.currentStates.each{
                    tVal = it.value.replace('"','')
                    tVal = it.value.replace("'","")
                    attrString += "<b>Name:</b>${it.name} <b>Value:</b>${tVal}<b> Type:</b>${it.dataType}<br>"
                }
                html += "<td>$attrString</td>"
                cStr =""
                it.supportedCommands.each{
                    cStr +="<b>$it.name</b>(${it.parameters})<br>"
                }
                html += "<td>$cStr</td>"
                dData = it.getData()
                dString = ""
                dData.each{
                    if(it.key == "ip" && it.key.indexOf(".") == -1){
                        tval = ""
                        for (i=0;i<8;i=i+2){
                            tVal = hh.hexStringToInt(it.value.substring(i,i+2))+"."
                        }
                        tVal = tVal.substring(0,tVal.size()-1)
                        dString = "<b>${it.key}</b> ${tVal}<br>"
                    } else if(it.key == "mac" && it.key.indexOf(":") == -1){
                        tVal = ""
                        for (i=0;i<12;i=i+2){
                            tVal+=it.value.substring(i,i+2)+":"
                        }
                        tVal = tVal.substring(0,tVal.size()-1)
                        dString = "<b>${it.key}</b> ${tVal}<br>"
                    }else if(it.key == port){
                        dString = "<b>${it.key}</b> ${hh.hexStringToInt(it.value)}<br>"                
                    } else {
                        dString += "<b>${it.key}</b>" 
                        if(it.value) dString+= " ${it.value.toString().replace('\"','')}<br>"
                    }
                }            
                html += "<td>$dString</td>"
        
                html += "<td>${it.deviceNetworkId}</td>"
                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ")
                SimpleDateFormat sdfOut = new SimpleDateFormat("ddMMMyyyy HH:mm:ss")
                Date wkDate = sdfIn.parse((String)it.lastActivity)
                fDate = sdfOut.format(wkDate)
                html += "<td>${fDate}</td>"
        
                html +="</tr>"
            }                
        }
    }
    
    html +="</table>"

    return html
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
