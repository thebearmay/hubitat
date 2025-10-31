/*
 * Shade Minder
 * Simple App to control outdoor shades based on illumance and wind speed
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
 *    27Oct2025        thebearmay            v0.0.1 - Original code
 *    28Oct2025        thebearmay            v0.0.2 - Add the Data Management Screen, Reverse Shade option, Daily Avg Scheduling option
 *    29Oct2025        thebearmay            v0.0.3 - Fix edge case in time averaging
 *    30Oct2025        thebearmay            v0.0.4 - Add the initialization values, and Tool Tips
*/
    


static String version()	{  return '0.0.4'  }
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
//include thebearmay.uiInputElements


definition (
	name: 			"Shade Minder",
    namespace: 		"thebearmay",
	author: 		"Jean P. May, Jr.",
	description: 	"Simple App to control outdoor shades based on illumance and wind speed",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/shadeMinder.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "decision"
    page name: "configPage"
    page name: "dataFileMgmt"

}
mappings {
/*    path("/refresh") {
        action: [POST: "refresh",
                 GET: "refresh"]
    }
*/
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
	if(!state.avgWind)
    	state.avgWind = 0
    if(!maxWind)
    	app.updateSetting("maxWind",[type:"number",value:75])
    if(!state.avgLight)
    	state.avgLight = 0
    if(!minLight)
    	app.updateSetting("minLight",[type:"number",value:1000])
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def decision(){
    dynamicPage (name: "decision", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:'cPageHndl', title:''){
            if(!dmFirst) 
            	configPage()
            else
                dataFileMgmt()
        }
    }
}

def configPage(){
    dynamicPage (name: "configPage", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:'cPageHndl', title:'Configuration Page'){
            String db = getInputElemStr(name:'debugEnabled', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Debug Enabled</b>', defaultValue: "${settings['debugEnabled']}")
            String rev = getInputElemStr(name:'reverseDir', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Reverse Shade Direction</b>', defaultValue: "${settings['reverseDir']}", hoverText:"Reverse the Open/Close commands issued")
            String uDf = getInputElemStr(name:'useAvg', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Use Avg Settings from File</b>', defaultValue: "${settings['useDataFile']}", hoverText:"Turn on to schedule using average daily values from the data file")
			String wind = getInputElemStr(name:'windDev', type:'capability.*', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Device for Wind Speed</b>', defaultValue: "${settings['windDev']}")
            //windSpeed
            String sunLight = getInputElemStr(name:'luxDev', type:'capability.illuminanceMeasurement', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Device for Light Measurement</b>', defaultValue: "${settings['luxDev']}")
            //illuminance
            String shades = getInputElemStr(name:'shadeDev', type:'capability.windowShade', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Shade Devices</b>', defaultValue: "${settings['shadeDev']}, multiple:true")
			String begTime = getInputElemStr(name:'sTime', type:'time', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Opening Time Override</b>', defaultValue: "${settings['sTime']}", hoverText:"This value will override the illumination setting")
            String endTime = getInputElemStr(name:'eTime', type:'time', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Closing Time Override</b>', defaultValue: "${settings['eTime']}", hoverText:"This value will override the illumination setting")
            String mWind = getInputElemStr(name:'maxWind', type:'number', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Maximum Wind Speed</b>', defaultValue: "${settings['maxWind']}")
            String mLight = getInputElemStr(name:'minLight', type:'number', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Minimum Lux to Close</b>', defaultValue: "${settings['minLight']}")
			String aRename = getInputElemStr(name:"nameOverride", type:"text", title: "<b>New Name for Application</b>", multiple: false, defaultValue: app.getLabel(), width:'15em', radius:'12px', background:'#e6ffff', hoverText:"Change this if you need multiple instances of the app")
            String cTable = "${ttStyleStr}<table><tr><td>${wind}</td><td>${sunLight}</td><td>${shades}</td></tr>"
            cTable += "<tr><td>${mLight}</td><td>${mWind}</td></tr>"            
            cTable += "<tr><td>${endTime}</td><td>${begTime}</td><td></td></tr></table>"

            ci = btnIcon(name:'event', size:'14px')
            paragraph getInputElemStr(name:"dMgmt", type:'href', title:"${ci} Data Management", destPage:'dataFileMgmt', width:'11em', radius:'15px', background:'#669999', hoverText:"Go to the maintenance page for the data file")
            
            paragraph cTable
            paragraph "<table><tr><td>${db}</td><td>${rev}</td></tr><tr><td>${aRename}</td><td>${uDf}</td></tr></table>"
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            if(luxDev) 
            	subscribe(luxDev, "illuminance", evtLux)
            else 
                unsubscribe(evtLux)
            if(windDev){
            	subscribe(windDev, "windSpeed", evtWind)
                if(windDev.hasAttribute('windGust'))
                	subscribe (windDev,'windGust', evtWind)
            } else
                unsubscribe(evtWind)
            subscribe(location,"sunrise", evtTime)
            subscribe(location,"sunset", evtTime)
			if(useAvg){
            	setByAvg()
                schedule("0 17 4 * * ? *", "setByAvg")
				paragraph "<span style='color:#FF000A;font-weight:bold'>Schedule is being run using the Daily Average File</span>"
            } else {
                unschedule("setByAvg")
            }
            checkSched()

        }
    }
}



def dataFileMgmt(){
    dynamicPage (name: "dataFileMgmt", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:'dFileHndl', title:'Data Management'){
            SimpleDateFormat sdfJulD = new SimpleDateFormat("DDD")
            String dmF = getInputElemStr(name:'dmFirst', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Make this the first page</b>', defaultValue: "${settings['dmFirst']}")            
            String cfg = getInputElemStr(name:"cfgPg", type:'href', title:"<i class='material-icons app-column-info-icon' style='font-size: 24px;'>settings_applications</i> Configuration Page", destPage:'configPage', width:'11em', radius:'15px', background:'#669999')
            paragraph "<table><tr><td>${cfg}</td><td>${dmF}</td></tr></table>"
            String fBuffer=''
            try {
            	fBuffer = new String(downloadHubFile("${app.id}.txt"),"UTF-8")
            } catch (e) {
                fBuffer = '000:1100:1300\n'
                uploadHubFile("${app.id}.txt",fBuffer.getBytes('UTF-8'))
            }
            String begTime = getInputElemStr(name:'s2Time', type:'time', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Opening Time</b>', defaultValue: "${settings['s2Time']}", hoverText:"Required, can be before or after Closing Time")
            String endTime = getInputElemStr(name:'e2Time', type:'time', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Closing Time</b>', defaultValue: "${settings['e2Time']}", hoverText:"Required, can be before or after Opening Time")
            String stoDate = getInputElemStr(name:'entryDate', type:'date', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Day to Log</b>', defaultValue: "${settings['entryDate']}", hoverText:"Required, only the Julian Day value is stored; multiple entries will be averaged")
            String storeD = getInputElemStr(name:'storeData', type:'button', width:'5em', radius:'12px', background:'#669999', title:'Add to File')
            String pData = "${ttStyleStr}<table><tr><td>${stoDate}</td><td>${begTime}</td><td>${endTime}</td></tr>"
            pData += "<tr><td>${storeD}</td><td></td><td></td></table>"
            paragraph pData
            if(state.updFile) {
                if(entryDate  && s2Time && e2Time){
	                state.updFile = false
    	            eDate = new Date(Integer.parseInt(entryDate.substring(0,4))-1900,Integer.parseInt(entryDate.substring(5,7))-1,Integer.parseInt(entryDate.substring(8,10)))            
        	        p1 = "${sdfJulD.format(eDate)}:"
            	    tWork = s2Time.toString()
                	tW2 = tWork.substring(tWork.indexOf("T")+1,tWork.indexOf("T")+6)
	                tW3 = tW2.split(":")
    	            p2 = "${tW3[0]}${tW3[1]}:"
					tWork = e2Time.toString()
            	    tW2 = tWork.substring(tWork.indexOf("T")+1,tWork.indexOf("T")+6)
	                tW3 = tW2.split(":")
    	            p3 = "${tW3[0]}${tW3[1]}\n"
        	        newEntry = "${p1}${p2}${p3}"
            	    if(debugEnabled) 
                		log.debug newEntry
	                fBuffer += newEntry             
    	            uploadHubFile("${app.id}.txt",fSort(fBuffer).getBytes('UTF-8'))
        	        app.removeSetting("entryDate")
					paragraph "<script type='text/javascript'>location.reload()</script>"
                }
            }
            paragraph getInputElemStr(name:'useAvg', type:'bool', width:'16em', radius:'12px', background:'#e6ffff', title:'<b>Schedule using daily average</b>', defaultValue: "${settings['useAvg']}",hoverText:"Turn on to schedule using average daily values from the data file")            
            if(useAvg){
            	setByAvg()
                schedule("0 17 4 * * ? *", "setByAvg")
            } else {
                unschedule("setByAvg")
                unschedule("forceClose")
                unschedule("forceOpen")
            }
        }
    }
}

def evtLux(evt){
    holdLevel = state?.preLightLevel
    holdTime = state?.preLightUpdate
	if(debugEnabled) 
    	log.debug "lux 1 - $holdLevel ${state.preLightLevel} ${state.lightLevel}"
    if(state.lightLevel)
    	state.preLightLevel = state.lightLevel
    else
        state.preLightLevel = Integer.parseInt(evt.value)
    state.preLightUpdate = state?.lightUpdateTime
    state.lightLevel = Integer.parseInt(evt.value)
    state.lightUpdateTime = new Date().getTime()
    if(!holdLevel || holdLevel < 0) 
    	holdLevel = Integer.parseInt(evt.value)

    if(!holdTime || holdTime < 0)
    	holdTime = new Long(0)

    if(debugEnabled) 
    	log.debug "lux 2 - $holdLevel ${state.preLightLevel} ${state.lightLevel}"
    if(state.lightUpdateTime - holdTime >= (10*60*1000)){// Only calculate a new average and check whether to open/close every 10 minutes
        state.avgLight = (holdLevel + state.preLightLevel + state.lightLevel)/3
        openCheck()
    }
}

def evtWind(evt){
    holdLevel = state?.preWindSpeed
    holdTime = state?.preWindUpdate 
    if(state.windSpeed) 
    	state.preWindSpeed = state?.windSpeed
    else
        state.preWindSpeed = Float.parseFloat(evt.value)
    state.preWindUpdate = state?.windUpdateTime
    state.windSpeed = Float.parseFloat(evt.value)
    state.windUpdateTime = new Date().getTime()
    
	if(!holdLevel || holdLevel < 0) 
    	holdLevel = Float.parseFloat(evt.value)
    
    if(!holdTime || holdTime < 0)
    	holdTime = new Long(0)
    
    if(Float.parseFloat(evt.value) > (1.2*maxWind)) { // open the shade if wind gust of 120% of max
       forceOpen()
    } else if(state.windUpdateTime - holdTime >= (10*60*1000)){
        state.avgWind = (holdLevel + state.preWindSpeed + state.windSpeed)/3
        openCheck()
    }
   
}

def evtTime(evt) {  //sunrise sunset check
    openCheck()
}

void forceOpen(){
    if (avgWind && maxWind && avgWind >= maxWind) // don't time force if recorded wind is too high
    	return 
	shadeDev.each {
        if(!reverseDir){
			it.open()
			if(debugEnabled) 
				log.debug "Oopening $it ${location.sunrise} $tNow ${location.sunset} ${state.avgWind} $maxWind ${state.avgLight} $minLight"
        } else {	
			it.close()
			if(debugEnabled) 
				log.debug "Closing $it ${location.sunrise} $tNow ${location.sunset} ${state.avgWind} $maxWind ${state.avgLight} $minLight"
        }
	} 
    if(!reverseDir){
		state.lastCommand = 'open'
    } else {
        state.lastCommand = 'close'
    }
}

void forceClose(){
	shadeDev.each {
        if(!reverseDir){
	        it.close()
			if(debugEnabled) 
				log.debug "Closing $it ${location.sunrise} $tNow ${location.sunset} ${state.avgWind} $maxWind ${state.avgLight} $minLight"
        } else {	
			it.open()
			if(debugEnabled) 
				log.debug "Opening $it ${location.sunrise} $tNow ${location.sunset} ${state.avgWind} $maxWind ${state.avgLight} $minLight"
        }            
	}
    if(!reverseDir){
		state.lastCommand = 'close'
    } else {
        state.lastCommand = 'open'
    }
}

void openCheck(){
    if(eTime || sTime) // open and close are being overridden
    	return
    Date tNow = new Date()

    if(tNow > location.sunrise && tNow < location.sunset && state.avgWind < maxWind && state.avgLight >= minLight ) {
		forceOpen()
    } else {
		forceClose()
    }
}

String fSort(fBuff){
    String sortedBuff = ''
    fList = fBuff.split("\n")
    fList = fList.sort()
    fList.each{
        sortedBuff += "$it\n"
    }
    return sortedBuff
}

List dailyAvg(){
	String fBuffer=''
	try {
		fBuffer = new String(downloadHubFile("${app.id}.txt"),"UTF-8")
	} catch (e) {
		fBuffer = '000:1100:1300\n'
        uploadHubFile("${app.id}.txt",fBuffer.getBytes('UTF-8'))
	} 
    fList = fBuffer.split("\n")
    List aList = []
   	String dayHold = '000'
    int i=0
    int sHold=0
    int eHold=0
    List dRec = []
    fList.each{
        dRec = it.split(':')
        if (dRec[0] == dayHold){
            sHold += Integer.parseInt(dRec[1])
            eHold += Integer.parseInt(dRec[2])
            i++
		} else {
            if(debugEnabled)
            	log.debug "$dayHold $sHold $eHold<br>$aList"
            sHoldS = (sHold/i).intValue().toString()
            eHoldS = (eHold/i).intValue().toString()
            if(debugEnabled)
            	log.debug "$dayHold $sHoldS $eHoldS<br>$aList"
            while (sHoldS.size() < 4){
                sHoldS = "0$sHoldS"
            }
			while (eHoldS.size() < 4){
                eHoldS = "0$eHoldS"
            }
            aList.add("${dayHold}:${sHoldS}:${eHoldS}")
            i=1
            sHold=Integer.parseInt(dRec[1])
            eHold=Integer.parseInt(dRec[2])
            dayHold = dRec[0]
        } 
    }            
    sHoldS = (sHold/i).intValue().toString()
	eHoldS = (eHold/i).intValue().toString()
	while (sHoldS.size() < 4){
		sHoldS = "0$sHoldS"
	}
	while (eHoldS.size() < 4){
		eHoldS = "0$eHoldS"
	}
    aList.add("${dayHold}:${sHoldS}:${eHoldS}")
    return aList
}

void setByAvg() {
    SimpleDateFormat sdfJulD = new SimpleDateFormat("DDD")
    List aList = dailyAvg()
    String targetInx = sdfJulD.format(new Date())
    String saTime
    String eaTime
    aList.each {
        item = it.split(':')
        if(item[0] <= targetInx){
            saTime = item[1]
            eaTime = item[2]
        }
    }
    if(debugEnabled)
    	log.debug "$aList<br>$saTime $eaTime<br>$targetInx"
    app.updateSetting('sTime',[type:"time",value:"${saTime.substring(0,2)}:${saTime.substring(2,4)}"])
    app.updateSetting('eTime',[type:"time",value:"${eaTime.substring(0,2)}:${eaTime.substring(2,4)}"])
	schedule("0 ${Integer.parseInt(saTime.substring(2,4))} ${Integer.parseInt(saTime.substring(0,2))} * * ? *", "forceClose")
	schedule("0 ${Integer.parseInt(eaTime.substring(2,4))} ${Integer.parseInt(eaTime.substring(0,2))} * * ? *", "forceOpen")

}

void checkSched(){
	if(sTime) {
		tWork = sTime.toString()
		tW2 = tWork.substring(tWork.indexOf("T")+1,tWork.indexOf("T")+6)
		tW3 = tW2.split(":")
		schedule("0 ${Integer.parseInt(tW3[1])} ${Integer.parseInt(tW3[0])} * * ? *", "forceClose")
	} else
		unschedule("forceClose")
	if(eTime) {
		tWork = eTime.toString()
		tW2 = tWork.substring(tWork.indexOf("T")+1,tWork.indexOf("T")+6)
		tW3 = tW2.split(":")
		schedule("0 ${Integer.parseInt(tW3[1])} ${Integer.parseInt(tW3[0])} * * ? *", "forceOpen")
	} else
		unschedule("forceOpen")
}


def appButtonHandler(btn) {
    switch(btn) {
        case "storeData":
        	state.updFile = true
        	break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}


/*
*
* Set of methods for UI elements
*
*
*  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*	Date			Who					Description
*	----------		--------------		-------------------------------------------------------------------------
*	11Mar2025		thebearmay			Add checkbox uiType, add trackColor and switchColor for type = bool
*	13Mar2025							Added hoverText, code cleanup
*	15Mar2025							Expand btnIcon to handle he- and fa- icons
*	18Mar2025							Add btnDivHide to hide/display div's (uiType='divHide')
* 	03Apr2025							Enable a default value for enums
*	04Apr2025							Size option for icons
*/

import groovy.transform.Field
import java.text.SimpleDateFormat
library (
    base: "app",
    author: "Jean P. May Jr.",
    category: "UI",
    description: "Set of methods that allow the customization of the INPUT UI Elements",
    name: "uiInputElements",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/uiInputElements.groovy",
    version: "0.0.7",
    documentationLink: ""
)

/************************************************************************
* Note: If using hoverText, you must add $ttStyleStr to at least one 	*
*			element display												*
************************************************************************/

String getInputElemStr(HashMap opt){
   switch (opt.type){
	case "text":
	   return inputItem(opt)
	   break
	case "number":
	   return inputItem(opt)
	   break
	case "decimal":
	   return inputItem(opt)
	   break
	case "date":
	   return inputItem(opt)
	   break
	case "time":
	   return inputItem(opt)
	   break	
	case "password":
	   return inputItem(opt)
	   break
	case "color":
	   return inputItem(opt)
	   break
   	case "enum":
	   return inputEnum(opt)
	   break
	case "mode":
	   return inputEnum(opt)
	   break
	case "bool":
	   return inputBool(opt)
	   break
	case "checkbox":
	   return inputCheckbox(opt)
	   break       
    case "button":
	   return buttonLink(opt)
	   break
	case "icon":
	   return btnIcon(opt)
	   break
	case "href":
	   return buttonHref(opt)
	   break
	case "divHide":
	   return btnDivHide(opt)
	   break
default:
       if(opt.type && (opt.type.contains('capability') || opt.type.contains('device')))
	       return inputCap(opt)
       else 
	       return "Type ${opt.type} is not supported"
	   break
   }
}

String appLocation() { return "http://${location.hub.localIP}/installedapp/configure/${app.id}/" }

/*****************************************************************************
* Returns a string that will create an input element for an app - limited to *
* text, password, number, date and time inputs currently                     *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) input type					     					 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/
String inputItem(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    if(settings[opt.name] != null){
        if(opt.type != 'time') {
        	opt.defaultValue = settings[opt.name]
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat('HH:mm')
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            opt.defaultValue = sdf.format(sdfIn.parse(settings[opt.name]))
        }
    }
    typeAlt = opt.type
    if(opt.type == 'number') {
    	step = ' step=\"1\" '
    } else if (opt.type == 'decimal') {
        step = ' step=\"any\" '
        typeAlt = 'number'
    } else {
        step = ' '
    }
        
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    
    if(opt.hoverText && opt.hoverText != 'null'){  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    }
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
	retVal+="<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield has-placeholder is-dirty is-upgraded' style='' data-upgraded=',MaterialTextfield'>"
    retVal+="<label for='settings[${opt.name}]' style='min-width:${opt.width}' class='control-label'>${opt.title}</label><div class='flex'><input type='${typeAlt}' ${step} name='settings[${opt.name}]' class='mdl-textfield__input submitOnChange' style='${computedStyle}' value='${opt.defaultValue}' placeholder='Click to set' id='settings[${opt.name}]'>"
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\"><div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div></div></div></div>"
    return retVal
}
	
/*****************************************************************************
* Returns a string that will create an input capability element for an app   *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     	 						 *
*	type - (required) capability type, 'capability.<capability or *>'    	 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color         *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputCap(HashMap opt) {
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize}"
    if(opt.radius) 
    	computedStyle += "border-radius:${opt.radius};"
    else 
    	opt.radius = '1px'
    if(!opt.multiple) opt.multiple = false
    String dList = ''
    String idList = ''
    int i=0
    if(settings["${opt.name}"]){
        ArrayList devNameId = []
        settings["${opt.name}"].each{
            devNameId.add([name:"${it.displayName}", devId:it.deviceId])
        }
        ArrayList devNameIdSorted = devNameId.sort(){it.name}
        devNameIdSorted.each{
			if(i>0) { 
                dList +='<br>'
                idList += ','
            }
            dList+="${it.name}"
            idList+="${it.devId}"
	        i++
	    }
    } else {
    	dList = 'Click to set'
    }
    String capAlt = opt.type.replace('.','')
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"

	String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<div class='capability ${capAlt} mdl-cell mdl-cell--4-col' style='margin: 8px 0; '>"//${computedStyle}
	//retVal += "<button type='button' class='btn btn-default btn-lg btn-block device-btn-filled btn-device mdl-button--raised mdl-shadow--2dp' style='text-align:left; width:100%;' data-toggle='modal' data-target='#deviceListModal' "
    retVal += "<button type='button' class='btn btn-lg btn-block btn-device' style='border-radius:${opt.radius};border:0px;text-align:left; width:100%; min-width:${opt.width}' data-toggle='modal' data-target='#deviceListModal' "
    retVal += "data-capability='${opt.type}' data-elemname='${opt.name}' data-multiple='${opt.multiple}' data-ignore=''>"
    	retVal += "<span style='white-space:pre-wrap;'>${opt.title}</span><br><div style='${computedStyle}'>"
	retVal += "<span id='${opt.name}devlist' class='device-text' style='text-align: left;'>${dList}</span></button>"
	retVal += "<input type='hidden' name='settings[${opt.name}]' class='form-control submitOnChange' value='${idList}' placeholder='Click to set' id='settings[${opt.name}]'>"
	retVal += "<input type='hidden' name='deviceList' value='${opt.name}'><div class='device-list' style='display:none'>"
	retVal += "<div id='deviceListModal' style='border:1px solid #ccc;padding:8px;max-height:300px;overflow:auto;min-width:${opt.width}'><div class='checkAllBoxes my-2'>"
	retVal += "<label class='mdl-checkbox mdl-js-checkbox mdl-js-ripple-effect checkall mdl-js-ripple-effect--ignore-events is-upgraded' id='${opt.name}-checkall' for='${opt.name}-checkbox-0' data-upgraded=',MaterialCheckbox,MaterialRipple'>"
    	retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"${opt.name}-checkall\").classList.contains(\"is-checked\")){document.getElementById(\"${opt.name}-checkall\").classList.remove(\"is-checked\");}else{document.getElementById(\"${opt.name}-checkall\").classList.add(\"is-checked\");}}</script>"    
    retVal += "<input type='checkbox' class='mdl-checkbox__input checkboxAll' id='${opt.name}-checkbox-0' onclick='toggleMe${opt.name}()'><span class='mdl-checkbox__label'>Toggle All On/Off</span>"
	retVal += "<span class='mdl-checkbox__focus-helper'></span><span class='mdl-checkbox__box-outline'><span class='mdl-checkbox__tick-outline'></span></span>"
	retVal += "<span class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple'></span></span></label></div>"
	retVal += "<div id='${opt.name}-options' class='modal-body' style='overflow:unset'></div></div></div>"
	retVal += "<div class='mdl-button mdl-js-button mdl-button--raised pull-right device-save' data-upgraded=',MaterialButton' style='${computedStyle};width:6em;min-width:6em;'>Update</div></div></div>"
    
    return retVal
}

/*****************************************************************************
* Returns a string that will create an input enum or mode element for an app *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	options - list of values for the enum (modes will autofill)	     		 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/


String inputEnum(HashMap opt){
    String computedStyle = ''
    if(opt.type == 'mode') opt.options = location.getModes()
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"    
    if(!opt.multiple) {
    	opt.multiple = false
        mult = ' '
    } else {
        mult = 'multiple'
    }
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield' style='margin: 8px 0; padding-right: 8px;' data-upgraded=',MaterialTextfield'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' class='control-label'>${opt.title}</label><div class='SumoSelect sumo_settings[${opt.name}]' tabindex='0' role='button' aria-expanded='false'>"
    retVal += "<div style='${computedStyle}'><select id='settings[${opt.name}]' ${mult} name='settings[${opt.name}]' class='selectpicker form-control mdl-switch__input submitOnChange SumoUnder' placeholder='Click to set' data-default='' tabindex='-1' style='${computedStyle}'></div>"
    ArrayList selOpt = []
	if(settings["${opt.name}"]){
        if("${settings["${opt.name}"].class}" == 'class java.lang.String')
        	selOpt.add("${settings["${opt.name}"]}")
       else {               
        	settings["${opt.name}"].each{
            	selOpt.add("$it")
        	}
       }
    } else if(opt.defaultValue) selOpt.add("${opt.defaultValue}")
    if(mult != 'multiple') retVal+="<option value=''>Click to set</option>"
    opt.options.each{ option -> 
        if("$option".contains(':')){
            optSplit = "$option".replace('[','').replace(']','').split(':')
            optVal = optSplit[0]
            optDis = optSplit[1]
        } else {
            optVal = option
            optDis = option
        }
        sel = ' '
        selOpt.each{
            //log.debug "$it $optVal ${"$it" == "$optVal"}"
            if("$it" == "$optVal" ) 
            	sel = 'selected'
        }
        retVal += "<option value='${optVal}' ${sel}>${optDis}</option>"
    }
    retVal+= "</select></div></div>"
    return retVal
}

/*****************************************************************************
* Returns a string that will create an input boolean element for an app 	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	trackColor - CSS color descriptor for the switch track					 *
*	switchColor - CSS color descriptor for the switch knob					 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputBool(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    String trackColor = ' '
    String switchColor = ' '
    if(opt.trackColor) trackColor = "background-color:$opt.trackColor"
    if(opt.switchColor) switchColor = "background-color:$opt.switchColor"

    if(settings["${opt.name}"]) opt.defaultValue = settings["${opt.name}"]
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"lbl${opt.name}\").classList.contains(\"is-checked\")){document.getElementById(\"lbl${opt.name}\").classList.remove(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",false);}else{document.getElementById(\"lbl${opt.name}\").classList.add(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",true);}}</script>"
    retVal+="<div class='mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield' style='${computedStyle}' data-upgraded=',MaterialTextfield'><div class='w-fit'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' onmouseup=\"toggleMe${opt.name}();changeSubmit(document.getElementById('settings[$opt.name]'))\" id='lbl${opt.name}' class='mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded"
    if(opt.defaultValue == true) retVal += " is-checked"
    retVal += "' data-upgraded=',MaterialSwitch,MaterialRipple'>"
	retVal += "<input name='checkbox[${opt.name}]' id='settings[${opt.name}]' class='mdl-switch__input ' type='checkbox'><div class='mdl-switch__label w-fit'>${opt.title}"
	retVal += "</div><div class='mdl-switch__track' style='$trackColor'></div><div class='mdl-switch__thumb' style='$switchColor'><span class='mdl-switch__focus-helper'></span></div><span class='mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple is-animating' style='width: 137.765px; height: 137.765px; transform: translate(-50%, -50%) translate(24px, 24px);'></span></span></label>"
	retVal += "</div>"
    retVal += "<input id='hid${opt.name}' name='settings[${opt.name}]' type='hidden' value='${opt.defaultValue}'>"
	retVal += "</div>"

    return retVal
}

/*****************************************************************************
* Returns a string that will create an input checkbox element for an app 	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	cBoxColor - CSS color descriptor for the checkbox color					 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputCheckbox(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
	opt.type = 'bool'
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    
    if(settings["${opt.name}"]) 
    	opt.defaultValue = settings["${opt.name}"]
    else
        opt.defaultValue = false
    if(!opt.cBoxColor) opt.cBoxColor = '#000000'
    String cbClass = 'he-checkbox-unchecked'
    if(opt.defaultValue) 
    	cbClass = 'he-checkbox-checked'    

    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"lbl${opt.name}\").classList.contains(\"is-checked\")){document.getElementById(\"lbl${opt.name}\").classList.remove(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",false);}else{document.getElementById(\"lbl${opt.name}\").classList.add(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",true);}}</script>"
    retVal+="<div class='mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield' style='${computedStyle}' data-upgraded=',MaterialTextfield'><div class='w-fit'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' onmouseup=\"toggleMe${opt.name}();changeSubmit(document.getElementById('settings[$opt.name]'))\" id='lbl${opt.name}' class='mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded"
    if(opt.defaultValue == true) retVal += " is-checked"
    retVal += "' data-upgraded=',MaterialSwitch,MaterialRipple'>"
	retVal += "<input name='checkbox[${opt.name}]' id='settings[${opt.name}]' class='mdl-switch__input ' type='checkbox'><div><i class='${cbClass}' style='color:${opt.cBoxColor}'></i><span class='mdl-switch__label w-fit'>${opt.title}</span>"
	retVal += "</div><span class='mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple is-animating' style='width: 137.765px; height: 137.765px; transform: translate(-50%, -50%) translate(24px, 24px);'></span></span></label>"
	retVal += "</div>"
    retVal += "<input id='hid${opt.name}' name='settings[${opt.name}]' type='hidden' value='${opt.defaultValue}'>"
	retVal += "</div>"

    return retVal
}

/*****************************************************************************
* Returns a string that will create an button element for an app 	     	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String buttonLink(HashMap opt) { //modified slightly from jtp10181's code
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    String computedStyle = 'cursor:pointer;text-align:center;box-shadow: 2px 2px 4px #71797E;'
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button HREF element for an app     	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	destPage - (required unless using destUrl) name of the app page to go to *
*	destUrl - (required unless using destPage) URL for the external page	 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*****************************************************************************/
String buttonHref(HashMap opt) { //modified jtp10181's code
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    if(!opt.destPage && !opt.destUrl) 
    	return "Error missing Destination info"
    String computedStyle = 'cursor:pointer;text-align:center;box-shadow: 2px 2px 4px #71797E;'
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.destPage) {
    	inx = appLocation().lastIndexOf("/")
    	dest = appLocation().substring(0,inx)+"/${opt.destPage}"
    } else if(opt.destUrl) {
    	dest=opt.destUrl
    }
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='window.location.replace(\"$dest\")' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button element to hide/display a div	 *
*     for an app                                                             *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	divName - (require) name of the division to hide or display				 *
*	hidden - if true will hide the div immediately							 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*****************************************************************************/

String btnDivHide(HashMap opt) { 
    if(!opt.name || !opt.title || !opt.divName) 
    	return "Error missing name, title or division"
    String computedStyle = 'cursor:pointer;box-shadow: 2px 2px 4px #71797E;'
    if(!opt.width) opt.width = '100%'
    computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.destPage) {
    	inx = appLocation().lastIndexOf("/")
    	dest = appLocation().substring(0,inx)+"/${opt.destPage}"
    } else if(opt.destUrl) {
    	dest=opt.destUrl
    }
    String btnElem = "<i id='btn${opt.name}' class='material-icons he-shrink2' style='font-size:smaller'></i>"
    String script= "<script>document.getElementById(\"${opt.divName}\").style.display=\"block\"</script>"
    if(opt.hidden){
    	btnElem = "<i id='btn${opt.name}' class='material-icons he-enlarge2' style='font-size:smaller'></i>"
        script="<script>document.getElementById(\"${opt.divName}\").style.display=\"none\"</script>"
    }
    
    opt.title = "${btnElem}&nbsp;${opt.title}"
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "$script<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='btn=document.getElementById(\"btn${opt.name}\");div=document.getElementById(\"${opt.divName}\");if(div.style.display==\"none\"){btn.classList.remove(\"he-enlarge2\");btn.classList.add(\"he-shrink2\");div.style.display=\"block\";} else {btn.classList.remove(\"he-shrink2\");btn.classList.add(\"he-enlarge2\");div.style.display=\"none\";}' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button icon element for an app from   *
*	the materials-icon font						     						 *
*                                                                            *
*	name - (required) name of the icon to create			     			 *
*****************************************************************************/

String btnIcon(HashMap opt) {  //modified from jtp10181's code
    String computedStyle = ' '
    if(opt.size) computedStyle += "font-size:${opt.size};"
    if(opt.color) computedStyle += "color:${opt.color};"
	if(opt.name.startsWith('he'))
    	return "<i class='p-button-icon p-button-icon-left pi ${opt.name}' data-pc-section='icon' style='${computedStyle}'></i>"
	else if(opt.name.startsWith('fa'))                               
        return "<i class='fa-regular ${opt.name}' style='${computedStyle}'></i>"//fa-circle-info
    else 
        return "<i class='material-icons ${opt.name}' style='${computedStyle}'>${opt.name}</i>"
}

@Field static String ttStyleStr = "<style>.tTip {display:inline-block;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;text-align:left;}</style>"
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;} tr {border-right:2px solid black;}</style>"
