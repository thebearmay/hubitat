 /*
 * HTD Groups
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
 *    Date        Who            What
 *    ----        ---            ----
 *    21Mar2022   thebearmay     Original version 0.0.1
*/
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "HTD Group Device", 
        namespace: "htdmca66", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/htd/htdGroups.groovy"
    ) {
        capability "Actuator"
 
        command "groupCreate",[[name:"groupName*",type:"STRING", description:"Group Name"],[name:"groupList*",type:"STRING", description:"List of Zones for Grouping"]]
        command "groupDelete", [[name:"groupName*",type:"STRING", description:"Group Name"]]
        command "groupOn",[[name:"groupName*",type:"STRING", description:"Group Name"]]
        command "groupOff",[[name:"groupName*",type:"STRING", description:"Group Name"]]
        command "groupSetVol",[[name:"groupName*",type:"STRING", description:"Group Name"],[name:"volume*",type:"NUMBER", description:"Volume"]]
        command "groupSetInput",[[name:"groupName*",type:"STRING", description:"Group Name"],[name:"input*",type:"NUMBER", description:"Input Number"]]
        
        command "presetCreate",[[name:"presetNum*",type:"NUMBER", description:"Preset Number", title: "Preset Number"],
                                [name:"inputNum*",type:"NUMBER", description:"Input Number"],
                                [name:"volume*",type:"NUMBER", description:"Volume"],
                                [name:"mute*",type:"ENUM", description:"Mute", constraints:["on", "off"]],
                                [name:"power*",type:"ENUM", description:"Power", constraints:["on","off"]],
                                [name:"balance*",type:"ENUM", description:"Power", constraints:[-18,-17,-16,-15,-14,-13,-12,-11,-10,-9,-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18],defaultValue:0],
                                [name:"treble*",type:"ENUM", description:"Power", constraints:[-10,-9,-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10],defaultValue:0],
                                [name:"bass*",type:"ENUM", description:"Power", constraints:[-10,-9,-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10],defaultValue:0]
                               ]
        command "presetDelete",[[name:"presetNum*",type:"NUMBER", description:"Preset Number", title: "Preset Number"]]
        command "presetExecute",[[name:"presetNum*",type:"NUMBER", description:"Preset Number", title: "Preset Number"],[name:"zOrG*",type:"ENUM", description:"Mute", constraints:["group", "zone"]],[name:"target*",type:"STRING", description:"Zone Number or Group Name", title: "Zone Number or Group Name"]]
        
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
}
}

@SuppressWarnings('unused')
void installed() {
    log.trace "installed()"
    initialize()
}

void initialize(){

}

@SuppressWarnings('unused')
void updated(){
    if(debugEnabled) runIn(1800,"logsOff")			
}

@SuppressWarnings('unused')
void configure() {
    if(debugEnable) log.debug "configure()"
}

@SuppressWarnings('unused')
void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.length() > 1024) {
        log.error "Attribute value for $aKey exceeds 1024, current size = ${aValue.length()}, truncating to 1024..."
        aValue = aValue.substring(0,1023)
    }
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

@SuppressWarnings('unused')
void groupCreate(gName, gList) {
    if(device.properties.data.lync12) maxZone = 12
    else maxZone = 6
    if(debugEnabled) log.debug "Max Zones = $maxZone"
    gSplit = gList.split(",")
    gSplit.each{
        if(it.toInteger() < 1 || (int)it.toInteger() > maxZone) {
            log.error "Invalid Zone value - $it"
            return
        }
    }
    gName = toCamelCase(gName)
    state["$gName"] = gSplit.toString()
}

@SuppressWarnings('unused')
void groupDelete(gName) {
    state.remove(gName)
}

@SuppressWarnings('unused')
void groupOn(gName){
    gSplit = new JsonSlurper().parseText(state["$gName"])
    gSplit.each{
        if(debugEnabled) log.debug "${it}"
        getParent().on(it.toInteger() as byte)
        pauseExecution(50)
    }
}

@SuppressWarnings('unused')
void groupOff(gName){
    gSplit = new JsonSlurper().parseText(state[gName])
    gSplit.each{
        getParent().off(it.toInteger() as byte)
        pauseExecution(50)
    }   
}

@SuppressWarnings('unused')
void groupSetVolume(gName, vol){
    gSplit = new JsonSlurper().parseText(state[gName])
    gSplit.each{
        getParent().lyncSetVolume(it.toInteger(), vol.toInteger)
        pauseExecution(50)
    }
}

@SuppressWarnings('unused')
void groupSetInput(gName, inputNum){
    gSplit = new JsonSlurper().parseText(state[gName])
    gSplit.each{
        getParent().selectInput(it.toInteger() as byte, inputNum.toInteger() as byte)
        pauseExecution(50)
    }    
}

@SuppressWarnings('unused')
void presetCreate(psNum, inputNum, vol, muted, pow,bal,treb,bas){
    if(device.properties.data.lync12 || device.properties.data.lync12) 
        maxInputs = 18
    else 
        maxInputs = 6
    
    if(inputNum > maxInputs || inputNum < 1) {
        log.error "Invalid Input Number: $inputNum"
        return
    }
    
    state["preSet$psNum"]=[input:inputNum,volume:vol,mute:"$muted",power:"$pow",bass:bas,treble:treb,balance:bal]
}

@SuppressWarnings('unused')
void presetDelete(psNum){
    String psName = "preSet$psNum"
    state.remove(psName)
}

@SuppressWarnings('unused')
void presetExecute(psNum, zOrG, target){
    ps= state["preSet$psNum"]
    if(zOrG == 'zone'){
        zone = target.toInteger()
        presetZoneExec(ps, zone)
    } else if(state[target]){
        gSplit = new JsonSlurper().parseText(state[target])
        gSplit.each{
            presetZoneExec(ps, it.toInteger())
            pauseExecution(50)
        }
    }            
}
    
void presetZoneExec(ps, zone){
    if(ps.power == 'on') 
        getParent().on(zone as byte)
    else
        getParent().off(zone as byte)
    pauseExecution(50)
    if(ps.mute == 'on')
        getParent().lyncMuteOn(zone as byte)
    else
        getParent().lyncMuteOff(zone as byte)
    pauseExecution(50)
    getParent().selectInput(zone as byte, ps.input as byte)
    pauseExecution(50)
    getParent().lyncSetVolume(zone as int, ps.volume as int)
    pauseExecution(50)
    getParent().setBalance(zone as int, ps.balance as int)
    pauseExecution(50)
    getParent().setBass(zone as int, ps.balance as int)
    pauseExecution(50)
    getParent().setTreble(zone as int, ps.balance as int)                          
}

@SuppressWarnings('unused')
String toCamelCase(init) {
    if (init == null)
        return null;

    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}


@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
