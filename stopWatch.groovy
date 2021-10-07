 /*
 * Stopwatch Device
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
 * 
 */
import groovy.transform.Field
@Field rangeMin = 1
@Field rangeMax = 5

static String version()	{  return '0.1.0'  }

metadata {
    definition (
		name: "Stopwatch", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/stopWatch.groovy"
	) {
        	capability "Actuator"

		
		    attribute "start1", "number"
            attribute "stop1", "number"
            attribute "split1", "number" //milliseconds
            attribute "duration1", "number" //milliseconds
		    attribute "start2", "number"
            attribute "stop2", "number"
            attribute "split2", "number" //milliseconds
            attribute "duration2", "number" //milliseconds
		    attribute "start3", "number"
            attribute "stop3", "number"
            attribute "split3", "number" //milliseconds
            attribute "duration3", "number" //milliseconds        
		    attribute "start4", "number"
            attribute "stop4", "number"
            attribute "split4", "number" //milliseconds
            attribute "duration", "number" //milliseconds
		    attribute "start5", "number"
            attribute "stop5", "number"
            attribute "split5", "number" //milliseconds
            attribute "duration5", "number" //milliseconds   
        
            command "startTimer",["number"]
            command "stopTimer", ["number"]
            command "timerSplit",["number"]
        
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

void installed() {
	log.trace "installed()"
    	setVariable("installed")
}

void startTimer(timerNum=1){
    if(timerNum < rangeMin || timerNum > rangeMax) timerNum = 1
    sendEvent(name:"start$timerNum",value:now())
}

void stopTimer(timerNum=1){
    if(timerNum < rangeMin || timerNum > rangeMax) timerNum = 1
    Long timeStop = now()
    sendEvent(name:"stop$timerNum",value:now())
    sendEvent(name:"duration$timerNum", value:timeStop-device.currentValue("start$timerNum"))
}

void timerSplit(timerNum=1){
    if(timerNum < rangeMin || timerNum > rangeMax) timerNum = 1
    sendEvent(name:"split$timerNum", value:now()-device.currentValue("start$timerNum"))
}

void updated(){
    log.trace "Stopwatch ${version()} updated()"
    if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
