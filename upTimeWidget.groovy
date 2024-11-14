 /*
 * Uptime Widget
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
 *    21Dec2023   thebearmay    Initialize at install
 *                              Add text color option
 *    14Nov2024   thebearmay    Add URL Capability
 *
*/
import java.text.SimpleDateFormat
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "1.0.4"}

metadata {
    definition (
        name: "Uptime Widget", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/upTimeWidget.groovy"
    ) {
        capability "Actuator"
        capability "Initialize"
        capability "URL"
        attribute "html", "string"
        attribute "URL", "string"
        attribute "type", "string"//iframe, image or video
      }   
}
preferences {
    //input("upTimeDesc", "enum", title: "Uptime Descriptors", defaultValue:"d/h/m/s", options:["d/h/m/s"," days/ hrs/ min/ sec"," days/ hours/ minutes/ seconds"])
    input("textColor","string",title: "Text Color", submitOnChange:true)
    input("backGroundColor","string",title:"Background Color", submitOnChange:true)
}
@SuppressWarnings('unused')
void installed() {
    log.trace "Uptime Widget v${version()} installed()"
    initialize()

}

void initialize() {
    log.info "Uptime Widget v${version()} initialized"
    state.hubStart = now()-(location.hub.uptime*1000)
    uploadHubFile("upTimeWidget.html",genHtml(state.hubStart).getBytes("UTF-8"))
    updateAttr('html',"<iframe src='http://${location.hub.localIP}:8080/local/upTimeWidget.html'></iframe>")
    updateAttr("URL","http://${location.hub.localIP}:8080/local/upTimeWidget.html")
    updateAttr("type","iframe")

}


void updated(){
    if(debugEnable) log.debug "updated"
    initialize()

}

void updateAttr(String aKey, aValue, String aUnit = ""){
    aValue = aValue.toString()
    if(aValue.contains("Your hub is starting up"))
       return

    sendEvent(name:aKey, value:aValue, unit:aUnit)
    if(attrLogging) log.info "$aKey : $aValue$aUnit"
}

def genHtml(sTime){
    //utD=upTimeDesc.split("/")
    html="""
<span style='color:$textColor;background-color:$backGroundColor' id='upTimeElement'>Initializing...</span>
 <script type='text/javascript'>

        setInterval(utimer,1000,parseInt($sTime/1000));

		//utimer(parseInt($sTime/1000))
        function utimer(ht){
			tnow = Math.floor(Date.now()/1000)
			ut = Math.floor( (tnow - ht))
            days = Math.floor(ut/(3600*24));
            hrs = Math.floor((ut - (days * (3600*24))) /3600);
            min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60);
            sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60)));
            oString = days+'days, '+hrs+'hrs, '+min+'min, '+sec+'sec';
            document.getElementById('upTimeElement').innerHTML = oString;
        }
</script>
"""
    return html
}
