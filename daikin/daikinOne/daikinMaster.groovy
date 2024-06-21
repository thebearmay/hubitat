/*
 * Daikin One Open Master 
 *
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
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "0.0.2"}

metadata {
    definition (
        name: "Daikin One Master", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/daikin/daikinOne/daikinMaster.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"

        attribute "locationMap", "string"

    }   
}

preferences {
    input("serverPath", "text", title:"Daikin Server Path:", required: true, submitOnChange:true, defaultValue: "https://api.daikinskyport.com")

    input("daiEmail", "text", title:"Email registered with Daiken:", required: true, submitOnChange:true)
    input("daiPwd", "password", title:"Daikin Password:", required: true, submitOnChange: true)

    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "${device.displayName} v${version()} installed()"
    initialize()
}

def initialize(){

}

@SuppressWarnings('unused')
def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,"logsOff")
    } else 
        unschedule("logsOff")

}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"
    getLocations()
    createChildDevices()

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

String getAuth() {
    if(serverPath == null)
        device.updateSetting("serverPath",[value:"https://api.daikinskyport.com",type:"string"])
    
    if(debugEnabled) log.debug "getAuth $daiEmail:$daiPwd"
    
    String bodyText = JsonOutput.toJson([email:daiEmail, password:daiPwd])
    
    Map requestParams =
	[
        uri:  "$serverPath/users/auth/login",
        headers: [
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        ], 
        body: "$bodyText"
	]

    authKey=""
    if(debugEnabled) log.debug "$requestParams"
    
    httpPost(requestParams) { resp ->
        if(debugEnabled) log.debug "$resp.properties"
        jsonData = (HashMap) resp.data
        authKey = jsonData.accessToken
    }
    return authKey       

}
    
HashMap getDeviceList(){
    HashMap devMap = sendGet("/devices")
    return devMap
}

HashMap getDevDetail(id) {
    HashMap devDetail = sendGet("/deviceData/$id")
    return devDetail
}

void getLocations() {
    HashMap locData = sendGet("/locations")
    updateAttr("locationMap","$locData")
}

HashMap sendGet(command){
    authToken = getAuth()
    if(debugEnabled) log.debug "sendGet token:$authToken API: $apiKey"
    Map requestParams =
	[
        uri:  "$serverPath$command",
        headers: [
            'Accept': 'application/json',
            "Authorization" : "Bearer $authToken"
        ]
	]

    if(debugEnabled) log.debug "get parameters $requestParams"
    httpGet(requestParams) { resp ->
        if(debugEnabled) log.debug "$resp.properties"
        jsonData = (HashMap) resp.data
    }
    if(debugEnabled) log.debug "get JSON $jsonData"
    return jsonData
                            
}

void createChildDevices(){
    if(debugEnabled) log.debug "Create Child Devices"
    HashMap devMap = getDeviceList()
    if(debugEnabled) log.debug "Dev List $devMap"
    //[{"id":"<UUID of the device>","locationId":"<UUID of location>","name":"<name of device>","model":"ONEPLUS","firmwareVersion":"1.4.5","createdDate":1563568617,"hasOwner":true,"hasWrite":true}]
//    devMap.each {
        it = devMap
        if(debugEnabled) log.debug "${it.properties}"
        if(debugEnabled) log.debug "add child device ${it.id}"
        devDetail = getDevDetail("$it.id")        
        dev = addChildDevice("thebearmay", "Daikin One Thermostat", "${it.id}", [name:"$it.name",label:"$it.name",model:"$it.model", firmware:"$it.firmwareVersion", isComponent:false])
        updateChild("$it.id", "C")
//    }
}

void updateChild(id, cOrF) {
 /*
"tempSPMin":10,
"tempSPMax":32,
"tempDeltaMin":2.2,
"tempOutdoor":29,
"tempIndoor":24.9,
"mode":3,
"fanCirculateSpeed":0,
"fanCirculate":0,
"fan":false,
"hspSched":20,

“mode”: 2 is cool, 3 is auto, 1 is heat, 0 is off, emergency heat is 4
“tempIndoor”: in C
“tempOutdoor”: in C
“humIndoor”: in %
“humOutdoor”: in %
“weatherDay[1-5]TempC”: forecast of the temps for days 1-5 (ex. weatherDay1TempC)
“weatherDay[1-5]Icon”: tstorms, partlycloudy, (these are all I have right now)
“weatherDay[1-5]Cond”: text description of conditions
“weatherDay[1-5]Hum”: humidity forecast
“fanCirculate”: 0=off, 1=always on, 2=schedule, manual fan control
*/
    modeStr=["off","heat","cool","auto","emergency heat"]
    circStr=["auto","on","circulate"]
  
    devDetail = getDevDetail("$id")
    dev = getChildDevice("$id")
    degUnit = "°C"
    if(cOrF == "F") {
        devDetail.tempDeltaMin = celsiusToFahrenheit(devDetail.tempDeltaMin.toFloat()).toFloat().round(0)
        devDetail.tempSPMin = celsiusToFahrenheit(devDetail.tempSPMin.toFloat()).toFloat().round(0)
        devDetail.hspHome = celsiusToFahrenheit(devDetail.hspHome.toFloat()).toFloat().round(0)
        devDetail.cspHome = celsiusToFahrenheit(devDetail.cspHome.toFloat()).toFloat().round(0)
        devDetail.tempSPMax = celsiusToFahrenheit(devDetail.tempSPMax.toFloat()).toFloat().round(0)
        devDetail.tempIndoor = celsiusToFahrenheit(devDetail.tempIndoor.toFloat()).toFloat().round(0)
        devDetail.tempOutdoor = celsiusToFahrenheit(devDetail.tempOutdoor.toFloat()).toFloat().round(0)
        degUnit = "°F"
    }
    dev.updateAttr("thermostatMode",modeStr[devDetail.mode.toInteger()])
    dev.updateAttr("fan",devDetail.fan)
    dev.updateAttr("thermostatFanMode",circStr[devDetail.fanCirculate.toInteger()])
    dev.updateAttr("fanCirculateSpeed",devDetail.fanCirculateSpeed)
    dev.updateAttr("setpointDelta",devDetail.tempDeltaMin,degUnit)
    dev.updateAttr("setpointMinimum",devDetail.tempSPMin,degUnit)
    dev.updateAttr("heatingSetpoint",devDetail.hspHome,degUnit)
    dev.updateAttr("coolingSetpoint",devDetail.cspHome,degUnit)
    if(devDetail.mode == 1) 
        dev.updateAttr("thermostatSetpoint",devDetail.hspSched,degUnit)
    else if(devDetail.mode == 2)
        dev.updateAttr("thermostatSetpoint",devDetail.cspSched,degUnit)
    dev.updateAttr("setpointMaximum",devDetail.tempSPMax,degUnit)
    dev.updateAttr("temperature",devDetail.tempIndoor,degUnit)
    dev.updateAttr("tempOutdoor",devDetail.tempOutdoor,degUnit)
    dev.updateAttr("humidity",devDetail.humIndoor,"%")
    dev.updateAttr("humidOutdoor",devDetail.humOutdoor,"%") 
}

void sendPut(command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "$serverPath$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        headers: [
            'Accept': 'application/json',
            "Authorization" : "Bearer $authToken"
        ],
        body: "$bodyText"
	]

    
    if(debugEnabled) log.debug "$requestParams"
    httpPut(requestParams) {resp ->
    }
}

@SuppressWarnings('unused')
void uninstalled(){
    devlist = getChildDevices()
    devlist.each{
       deleteChildDevice(it.deviceNetworkId)
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
