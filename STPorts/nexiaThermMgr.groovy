/**
 *  Copyright 2015 SmartThings
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
 *    Nexia Thermostat Service Manager
 *
 *    Author: Trent Foley
 *    Date: 2016-01-19
 *
 * **	Modifications **
 *	Date		Who		    Description
 *	2022-09-15	thebearmay	Port to Hubitat
 *	2022-09-16	thebearmay	Fix thermostatOperatingMode
 *  2022-10-04  thebearmay  Add permanent hold and return to schedule
 *  2022-10-07  thebearmay  Option to use American Standard Login 
 *
 */
static String version()	{  return '1.0.3' }

definition(
    name: "Nexia Thermostat Manager",
    namespace: "trentfoley",
    author: "Trent Foley",
    description: "Connect your Nexia thermostat to Hubitat.",
    category: "Convenience",
	importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/STPorts/nexiaThermMgr.groovy",    
    iconUrl: "http://lh4.ggpht.com/oMx3-nlICwLmUxpDhTXWsZ6Ocuzu9P2yfz9jpXBx1rhrW_Vcj94kPl2M9ooApckK6TM1=w60",
    iconX2Url: "https://www.trane.com/content/dam/Trane/residential/products/nexia/medium/TR_Nexia%20-%20Medium.jpg",
    iconX3Url: "https://www.trane.com/content/dam/Trane/residential/products/nexia/medium/TR_Nexia%20-%20Medium.jpg",
    singleInstance: true
) { }

preferences {
    section("<h2 style='color:blue'>Nexia Authentication<br><span style='font-size:small'>v${version()}</span></h2>") {
        input "username", "text", title: "Username"
        input "password", "password", title: "Password"
        input "debugEnabled", "bool", title: "Enable debug logging?", width:4
        input "useAmerStand", "bool", title: "Use American Standard login", width:4, defaultValue:false
        if(debugEnabled) runIn(1800, "logsOff")
    }
}

def getChildNamespace() { "trentfoley" }
def getChildName() { "Nexia Thermostat" }
def getServerUrl() { 
    if(useAmerStand) 
        return "https://asairhome.com/login" 
    else
        return "https://www.tranehome.com/login" 
}

def installed() {
    if(debugEnabled) log.debug("installed()")
    initialize()
}

def updated() {
    if(debugEnabled) log.debug("updated()")
    unsubscribe()
    initialize()
}

def initialize() {
    if(debugEnabled) log.debug("initialize()")
    
    // Ensure authenticated
    refreshAuthToken()
    
    // Get list of thermostats and ensure child devices
    def homeParams = [
        //method: 'GET',
        uri: serverUrl,
        headers: getDefaultHeaders()
    ]

    try {
        httpGet(homeParams) { homeResp ->

        	def respData = homeResp.data[0]
        	
            // html / body / div id=footer-wrapper / div id=content / div id=content_sidebar / nav / ul / li / a id=climate_link
            // Recursive search for climate/index link.  Should be more robust to Nexia DOM changes
            respData.children().each{
                searchForClimate(it)
            }
        }
    }
    catch(e) {
        log.error("Caught exception determining thermostats path $e")
    }
    
    // Get list of thermostats and ensure child devices
    requestThermostats { thermostatsResp ->
        def devices = thermostatsResp.data.collect { stat ->
            if(debugEnabled) log.debug("Found thermostat with ID: ${stat.id}")
            
            //Check for Multiple Zones
            def dni = getDeviceNetworkId(stat.id)
            def device = null;
            if(stat.zones.size > 1) {
                stat.zones.each {
                    dni = getDeviceNetworkId(stat.id + "_" + it.id)
                    device = addMultipleDevices(dni, it.name)
                }
            }
            else {
                dni = getDeviceNetworkId(stat.id)
                device = addMultipleDevices(dni, stat.name)
            }
            device.initialize()
            return device
        }

        if(debugEnabled) log.debug("Discovered ${devices.size()} thermostats")
    }
}

private searchForClimate(httpNode) {
    if(httpNode != null && !(httpNode instanceof String)) {
        def href = httpNode.attributes()["href"]
        if(href != null) {
            if(debugEnabled) log.debug "$href"
            if(href.matches("/houses/(?i).*climate"))
            {
                if(debugEnabled) log.debug "Found climate"
                state.thermostatsPath = href.replace("/climate", "/xxl_thermostats")
                state.zonesPath = href.replace("/climate", "/xxl_zones")
                if(debugEnabled) log.debug("state.thermostatsPath = ${state.thermostatsPath}; state.zonesPath = ${state.zonesPath}")
            }
        }
        if(httpNode.children() != null) {
            httpNode.children().each {
                if(it!=null)
                    searchForClimate(it)
            }
        }
    }
}

private def addMultipleDevices(dni, statname) {
    def device = getChildDevice(dni)
    if(!device) {
        device = addChildDevice(childNamespace, childName, dni, null, [ label: "${childName} (${statname})" ])
        if(debugEnabled) log.debug("Created ${device.displayName} with device network id: ${dni}")
    } else {
        if(debugEnabled) log.debug("Found already existing ${device.displayName} with device network id: ${dni}")
    }
    return device
}

private refreshCsrfToken(resp) {
    def respData = resp.data[0]

    // Get CSRF token from response
    // head / <meta name="csrf-token" content="***" />
    respData.children[0].children().each {
        if (it.attributes()["name"] == "csrf-token") {
            state.csrfToken = it.attributes()["content"]
            if(debugEnabled) log.debug("state.csrfToken = ${state.csrfToken}")
        }
    }
}

private searchForAuthToken(httpNode) {
    if(httpNode != null && !(httpNode instanceof String)) {
        if (httpNode.attributes()["name"] != null) {
            if(httpNode.attributes()["name"]=="authenticity_token") {
                state.AuthToken = httpNode.attributes()["value"]
                if(debugEnabled) log.debug("state.AuthToken = ${state.AuthToken}")
            }
        }
        
        if (httpNode.children() != null) {
            httpNode.children().each {
                if(it != null)
                    searchForAuthToken(it)
            }
        }
    }
}

private String getDeviceNetworkId(def statId) {
    return [ app.id, statId ].join('.')
}

private updateCookies(response) {
    response.getHeaders('Set-Cookie').each {
        def cookieValue = it.value.split(';')[0]
        def cookieName = cookieValue.split('=')[0]
        state.cookies[(cookieName)] = cookieValue
        if(debugEnabled) log.debug("state.cookies[${cookieName}] = ${cookieValue}")
    }
}

def getDefaultHeaders() {
    def headers = [
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Accept-Encoding': 'gzip, deflate',
        'Accept-Language': 'en-US,en,q=0.8',
        'Cache-Control': 'max-age=0',
        'Connection': 'keep-alive',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36',
        'X-CSRF-Token': state.csrfToken,
		'X-Requested-With': 'XMLHttpRequest'
    ]

    def cookieString = state.cookies?.collect { entry -> entry.value }?.join('; ');
    if (cookieString) { headers.Cookie = cookieString }
    return headers
}

private refreshAuthToken() {
    if(debugEnabled) log.debug("refreshAuthToken()")

    // Initialize / clear any existing cookies
    state.cookies = [:]

    def loginParams = [
        //method: 'GET',
        uri: serverUrl,
        path: "/login",
        headers: getDefaultHeaders()
    ]
    
    try {
         httpGet(loginParams) { loginResp ->
            updateCookies(loginResp)
            // html / body   / div id=content / div id=external-wrapper / div id=external-content / div id=login-form / form / div / input name=authenticity_token
            // OLD def authenticityToken = loginResp.data[0].children[1].children[1].children[0].children[0].children[1].children[2].children[0].children[1].attributes()["value"]
            //def authenticityToken = loginResp.data[0].children[1].children[1].children[0].children[0].children[0].children[0].children[2].children[0].children[1].attributes()["value"]
            // Recursive search for authenticity token.  Should be more robust to Nexia DOM changes
            searchForAuthToken(loginResp.data[0])
            def authenticityToken = state.AuthToken
            refreshCsrfToken(loginResp);
            
            def sessionParams = [
                //method: 'POST',
                uri: serverUrl,
                path: '/session',
                requestContentType: 'application/x-www-form-urlencoded',
                headers: getDefaultHeaders(),
                body: [
                    'utf8': '✓',
                    'authenticity_token': authenticityToken,
                    'login': settings.username,
                    'password': settings.password
                ]
            ]

            httpPost(sessionParams) { sessionResp ->
                if (sessionResp.status != 302) {
                	log.error("Did not receive expected response status code.  Expected 302, actual ${sessionResp.status}")
                }
                updateCookies(sessionResp)
                refreshCsrfToken(sessionResp);
            }
        }
    }
    catch(e) {
        log.error("Caught exception refreshing auth token $e")
    }
}

private requestThermostats(Closure closure) {
    if(debugEnabled) log.debug("requestThermostats(${state.thermostatsPath})")
    
    def thermostatsParams = [
        uri: serverUrl,
        path: state.thermostatsPath,
        headers: getDefaultHeaders()
    ]
    
    try {
        httpGet(thermostatsParams) { resp ->
            if (resp.status == 200) {
                closure(resp)
            } else if (resp.status == 302) { // Redirect to login page due to session expiration
                refreshAuthToken()
                requestThermostats(closure)
            } else {
                log.error("Unexpected status while requesting thermostats: ${resp.status}")
            }
        }
    }
    catch(e) {
        log.error("Caught exception requesting thermostats $e")
    }
}

private requestThermostat(deviceNetworkId, Closure closure) {
    if(debugEnabled) log.debug("requestThermostat(${deviceNetworkId})")
    requestThermostats { resp ->
        def stat = resp.data.find { it -> getDeviceNetworkId(it.id) == deviceNetworkId }
        if (!stat) {
            log.error("Device connection removed? No data found for ${deviceNetworkId} after polling")
        } else {
            closure(stat)
        }
    }
}

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
def pollChild(child) {
    //if zoned, take off zone id... performs a repetitive update due to zoning, fix later
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("ZoneBool ${zonedBool} pollChild(${deviceNetworkId})")

    def statData = [:]

    requestThermostat(deviceNetworkId) { stat ->
        def zone = stat.zones[0]
        if(zonedBool > 1) {
            def zoneNetworkId = ((child.device.deviceNetworkId).split('_'))[1]
            zone = stat.zones.find {it.id == zoneNetworkId.toInteger()}
        }
        
        def systemStatusToOperatingStateMapping = [
            "System Idle": "idle",
            "Waiting...": "pending ${zone.zone_mode.toLowerCase()}",
            "Heating": "heating",
            "Cooling": "cooling",
            "Fan Running": "fan only"
        ]
        if(debugEnabled) log.debug "Zone: $zone"

        statData = [
            temperature: zone.temperature.toInteger(),
            heatingSetpoint: zone.heating_setpoint.toInteger(),
            coolingSetpoint: zone.cooling_setpoint.toInteger(),
            thermostatSetpoint: ((zone.zone_mode == "COOL") ? zone.cooling_setpoint : zone.heating_setpoint).toInteger(),
            // TODO: handle case for "emergency heat"
            thermostatMode: zone.requested_zone_mode.toLowerCase(), // "auto" "emergency heat" "heat" "off" "cool"
            thermostatFanMode: stat.fan_mode,  // "auto" "on" "circulate"
            thermostatOperatingState: systemStatusToOperatingStateMapping[stat.system_status], // "heating" "idle" "pending cool" "vent economizer" "cooling" "pending heat" "fan only"
            systemStatus: stat.system_status,
            activeMode: zone.zone_mode.toLowerCase(),
            emergencyHeatSupported: stat.emergency_heat_supported,
            humidity: (stat.current_relative_humidity * 100).toInteger(),
            outdoorTemperature: stat.raw_outdoor_temperature.toInteger(),
            setpointStatus: zone.setpoint_status
        ]
    }
    
    return statData
}

// updateType can be: "setpoints", "zone_mode"
private updateZone(zone, updateType) {
    if(debugEnabled) log.debug("updateZone(${zone.id}, ${updateType})")
    
    zone.hold_time = zone.hold_time.toBigInteger()
    
    def requestParams = [
        uri: serverUrl,
        path: "${state.zonesPath}/${zone.id}/${updateType}",
        headers: getDefaultHeaders(),
        body: zone
    ]

    httpPutJson(requestParams) { resp ->
        if (resp.status == 200) {
            if(debugEnabled) log.debug("Zone update suceeded")
        } else {
        	log.error("Unexpected status while attempting to update zone: ${resp.status}")
            
            /*
            def zoneJson = new org.json.JSONObject(zone).toString()
            def interations = Math.ceil(zoneJson.length() / 1200.0)
            for(int i = 0; i <= interations; i++) {
            	def end = i * 1200 + 1200
                if (zoneJson.length() < end) {
                	end = zoneJson.length()
                }
                if(debugEnabled) log.debug "${i}: ${zoneJson.substring(i * 1200, end)}"
            }
            */
        }
    }
}

// updateType can be: "fan_mode"
private updateThermostat(stat, updateType) {
    if(debugEnabled) log.debug("updateThermostat(${stat.id}, ${updateType})")
    def requestParams = [
        uri: serverUrl,
        path: "${state.thermostatsPath}/${stat.id}/${updateType}",
        headers: getDefaultHeaders(),
        body: stat
    ]

    httpPutJson(requestParams) { resp ->
        if (resp.status == 200) {
            if(debugEnabled) log.debug("Thermostat update suceeded")
        } else {
            log.error("Unexpected status while attempting to update thermostat: ${resp.status} ${stat}")
        }
    }
}

def setHeatingSetpoint(child, degreesF) {
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("setHeatingSetpoint(${deviceNetworkId}, ${degreesF})")
    
    requestThermostat(deviceNetworkId) { stat ->
        def zone = stat.zones[0]
        if(zonedBool > 1) {
            def zoneNetworkId = ((child.device.deviceNetworkId).split('_'))[1]
            zone = stat.zones.find {it.id == zoneNetworkId.toInteger()}
        }
        zone.heating_setpoint = degreesF
        zone.heating_integer = "${degreesF.toInteger()}"
        zone.heating_decimal = ""
        zone.cooling_setpoint = zone.cooling_setpoint
        zone.cooling_integer = "${zone.cooling_setpoint}"
        zone.cooling_decimal = ""
        
        updateZone(zone, "setpoints")
    }
}

def setCoolingSetpoint(child, degreesF) {
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("setCoolingSetpoint(${deviceNetworkId}, ${degreesF})")
    
    requestThermostat(deviceNetworkId) { stat ->
        def zone = stat.zones[0]
        if(zonedBool > 1) {
            def zoneNetworkId = ((child.device.deviceNetworkId).split('_'))[1]
            zone = stat.zones.find {it.id == zoneNetworkId.toInteger()}
        }
        zone.heating_setpoint = zone.heating_setpoint
        zone.heating_integer = "${zone.heating_setpoint.toInteger()}"
        zone.heating_decimal = ""
        zone.cooling_setpoint = degreesF
        zone.cooling_integer = "${degreesF.toInteger()}"
        zone.cooling_decimal = ""
        
        updateZone(zone, "setpoints")
    }
}

def setThermostatMode(child, value) {
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("setThermostatMode(${deviceNetworkId}, ${value})")
    
    requestThermostat(deviceNetworkId) { stat ->
        def zone = stat.zones[0]
        if(zonedBool > 1) {
            def zoneNetworkId = ((child.device.deviceNetworkId).split('_'))[1]
            zone = stat.zones.find {it.id == zoneNetworkId.toInteger()}
        }
        zone.requested_zone_mode = value.toUpperCase()
        zone.last_requested_zone_mode = value.toUpperCase()
        updateZone(zone, "zone_mode")
    }
}

def setHoldMode(child, value) {//"permanent_hold" or "return_to_schedule"
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("setThermostatMode(${deviceNetworkId}, ${value})")
    
    requestThermostat(deviceNetworkId) { stat ->
        def zone = stat.zones[0]
        if(zonedBool > 1) {
            def zoneNetworkId = ((child.device.deviceNetworkId).split('_'))[1]
            zone = stat.zones.find {it.id == zoneNetworkId.toInteger()}
        }
        
        updateZone(zone, value)
    }
}

def setThermostatFanMode(child, value) {
    def deviceNetworkId = ((child.device.deviceNetworkId).split('_'))[0]
    def zonedBool = ((child.device.deviceNetworkId).split('_')).size()
    if(debugEnabled) log.debug("setThermostatFanMode(${deviceNetworkId}, ${value})")
    
    requestThermostat(deviceNetworkId) { stat ->
        stat.fan_mode = value
        updateThermostat(stat, "fan_mode")
    }
}

void logsOff(){
    device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
