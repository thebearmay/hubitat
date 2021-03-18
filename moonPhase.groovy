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
 *                               Calc corrections, add alternate input stream
 *    2021-03-18  thebearmay     Add an tile attribute, and icon path override
 *                               add scheduled update at midnight + 1 second
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.4.0'  }

metadata {
    definition (
		name: "Moon Phase", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy"
	) {
        capability "Actuator"
        
        attribute "moonPhase", "string"
		attribute "moonPhaseNum", "number"
		attribute "lastQryDate", "string"
        attribute "moonPhaseTile", "string"
        
        
        command "getPhase"
        command "calcPhase", [[name:"dateStr", type:"STRING", description:"Date (yyyy-MM-dd HH:mm:ss) to calculate the moon phase for."]]              
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("autoUpdate", "bool", title: "Enable automatic update at midnight")
    input("iconPathOvr", "string", title: "Alternate path to moon phase icons \n(must contain file names moon-phase-icon-0 through moon-phase-icon-7)")
}

def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
}

def calcPhase (dateStr){
    cDate = dateCheck(dateStr)
    if (cDate !=0) getPhase(cDate)

}

def dateCheck(dateStr) {
    try {
        cDate = Date.parse("yyyy-MM-dd HH:mm:ss",dateStr)
        return cDate.getTime()
    } catch (Exception e) {
        updateAttr("error", "Invalid date string use format yyyy-MM-dd HH:mm:ss")
        return 0
    }
}

def getPhase(cDate = now()) {
    refDate = Date.parse("yyyy-MM-dd HH:mm:ss","2000-01-06 18:14:00") //First New Moon of 2000
    refDate = refDate.getTime()

    lunarDays = 29.53058770576
    lunarSecs = lunarDays*8640000

    sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    Double phaseWork = cDate - refDate //subtract out first new moon of 2000 to get elapsed seconds
    phaseWork = phaseWork/lunarSecs/10 //calculate lunar cycles
    
    phaseWork = phaseWork - phaseWork.toInteger() //remove whole cycles
    phaseWork = phaseWork.round(2)
    
    if(phaseWork== 1.0) phaseWork = 0.0
    
	updateAttr("moonPhaseNum", phaseWork)
    updateAttr("lastQryDate",sdf.format(cDate))
    
    iconPath = "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    
    if (phaseWork == 0){
        imgNum = 0
		phaseText = "New Moon"
    }else if (phaseWork < 0.25){
        imgNum = 1
		phaseText = "Waxing Crescent" 
    }else if (phaseWork == 0.25){
        imgNum = 2
        phaseText = "First Quarter"
    }else if (phaseWork < 0.5){
        imgNum = 3
		phaseText =  "Waxing Gibbous"	
    }else if (phaseWork == 0.5){
        imgNum = 4
		phaseText =  "Full Moon" 	
    }else if (phaseWork < 0.75){
        imgNum = 5
		phaseText = "Waning Gibbous"	
    }else if (phaseWork == 0.75){
        imgNum = 6
		phaseText = "Last Quarter" 
    }else if (phaseWork < 1){
        imgNum = 7
		phaseText = "Waning Crescent"
    }else {
        phaseText = "Error - Out of Range"
        imgNum = ""
    }
    
    updateAttr("moonPhase", phaseText)
    phaseIcon = "<div id='moonTile'><img class='moonPhase' src='${iconPath}moon-phase-icon-${imgNum}.png'><p class='small' style='text-align:center'>$phaseText</p></img></div>"
    updateAttr("moonPhaseTile",phaseIcon)
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
    if(autoUpdate) schedule("1 0 0 ? * * *", getPhase)
    else unschedule(getpPhase)
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
