/*
 * Danfoss Master App
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
 *    2022-09-26  thebearmay    add window state
 *    
*/

static String version()	{  return '1.0.1'}

import java.text.SimpleDateFormat
import java.util.Date
import groovy.json.JsonSlurper
import groovy.transform.Field

definition (
	name: 			"Danfoss Master", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Danfoss Integration ",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/danfoss/danfossMstr.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "options"
}



void installed() {
    if(debugEnabled) log.trace "${app.getLabel()} installed()"
    state?.isInstalled = true
    initialize()
}

void updated(){
	if(debugEnabled) log.trace "${app.getLabel()} updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

void initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') { 
            section("") {
                input "apiKey", "text", title: "<b>Danfoss API Key</b>", submitOnChange:true, required:false, width:4
                input "apiSecret", "password", title: "<b>Danfoss API Secret</b>", submitOnChange:true, required:false, width:4
                input "sim", "bool", title: "Use Simulated Values", width:4, submitOnChange:true
                input "debugEnabled", "bool", title: "Enable Debug for 30 Minutes", width:4, submitOnChange:true
                input "checkCred", "button", title: "Check Credentials"
                if(state?.loginCheck == true) {
                    danfossLogin()
                    state.loginCheck = "false"
                }
	            paragraph "Current Token: ${state?.authToken}"
                input "devCheck", "button", title:"Get Devices"
                if(state?.getDev == true){
                    state.getDev = false
                    getDevices()                 
                }
                if(state?.devResp)
                paragraph "${state?.devCount} Devices Processed"
            }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }        
    }
}

def danfossLogin(){
    String encodedString = "$apiKey:$apiSecret".bytes.encodeBase64().toString();
    if(debugEnabled) log.debug "$encodedString"
    try{
        params = [
					uri: "https://api.danfoss.com/oauth2/token",
                    headers: [
                        Authorization: "Basic $encodedString",
                        "Content-Type": "application/x-www-form-urlencoded",
                        Accept: "application/json"
                        ],
					body:[grant_type:"client_credentials"]
				]
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"

            state.authToken = resp.data.access_token
		}
    }catch (e){
        log.error "Error logging in: ${e}"
    }
}

def getDevices() {
    danfossLogin()
    devCount = 0
    try{
        params = [
					uri: "https://api.danfoss.com/ally/devices",
                    headers: [
                        Authorization: "Bearer ${state.authToken}",
                        Accept: "application/json",
                        textParser:false
                        ]

				]
        if(debugEnabled) log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) log.debug "Response: $resp.data"
            state.devResp = resp.data
		}
    }catch (e){
        if(!sim) log.error "Error getting devices: ${e}"
    }
    if(sim) state.devResp = '[  "result": [[   "active_time": 1605086157,   "create_time": 1591615719,   "id": "bff29edfd82316bc2bbrlu",   "name": "Danfoss Ally™ Gateway",   "online": true,   "status": [],   "sub": false,   "time_zone": "+01:00",   "update_time": 1605296207,   "device_type": "Danfoss Ally™ Gateway" }, {   "active_time": 1605087321,   "create_time": 1605086381,   "id": "bf80b9a848085c5902tiw2",   "name": "Icon RT 8",   "online": true,   "status": [  {    "code": "temp_set",    "value": 200  },  {    "code": "mode",    "value": "hot"  }   ],   "sub": true,   "time_zone": "+08:00",   "update_time": 1605482266,   "device_type": "Icon RT" }, {   "active_time": 1605087321,   "create_time": 1605086381,   "id": "bf80b9a848085c5902bear",   "name": "Bear Danfoss",   "online": true,   "status": [  {    "code": "temp_set",    "value": 200  },  {    "code": "mode",    "value": "hot"  }   ],   "sub": true,   "time_zone": "+08:00",   "update_time": 1605482266,   "device_type": "Icon RT" ]],  "t": 1604188800]'
    if(debugEnabled) log.debug state.devResp
    if(state?.devResp != null){
        resMap = state.devResp
        resMap.result.each() {
            if(it.device_type.indexOf('Thermostat') > -1){
                devCount++
                if(debugEnabled) log.debug "${it.name}:${it.device_type}:${it.id}"
                if(!this.getChildDevice("${it.id}"))
                    cd = addChildDevice("thebearmay", "Danfoss Thermostat", "${it.id}", [name: "${it.name}", deviceType:"${it.device_type}", isComponent: true])
                else 
                    cd = this.getChildDevice("${it.id}")
                cd.sendEvent(name:"online",value:"${it.online}")
                it.status.each{
                    if(it.code == "temp_set") {
                        tValue = it.value.toDouble()/10
                        cd.sendEvent(name:"thermostatSetpoint",value:"${tValue}",unit:"°C")
                        cd.sendEvent(name:"heatingSetpoint",value:"${tValue}",unit:"°C") 
                     }
                    if(it.code == "mode") {
                        cd.sendEvent(name:"thermostatMode",value:"${it.value}")     
                    }
                    if(it.code == "battery_percentage") {
                        cd.sendEvent(name:"battery",value:"${it.value}",unit:"%")
                    }
                    if(it.code == "temp_current") {
                        tValue = it.value.toDouble()/10
                        cd.sendEvent(name:"temperature",value:"${tValue}",unit:"°C") 
                    }
                    if(it.code == "window_state") {
                        cd.sendEvent(name:"window_state",value:"${it.value}") 
                    }                    
                }
            }
            if(it.device_type.indexOf('Sensor') > -1){
                devCount++
                if(debugEnabled) log.debug "${it.name}:${it.device_type}:${it.id}"
                if(!this.getChildDevice("${it.id}"))
                    cd = addChildDevice("thebearmay", "Danfoss Sensor", "${it.id}", [name: "${it.name}", deviceType:"${it.device_type}", isComponent: true])
                else 
                    cd = this.getChildDevice("${it.id}")
                cd.sendEvent(name:"online",value:"${it.online}")
                it.status.each{
                    if(it.code == "va_temperature") {
                        tValue = it.value.toDouble()/10
                        cd.sendEvent(name:"temperature",value:"${tValue}",unit:"°C")
                     }
                    if(it.code == "va_humidity") {
                        tValue = it.value.toDouble()/10
                        cd.sendEvent(name:"humidity",value:"${tValue}")     
                    }
                    if(it.code == "battery_percentage") {
                        cd.sendEvent(name:"battery",value:"${it.value}",unit:"%")
                    }
                } 
            }
            
        }
        state.devCount = devCount
    }
}

def updateChild(id, cOrF){
    danfossLogin()
    try{
        params = [
					uri: "https://api.danfoss.com/ally/devices/$id",
                    headers: [
                        Authorization: "Bearer ${state.authToken}",
                        Accept: "application/json"
                        ]

				]
        if(debugEnabled) log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) log.debug "$resp.data"
            state.devResp = resp.data
		}
    }catch (e){
        if(!sim) log.error "Error getting devices: ${e}"
    }
    if(sim) state.devResp = '{ "result": { "active_time": 1605087321, "create_time": 1605086381, "id": "bf80b9a848085c5902tiwi", "name": "Icon RT 8", "online": true, "status": [  {  "code": "temp_set",  "value": 201  },  {  "code": "mode",  "value": "hot"  } ], "sub": true, "time_zone": "+08:00", "update_time": 1605482266, "device_type": "Icon RT" }, "t": 1604188800  }'
    log.debug state.devResp
    if(state?.devResp != null){
        respWork = state.devResp.toString()
        if( respWork.substring(0,1) == '['){
            respWork = '{'+respWork.substring(1,respWork.length()-1)+'}'
            if (debugEnabled) log.debug respWork
        }
        def jSlurp = new JsonSlurper()
        Map resMap = (Map)jSlurp.parseText(respWork)
        cd = this.getChildDevice("${id}")
        cd.sendEvent(name:"online",value:"${resMap.result.online}")
        resMap.result.status.each{
            if(it.code == "temp_set") {
                if(debugEnabled) log.debug cOrF
                if(cOrF == "F")
                    tempValue = celsiusToFahrenheit(it.value.toFloat()/10).toFloat().round(0)
                else
                    tempValue = it.value.toFloat()/10
                cd.sendEvent(name:"thermostatSetpoint",value:"${tempValue}",unit:"°cOrF")
                cd.sendEvent(name:"heatingSetpoint",value:"${tempValue}",unit:"°cOrF")   
            }
            if(it.code == "mode") {
                cd.sendEvent(name:"thermostatMode",value:"${it.value}")     
            }
            if(it.code == "battery_percentage") {
                cd.sendEvent(name:"battery",value:"${it.value}",unit:"%")
            } 
            if(it.code == "va_temperature" || it.code == "temp_current"){
                if(debugEnabled) log.debug cOrF
                if(cOrF == "F")
                    tempValue = celsiusToFahrenheit(it.value.toFloat()/10).toFloat().round(0)
                else
                    tempValue = it.value.toFloat()/10
                cd.sendEvent(name:"temperature",value:"${tempValue}",unit:"°cOrF")
            }
            if(it.code == "va_humidity") {
                tValue = it.value.toFloat()/10
                cd.sendEvent(name:"humidity",value:"${tValue}",unit:"%")
            }
            if(it.code == "window_state") {
                cd.sendEvent(name:"window_state",value:"${it.value}") 
            }              
        }
    }    
}

def sendCmd(devId,cmd,val){
    danfossLogin()
    cLine = '{'+"\"commands\":[{\"code\":\"$cmd\",\"value\":\"$val\"}]"+'}'
    try{
        params = [
					uri: "https://api.danfoss.com/ally/devices/$devId/commands",
                    headers: [
                        Authorization: "Bearer ${state.authToken}",
                        Accept: "application/json",
                        "Content-Type":"application/json",
                        textParser:false
                        ],
                    body:"$cLine"
				]    
        if(debugEnabled) log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) "$resp.data"
        }
    } catch (e){
        log.error "Error on command send: ${e} ${e.getResponse().data}"
    }
}

def appButtonHandler(btn) {
    switch(btn) {
        case "checkCred":
            state.loginCheck = true
            break
        case "devCheck":
            state.getDev = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}
