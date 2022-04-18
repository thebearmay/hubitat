/* Even Day Switch - check the day of the year and turn on a switch based on even/odd
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
 *	Date		Who		        Description
 *	----------	-------------	----------------------------------------------------------------------------
 *	2021-06-07	thebearmay	    Original Code
 *	2021-06-08	thebearmay	    Code Cleanup, add license, etc.
 *  2022-04-18  thebearmay      add week option
 *
*/

@SuppressWarnings('unused')
static String version() {return "0.0.3"}

metadata {
    definition (
        name: "Even Day Switch", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/evenDaySwitch.groovy"
    ) {
        capability "Actuator"
        capability "Initialize"
        capability "Switch"
        
        attribute "evenOdd", "string"
        attribute "dayOfYear", "number"
        attribute "weekOfYear", "number"
    }   
}

preferences {
    input("onWhenEven", "bool", title: "Turn switch on when even day of the year", defaultValue: true)
    input("autoToggleOn", "bool", title: "Reverse the daily even-odd switch behavior \nwhen previous previous interation returns same value", defaultValue:true)
    input("useWeeks", "bool", title: "Use weeks instead of days for switch", defaultValue:false)

}
def installed() {
    initialize()
}

def uninstalled(){
    unschedule()
}

def initialize() {
	if(onWhenEven==null) device.updateSetting("onWhenEven",[value:"true",type:"bool"])
	if(autoToggleOn==null) device.updateSetting("autoToggleOn",[value:"true",type:"bool"])
	schedule("0 0 0 ? * *", dayProcessing)
    dayProcessing()
}

void dayProcessing() {
    if(useWeeks) {
        dateNow = new Date()
	    dayOfYear = dateNow[Calendar.DAY_OF_YEAR]
	    sendEvent(name:"dayOfYear", value: dayOfYear)
        weekProcessing()
    } else
        dailyProcessing()
}  
    
void dailyProcessing(){
    dayPrev = device.currentValue("dayOfYear")?.toInteger() //will be null on device creation
    dateNow = new Date()
	dayOfYear = dateNow[Calendar.DAY_OF_YEAR]
	sendEvent(name:"dayOfYear", value: dayOfYear)
    weekOfYear = dateNow.getAt(Calendar.WEEK_OF_YEAR)
    sendEvent(name:"weekOfYear", value:weekOfYear)
    if(dayOfYear == 1 && dayPrev == 365 && autoToggleOn) {
        if(onWhenEven) device.updateSetting("autoToggleOn",[value:"false",type:"bool"])
        else device.updateSetting("autoToggleOn",[value:"true",type:"bool"])
    }
	if(dayOfYear % 2 == 0) {
	   sendEvent(name:"evenOdd", value:"even")
	   if(onWhenEven) on()
	   else off()
	}	else {
	   sendEvent(name:"evenOdd", value:"odd")
	   if(onWhenEven) off()
	   else on()
	}
}

void weekProcessing(){
    weekPrev = device.currentValue("weekOfYear")?.toInteger() //will be null on device creation 
    dateNow = new Date()
    weekOfYear = dateNow.getAt(Calendar.WEEK_OF_YEAR)
    sendEvent(name:"weekOfYear", value:weekOfYear)
    if(weekOfYear == 1 && weekPrev == 53 && autoToggleOn) {
        if(onWhenEven) device.updateSetting("autoToggleOn",[value:"false",type:"bool"])
        else device.updateSetting("autoToggleOn",[value:"true",type:"bool"])
    }
	if(weekOfYear % 2 == 0) {
	   sendEvent(name:"evenOdd", value:"even")
	   if(onWhenEven) on()
	   else off()
	} else {
	   sendEvent(name:"evenOdd", value:"odd")
	   if(onWhenEven) off()
	   else on()
	}    
}

def updated(){
	initialize()
}

def on(){
	sendEvent(name:"switch", value:"on")
}

def off(){
	sendEvent(name:"switch", value:"off")
}
