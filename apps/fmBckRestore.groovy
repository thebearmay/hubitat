/*
 * File Manager Backup 
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
 *    27Jan2023    thebearmay    v1.1.0 Add Backup and Backup Purge scheduling
 *                               v1.1.1 Add a backup and download option
 *    28Jan2023                  v1.1.2 Create Location Event when backup taken/removed
 *                                        Retention purge error fix
 *    31Jan2023                  v1.2.0 Rewrite restore logic to reduce time
 *    02Feb2023                  v1.3.0 Add download endpoint
 *    05Feb2023                  v1.3.1 Add Content Length to download endpoint
 */
import java.util.zip.*
import java.util.zip.ZipOutputStream    
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '1.3.1' }

definition (
	name: 			"File Manager Backup & Restore", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"File Manager Backup & Restore - provides a facility to create and manage the retention of backups for the File Manager.\n  Also permits single/multi-file recover from a backup as well as a full retore.",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/fmBckRestore.groovy",
    installOnOpen:  true,
	oauth: 			false,
    singleThreaded: true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "backupFM"
    page name: "restoreFM"
}

mappings {
    path("/latest") {
        action: [GET: "remBackup"]
    }
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
}

void logsOff(){
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
            section("<h3>Main</h3>"){
                
                href "backupFM", title: "File Manager Backup", required: false
                href "restoreFM", title: "File Manager Restore", required: false
                input "debugEnabled", "bool", title: "Turn on Debug Logging"
                input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
                if (security) { 
                    input("username", "string", title: "Hub Security Username", required: false, submitOnChange: true)
                    input("password", "password", title: "Hub Security Password", required: false, submitOnChange: true)
                    login = securityLogin()
                    paragraph "Login successful: ${login.result}\n${login.cookie}"
                }
            }
            section("<h3>Backup Endpoint Information</h3>", hideable:true, hidden:true){
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Backup Endpoint:</b> ${getFullLocalApiServerUrl()}/latest?access_token=${state.accessToken}"
                input "resetToken", "button", title:"Reset Token"
                if(resetReq){
                    resetReq = false
                    createAccessToken()
                }               
            }
            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
            }            
        }
    }
}

def backupFM(){
    dynamicPage (name: "backupFM", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3>Backup Management</h3>"){
            input "excludeHgz", "bool", title: "Exclude .hgz Files", defaultValue: true, submitOnChange: true, width:4
            input "dwnldBackup", "button", title: "<span style='color:white;font-weight:bold;'>Create and Download Backup</span>", width:3, backgroundColor:"green"
            if(state?.download){              
                createBackup()
                pauseExecution(3000)
                latest = getLatest()
                if(debugEnabled) log.debug "$latest"
                s2 = "<script type='text/javascript'>const anchor = document.createElement('a');anchor.href ='${location.hub.localIP}:8080/local}';anchor.download = '$latest';document.body.appendChild(anchor);anchor.click();document.body.removeChild(anchor);location.reload();</script>"
                paragraph s2
                state.download = false
            }
            input "reqBackup", "button", title: "Create Backup File", width:2
            if(state?.createHgz){
                createBackup()
                state.createHgz = false
            }
            input "cleanNow", "button", title: "Check Retention", width:2
            if(state?.runPurge) {
                backupPurge()
                state.runPurge = false
            }

          
        }
        section(title:"<h3>Frequency Management</h3>", hideable: true, hidden: true){
            input "autoEnabled", "bool",title: "Automatic Backup Enabled", defaultValue: false, width:4
            input "backupFreq", "enum", title: "Backup Frequency", options: [["86400":"Daily"], ["604800":"Weekly"], ["-1":"Monthly"]], width:4
            input "backupTime", "time", title: "Time for Backup", width:4
            input "firstBackup", "date", title: "Date of First Backup", width:4
            numDays = ["Always"]
            for(i=0;i<31;i++){
                numDays.add(i)
            }
            input "retDays", "enum", title: "Days to Retain Backups", options:numDays, width:4
            input "retTime", "time", title: "Time to Purge Backups", width:4
            input "freqSave","button", title: "<span style='color:white;font-weight:bold;'>Save</span>",width:4, backgroundColor:"red"
            if(autoEnabled) 
                subNextBackup()
            else
                unschedule("createBackup")
            unschedule("backupPurge")
            if(retDays != null && retDays != "Always" && retTime != null) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
                rTime = sdf.parse(retTime)
                hrs = rTime.getHours()
                mins = rTime.getMinutes()
                schedule("0 $mins $hrs ? * * *", "backupPurge")
            }            
        }        
    }
}

void subNextBackup() {
    if(!autoEnabled) return
    if(backupTime == null || backupFreq == null || backupTime == null) {
        log.error "Backup Scheduling information is incomplete"
        return
    }
    sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
    sdf2 = new SimpleDateFormat("yyyy-MM-dd")
    tNow = new Date()
    tPart = backupTime.substring(backupTime.indexOf("T")+1,backupTime.indexOf("T")+9)
    if(tNow.before(sdf.parse("${firstBackup}T${tPart}"))){  //First run
        long nextDate = sdf.parse("${firstBackup}T${tPart}").getTime()
        long secs = ((nextDate - new Date().getTime())/1000).toInteger()+1
        runIn(secs,"createBackup")
    } else if(backupFreq != -1){ //old run calc from current date - daily or weekly 
        wDate = new Date(new Date().getTime()+(backupFreq.toInteger()*1000))
        dayPart = sdf2.format(wDate)
        long nextDate = sdf.parse("${dayPart}T${tPart}").getTime()
        long secs = ((nextDate - new Date().getTime())/1000).toInteger()+1
        runIn(secs,"createBackup")
    } else { //old run calc from current date - monthly
        wDate = new Date()
        MM = wDate.getMonth()
        MM++
        if(MM > 12) MM=1
        YYYY = wDate.getYear()
        DD = wDate.getDate()
        dayPart = sdf2.format(new Date(YYYY,MM,DD))
        long nextDate = sdf.parse("${dayPart}T${tPart}").getTime()
        long secs = ((nextDate - new Date().getTime())/1000).toInteger()+1
        runIn(secs,"createBackup")        
    }  
}

void backupPurge() {
    if(retDays == null || retDays == 'Always')
        return
    secondsBack = retDays.toInteger()*86400*1000
    purgeDate = (long) (new Date().getTime() - secondsBack)
    fList = listFiles('json').jStr
    i=0
    for (rec in fList.files) {
        if(rec.name.contains(".hgz")){
            if(debugEnabled) 
                log.debug "${rec.name} Purge Date: $purgeDate File Date:${rec.date}"
            if(rec.date.toLong() <= purgeDate.toLong()){
                if(debugEnabled)
                    log.debug "file delete ${rec.name}"
                deleteHubFile("${rec.name.trim()}")
                i++
            }
        }
    }
    sendLocationEvent(name:"fmBackup", value:"cleanup", descriptionText:"FM Backup removed $i file(s) based on retention settings", type:"USER")
}

String getLatest() {
    fList = listFiles('json').jStr
    String latest = ''
    long dCompare = 0
    for(rec in fList.files){
        if(rec.name.contains(".hgz")){
            if(debubEnabled) 
                log.debug "${rec.name} $dCompare ${rec.date}"
            if(rec.date.toLong() > dCompare){
                latest = rec.name
                dCompare = rec.date.toLong()
            }
        }
    }
    return latest
}

def restoreFM(){
    dynamicPage (name: "restoreFM", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3>Restore Data</h3>"){

            hgzList=[]
            fList = listFiles().fList
            if(!fList.toString().contains("$rFile")){
                rFile = "No Selection"
                rFileList = ""
                app.updateSetting("rFile",[value:"No Selection",type:"enum"])
                app.updateSetting("rFileList", [value:"", type:"enum"])
                state.restoreHgz = false
            }
            
            fList.each{
                if("$it".contains(".hgz"))
                   hgzList.add("$it")
            }
            input "rFile", "enum", options:hgzList, title:"Select the backup to restore", width:6, submitOnChange:true
            if(rFile != null && rFile != 'No Selection'){
                String[] zFiles = readHeader("$rFile")
                input "rFileList", "enum", options:zFiles, title:"Select Files to Restore", width:6, submitOnChange: true, multiple:true
            }
            input "noOverWrite", "bool", title: "Do not overwrite existing files", submitOnChange: true
            if(rFile != null && rFile != "No Selection" && rFileList != null){   
                input "reqRestore", "button", title: "Restore from Backup", width:2
                if(state?.restoreHgz){
                    if(rFile != null && rFile != "No Selection" && rFileList != null) restoreBackup("$rFile",rFileList)
                    rFile = "No Selection"
                    rFileList = ""
                    app.updateSetting("rFile",[value:"No Selection",type:"enum"])
                    app.updateSetting("rFileList", [value:"", type:"enum"])
                    state.restoreHgz = false
                }
            }
                
        }
    }
}

void restoreBackup(restFile, fList){
    long rStart = new Date().getTime()
    byte[] rData = downloadHubFile("$restFile")
    long rEnd = new Date().getTime()
    if(debugEnabled) log.debug "Read time ${(rEnd-rStart)/1000} seconds"
    String rFile = new String(new String(rData))
    i = 0
    for(;i<rData.size();i++){
        if(rData[i] == (byte)']') break
    }
    foundIt = i
   //read header
    fHeaderStr = new String(rData, "UTF-8").substring(0,foundIt+1)
    def jSlurp = new JsonSlurper()
    fHeader = jSlurp.parseText(fHeaderStr)
    if(debugEnabled) log.debug "$fHeader"
    rEnd2 = new Date().getTime()  
    if(debugEnabled) log.debug "Read Header ${(rEnd2-rEnd)/1000} seconds"      
  
    newByte = new ByteArrayOutputStream();
    j=0
    for(i=foundIt+1;i<(rData.size());i++){
        newByte.write(rData[i])
    }
                      
    fTest = unzip(newByte.toByteArray())

    rEnd3 = new Date().getTime()
    if(debugEnabled) log.debug "Unzip ${(rEnd3-rEnd2)/1000} seconds"    
    fStr = fList.toString()
    fHeader.each {
        if(it.fName != ">>>fEntryEnd>>>" && (fStr.contains("${it.fName}") || fStr.contains("All"))){
            if(debugEnabled)log.debug "${it.fName} ${it.fStart.toInteger()} ${it.fEnd.toInteger()}"
            fLength = it.fEnd.toInteger()+1 - it.fStart.toInteger()
            
            bOut = new ByteArrayOutputStream();
            for(i=it.fStart.toInteger();i<fLength+it.fStart.toInteger();i++){
                bOut.write(fTest[i])
            }

            fOut2 = bOut.toByteArray()
            if(debugEnabled)log.debug "$fOut2"
            
            if(noOverWrite && fileExists("${it.fName}")){
               tNow = new Date().getTime().toString()
               it.fName = it.fName.replace(".","_$tNow.")
            }

            uploadHubFile("${it.fName}",fOut2)
            if(debugEnabled)log.debug "${it.fName}"
            
        }
    }
    rEnd4 = new Date().getTime()
    if(debugEnabled) log.debug "Restore time ${(rEnd4-rEnd3)/1000} seconds, Total time ${(rEnd4-rEnd)/1000} seconds "
}

def remBackup(){
    createBackup()
    pauseExecution(3000)
    latestBkup = getLatest()
    fData = downloadHubFile("$latestBkup")
    contentBlock = [
        contentDisposition: "attachment; fileName:$latestBkup", 
        contentType: "application/octet-stream", 
        data:fData,
        contentLength:fData.size()
    ]
    
    render(contentBlock)

}

void createBackup(){
    fList = listFiles().fList
    if(debugEnabled)log.debug "$fList"
    
    i=0
    fTable=[]
    ByteArrayOutputStream bOutD = new ByteArrayOutputStream();
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    fList.each{
        if((excludeHgz && !"$it".contains(".hgz")) || !excludeHgz){
            zfile = downloadHubFile("$it")
            fEntry=[fName:"$it", fStart:i, fEnd:(zfile.size()-1+i)]
            fTable.add(fEntry)
            i+=zfile.size()
            if(debugEnabled)log.debug "$fEntry ${zfile[0..10]}"
            bOutD.write(zfile)
        }
    }          
    fEntry=[fName:">>>fEntryEnd>>>"]
    fTable.add(fEntry)          
    jStr = (String) JsonOutput.toJson(fTable)
          
    jByte= "$jStr".getBytes("UTF-8")
    bOut.write(jByte)
          
    byte[] bData = bOutD.toByteArray()
    zipfile = zip(bData)
    bOut.write(zipfile)
    fData = bOut.toByteArray()
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss")
    datePart = sdf.format(new Date())
    
    zFileName = "${toCamelCase("fmb ${location.name}")}_${datePart}.hgz"
    uploadHubFile("$zFileName",fData) 
    if(autoEnabled) 
        subNextBackup()
    else
        unschedule("createBackup")
    sendLocationEvent(name:"fmBackup", value:"created", descriptionText:"$zFileName FM Backup created", type:"USER")
}

String[] readHeader(restFile) {
    byte[] rData = downloadHubFile("$restFile")
    String rFile = new String(new String(rData))
    i = 0
    for(;i<rData.size();i++){
        if(rData[i] == (byte)']') break
    }
    foundIt = i

    fHeaderStr = new String(rData, "UTF-8").substring(0,foundIt+1)
    def jSlurp = new JsonSlurper()
    fHeader = jSlurp.parseText(fHeaderStr)
    fList = ["All"]
    fHeader.each{
        if(it.fName != ">>>fEntryEnd>>>"){
            fList.add("${it.fName}")
        }        
    }
    return fList
}

static byte[] zip(byte[] uncompressedData) {
    ByteArrayOutputStream bos = null
    GZIPOutputStream gzipOS = null
    try {
        bos = new ByteArrayOutputStream(uncompressedData.length)
        gzipOS = new GZIPOutputStream(bos)
        gzipOS.write(uncompressedData)
        gzipOS.close()
        return bos.toByteArray()

    } catch (IOException e) {
            e.printStackTrace()
    }
    finally {
        try {
            gzipOS.close()
            bos.close()
        } catch (Exception ignored) {}
    }
    return null;
}

byte[] unzip(byte[] compressedData) {
    ByteArrayInputStream bis = null
    ByteArrayOutputStream bos = null
    GZIPInputStream gzipIS = null

    bis = new ByteArrayInputStream(compressedData)
            
    bos = new ByteArrayOutputStream()
    gzipIS = new GZIPInputStream(bis)

    byte[] buffer = new byte[1024]
    int len;
    while((len = gzipIS.read(buffer)) != -1){
        bos.write(buffer, 0, len)
    }
    return bos.toByteArray()
    try {
        gzipIS.close()
        bos.close()
        bis.close()
    }
    catch (Exception e) {
        e.printStackTrace()
    }
}

String toCamelCase(init) {
    if (init == null)
        return null;
    init = init.replaceAll("[^a-zA-Z0-9]+","")
    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

@SuppressWarnings('unused')
HashMap listFiles(retType='nameOnly'){
    if(security) cookie = securityLogin().cookie
    if(debugEnabled) log.debug "Getting list of files"
    uri = "http://${location.hub.localIP}:8080/hub/fileManager/json";
    def params = [
        uri: uri,
        headers: [
				"Cookie": cookie
            ]        
    ]
    try {
        fileList = []
        json = ''
        httpGet(params) { resp ->
            if (resp != null){
                if(logEnable) log.debug "Found the files"
                json = resp.data
                if(debugEnabled) log.debug "$json"
                for (rec in json.files) {
                    if(rec.type == 'file')
                        fileList << rec.name.trim()
                }
            } else {
                //
            }
        }
        if(debugEnabled) log.debug fileList.sort()
        if(retType == 'json') 
            return [jStr: json]
        else
            return [fList: fileList.sort()]
    } catch (e) {
        log.error e
    }
}

@SuppressWarnings('unused')
HashMap securityLogin(){
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
	return [result: result, cookie: cookie]
}

Boolean fileExists(fName){

    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                return true;
            } else {
                return false;
            }
        }
    } catch (exception){
        if (exception.message.toLowerCase().contains("not found")){
            if(debugEnabled) log.debug "File DOES NOT Exists for $fName"
        } else {
            log.error "Find file $fName :: Connection Exception: ${exception.message}"
        }
        return false;
    }

}


def appButtonHandler(btn) {
    switch(btn) {
        case "reqBackup":
            state.createHgz = true
            break
        case "reqRestore":
            state.restoreHgz = true
            break
        case "freqSave":
            break
        case "dwnldBackup":
            state.download = true
            break
        case "cleanNow":
            state.runPurge = true
            break
        case "resetToken":
            state.resetReq = true
            break
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}               
