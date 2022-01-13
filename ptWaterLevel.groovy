/*
 * PT Device 
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
 *    12Jan2022    thebearmay    Add computations based on observed values
 *    13Jan2022    thebearmay    Add html attribute
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.1.2"}

metadata {
    definition (
        name: "PT Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/ptWaterLevel.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        attribute "tx1", "number"
        attribute "tx1z", "number"
        attribute "tx2", "decimal"
        attribute "tx3", "decimal"
        attribute "battery", "number"
        attribute "fillPct", "number"
        attribute "compLiquid", "number"
        attribute "html", "string"
//        command "forceCompute"
    }   
}

preferences {
    input("serverAddr", "text", title:"IP address and path to Poll:", required: true, submitOnChange:true)
    input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true)
    input("obsFull", "number", title: "Observed Full Tx Value", submitOnChange: true)
    input("obsEmpty", "number", title: "Observed Empty Tx Value", submitOnChange: true)
    input("tankCap", "number", title: "Tank Capacity", submitOnChange: true)
    input("volumeUnit", "text", title: "Unit for Volume, i.e. <i>G</i>allon, <i>L</i>iter, etc.", submitOnChange:true)
    input("security", "bool", title: "Hub Security Enabled?", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
    input("debugEnable", "bool", title: "Enable debug logging?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){

}

@SuppressWarnings('unused')
def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }			
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnable) log.debug "configure()"
    getPollValues()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void getPollValues(){
    // start - Modified from dman2306 Rebooter app
    String cookie=(String)null
    if(security) {
        httpPost(
            [
                uri: "http://127.0.0.1:8080",
                path: "/login",
                query: [ loginRedirect: "/" ],
                body: [
                    username: username,
                    password: password,
                    submit: "Login"
                ]
            ]
        ) { resp -> cookie = ((List)((String)resp?.headers?.'Set-Cookie')?.split(';'))?.getAt(0) }
    }
    // End - Modified from dman2306 Rebooter app

    Map params = [
        uri    : serverAddr,
        headers: ["Cookie": cookie]
    ]
    if (debugEnable) log.debug params
    asynchttpGet("getPTData", params)

    if (debugEnable) log.debug "tempPollRate: $tempPollRate"


    if(tempPollRate == null){
        device.updateSetting("tempPollRate",[value:300,type:"number"])
        runIn(300,"getPollValues")
    }else if(tempPollRate > 0){
        runIn(tempPollRate,"getPollValues")
    }
}
//{"free_space":"667648","rx_id":"227","tx_id":"50","tx_rssi":"-71","rx_rssi":"-59","firmware_version":"212","hardware_version":"4","id":"483FDA91E94F","ip":"10.54.25.254","subnet":"255.255.255.0","gateway":"10.54.25.1","dns":"unknown","tx_firmware_version":"7","tx_hardware_version":"5","fails":"3","rx_sensors":"[]","tx_sensors":"[{"1":366,"z":58},{"2":6.16},{"3":-15.01}]"}

@SuppressWarnings('unused')
void getPTData(resp, data){
    try{
        if (resp.getStatus() == 200){
            if (debugEnable) log.info resp.data
            dataIn = resp.data.toString()
            focusData = dataIn.substring(dataIn.indexOf('"tx_sensors":"')+14,dataIn.length()-2)
            focusData = focusData.replace("{","")
            focusData = focusData.replace("}","")
            updateAttr("debug",focusData)
            HashMap retData=(HashMap)evaluate(focusData)
            updateAttr("tx1",retData['1'])
            updateAttr("txz",retData['z'])
            updateAttr("tx2",retData['2'])
            updateAttr("tx3",retData['3'])
            computeValues()
            buildHtml()
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}   

/*void forceCompute() {
    updateAttr("tx1", 529)
    updateAttr("tx2", 5.95)
    computeValues()
}*/

@SuppressWarnings('unused')
void computeValues() {
    if(obsFull && obsEmpty){
        Integer fillPct = (((device.currentValue("tx1", true).toInteger() - obsEmpty)/(obsFull - obsEmpty))*100)
        updateAttr("fillPct", fillPct, "%")
        if(tankCap > 0){
            Integer computedFill = (tankCap * (fillPct / 100)) 
            updateAttr("compLiquid", computedFill, volumeUnit)
        }        
    }
    // Unit uses 4 AA batteries with a nominal voltage reported of ~6.0v, 1.5v/battery is considered full and 1.2v is considered "dead"
    Integer battery = (((device.currentValue("tx2").toDouble() - (1.2 * 4)) / (6 - (1.2 * 4)))*100)
    updateAttr("battery", battery, "%")
                                                                            
}

@SuppressWarnings('unused')
void buildHtml() {
    String htmlStr = ""
    htmlStr+="<div class='tile-contents'>"
    htmlStr+="<div class='tile-primary'>Fill Level ${device.currentValue('fillPct',true)}%<br/>${device.currentValue('compLiquid',true)} ${device.currentState('compLiquid')?.unit}</div>"
    htmlStr+="<div style='text-align:left;position:absolute;top:4px;left:8px;font-size:12px'>${device.currentValue('battery',true)}%</div>"
    htmlStr+="</div>"
    updateAttr("html",htmlStr)
}


@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
