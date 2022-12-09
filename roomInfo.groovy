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
*/
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

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

    }
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
}

void initialize() {
    getRoomList()
}

void refresh() {
    getRoomList()
}

def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void getRoomList(){
    respData = readPage("http://127.0.0.1:8080/room/list")
    recs = respData.split("\n")
    if (debugEnabled) log.debug recs.size()
    roomList = [:]
    devRoomList = [:]
    devNameList = [:]
    roomImageList = [:]
    recs.each {
        if(it.contains('/room/edit/') && it.contains('gridRoomLink')){
            found = it.indexOf('/room/edit/')
            rName = it.substring(it.indexOf(">")+1,it.lastIndexOf("<"))
            rNum=it.substring(found+11, it.indexOf('"', found))
            roomList["$rName"]="$rNum"
            currRoom = rNum
        }
        if(it.contains('/device/edit/') && it.contains('gridRoomDeviceLink')){
            found = it.indexOf('/device/edit/')
            dName = it.substring(it.indexOf(">")+1,it.lastIndexOf("<"))
            dNum=it.substring(found+13, it.indexOf('"', found))
            devRoomList["$dNum"]="$currRoom"
            devNameList["$dNum"]="$dName"
        }
        if(it.contains('/room/image/') && it.contains('listRoomImage')){
            found = it.indexOf('/room/image/')
            iNum=it.substring(found+12, it.indexOf('"', found))
            roomImageList["$iNum"]="true"
        }
    }
//    updateAttr("roomList",roomList) 
//    updateAttr("devRoomList",devRoomList)
//    updateAttr("devNameList",devNameList)
    buildRoomJSON(roomList,devRoomList,devNameList,roomImageList)
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
        updateAttr("cmdReturn", "${rInfo["$rNum"]?.deviceList}")
    else
        updateAttr("cmdReturn","Invalid Room")
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
