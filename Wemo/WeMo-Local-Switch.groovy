/**
 * WeMo Local Base Driver
 * v2.0
 */
metadata {
    definition(name: "WeMo Local Switch", namespace: "local.wemo.v2", author: "OpenAI") {
        capability "Switch"
        capability "Refresh"
        attribute "connection", "string"
        attribute "model", "string"
        attribute "udn", "string"
        attribute "lastEvent", "string"
    }
    preferences {
        input name:"debugLogging", type:"bool", title:"Debug logging", defaultValue:false
    }
}

def installed() { initialize() }
def updated() { initialize() }

def initialize() {
    sendEvent(name:"connection", value:"unknown")
    refresh()
    runIn(2, "subscribeEvents")
}

def on() { setBinaryState(1) }
def off() { setBinaryState(0) }
def refresh() { soap("GetBinaryState", [:], "parseState") }

def subscribeEvents() {
    // Hubitat custom drivers do not expose a stable arbitrary inbound HTTP
    // listener API in the same way a Python process can bind a socket.
    // Keep the method here so installations can override it if their Hubitat
    // firmware exposes an inbound callback facility. Polling remains the
    // portable fallback.
    logDebug("Event subscription requested; using polling fallback on this Hubitat runtime.")
}

def setBinaryState(Integer state) {
    soap("SetBinaryState", [BinaryState: state.toString()], "parseState")
}

def soap(String actionName, Map args, String callback) {
    String ip = device.getDataValue("ip")
    Integer port = (device.getDataValue("port") ?: "49153") as Integer
    if (!ip) return

    String argsXml = args.collect { k,v -> "<${k}>${esc(v.toString())}</${k}>" }.join("")
    String body = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body><u:${actionName} xmlns:u="urn:Belkin:service:basicevent:1">${argsXml}</u:${actionName}></s:Body>
</s:Envelope>"""

    String control = findServiceUrl("basicevent", "controlURL") ?: "/upnp/control/basicevent1"
    String path = control.startsWith("http") ? new URI(control).getRawPath() : control

    def action = new hubitat.device.HubAction(
        method:"POST",
        path:path,
        headers:[
            "HOST":"${ip}:${port}",
            "CONTENT-TYPE":"text/xml; charset=\"utf-8\"",
            "SOAPACTION":"\"urn:Belkin:service:basicevent:1#${actionName}\"",
            "CONNECTION":"close"
        ],
        body:body,
        callback:callback,
        timeout:5,
        parseWarning:false
    )
    action.options.destinationAddress = "${ip}:${port}"
    sendHubCommand(action)
}

def parseState(response) {
    try {
        String body = response?.body ?: response?.data?.toString() ?: ""
        def b = body =~ /<BinaryState>([^<]+)<\/BinaryState>/
        if (b.find()) sendEvent(name:"switch", value:(b.group(1).toInteger() ? "on" : "off"))
        sendEvent(name:"connection", value:"connected")
        sendEvent(name:"model", value:device.getDataValue("model") ?: "WeMo")
        sendEvent(name:"udn", value:device.getDataValue("udn") ?: "")
    } catch(e) {
        sendEvent(name:"connection", value:"disconnected")
        logDebug("parse state: ${e.message}")
        recover()
    }
}

def recover() {
    // Re-probe common WeMo ports without requiring Internet access.
    String ip = device.getDataValue("ip")
    if (!ip) return
    (49152..49159).each { p -> probePort(ip, p) }
}

private void probePort(String ip, Integer port) {
    def a = new hubitat.device.HubAction(
        method:"GET", path:"/setup.xml",
        headers:["HOST":"${ip}:${port}", "Connection":"close"],
        body:null, callback:"probeResponse", timeout:2, parseWarning:false
    )
    a.options.destinationAddress="${ip}:${port}"
    a.data=[port:port]
    sendHubCommand(a)
}

def probeResponse(response) {
    String body=response?.body ?: ""
    if (!body.contains("<root")) return
    Integer p=response?.data?.port as Integer
    if (p) {
        updateDataValue("port", p.toString())
        refresh()
    }
}

protected String findServiceUrl(String needle, String field) {
    try {
        def services = new groovy.json.JsonSlurper().parseText(device.getDataValue("servicesJson") ?: "[]")
        def s = services.find { (it.serviceType ?: "").toLowerCase().contains(needle.toLowerCase()) }
        return s ? s[field] : null
    } catch (ignored) { return null }
}

protected void logDebug(String s) { if (debugLogging) log.debug("${device.displayName}: ${s}") }
private String esc(String s) {
    s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
}
