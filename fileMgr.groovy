/*
 * File Manager Device
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
 *    21Mar2022    thebearmay    take the text file methods and place them into a simple to use device driver
 *    22Mar2022    thebearmay    add listFiles command
 *    23Mar2022    thebearmay    add a temporary fileContent attribute, copyFile and fileTrimTop commands
 *    25Mar2022    thebearmay    add callback option for non-child application usage (input( "x", "capability.*"...))
 *    27Mar2022    thebearmay    allow hub w/security to be external file source
 *    08Apr2022    thebearmay    ensure correct encoding returned for extended characters
 *    03Oct2022    thebearmay    add image file capabilities
 *    04Oct2022    thebearmay    combine write methods
 *    01Nov2022    thebearmay    add file delete
 *    05Nov2022    thebearmay	 exist attribute instantiate
 *    08Dec2022    thebearmat    Fix typo in fileDelete method
*/

import java.net.URLEncoder
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

@SuppressWarnings('unused')
static String version() {return "0.2.6"}

metadata {
    definition (
        name: "File Manager Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        description: "Device (or child device) that allows string data to be save and retrieved from HE Local File Storage",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/fileMgr.groovy"
    ) {
        
      
        capability "Actuator"
        
        attribute "exist", "string"
        attribute "fileList", "string"
        attribute "fileContent", "string"
 
        command "writeFile",[[name:"fileName", type:"STRING", description:"File Manager Destination Name"],
                             [name:"writeString", type:"STRING", description:"String to Store"]
                            ]
        command "writeFileAsHex",[[name:"fileName", type:"STRING", description:"File Manager Hex Encoded Destination Name"],
                             [name:"writeString", type:"STRING", description:"String to Store"]
                            ]
        command "xferFile",[[name:"inputURL", type:"STRING", description:"Input URL"],
                            [name:"fileName", type:"STRING", description:"File Manager Destination Name"]
                           ]
        command "copyFile",[[name:"inputURL", type:"STRING", description:"File Manager Source Name"],
                            [name:"fileName", type:"STRING", description:"File Manager Destination Name"]
                           ]        
        command "appendFile",[[name:"fileName", type:"STRING", description:"File Manager Destination Name"],
                              [name:"appendString", type:"STRING", description:"String to Append"]
                             ]
        command "readFile",[[name:"fileName", type:"STRING",description:"Local File to Read"]]
        command "readFileAsHex",[[name:"fileName", type:"STRING",description:"Local Hex Encoded File to Read"]]
        command "fileExists",[[name:"fileName", type:"STRING",description:"File to Look for"]]
        command "fileTrimTop",[[name:"fileName", type:"STRING",description:"File to Trim"],
                               [name:"offset", type:"NUMBER",description:"Number of Characters to Remove from the Beginning of the File"]
                              ]               
        command "listFiles"
        command "uploadImage",[[name:"iPath", type:"STRING",title:"Path Image"],
                            [name:"oName", type:"STRING",title:"File Manager Name"]
                           ]  
        command "deleteFile", [[name:"f2Delete", type:"STRING", title: "Name of File to Delete"]]
        command "readExternalFile",[[name:"fUrl", type:"STRING", title: "Path of file to read"]]
    }
}
        

preferences {
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false)
        input("password", "password", title: "Hub Security Password", required: false)
    }
    input("debugEnabled", "bool", title: "Enable debug logging?")
    input("logResponses", "bool", title: "Log Responses to Commands")
    input("allowAttrib", "bool", title: "Allow a TEMPORARY attribute for File Contents\n<b>If you use this set Event and State History to 1</b>")
 
}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def updated(){
    if(debugEnable) {
        log.debug "updated()"
        runIn(1800,logsOff)
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
//			log.debug resp.data?.text
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

@SuppressWarnings('unused')
def fileTrimTop(fname, trimOffset, Closure closure) {
    closure(fileTrimTop(fname, trimOffset))
}

@SuppressWarnings('unused')
Boolean fileTrimTop(fname, trimOffset){
   fileData = readFile(fname)
   return writeFile(fname, fileData.substring(trimOffset.toInteger(),fileData.length()-1))
}

@SuppressWarnings('unused')
def copyFile(fnameIn, fnameOut, Closure closure) {
    closure(copyFile(fnameIn, fnameOut))
}

@SuppressWarnings('unused')
Boolean copyFile(fnameIn, fnameOut){
    fileData = readFile(fnameIn)
    return writeFile(fnameOut, fileData.substring(0,fileData.length()-1))
}

@SuppressWarnings('unused')
def fileExists(fName, Closure closure) {
    closure(fileExists(fName))
}

@SuppressWarnings('unused')
Boolean fileExists(fName){

    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri          
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                if(logResponses) log.info "File Exist: true"
                updateAttr("exist","true")
                return true;
            } else {
                if(logResponses) log.info "File Exist: true"
		updateAttr("exist","false")    
                return false
            }
        }
    } catch (exception){
        if (exception.message == "Not Found"){
            if(logResponses) log.info "File Exist: false"
        } else {
            log.error("Find file $fName) :: Connection Exception: ${exception.message}")
        }
	updateAttr("exist","false")    
        return false;
    }

}

@SuppressWarnings('unused')
def readFile(fName, Closure closure) {
    closure(readFile(fName))
}

@SuppressWarnings('unused')
String readFile(fName){
    if(security) cookie = securityLogin().cookie
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie,
                "Accept": "application/octet-stream"
            ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {       
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               }
               if(logResponses) log.info "File Read Data: $delim"
               if(allowAttrib) {
                    updateAttr("fileContent", "$delim")
                    runIn(30,"removeAttr")
                }
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
def appendFile(fName,newData,Closure closure) {
    closure(appendFile(fName,newData))
}

@SuppressWarnings('unused')
Boolean appendFile(fName,newData){
    try {
        fileData = (String) readFile(fName)
        if(fileData.length()>0) 
            fileData = fileData.substring(0,fileData.length()-1)
        else fileData = " "
        return writeFile(fName,fileData+newData)
    } catch (exception){
        if (exception.message == "Not Found"){
            writeStatus =  writeFile(fName, newData)
            if(logResponses) log.info "Append Status: $writeStatus"
            return writeStatus
        } else {
            log.error("Append $fName Exception: ${exception}")
            return false
        }
    }
}

@SuppressWarnings('unused')
def writeFileAsHex(String fName, String fData, Closure closure) {
    closure(writeFileAsHex(fName, fData))
}

@SuppressWarnings('unused')
Boolean writeFileAsHex(String fName, String fData) {
    byte[] bStr = fData.getBytes()
    if(debubEnabled) log.debug "$bStr"
    String hexStr = hubitat.helper.HexUtils.byteArrayToHexString(bStr)
    return writeFile(fName, hexStr)
}

@SuppressWarnings('unused')
def readFileAsHex(fName, Closure closure) {
    closure(readFileAsHex(fName))
}

@SuppressWarnings('unused')
String readFileAsHex(String fName) {
    String dataRet = readFile(fName)
    byte[] bStr = hubitat.helper.HexUtils.hexStringToByteArray(dataRet)
    String retStr = new String(bStr)
    if(debugEnabled) log.debug "$retStr"
    return retStr
}

@SuppressWarnings('unused')
def writeFile(String fName, String fData, Closure closure) {
    closure(writeFile(fName, fData))
}

@SuppressWarnings('unused')
Boolean writeFile(String fName, String fData) {
    byte[] fDataB = fData.getBytes("UTF-8")
    return writeImageFile(fName, fDataB, "text/html")   
}

@SuppressWarnings('unused')
def xferFile(fileIn, fileOut, Closure closure) {
    closure(xferFile(fileIn, fileOut))
}

@SuppressWarnings('unused')
Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    if(logResponses) log.info "File xFer Status: $retStat"
    return retStat
}

def readExternalFile(fName){
    updateAttr("fileContent",readExtFile(fName))
}

@SuppressWarnings('unused')
def readExtFile(fName, Closure closure) {
    closure(readExtFile(fName))
}

@SuppressWarnings('unused')
String readExtFile(fName){
    if(security) cookie = securityLogin().cookie   
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
            ]
        
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               if(logResponses) log.info "Read External File result: delim"
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

@SuppressWarnings('unused')
def listFiles(Closure closure) {
    closure(listFiles())
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
                for (rec in json.files) {
                    fileList << rec.name
                }
            } else {
                //
            }
        }
        if(debugEnabled) log.debug fileList.sort()
        updateAttr("fileList", fileList.sort())
        return fileList.sort()
    } catch (e) {
        log.error e
    }
}
@SuppressWarnings('unused')
def uploadImage(imagePath, oName){
    imageData = readImage(imagePath)
    writeImageFile(oName, imageData.iContent, imageData.iType)
}

@SuppressWarnings('unused')
String deleteFile(fName){
    bodyText = JsonOutput.toJson(name:"$fName",type:"file")
    params = [
        uri: "http://127.0.0.1:8080",
        path: "/hub/fileManager/delete",
        contentType:"text/plain",
        requestContentType:"application/json",
        body: bodyText
        ]
    httpPost(params) { resp ->
        return resp.data.toString()
    }
}

@SuppressWarnings('unused')
def readImage(Closure closure) {
    closure(readImage(imagePath))
}                             
                             
HashMap readImage(imagePath){   
    def imageData
    if(security) cookie = securityLogin().cookie   

    if(debugEnabled) log.debug "Getting Image $imagePath"
    httpGet([
        uri: "$imagePath",
        contentType: "*/*",
        headers: [
            "Cookie": cookie
        ],
        textParser: false]){ response ->
            if(debugEnabled) log.debug "${response.properties}"
            imageData = response.data 
            if(debugEnabled) log.debug "Image Size (${imageData.available()} ${response.headers['Content-Length']})"

            def bSize = imageData.available()
            def imageType = response.contentType 
            byte[] imageArr = new byte[bSize]
            imageData.read(imageArr, 0, bSize)
            if(debugEnabled) log.debug "Image size: ${imageArr.length} Type:$imageType"  
            return [iContent: imageArr, iType: imageType]
        }    
}

@SuppressWarnings('unused')
def writeImageFile(String fName, byte[] fData, String imageType, Closure closure) {
    closure(writeImageFile(fName, fData, imageType))
}                             

Boolean writeImageFile(String fName, byte[] fData, String imageType) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();
    bDataTop = """--${encodedString}\r\nContent-Disposition: form-data; name="uploadFile"; filename="${fName}"\r\nContent-Type:${imageType}\r\n\r\n""" 
    bDataBot = """\r\n\r\n--${encodedString}\r\nContent-Disposition: form-data; name="folder"\r\n\r\n--${encodedString}--"""
    byte[] bDataTopArr = bDataTop.getBytes("UTF-8")
    byte[] bDataBotArr = bDataBot.getBytes("UTF-8")
    
    ByteArrayOutputStream bDataOutputStream = new ByteArrayOutputStream();

    bDataOutputStream.write(bDataTopArr);
    bDataOutputStream.write(fData);
    bDataOutputStream.write(bDataBotArr);

    byte[] postBody = bDataOutputStream.toByteArray();  
    try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
            requestContentType: "application/octet-stream",
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString"
			], 
            body: postBody,
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
            if(debugEnabled) log.debug "writeImageFile ${resp.properties}"
            log.info "${resp.data.success} ${resp.data.status}"
            return resp.data.success == 'true' ? true:false
		}
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}

@SuppressWarnings('unused')
void removeAttr(){
    if(location.hub.firmwareVersionString >= "2.2.8.141")
        device.deleteCurrentState("fileContent")
    else
        updateAttr("fileContent", "expired")
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false",type:"bool"])
}
