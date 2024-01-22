/**
 *	Nexia Thermostat
 *
 *	Author: Trent Foley
 *	Date: 2016-01-25
 *
 *
 * ** Modifications **
 *  Date        Who          Description
 *  __________  __________   ____________________________________________________________________
 *  2022-09-14  thebearmay   port over to Hubitat
 *  2022-09-16	thebearmay   Fix thermostatOperatingState
 *  2022-10-04  thebearmay   Add permanent hold and return to schedule
 *  2024-01-21  thebearmay   Add supportedThermostatModes
 */
static String version()	{  return '1.0.3'  }

metadata {
    definition (name: "Nexia Thermostat", 
                namespace: "trentfoley", 
                author: "Trent Foley",
                importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/STPorts/nexiaThermostat.groovy",                
               ) {
        capability "Actuator"
        capability "Temperature Measurement"
        capability "Thermostat"
        capability "Relative Humidity Measurement"
        capability "Polling"
        capability "Sensor"
        capability "Refresh"
        capability "Initialize"	    
        command "setTemperature", [[name:"Temperature*", type:"number", description:"Temperature in Degrees Fahrenheit"]]
        command "setHold", [[name:"holdType", type:"ENUM", constraints:["Permanent Hold","Return to Schedule"]]]

        attribute "activeMode", "string"
        attribute "outdoorTemperature", "number"
        attribute "thermostatOperatingState", "string"
        attribute "holdStatus", "string"
	attribute "supportedThermostatModes", "string"    
    }
}

preferences {
    input "pollInterval", "enum", title:"Enter Poll Cycle", options:['1 Minute','5 Minutes','10 Minutes','15 Minutes','30 Minutes','1 Hour','3 Hours'], submitOnChange:true
    input "debugEnabled", "bool", title: "Enable debug logging?"

}

def updated() {
    if(debugEnabled) log.debug "update $pollInterval"
    unschedule()
    if(debugEnabled) runIn(1800,"logsOff")
    switch(pollInterval){
        case "1 Minute":
			runEvery1Minute("poll")
            break
        case "5 Minutes":
			runEvery5Minutes("poll")
            break
        case "10 Minutes":
			runEvery10Minutes("poll")
            break
        case "15 Minutes":
			runEvery15Minutes("poll")
            break
        case "30 Minutes":
			runEvery30Minutes("poll")
            break
        case "1 Hour":
			runEvery1Hour("poll")
            break
        case "3 Hours":
			runEvery3Hours("poll")
            break	
		default:
			log.error "Invalid Interval Selected $pollInterval"
			break
    }
            
}
def initialize(){
    sendEvent(name:'supportedThermostatModes', value:'["auto", "off", "heat", "emergency heat", "cool"]')
    poll()
}

// parse events into attributes
def parse(String description) {
    if(debugEnabled) log.debug "parse('${description}')"
}

// Implementation of capability.refresh
def refresh() {
    if(debugEnabled) log.debug "refresh()"
    poll()
}

// Implementation of capability.polling
def poll() {
    if(debugEnabled) log.debug "poll()"
    def data = parent.pollChild(this)
    if(debugEnabled) log.debug "$data"

    if(data) {
        sendEvent(name: "temperature", value: data.temperature, unit: "°F")
            sendEvent(name: "heatingSetpoint", value: data.heatingSetpoint, unit: "°F")
            sendEvent(name: "coolingSetpoint", value: data.coolingSetpoint, unit: "°F")
            sendEvent(name: "thermostatSetpoint", value: data.thermostatSetpoint, unit: "°F")
            sendEvent(name: "thermostatMode", value: data.thermostatMode)
            sendEvent(name: "thermostatFanMode", value: data.thermostatFanMode)
            sendEvent(name: "thermostatOperatingState", value: data.thermostatOperatingState)
            sendEvent(name: "humidity", value: data.humidity, unit: "%")
            sendEvent(name: "activeMode", value: data.activeMode)
            sendEvent(name: "outdoorTemperature", value: data.outdoorTemperature, unit: "°F")
            sendEvent(name: "holdStatus", value: data.setpointStatus)
    } else {
        log.error "ERROR: Device connection removed? No data found for ${device.deviceNetworkId} after polling"
    }
}

def setTemperature(degreesF) {
    if(debugEnabled) log.debug "setTemperature(${degreesF})"
    def delta = degreesF - device.currentValue("temperature")
    if(debugEnabled) log.debug "Determined delta to be ${delta}"

    if (device.currentValue("activeMode") == "cool") {
        setCoolingSetpoint(device.currentValue("coolingSetpoint") + delta)
    } else {
        setHeatingSetpoint(device.currentValue("heatingSetpoint") + delta)
    }
}

def setHold(hType){
    hType = hType.toLowerCase().replace(" ","_")
    parent.setHoldMode(this, hType)
    if(hType == "permanent_hold")
        sendEvent(name:"holdStatus",value:"Permanent Hold Requested")
    else
        sendEvent(name:"holdStatus",value:"Return to Schedule Requested")
}

// Implementation of capability.thermostat
def setHeatingSetpoint(degreesF) {
    if(debugEnabled) log.debug "setHeatingSetpoint(${degreesF})"
    sendEvent(name: "heatingSetpoint", value: degreesF, unit: "°F")
    sendEvent(name: "thermostatSetpoint", value: degreesF, unit: "°F")
    parent.setHeatingSetpoint(this, degreesF)
}

// Implementation of capability.thermostat
def setCoolingSetpoint(degreesF) {
    if(debugEnabled) log.debug "setCoolingSetpoint(${degreesF})"
    sendEvent(name: "coolingSetpoint", value: degreesF, unit: "°F")
    sendEvent(name: "thermostatSetpoint", value: degreesF, unit: "°F")
    parent.setCoolingSetpoint(this, degreesF)
}

// Implementation of capability.thermostat
// Valid values are: "auto" "emergency heat" "heat" "off" "cool"
def setThermostatMode(String mode) {
    if(debugEnabled) log.debug "setThermostatMode(${mode})"
    sendEvent(name: "thermostatMode", value: mode)
    parent.setThermostatMode(this, mode)
}

// Implementation of capability.thermostat
def off() { setThermostatMode("off") }

// Implementation of capability.thermostat
def heat() { setThermostatMode("heat") }

// Implementation of capability.thermostat
def emergencyHeat() { setThermostatMode("emergency heat") }

// Implementation of capability.thermostat
def cool() { setThermostatMode("cool") }

// Implementation of capability.thermostat
def auto() { setThermostatMode("auto") }

// Implementation of capability.thermostat
// Valid values are: "auto" "on" "circulate"
def setThermostatFanMode(String fanMode) {
    if(debugEnabled) log.debug "setThermostatFanMode(${fanMode})"
    sendEvent(name: "thermostatFanMode", value: fanMode)
    parent.setThermostatFanMode(this, fanMode)
}

// Implementation of capability.thermostat
def fanOn() { setThermostatFanMode("on") }

// Implementation of capability.thermostat
def fanAuto() { setThermostatFanMode("auto") }

// Implementation of capability.thermostat
def fanCirculate() { setThermostatFanMode("circulate") }

void logsOff(){
    device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
