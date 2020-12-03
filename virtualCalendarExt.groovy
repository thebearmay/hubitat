 /*
 * Virtual Calendar
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
 *    2020-11-30  thebearmay	 Original version 1.0.0
 *    2020-12-01  thebearmay	 Version 1.1.0 add dayOfYear and holiday logic
 * 
 */

static String version()	{  return '1.1.0'  }

metadata {
    definition (
		name: "Virtual Calendar Extension Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr."
	) {
        capability "Actuator"
		
		attribute "inputDate", "string"
		attribute "calWk", "string"
		attribute "dayOfWeekStr", "string"
		attribute "dayOfWeek", "string"
		attribute "dayOfYear", "string"
        attribute "Date1", "string"
        attribute "Date2", "string"
        attribute "Date3", "string"
        attribute "Date4", "string"
        attribute "Date5", "string"
        attribute "Date6", "string"
        attribute "Date7", "string"
        attribute "Date8", "string"
        attribute "Date9", "string"
        attribute "Date10", "string"
        attribute "Date11", "string"
        attribute "Date12", "string"
		attribute "isHoliday", "bool"
        
		command "configure", []
		command "storeHoliday", ["number", "string"]
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def updateValues() {
   dayMap=[
       1:"Sunday",
       2:"Monday",
       3:"Tuesday",
       4:"Wednesday",
       5:"Thursday",
       6:"Friday",
       7:"Saturday"]
    dateNow = new Date()
    calWk = dateNow.getAt(Calendar.WEEK_OF_YEAR)	
    sendEvent(name: "calWk", value: calWk)
    dayOfWeek = dateNow.getAt(Calendar.DAY_OF_WEEK)
    sendEvent(name: "dayOfWeek", value: dayOfWeek)
    dayOfWeekStr = dayMap[dayOfWeek]
    sendEvent(name: "dayOfWeekStr", value: dayOfWeekStr)
    dayOfYear = dateNow[Calendar.DAY_OF_YEAR]
    sendEvent(name: "dayOfYear", value: dayOfYear)
	
    checkHoliday()
    
    midnight=dateNow+1
    midnight.clearTime()
    secondsToMidnight = Math.round((midnight.getTime() - new Date().getTime())/1000)+1
    
    if(debugEnable) log.debug "values updated...next update in $secondsToMidnight seconds"
    runIn(secondsToMidnight,updateValues)
}

def storeHoliday(inx, holiDateStr) {
    try {
        date1 = Date.parse("yyyy-MM-dd",holiDateStr)
        
        if(inx > 0 && inx < 13)
            sendEvent(name:"Date"+inx, value: (holiDateStr))
        else
            log.warn "index out of bounds (1-12):$inx"
    } catch (Exception e) {
        log.warn "Invalid date string use format yyyy-MM-dd"
    }
    checkHoliday()
}

def checkHoliday(){
	isHoliday = false
    if(debugEnable) log.debug "checkHoliday()"
    for (i=1; i<13; i++) {
        holiday = device.currentValue("Date"+i)
        if(debugEnable) log.debug "checkHoliday $holiday"
	    if (holiday != null){
		    date1 = Date.parse("yyyy-MM-dd",holiday)
            date2 = new Date()
            date2 = date2.clearTime()
            if (date1 == date2){
                isHoliday = true
                break
            }
            if (debugEnable) log.debug "date1: $date1|date2: $date2"
	    }
	}
    sendEvent(name:"isHoliday", value:isHoliday)
}


def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    updateValues()   
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
