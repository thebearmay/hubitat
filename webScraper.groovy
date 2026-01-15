/*
 * Web Scraper Device
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
 *    ----         ---           ----
 *    22Mar2022    thebearmay    original code  
 *    27Mar2022    thebearmay    allow scraping of Hub UI with Security
 *    31Mar2022    thebearmay    option to show raw source
 *    27Jul2024    thebearmay    fix polling issue
 *	  10Jan2026					 Fix Offsets not carrying correctly
 *	  15Jan2026					 fix recurring logic
*/


@SuppressWarnings('unused')
static String version() {return "0.0.9"}

metadata {
    definition (
        name: "Web Scraper", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        description: "Scrapes a website looking for a search string and returns a string based on offsets from the search",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/webScraper.groovy"
    ) {
 
        capability "Actuator"
        
        attribute "successful", "STRING"
        attribute "textReturned", "STRING"
        attribute "lastURL", "STRING"
        attribute "lastSearch", "STRING"
        attribute "offsets", "STRING"
        attribute "lastRun", "STRING"
        attribute "followed", "STRING"
 
        command "scrape",[[name:"inputURL*", type:"STRING", description:"Input URL"],
                          [name:"searchStr*", type:"STRING", description:"String to look for"],
                          [name:"retBegOffset", type:"NUMBER", description: "Beginning offset from Found Item to Return Data"],
                          [name:"retEndOffset", type:"NUMBER", description: "Ending offset from Found Item to Return Data"],
                          [name:"followRed", type:"ENUM", description: "Follow Redirects", constraints: ["true", "false"]]
                         ]

    }   
}

preferences {
    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("pollRate", "number", title: "Poll Rate in minutes (0 = No Polling)", defaultValue:0)
    input("showSrc", "bool", title: "Show Raw Source", defaultValue: false, submitOnChange: true)
    
    input("siteLogin", "bool", title: "Site Requires Login", defaultValue: false, submitOnChange: true)
    if (siteLogin) { 
        input("sitePath", "string", title: "URL for site login", required: false)
        input("siteUname", "string", title: "Site Username", required: false)
        input("sitePwd", "password", title: "Site Password", required: false)
        input("siteSubmit", "string", title: "Site Submit function (normally Login):", required: false)
    }    
}

@SuppressWarnings('unused')
def installed() {

}
@SuppressWarnings('unused')
def updateAttr(String aKey, aValue, String aUnit = ""){
    if(aValue.length() > 1024) aValue = aValue.substring(0,1023)
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}


void updated(){
    if(debugEnabled) {
        log.debug "updated()"
        runIn(1800,"logsOff")
    } 
    if(pollRate > 0) 
        runIn(pollRate*60, "scrape")
    else
        unschedule("scrape")
}

void scrape() {
    offset = device.currentValue("offsets", true).replace('[','').replace(']','').split(',')
    scrape(device.currentValue('lastURL'),device.currentValue('lastSearch'),offset[0].toInteger(),offset[1].toInteger(),device.currentValue('followed'))
}

void scrape (url, searchStr, beg, end, follow='false'){
    beg=beg.toInteger()
    end=end.toInteger()
    if(debugEnabled) log.debug "$url, /$searchStr/, $beg, $end"
    updateAttr("lastURL", url)
    updateAttr("lastSearch",searchStr)
    ofList = [beg, end]
    updateAttr("offsets", ofList.toString())
    updateAttr("successful","running")
    updateAttr("textReturned","null")
    updateAttr("followed","$follow")
    dataRet = readExtFile(url, follow).toString()
    if(debugEnabled) "${dataRet.length()} characters returned"
    if(showSrc) {
        writeFile("scrapeWork.txt","$dataRet")
        updateAttr("temporary", "<script> window.open('http://${location.hub.localIP}:8080/local/scrapeWork.txt')</script>")
        runIn(5,"wipeFile")
    }
    found = dataRet.indexOf(searchStr)
    updateAttr("lastRun", new Date().toString())
    if(found == -1) {
        updateAttr("successful","false")
        updateAttr("textReturned","null")
        if(pollRate > 0) 
            runIn(pollRate*60, "scrape")
        if(debugEnabled) log.deubg "Not Found"
        return
    }
    updateAttr("successful", "true")
    int begin = found+beg
    int ending = found+end
    updateAttr("textReturned",dataRet.substring(begin,ending))
    if(debugEnabled) "Found at $found"
    if(pollRate > 0) 
        runIn(pollRate*60, "scrape")
    return
                   
}

String readExtFile(fName, follow){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        followRedirects: follow,            
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                       char c =  (char) i
                       delim+=c
                       i = resp.data.read()
                       if(i < 0 || i > 255) return delim
               } 
               if(debugEnabled) log.info "Read External File result: delim"
               return delim.toString()
            }
            else {
                log.error "Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();    
    
try {
		def params = [
			uri: "http://${location.hub.localIP}:8080",
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString"
			],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}
              
@SuppressWarnings('unused')
void wipeFile(){              
        writeFile("scrapeWork.txt","")
}
              
@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
