/* Doesn't work as a library - Server Error 500 copy and paste the methods

library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "localFileUtilities",
    description: "Local File Access Methods",
    name: "localFileMethods",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/localFileMethods.groovy",
    version: "0.0.5",
    documentationLink: ""
)
*/
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
            log.debug("File DOES NOT Exists for $fName)");
        } else {
            log.error("Find file $fName) :: Connection Exception: ${exception.message}");
        }
        return false;
    }

}

String readFile(fName){
    if(security) cookie = securityLogin().cookie
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie
            ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {       
              // return resp.data
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
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

Boolean appendFile(fName,newData){
    try {
        fileData = (String) readFile(fName)
        fileData = fileData.substring(0,fileData.length()-1)
        return writeFile(fName,fileData+newData)
    } catch (exception){
        if (exception.message == "Not Found"){
            return writeFile(fName, newData)      
        } else {
            log.error("Append $fName Exception: ${exception}")
            return false
        }
    }
}

Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();    
    updateAttr("debug",encodedString)
    
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
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

Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    return retStat
}

String readExtFile(fName){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
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
