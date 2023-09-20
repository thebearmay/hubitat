/*
 * Room Information 
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
 *    09Dec2022    thebearmay    Original Code
 *    11Dec2022    thebearmay    add room lookup
 *    05Sep2023    thebearmay    2.3.6.x changes (Note: full JSON for rooms is at /hub2/roomsList)
 *    20Sep2023    thebearmay    check to see if hub is still rebooting
*/
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

static String version() {return "1.0.3"}

metadata {
    definition (
        name: "Room Information", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/roomInfo.groovy",
        description: "Consolidates the room information into a number of lists and a summary JSON"
    ) {
          
        capability "Actuator"
        capability "Refresh"
        capability "Initialize"
        
        attribute "roomJson", "string"
        attribute "cmdReturn", "string"
        
        command "getRoomImage", [[name:"roomNum", type:"NUMBER", description:"Room Number"]]
        command "getRoomName", [[name:"roomNum", type:"NUMBER", description:"Room Number"]]
        command "getRoomDevices", [[name:"roomNum", type:"NUMBER", description:"Room Number"]]
        command "roomLookup",[[name:"roomName", type:"STRING", description:"Partial or Full Name"]]

    }
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
    input("pollRate","number", title:"Poll rate (in minutes) (Default:1440, Disable:0):", defaultValue:1440, submitOnChange:true, width:4)    
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
}

void initialize() {
    if(pollRate == null) {
        device.updateSetting("pollRate",[value:1440,type:"number"])
    }
    if(pollRate == 0)
        unschedule("refresh")
    else 
        runIn(pollRate.toInteger()*60,"refresh")    
    getRoomList()
}

void refresh() {
    getRoomList()
    if(pollRate == 0)
        unschedule("refresh")
    else 
        runIn(pollRate.toInteger()*60,"refresh")    
}

def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    if(pollRate == null) {
        device.updateSetting("pollRate",[value:1440,type:"number"])
    }
    if(pollRate == 0)
        unschedule("refresh")
    else 
        runIn(pollRate*60,"refresh")
    
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void getRoomList(){
    respData = readPage("http://127.0.0.1:8080/hub2/devicesList")
    if (debugEnabled) log.debug "${respData}"
    if(respData.substring(0,1) == '<')
        runIn(10,"getRoomList")
    else {
        def jSlurp = new JsonSlurper()
        Map recs = (Map)jSlurp.parseText((String)respData)
        roomList = [:]
        devRoomList = [:]
        devNameList = [:]
        roomImageList = [:]
        recs.devices.each {
            if(it.data.roomId == 0 || it.data.roomName == null){
                it.data.roomName = "No Room Assigned"
            }
      
            roomList["${it.data.roomName}"]="${it.data.roomId}"
            currRoom = it.data.roomId
            devRoomList["${it.data.id}"]="$currRoom"
            devNameList["${it.data.id}"]="${it.data.name}"
            imageTest = readPage("http://127.0.0.1:8080/room/image/${it.data.roomId}")
            if(imageTest.size() != 1207)
                roomImageList["${it.data.roomId}"]="true"                                 
        
        }
        buildRoomJSON(roomList,devRoomList,devNameList,roomImageList)
    }
}

void buildRoomJSON(rList,dToR,dToN,rToI){
    roomMap = [:]
    tempMap = [:]    
    rList.each{ r ->
        dToR.each {
            if (debugEnabled) log.debug "$it.value $it.key $r.value"
            if("${it.value}" == "${r.value}"){
                if (debugEnabled) log.debug "${dToN["${it.key}"]} ${it.key}"
                tempMap["${dToN["${it.key}"]}"]="$it.key"
                if (debugEnabled) log.debug "$tempMap"
            }
        }
        if (debugEnabled) log.debug "tMap $tempMap"
        roomMap["${r.value}"]=[:]
        roomMap["${r.value}"].name = "${r.key}"
        if(tempMap != null){
            roomMap["${r.value}"].deviceList = tempMap
            if (debugEnabled) log.debug "${r.key} Devices: ${roomMap["${r.value}"].deviceList} / $tempMap"
        }
        if (debugEnabled) log.debug "${r.key} ${r.value} $rToI ${rToI["${r.value}"]}"
        if(rToI["${r.value}"] != null)
            roomMap["${r.value}"].hasImage = "true"
        else
            roomMap["${r.value}"].hasImage = "false"
        tempMap = [:]
    }
    roomJson = JsonOutput.toJson(roomMap)
    updateAttr("roomJson", roomJson)
    
}

void getRoomImage(rNum) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText(device.currentValue("roomJson"))
    if(rInfo["$rNum"]?.hasImage == "true")
        updateAttr("cmdReturn", "<img src='http://${location.hub.localIP}:8080/room/image/$rNum' />")
    else
        updateAttr("cmdReturn","No Image Available")
}

void getRoomName(rNum) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText(device.currentValue("roomJson"))
    if(rInfo["$rNum"]?.name)
        updateAttr("cmdReturn", "${rInfo["$rNum"]?.name}")
    else
        updateAttr("cmdReturn","Invalid Room")
}

void getRoomDevices(rNum) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText(device.currentValue("roomJson"))
    if(rInfo["$rNum"]?.deviceList)
        updateAttr("cmdReturn", "${JsonOutput.toJson(rInfo["$rNum"]?.deviceList)}")
    else
        updateAttr("cmdReturn","Invalid Room")
}

void roomLookup(rName){
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText(device.currentValue("roomJson"))
    rName=rName.toLowerCase()
    rList=[:]
    rInfo.each{
        if(it.value.name.toLowerCase().contains("$rName")){
            rList["${it.value.name}"]="${it.key}"
        }
    }
    if(rList.size()>0)
    updateAttr("cmdReturn","${JsonOutput.toJson(rList)}")
    else
        updateAttr("cmdReturn","Room not Found")
}

String readPage(fName){
    if(security) cookie = securityLogin().cookie
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
        ]
                    
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}
HashMap securityLogin(){
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
	return [result: result, cookie: cookie]
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
