/*
 * Air Things Device
 *
 *
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date         Who           What
 *    ----         ---           ----
 *    16Oct2022    thebearmay    add capability CarbonDioxideMeasureMent,RelativeHumidityMeasurement
 *    21Nov2022    thebearmay    make the tile template selection an ENUM
 *                               add absHumidity
 *    30Nov2022    thebearmay    add option to force Integer values, add mold attribute
 *    16Dec2022    thebearmay    handle mismatched return data elements
 *    22Dec2022    thebearmay    hub security
 *    15Jan2023    thebearmay    add descriptionText
 *                               trap relayDeviceType
 *    04Dec2023    thebearmay    PM25 -> AQI
 *    28Mar2024	   gfa 			 add option for BaroPres conversion from mBar -> inHg
 *    31Aug2025	   thebearmay	 make sure format is selected before use
 *    04Sep2025					 put the template processing back in
 */
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.transform.Field
#include thebearmay.localFileMethods
#include thebearmay.templateProcessing

@SuppressWarnings('unused')
static String version() {return "0.0.21"}

metadata {
    definition (
        name: "Air Things Device",
        namespace: "thebearmay",
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/airthings/airthingsDevice.groovy"
    ) {
        capability "Actuator"
        capability "AirQuality"
        capability "Battery"
        capability "CarbonDioxideMeasurement"
        capability "Initialize"
        capability "PressureMeasurement"
        capability "Refresh"
        capability "RelativeHumidityMeasurement"
        capability "TemperatureMeasurement"

        attribute "radonShortTermAvg", "number"
        attribute "co2", "number"
        attribute "voc", "number"
        attribute "pm1", "number"
        attribute "pm10", "number"
        attribute "pm11", "number"
        attribute "pm12", "number"
        attribute "pm13", "number"
        attribute "pm14", "number"
        attribute "pm15", "number"
        attribute "pm16", "number"
        attribute "pm17", "number"
        attribute "pm18", "number"
        attribute "pm19", "number"
        attribute "pm2", "number"
        attribute "pm20", "number"
        attribute "pm21", "number"
        attribute "pm22", "number"
        attribute "pm23", "number"
        attribute "pm24", "number"
        attribute "pm25", "number"
        attribute "pm26", "number"
        attribute "pm27", "number"
        attribute "pm28", "number"
        attribute "pm29", "number"
        attribute "pm25Aqi","number"
        attribute "pm25AqiText","string"
        attribute "lastPoll", "string" //GFA
        attribute "absHumidity", "number"
        attribute "mold", "number"
        attribute "html", "string"
    	attribute "relayDeviceType", "string"    //GFA
        attribute "rssi", "number"

//        command "test",[[name:"val*", type:"NUMBER", description:"pm25 Value"]]
    }
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", defaultValue:false)
    input("useFahrenheit", "bool", title: "Use Fahrenheit", defaultValue:false)
    input("usePicoC", "bool", title: "Use pCi/L for Radon", defaultValue:false)
    input("useinHg", "bool", title: "Use inHg for Barometric Pressure", defaultValue:false) //GFA Added
    input("forceInt", "bool", title: "Store values as Integer", defaultValue: false)
    input("pollRate", "number", title: "Sensor Polling Rate (minutes)\nZero for no polling:", defaultValue:0)
    input("lstPollSdfPref", "enum", title: "Date/Time Format for Last Poll", options:sdfList, defaultValue:"yyyy-MM-dd HH:mm:ss", width:4)
//    input("security", "bool", title: "Enable if using Hub Security", defaultValue: false, submitOnChange:true)
//    if(security){
//        input("username","string", title:"Hub Security Username")
//        input("password","string", title:"Hub Security Password")
//    }
    fileList = listFiles()

    input("tileTemplate", "enum", title:"Template for generating HTML for dashboard tile", options:fileList, defaultValue:"--No Selection--", submitOnChange:true)
}

@SuppressWarnings('unused')
def installed() {
    log.trace "${device.displayName} v${version()} installed()"
    initialize()
}

def initialize(){
   updated()
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    if(pollRate == null)
        device.updateSetting("pollRate",[value:0,type:"number"])
    if(pollRate > 0){
        runIn(pollRate*60,"refresh")
    } else
        unschedule("refresh")
    refresh()
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnabled) log.debug "configure()"

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    def desc = "${aKey} level of ${aValue} detected"
    sendEvent(name:aKey, value:aValue, unit:aUnit, descriptionText:desc)
}

void refresh() {
    parent.updateChild(device.data.deviceId)
    if(pollRate > 0)
        runIn(pollRate*60,"refresh")
}

void dataRefresh(retData){
    if(debugEnabled) log.debug "$retData.data ${retData.data.size()}"
    retData.data.each{
        if(debugEnabled) log.debug "Each:${it.key}"
        def unit = ""
        def attr = it.key
        def value = it.value
        switch (attr){
            case("temp"):
            	attr = "temperature"
                unit="°C"
                if(useFahrenheit){
                    value = celsiusToFahrenheit(value)
                    unit = "°F"
                }
                break
            case("radonShortTermAvg"):
                if(usePicoC){
                    value = (value/37).toFloat().round(2)
                    unit="pCi/L"
                }else
                    unit="Bq/m<sup>3</sup>"
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("humidity"):
                unit="%"
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("co2"):
                unit="ppm"
                if(forceInt) value = value.toFloat().toInteger()
                updateAttr("carbonDioxide", value, unit) //required for capability CarbonDioxideMeasurement, co2 retained for backward compatibility
                break
            case("pressure"):
            	if(useinHg){ //GFA
					value = (value/33.86389).toFloat().round(3) //GFA
					unit = "inHg" //GFA
				}else //GFA
                	unit="mBar"
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("voc"):
                unit="ppb"
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("battery"):
                unit="%"
                break
            case("rssi"):
                unit="dBm"
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("mold")://mold risk index - integer 0-10
                if(forceInt) value = value.toFloat().toInteger()
                break
            case("relayDeviceType"):
                break
            case("time"):// update timestamp
            	attr = "lastPoll"
				Date lastPoll = new Date(1000 * value.longValue()) // As need UNIX date which is in msec (and API value is in secs)
				SimpleDateFormat sdf = new SimpleDateFormat(lstPollSdfPref ?: "yyyy-MM-dd HH:mm:ss")
				value = sdf.format(lastPoll)
                //state.lastUpdate = value.toInteger()
                break
            default:
                try{
                    value = Math.floor(10 * value.toFloat()) / 10
                } catch(e) {
                    log.warn "Return Data Mismatch, Key: ${attr} Value: ${value} - value will be set to zero"
                    value = 0
                }
                break
        }
        if(debugEnabled) log.debug "${attr}:${value}${unit}"
        updateAttr(attr, value, unit)
        if(attr == "pm25")
            calcPm25Aqi(value)
    }
    calcAbsHumidity()
    if(tileTemplate && tileTemplate != "No selection" && tileTemplate != "--No Selection--"){
        def tileHtml = genHtml(tileTemplate)
        updateAttr("html","$tileHtml")
    }

}

void calcAbsHumidity() {
    if(device.currentValue("temperature",true) == null || device.currentValue("humidity",true) == null)
        return //Calculation cannot continue
    def deviceTempInCelsius
    if(useFahrenheit)
        deviceTempInCelsius = fahrenheitToCelsius(device.currentValue("temperature",true).toFloat())
    else
        deviceTempInCelsius = device.currentValue("temperature",true).toFloat()
    //(6.112 × e^[(17.67 × T)/(T+243.5)] × rh × 2.1674)     / (273.15+T)
    Double numerator = 6.112 * Math.exp((17.67 * deviceTempInCelsius)/(deviceTempInCelsius + 243.5)) * device.currentValue("humidity",true).toFloat() * 2.1674
    Double denominator = (273.15+deviceTempInCelsius)
    Double absHumidity = numerator/denominator
    //updateAttr("d", denominator)
    //updateAttr("n", numerator)
    def absHumidityR = absHumidity.round(2)
    updateAttr("absHumidity", absHumidityR, "g/m<sup>3</sup>")
}

void test(val){
    log.debug "test($val)"
    x=[:]
    x.data = [pm25:val]
    dataRefresh(x)
}


@Field static final aqiLevel = [[max: 50,  color: "green", name: "Good"],
                                [max: 100, color: "yellow", name: "Moderate"],
                                [max: 150, color: "orange", name: "Unhealthy for sensitive groups"],
                                [max: 200, color: "red", name: "Unhealthy"],
                                [max: 300, color: "purple", name: "Very unhealthy"],
                                [max: 500, color: "maroon", name: "Hazardous"]]

void calcPm25Aqi(pm25Val){
    if(debugEnabled)
        log.debug "calcPm25Aqi($pm25Val)"
    Float a = pm25Val.toFloat();

    // breakpoints - February 7, 2024 update as per https://www.epa.gov/system/files/documents/2024-02/pm-naaqs-air-quality-index-fact-sheet.pdf
    // New BreakPoints GFA
    Float c = a < 0 ? 0 // values below 0 are considered beyond AQI
        //: a < 9.1 ? lerp(  0.0,  9.0,   0,  50, a)
    	: a < 12.1 ? lerp( 0.0, 12.0, 0, 50, a)
        : a < 35.5 ? lerp( 12.1,  35.4,  51, 100, a)
        : a < 55.5 ? lerp( 35.5,  55.4, 101, 150, a)
        //: a < 125.5 ? lerp( 55.5, 125.4, 151, 200, a)
        //: a < 225.5 ? lerp(150.5, 225.4, 201, 300, a)
        //: a < 325.5 ? lerp(225.5, 325.4, 301, 500, a)
		: a < 150.5 ? lerp( 55.5, 150.4, 151, 200, a)
		: a < 250.5 ? lerp(150.5, 250.4, 201, 300, a)
		: a < 350.5 ? lerp(250.5, 350.4, 301, 400, a)
		: a < 500.5 ? lerp(350.5, 500.4, 401, 500, a)
        : 500// values above 500 are considered beyond AQI
    if(debugEnabled) log.debug "lerp returned $c"
    Float aLevel = Math.floor(10 * c) / 10
    updateAttr("pm25Aqi",aLevel)
    updateAttr("airQualityIndex",aLevel.toInteger())
    for (int i=0;i<aqiLevel.size();i++){
        if(debugEnabled) log.debug "$aLevel:${aqiLevel[i].max}"
        if(aLevel <= aqiLevel[i].max){
            def pm25AqiText = "<span style='color:${aqiLevel[i].color}'>${aqiLevel[i].name}</span>"
            updateAttr("pm25AqiText",pm25AqiText)
            break
        }
    }
}

float lerp(plo, phi, ilo, ihi, p) {
    if(debugEnabled)
        log.debug "lerp $plo $phi $ilo $ihi $p"
    float calcAqi = (((ihi-ilo)/(phi-plo))*(p-plo))+ilo
    if(calcAqi > ihi.toFloat()) calcAqi = ihi
    return calcAqi
}

@SuppressWarnings('unused')
List<String> listFiles(){
//    if(security) cookie = securityLogin().cookie
    if(debugEnabled) log.debug "Getting list of files"
    uri = "http://127.0.0.1:8080/hub/fileManager/json";
    def params = [
        uri: uri,
        headers: [
				"Cookie": cookie
            ]
    ]
    try {
        fileList = []
        httpGet(params) { resp ->
            if (resp != null){
                if(debugEnabled) log.debug "Found the files"
                def json = resp.data
                for (rec in json.files) {
                    fileList << rec.name
                }
            } else {
                //
            }
            fileList.add("--No Selection--")
        }
        //if(debugEnabled) log.debug fileList.sort()
        return fileList.sort()
    } catch (e) {
        log.error e
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

@Field static final sdfList = ["yyyy-MM-dd","yyyy-MM-dd HH:mm","yyyy-MM-dd h:mma","yyyy-MM-dd HH:mm:ss","ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "HH:mm", "H:mm","h:mma", "HH:mm:ss", "Milliseconds"]
