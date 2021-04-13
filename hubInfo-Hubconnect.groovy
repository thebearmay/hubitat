/*
 *	Copyright 2019-2020 csteele.
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 */
def getDriverVersion() {[platform: "Universal", major: 2, minor: 0, build: 0]}

metadata
{
	definition(name: "HubConnect Hub Information", namespace: "shackrat", author: "Steve White")
	{
		capability "TemperatureMeasurement"
		capability "Refresh"
		capability "Sensor"

		attribute "data", "string"
		attribute "firmwareVersionString", "string"
		attribute "formattedUptime", "string"
		attribute "freeMemory", "number"
		attribute "hardwareID", "string"
		attribute "html", "string"
		attribute "hubVersion", "string"
		attribute "id", "string"
		attribute "lastHubRestart", "string"
		attribute "lastHubRestartFormatted", "string"
		attribute "lastUpdated", "string"
		attribute "latitude", "string"
		attribute "localIP", "string"
		attribute "localSrvPortTCP", "string"
		attribute "locationId", "string"
		attribute "locationName", "string"
		attribute "longitude", "string"
		attribute "name", "string"
		attribute "temperatureC", "string"
		attribute "temperatureF", "string"
		attribute "temperatureScale", "string"
		attribute "timeZone", "string"
		attribute "type", "string"
		attribute "uptime", "number"
		attribute "zigbeeEui", "string"
		attribute "zigbeeId", "string"
		attribute "zipCode", "string"

		attribute "version", "string"

		attribute "jvmTotal", "number"
        	attribute "jvmFree", "number"
        	attribute "jvmFreePct", "number"
        	attribute "cpu5Min", "number"
		attribute "cpuPct", "number"
		attribute "dbSize", "number"
		attribute "publicIP", "string"
		
		command "sync"
	}
}


/*
	installed
*/
def installed()
{
	initialize()
}


/*
	updated
*/
def updated()
{
	initialize()
}


/*
	initialize
*/
def initialize()
{
	refresh()
}


/*
	uninstalled

	Reports to the remote that this device is being uninstalled.
*/
def uninstalled()
{
	// Report
	parent?.sendDeviceEvent(device.deviceNetworkId, "uninstalled")
}


/*
	refresh
*/
def refresh()
{
	// The server will update status
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}


/*
	sync
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "HubConnectHubInformation")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
