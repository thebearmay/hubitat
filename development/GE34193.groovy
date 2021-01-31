/**
 *  GE Portable Smart Motion Sensor (Model 34193) DTH
 *
 *  Copyright © 2018 Michael Struck
 *  Original Author: Jeremy Williams (@jwillaz)
 *
 *  Version 1.0.1 9/29/18 
 *
 *  Version 1.0.0 (11/17/17)- Original release by Jeremy Williams . Great Work!
 *  Version 1.0.1 (9/29/18)- Updated by Michael Struck-Streamlined interface, added additional logging
 *
 *  Uses code from SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "GE Portable Smart Motion Sensor 34193", namespace: "heidrickla", author: "Lewis Heidrick", ocfDeviceType: "x.com.st.d.sensor.motion") {
		capability "Motion Sensor"
		capability "Sensor"
		capability "Battery"
		capability "Health Check"
 
		fingerprint mfr: "0063", prod: "4953", model: "3133", deviceJoinName: "GE Portable Smart Motion Sensor"
	}
	simulator {
		status "inactive": "command: 3003, payload: 00"
		status "active": "command: 3003, payload: FF"
	}
	preferences {
        input "parameterThirteen", "enum", title: "Motion Sensitivity (Default: 'High')", options: [1:"Low", 2:"Medium", 3:"High"],  defaultValue: 3, required: false
        input "parameterEighteen", "number", title: "Motion Timeout Duration (1 to 60 minutes or 0 for 5 second test mode) Test mode will default to 4 minutes after 10 minutes.  Default: 1 minute",  defaultValue: 1, required: false, range: "0..60"
	    input "parameterTwenty", "enum", title: "Motion Sensitivity (Default: 'Basic Report')", options: [1:"Notification Report", 2:"Basic Set", 3:"Basic Report"],  defaultValue: 3, required: false
        input "parameterTwentyFour", "number", title: "Temperature and Light Sensor Frequency Setting (5 to 60 minutes) Default: 60 minutes",  defaultValue: 60, required: false, range: "5..60"
        input "parameterTwentyEight", "bool", title: "Enable LED Flash Indicator", defaultValue: true
    }
}
def ping() {
    log.debug "ping()"
    refresh()
}
def poll() {
    log.debug "poll()"
    refresh()
}
def refresh() {
    log.debug "refresh()"
    def cmds = []
    cmds << zwave.batteryV1.batteryGet()
    return commands(cmds)
}
private commands(commands, delay=1000) {
    delayBetween(commands.collect{ command(it) }, delay)
}
private command(hubitat.zwave.Command cmd) {
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd.format()
    }
    Short S2 = getDataValue("S2")?.toInteger()
    String encap = ""
    String keyUsed = "S0"
    if (S2 == null) { //S0 existing device
        encap = "988100"
    } else if ((S2 & 0x04) == 0x04) { //S2_ACCESS_CONTROL
        keyUsed = "S2_ACCESS_CONTROL"
        encap = "9F0304"
    } else if ((S2 & 0x02) == 0x02) { //S2_AUTHENTICATED
        keyUsed = "S2_AUTHENTICATED"
        encap = "9F0302"
    } else if ((S2 & 0x01) == 0x01) { //S2_UNAUTHENTICATED
        keyUsed = "S2_UNAUTHENTICATED"
        encap = "9F0301"
    } else if ((S2 & 0x80) == 0x80) { //S0 on C7
        encap = "988100"
    }
    return "${encap}${cmd.format()}"
}

def installed() {
// Device wakes up every 4 hours, this interval allows us to miss one wakeup notification before marking offline
	sendEvent(name: "checkInterval", value: 8 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}
def updated() {
	//Device wakes up every 4 hours, this interval allows us to miss one wakeup notification before marking offline
	sendEvent(name: "checkInterval", value: 8 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])   
    response(configure())
}
private getCommandClassVersions() {
	[0x20: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1]
}
def parse(String description) {
	def result = null
	if (description.startsWith("Err")) {
	    result = createEvent(descriptionText:description)
	} else {
		def cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
		} else {
			result = createEvent(value: description, descriptionText: description, isStateChange: false)
		}
	}
	return result
}
def sensorValueEvent(value) {
	if (value) {
		createEvent(name: "motion", value: "active", descriptionText: "$device.displayName detected motion")
	} else {
		createEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName motion has stopped")
	}
}
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd)
{
	sensorValueEvent(cmd.value)
}
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}
def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	sensorValueEvent(cmd.value)
}
def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}
def zwaveEvent(hubitat.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	sensorValueEvent(cmd.sensorState)
}
def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd)
{
	def result = []
	if (cmd.notificationType == 0x07) {
		if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice ensors
			result << sensorValueEvent(cmd.v1AlarmLevel)
		} else if (cmd.event == 0x01 || cmd.event == 0x02 || cmd.event == 0x07 || cmd.event == 0x08) {
			result << sensorValueEvent(1)
		} else if (cmd.event == 0x00) {
			result << sensorValueEvent(0)
		} else if (cmd.event == 0x03) {
			result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName covering was removed", isStateChange: true)
			result << response(zwave.batteryV1.batteryGet())
		} else if (cmd.event == 0x05 || cmd.event == 0x06) {
			result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, isStateChange: true, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, isStateChange: true, displayed: false)
	}
	result
}
def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
	def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
 
	if (state.MSR == "011A-0601-0901" && device.currentState('motion') == null) {  // Enerwave motion doesn't always get the associationSet that the hub sends on join
		result << response(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
	}
	if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
		result << response(zwave.batteryV1.batteryGet())
	} else {
		result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
	}
	result
}
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbat = new Date().time
	[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
	def map = [ displayed: true, value: cmd.scaledSensorValue.toString() ]
	switch (cmd.sensorType) {
		case 1:
			map.name = "temperature"
			map.unit = cmd.scale == 1 ? "F" : "C"
			break;
		case 3:
			map.name = "illuminance"
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = "lux"
			break;
		case 5:
			map.name = "humidity"
			map.value = cmd.scaledSensorValue.toInteger().toString()
			map.unit = cmd.scale == 0 ? "%" : ""
			break;
		case 0x1E:
			map.name = "loudness"
			map.unit = cmd.scale == 1 ? "dBA" : "dB"
			break;
	}
	createEvent(map)
}
def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	// log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		zwaveEvent(encapsulatedCommand)
	}
}
def zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd)
{
	// def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	def version = commandClassVersions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		return zwaveEvent(encapsulatedCommand)
	}
}
def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def result = null
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	log.debug "Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
		result = zwaveEvent(encapsulatedCommand)
	}
	result
}
def zwaveEvent(hubitat.zwave.commands.multicmdv1.MultiCmdEncap cmd) {
	log.debug "MultiCmd with $numberOfCommands inner commands"
	cmd.encapsulatedCommands(commandClassVersions).collect { encapsulatedCommand ->
		zwaveEvent(encapsulatedCommand)
	}.flatten()
}
def zwaveEvent(hubitat.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)
}
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)
	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
	result
}
def configure() { 
    def param13 = parameterThirteen ? parameterThirteen as int : 3
    def param18 = parameterEighteen ? parameterEighteen as int : 4
    def param20 = parameterTwenty ? parameterTwenty as int : 3
    def param24 = parameterTwentyFour ? parameterTwentyFour as int : 60
    def param28 = parameterTwentyEight ? 1 : 0
    log.info "Sending parameters to device..."
    log.info "Sending parameter 13, motion sensitivity: ${param13==1 ? "1=Low" : param13==2 ? "2=Medium" : "3=High"}"
    if (param18 < 1 || param18 >255 || (param18 > 60 && param18 != 255)) {
    	param18 = 255
    }
    def timing = param18==1 ? "Minute" : "Minutes"
        if (param24 <5 || param24 > 60) {
        param24 = 60
        log.warn "Invalid Motion Timeout Duration. Valid ranges are from 5 to 60 minutes. Defaluting to 60."
    }
    log.info "Sending parameter 18, Motion Timeout: ${param18==255 ? " 5 Seconds" : param18 + " ${timing}"}"
    log.info "Sending parameter 20, PIR Timeout Duration Setting: " + param20 + "${param20}"
    log.info "Sending parameter 24, Temperature and Light Sensor Sensing Frequency Setting: " + param24 + "${param24}"
    log.info "Sending parameter 28, LED Flash Indicator: " + param28 + "${param28==1 ? "=On" : "=Off"}"
    delayBetween([
        zwave.configurationV1.configurationSet(configurationValue: [param13], parameterNumber: 13, size: 1).format(),
        zwave.configurationV1.configurationSet(configurationValue: [param18], parameterNumber: 18, size: 1).format(),
        zwave.configurationV1.configurationSet(configurationValue: [param20], parameterNumber: 20, size: 1).format(),
        zwave.configurationV1.configurationSet(configurationValue: [param24], parameterNumber: 24, size: 1).format(),
        zwave.configurationV1.configurationSet(configurationValue: [param28], parameterNumber: 28, size: 1).format()
    ], 500)
}
