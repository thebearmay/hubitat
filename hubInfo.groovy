 /*
 * Hub Info
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
 *    Date        Who            What
 *    ----        ---            ----
 *    2020-12-07  thebearmay	 Original version 0.1.0
 *    2021-01-30  thebearmay     Add full hub object properties
 *    2021-01-31  thebearmay     Code cleanup, release ready
 *    2021-01-31  thebearmay     Putting a config delay in at initialize to make sure version data is accurate
 *    2021-02-16  thebearmay     Add text date for restart
 *    2021-03-04  thebearmay     Added CPU and Temperature polling 
 *    2021-03-05  thebearmay     Merged CSteele Add the degree symbol and scale to the temperature attribute 
 *    2021-03-05  thebearmay	 Merged addtions from LGKhan: Added new formatted uptime attr, also added an html attr that stores a bunch of the useful 
 *					                info in table format so you can use on any dashboard
 */
import java.text.SimpleDateFormat
static String version()	{  return '1.4.4'  }

metadata {
    definition (
		name: "Hub Information", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importURL:"https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfo.groovy"
	) {
        capability "Actuator"
	capability "Initialize"
	capability "TemperatureMeasurement"
        
	attribute "latitude", "string"
	attribute "longitude", "string"
        attribute "hubVersion", "string"
        attribute "id", "string"
        attribute "name", "string"
        attribute "data", "string"
        attribute "zigbeeId", "string"
        attribute "zigbeeEui", "string"
        attribute "hardwareID", "string"
        attribute "type", "string"
        attribute "localIP", "string"
        attribute "localSrvPortTCP", "string"
        attribute "uptime", "string"
        attribute "lastUpdated", "string"
        attribute "lastHubRestart", "string"
	    attribute "firmwareVersionString", "string"
        attribute "timeZone", "string"
        attribute "temperatureScale", "string"
        attribute "zipCode", "string"
        attribute "locationName", "string"
        attribute "locationId", "string"
        attribute "lastHubRestartFormatted", "string"
        attribute "freeMemory", "string"
	    attribute "temperatureF", "string"
        attribute "temperatureC", "string"
        attribute "formattedUptime", "string"
        attribute "html", "string";                              
   
	command "configure"
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("tempPollEnable", "bool", title: "Enable Temperature/Memory/html Polling")
    input("tempPollRate", "number", title: "Temperature/Memory Polling Rate (seconds)\nDefault:300", default:300, submitOnChange: true)
    input("attribEnable", "bool", title: "Enable Info attribute?", default: false, required: false, submitOnChange: true)
}

def installed() {
	log.trace "installed()"
}

def configure() {
    //if(debugEnable) 
    log.debug "configure()"
    locProp = ["latitude", "longitude", "timeZone", "zipCode", "temperatureScale"]
    def myHub = location.hub
    hubProp = ["id","name","data","zigbeeId","zigbeeEui","hardwareID","type","localIP","localSrvPortTCP","firmwareVersionString","uptime"]
    for(i=0;i<hubProp.size();i++){
        updateAttr(hubProp[i], myHub["${hubProp[i]}"])
    }
    for(i=0;i<locProp.size();i++){
        updateAttr(locProp[i], location["${locProp[i]}"])
    }
    formatUptime()
    updateAttr("hubVersion", location.hub.firmwareVersionString) //retained for backwards compatibility
    updateAttr("locationName", location.name)
    updateAttr("locationId", location.id)
    updateAttr("lastUpdated", now())
    if (tempPollEnable) getTemp()
    if (attribEnable) formatAttrib()
}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def initialize(){
    log.trace "Hub Information initialize()"
// psuedo restart time - can also be set at the device creation or by a manual initialize
    restartVal = now()
    updateAttr("lastHubRestart", restartVal)	
    sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    updateAttr("lastHubRestartFormatted",sdf.format(restartVal))
    runIn(30,configure)
}

def formatUptime(){
  String attrval 

    Integer ut = device.currentValue("uptime").toDouble()
    Integer days = (ut/(3600*24))
    Integer hrs = (ut - (days * (3600*24))) /3600
    Integer min =  (ut -  ((days * (3600*24)) + (hrs * 3600))) /60
    Integer sec = ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))
    
    attrval = days.toString() + " days, " + hrs.toString() + " hours, " + min.toString() + " minutes and " + sec.toString() + " seconds."
    sendEvent(name: "formattedUptime", value: attrval, isChanged: true) 
}

def formatAttrib(){ 
    if(debubEnable) log.debug "formatAttrib"
   state.attrString = "<table>"

   def currentState = state.attrString
   def result1 = addToAttr("Name","name")
   def result2 = addToAttr("Version","hubVersion")
   def result3 = addToAttr("Address","localIP")
   def result4 = addToAttr("Free Memory","freeMemory","int")
   def result5 = addToAttr("Last Restart","lastHubRestartFormatted")
   def result6 = addToAttr("Uptime","formattedUptime")
   def tempAttrib = "temperatureC"
 
   if (location.temperatureScale == "F") 
      tempAttrib = "temperatureF"
   
    result7 = addToAttr("Temperature",tempAttrib)
    
    state.attrString = currentState + result1 + result2 + result3 + result4 + result5 + result6 + result7 + "</table>"
   
    if (enableDebug) log.debug "after calls attr string = $state.attrString"
    sendEvent(name: "html", value: state.attrString, isChanged: true)
}

def addToAttr(String name, String key, String convert = "none")
{
   // log.debug "adding $name, $key"
    String retResult
    retResult = '<Tr><td align="left">'
    retResult = retResult + name + '</td><td space="5"> </td><td align="left">'
    String attrval 
    
    if (convert == "int")
    {      
    Integer temp = device.currentValue(key).toDouble()
    attrval = temp.toString()
    }
    
   else attrval = device.currentValue(key)
    
    retResult = retResult + attrval
    retResult = retResult + '</td></tr>'
  
    retResult
}
    
def getTemp(){
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/internalTempCelsius"
    ]
    if(debugEnable)log.debug params
    httpGet(params, {response -> 
        if(debugEnable) {
            response.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug response.data
        }
        tempWork = new Double(response.data.toString())
        if(debugEnable) log.debug tempWork
        if (location.temperatureScale == "F")
            sendEvent(name:"temperature",value:celsiusToFahrenheit(tempWork),unit:"°${location.temperatureScale}")
        else
            sendEvent(name:"temperature",value:tempWork,unit:"°${location.temperatureScale}")
        updateAttr("temperatureF",celsiusToFahrenheit(tempWork)+ "<span class='small'> °F</span>")
        updateAttr("temperatureC",tempWork+ "<span class='small'> °C</span>")
    })
    
    // get Free Memory
    params = [
        uri: "http://${location.hub.localIP}:8080",
        path:"/hub/advanced/freeOSMemory"
    ]
    if(debugEnable)log.debug params
    httpGet(params, {response -> 
        if(debugEnable) {
            response.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug response.data
        }
        memWork = new Double(response.data.toString())
        if(debugEnable) log.debug memWork
        
            updateAttr("freeMemory",memWork)
    })
	
    updateAttr("uptime", location.hub.uptime)
	formatUptime()
    
    if(tempPollRate == null)  device.updateSetting("tempPollRate",[value:300,type:"number"])
      
    if (attribEnable) formatAttrib()
    if (debugEnable) log.debug tempPollRate
    if (tempPollEnable) runIn(tempPollRate,getTemp)
}


def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
