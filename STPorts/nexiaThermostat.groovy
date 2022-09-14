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
 * 
 */
metadata {
    definition (name: "Nexia Thermostat", namespace: "trentfoley", author: "Trent Foley") {
        capability "Actuator"
        capability "Temperature Measurement"
        capability "Thermostat"
        capability "Relative Humidity Measurement"
        capability "Polling"
        capability "Sensor"
        capability "Refresh"
        command "setTemperature"

        attribute "activeMode", "string"
        attribute "outdoorTemperature", "number"
    }

// parse events into attributes
def parse(String description) {
    log.debug "parse('${description}')"
}

// Implementation of capability.refresh
def refresh() {
    log.debug "refresh()"
    poll()
}

// Implementation of capability.polling
def poll() {
    log.debug "poll()"
    def data = parent.pollChild(this)

    if(data) {
        sendEvent(name: "temperature", value: data.temperature, unit: "F")
            sendEvent(name: "heatingSetpoint", value: data.heatingSetpoint, unit: "F")
            sendEvent(name: "coolingSetpoint", value: data.coolingSetpoint, unit: "F")
            sendEvent(name: "thermostatSetpoint", value: data.thermostatSetpoint, unit: "F")
            sendEvent(name: "thermostatMode", value: data.thermostatMode)
            sendEvent(name: "thermostatFanMode", value: data.thermostatFanMode)
            sendEvent(name: "thermostatOperatingState", value: data.thermostatOperatingState)
            sendEvent(name: "humidity", value: data.humidity, unit: "%")
            sendEvent(name: "activeMode", value: data.activeMode)
            sendEvent(name: "outdoorTemperature", value: data.outdoorTemperature, unit: "F")
    } else {
        log.error "ERROR: Device connection removed? No data found for ${device.deviceNetworkId} after polling"
    }
}

def setTemperature(degreesF) {
    log.debug "setTemperature(${degreesF})"
    def delta = degreesF - device.currentValue("temperature")
    log.debug "Determined delta to be ${delta}"

    if (device.currentValue("activeMode") == "cool") {
        setCoolingSetpoint(device.currentValue("coolingSetpoint") + delta)
    } else {
        setHeatingSetpoint(device.currentValue("heatingSetpoint") + delta)
    }
}

// Implementation of capability.thermostat
def setHeatingSetpoint(degreesF) {
    log.debug "setHeatingSetpoint(${degreesF})"
    sendEvent(name: "heatingSetpoint", value: degreesF, unit: "F")
    sendEvent(name: "thermostatSetpoint", value: degreesF, unit: "F")
    parent.setHeatingSetpoint(this, degreesF)
}

// Implementation of capability.thermostat
def setCoolingSetpoint(degreesF) {
    log.debug "setCoolingSetpoint(${degreesF})"
    sendEvent(name: "coolingSetpoint", value: degreesF, unit: "F")
    sendEvent(name: "thermostatSetpoint", value: degreesF, unit: "F")
    parent.setCoolingSetpoint(this, degreesF)
}

// Implementation of capability.thermostat
// Valid values are: "auto" "emergency heat" "heat" "off" "cool"
def setThermostatMode(String mode) {
    log.debug "setThermostatMode(${mode})"
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
    log.debug "setThermostatFanMode(${fanMode})"
    sendEvent(name: "thermostatFanMode", value: fanMode)
    parent.setThermostatFanMode(this, fanMode)
}

// Implementation of capability.thermostat
def fanOn() { setThermostatFanMode("on") }

// Implementation of capability.thermostat
def fanAuto() { setThermostatFanMode("auto") }

// Implementation of capability.thermostat
def fanCirculate() { setThermostatFanMode("circulate") }
