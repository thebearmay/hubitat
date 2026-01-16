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
 *    14Jan2022    thebearmay    Option to use factory zero instead of observed
 *    21Feb2022    thebearmay    Update values on hub restart
 *	  13Jan2026					 Strip out Hub Security, address 408 issue and look at potential JDK incapabilities
*/
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.1.10"}

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
        capability "Refresh"
        attribute "tx1", "number"
        attribute "tx1z", "number"
        attribute "tx2", "decimal"
        attribute "tx3", "decimal"
        attribute "battery", "number"
        attribute "fillPct", "number"
        attribute "compLiquid", "number"
        attribute "html", "string"
    }   
}

preferences {
    input("serverAddr", "text", title:"IP address and path to Poll:", required: true, submitOnChange:true)
    input("tempPollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true)
    input("obsFull", "number", title: "Observed Full Tx Value", submitOnChange: true)
    input("obsEmpty", "number", title: "Observed Empty Tx Value", submitOnChange: true)
    input("tankCap", "number", title: "Tank Capacity", submitOnChange: true)
    input("volumeUnit", "text", title: "Unit for Volume, i.e. <i>G</i>allons, <i>L</i>iters, etc.", submitOnChange:true)
    input("factoryZero", "bool", title: "Use Factory Zero instead of Observed", defaultValue: false, submitOnChange: true)
	input("debugEnable", "bool", title: "Enable debug logging?")
    input("warnSuppress", "bool", title: "Suppress Warnings?")
}

@SuppressWarnings('unused')
def installed() {
    log.trace "installed()"
    initialize()
}

def initialize(){
    if (serverAddr != null){
        if(tempPollRate == null){
            device.updateSetting("tempPollRate",[value:300,type:"number"])
            runIn(300,"getPollValues")
        }else if(tempPollRate > 0){
            runIn(tempPollRate,"getPollValues")
        }
    }
}

@SuppressWarnings('unused')
def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
    }
    getPollValues()
}

@SuppressWarnings('unused')
def configure() {
    if(debugEnable) log.debug "configure()"
    getPollValues()
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh() {
    getPollValues()
}

void getPollValues(){
    Map params = [
        uri    : serverAddr,
        headers: [
        	"Connection-Timeout":600
        ]
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

@SuppressWarnings('unused')
void getPTData(resp, data){
    if(debugEnable) log.debug "getPTData"
    //String simString = '{"free_space":"667648","rx_id":"227","tx_id":"50","tx_rssi":"-71","rx_rssi":"-59","firmware_version":"212","hardware_version":"4","id":"483FDA91E94F","ip":"10.54.25.254","subnet":"255.255.255.0","gateway":"10.54.25.1","dns":"unknown","tx_firmware_version":"7","tx_hardware_version":"5","fails":"3","rx_sensors":"[]","tx_sensors":"[{"1":359,"z":58},{"2":6.16},{"3":-15.01}]"}'
    try{
        if (resp.getStatus() == 200 || simString){
            if (debugEnable) log.info resp.data
            if(simString) 
            	dataIn = simString
            else
            	dataIn = resp.data.toString()
            focusData = dataIn.substring(dataIn.indexOf('"tx_sensors":"')+14,dataIn.length()-2)
            focusData = focusData.replace("{","")
            focusData = focusData.replace("}","")
            focusData = focusData.replace("]","")
            focusData = focusData.replace("[","")
            focusData = focusData.replace("\"","")
            focusData = focusData.replace("\\","")
            fdSplit = focusData.split(",")
            //updateAttr("debug1","$focusData")
            //updateAttr("debug",fdSplit)
            fdSplit.each {
                items=it.split(":")
                if(items[0] == 'z')
                	updateAttr("tx1z","${items[1]}")
                else
                    updateAttr("tx${items[0]}","${items[1]}")
            }
            computeValues()
        } else {
            if (!warnSuppress) log.warn "Status ${resp.getStatus()} while fetching IP"
        } 
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}   

@SuppressWarnings('unused')
void computeValues() {
    if(debugEnable) log.debug "Computing Values"
    try{
	    if((obsFull && obsEmpty) || (obsFull && factoryZero)){
    	    if(factoryZero) 
        		zValue = device.currentValue("tx1z", true).toInteger()
        	else 
            	zValue =  obsEmpty
	        Integer fillPct = (((device.currentValue("tx1", true).toInteger() - zValue)/(obsFull - zValue))*100)
    	    updateAttr("fillPct", fillPct, "%")
        	if(tankCap > 0){
            	Integer computedFill = (tankCap * (fillPct / 100)) 
            	updateAttr("compLiquid", computedFill, volumeUnit)
        	}        
    	}	
    	// Unit uses 4 AA batteries with a nominal voltage reported of ~6.0v, 1.5v/battery is considered full and 1.2v is considered "dead"
    	Integer battery = (((device.currentValue("tx2", true).toDouble() - (1.2 * 4)) / (6 - (1.2 * 4)))*100)
   		if(battery > 100) battery = 100
    	if(battery < 0) battery = 0
    	if(debugEnable)
    		log.debug "Battery: $battery"
    	updateAttr("battery", battery, "%")
    } catch (msg) {
    	log.error "Compute Values failed - $msg<br>Other processing continues"
    }
    
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
