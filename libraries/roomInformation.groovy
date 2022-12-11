/*
* Methods:
* 
* String getRoomList(String username, String password)
*    Returns a JSON String containing a list of rooms with their associated attributes, i.e.
*        {"roomNumber":{"name":"roomName","hasImage":"trueOrFalse","deviceList":{"deviceName":"deviceNumber",...}}}
*        (this is the roomJson that all other methods are expecting)
*        username and password are only required if the hub is using Hub Security
*
* String getRoomImage(Integer rNum, String roomJson)
*    Returns a string containing an html image tag for the room image or "-1" if no image was assigned
*
* String getRoomName(Integer rNum, String roomJson)
*    Returns a string containing the room name, or "-1" if an invalid room number was passed
*
* String getRoomDevices(Integer rNum, String roomJson)
*    Returns a JSON string containing a list of devices and their device number {"deviceName":"deviceNumber",...}
*        or {"Error":"-1"} if an invalid room number was passed
*
* String roomLookup(String rName, String roomJson)
*    Returns a JSON string containing a list of rooms (and their room number) that contain rName (case insensitive)
*        in their name {"roomName":"roomNumber",...} or {"Error":"-1"} if an invalid room number was passed
*
*/


library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "Utilities",
    description: "Room Processing Methods",
    name: "roomInformation",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/roomInformation.groovy",
    version: "1.0.0",
    documentationLink: ""
)

String getRoomList(username="", password=""){
    respData = readPage("http://127.0.0.1:8080/room/list", username, password)
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
    return buildRoomJSON(roomList,devRoomList,devNameList,roomImageList)
}

String buildRoomJSON(rList,dToR,dToN,rToI){
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
    return roomJson
    
}

String getRoomImage(rNum, roomJson) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText("$roomJson")
    if(rInfo["$rNum"]?.hasImage == "true")
        return "<img src='http://${location.hub.localIP}:8080/room/image/$rNum' />"
    else
        return "-1"
}

String getRoomName(rNum, roomJson) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText("$roomJson")
    if(rInfo["$rNum"]?.name)
        return "${rInfo["$rNum"]?.name}")
    else
        return "-1"
}

String getRoomDevices(rNum, roomJson) {
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText("$roomJson")
    if(rInfo["$rNum"]?.deviceList)
        return "${JsonOutput.toJson(rInfo["$rNum"]?.deviceList)}"
    else
        return ${JsonOutput.toJson(["Error":"-1"])}"
}

String roomLookup(rName, roomJson){
    JsonSlurper jSlurp = new JsonSlurper() 
    Map rInfo = (Map) jSlurp.parseText("$roomJson")
    rName=rName.toLowerCase()
    rList=[:]
    rInfo.each{
        if(it.value.name.toLowerCase().contains("$rName")){
            rList["${it.value.name}"]="${it.key}"
        }
    }
    if(rList.size()>0)
        return "${JsonOutput.toJson(rList)}"
    else
        return ${JsonOutput.toJson(["Error":"-1"])}"
}

String readPage(fName, username="", password=""){
    if(username) cookie = securityLogin(username, password).cookie
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
HashMap securityLogin(username, password){
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
