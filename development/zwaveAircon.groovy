/**
 *  Adapted from the 2015 SmartThings Z-Wave Thermostat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Z-Wave Aircon", namespace: "thebearmay", author: "thebearmay") {
		capability "Actuator"
		capability "Temperature Measurement"
//		capability "Thermostat"
//		capability "Thermostat Heating Setpoint"
		capability "Thermostat Cooling Setpoint"
		capability "Thermostat Operating State"
		capability "Thermostat Mode"
		capability "Thermostat Fan Mode"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"
		
		attribute "thermostatFanState", "string"

		command "switchMode"
		command "switchFanMode"
        command "setFanSpeed", [[name:"fanMode", type:"ENUM", constraints:["off","auto","low","medium","high"]]]
//		command "lowerHeatingSetpoint"
//		command "raiseHeatingSetpoint"
		command "lowerCoolSetpoint"
		command "raiseCoolSetpoint"
		command "poll"

	}
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
}

def installed() {
	// Configure device
	def cmds = [new hubitat.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format()),
			new hubitat.device.HubAction(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())]
	sendHubCommand(cmds)
	runIn(3, "initialize", [overwrite: true])  // Allow configure command to be sent and acknowledged before proceeding
}

def updated() {
	// If not set update ManufacturerSpecific data
	if (!getDataValue("manufacturer")) {
		sendHubCommand(new hubitat.device.HubAction(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()))
		runIn(2, "initialize", [overwrite: true])  // Allow configure command to be sent and acknowledged before proceeding
	} else {
		initialize()
	}
    unschedule("logsOff")
    if(debugEnabled) runIn(1800,"logsOff")
}

def initialize() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	unschedule()
//	if (getDataValue("manufacturer") != "Honeywell") {
//		runEvery5Minutes("poll")  // This is not necessary for Honeywell Z-wave, but could be for other Z-wave thermostats
//	}
	pollDevice()
}

def parse(String description)
{
    if(debugEnabled) log.debug "Parse: $description"
	def result = null
	if (description == "updated") {
	} else {
		def zwcmd = zwave.parse(description, [0x42:1, 0x43:2, 0x31: 3, 0x44: 4])//0x43 Setpoint, 0x31 Sensor Multi
		if (zwcmd) {
			result = zwaveEvent(zwcmd)
		} else {
			if(debugEnabled) log.debug "$device.displayName couldn't parse $description"
		}
	}
    if(debugEnabled) log.debug "Parse Result: $result"
	if (!result) {
		return []
	}
	return [result]
}

// Event Generation
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd) {
    if(debugEnabled) log.debug "ZWEvt SetpointReport: $cmd"
	def cmdScale = cmd.scale == 1 ? "F" : "C"
	def setpoint = getTempInLocalScale(cmd.scaledValue, cmdScale)
	def unit = getTemperatureScale()
	switch (cmd.setpointType) {
		case 1:
//			sendEvent(name: "heatingSetpoint", value: setpoint, unit: unit, displayed: false)
//			updateThermostatSetpoint("heatingSetpoint", setpoint)
			break;
		case 2:
			sendEvent(name: "coolingSetpoint", value: setpoint, unit: unit, displayed: false)
			updateThermostatSetpoint("coolingSetpoint", setpoint)
			break;
		default:
			if(debugEnabled) log.debug "unknown setpointType $cmd.setpointType"
			return
	}
	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
	// Make sure return value is not result from above expresion
	return 0
}

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv3.SensorMultilevelReport cmd) {
    if(debugEnabled) log.debug "ZWEvt SensorMulti Report: $cmd"
	def map = [:]
	if (cmd.sensorType == 1) {
		map.value = getTempInLocalScale(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C")
		map.unit = getTemperatureScale()
		map.name = "temperature"
		updateThermostatSetpoint(null, null)
	} else if (cmd.sensorType == 5) {
		map.value = cmd.scaledSensorValue
		map.unit = "%"
		map.name = "humidity"
	}
    if(debugEnabled) log.debug "ZWEvt SensorMulti Report map: $map"
	sendEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd) {
    if(debugEnabled) log.debug "ZWEvt OpStateRpt: $cmd"
	def map = [name: "thermostatOperatingState"]
	switch (cmd.operatingState) {
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_COOLING:
			map.value = "cooling"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_FAN_ONLY:
			map.value = "fan only"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
			break
	}
    if(debugEnabled) log.debug "ZWEvt OpStateRpt map: $map"
	// Makes sure we have the correct thermostat mode
	sendHubCommand(new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeGet().format()))
	sendEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
    if(debugEnabled) log.debug "ZWEvt FanStateRpt: $cmd"
	def map = [name: "thermostatFanState", unit: ""]
	switch (cmd.fanOperatingState) {
		case 0:
			map.value = "idle"
			break
		case 1:
			map.value = "running"
			break
		case 2:
			map.value = "running high"
			break
	}
    if(debugEnabled) log.debug "ZWEvt FanStateRpt map: $map"
	sendEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
    if(debugEnabled) log.debug "ZWEvt ModeRpt: $cmd"
	def map = [name: "thermostatMode", data:[supportedThermostatModes: state.supportedModes]]
	switch (cmd.mode) {
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
//			map.value = "heat"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
//			map.value = "emergency heat"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
			break
	}
    if(debugEnabled) log.debug "ZWEvt ModeRpt map: $map"
	sendEvent(map)
	updateThermostatSetpoint(null, null)
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
    if(debugEnabled) log.debug "ZWEvt FanModeRpt: $cmd"
	def map = [name: "thermostatFanMode", data:[supportedThermostatFanModes: state.supportedFanModes]]
	switch (cmd.fanMode) {
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:
			map.value = "auto"
			break	
        case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW:
			map.value = "low"
			break
        case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_MEDIUM:
			map.value = "auto"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_MEDIUM:
			map.value = "medium"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_HIGH:
			map.value = "auto"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_HIGH:
			map.value = "high"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:
			map.value = "circulate"
			break
	}
    if(debugEnabled) log.debug "ZWEvt FanModeRpt map: $map"
	sendEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
    if(debugEnabled) log.debug "ZWEvt ModeSuptRpt: $cmd"
	def supportedModes = []
	if(cmd.off) { supportedModes << "off" }
	//if(cmd.heat) { supportedModes << "heat" }
	if(cmd.cool) { supportedModes << "cool" }
	if(cmd.auto) { supportedModes << "auto" }
	//if(cmd.auxiliaryemergencyHeat) { supportedModes << "emergency heat" }

	state.supportedModes = supportedModes
    if(debugEnabled) log.debug "ZWEvt ModeSupRpt modes: $supportedModes"
	sendEvent(name: "supportedThermostatModes", value: supportedModes, displayed: false)
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
    if(debugEnabled) log.debug "ZWEvt FanModeSuptRpt: $cmd"
	def supportedFanModes = []
	if(cmd.auto) { supportedFanModes << "auto" }
	if(cmd.circulation) { supportedFanModes << "circulate" }
	if(cmd.low) { supportedFanModes << "low" }
	if(cmd.medium) { supportedFanModes << "medium" }
	if(cmd.high) { supportedFanModes << "high" }    

	state.supportedFanModes = supportedFanModes
    if(debugEnabled) log.debug "ZWEvt FanModeSuptRpt modes: $supportedFanModes"
	sendEvent(name: "supportedThermostatFanModes", value: supportedFanModes, displayed: false)
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    if(debugEnabled) log.debug "ZWEvt ManfSpec: $cmd"
	if (cmd.manufacturerName) {
		updateDataValue("manufacturer", cmd.manufacturerName)
	}
	if (cmd.productTypeId) {
		updateDataValue("productTypeId", cmd.productTypeId.toString())
	}
	if (cmd.productId) {
		updateDataValue("productId", cmd.productId.toString())
	}
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	if(debugEnabled) log.debug "Zwave BasicReport: $cmd"
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	log.warn "Unexpected zwave command $cmd"
}

// Command Implementations
def poll() {
	// Call refresh which will cap the polling to once every 2 minutes
	refresh()
}

def refresh() {
	// Only allow refresh every 4 minutes to prevent flooding the Zwave network
	def timeNow = now()
	if (!state.refreshTriggeredAt || (4 * 60 * 1000 < (timeNow - state.refreshTriggeredAt))) {
		state.refreshTriggeredAt = timeNow
		if (!state.longRefreshTriggeredAt || (48 * 60 * 60 * 1000 < (timeNow - state.longRefreshTriggeredAt))) {
			state.longRefreshTriggeredAt = timeNow
			// poll supported modes once every 2 days: they're not likely to change
			runIn(10, "longPollDevice", [overwrite: true])
		}
		// use runIn with overwrite to prevent multiple DTH instances run before state.refreshTriggeredAt has been saved
		runIn(2, "pollDevice", [overwrite: true])
	}
}

def pollDevice() {
	def cmds = []
	cmds << new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeGet().format())
	cmds << new hubitat.device.HubAction(zwave.thermostatFanModeV3.thermostatFanModeGet().format())
	cmds << new hubitat.device.HubAction(zwave.sensorMultilevelV2.sensorMultilevelGet().format()) // current temperature
	cmds << new hubitat.device.HubAction(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format())
	cmds << new hubitat.device.HubAction(zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format())
	cmds << new hubitat.device.HubAction(zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format())
    if(debugEnabled) "PollDev: $cmds"
	sendHubCommand(cmds)
}

// these values aren't likely to change
def longPollDevice() {
	def cmds = []
	cmds << new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeSupportedGet().format())
	cmds << new hubitat.device.HubAction(zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format())
    if(debugEnabled) log.debug "LongPollDev: $cmds"
	sendHubCommand(cmds)
}

def raiseHeatingSetpoint() {
    if(debugEnabled) log.debug "Raise Heat Setpoint"
	alterSetpoint(true, "heatingSetpoint")
}

def lowerHeatingSetpoint() {
    if(debugEnabled) log.debug "Lower Heat Setpoint"
	alterSetpoint(false, "heatingSetpoint")
}

def raiseCoolSetpoint() {
    if(debugEnabled) log.debug "Raise Cool Setpoint"
	alterSetpoint(true, "coolingSetpoint")
}

def lowerCoolSetpoint() {
    if(debugEnabled) log.debug "Lower Cool Setpoint"
	alterSetpoint(false, "coolingSetpoint")
}

// Adjusts nextHeatingSetpoint either .5° C/1° F) if raise true/false
def alterSetpoint(raise, setpoint) {
    if(debugEnabled) log.debug "Alter Setpoint: $raise $setpoint"
	def locationScale = getTemperatureScale()
	def deviceScale = (state.scale == 1) ? "F" : "C"
	def heatingSetpoint = getTempInLocalScale("heatingSetpoint")
	def coolingSetpoint = getTempInLocalScale("coolingSetpoint")
	def targetValue = (setpoint == "heatingSetpoint") ? heatingSetpoint : coolingSetpoint
	def delta = (locationScale == "F") ? 1 : 0.5
	targetValue += raise ? delta : - delta

	def data = enforceSetpointLimits(setpoint, [targetValue: targetValue, heatingSetpoint: heatingSetpoint, coolingSetpoint: coolingSetpoint])
	// update UI without waiting for the device to respond, this to give user a smoother UI experience
	// also, as runIn's have to overwrite and user can change heating/cooling setpoint separately separate runIn's have to be used
	if (data.targetHeatingSetpoint) {
		sendEvent("name": "heatingSetpoint", "value": getTempInLocalScale(data.targetHeatingSetpoint, deviceScale),
				unit: getTemperatureScale(), eventType: "ENTITY_UPDATE", displayed: false)
	}
	if (data.targetCoolingSetpoint) {
		sendEvent("name": "coolingSetpoint", "value": getTempInLocalScale(data.targetCoolingSetpoint, deviceScale),
				unit: getTemperatureScale(), eventType: "ENTITY_UPDATE", displayed: false)
	}
	if (data.targetHeatingSetpoint && data.targetCoolingSetpoint) {
		runIn(5, "updateHeatingSetpoint", [data: data, overwrite: true])
	} else if (setpoint == "heatingSetpoint" && data.targetHeatingSetpoint) {
		runIn(5, "updateHeatingSetpoint", [data: data, overwrite: true])
	} else if (setpoint == "coolingSetpoint" && data.targetCoolingSetpoint) {
		runIn(5, "updateCoolingSetpoint", [data: data, overwrite: true])
	}
}

def updateHeatingSetpoint(data) {
    if(debugEnabled) log.debug "Update Heat Setpoint: $data"
	updateSetpoints(data)
}

def updateCoolingSetpoint(data) {
    if(debugEnabled) log.debug "Update Cool Setpoint: $data"
	updateSetpoints(data)
}

def enforceSetpointLimits(setpoint, data) {
    if(debugEnabled) log.debug "Enforce Setpoint: $setpoint $data"
	def locationScale = getTemperatureScale() 
	def minSetpoint = (setpoint == "heatingSetpoint") ? getTempInDeviceScale(40, "F") : getTempInDeviceScale(50, "F")
	def maxSetpoint = (setpoint == "heatingSetpoint") ? getTempInDeviceScale(90, "F") : getTempInDeviceScale(99, "F")
	def deadband = (state.scale == 1) ? 3 : 2  // 3°F, 2°C
	def targetValue = getTempInDeviceScale(data.targetValue, locationScale)
	def heatingSetpoint = null
	def coolingSetpoint = null
	// Enforce min/mix for setpoints
	if (targetValue > maxSetpoint) {
		targetValue = maxSetpoint
	} else if (targetValue < minSetpoint) {
		targetValue = minSetpoint
	}
	// Enforce 3 degrees F deadband between setpoints
	if (setpoint == "heatingSetpoint") {
		heatingSetpoint = targetValue 
		coolingSetpoint = (heatingSetpoint + deadband > getTempInDeviceScale(data.coolingSetpoint, locationScale)) ? heatingSetpoint + deadband : null
	}
	if (setpoint == "coolingSetpoint") {
		coolingSetpoint = targetValue
		heatingSetpoint = (coolingSetpoint - deadband < getTempInDeviceScale(data.heatingSetpoint, locationScale)) ? coolingSetpoint - deadband : null
	}
    if(debugEnabled) log.debug "Enforce Setpoint - Heat: $heatingSetpoint Cool: $targetCoolngSetpoint"
	return [targetHeatingSetpoint: heatingSetpoint, targetCoolingSetpoint: coolingSetpoint]
}

def setHeatingSetpoint(degrees) {
    if(debugEnabled) log.debug "Set Heat: $degrees"
	if (degrees) {
		state.heatingSetpoint = degrees.toDouble()
		runIn(2, "updateSetpoints", [overwrite: true])
	}
}

def setCoolingSetpoint(degrees) {
    if(debugEnabled) log.debug "Set Cool: $degrees"
	if (degrees) {
		state.coolingSetpoint = degrees.toDouble()
		runIn(2, "updateSetpoints", [overwrite: true])
	}
}

def updateSetpoints() {
    if(debugEnabled) log.debug "Update Setpoints"
	def deviceScale = (state.scale == 1) ? "F" : "C"
	def data = [targetHeatingSetpoint: null, targetCoolingSetpoint: null]
	def heatingSetpoint = getTempInLocalScale("heatingSetpoint")
	def coolingSetpoint = getTempInLocalScale("coolingSetpoint")
	if (state.heatingSetpoint) {
		data = enforceSetpointLimits("heatingSetpoint", [targetValue: state.heatingSetpoint,
				heatingSetpoint: heatingSetpoint, coolingSetpoint: coolingSetpoint])
	}
	if (state.coolingSetpoint) {
		heatingSetpoint = data.targetHeatingSetpoint ? getTempInLocalScale(data.targetHeatingSetpoint, deviceScale) : heatingSetpoint
		coolingSetpoint = data.targetCoolingSetpoint ? getTempInLocalScale(data.targetCoolingSetpoint, deviceScale) : coolingSetpoint
		data = enforceSetpointLimits("coolingSetpoint", [targetValue: state.coolingSetpoint,
				heatingSetpoint: heatingSetpoint, coolingSetpoint: coolingSetpoint])
		data.targetHeatingSetpoint = data.targetHeatingSetpoint ?: heatingSetpoint
	}
	state.heatingSetpoint = null
	state.coolingSetpoint = null
	updateSetpoints(data)
}

def updateSetpoints(data) {
    if(debugEnabled) log.debug "Update Setpoints(data): $data"
	def cmds = []
	if (data.targetHeatingSetpoint) {
		cmds << zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: state.scale,
				precision: state.precision, scaledValue: data.targetHeatingSetpoint)
		cmds << zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1)
	}
	if (data.targetCoolingSetpoint) {
		cmds << zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: state.scale,
				precision: state.precision, scaledValue: data.targetCoolingSetpoint)
		cmds << zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2)
	}
    if(debugEnabled) log.debug "Update Setpoints(data) cmds: $cmds"
	sendHubCommand(cmds, 1000)
}

// thermostatSetpoint is not displayed by any tile as it can't be predictable calculated due to
// the device's quirkiness but it is defined by the capability so it must be set, set it to the most likely value
def updateThermostatSetpoint(setpoint, value) {
    if(debugEnabled) log.debug "UpD Setpoint: $setpoint $value"
	def scale = getTemperatureScale()
	def heatingSetpoint = (setpoint == "heatingSetpoint") ? value : getTempInLocalScale("heatingSetpoint")
	def coolingSetpoint = (setpoint == "coolingSetpoint") ? value : getTempInLocalScale("coolingSetpoint")
	def mode = device.currentValue("thermostatMode")
	def thermostatSetpoint = heatingSetpoint    // corresponds to (mode == "heat" || mode == "emergency heat")
	if (mode == "cool") {
		thermostatSetpoint = coolingSetpoint
	} else if (mode == "auto" || mode == "off") {
		// Set thermostatSetpoint to the setpoint closest to the current temperature
		def currentTemperature = getTempInLocalScale("temperature")
		if (currentTemperature > (heatingSetpoint + coolingSetpoint)/2) {
			thermostatSetpoint = coolingSetpoint
		}
	}
    if(debugEnabled) log.debug "UpD Setpoint thermostatSetpoint: $thermostatSetpooint"
	sendEvent(name: "thermostatSetpoint", value: thermostatSetpoint, unit: getTemperatureScale())
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	if(debugEnabled) log.debug "ping() called"
	// Just get Operating State there's no need to flood more commands
	sendHubCommand(new hubitat.device.HubAction(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format()))
}

def switchMode() {
    if(debugEnabled) log.debug "SwitchMode"
	def currentMode = device.currentValue("thermostatMode")
	def supportedModes = state.supportedModes
	// Old version of supportedModes was as string, make sure it gets updated
	if (supportedModes && supportedModes.size() && supportedModes[0].size() > 1) {
		def next = { supportedModes[supportedModes.indexOf(it) + 1] ?: supportedModes[0] }
		def nextMode = next(currentMode)
		runIn(2, "setGetThermostatMode", [data: [nextMode: nextMode], overwrite: true])
	} else {
		log.warn "supportedModes not defined"
		getSupportedModes()
	}
}

def switchToMode(nextMode) {
    if(debugEnabled) log.debug "Switch to Mode: $nextMode"
	def supportedModes = state.supportedModes
	// Old version of supportedModes was as string, make sure it gets updated
	if (supportedModes && supportedModes.size() && supportedModes[0].size() > 1) {
		if (supportedModes.contains(nextMode)) {
			runIn(2, "setGetThermostatMode", [data: [nextMode: nextMode], overwrite: true])
		} else {
			log.debug("ThermostatMode $nextMode is not supported by ${device.displayName}")
		}
	} else {
		log.warn "supportedModes not defined"
		getSupportedModes()
	}
}

def getSupportedModes() {
	def cmds = []
	cmds << new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeSupportedGet().format())
    if(debugEnabled) log.debug "Get Supt Modes: $cmds"
	sendHubCommand(cmds)
}

def switchFanMode() {
    if(debugEnabled) log.debug "Switch Fan Mode"
	def currentMode = device.currentValue("thermostatFanMode")
	def supportedFanModes = state.supportedFanModes
	// Old version of supportedFanModes was as string, make sure it gets updated
	if (supportedFanModes && supportedFanModes.size() && supportedFanModes[0].size() > 1) {
		def next = { supportedFanModes[supportedFanModes.indexOf(it) + 1] ?: supportedFanModes[0] }
		def nextMode = next(currentMode)
		runIn(2, "setGetThermostatFanMode", [data: [nextMode: nextMode], overwrite: true])
	} else {
		log.warn "supportedFanModes not defined"
		getSupportedFanModes()
	}
}

def switchToFanMode(nextMode) {
    if(debugEnabled) log.debug "Switch to Fan Mode: $nextMode"
	def supportedFanModes = state.supportedFanModes
	// Old version of supportedFanModes was as string, make sure it gets updated
	if (supportedFanModes && supportedFanModes.size() && supportedFanModes[0].size() > 1) {
		if (supportedFanModes.contains(nextMode)) {
			runIn(2, "setGetThermostatFanMode", [data: [nextMode: nextMode], overwrite: true])
		} else {
			log.debug("FanMode $nextMode is not supported by ${device.displayName}")
		}
	} else {
		log.warn "supportedFanModes not defined"
		getSupportedFanModes()
	}
}

def getSupportedFanModes() {
	def cmds = [new hubitat.device.HubAction(zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format())]
    if(debugEnabled) log.debug "Get Supr Fan Modes: $cmds"
	sendHubCommand(cmds)
}

def getModeMap() { [
	"off": 0,
//	"heat": 1,
	"cool": 2,
	"auto": 3
//	"emergency heat": 4
]}

def setThermostatMode(String value) {
    if(debugEnabled) log.debug "Set Mode: $value"
	switchToMode(value)
}

def setGetThermostatMode(data) {
	def cmds = [new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[data.nextMode]).format()),
			new hubitat.device.HubAction(zwave.thermostatModeV2.thermostatModeGet().format())]
    if(debugEnabled) log.debug "Get Mode: $cmds"
	sendHubCommand(cmds)
}

def getFanModeMap() { [
	"auto": 0,
	"on": 1,
    "low":1,
    "medium":2,
    "high":3,
	"circulate": 6
]}

def setThermostatFanMode(String value) {
    if(debugEnabled) log.debug "Set Fan Mode: $value"
	switchToFanMode(value)
}

def setGetThermostatFanMode(data) {
	def cmds = [new hubitat.device.HubAction(zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: fanModeMap[data.nextMode]).format()),
			new hubitat.device.HubAction(zwave.thermostatFanModeV3.thermostatFanModeGet().format())]
    if(debugEnabled) log.debug "Get Fan Mode: $data"
	sendHubCommand(cmds)
}

def off() {
    if(debugEnabled) log.debug "Off"
	switchToMode("off")
}

def heat() {
    if(debugEnabled) log.debug "Heat"
	switchToMode("heat")
}

def emergencyHeat() {
    if(debugEnabled) log.debug "Emergency Heat"
	switchToMode("emergency heat")
}

def cool() {
    if(debugEnabled) log.debug "Cool"
	switchToMode("cool")
}

def auto() {
    if(debugEnabled) log.debug "Auto"
	switchToMode("auto")
}

def fanOn() {
    if(debugEnabled) log.debug "Fan on"
	switchToFanMode("on")
}

def fanAuto() {
    if(debugEnabled) log.debug "Fan Auto"
	switchToFanMode("auto")
}

def fanLow() {
    if(debugEnabled) log.debug "Fan Low"
	switchToFanMode("low")
}

def fanMedium() {
    if(debugEnabled) log.debug "Fan Med"
	switchToFanMode("medium")
}

def fanHigh() {
    if(debugEnabled) log.debug "Fan High"
	switchToFanMode("high")
}

def fanCirculate() {
    if(debugEnabled) log.debug "Fan Circ"
	switchToFanMode("circulate")
}

def setFanSpeed(speed){
//    sendEvent(name:"debug",value:speed)
    if(debugEnabled) log.debug "Fan Speed: $speed"
    switchToFanMode(speed)
}

// Get stored temperature from currentState in current local scale
def getTempInLocalScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInLocalScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

// get/convert temperature to current local scale
def getTempInLocalScale(temp, scale) {
	if (temp && scale) {
		def scaledTemp = convertTemperatureIfNeeded(temp.toBigDecimal(), scale).toDouble()
		return (getTemperatureScale() == "F" ? scaledTemp.round(0).toInteger() : roundC(scaledTemp))
	}
	return 0
}

def getTempInDeviceScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInDeviceScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

def getTempInDeviceScale(temp, scale) {
	if (temp && scale) {
		def deviceScale = (state.scale == 1) ? "F" : "C"
		return (deviceScale == scale) ? temp :
				(deviceScale == "F" ? celsiusToFahrenheit(temp).toDouble().round(0).toInteger() : roundC(fahrenheitToCelsius(temp)))
	}
	return 0
}

def roundC (tempC) {
	return (Math.round(tempC.toDouble() * 2))/2
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
