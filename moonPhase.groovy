/*
 * Moon Phase
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
 *    2021-03-17  thebearmay	 Original version 0.1.0
 */

static String version()	{  return '0.1.0'  }

metadata {
    definition (
		name: "Moon Phase", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy"
	) {
        capability "Actuator"
        capability "Configuration"
        attribute "moonPhase", "string"
		attribute "moonPhaseNum", "number"
		attribute "lastQryDate", "string"
        
        
        command "getPhase"
            
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
}

def getPhase(){
    def Long referenceDate = 1611861360000                    //UTC for 19:16 01Jan21
    def Long phaseTime = 254880000                            //# seconds in moon phase

	cDate = new Date().getTime()
	phaseWork = (cDate.toLong() - referenceDate)/phaseTime
	phaseWork = phaseWork - phaseWork.toInteger()
	updateAttr("moonPhaseNum", phaseWork)
    updateAttr("lastQryDate",now())
    
    if (phaseWork == 0){
		updateAttr("moonPhase", "New Moon")
    }else if (phaseWork < 0.25){
		updateAttr("moonPhase", "Waxing Crescent") 
    }else if (phaseWork == 0.25){
        updateAttr("moonPhase", "First Quarter")
    }else if (phaseWork < 0.5){
		updateAttr("moonPhase", "Waxing Gibbous") 		
    }else if (phaseWork == 0.5){
		updateAttr("moonPhase", "Full Moon") 	
    }else if (phaseWork < 0.75){
		updateAttr("moonPhase", "Waning Gibbous") 	
    }else if (phaseWork == 0.75){
		updateAttr("moonPhase", "Last Quarter") 
    }else if (phaseWork < 1){
		updateAttr("moonPhase", "Waning Crescent")
    }else updateAttr("moonPhase", "Error - Out of Range")

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
