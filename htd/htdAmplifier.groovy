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
metadata {
    definition(name: "HTD MCA66 Amplifier Interface", namespace: "htdmca66", author: "Jeff Mehlman") {
        command "sendTestMessage"

        command "volumeUp", [[name:"zone",type:"NUMBER", description:"Zone #1-6", constraints:["NUMBER"]]]
        command "volumeDown", [[name:"zone",type:"NUMBER", description:"Zone #1-6", constraints:["NUMBER"]]]
        command "createZones"
        command "deleteZones"
        command "setLyncVolume", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]],[name:"level",type:"NUMBER", description:"Lync ONLY 0-60", constraints:["NUMBER"]]]
        //command "lyncMuteOn", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]]]
        //command "lyncMuteOff", [[name:"zone",type:"NUMBER", description:"Lync ONLY Zone #1-12", constraints:["NUMBER"]]]
        
        capability "HealthCheck"
        //capability "AudioVolume"

    }

    preferences{
        input name: 'ipAddress', type: 'text', title: 'IP Address', required: true, defaultValue: '10.0.0.53', description: 'IP Address for Gateway'
        input name: 'port', type: 'number', title: 'IP Port', required: true, defaultValue: 10006, description: 'IP Port for Gateway'
        input name: 'lync6Zone', type: 'bool', title: 'Use Lync 6 Zone Codes', defaultValue: false, submitOnChange: true
        input name: 'lync12Zone', type: 'bool', title: 'Use Lync 12 Zone Codes', defaultValue: false, submitOnChange: true
        for(int i=0; i<state.numInputs; i++){
            input name: "input${i+1}Name", type: 'text', title: "input ${i+1} Name", required: true, defaultValue: "input ${i+1}", description: "Name for input ${i+1}"
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
        log.error "Invalid Command for MCA"
        return
    }
    if(zone < 1 || zone > state.numZones) {
        log.error "Invalid Zone - $zone"
        return
    }
    if(level < 0 || level > 60) {
        log.error "Invalid Level - $level"
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
        msg = [2,0,zone,4,0x55] as byte[]
    sendMessage(msg)
}

void off(byte zone) {
    def msg = [2,0,zone,4,0x21] as byte[]
    if(state.useLyncCodes)
        msg = [2,0,zone,4,0x56] as byte[] 
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
    def inputNumRange = 1..state.numZones
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

void receiveMessage(byte[] byte_message)
{
    def PACKET_SIZE = 14 // all packets should be 14 bytes

    if(debugEnabled) log.debug "Received Message: ${byte_message}, length ${byte_message.length}"

    // iterate over packets
    for (int i = 0; i < byte_message.length; i+=PACKET_SIZE)
    {
        if(debugEnabled) log.debug "Decoding Packet #${i/PACKET_SIZE}"
        def header = [2, 0] as byte[]
        if (byte_message[i..i+1] != header) {
            if(debugEnabled) log.debug "parse Invalid packet value"
            continue
        }

        zone = byte_message[i+2]

        // Command should be 0x05 (Lync first packet is 0x06)
        if (byte_message[3] != 0x05) {
            //if(debugEnabled) log.debug "Unknown packet type - ${byte_message[3]}"
            continue
        }

        def d1 = byte_message[4] as byte
        boolean powerOn = (d1 >> 7 & 0x01)
        def poweris = 'off'
        if(powerOn) poweris = 'on'

        boolean mute = d1 >> 6 & 0x01
        def muteIs = 'muted'

        if(!mute) muteIs = 'unmuted'

        def input = byte_message[8]+1
        def volume = byte_message[9]+60 as int

        def volumePercentage = volume*100/60

        if(debugEnabled) log.debug "Zone: ${zone}, power: ${powerOn}, mute = ${mute}, input = ${input}, volume = ${volume}"

        // Put in state map for update
        def zoneStates = ['switch' : poweris, 'mute' : muteIs, 'volume' : volumePercentage, 'inputNumber' : input]
        if(debugEnabled) log.debug "${device.deviceNetworkId}-ep${zone}<br>$zoneStates"
        if(byte_message[3] == 0x05)
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
