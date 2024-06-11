/*
* Log Tile Device
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
*    2024-06-10  thebearmay     combine Maverrick85's InfluxDB Logger code with the Notification Tile to create a logging tile	
*    2024-06-11                 Add color to Info logging
*                               v1.0.3 Change name escaping to use the HTML version
*                               v1.0.4 Minor code cleanup
*
*/
import java.text.SimpleDateFormat
import groovy.transform.Field
//Logger imports
import groovy.json.JsonSlurper
import hubitat.device.HubAction
import hubitat.device.Protocol
import java.time.*

static String version()	{  return '1.0.4'  }

@Field sdfList = ["ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy HH:mm:ss:SSS","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss","dd/MM/yyyy HH:mm:ss:SSS", "MM/dd/yyyy HH:mm:ss","MM/dd/yyyy HH:mm:ss:SSS", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "MM/dd h:mma", "HH:mm", "H:mm","h:mma", "None"]

metadata {
	definition (
			name: "Log Tile Device", 
			namespace: "thebearmay", 
			description: "Simple driver to display a small subset of the active log.",
			author: "Jean P. May, Jr.",
			importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/logTile.groovy",
            singleThreaded: true
		) 
	{

		capability "Momentary"
		capability "Configuration"
		capability "Initialize"

		attribute "html", "STRING"
        
        command "disconnect"

	}   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
	input("sdfPref", "enum", title: "Date/Time Format", options:sdfList, defaultValue:"ddMMMyyyy HH:mm:ss:SSS")
	input("leadingDate", "bool", title:"Use leading date instead of trailing")
	input("msgLimit", "number", title:"Number of messages from 5 to 20",defaultValue:5, range:5..20)

}

void installed() {
	if (debugEnable) log.trace "installed()"
	state.lastLimit=0
	configure()
}

void updated(){
	if (debugEnable) log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)

// V2.0.2 When converting from original version set state variables, adjust html in html to make it work with V2.0.0+	
	if (state?.msgCount == null)
		{
		state.lastLimit=5
		wkTile=device.currentValue("html")
		int x = wkTile.lastIndexOf('</div>');	
		if (x>0)										//if there is anything in tile, adjust for v2.0.0
			{
			msgFilled=5
			int i = wkTile.lastIndexOf('<br /> </div>');	
			if (debugEnable) log.debug "at While i: ${i} ${msgFilled}"
			while (i>0 && msgFilled>0)
				{
				if (debugEnable) log.debug "in loop i: ${i} ${msgFilled}"
				msgFilled--
				wkTile = wkTile.substring(0, i) + '</div>'
				i = wkTile.lastIndexOf('<br /> </div>');
				if (debugEnable) log.debug "out loop i: ${i} ${msgFilled}"
				}
			if (debugEnable) log.debug "done While i: ${i} ${msgFilled}"
			sendEvent(name:"html", value:wkTile)
			state.msgCount=msgFilled
			}
		else
			{												//process empty tile
			if (debugEnable) log.debug "Initialize an empty tile" 
			state.msgCount=0
			configure()
			}
		}

	if(msgLimit == null) device.updateSetting("msgLimit",[value:5,type:"number"])
// V2.0.3 When new msgLimit less than prior(state) msgLimit adjust message and state values	
	if (state?.lastLimit.toInteger()>settings.msgLimit.toInteger())
		{
		wkTile=device.currentValue("html")
		msgFilled=state.msgCount.toInteger()
		if (debugEnable) log.debug "Shinking tile count lastLimit ${state.lastLimit} newLimit ${settings.msgLimit} msgCount ${msgFilled}"
		int i = wkTile.lastIndexOf('<br />');
		while (i != -1 && msgFilled > settings.msgLimit.toInteger())
			{
			wkTile = wkTile.substring(0, i) + '</div>';
			msgFilled--
			i = wkTile.lastIndexOf('<br />');
			if (debugEnable) log.debug "looping on shrink msgCount ${msgFilled}"
			}
		state.msgCount=msgFilled
		sendEvent(name:"html", value:wkTile)
		}

	state.lastLimit=settings.msgLimit	
}

void configure() {
	log.trace "configure()"
	if(msgLimit == null) device.updateSetting("msgLimit",[value:5,type:"number"])
	sendEvent(name:"html", value:'<div class="html" style="text-align:left"></div>')
	state.msgCount=0
	runIn(5, "connect")
}

void initialize(){
    runIn(5, "connect")
}

void logReceived(notification, timeStamp){
	if (debugEnable) log.debug "logReceived entered: ${notification}" 
	//dateNow = new Date()
    if(sdfPref == null) device.updateSetting("sdfPref",[value:"ddMMMyyyy HH:mm:ss:SSS",type:"enum"])
    if(sdfPref != "None") {
        SimpleDateFormat sdf = new SimpleDateFormat(sdfPref)
	    if (leadingDate)
			notification = sdf.format(timeStamp) + " " + notification
		else
			notification += " " + sdf.format(timeStamp)
    }

	//	insert new message at beginning	of html string
		msgFilled = state.msgCount.toInteger()
		if (msgFilled>0)
			wkTile=device.currentValue("html").replace('<div class="html" style="text-align:left">','<div class="html" style="text-align:left">' + notification + '<br />')
		else
			wkTile=device.currentValue("html").replace('<div class="html" style="text-align:left">','<div class="html" style="text-align:left">' + notification)

	//	when msg count exceeds limit, purge last message
		if (debugEnable) log.debug "logReceived msgFilled: ${msgFilled} msgLimit: ${settings.msgLimit}" 
		if (msgFilled < settings.msgLimit.toInteger())
			msgFilled++
		else
			{
			int i = wkTile.lastIndexOf('<br />');
			if (i != -1) 
				wkTile = wkTile.substring(0, i) + '</div>';
			}

	//	Ensure tile length is less than 1024 and hopefully stop loops
		int wkLen=wkTile.length()	
		while (wkLen > 1024 && msgFilled > 0)
			{
			if (debugEnable) log.debug "wkTile length ${wkLen}> 1024 truncating msgCount: ${msgFilled}"
			int i = wkTile.lastIndexOf('<br />');
			if (i != -1) 
				{
				wkTile = wkTile.substring(0, i) + '</div>';
				msgFilled--
				}
			else
				{
				wkTile='<div class="html" style="text-align:left"></div>'
				msgFilled=0
				}
			wkLen=wkTile.length()
			if (debugEnable) log.debug "Truncated wkTile length ${wkLen}, msgCount: ${msgFilled}"
			}

	//	Update attributes and state
		sendEvent(name:"html", value: wkTile)
		state.msgCount = msgFilled

	}    

void logsOff(){
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void push() {
 	configure()
}



// Start of modified InfluxDB Logger code
void parse(String description) {

    def descData = new JsonSlurper().parseText(description)

    if ("${descData.id}" != "${device.id}") {
        String name = escapeStringHTMlforMsg(descData.name)
        String message = escapeStringHTMlforMsg(descData.msg)
        String timestmp = '"' + descData.time + '"'
        String id = descData.id.toString()
        switch (descData.level) {
            case 'error':
                sevCode = 3
                break
            case 'warn':
                sevCode = 4
                break
            case 'info':
                sevCode = 6
                break
            default :
                sevCode = 7
        }

        switch (descData.level) {
            case 'error':
                severity = "<span style='background-color:red;color:black'>err</span>"
                break
            case 'warn':
                severity = "<span style='background-color:yellow;color:black'>warn</span>"
                break
            case 'info':
                severity = "<span style='background-color:#DDBD9E;color:black'>info</span>"
                break
            default :
                severity = "<span style='background-color:lightBlue;color:black'>debug</span>"
        }
        String level = descData.level.toString()
        String type = descData.type.toString()
        long timeNow = new Date().time
        def dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        def date = Date.parse(dateFormat, descData.time)
        epoch_milis = date.getTime()// * 1000000

        //loggerQueueMap += ["syslog,appname=${name},facility=${type},host=${hubName},hostname=${hubName},severity=${severity} facility_code=1,message=${msg},procid=${id},severity_code=${sevCode},timestamp=${timestmp} ${epoch_milis}"]
        logMsg = "${type}:${name} ${severity} ${message}"
        logReceived(logMsg, epoch_milis)
    }
}
    
void connect() {
    if (debugEnable) { log.debug "attempting connection" }
    try {
        interfaces.webSocket.connect("http://localhost:8080/logsocket")
        pauseExecution(1000)
    } catch (e) {
        log.error "initialize error: ${e.message}"
    }

}

void disconnect() {
    interfaces.webSocket.close()
}

void webSocketStatus(String message) {
    // handle error messages and reconnect
    if (debugEnable) { log.debug "Got status ${message}" }
    if (message.startsWith("failure")) {
        // reconnect in a little bit
        runIn(5, connect)
    }
}

private String escapeStringForInfluxDB(String str) {
    //logger("$str", "info")
    if (str) {
        str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
        str = str.replaceAll(",", "\\\\,") // Escape commas.
        str = str.replaceAll("=", "\\\\=") // Escape equal signs.
        str = str.replaceAll("\"", "\\\\\"") // Escape double quotes.
    //str = str.replaceAll("'", "_")  // Replace apostrophes with underscores.
    }
    else {
        str = 'null'
    }
    return str
}

private String escapeStringHTMlforMsg(String str) {
    //logger("$str", "info")
    if (str) {
        str = str.replaceAll("&apos;", "&") // Escape spaces.
        str = str.replaceAll("&lt;", "<") // Escape commas.
        str = str.replaceAll("&gt;", ">") // Escape equal signs.
        str = str.replaceAll("&#027;", "'") // Escape double quotes.
        str = str.replaceAll("&#039;", "'")  // Replace apostrophes with underscores.
        str = str.replaceAll("&apos;", "'")
    }
    else {
        str = 'null'
    }
    return str
}
