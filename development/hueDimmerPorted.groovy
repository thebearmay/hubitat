/**
 *  Hue Dimmer Switch (Latest Model)
 *
 *  Copyright 2021 Matvei Vevitsis
 *  Based on code by Jaewon Park
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  Ported from ST for HE - thebearmay
 */
import groovy.json.JsonOutput
import hubitat.zigbee.zcl.DataType

metadata {
	definition (
		name: "Hue Dimmer Switch Ported", 
		namespace: "hubitat", 
		author: "mvevitsis", 
		importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/development/hueDimmerPorted.groovy"
	) {
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
		capability "PushableButton"
        	capability "HoldableButton"
        	capability "ReleasableButton"
		capability "Health Check"
		capability "Sensor"

		attribute "lastCheckin", "string"
		attribute "lastButtonState", "string"
		attribute "lastButtonName", "string"
		
		fingerprint profileId: "0104", endpointId: "01", application:"02", outClusters: "0019, 0000, 0003, 0004, 0006, 0008, 0005, 1000", inClusters: "0000, 0001, 0003, FC00, 1000", manufacturer: "Signify Netherlands B.V.", model: "RWL022", deviceJoinName: "Hue Dimmer Switch (Latest Model)"
	}
	preferences {
		input name: "holdTimingValue", type: "enum", title: "Hold event firing timing", options:["0": "When Hold starts", "1": "When Hold ends"], defaultValue: "0"
        input name: "debugEnabled", type: "bool", title: "Turn on debug logging"
	}
}

private getBATTERY_MEASURE_VALUE() { 0x0020 }


private getButtonLabel(buttonNum) {
	def hueDimmerNames = ["On","Up","Down","Off"]
	return hueDimmerNames[buttonNum - 1]
}


private getButtonName(buttonNum) {
	return "${device.displayName} " + getButtonLabel(buttonNum)
}


def parse(String description) {
	def result = []
	
   	if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
		result = parseMessage(description)
	}
	/*else if (description?.startsWith('enroll request')) {
		def cmds = zigbee.enrollResponse()
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}
	if (now() - state.battRefresh > 12 * 60 * 60 * 1000) { // send battery query command in at least 12hrs time gap
		state.battRefresh = now()
		def cmds = refresh()
		cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) } 
	}*/
	sendEvent(name: "lastCheckin", value: (new Date().format("MM-dd HH:mm:ss ", location.timeZone)), displayed: false)	
	return result
}


private parseMessage(String description) {
	def descMap = zigbee.parseDescriptionAsMap(description)

	switch(descMap.clusterInt) {
		case zigbee.POWER_CONFIGURATION_CLUSTER:
			if (descMap?.attrInt == BATTERY_MEASURE_VALUE && descMap.value) {
				return getBatteryResult(zigbee.convertHexToInt(descMap.value))
			}
			break
		case 0xFC00:
			if ( descMap.command == "00" ) {
				return getButtonResult(descMap.data)
			}
			break
	}
	return [:]
}


private getBatteryResult(rawValue) {
	def volts = rawValue / 10
	if (volts > 3.0 || volts == 0 || rawValue == 0xFF) {
		return [:]
	}
	def minVolts = 2.1
	def maxVolts = 3.0
	def pct = Math.max(1, Math.min(100, (int)(((volts - minVolts) / (maxVolts - minVolts)) * 100)))
	log.debug "Battery rawData: ${rawValue}  Percent: ${pct}"
	return createEvent(name: "battery", value: pct, descriptionText: "${device.displayName} battery is ${pct}%")
}

def push(buttonNumber){
//    sendEvent(name: "pushed", value:buttonNumber)
    sendButtonEvent(buttonNumber, "pushed")
}

def hold(buttonNumber){
//    sendEvent(name:"held", value:buttonNumber)
    sendButtonEvent(buttonNumber, "held")
}

def release(buttonNumber){
//    sendEvent(name: "released", value:buttonNumber)
    sendButtonEvent(buttonNumber, "released")
}

private getButtonResult(rawValue) {
	def result = []
	def buttonStateTxt
	
	def button = zigbee.convertHexToInt(rawValue[0])
	def buttonState = rawValue[4]
	def buttonHoldTime = rawValue[6]
	if(debugEnabled) log.debug "Button data : button=${button}  buttonState=${buttonState}  buttonHoldTime=${buttonHoldTime}"
	
	if ( buttonState == "00" ) {  // button pressed
		return [:]
	} else if ( buttonState == "02" ) {  // button released after push
		buttonStateTxt = "pushed"
	} else if ( buttonState == "03" ) {  // button released after hold
		buttonStateTxt = (HOLDTIMING)? "held" : "released"
	} else if ( buttonHoldTime == "08" ) {  // The button is being held
		if (HOLDTIMING) {
			return [:]
		} else {
			buttonStateTxt = "held"
		}
	} else {
		return [:]
	}
	
	def descriptionText = "${getButtonLabel(button)} button was ${buttonStateTxt}"
	if(debugEnabled) log.debug descriptionText
   	result << createEvent(name: "lastButtonName", value: getButtonLabel(button), displayed: false)
	result << createEvent(name: "lastButtonState", value: buttonStateTxt, displayed: false)
	
	if (buttonStateTxt == "pushed" || buttonStateTxt == "held") {
		result << createEvent(name: "button", value: buttonStateTxt, data: [buttonNumber: button], descriptionText: descriptionText, isStateChange: true, displayed: false)
		sendButtonEvent(button, buttonStateTxt)
		if (buttonStateTxt == "pushed" || HOLDTIMING) {
			runIn(1, "setReleased", [overwrite:true,data:button])
		}
	}
	return result
}


private sendButtonEvent(buttonNum, buttonState) {
	def child = childDevices?.find { channelNumber(it.deviceNetworkId) == buttonNum }
	if (child) {
		def descriptionText = "${child.displayName} button is ${buttonState}"
		if(debugEnabled) log.debug child.deviceNetworkId + " : " + descriptionText
		child.sendEvent(name: "button", value: buttonState, data: [buttonNumber: 1], descriptionText: descriptionText, isStateChange: true, displayed: true)
		if(buttonState == "pushed") {
			sendEvent(name:"pushed", value: buttonNum,isStateChange: true)
            		//child.sendEvent(name:"pushed", value:1,isStateChange: true)
            child.push(1)
		}else if (buttonState == "held"){
			sendEvent(name:"held", value: buttonNum,isStateChange: true)
            		//child.sendEvent(name:"held", value:1,isStateChange: true)
            child.hold(1)
		}else if (buttonState == "released") {
			sendEvent(name:"released", value: buttonNum,isStateChange: true)
            		//child.sendEvent(name:"released", value:1,isStateChange: true)
            child.release(1)
		}
	} else {
		log.warn "Child device $buttonNum not found!"
	}
}


private setReleased(buttonNum=1) {
	if(debugEnabled) log.debug "setReleased()"
	sendEvent(name: "lastButtonState", value: "released", displayed: false)
	sendButtonEvent(buttonNum,"released")
}

def ping () {
    configure()
}

def refresh() {
	def refreshCmds = zigbee.configureReporting(0xFC00, 0x0000, DataType.BITMAP8, 30, 30, null) + zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_MEASURE_VALUE, DataType.UINT8, 7200, 7200, 0x01)
	refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_MEASURE_VALUE)
	if(debugEnabled) log.debug "refresh() returns $refreshCmds"
	return refreshCmds
}


def configure() {
	if(debugEnabled) log.debug "configure() returns refresh()"
	return refresh()
}


def updated() {
	if(debugEnabled){
        	log.debug "updated() called"
        	runIn(1800, logsOff)
	}
	if (childDevices && device.label != state.oldLabel) {
		childDevices.each {
			def newLabel = getButtonName(channelNumber(it.deviceNetworkId))
			it.setLabel(newLabel)
		}
		state.oldLabel = device.label
	}
	for (child in childDevices) {
		if (!child.deviceNetworkId.startsWith(device.deviceNetworkId)) { //parent DNI has changed after rejoin
			child.setDeviceNetworkId("${device.deviceNetworkId}:${channelNumber(child.deviceNetworkId)}")
		}
	}	
}


def installed() {
	if(debugEnabled) log.debug "installed() called"
	def numberOfButtons = 4
    	def supportedValues = ["pushed","held"]
	createChildButtonDevices(numberOfButtons)
	sendEvent(name: "supportedButtonValues", value: JsonOutput.toJson(supportedValues), displayed: false)
	sendEvent(name: "numberOfButtons", value: numberOfButtons, displayed: false)
	numberOfButtons.times {
		sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
	}
	// These devices don't report regularly so they should only go OFFLINE when Hub is OFFLINE
	sendEvent(name: "DeviceWatch-Enroll", value: ([protocol: "zigbee", scheme:"untracked"]), displayed: false)
	sendEvent(name: "lastButtonState", value: "released", displayed: false)
	//state.battRefresh = now()
}


private void createChildButtonDevices(numberOfButtons) {
	if(debugEnabled) log.debug "Creating $numberOfButtons child buttons"
	def supportedValues = ["pushed", "held"]
	for (i in 1..numberOfButtons) {
		def child = childDevices?.find { it.deviceNetworkId == "${device.deviceNetworkId}:${i}" }
		if (child == null) {
			log.trace "..Creating child $i"
			child = addChildDevice("hubitat", "ST Child Button", "${device.deviceNetworkId}:${i}", [completedSetup: true, label: getButtonName(i),
				 isComponent: true, componentName: "button$i", componentLabel: "Button "+getButtonLabel(i)])
		}
		child.sendEvent(name: "supportedButtonValues", value: JsonOutput.toJson(supportedValues), displayed: false)
		child.sendEvent(name: "numberOfButtons", value: 1, displayed: false)
		child.sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)
	}
	state.oldLabel = device.label
}


private channelNumber(String dni) {
	dni.split(":")[-1] as Integer
}

private getHOLDTIMING() {
	return (holdTimingValue=="1")
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
