/*
 * Daikin One Open Master 
 *
 * API document: https://synaccess.com/support/webapi#table-of-contents
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

@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "Daikin One Master", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/daikin/daikinMaster.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"


    }   
}

preferences {
    input("serverPath", "text", title:"Daikin Server Path:", required: true, submitOnChange:true, defaultValue: "https://integrator-api.daikinskyport.com")
    input("token", "text", title:"Daikin Integrator Token:", required: true, submitOnChange:true)
    input("regEmail", "text", title:"Email registered with Daiken:", required: true, submitOnChange:true)

    input("debugEnabled", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "${device.displayLabel} v${version} installed()"
    initialize()
    createChildDevices()
}

def initialize(){

}

@SuppressWarnings('unused')
def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

String getAuth() {
    def bodyText = JsonOutput.toJson([email:"$regEmail", integratorToken:"$token"])
    Map requestParams =
	[
        uri:  "$serverPath/v1/token",
        requestContentType: 'application/json',
		contentType: 'application/json',
        headers: [
            "x-api-key": "$token",
        ],
        body: "$bodyText"
	]

    authKey=""
    if(debugEnabled) log.debug "$requestParams"
    httpPost(requestParams) { resp ->
        jsonData = (HashMap) resp.JSON
        authKey = jsonData.accessToken
    }
    return authKey                                 
}
    
HashMap getDeviceList(){
    HashMap devMap = sendGet("/v1/devices")
    return devMap
}

HashMap getDevDetail(id) {
    HashMap devDetail = sendGet("/v1/devices/$id")
    return devDetail
}

HashMap sendGet(command){
    authToken = getAuth()
    if(debugEnabled) log.debug "sendGet token:$authToken"
    Map requestParams =
	[
        uri:  "$serverPath$command",
        requestContentType: 'application/json',
		contentType: 'application/json',
        headers: [
            "x-api-key": "$token",
            "Authorization" : "Bearer $authToken"
        ]
	]

    if(debugEnabled) log.debug "get parameters $requestParams"
    httpGett(requestParams) { resp ->
        jsonData = (HashMap) resp.JSON
    }
    if(debugEnabled) log.debug "get JSON $jsonData"
    return jsonData
                            
}

void createChildDevices(){
    if(debugEnabled) log.debug "Create Child Devices"
    HashMap devMap = getDeviceList()
    devMap.devices.each {
        if(debugEnabled) "add child device ${it.id}"
        devDetail = getDevDetail("$it.id")        
        dev = addChildDevice("thebearmay", "Daikin Thermostat", "DaikenChild:${it.id}", [name:"$it.id",label:"$it.name",model:"$it.model", firmware:"$it.firmware", isComponent:true])
        updateChild("$it.id")

    }
}

void updateChild(id) {
    devDetail = getDevDetail("$id")
    dev = getChildDevice("DaikenChild:${id}")
    dev.updateAttr("equipmentStatus",devDetail.equipmentStatus)
    dev.updateAttr("mode",devDetail.mode)
    dev.updateAttr("modeLimit",devDetail.modeLimit)
    dev.updateAttr("modeEmHeatAvailable",devDetail.modeEmHeatAvailable)
    dev.updateAttr("fan",devDetail.fan)
    dev.updateAttr("fanCirculate",devDetail.fanCirculate)
    dev.updateAttr("fanCirculateSpeed",devDetail.fanCirculateSpeed)
    dev.updateAttr("heatSetpoint",devDetail.heatSetpoint)
    dev.updateAttr("coolSetpoint",devDetail.coolSetpoint)
    dev.updateAttr("setpointDelta",devDetail.setpointDelta)
    dev.updateAttr("setpointMinimum",devDetail.setpointMinimum)
    dev.updateAttr("setpointMaximum",devDetail.setpointMaximum)
    dev.updateAttr("tempIndoor",devDetail.tempIndoor)
    dev.updateAttr("humIndoor",devDetail.humIndoor)
    dev.updateAttr("tempOutdoor",devDetail.tempOutdoor)
    dev.updateAttr("humOutdoor",devDetail.humOutdoor)
    dev.updateAttr("scheduleEnabled",devDetail.scheduleEnabled)
    dev.updateAttr("geofencingEnabled",devDetail.geofencingEnabled)

}

void sendPut(command, bodyMap){
    def bodyText = JsonOutput.toJson(bodyMap)
	Map requestParams =
	[
        uri:  "$remoteAPI$command?access_token=$token",
        requestContentType: 'application/json',
		contentType: 'application/json',
        headers: [
            "x-api-key": "$token",
            "Authorization" : "Bearer $authToken"
        ],
        body: "$bodyText"
	]

    
    if(debugEnabled) log.debug "$requestParams"
    httpPut(requestParams) {resp ->
    }
}



@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
