 /*
 * RGB Effects - Inovelli Black Dimmers
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
 *    Date        Who           What
 *    ----        ---           ----
 *    2021-01-15  thebearmay	Original version 0.1.0
 *    2021-01-22  thebearmay    Testing complete setting version to 1.0.0
 * 
 */

static String version()	{  return '1.0.0'  }


definition (
	name: 			"RGB Effects - Inovelli Black Dimmers", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Creates special effects for RGB devices that don't natively support them",
	category: 		"My Apps",
	importURL:		"https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/rgbEffects-LZW31.groovy",
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {

   page name: "mainPage"
   page name: "deviceCharacteristics"
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
    atomicState.runEffect = false
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
	    	section("Main")
		    {
                def colorOptions = ["red","orange","yellow","green","blue","violet"]
                input "rgbDevice", "capability.colorControl", title: "RGB Color Device:", multiple: true, required: true, submitOnChange: true
                if (rgbDevice != null) href "deviceCharacteristics", title: "Device Characteristics", required: false
                
                input name: "effect", type: "enum", title: "Select Effect", required: true, multiple: false, options: ["Simple Color Change","Rotate","Flash","Pulse"], submitOnChange: true
                
                if (effect == "Pulse" || effect == "Flash") paragraph "<h3>Warning:</h3><p>Use of the Pulse or Flash effect could flood the mesh if used for prolonged periods</p>"
                
                if (effect == "Rotate"){
                    input name:"colors", type:"enum", title:"Colors to Rotate", required: true, multiple: true, options: colorOptions, submitOnChange: true
                    input name:"colorDispTime", type:"number", title:"Seconds to Display Each Color", required: true, multiple: false, defaultValue:5
                } 
                else input name:"colors", type:"enum", title:"Color for effect", required: true, multiple: false, options: colorOptions, submitOnChange: true

                input name:"numSeconds", type:"number", title:"Number of Seconds to Run (0=infinite)",required: true
                input "storeAndExecute", "button", title:"Execute Effect"
                input "restoreOverride", "bool", title: "Override auto restore", defaultValue: false, submitOnChange:true
                input "stopAndRestore", "button", title: "Stop Effect"
		    }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def deviceCharacteristics(){
    dynamicPage (name: "deviceCharacteristics", title: "", install: false, uninstall: false) {
	  section("Device Characteristics"){
          
          for(i=0;i<rgbDevice.size();i++){
            if (rgbDevice[i].label) rgbName= rgbDevice[i].label
            else rgbName = rgbDevice[i].name
            def rgbDeviceState = ""
            def nl = false
            rgbDevice[i].supportedAttributes.each {
                if (nl) rgbDeviceState += "\n"
                def tempValue = rgbDevice[i].currentValue("$it")
    	        rgbDeviceState += "$it: $tempValue"
                nl = true
            }
            def devAttr = rgbDevice[i].supportedAttributes
            def devCap = rgbDevice[i].capabilities
            def devCmd = rgbDevice[i].supportedCommands
            paragraph "<h2>$rgbName</h2><span style='font-weight:bold'>Attributes:</span>$devAttr\n\n$rgbDeviceState\n\n<span style='font-weight:bold'>Capabilities:</span> $devCap\n\n<span style='font-weight:bold'>Commands:</span> $devCmd"    
            href "mainPage", title: "Return", required: false
          }
       }
    }
}

def appButtonHandler(btn) {
    switch(btn) {
          case "storeAndExecute":  storeAndExecute()
               break
          case "stopAndRestore": stopAndRestore()
              break
          default: 
              log.error "Undefined button $btn pushed"
              break
      }
}

def storeAndExecute() {
    log.info "Starting Effect $effect"
    state.clear()
    state.runEffect = true
    addDevice()
    
    switch (effect){
        case "Simple Color Change":
            if(numSeconds>0) runIn(numSeconds, "stopAndRestore")
            simple(colors)
            break
        case "Rotate":
            if(numSeconds>0) runIn(numSeconds, "stopAndRestore")
            rotateColors(["inx":0])
            break
        case "Flash":
            if(numSeconds>0) runIn(numSeconds, "stopAndRestore")
            flashColor()
            break
        case "Pulse":
            if(numSeconds>0) runIn(numSeconds, "stopAndRestore")
            pulseColor(["inx":5])
            break
        default: log.error "Invalid Effect Selected - $effect</p>"
    }
}

def stopAndRestore(){
    log.info "Effect terminating with restore override = $restoreOverride"
    state.runEffect = false
    if(!restoreOverride){
        for (i=0; i<state.numDev; i++){
            sHue=state.devAttr["$i"]["hue"]
            sSat=state.devAttr["$i"]["saturation"]
            sLev=state.devAttr["$i"]["level"]
            rgbDevice[i].setColor([hue:sHue, saturation:sSat, level:sLev])
        }
    }

}
                       
def addDevice(){
//    log.trace "addDevice"
    if(state.numDev == null) state.numDev = 0
    if(state.devAttr == null) state.devAttr=[:]
    for(i=0;i<rgbDevice.size();i++){
        if (rgbDevice[i].label) rgbName= rgbDevice[i].label
        else rgbName = rgbDevice[i].name
        state.devAttr[state.numDev] = [name: rgbName, hue:rgbDevice[i].currentValue("hue"), saturation:rgbDevice[i].currentValue("saturation"), level:rgbDevice[i].currentValue("level")]
    
        state.numDev++
    }
}

def intialize() {
    state.numDev = 0 
    state.devAttr=[:]
}
 
    
def simple(color) {
    // Loop through the devices selected
    for(i=0;i<rgbDevice.size();i++){
        rgbDevice[i].setColor([hue:colorToHue(color),saturation:rgbDevice[i].currentValue("saturation"),level:rgbDevice[i].currentValue("level")])
    }
}
    
def rotateColors(data){
    colorInx = data.inx
    // Loop through the colors selected
    if(state.runEffect){
        simple(colors[colorInx])
        colorInx++
        if(colorInx >= colors.size()) colorInx = 0
        runIn(colorDispTime,"rotateColors",[data:[inx:colorInx]]) 
    } 
}

def flashColor() {
 //   log.debug "on"
    if (state.runEffect){
        for(i=0;i<rgbDevice.size();i++){
           rgbDevice[i].on()
        }
       runIn(2,"flashColorOff")
    }
}

def flashColorOff() {
//    log.debug "off"
    if (state.runEffect){
       for(i=0;i<rgbDevice.size();i++){
           rgbDevice[i].off()
       }
       runIn(2,"flashColor")
    }
}

    
def pulseColor(data) {
    levelInc=data.Inx
    if (state.runEffect){ 
       for(i=0;i<rgbDevice.size();i++){
          rgbDevice[i].setLevel(levelInc)
       }            
       levelInc+=5
       if(levelInc >= 100) levelInc = 5
       runIn(1,"pulseColor",[data:[inx:levelInc]])
    }
}

def colorToHue(cName){
   switch (cName){
        case "red": 
            return 99
            break
        case "orange":
            return 0
            break
        case "yellow":
            return 11
            break
        case "green":
            return 30
            break
        case "blue": 
            return 67
            break
        case "violet":
            return 76
            break
       default: 
           log.error "Invalid color requested: $cName"
           break
    }
}
