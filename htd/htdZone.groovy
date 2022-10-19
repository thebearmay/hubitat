/**
 *  HTD MCA66 Amplifier Zone
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
 *     03/14/2022 v    - thebearmay add Lync options
 *
 */

metadata {
    definition(name: "HTD MCA66 Amplifier Zone", namespace: "htdmca66", author: "Jeff Mehlman") {
        command "sendTestMessage"
        command "selectInput", [[name:"inputNum",type:"NUMBER", description:"Input Number", constraints:["NUMBER"]]]

        capability "AudioVolume"
        capability "HealthCheck"
        capability "Switch"

        attribute "ZoneNumber", "number"
        attribute "ZoneName", "string"
        attribute "bass", "number"
        attribute "treble", "number"
        attribute "balance", "number"
        attribute "volume", "number"
        attribute "mute","string"
    }

    preferences{
        input name: 'zoneName', type: 'text', title: 'Zone Name', required: true, defaultValue: 'Zone X', description: 'Name for Zone'
        input name: 'debugEnabled', type: 'bool', title: 'Enable debug logging'
    }
}

void configure() {}

void installed() {
    runIn(1, "off")
    sendEvent(name:'volume',value:0)
}

void updated() {
    if(debugEnabled) {
        log.debug "${zoneName}"
        runIn(1800,'logsOff')
    }
    sendEvent(name: 'ZoneName',value: zoneName)
}

void setZone(zone) {
    state.ZoneNumber = zone
}

int getZone() {
    return state.ZoneNumber
}

void volumeUp() {
    def zone = state.ZoneNumber as int
    if(device.properties.data.lync){
        if(device.currentValue('volume') != null)
            def currentVolume = device.currentValue('volume') as int
        else 
            currentVolume = 0
        getParent().lyncSetVolume((Integer)zone, (Integer)currentVolume+5)
        if(debugEnabled) "VolUp - lyncSetVolume($zone, ${currentVolume-5})"
    } else
        getParent().volumeUp(zone)
}

void volumeDown() {
    def zone = state.ZoneNumber as int
    if(device.properties.data.lync){
        if(device.currentValue('volume')!= null)
            def currentVolume = device.currentValue('volume') as int
        else 
            currentVolume = 5
        getParent().lyncSetVolume((Integer)zone, (Integer)currentVolume-5)
        if(debugEnabled) "VolDwn - lyncSetVolume($zone, ${currentVolume-5})"
    } else
        getParent().volumeDown(zone)
}


void setVolume(volume) {
    if (device.currentValue('switch') == 'off') {
        if(debugEnabled) log.debug "Device off, no volume control"
        return
    }

    def zone = state.ZoneNumber as byte
    if(device.currentValue('volume')!= null)
        def currentVolume = device.currentValue('volume')*60/100 as int
    else currentVolume = 0

    def desiredVolume = volume*60/100 as int

    if(debugEnabled) log.debug "Input Volume: ${volume}, Desired Volume: ${desiredVolume}, Current Volume: ${currentVolume}"

    state.updatingVolume = true
    
    if(device.properties.data.lync) {
        getParent().lyncSetVolume((Integer)zone, (Integer)volume)
        state.updatingVolume = false
        return
    }

    if (currentVolume < desiredVolume)
    {
        def diff = desiredVolume - currentVolume
        for (i in 1..diff) {
            volumeUp()
        }
        state.updatingVolume = false

    }
    else if (currentVolume > desiredVolume )
    {
        def diff = currentVolume - desiredVolume
        for (i in 1..diff) {
            volumeDown()
        }
        state.updatingVolume = false
    }
    else
    {
        state.updatingVolume = false
        return
    }
}

void setLevel(volume) {
    setVolume(volume)
}

void on() {
    def zone = state.ZoneNumber as byte

    getParent().on(zone)
    //sendEvent(name: "switch", value: "on")
}

void off() {
    def zone = state.ZoneNumber as byte

    getParent().off(zone)
    //sendEvent(name: "switch", value: "off")
}

void mute() {
    def zone = state.ZoneNumber as byte
    if(device.properties.data.lync){
        getParent().lyncMuteOn(zone)
        return
    }
    if (state.mute == 'unmuted') {
        getParent().toggleMute(zone)
    }
}

void unmute() {
    def zone = state.ZoneNumber as byte
    if(device.properties.data.lync){
        getParent().lyncMuteOff(zone)
        return
    }
    if (state.mute == 'muted') {
        getParent().toggleMute(zone)
    }
}

void selectInput(inputNum) {
    if(debugEnabled) log.debug "${state.ZoneNumber} requesting Input $inputNum"
    def zone = state.ZoneNumber as byte

    getParent().selectInput(zone, inputNum as byte)
}

void updateState(statesMap) {
    if(debugEnabled) log.debug "Request from parent to update states $statesMap"
    if (state.updatingVolume == null)
    {
        state.updatingVolume = false
    }
    if(debugEnabled) log.debug "updatingVolume = ${state.updatingVolume} lync = ${device.properties.data.lync}"
    if (state.updatingVolume == false || device.properties.data.lync) {
        statesMap.each{entry -> sendEvent(name: entry.key, value: entry.value)
        state."${entry.key}" = entry.value
        }
    }
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
