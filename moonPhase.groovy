/*
 * Moon Phase
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
 *    2021-03-17  thebearmay	 Original version 0.1.0
 *                               Calc corrections, add alternate input stream
 *    2021-03-18  thebearmay     Add an tile attribute, and icon path override
 *                               add scheduled update at midnight + 1 second
 *    2021-03-28  thebearmay     Add option to widen the quarterly checkpoints by 1%
 *    2021-03-29  thebearmay     Image path as an attribute
 *    2021-03-30  thebearmay     Image Only tile instead of path
 *    2021-07-04  thebearmay	 Merge pull request from imnotbob, strong typing of variables
 *    2021-08-28  thebearmay	 add option to use html attribute instead of moonPhaseTile
 *    2021-09-29  thebearmay	 Last Quarter typo - left out the first "r"
 *    2021-10-03  thebearmay     Change refresh to sunset
 *    2024-09-29  thebearmay 	 Typo
 *    2024-10-01  thebearmay     Normalize the phase breakpoints to cause less of a jump in images
 *                LibraSun       Add SVG graphic and Emoji text output
 */

import java.text.SimpleDateFormat
static String version()	{  return '2.0.1'  }

metadata {
    definition (
		name: "Moon Phase", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy"
	) {
        capability "Actuator"
        
        attribute "moonPhase", "enum", ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"]
	    attribute "moonPhaseNum", "number"
	    attribute "lastQryDate", "string"
        attribute "moonPhaseEmoji", "enum", ["🌑","🌒","🌓","🌔","🌕","🌖","🌗","🌘"]
        attribute "moonPhaseTile", "string"
        attribute "moonPhaseImg", "string"
        attribute "moonPhaseSvg", "string"
        attribute "html", "string"
        
        command "getPhase"
        command "calcPhase", [[name:"dateStr", type:"STRING", description:"Date (yyyy-MM-dd HH:mm:ss) to calculate the moon phase for."]]              
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("autoUpdate", "bool", title: "Enable automatic update at sunset")
    input("widenRange","bool",title:"Widen the Qtrly Checkpoints by 1%")
    input("htmlVtile", "bool", title:"Use html attribute instead of moonPhaseTile")
    input("iconPathOvr", "string", title: "Alternate path to moon phase icons \n(must contain file names moon-phase-icon-0 through moon-phase-icon-7)")
}

def installed() {
    log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
}

def calcPhase (String dateStr){
    Long cDate = dateCheck(dateStr)
    if (cDate !=0L) getPhase(cDate)

}

Long dateCheck(String dateStr) {
    try {
        Date cDate = Date.parse("yyyy-MM-dd HH:mm:ss",dateStr)
        updateAttr("error", "Valid time " + cDate + " entered.")
        return cDate.getTime()
    } catch (ignored) {
        updateAttr("error", "Invalid date string use format yyyy-MM-dd HH:mm:ss")
        return 0L
    }
}

void getPhase(Long cDate = now()) {
    Date d_refDate = Date.parse("yyyy-MM-dd HH:mm:ss","2000-01-06 18:14:00") //First New Moon of 2000
    Long refDate = d_refDate.getTime()

    Double lunarDays = 29.53058770576
    Double lunarSecs = lunarDays*8640000

    def sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    Double phaseWork = cDate - refDate //subtract out first new moon of 2000 to get elapsed seconds
    phaseWork = phaseWork/lunarSecs/10.0D //calculate lunar cycles
    
    phaseWork = phaseWork - phaseWork.toInteger() //remove whole cycles
    if(!widenRange)
        phaseWork = phaseWork.round(2)
    else
        phaseWork = phaseWork.round(1)
    
    if(phaseWork == 1.0) phaseWork = 0.0
    
    updateAttr("moonPhaseNum", phaseWork)
    updateAttr("lastQryDate",sdf.format(cDate))
    
    String iconPath = "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    Integer imgNum
    String phaseEmoji
    String phaseText
    //                                 .125               .250             .375              .500         .625              .750            .875
    List<String>imgList = ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"]
    if (phaseWork < 0.125D){
        imgNum = 0
    }else if (phaseWork < 0.25D){
        imgNum = 1
    }else if (phaseWork < 0.375D){
        imgNum = 2
    }else if (phaseWork < 0.5D){
        imgNum = 3
    }else if (phaseWork < 0.625D){
        imgNum = 4	
    }else if (phaseWork < 0.75D){
        imgNum = 5
    }else if (phaseWork < 0.875D){
        imgNum = 6
    }else if (phaseWork <= 1.0D){
        imgNum = 7
    }else {
        imgNum = null
    }

/*    if(!widenRange){
        if (phaseWork == 0.0D){
            imgNum = 0
        }else if (phaseWork < 0.25D){
            imgNum = 1
        }else if (phaseWork == 0.25D){
            imgNum = 2
        }else if (phaseWork < 0.5D){
            imgNum = 3
        }else if (phaseWork == 0.5D){
            imgNum = 4	
        }else if (phaseWork < 0.75D){
            imgNum = 5
        }else if (phaseWork == 0.75D){
            imgNum = 6
        }else if (phaseWork < 1.0D){
            imgNum = 7
        }else {
            imgNum = null
        }
    }else {
        if (phaseWork <= 0.01D){
            imgNum = 0
        }else if (phaseWork < 0.24D){
            imgNum = 1
        }else if (phaseWork <= 0.26D){
            imgNum = 2
        }else if (phaseWork < 0.49D){
            imgNum = 3
        }else if (phaseWork <= 0.51D){
            imgNum = 4
        }else if (phaseWork < 0.74D){
            imgNum = 5
        }else if (phaseWork <= 0.76D){
            imgNum = 6
        }else if (phaseWork < 1.0D){
            imgNum = 7
        }else {
            imgNum = null
        }
    }
*/

    if(imgNum!=null) {
        phaseText = imgList[imgNum]
    } else phaseText = "Error - Out of Range"
    
    // Select emoji for moon phase
    phaseEmoji = ["🌑","🌒","🌓","🌔","🌕","🌖","🌗","🌘"][imgNum]
    
    // Generate SVG output from template
    String svgString = """
  <svg width="140" height="140">
  <filter id="blur"><feGaussianBlur stdDeviation="5"/></filter>  
  <mask id="arc">
  <path d="M70,5 Arx1,65 180 0 sf1 70,135 Arx2,65 180 0 sf2 70,5 z" fill="#fff" filter="url(#blur)"/></mask>
  <radialGradient id="shadow"><stop offset="10%" stop-color="#0007"/>
  <stop offset="90%" stop-color="#000d"/></radialGradient>
  <image x="6" y="4" width="130" href="https://bit.ly/3Brrs54"/>
  <circle cx="70" cy="70" r="65" mask="url(#arc)" fill="url(#shadow)"/>
  </svg>
      """
    Double rx1 = 65.0 // right limn radius
    Integer sf1 = 1 // right limn sweep flag concave )
    Double rx2 = 65.0  // left limn radius
    Integer sf2 = 1 // left limn sweep flag concave (
    if (phaseWork<=0.25) {
        rx1 = rx1 * (1 - 4*phaseWork) // 70 🌑 .. 🌒 .. 0 🌓
        // right limn moving, concave )
        // left limn fixed, concave (
    } else if (phaseWork>0.25 && phaseWork<=0.50) {
        rx1 = rx1 * (4*phaseWork - 1) // 0 🌓 .. 🌔 .. 70 🌕
        sf1 = 0 // right limn moving, vertical ► concave (
        // left limn fixed, concave (
    } else if (phaseWork>0.50 && phaseWork<=0.75) {
        rx2 = rx2 * (3 - 4*phaseWork) // 70 🌕 .. 🌖 .. 0 🌗
        // right limn fixed, concave )
        sf2 = 0 // left limn moving, concave )
    } else {
        rx2 = rx2 * (4*phaseWork - 3) // 0 🌗 .. 🌘 .. 70 🌑
        // right limn fixed, concave )
        // left limn moving, vertical ► concave (
    }
    svgString = svgString.replace("rx1","$rx1").replace("rx2","$rx2").replace("sf1","$sf1").replace("sf2","$sf2")
    
    // Update device attributes
    updateAttr("moonPhaseEmoji", phaseEmoji)
    updateAttr("moonPhaseSvg", svgString)
    updateAttr("moonPhaseImg", "<img class='moonPhase' src='${iconPath}moon-phase-icon-${imgNum}.png' />")
    updateAttr("moonPhase", phaseText)
    String phaseIcon = "<div id='moonTile'><img class='moonPhase' src='${iconPath}moon-phase-icon-${imgNum}.png'><p class='small' style='text-align:center'>$phaseText</p></img></div>"
    if(!htmlVtile)
        updateAttr("moonPhaseTile",phaseIcon)
    else
        updateAttr("html",phaseIcon)
    
    HashMap riseAndSet = getSunriseAndSunset()
    if(riseAndSet.sunset < new Date()){
        getSunriseAndSunset(sunsetOffset: "+24:00")
    }
    unschedule()
    runOnce(riseAndSet.sunset, getPhase)
}

void updateAttr(String aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

void updateAttr(String aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def updated(){
    log.trace "updated()"
    HashMap riseAndSet = getSunriseAndSunset()
    if(riseAndSet.sunset < new Date()){
        getSunriseAndSunset(sunsetOffset: "+24:00")
    }
    unschedule()
    runOnce(riseAndSet.sunset, getPhase)
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
