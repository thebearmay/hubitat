/*
 * File Manager Backup 
 *
 */
import java.util.zip.*
import java.util.zip.ZipOutputStream    
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat

static String version()	{  return '1.0.0' }

definition (
	name: 			"File Manager Backup & Restore", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Logic Check .",
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
            section("<h3>Change Application Name</h3>", hideable: true, hidden: true){
               input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
               if(nameOverride != app.getLabel) app.updateLabel(nameOverride)
            }            
        }
    }
}

def backupFM(){
    dynamicPage (name: "backupFM", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3>Backup Management</h3>"){
            input "excludeHgz", "bool", title: "Exclude .hgz Files", defaultValue: true, submitOnChange: true, width:4
            input "reqBackup", "button", title: "Create Backup File", width:4
            if(state?.createHgz){
                createBackup()
                state.createHgz = false
            }
            
        }
    }
}

def restoreFM(){
    dynamicPage (name: "restoreFM", title: "<h2>File Manager Backup & Restore</h2><p style='font-size:small'>v${version()}</p>", install: false, uninstall: false) {
        section("<h3>Restore Data</h3>"){

            hgzList=[]
            fList = listFiles()
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
                input "reqRestore", "button", title: "<br>Restore from Backup", width:2
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

void createBackup(){
    fList = listFiles()
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


void restoreBackup(restFile, fList){
    byte[] rData = downloadHubFile("$restFile")
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
    Byte[] newByte = new Byte [rData.size()-(foundIt+1)]
    j=0
    for(i=foundIt+1;i<(rData.size());i++){
        newByte[j]=rData[i]
        j++
    }
    fTest = unzip(newByte)
    if(debugEnabled)log.debug "${fTest[132611..132704]}"
   
    fStr = fList.toString()
    fHeader.each {
        if(it.fName != ">>>fEntryEnd>>>" && (fStr.contains("${it.fName}") || fStr.contains("All"))){
            if(debugEnabled)log.debug "${it.fName} ${it.fStart.toInteger()} ${it.fEnd.toInteger()}"
            
            fLength = it.fEnd.toInteger()+1 - it.fStart.toInteger()
            fOut = new Byte[fLength]
            for(i=it.fStart.toInteger();i<fLength+it.fStart.toInteger();i++){
                j=i-it.fStart.toInteger()
                fOut[j]=fTest[i]
                if(debugEnabled)log.debug "${fOut[j]} ${fTest[i]} $i $j"
            }
            if(debugEnabled)log.debug "$fOut"
            bOut = new ByteArrayOutputStream();
            bOut.write(fOut)
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
List<String> listFiles(){
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
        httpGet(params) { resp ->
            if (resp != null){
                if(logEnable) log.debug "Found the files"
                def json = resp.data
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
        return fileList.sort()
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
        if (exception.message == "Not Found"){
            if(debugEnabled) log.debug("File DOES NOT Exists for $fName)");
        } else {
            log.error("Find file $fName) :: Connection Exception: ${exception.message}");
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
        default: 
            log.error "Undefined button $btn pushed"
            break
    }
}               
