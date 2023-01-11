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
 *     Date              Who           Description
 *    ===========       ===========   =====================================================
 *    2021-09-11        thebearmay    Change alerts to include HIA and require values, Add IP change to the alerts, default alert values, move to release status
 *    2021-11-02	    thebearmay    Add hubUpdateStatus check
 *    2021-12-07        thebearmay    getMacAddress() retired from API
 *    2022-03-23        thebearmay    remove auth for HIA-HI
 *    2022-04-06        thebearmay    use local file space to allow tiles over 1024
 *    2022-04-12        thebearmay    typo in memory warning
 *    2022-12-30        thebearmay    error when removing child device that doesn't exist
 *    2023-01-11        thebearmay    allow for V3
 */

static String version()	{  return '1.0.9'  }


definition (
	name: 			"Hub Information Aggregation", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Provides a utility to compare multiple Hub Information devices, customize the html attributes, and set alert levels",
	category: 		"Utility",
	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubInfoAggregation.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "hubAttrSelect"
    page name: "attrRepl"
    page name: "hubInfoAgg"
    page name: "hubAlerts"
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
                input "qryDevice", "device.HubInformatin,device.HubInformationV3", title: "Devices of Interest:", multiple: true, required: true, submitOnChange: true
                if (qryDevice != null){
                    hubDevCheck = true
                   // qryDevice.each{
                   //     if(it.typeName != 'Hub Information') hubDevCheck = false
                   // }
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
			    unsubscribe()
			    qryDevice.each{
                            	subscribe(it, "uptime", "refreshDevice")
			    }
                            refreshDevice()
                        } else unsubscribe()
                        href "hubAlerts", title: "Configure Hub Alerts", required: false
                    } else
                        paragraph "Invalid Device Selected"
                }
		    }
            section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
            }  
	    } else {
            section("Change Application Name", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
            }  
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

def hubAlerts(){
    dynamicPage (name: "hubAlerts", title: "Hub Alerts", install: false, uninstall: false) {
	  section(""){
          input "notifyDevice", "capability.notification", title: "Notification Devices:", multiple:true
          int numHub=0
          qryDevice.each{
              paragraph "<b><u>${it.currentValue('locationName')}</u></b>"
              input "maxTemp$numHub", "number", title:"Max Temperature (0..200)",range:"0..200", required: true
              input "maxDb$numHub", "number", title:"Max DB Size (0..1000)", range:"0..1000", required:true
              input "minMem$numHub", "number", title:"Min Free Memory (0..600000)", range:"0..600000", required:true
              input "trackIp$numHub", "bool", title:"Alert on IP Change", required:true, submitOnChange:true
              if(settings["trackIp$numHub"]) app.updateSetting("ip$numHub",[value: it.currentValue('localIP'), type:"string"])
              input "trackUpdStat$numHub", "bool", title:"Alert on Hub Update Status Change", required:true, submitOnChange:true
              if(settings["trackUpdStat$numHub"]) app.updateSetting("updStat$numHub",[value: it.currentValue('hubUpdateStatus'), type:"string"])
              numHub++
          }
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
                htmlWork += '<td>'+qryDevice[i].currentValue("$it",true)+'</td>'
            }
            htmlWork+='</tr>'
        }
        htmlWork+='</table>' 
    return htmlWork
}

def addDevice() {
    if(!this.getChildDevice("hia${app.id}"))
        addChildDevice("thebearmay","Hub Information Aggregation Device","hia${app.id}")
}

def removeDevice(){
    unschedule("refreshDevice")
    if(this.getChildDevice("hia${app.id}")) deleteChildDevice("hia${app.id}")
}

def refreshDevice(evt = null){
    String htmlStr = buildTable()
    if(htmlStr.size() > 1024){
        writeFile("hia${app.id}.html",htmlStr)
        htmlStr="<iframe src='http://${location.hub.localIP}:8080/local/hia${app.id}.html' style='width:100%;height:100%'></iframe>"
    //if(htmlStr.size() > 1024) htmlStr="<b>Attribute Size Exceeded - ${htmlStr.size()} Characters</b>"
    }
    if(createChild){
        dev = getChildDevice("hia${app.id}")
        dev.sendEvent(name:"html",value:htmlStr)
    }
    if(overwrite){
        qryDevice.each{ 
            //log.debug "$it ${htmlStr.size()} ${it.currentValue("macAddr")}"
            it.hiaUpdate( "$htmlStr")
        }
    }
    if(notifyDevice){
        int numHub=0
        qryDevice.each{
            if(it.currentValue("temperature",true) >= settings["maxTemp$numHub"]){
                notifyStr = "HIA Temperature Warning on ${it.currentValue('locationName')} - ${it.currentValue("temperature",true)}°"
                sendNotification(notifyStr)
            }
            if(it.currentValue("dbSize",true) >= settings["maxDb$numHub"]){
                notifyStr = "HIA DB Size Warning on ${it.currentValue('locationName')} - ${it.currentValue("dbSize",true)}"
                sendNotification(notifyStr)
            }
            if(it.currentValue("freeMemory",true) <= settings["minMem$numHub"]){
                notifyStr = "HIA Free Memory Warning on ${it.currentValue('locationName')} - ${it.currentValue("freeMemory",true)}"
                sendNotification(notifyStr)
            }
            if(it.currentValue("localIP",true) != settings["ip$numHub"] && settings["ip$numHub"]) {
                notifyStr = "Hub Monitor - Hub IP Address for ${it.currentValue('locationName')} has changed to ${it.currentValue("localIP",true)}"
                sendNotification(notifyStr)
		        app.updateSetting("ip$numHub",[value: it.currentValue('localIP'), type:"string"])
            }
               if(it.currentValue("hubUpdateStatus",true) != settings["updStat$numHub"] && settings["updStat$numHub"]) {
                notifyStr = "Hub Update Status for ${it.currentValue('locationName')} has changed to ${it.currentValue("hubUpdateStatus",true)}"
                sendNotification(notifyStr)
		        app.updateSetting("updStat$numHub",[value: it.currentValue('hubUpdateStatus'), type:"string"])
            }
            numHub++
         }
    
    }
 }

void sendNotification(notifyStr){
    notifyDevice.each { 
      it.deviceNotification(notifyStr)  
    }   
}

@SuppressWarnings('unused')
Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString(); 
    
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString;text/html; charset=utf-8"
			],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
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
