/**
 * WeMo Local Control for Hubitat
 * v2.0
 *
 * Local-only WeMo integration inspired by pyWeMo/Home Assistant:
 * - SSDP discovery
 * - setup.xml parsing
 * - dynamic service discovery
 * - SOAP actions
 * - local UPnP event subscription via Hubitat HTTP server
 * - recovery/re-probing when WeMo ports change
 * - device-specific child drivers
 *
 * No Belkin cloud / Internet access is used.
 */

definition(
    name: "WeMo Local Control v2",
    namespace: "local.wemo.v2",
    author: "OpenAI",
    description: "Fuller local-only WeMo integration: discovery, SOAP, subscriptions and recovery.",
    category: "Integrations"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "WeMo Local Control v2", install: true, uninstall: true) {
        section("Local-only") {
            paragraph "All communication is directly between Hubitat and your WeMo devices. No Belkin cloud services are used."
            input "discoverOnStartup", "bool", title: "Discover at startup", defaultValue: true
            input "pollMinutes", "number", title: "Fallback poll interval (minutes)", defaultValue: 2
            input "debugLogging", "bool", title: "Debug logging", defaultValue: false
            input "manualIps", "text", title: "Manual WeMo IPs (comma separated)", required: false
        }
        section("Actions") {
            input "discoverNow", "button", title: "Discover WeMo Devices"
            input "refreshAll", "button", title: "Refresh All Devices"
            input "resubscribeAll", "button", title: "Re-subscribe to Events"
        }
        section("Status") {
            paragraph "Known devices: ${(state.devices ?: [:]).size()}"
            paragraph "Hub callback port: ${getCallbackPort() ?: 'not yet assigned'}"
        }
    }
}

def installed() { initialize() }

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    state.devices = state.devices ?: [:]
    if (discoverOnStartup != false) runIn(2, discover)
    if (pollMinutes) runEvery1Minute("pollChildren")
    runIn(10, subscribeAll)
}

def appButtonHandler(btn) {
    switch(btn) {
        case "discoverNow": discover(); break
        case "refreshAll": refreshAll(); break
        case "resubscribeAll": subscribeAll(); break
    }
}

def pollChildren() {
    def n = pollMinutes ?: 2
    def minute = ((now() / 60000L) as Long) as Integer
    if (minute % n != 0) return
    getChildDevices()?.each { d ->
        try { d.refresh() } catch (e) { logDebug("poll ${d.deviceNetworkId}: ${e.message}") }
    }
}

def refreshAll() {
    getChildDevices()?.each { d -> try { d.refresh() } catch (ignored) {} }
}

def subscribeAll() {
    getChildDevices()?.each { d ->
        try { d.subscribeEvents() } catch (e) { logDebug("subscribe ${d.deviceNetworkId}: ${e.message}") }
    }
}

def discover() {
    logDebug("SSDP discovery")
    def targets = [
        "urn:Belkin:device:**",
        "urn:Belkin:service:basicevent:1",
        "upnp:rootdevice",
        "ssdp:all"
    ]
    targets.each { st ->
        def msg = """M-SEARCH * HTTP/1.1\r
HOST: 239.255.255.250:1900\r
MAN: "ssdp:discover"\r
MX: 3\r
ST: ${st}\r
\r
"""
        try {
            def action = new hubitat.device.HubAction(
                msg,
                hubitat.device.Protocol.LAN,
                [
                    type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                    destinationAddress: "239.255.255.250:1900",
                    callback: "ssdpResponse",
                    timeout: 5,
                    parseWarning: false
                ]
            )
            sendHubCommand(action)
        } catch (e) {
            log.warn "SSDP send failed: ${e.message}"
        }
    }

    // Manual IP fallback. Probe the normal WeMo port range.
    (manualIps ?: "").split(",").collect { it.trim() }.findAll { it }.each { ip ->
        (49152..49159).each { port -> fetchSetup(ip, port) }
    }
}

def ssdpResponse(response) {
    try {
        def lan = parseLanMessage(response)
        def text = decodePayload(lan?.payload) ?: ""
        def loc = getHeader(text, "LOCATION")
        if (!loc) return
        def m = loc =~ /https?:\/\/([^:\/]+)(?::(\d+))?\/setup\.xml/i
        if (m.find()) fetchSetup(m.group(1), (m.group(2) ?: "49153").toInteger())
    } catch (e) {
        logDebug("SSDP response parse: ${e.message}")
    }
}

private void fetchSetup(String ip, Integer port) {
    try {
        def action = new hubitat.device.HubAction(
            method: "GET",
            path: "/setup.xml",
            headers: ["HOST": "${ip}:${port}", "Connection": "close"],
            body: null,
            callback: "setupResponse",
            timeout: 5,
            parseWarning: false
        )
        action.options.destinationAddress = "${ip}:${port}"
        action.data = [ip: ip, port: port]
        sendHubCommand(action)
    } catch (e) {
        logDebug("setup.xml ${ip}:${port}: ${e.message}")
    }
}

def setupResponse(response) {
    try {
        String body = response?.body ?: response?.data?.toString() ?: ""
        if (!body.contains("<root")) return

        def ip = response?.headers?."x-hubitat-source-ip"
        if (!ip) ip = response?.data?.ip
        if (!ip) {
            def hm = body =~ /<URLBase>https?:\/\/([^:\/]+)/
            if (hm.find()) ip = hm.group(1)
        }

        def root = new XmlParser(false, false).parseText(body)
        def dev = root.device
        def friendly = dev.friendlyName?.text() ?: "WeMo"
        def udn = dev.UDN?.text()
        def type = dev.deviceType?.text() ?: ""
        def model = dev.modelName?.text() ?: ""
        def serial = dev.serialNumber?.text() ?: ""

        def services = []
        dev.serviceList?.service?.each { s ->
            services << [
                serviceType: s.serviceType?.text() ?: "",
                serviceId: s.serviceId?.text() ?: "",
                controlURL: s.controlURL?.text() ?: "",
                eventSubURL: s.eventSubURL?.text() ?: "",
                SCPDURL: s.SCPDURL?.text() ?: ""
            ]
        }

        def basic = services.find { it.serviceType.contains("basicevent") }
        if (basic?.controlURL) {
            def pm = basic.controlURL =~ /:(\d+)\//
            if (pm.find()) {
                // use discovered control URL port if present
                def p = pm.group(1).toInteger()
                if (p) port = p
            }
        }
        Integer port = response?.data?.port ? (response.data.port as Integer) : 49153

        def dni = "wemo-${udn ? udn.replaceAll(/[^A-Za-z0-9_-]/, '_') : "${ip}-${port}"}"
        def existing = getChildDevice(dni)

        Map data = [
            ip: ip,
            port: port.toString(),
            udn: udn ?: "",
            model: model ?: "",
            deviceType: type ?: "",
            serial: serial ?: "",
            servicesJson: groovy.json.JsonOutput.toJson(services)
        ]

        if (!existing) {
            String driver = chooseDriver(type, model, services)
            existing = addChildDevice("local.wemo.v2", driver, dni, [
                name: friendly,
                label: friendly,
                isComponent: false,
                data: data
            ])
            log.info "Added ${friendly} (${driver}) at ${ip}:${port}"
        } else {
            data.each { k,v -> existing.updateDataValue(k, v.toString()) }
            existing.setLabel(friendly)
        }

        data.each { k,v -> try { existing.updateDataValue(k, v.toString()) } catch (ignored) {} }
        state.devices[dni] = [name:friendly, ip:ip, port:port, udn:udn, model:model, type:type]
        existing.refresh()
        runIn(2, { try { existing.subscribeEvents() } catch (ignored) {} })
    } catch (e) {
        logDebug("setup response: ${e.message}")
    }
}

private String chooseDriver(String type, String model, List services) {
    String t = (type ?: "").toLowerCase()
    String m = (model ?: "").toLowerCase()
    if (t.contains("insight") || m.contains("insight") || services.any { it.serviceType?.toLowerCase()?.contains("insight") }) {
        return "WeMo Local Insight"
    }
    if (t.contains("lightswitch") || t.contains("dimmer") || m.contains("dimmer") || m.contains("lightswitch")) {
        return "WeMo Local Dimmer"
    }
    if (t.contains("motion") || t.contains("sensor")) {
        return "WeMo Local Sensor"
    }
    return "WeMo Local Switch"
}

private String getCallbackPort() {
    try {
        return location.hub?.localIP ? "Hubitat hub HTTP listener" : null
    } catch (ignored) { return null }
}

private String getHeader(String text, String name) {
    def line = text.readLines().find { it.toUpperCase().startsWith(name.toUpperCase() + ":") }
    line ? line.substring(line.indexOf(":")+1).trim() : null
}

private String decodePayload(String payload) {
    if (!payload) return null
    try { return new String(payload.decodeBase64(), "UTF-8") } catch (ignored) { return payload }
}

private void logDebug(String s) { if (debugLogging) log.debug s }
