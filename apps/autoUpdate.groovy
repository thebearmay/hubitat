/*
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
*/
@SuppressWarnings('unused')
static String version() {return "0.0.1"}

definition(
    name: "Auto Update",
    namespace: "community",
    author: "thebearmay AI",
    description: "Checks for Hubitat platform updates and automatically applies them if available.",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/autoUpdate.groovy",    
    category: "Utility",
    installOnOpen:  true,
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Automated Platform Updater Configuration", install: true, uninstall: true) {
        section("Polling Configuration") {
            input "termsAccepted","bool",title: "<b>I agree to the <a href='https://hubitat.com/terms-of-service'>Hubitat Terms of Service</a></b>"
            input "pollInterval", "enum", title: "Check for updates every:", options: ["Every Hour", "Every 3 Hours", "Every 6 Hours", "Every 12 Hours", "Daily"], defaultValue: "Daily", required: true, width: 4
        	input "delayedUpdate","time", title:"Schedule update at this time (leave blank for immediately)", width:4

            
        }
        section("Logging") {
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
    }
}

def installed() {
    log.info "Automated Platform Updater Installed"
    initialize()
}

def updated() {
    log.info "Automated Platform Updater Updated"
    unschedule()
    initialize()
}

def initialize() {
    // Schedule the update check based on user preference
	switch(pollInterval) {
        case "Every Hour": 
            schedule("0 0 * ? * * *", checkForUpdates) // Top of every hour
            break
        case "Every 3 Hours": 
            schedule("0 0 */3 ? * * *", checkForUpdates) // Every 3 hours
            break
        case "Every 6 Hours": 
            schedule("0 0 */6 ? * * *", checkForUpdates) // Every 6 hours
            break
        case "Every 12 Hours": 
            schedule("0 0 */12 ? * * *", checkForUpdates) // Every 12 hours
            break
        case "Daily": 
            schedule("0 0 2 ? * * *", checkForUpdates) // Daily at 2:00 AM
            break
        default: 
            schedule("0 0 2 ? * * *", checkForUpdates)
    }    
    // Run an initial check 10 seconds after setup/modification
    runIn(10, checkForUpdates)
}

def checkForUpdates() {
    if (logEnable) log.debug "Checking for Hubitat platform updates..."
    
    def params = [
        uri: "http://127.0.0.1:8080/hub/cloud/checkForUpdate",
        requestContentType: 'application/json',
        contentType: 'application/json',
        timeout: 20
    ]
    
    try {
        asynchttpGet('handleUpdateCheckResponse', params)
    } catch (Exception e) {
        log.error "Failed to initiate update check: ${e.message}"
    }
}

def handleUpdateCheckResponse(response, data) {
    if (response.hasError()) {
        log.error "Error checking for updates: ${response.getErrorMessage()} (Status: ${response.getStatus()})"
        return
    }
    
    try {
        def json = response.getJson()
        if (logEnable) log.debug "Update check response: ${json}"
        
        if (json && json.status != "NO_UPDATE_AVAILABLE") {
            log.warn "New update detected (Status: ${json.status}). Initiating platform update..."
            delayUpdate()
            //triggerPlatformUpdate()
        } else {
            if (logEnable) log.info "Hubitat platform is up to date."
        }
    } catch (Exception e) {
        log.error "Failed to parse update check response JSON: ${e.message}"
    }
}

void delayUpdate(){
    if(!delayedUpdate) 
    	triggerPlatformUpdate()
	dTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", delayedUpdate).getTime()
    tNow = new Date().getTime()
    if (dTime < tNow) {
        dTime += 86400000
    }
    secDelay = (dTime - tNow)/1000
	runIn(secDelay, "triggerPlatformUpdate")  
}

def triggerPlatformUpdate() {
    if(!termsAccepted) {
        log.error "Update available but terms have not been accepted"
		return
    }
    def params = [
        uri: "http://127.0.0.1:8080/hub/cloud/updatePlatform",
        requestContentType: 'application/json',
        contentType: 'application/json',
        timeout: 20
    ]
    
    try {
        asynchttpGet('handleUpdateTriggerResponse', params)
    } catch (Exception e) {
        log.error "Failed to send update platform command: ${e.message}"
    }
}

def handleUpdateTriggerResponse(response, data) {
    if (response.hasError()) {
        log.error "Error triggering platform update: ${response.getErrorMessage()} (Status: ${response.getStatus()})"
    } else {
        log.info "Platform update successfully triggered! The hub will now download the update and reboot."
    }
}
