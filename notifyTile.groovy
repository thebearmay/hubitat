/*
* Notify Tile Device
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
*    2021-01-06  thebearmay	Original version 0.1.0
*    2021-01-07  thebearmay	Fix condition causing a loss notifications if they come in rapidly
*    2021-01-07  thebearmay	Add alternative date format
*    2021-01-07  thebearmay	Add last5H for horizontal display
*    2021-01-07  thebearmay	Add leading date option
*    2021-03-10  thebearmay	Lost span tag with class=last5
*    2021-11-14  ArnB  2.0.0	Add capability Momentary an routine Push allowing a Dashboard switch to clear all messages. 	
*    2021-11-15  ArnB  2.0.0	Revise logic minimizing attributes and sendevents. Allow for 5 to 20 messages in tile. Insure tile is less than 1024 	
*    2021-11-16  ArnB  2.0.1	Fix: storing one less message than requested. 
*					correct <br/> to <br />
*					Restore: attribute last5H as an optional preference. 
*    2021-11-17  ArnB  2.0.2	Add conversion logic from original version in Update routine 
*    2021-11-17  ArnB  2.0.3	Add logic when message count shinks rather than reconfigure
*    2021-11-18  ArnB  2.0.4	Add singleThreaded true
*    2021-11-18  thebearmay    2.0.5 Remove unused attributes from v1.x.x
*    2021-11-20  thebearmay    Add option to only display time
*    2021-11-22  thebearmay    make date time format a selectable option
*    2021-12-07  thebearmay    add "none" as a date time format
*    2022-04-06  thebearmay    fix max message state coming back as string
*    2022-09-15  thebearmay    issue with clean install
*    2022-12-06  thebearmay    additional date/time format
* 	 2025-04-03	 thebearmay	   add time/date formats, lowered mininum message count to 1
*    2025-04-20  amithalp	   add color options
*/
import java.text.SimpleDateFormat
import groovy.transform.Field
static String version()	{  return '2.0.13'  }

@Field sdfList = ["ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "MM/dd h:mma", "HH:mm", "H:mm","h:mma", "HH:mm ddMMMyyyy","HH:mm:ss ddMMMyyyy","hh:mma ddMMMyyyy", "HH:mm:ss dd/MM/yyyy", "HH:mm:ss MM/dd/yyyy", "hh:mma dd/MM/yyyy ", "hh:mma MM/dd/yyyy", "HH:mm yyyy-MM-dd", "None"]

metadata {
	definition (
			name: "Notification Tile", 
			namespace: "thebearmay", 
			description: "Simple driver to act as a destination for notifications, and provide an attribute to display the last 5 on a tile.",
			author: "Jean P. May, Jr.",
			importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifyTile.groovy",
            singleThreaded: true
		) {
			capability "Notification"
			capability "Momentary"
            capability "Configuration"

			attribute "last5", "STRING"
			attribute "last5H", "STRING"

			}   
		}

	preferences {
		input("debugEnable", "bool", title: "Enable debug logging?")
		input("sdfPref", "enum", title: "Date/Time Format", options:sdfList, defaultValue:"ddMMMyyyy HH:mm")
		input("leadingDate", "bool", title:"Use leading date instead of trailing")
		input("msgLimit", "number", title:"Number of messages from 1 to 20",defaultValue:5, range:1..20)
		input("create5H", "bool", title: "Create horizontal message tile?")
		
        input("colorE", "text", title: "Color for [E] Emergency", defaultValue: "red")
        input("colorH", "text", title: "Color for [H] High", defaultValue: "orange")
        input("colorL", "text", title: "Color for [L] Low", defaultValue: "goldenrod")
        input("colorN", "text", title: "Color for [N] Normal", defaultValue: "green")
        input("colorDefault", "text", title: "Default color (no tag)", defaultValue: "black")

	}

	void installed() {
		if (debugEnable) log.trace "installed()"
		state.lastLimit=0
		configure()
	}

	void updated(){
        if (debugEnable) log.trace "updated()"
		if(debugEnable) runIn(1800,logsOff)

	// V2.0.2 When converting from original version set state variables, adjust html in last5 to make it work with V2.0.0+	
		if (state?.msgCount == null)
			{
			state.lastLimit=5
			wkTile=device.currentValue("last5")
			int x = wkTile.lastIndexOf('</span>');	
			if (x>0)										//if there is anything in tile, adjust for v2.0.0
				{
				msgFilled=5
				int i = wkTile.lastIndexOf('<br /> </span>');	
				if (debugEnable) log.debug "at While i: ${i} ${msgFilled}"
				while (i>0 && msgFilled>0)
					{
					if (debugEnable) log.debug "in loop i: ${i} ${msgFilled}"
					msgFilled--
					wkTile = wkTile.substring(0, i) + '</span>'
					i = wkTile.lastIndexOf('<br /> </span>');
					if (debugEnable) log.debug "out loop i: ${i} ${msgFilled}"
					}
				if (debugEnable) log.debug "done While i: ${i} ${msgFilled}"
				sendEvent(name:"last5", value:wkTile)
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
			wkTile=device.currentValue("last5")
			msgFilled=state.msgCount.toInteger()
			if (debugEnable) log.debug "Shinking tile count lastLimit ${state.lastLimit} newLimit ${settings.msgLimit} msgCount ${msgFilled}"
			int i = wkTile.lastIndexOf('<br />');
			while (i != -1 && msgFilled > settings.msgLimit.toInteger())
				{
				wkTile = wkTile.substring(0, i) + '</span>';
				msgFilled--
				i = wkTile.lastIndexOf('<br />');
				if (debugEnable) log.debug "looping on shrink msgCount ${msgFilled}"
				}
			state.msgCount=msgFilled
			sendEvent(name:"last5", value:wkTile)
			}
		
		if (!settings.create5H)
			sendEvent(name:"last5H", value:'<span class="last5"></span>')
		state.lastLimit=settings.msgLimit	
	}

	void configure() {
		log.trace "configure()"
        if(msgLimit == null) device.updateSetting("msgLimit",[value:5,type:"number"])
		sendEvent(name:"last5", value:'<span class="last5"></span>')
		sendEvent(name:"last5H", value:'<span class="last5"></span>')
		state.msgCount=0
        if(location.hub.firmwareVersionString >= "2.2.8.0") {
            if(notify1){
                device.deleteCurrentState("notify1")
                device.deleteCurrentState("notify2")
                device.deleteCurrentState("notify3")
                device.deleteCurrentState("notify4")
                device.deleteCurrentState("notify5")
            }
        }
	}

void deviceNotification(notification){
        if (debugEnable) log.debug "deviceNotification entered: ${notification}" 

        dateNow = new Date()
        if(sdfPref == null) device.updateSetting("sdfPref",[value:"ddMMMyyyy HH:mm",type:"enum"])

        String originalMsg = notification?.trim()

        // Keep the tag for parsing, remove after colorization
        String msgForColor = originalMsg
        String tag = msgForColor.find(/\[[A-Z]\]/)  // e.g. [E], [H], etc.
        String cleanedMsg = originalMsg.replaceFirst(/\[[A-Z]\]/, '').trim()

        // Add timestamp
        String timestamp = ""
        if (sdfPref != "None") {
            SimpleDateFormat sdf = new SimpleDateFormat(sdfPref)
            timestamp = sdf.format(dateNow)
        }

        String msgWithTime = leadingDate ? "${timestamp} ${cleanedMsg}" : "${cleanedMsg} ${timestamp}"

        // Reattach the tag temporarily for coloring
        String msgWithTag = tag ? "${tag} ${msgWithTime}".trim() : msgWithTime

        // Colorize
        notification = colorizeNotification(msgWithTag)
        //if (debugEnable) log.debug "deviceNotification entered: ${notification}"
		if (debugEnable) log.debug "Final colorized notification: ${notification}"

    

	//	insert new message at beginning	of last5 string
		msgFilled = state.msgCount.toInteger()
		String existing = device.currentValue("last5")?.replace('<span class="last5">', '')?.replace('</span>', '')?.trim()
		if (msgFilled > 0 && existing) {
            wkTile = '<span class="last5">' + notification + '<br />' + existing + '</span>'
        } else {
            wkTile = '<span class="last5">' + notification + '</span>'
        }


	//	when msg count exceeds limit, purge last message
		if (debugEnable) log.debug "deviceNotification2 msgFilled: ${msgFilled} msgLimit: ${settings.msgLimit}" 
		if (msgFilled < settings.msgLimit.toInteger())
			msgFilled++
		else
			{
			int i = wkTile.lastIndexOf('<br />');
			if (i != -1) 
				wkTile = wkTile.substring(0, i) + '</span>';
			}

	//	Ensure tile length is less than 1024 and hopefully stop loops
		int wkLen=wkTile.length()	
		while (wkLen > 1024 && msgFilled > 0)
			{
			if (debugEnable) log.debug "wkTile length ${wkLen}> 1024 truncating msgCount: ${msgFilled}"
			int i = wkTile.lastIndexOf('<br />');
			if (i != -1) 
				{
				wkTile = wkTile.substring(0, i) + '</span>';
				msgFilled--
				}
			else
				{
				wkTile='<span class="last5"></span>'
				msgFilled=0
				}
			wkLen=wkTile.length()
			if (debugEnable) log.debug "Truncated wkTile length ${wkLen}, msgCount: ${msgFilled}"
			}

	//	Update attributes and state
		sendEvent(name:"last5", value: wkTile)
		state.msgCount = msgFilled
		if (settings.create5H)
			sendEvent(name:"last5H", value: " ** "+wkTile.replaceAll("<br />"," ** ")+" ** ")
	}    

String colorizeNotification(String msg) {
    String color
    String cleanedMsg

    if (msg.startsWith("[E]")) {
        color = settings.colorE ?: "red"
        cleanedMsg = msg.replaceFirst(/\[E\]/, '').trim()
    } else if (msg.startsWith("[H]")) {
        color = settings.colorH ?: "orange"
        cleanedMsg = msg.replaceFirst(/\[H\]/, '').trim()
    } else if (msg.startsWith("[L]")) {
        color = settings.colorL ?: "goldenrod"
        cleanedMsg = msg.replaceFirst(/\[L\]/, '').trim()
    } else if (msg.startsWith("[N]")) {
        color = settings.colorN ?: "gray"
        cleanedMsg = msg.replaceFirst(/\[N\]/, '').trim()
    } else {
        color = settings.colorDefault ?: "black"
        cleanedMsg = msg
    }

    String formatted = "<span style='color:${color};'>${cleanedMsg}</span>"
    return formatted
}

void logsOff(){
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void push() {
	configure()
}
