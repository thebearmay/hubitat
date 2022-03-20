/**
 *  HTD MCA66 Amplifier Interface
 *
 *  Copyright 2020 Jeff Mehlman
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.

 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   Change Log:
 *     12/21/2020 v1.0 - Initial release
 *     12/22/2020 v1.1 - added mute & input select
 *     03/14/2022 v    - thebearmay: add Lync Codes option
 *
 */
import groovy.transform.Field
//https://raw.githubusercontent.com/thebearmay/hubitat/main/htd/htdAmplifier.groovy
@Field iName =["input0Name","input1Name","input2Name","input3Name","input4Name","input5Name","input6Name","input7Name","input8Name","input9Name","input10Name","input11Name","input12Name"]
metadata {
    definition(name: "HTD MCA66 Amplifier Interface", namespace: "htdmca66", author: "Jeff Mehlman") {
        command "sendTestMessage"

        command "volumeUp", [[name:"zone",type:"NUMBER", description:"Zone #1-6", constraints:["NUMBER"]]]
        command "volumeDown", [[name:"zone",type:"NUMBER", description:"Zone #1-6", constraints:["NUMBER"]]]
        command "createZones"
        command "deleteZones"
        command "lyncSetVolume", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]],[name:"level",type:"NUMBER", description:"Lync ONLY 0-60", constraints:["NUMBER"]]]
        //command "lyncMuteOn", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]]]
        //command "lyncMuteOff", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]]]
        command "simMessage", [[name:"packetString",type:"STRING",description:"Message String"]]
        
        capability "HealthCheck"
        //capability "AudioVolume"

    }

    preferences{
        input name: 'ipAddress', type: 'text', title: 'IP Address', required: true, defaultValue: '10.0.0.53', description: 'IP Address for Gateway'
        input name: 'port', type: 'number', title: 'IP Port', required: true, defaultValue: 10006, description: 'IP Port for Gateway'
        input name: 'lync6Zone', type: 'bool', title: 'Use Lync 6 Zone Codes', defaultValue: false, submitOnChange: true
        input name: 'lync12Zone', type: 'bool', title: 'Use Lync 12 Zone Codes', defaultValue: false, submitOnChange: true
        for(int i=0; i<state.numInputs; i++){
            input name: iName[i+1], type: 'text', title: "input ${i+1} Name", required: true, defaultValue: "input ${i+1}",submitOnChange:true,description: "Name for input ${i+1}"
        }
        input name: 'debugEnabled', type: 'bool', title: 'Enable Debug Messages', defaultValue: false, submitOnChange: true

    }
}

void configure() {}

void installed() {
    // Change to manual invocation due to different number of zones MCA vs. Lync
    //createZones()
    updateStates()
}

void updated() {
    if(debugEnabled) {
        log.debug "Preferences updated()"
        runIn(1800,logsOff)
    }
    updateStates()
}

void updateStates(){
       if(lync6Zone) {
            state.numInputs = 18
            state.numZones = 6
            state.useLyncCodes = true
            device.updateSetting("lync12Zone",[value:"false",type:"bool"])
        } else if (lync12Zone) {
            state.numInputs = 18
            state.numZones = 12
            state.useLyncCodes = true
            device.updateSetting("lync6Zone",[value:"false",type:"bool"])
        } else {
            state.numInputs = 6
            state.numZones = 6
            state.useLyncCodes = false
        }
}


void lyncSetVolume(Integer zone, Integer level){
    if(debugEnabled) log.debug "lSVol $zone $level"
   //Lync uses 1..60  
    level = (level * 0.6).toInteger()
    
    if(!state.useLyncCodes) {
        log.error "Invalid Command for MCA (lSV)"
        return
    }
    if(zone < 1 || zone > state.numZones) {
        log.error "lSV Invalid Zone - $zone"
        return
    }
    if(level < 0 || level > 60) {
        log.error "lSV Invalid Level - $level"
        return
    }

    def lyncVolume = 0xC4 + level
    def msg = [0x02, 0x01, zone, 0x15, lyncVolume] as byte[]
    if(debugEnabled) log.debug "lyncSetVolume $msg"
    sendMessage(msg)    
}

void volumeUp(zone) {
    if(state.useLyncCodes) {
        log.error "Invalid VolUp Command for Lync"
        return
    }
    if (zone<1 || zone> 6)
    {
        log.error "Invalid Zone"
        return
    }
    else
    {

        def cmd = [0x02, 0x00, zone, 0x04, 0x09] as byte[]

        sendMessage(cmd)
    }
}

void volumeDown(zone) {
    if(state.useLyncCodes) {
        log.error "Invalid VolDwn Command for Lync"
        return
    }
    if (zone<1 || zone>6)
    {
        log.error "Invalid Zone"
        return
    }
    else
    {

        def cmd = [0x02, 0x00, zone, 0x04, 0x0A] as byte[]
        sendMessage(cmd)
    }
}

void on(byte zone) {
    
    def msg = [2,0,zone,4,0x20] as byte[]
    if(state.useLyncCodes)
        msg = [2,0,zone,4,0x57] as byte[]
    sendMessage(msg)
}

void off(byte zone) {
    def msg = [2,0,zone,4,0x21] as byte[]
    if(state.useLyncCodes)
        msg = [2,0,zone,4,0x58] as byte[] 
    sendMessage(msg)
}

void sendTestMessage() {
    def msgon = [2,0,5,4,0x20] as byte[]

    sendMessage(msgon)
}

void toggleMute(byte zone) {
    if(state.useLyncCodes) {
        log.error "Invalid Command (TM) for Lync"
        return
    }
    def msg = [0x02,0x00,zone,0x04,0x22] as byte[]

    sendMessage(msg)
}

void lyncMuteOn(byte zone){
    if(!state.useLyncCodes) {
        log.error "Invalid Command (LMOn) for MCA"
        return
    }
    def msg = [0x02,0x00,zone,0x04,0x1E] as byte[]

    sendMessage(msg)    
    
}

void lyncMuteOff(byte zone){
    if(!state.useLyncCodes) {
        log.error "Invalid Command (LMOff) for MCA"
        return
    }
    def msg = [0x02,0x00,zone,0x04,0x1F] as byte[]

    sendMessage(msg)        
}

void selectInput(byte zone, byte inputNum) {
    def inputNumRange = 1..state.numInputs
    if ( inputNumRange.contains(inputNum as int) )
    {
        if(!state.useLyncCodes)
            def msg = [0x02, 0x00, zone, 0x04, inputNum+2] as byte[]
        else if(inputNum < 13)
            msg = [0x02, 0x00, zone, 0x04, inputNum+15] as byte[]
        else
            msg = [0x02, 0x00, zone, 0x04, inputNum+86] as byte[]
        if(debugEnabled) log.debug "SelInput $zone $inputNum [$msg]"
        sendMessage(msg)
    }
    else {
        log.error "Invalid input number: ${inputNum}"
    }
}


void createZones() {
//    updateStates()
    for (i in 1..state.numZones)
    {
       cd = addChildDevice("htdmca66", "HTD MCA66 Amplifier Zone", "${device.deviceNetworkId}-ep${i}", [name: "${device.displayName} (Zone${i})", isComponent: true, lync:"$state.useLyncCodes"])
       cd.setZone(i)
    }
}

void deleteZones() {
    zones = getChildDevices()
    if(debugEnabled)log.debug "${zones}"
    for (i in 1..state.numZones) {
        deleteChildDevice("${device.deviceNetworkId}-ep${i}")
    }
}

void setBass(bass, zone){
    if(bass < -10 || bass > 10) bass = 0
    msg = [0x02, 0x00, zone, 0x18, bass] as byte[]
    sendMessage(msg)
}

void setTreble(treble, zone){
    if(treble< -10 || treble > 10) treble = 0    
    msg = [0x02, 0x00, zone, 0x17, treble] as byte[]
    sendMessage(msg)
}

void setBalance(balance, zone){
    if(balance < -18 || balance > 18) balance = 0 
    msg = [0x02, 0x00, zone, 0x16, balance] as byte[]
    sendMessage(msg)    
}

/*******************
**   Message API  **
********************/
void sendMessage(byte[] byte_message) {
    def ip = ipAddress as String
    def p = port as int

    // calculate checksum
    def cksum = [0] as byte[]
    for (byte i : byte_message)
    {
        cksum[0] += i
    }
    if(debugEnabled) log.debug "Cksum computed as: ${cksum}"

    def msg_cksum = [byte_message, cksum].flatten() as byte[]

    def strmsg = hubitat.helper.HexUtils.byteArrayToHexString(msg_cksum)

    if(debugEnabled) log.debug "Sending Message: ${strmsg} to ${ipAddress}:${port}"

    interfaces.rawSocket.connect(ip, p, 'byteInterface':true)
    interfaces.rawSocket.sendMessage(strmsg)

    //interfaces.rawSocket.close()
}

void simMessage(input) {
    iToMsg= input.split(",")

    if(debugEnabled)log.debug "$iToMsg"
    int i=0
    int[] preMsg = new int[iToMsg.size()]
    iToMsg.each{
        preMsg[i]=it.toInteger()
        i++
    }
    if(debugEnabled)log.debug "$preMsg"
    char[] msg = new char[iToMsg.size()]
    i=0
    preMsg.each{
        msg[i]=(char)it
        i++
    }    
    
    receiveMessage(msg as byte[])
}

void receiveMessage(byte[] byte_message)
{
    def PACKET_SIZE = 14 // all packets should be 14 bytes

    if(debugEnabled) log.debug "Received Message: ${byte_message}, length ${byte_message.length}"
    if( byte_message.length % PACKET_SIZE != 0 ) return //non-standard
    // iterate over packets
    def header = [2, 0] as byte[]
    for (int i = 0; i < byte_message.length; i+=PACKET_SIZE)
    {
        if(debugEnabled) log.debug "Decoding Packet #${i/PACKET_SIZE}"
        packet = byte_message[i..i+PACKET_SIZE-1]

        if(debugEnabled)log.debug "$packet"
        if (packet[0..1] != header) {
            if(debugEnabled) log.debug "parse Invalid packet value - $packet"
            continue
        }

        zone = packet[2]

        // Only interested in Command 0x05 
        if (packet[3] != 0x05) {
            if(debugEnabled) log.debug "Non-5 Packet Skipped - $packet"
            continue
        }
        
        def d1 = packet[4] as byte
        def muteIs = 'muted'
        def poweris = 'off'
        if (!state.useLyncCodes){
            boolean powerOn = (d1 >> 7 & 0x01)      
            if(powerOn) poweris = 'on'
            boolean mute = d1 >> 6 & 0x01
            if(!mute) muteIs = 'unmuted'
        } else {
            powerOn = (d1 & 0x01)            
            if(powerOn) poweris = 'on'
            mute = d1 >> 1 & 0x01
            if(!mute) muteIs = 'unmuted'           
        }

        def input = packet[8]+1
        def volume = packet[9]+60 as int
        def treble = packet[10]
        def bass = packet[11]
        def balance = packet[12]
        

        def volumePercentage = volume*100/60

        if(debugEnabled)
            log.debug "$packet<br>Zone: ${zone}, power: ${powerOn}, mute = ${mute}, input = ${input}, volume = ${volume}"

        // Put in state map for update
        def zoneStates = ['switch' : poweris, 'mute' : muteIs, 'volume' : volumePercentage.toInteger(), 'inputNumber' : input, 'bass' : bass, 'treble': treble, 'balance': balance]
        if(debugEnabled) log.debug "${device.deviceNetworkId}-ep${zone}<br>$zoneStates"
 ///       if(packet[3] == 0x05 && zone in 1..state.numZones)
            getChildDevice("${device.deviceNetworkId}-ep${zone}").updateState(zoneStates)

    }
}

// Asynchronous receive function
void parse(String msg) {
    receiveMessage(hubitat.helper.HexUtils.hexStringToByteArray(msg))
}

@SuppressWarnings('unused')
void logsOff(){
    device.updateSetting("debugEnabled",[value:"false",type:"bool"])
    log.info "Debug logging turned off"
}
