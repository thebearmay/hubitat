/*
 * Download Speed Test
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
 *    Date         Who           What
 *    ----         ---           --------------------------------------
 *    20Jun2024    thebearmay    Round the result to 2 places
 *    21Jun2024                  Separate image and text downloads
 *
 */
import groovy.transform.Field

static String version()	{  return '0.0.3'  }

metadata {
    definition (
		name: "Download Speed Test", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/speedTest.groovy"
	) {
        capability "Actuator"
       
        attribute "result", "string"
        
                          
        command "checkSpeed", [[name:"url*", type:"STRING", description:"URL to download from", title:"URL"]]
    
    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?", width:4)
}


def installed() {
    log.trace "Speed Test v${version()} installed()"
}

def configure() {
    if(debugEnabled) log.debug "configure()"

}

def updateAttr(aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def test(){
    log.debug "Test $debugEnabled"
}

def checkSpeed(url){
    if(debugEnabled)
        log.debug "checking speed for $url"
    fileExt = url.substring(url.lastIndexOf(".")+1)
    updateAttr("result","Test Running")
    imageType = ['gif','jpg','png', 'svg']
    if(fileExt in imageType)
        readImage(url)
    else {
        try {
            tStart = new Date().getTime()
            httpGet([uri: url, textParser: true]) { resp ->
                if(resp!= null) {
                    dataSize = """${resp.data}""".size()
                   //if(debugEnabled) log.debug "$dataSize bytes read" 
                   tEnd = new Date().getTime()
                   totTime = (tEnd-tStart)/100
                   mBits = (dataSize*8)/1000
                   result = "Start Time: $tStart<br>End Time: $tEnd<br>Total Time(seconds): $totTime<br>File Size: $dataSize bytes (${mBits.toFloat().round(2)} mbits)<br>Download Speed: ${((mBits/totTime)).toFloat().round(2)} Mbps"
                   updateAttr("result", result)
                }
                else {
                    updateAttr("result", "Null Response")
                }
            }
        } catch (exception) {
            log.error "Read Error: ${exception.message}"
            updateAttr("result", exception.message)
        }
    }
}

void readImage(url){ 
    def imageData

    if(debugEnabled) log.debug "Getting Image $imagePath"
    tStart = new Date().getTime()
    httpGet([
        uri: "$url",
        contentType: "*/*",
        textParser: false]){ response ->
            if(debugEnabled) log.debug "${response.properties}"
            imageData = response.data 
            if(debugEnabled) log.debug "Image Size (${imageData.available()} ${response.headers['Content-Length']})"

            def bSize = imageData.available()
            def imageType = response.contentType 
            byte[] imageArr = new byte[bSize]
            imageData.read(imageArr, 0, bSize)
            tEnd = new Date().getTime()
            totTime = (tEnd-tStart)/100
            mBits = (imageArr.length*8)/1000
            if(debugEnabled) log.debug "Image size: ${imageArr.length} Type:$imageType" 
                result = "Start Time: $tStart<br>End Time: $tEnd<br>Total Time(seconds): $totTime<br>File Size: ${imageArr.length} bytes (${(mBits).toFloat().round(2)} mbits)<br>Download Speed: ${(mBits/totTime).toFloat().round(2)} Mbps"
            updateAttr("result", result)
            //return [iContent: imageArr, iType: imageType]
        }    
}
    
def updated(){
	log.trace "updated()"
	if(debugEnabled) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
