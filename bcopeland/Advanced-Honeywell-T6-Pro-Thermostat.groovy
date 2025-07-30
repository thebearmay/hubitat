import groovy.transform.Field
import java.text.SimpleDateFormat

/**
 * Advanced Honeywell T6 Pro
 * Modified using bcopeland's v1.2 code as the baseline
 *
 * Date       Who           Description
 * 2022-09-26 thebearmay    post 269 modifications
 * 2022-09-27 thebearmay    add jkenn99 pull request changes
 * 2022-09-29 thebearmay    correct refresh C to F jumping, setPoint rounding issue correction, add option to always send temperature reading events
 *                          add presence capability - jkenn99 completion
 * 2022-09-30 thebearmay    v1.2.6 last activity attribute, force time sync option, sync time on Mains Reconnect
 *                          v1.2.7 fix typo in activity reporting
 *                          v1.2.8 fix typo in setpoint scaling, change where sendAllTemps is evaluated
 * 2022-10-01 thebearmay    v1.2.9 change lastActivity to use device.lastActivity
 *.                         v1.2.10 powersource event getting overwritten by syncClock
 *                          v1.2.11 scale vs Scale code reversion fix
 *                          v1.2.12 basic report correction
 * 2022-10-02 thebearmay    v1.2.13 add thermostatFanOperatingState, replace sendAllTemps with deltaTempRpt and deltaHumRpt                    
 * 2022-10-03 thebearmay    v1.2.14 refresh to create temperature and humidity events regardless of deltas
 * 2022-10-11 thebearmay    v1.2.15 first run humidity and temperature error correction
 * 2022-12-15 thebearmay    v1.2.16 allow external humidity calibration
 * 2022-12-24 thebearmay    v1.2.17 fixes for Thermostat Scheduler
 * 2023-02-06 thebearmay    v1.2.18 set powerSource on battery report or power applied
 * 2023-02-27 thebearmay    v1.2.19 implement Home and Away
 * 2023--5-26 thebearmay    v1.2.20 replace infoEnable with txtEnable
 *
 */

static String version()	{  return "1.2.20" }
metadata {
    definition (name: "Advanced Honeywell T6 Pro Thermostat", 
                namespace: "djdizzyd", 
                author: "Bryan Copeland", 
                importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/bcopeland/Advanced-Honeywell-T6-Pro-Thermostat.groovy") {

        capability "Actuator"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Thermostat"
        capability "ThermostatMode"
        capability "ThermostatFanMode"
        capability "ThermostatSetpoint"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatOperatingState"
        capability "RelativeHumidityMeasurement"
        capability "PowerSource"
        capability "PresenceSensor"

        attribute "currentSensorCal", "number"
        attribute "currentHumidityCal", "number"
        attribute "idleBrightness", "number"
        attribute "thermostatFanOperatingState", "string"
        attribute "unhandled", "string"
       
        command "SensorCal", [[name:"calibration",type:"ENUM", description:"Number of degrees to add/subtract from thermostat sensor", constraints:["-3", "-2", "-1", "0", "1", "2", "3"]]]
        command "HumidityCal", [[name:"humidity calibration",type:"ENUM", description:"Percent to add/subtract from humidity sensor", constraints:["-12","-11","-10","-9","-8","-7","-6","-5","-4","-3", "-2", "-1", "0", "1", "2", "3","4","5","6","7","8","9","10","11","12"]]]
        command "IdleBrightness", [[name:"brightness",type:"ENUM", description:"Set idle brightness", constraints:["0", "1", "2", "3", "4", "5"]]]
        command "syncClock"
        command "home"
        command "away"
        command "initializeVars"
        command "checkFilter"
        
        fingerprint  mfr:"0039", prod:"0011", deviceId:"0008", inClusters:"0x5E,0x85,0x86,0x59,0x31,0x80,0x81,0x70,0x5A,0x72,0x71,0x73,0x9F,0x44,0x45,0x40,0x42,0x43,0x6C,0x55", deviceJoinName: "Honeywell T6 PRO"

    }
    preferences {
        configParams.each { input it.value.input }
        input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
        input "txtEnable", "bool", title: "Enable info logging", defaultValue: false
//		input "sendAllTemps", "bool", title: "Create Events for All Temperature and Humidity Readings", submitOnChange: true, defaultValue: false
        input "deltaTempRpt", "number", title: "Minimum Temperature Delta to Generate Event, O reports all readings", submitOnChange:true, defaultValue: 1
        input "deltaHumRpt", "number", title: "Minimum Humidity Delta to Generate Event, O reports all readings", submitOnChange:true, defaultValue: 1
        input "forceTimeSync", "number", title: "Mintutes Before Forcing Time Sync if no Activity", submitOnChange:true, defaultValue: 0
    }

}

@Field static Map CMD_CLASS_VERS=[0x71:3, 0x7A:2, 0x81:1, 0x73:1, 0x2B:1, 0x2C:1, 0x85:2, 0x72:1, 0x86:2, 0x8F:1, 0x31:5, 0x70:1, 0x80:1, 0x45:1, 0x44:3, 043:2, 0x42:1, 0x40:2, 0x5A:1, 0x59:1, 0x5E:2]
@Field static Map THERMOSTAT_OPERATING_STATE=[0x00:"idle",0x01:"heating",0x02:"cooling",0x03:"fan only",0x04:"pending heat",0x05:"pending cool",0x06:"vent economizer"]
@Field static Map THERMOSTAT_MODE=[0x00:"off",0x01:"heat",0x02:"cool",0x03:"auto",0x04:"emergency heat"]
@Field static Map SET_THERMOSTAT_MODE=["off":0x00,"heat":0x01,"cool":0x02,"auto":0x03,"emergency heat":0x04]
@Field static Map THERMOSTAT_FAN_MODE=[0x00:"auto",0x01:"on",0x02:"auto",0x03:"on",0x04:"auto",0x05:"on",0x06:"circulate",0x07:"circulate"]
@Field static Map SET_THERMOSTAT_FAN_MODE=["auto":0x00,"on":0x01,"circulate":0x06, " auto":0x00, " on":0x01, " circulate":0x06] //@Field static Map SET_THERMOSTAT_FAN_MODE=["auto":0x00,"on":0x01,"circulate":0x06]
@Field static Map THERMOSTAT_FAN_STATE=[0x00:"idle", 0x01:"running", 0x02:"running high",0x03:"running medium",0x04:"circulation mode",0x05:"humidity circulation mode",0x06:"right - left circulation mode",0x07:"quiet circulation mode"]
@Field static Map SET_PRESENCE=["not present":0x00, "present":0xFF]
@Field static Map PRESENCE_VALUE=[0x00:"not present",0xFF:"present"]
@Field static List<String> supportedThermostatFanModes=["on","auto","circulate"]
@Field static List<String> supportedThermostatModes=["auto", "off", "heat", "emergency heat", "cool"]
@Field static Map ZWAVE_NOTIFICATION_TYPES=[0:"Reserved", 1:"Smoke", 2:"CO", 3:"CO2", 4:"Heat", 5:"Water", 6:"Access Control", 7:"Home Security", 8:"Power Management", 9:"System", 10:"Emergency", 11:"Clock", 12:"First"]
@Field static Map configParams = [
        1: [input: [name: "configParam1", type: "enum", title: "Schedule Type", description: "", defaultValue: 2, options: [0:"No schedule/Occupacy based schedule",1:"Every day the same",2:"5-2 Schedule",3:"5-1-1 Schedule",4:"Every day individual"]], parameterSize: 1],
        2: [input: [name: "configParam2", type: "enum", title: "Temperature Scale", description:"", defaultValue: 0, options: [0:"Fahrenheit", 1:"Celsius"]], parameterSize: 1],
        3: [input: [name: "configParam3", type: "enum", title: "Outdoor Temperature", description:"", defaultValue: 0, options: [0:"No", 1:"Wired"]], parameterSize: 1],
        4: [input: [name: "configParam4", type: "enum", title: "Equipment Type", defaultValue: 2, options: [0:"None", 1:"Standard Gas",2:"High Efficiency Gas",3:"Oil",4:"Electric",5:"Fan Coil",6:"Air to Air Heat Pump",7:"Geothermal Heat Pump",8:"Hot Water",9:"Steam"]], parameterSize: 1],
        5: [input: [name: "configParam5", type: "enum", title: "Reversing Valve", defaultValue: 0, options: [0:"O/B on Cool", 1:"O/B on Heat"]], parameterSize:1],
        6: [input: [name: "configParam6", type: "enum", title: "Stages", defaultValue: 1, options: [0:"0", 1:"1",2:"2"]], parameterSize:1],
        7: [input: [name: "configParam7", type: "enum", title: "Heat Stages Aux/E stages", defaultValue: 1, options: [0:"0", 1:"1",2:"2"]], parameterSize:1],
        8: [input: [name: "configParam8", type: "enum", title: "Aux/E Control", defaultValue: 0, options:[0:"Both Aux and E", 1:"Either Aux/E"]], parameterSize: 1],
        9: [input: [name: "configParam9", type: "enum", title: "Aux Heat Type", defaultValue: 0, options:[0:"Electric", 1:"Gas/Oil"]], parameterSize: 1],
        10: [input: [name: "configParam10", type: "enum", title: "EM Heat Type", defaultValue: 0, options:[0:"Electric", 1:"Gas/Oil"]], parameterSize: 1],
        11: [input: [name: "configParam11", type: "enum", title: "Fossil Kit Control", defaultValue: 0, options:[0:"Thermostat",1:"External"]], parameterSize: 1],
        12: [input: [name: "configParam12", type: "enum", title: "Auto Changeover", defaultValue: 0, options:[0:"Off",1:"On"]], parameterSize: 1],
        13: [input: [name: "configParam13", type: "enum", title: "Auto Differential", defaultValue: 0, options:[0:"0°F",1:"1°F",2:"2°F",3:"3°F",4:"4°F",5:"5°F"]], parameterSize: 1],
        14: [input: [name: "configParam14", type: "enum", title: "High Cool Stage Finish", defaultValue: 0, options:[0:"No",1:"Yes"]], parameterSize: 1],
        15: [input: [name: "configParam15", type: "enum", title: "High Heat Stage Finish", defaultValue: 0, options:[0:"No",1:"Yes"]], parameterSize: 1],
        16: [input: [name: "configParam16", type: "enum", title: "Aux Heat Droop", defaultValue: 0, options:[0:"Comfort",2:"2°F",3:"3°F",4:"4°F",5:"5°F",6:"6°F",7:"7°F",8:"8°F",9:"9°F",10:"10°F",11:"11°F",12:"12°F",13:"13°F",14:"14°F",15:"15°F"]], parameterSize: 1],
        17: [input: [name: "configParam17", type: "enum", title: "Up Stage Timer Aux Heat", defaultValue: 0, options:[0:"Off",1:"30 minutes",2:"45 minutes",3:"60 minutes",4:"75 minutes",5:"90 minutes",6:"2 hours",7:"3 hours",8:"4 hours",9:"5 hours",10:"6 hours",11:"8 hours",12:"10 hours",13:"12 hours",14:"14 hours",15:"16 hours"]], parameterSize: 1],
        18: [input: [name: "configParam18", type: "enum", title: "Balance Point (Compressor Lockout)", defaultValue: 65, options:[0:"Off",5:"5°F",10:"10°F",15:"15°F",20:"20°F",25:"25°F",30:"30°F",35:"35°F",40:"40°F",45:"45°F",50:"50°F",55:"55°F",60:"60°F",65:"65°F"]], parameterSize: 1],
        19: [input: [name: "configParam19", type: "enum", title: "Aux Heat Lock Out (Aux Heat Outdoor Lockout)", defaultValue: 0, options:[0:"Off",5:"5°F",10:"10°F",15:"15°F",20:"20°F",25:"25°F",30:"30°F",35:"35°F",40:"40°F",45:"45°F",50:"50°F",55:"55°F",60:"60°F",65:"65°F"]], parameterSize: 1],
        20: [input: [name: "configParam20", type: "enum", title: "Cool 1 CPH (Cooling cycle rate stage 1)", defaultValue: 3, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6"]], parameterSize: 1],
        21: [input: [name: "configParam21", type: "enum", title: "Cool 2 CPH (Cooling cycle rate stage 2)", defaultValue: 3, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6"]], parameterSize: 1],
        22: [input: [name: "configParam22", type: "enum", title: "Heat 1 CPH (Heating cycle rate stage 1)", defaultValue: 3, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12"]], parameterSize: 1],
        23: [input: [name: "configParam23", type: "enum", title: "Heat 2 CPH (Heating cycle rate stage 2)", defaultValue: 3, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12"]], parameterSize: 1],
        24: [input: [name: "configParam24", type: "enum", title: "Aux Heat CPH (Heating cycle rate Auxiliary Heat)", defaultValue: 9, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12"]], parameterSize: 1],
        25: [input: [name: "configParam25", type: "enum", title: "EM Heat CPH (Heating cycle rate Emergency Heat)", defaultValue: 9, options:[1:"1",2:"2",3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12"]], parameterSize: 1],
        26: [input: [name: "configParam26", type: "enum", title: "Compressor Protection", defaultValue: 5, options:[0:"Off",1:"1 minutes",2:"2 minutes",3:"3 minutes",4:"4 minutes",5:"5 minutes"]], parameterSize: 1],
        27: [input: [name: "configParam27", type: "enum", title: "Adaptive Intelligent Recovery", defaultValue: 1, options:[0:"Off",1:"On"]], parameterSize: 1],
        28: [input: [name: "configParam28", type: "number", title: "Minimum Cool Temperature", description: "degrees fahrenheit", defaultValue: 50, range: "50..99"], parameterSize: 1],
        29: [input: [name: "configParam29", type: "number", title: "Maximum Heat Temperature", description: "degrees fahrenheit", defaultValue: 90, range: "40..90"], parameterSize: 1],
        30: [input: [name: "configParam30", type: "enum", title: "Air Filters", defaultValue: 0, options:[0:"0",1:"1",2:"2"]], parameterSize: 1],
        31: [input: [name: "configParam31", type: "enum", title: "Air Filter 1 Reminder", defaultValue: 0, options:[0:"Off",1:"10 run time days",2:"20 run time days",3:"30 run time days",4:"45 run time days",5:"60 run time days",6:"90 run time days",7:"120 run time days",8:"150 run time days",9:"30 days",10:"45 days",11:"60 days",12:"75 days",13:"3 months",14:"4 months",15:"5 months",16:"6 months",17:"9 months",18:"12 months",19:"15 months"]], parameterSize: 1],
        32: [input: [name: "configParam32", type: "enum", title: "Air Filter 2 Reminder", defaultValue: 0, options:[0:"Off",1:"10 run time days",2:"20 run time days",3:"30 run time days",4:"45 run time days",5:"60 run time days",6:"90 run time days",7:"120 run time days",8:"150 run time days",9:"30 days",10:"45 days",11:"60 days",12:"75 days",13:"3 months",14:"4 months",15:"5 months",16:"6 months",17:"9 months",18:"12 months",19:"15 months"]], parameterSize: 1],
        33: [input: [name: "configParam33", type: "enum", title: "Humidification Pad Reminder", defaultValue: 0, options:[0:"Off",1:"6 months",2:"12 months"]], parameterSize: 1],
        34: [input: [name: "configParam34", type: "enum", title: "Dehumidification Filter Reminder", defaultValue: 0, options:[0:"Off",1:"1 months",2:"2 months",3:"3 months",4:"4 months",5:"5 months",6:"6 months",7:"7 months",8:"8 months",9:"9 months",10:"10 months",11:"11 months",12:"12 months"]], parameterSize: 1],
        35: [input: [name: "configParam35", type: "enum", title: "Ventilation Filter Reminder", defaultValue: 0, options:[0:"Off",3:"3 months",6:"6 months",9:"9 months",12:"12 months"]], parameterSize: 1],
        36: [input: [name: "configParam36", type: "enum", title: "UV Devices", defaultValue: 0, options:[0:"0",1:"1",2:"2"]], parameterSize: 1],
        37: [input: [name: "configParam37", type: "enum", title: "UV Bulb 1 Reminder", defaultValue: 0, options:[0:"Off",6:"6 months",12:"12 months",24:"24 months"]], parameterSize: 1],
        38: [input: [name: "configParam38", type: "enum", title: "UV Bulb 2 Reminder", defaultValue: 0, options:[0:"Off",6:"6 months",12:"12 months",24:"24 months"]], parameterSize: 1],
        39: [input: [name: "configParam39", type: "enum", title: "Idle Brightness", defaultValue: 0, options:[0:"0",1:"1",2:"2",3:"3",4:"4",5:"5"]], parameterSize: 1],
        40: [input: [name: "configParam40", type: "enum", title: "Clock Format", defaultValue: 0, options: [0:"12 hour", 1:"24 hour"]], parameterSize:1],
        41: [input: [name: "configParam41", type: "enum", title: "Daylight Savings", defaultValue:1, options:[0:"Off",1:"On"]], parameterSize: 1],
        42: [input: [name: "configParam42", type: "enum", title: "Temperature Offset", defaultValue: 0, options:[(-3):"-3°F",(-2):"-2°F",(-1):"-1°F",0:"Off",1:"+1°F",2:"+2°F",3:"+3°F"]], parameterSize: 1],
        43: [input: [name: "configParam43", type: "enum", title: "Humidity Offset", defaultValue: 0, options:[(-12):"-12%",(-11):"-11%",(-10):"-10%",(-9):"-9%",(-8):"-8%",(-7):"-7%",(-6):"-6%",(-5):"-5%",(-4):"-4%",(-3):"-3%",(-2):"-2%",(-1):"-1%",0:"Off",1:"+1%",2:"+2%",3:"+3%",4:"+4%",5:"+5%",6:"+6%",7:"+7%",8:"+8%",9:"+9%",10:"+10%",11:"11%",12:"12%",]], parameterSize: 1]
]

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void configure() {
    if (!state.initialized) initializeVars()
    runIn(10, "syncClock")
    runIn(5, "pollDeviceData")
    runEvery3Hours("syncClock")
}

void initializeVars() {
    // first run only
    sendEvent(name:"supportedThermostatModes", value: supportedThermostatModes.toString().replace('[','[\"').replace(', ','\", \"').replace(']','\"]'), isStateChange:true)
    sendEvent(name:"supportedThermostatFanModes", value: supportedThermostatFanModes.toString().replace('[','[\"').replace(', ','\", \"').replace(']','\"]'), isStateChange:true)

    state.initialized=true
    runIn(15, refresh)
}

void installed() {
    if (logEnable) log.debug "installed()..."
    initializeVars()
}

void updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    unschedule()
    if (logEnable) runIn(1800,logsOff)
    runConfigs()
    runEvery3Hours("syncClock")
}

void SensorCal(value) {
    if (logEnable) log.debug "SensorCal($value)"
    List<hubitat.zwave.Command> cmds=[]
    cmds.addAll(configCmd(42,1,value))
    sendToDevice(cmds)
}

void HumidityCal(value) {
    if (logEnable) log.debug "HumidityCal($value)"
    List<hubitat.zwave.Command> cmds=[]
    cmds.addAll(configCmd(43,1,value))
    sendToDevice(cmds)
}

void IdleBrightness(value) {
    if (logEnable) log.debug "IdleBrightness($value)"
    List<hubitat.zwave.Command> cmds=[]
    cmds.addAll(configCmd(39,1,value))
    sendToDevice(cmds)
}

void zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
    Map evt = [isStateChange:false]
    log.info "Notification: " + ZWAVE_NOTIFICATION_TYPES[cmd.notificationType]
    if (cmd.notificationType==8) {
        // power management
        switch (cmd.event) {
            case 0:
                // idle
                break
            case 1:
                // Power has been applied
                evt.isStateChange=true
                evt.name="powerSource"
                evt.value="mains"
                evt.descriptionText="${device.displayName} AC mains applied"                    
                //log.info "${device.displayName} Power has been applied"
                break
            case 2:
                // AC mains disconnected
                evt.name="powerSource"
                evt.isStateChange=true
                evt.value="battery"
                evt.descriptionText="${device.displayName} AC mains disconnected"
                break
            case 3:
                // AC mains re-connected
                runIn(10,"syncClock")
                evt.name="powerSource"
                evt.isStateChange=true
                evt.value="mains"
                evt.descriptionText="${device.displayName} AC mains re-connected"
                break
            case 4:
                // surge detected
                log.warn "${device.displayName} surge detected"
                break
            case 5:
                // voltage drop / drift
                break
            case 6:
                // Over-current detected
                break
            case 7:
                // Over-voltage detected
                break
            case 8:
                // over-load detected
                break
            case 9:
                // load error
                break
            case 10:
                // replace battery soon
                log.info "${device.displayName} Low Battery"
                break
            case 11:
                // replace battery now
                break
            case 12:
                // battery is charging
                log.info "${device.displayName} Battery is charging"
                break
            case 13:
                // battery is fully charged
                break
            case 14:
                // charge battery soon
                break
            case 15:
                // charge battery now
                break
            case 16:
                // backup battery is low
                log.info "${device.displayName} Low Battery"
                break
            case 17:
                // battery fluid is low
                break
            case 18:
                // backup battery disconnected
                break
            case 254:
                log.info "unknown power management event / state - case 254"
                break
            default:
                log.info "unknown power management event / state - case ${cmd.event}"
        }
    } else {
        log.info "unhandled notification - ${cmd.event}"
        sendEvent(name:"unhandled", value:"${cmd.event}")
    }
    if (evt.isStateChange) {
        if (txtEnable) log.info evt.descriptionText
        eventProcess(evt)
    }
}

void checkFilter(){
/*class hubitat.zwave.commands.hrvstatusv1.HrvStatusGet {
    Short statusParameter
     static Short STATUS_PARAMETER_DISCHARGE_AIR_TEMPERATURE = 3
     static Short STATUS_PARAMETER_EXHAUST_AIR_TEMPERATURE = 2
     static Short STATUS_PARAMETER_OUTDOOR_AIR_TEMPERATURE = 0
     static Short STATUS_PARAMETER_RELATIVE_HUMIDITY_IN_ROOM = 5
     static Short STATUS_PARAMETER_REMAINING_FILTER_LIFE = 6
     static Short STATUS_PARAMETER_ROOM_TEMPERATURE = 4
     static Short STATUS_PARAMETER_SUPPLY_AIR_TEMPERATURE = 1

     List<Short> getPayload()
     String format()
}*/

    //List<hubitat.zwave.Command> cmds=[]
    //cmds.add(zwave.hrvstatusv1.HrvStatusGet((Short)0x06))
    //log.debug "${hubitat.zwave.Zwave.getHrvStatusV1.properties}"//hubitat.zwave.Zwave.properties}"
    //sendToDevice([zwave.hrvStatusV1.hrvStatusGet(statusParameter:(Short)0x06)])
    log.debug sendToDevice([zwave.hrvStatusV1.hrvStatusGet(statusParameter:(Short)0x06)])
}

void zwaveEvent(hubitat.zwave.commands.hrvstatusv1.HrvStatusReport cmd) {
/*class hubitat.zwave.commands.hrvstatusv1.HrvStatusReport {
     Short precision
     Short scale
     BigDecimal scaledValue
     Short size
     Short statusParameter
     List<HrvStatusReport> value
     static Short STATUS_PARAMETER_DISCHARGE_AIR_TEMPERATURE = 3
     static Short STATUS_PARAMETER_EXHAUST_AIR_TEMPERATURE = 2
     static Short STATUS_PARAMETER_OUTDOOR_AIR_TEMPERATURE = 0
     static Short STATUS_PARAMETER_RELATIVE_HUMIDITY_IN_ROOM = 5
     static Short STATUS_PARAMETER_REMAINING_FILTER_LIFE = 6
     static Short STATUS_PARAMETER_ROOM_TEMPERATURE = 4
     static Short STATUS_PARAMETER_SUPPLY_AIR_TEMPERATURE = 1

     List<Short> getPayload()
     String format()
*/
    log.debug cmd
}


void runConfigs() {
    List<hubitat.zwave.Command> cmds=[]
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
        }
    }
    sendToDevice(cmds)
}

List<hubitat.zwave.Command> pollConfigs() {
    List<hubitat.zwave.Command> cmds=[]
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.add(zwave.configurationV1.configurationGet(parameterNumber: param.toInteger()))
        }
    }
    return cmds
}

List<hubitat.zwave.Command> configCmd(parameterNumber, size, scaledConfigurationValue) {
    if (logEnable) log.debug "ParameterNumber: ${parameterNumber}, Size: ${size}, Value: ${scaledConfigurationValue}"
    List<hubitat.zwave.Command> cmds = []
    int intval=scaledConfigurationValue.toInteger()
    if (intval<0) intval=256 + intval
    cmds.add(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: [(intval & 0xFF)]))
    cmds.add(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()))
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    int scaledValue
    cmd.configurationValue.reverse().eachWithIndex { v, index -> scaledValue=scaledValue | v << (8*index) }
    if(configParams[cmd.parameterNumber.toInteger()]) {
        Map configParam=configParams[cmd.parameterNumber.toInteger()]
        if (scaledValue > 127) scaledValue = scaledValue - 256
        device.updateSetting(configParam.input.name, [value: "${scaledValue}", type: configParam.input.type])
        if (cmd.parameterNumber==42) {
            eventProcess(name: "currentSensorCal", value: scaledValue)
        }
        if (cmd.parameterNumber==39) {
            eventProcess(name: "idleBrightness", value: scaledValue)
        }
        if (cmd.parameterNumber==43) {
            eventProcess(name: "currentHumidityCal", value: scaledValue)
        }
    }
}

void eventProcess(Map evt) {
    if(evt.name == "temperature"){
        if(device.currentValue(evt.name) == null) sendEvent(evt)
        else {
            absDiff = Math.abs(device.currentValue(evt.name).toFloat() - evt.value.toFloat())
            if(deltaTempRpt == null) deltaTempRpt = 1
            if(absDiff >= deltaTempRpt.toFloat() || state?.refreshingTemp){
                state.refreshingTemp = false
                evt.isStateChange=true
                sendEvent(evt)        
            }
        }
    } else if(evt.name == "humidity"){
        if(device.currentValue(evt.name) == null) sendEvent(evt)
        else {        
            absDiff = Math.abs(device.currentValue(evt.name).toFloat() - evt.value.toFloat())
            if(deltaHumRpt == null) deltaHumRpt = 1
            if(absDiff >= deltaHumRpt.toFloat() || state?.refreshingHum){
                state.refreshingHum = false
                evt.isStateChange=true
                sendEvent(evt)        
            }
        }
    } else if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
        evt.isStateChange=true
        sendEvent(evt)
    }    
}

void pollDeviceData() {
    List<hubitat.zwave.Command> cmds = []
    cmds.addAll(processAssociations())
    cmds.add(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1))
    cmds.add(zwave.versionV2.versionGet())
    cmds.addAll(pollConfigs())
    sendToDevice(cmds)
}

void refresh() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.batteryV1.batteryGet())
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1, scale: configParam2==0?1:0))
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5, scale: 0))
    cmds.add(zwave.thermostatFanModeV3.thermostatFanModeGet())
    cmds.add(zwave.thermostatFanStateV1.thermostatFanStateGet())
    cmds.add(zwave.thermostatModeV2.thermostatModeGet())
    cmds.add(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet())
    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 1))
    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 2))
    cmds.add(zwave.basicV1.basicGet())
    sendToDevice(cmds)
    state.refreshingTemp = true
    state.refreshingHum = true
    runIn(10, "syncClock")
}

void syncClock() {
    Calendar currentDate = Calendar.getInstance()
    sendToDevice(zwave.clockV1.clockSet(hour: currentDate.get(Calendar.HOUR_OF_DAY), minute: currentDate.get(Calendar.MINUTE), weekday: currentDate.get(Calendar.DAY_OF_WEEK)))
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

void zwaveEvent(hubitat.zwave.commands.multicmdv1.MultiCmdEncap cmd) {
    if (logEnable) log.debug "Got multicmd: ${cmd}"
    cmd.encapsulatedCommands(CMD_CLASS_VERS).each { encapsulatedCommand ->
        zwaveEvent(encapsulatedCommand)
    }
}

void parse(String description) {
    if(forceTimeSync > 0)runIn(10,"activityCheck")    
    if (logEnable) log.debug "parse:${description}"
    hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
    if (cmd) {
        zwaveEvent(cmd)
    } else if(txtEnable)
        log.info "Non-cmd received: $description"
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    if (logEnable) log.debug "Supervision get: ${cmd}"
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
    sendToDevice(new hubitat.zwave.commands.supervisionv1.SupervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0))
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report: ${cmd}"
    switch (cmd.deviceIdType) {
        case 1:
            // serial number
            def serialNumber=""
            if (cmd.deviceIdDataFormat==1) {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
            } else {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    if (logEnable) log.debug "version2 report: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void sendToDevice(List<hubitat.zwave.Command> cmds) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(hubitat.zwave.Command cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

List<String> commands(List<hubitat.zwave.Command> cmds, Long delay=200) {
    return delayBetween(cmds.collect{ secureCommand(it) }, delay)
}

String secureCommand(hubitat.zwave.Command cmd) {
    secureCommand(cmd.format())
}

String secureCommand(String cmd) {
    //return zwaveSecureEncap(cmd)
    String encap=""
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd
    } else {
        encap = "988100"
    }
    return "${encap}${cmd}"
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

List<hubitat.zwave.Command> setDefaultAssociation() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId))
    cmds.add(zwave.associationV2.associationGet(groupingIdentifier: 1))
    return cmds
}

List<hubitat.zwave.Command> processAssociations(){
    List<hubitat.zwave.Command> cmds = []
    cmds.addAll(setDefaultAssociation())
    return cmds
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    List<String> temp = []
    if (cmd.nodeId != []) {
        cmd.nodeId.each {
            temp.add(it.toString().format( '%02x', it.toInteger() ).toUpperCase())
        }
    }
    updateDataValue("zwaveAssociationG${cmd.groupingIdentifier}", "$temp")
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    if (logEnable) log.debug "got battery report: ${cmd.batteryLevel}"
    if (cmd.batteryLevel == device.currentValue("battery")) {
        if (logEnable) log.debug "Battery level same as before. Ignoring."
        return
    }
    Map evt = [name: "powerSource", value: "battery"]
    if(device.currentValue("powerSource") == null)
        eventProcess(evt)
    evt.name = "battery"
    evt.unit = "%"
    if (cmd.batteryLevel == 0xFF) {
        evt.descriptionText = "${device.displayName} has a low battery"
        evt.value = "1"
    } else {
        evt.descriptionText = "${device.displayName} battery is ${cmd.batteryLevel}%"
        evt.value = "${cmd.batteryLevel}"
    }
    if (txtEnable) log.info evt.descriptionText
    eventProcess(evt)
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    if (cmd.sensorType.toInteger() == 1) {
        if (logEnable) log.debug "got temp: ${cmd.scaledSensorValue}"
        if (celsiusToFahrenheit(cmd.scaledSensorValue) == device.currentValue("temperature") 
            || fahrenheitToCelsius(cmd.scaledSensorValue) == device.currentValue("temperature") 
            || cmd.scaledSensorValue == device.currentValue("temperature")) {
            if (logEnable) log.debug "Temperature same as before. Ignoring."
            return
        }
        if(configParam2.toInteger() != cmd.scale.toInteger()) //temperature scale and configParam2 have reversed values
            eventProcess(name: "temperature", value: cmd.scaledSensorValue, unit: cmd.scale == 1 ? "F" : "C")//, isStateChange: sendAllTemps)
        else //Temperature Scale reported mismatch 
            adjustReportedTemp(cmd.scaledSensorValue, cmd.scale)
    } else if (cmd.sensorType.toInteger() == 5) {
        if (logEnable) log.debug "got humidity: ${cmd.scaledSensorValue}"//if (logEnable) log.debug "got temp: ${cmd.scaledSensorValue}"
        if (cmd.scaledSensorValue == device.currentValue("humidity")) {
            if (logEnable) log.debug "Humidity same as before. Ignoring."
            return
        }
        if(cmd.scaledSensorValue>=0 && cmd.scaledSensorValue<=100) 
            eventProcess(name: "humidity", value: Math.round(cmd.scaledSensorValue), unit: cmd.scale == 0 ? "%": "g/m³")//, isStateChange: sendAllTemps)
    }
}

void setpointCalc(String newmode, String unit, value) {
    String mode="cool"
    if (device.currentValue("thermostatMode")=="heat" || device.currentValue("thermostatMode")=="emergency heat") {
        state.lastMode="heat"
        mode="heat"
    } else if (device.currentValue("thermostatMode")=="cool") {
        state.lastMode="cool"
        mode="cool"
    } else if (device.currentValue("thermostatOperatingState")=="heating" || device.currentValue("thermostatOperatingState")=="pending heat") {
        state.lastMode="heat"
        mode="heat"
    } else if (device.currentValue("thermostatOperatingState")=="cooling" || device.currentValue("thermostatOperatingState")=="pending cool") {
        state.lastMode="cool"
        mode="cool"
    } else if (state.lastMode) {
        mode=state.lastMode
    }
    if (newmode==mode) {
        eventProcess(name: "thermostatSetpoint", value: String.format("%.1f",value), unit: unit, type: state.isDigital?"digital":"physical")
    }
}

void zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd) {
    if (logEnable) log.debug "Got thermostat setpoint report: ${cmd}"
    if (device.currentValue("thermostatMode")=="heat") mode="heat"
    if (device.currentValue("thermostatMode")=="cool") mode="cool"
    String unit=cmd.scale == 1 ? "F" : "C"
    switch (cmd.setpointType) {
        case 1:
            eventProcess(name: "heatingSetpoint", value: String.format("%.1f",cmd.scaledValue), unit: unit, type: state.isDigital?"digital":"physical")
            setpointCalc("heat", unit, cmd.scaledValue)
            break
        case 2:
            eventProcess(name: "coolingSetpoint", value: String.format("%.1f",cmd.scaledValue), unit: unit, type: state.isDigital?"digital":"physical")
            setpointCalc("cool", unit, cmd.scaledValue)
            break
    }
    state.isDigital=false
}

void zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd) {
    if (logEnable) log.debug "Got thermostat operating state report: ${cmd}"
    String newstate=THERMOSTAT_OPERATING_STATE[cmd.operatingState.toInteger()]
    if (logEnable) log.debug "Translated state: " + newstate
    eventProcess(name: "thermostatOperatingState", value: newstate)
    if (newstate=="cooling") {
        state.lastMode="cool"
    } else if (newstate=="heating") {
        state.lastMode="heat"
    } else if (newstate=="pending heat") {
        state.lastMode="heat"
    } else if (newstate=="pending cool") {
        state.lastMode="cool"
    }
}

void zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
    if (logEnable) log.debug "Got thermostat fan state report: ${cmd}"
    String newstate=THERMOSTAT_FAN_STATE[cmd.fanOperatingState.toInteger()]
    if (logEnable) log.debug "Translated fan state: " + newstate
    eventProcess(name: "thermostatFanOperatingState", value: newstate)
    sendToDevice(zwave.configurationV1.configurationGet(parameterNumber: 52))
    if (newstate=="idle" && (device.currentValue("thermostatOperatingState")=="heating" || device.currentValue("thermostatOperatingState")=="cooling")) 
        sendToDevice(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet())
}

void zwaveEvent(hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport cmd) {
    if (logEnable) log.debug "Got thermostat fan mode report: ${cmd}"
    String newmode=THERMOSTAT_FAN_MODE[cmd.fanMode.toInteger()]
    if (logEnable) log.debug "Translated fan mode: " + newmode
    eventProcess(name: "thermostatFanMode", value: newmode, type: state.isDigital?"digital":"physical")
    state.isDigital=false
}

void zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
    if (logEnable) log.debug "Got thermostat mode report: ${cmd}"
    String newmode=THERMOSTAT_MODE[cmd.mode.toInteger()]
    if (logEnable) log.debug "Translated thermostat mode: " + newmode
    eventProcess(name: "thermostatMode", value: newmode, type: state.isDigital?"digital":"physical")
    state.isDigital=false
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    if (logEnable) log.debug "Got presence report: ${cmd}"
    String newmode=PRESENCE_VALUE[cmd.value.toInteger()]
    if (logEnable) log.debug "Translated presence: " + newmode
    eventProcess(name: "presence", value: newmode, type: state.isDigital?"digital":"physical")
    state.isDigital=false
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    // setup basic reports for missed operating state changes
    if (cmd.value.toInteger()==0xFF) {
        if (device.currentValue("thermostatOperatingState")!="heating" && device.currentValue("thermostatOperatingState")!="cooling") sendToDevice(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet())
    } else {
        if (device.currentValue("thermostatOperatingState")=="heating" || device.currentValue("thermostatOperatingState")=="cooling") sendToDevice(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet())
    }
}

private void setSetpoint(setPointType, value) {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointSet(setpointType: setPointType, scale: getTemperatureScale()=="F" ? 1:0 , precision: 0, scaledValue: new BigDecimal(value)))//cmds.add(zwave.thermostatSetpointV2.thermostatSetpointSet(setpointType: setPointType, scale: getTemperatureScale()=="F" ? 1:0 , precision: 0, scaledValue: value))
    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: setPointType))
    state.isDigital=true
    sendToDevice(cmds)
}

void setHeatingSetpoint(degrees) {
    if (logEnable) log.debug "setHeatingSetpoint(${degrees}) called"
    runIn(3,"refresh")
    setSetpoint(1,degrees)
    state.isDigital=true
}

void setCoolingSetpoint(degrees) {
    if (logEnable) log.debug "setCoolingSetpoint(${degrees}) called"
    runIn(3,"refresh")
    setSetpoint(2,degrees)
    state.isDigital=true
}

void setThermostatMode(mode) {
    if (logEnable) log.debug "setThermostatMode($mode)"
    List<hubitat.zwave.Command> cmds = []
    if (logEnable) log.debug "setting zwave thermostat mode ${SET_THERMOSTAT_MODE[mode]}"
    cmds.add(zwave.thermostatModeV2.thermostatModeSet(mode: SET_THERMOSTAT_MODE[mode]))
    cmds.add(zwave.thermostatModeV2.thermostatModeGet())
    state.isDigital=true
    sendToDevice(cmds)
}

void activityCheck() {
    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")
    Long devActivity = sdf.parse("${device.lastActivity}").getTime()
    Long timeDiff = (Long) new Date().getTime() - devActivity
    if(timeDiff/60000 >= 60*forceTimeSync)
        syncClock()
}

void adjustReportedTemp(temp, scale){ //scale is reported scale, needs to be reversed
    if(scale == 0){
        temp = celsiusToFahrenheit(temp)
        scale = 1
    }else{
        temp = fahrenheitToCelsius(temp)
        scale = 0
    }
    eventProcess(name: "temperature", value: temp, unit: scale == 1 ? "F" : "C")//, isStateChange: sendAllTemps)
}

void off() {
    state.isDigital=true
    setThermostatMode("off")
}

void on() {
    log.warn "Ambiguous use of on()"
}

void heat() {
    state.isDigital=true
    setThermostatMode("heat")
}

void emergencyHeat() {
    state.isDigital=true
    setThermostatMode("emergency heat")
}

void cool() {
    state.isDigital=true
    setThermostatMode("cool")
}

void auto() {
    state.isDigital=true
    setThermostatMode("auto")
}

void setThermostatFanMode(mode) {
    if (logEnable) log.debug "setThermostatFanMode($mode)"
    List<hubitat.zwave.Command> cmds = []
    if (logEnable) log.debug "setting zwave thermostat fan mode ${SET_THERMOSTAT_FAN_MODE[mode]}"
    cmds.add(zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: SET_THERMOSTAT_FAN_MODE[mode]))
    cmds.add(zwave.thermostatFanModeV3.thermostatFanModeGet())
    state.isDigital=true
    sendToDevice(cmds)
}

void fanOn() {
    state.isDigital=true
    setThermostatFanMode("on")
}

void fanAuto() {
    state.isDigital=true
    setThermostatFanMode("auto")
}

void fanCirculate() {
    state.isDigital=true
    setThermostatFanMode("circulate")
}

void home() {
    sendEvent(name:"presence",value:"present")
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.basicV1.basicSet(value:0xff))
    sendToDevice(cmds)
}

void away() {
    sendEvent(name:"presence", value:"not present")
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.basicV1.basicSet(value:0x00))
    sendToDevice(cmds)
}

void setSchedule() {
    log.warn "setSchedule is not supported by this driver"
}
