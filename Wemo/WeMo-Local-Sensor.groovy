metadata {
    definition(name: "WeMo Local Sensor", namespace: "local.wemo.v2", author: "OpenAI") {
        capability "Refresh"
        capability "MotionSensor"
        capability "ContactSensor"
        capability "PresenceSensor"
        attribute "connection", "string"
        attribute "lastEvent", "string"
    }
}
def installed(){refresh()}
def updated(){refresh()}
def refresh(){
    // WeMo sensors expose different actions depending on firmware.
    // Query the most common basic-event state; unsupported actions are ignored.
    soap("GetBinaryState",[:],"parse")
}
def parse(r){
    String b=r?.body ?: r?.data?.toString() ?: ""
    def x=b =~ /<BinaryState>([^<]+)<\/BinaryState>/
    if(x.find()) sendEvent(name:"contact",value:x.group(1).toInteger() ? "open":"closed")
    sendEvent(name:"connection",value:"connected")
}
def soap(String actionName,Map args,String cb){
    String ip=device.getDataValue("ip"), port=device.getDataValue("port") ?: "49153"
    String ax=args.collect{k,v->"<${k}>${v}</${k}>"}.join("")
    String body="""<?xml version="1.0" encoding="utf-8"?><s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:${actionName} xmlns:u="urn:Belkin:service:basicevent:1">${ax}</u:${actionName}></s:Body></s:Envelope>"""
    def a=new hubitat.device.HubAction(method:"POST",path:"/upnp/control/basicevent1",headers:["HOST":"${ip}:${port}","CONTENT-TYPE":"text/xml; charset=\"utf-8\"","SOAPACTION":"\"urn:Belkin:service:basicevent:1#${actionName}\""],body:body,callback:cb,timeout:5,parseWarning:false)
    a.options.destinationAddress="${ip}:${port}"
    sendHubCommand(a)
}
