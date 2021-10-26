library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "localFileUtilities",
    description: "Local File Access Methods",
    name: "localFileMethods",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/localFileMethods.groovy",
    version: "0.0.1",
    documentationLink: ""
)

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
    uri = "http://${location.hub.localIP}:8080/local/${fName}"

    def params = [
        uri: uri,
        contentType: "text/html; charset=UTF-8"
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               data = resp.getData();           
               return data.toString()
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



void appendFile(fName,newData){

    uri = "http://${location.hub.localIP}:8080/local/${fName}"

    def params = [
        uri: uri,
        contentType: "text/html; charset=UTF-8"
    ]

    try {
        httpGet(params) { resp ->
           //File exists and is good
            fileData = resp.getData().toString();
            fileData = fileData.substring(0,fileData.length()-1)
            writeFile(fName,fileData+newData)

        }
    } catch (exception){
        if (exception.message == "Not Found"){
            writeFile(fName, newData)      
        } else {
             log.error("Append $fName :: Connection Exception: ${exception}");
        }
    }
}

Boolean writeFile(fName, fData) {
    if(security) {
        httpPost(
                    [
                        uri: "http://127.0.0.1:8080",
                        path: "/login",
                        query: [ loginRedirect: "/" ],
                        body: [
                            username: username,
                            password: password,
                            submit: "Login"
                        ]
                    ]
                ) { resp -> cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0) }
    } 
	try
	{
		def params = [
			uri: "http://127.0.0.1:8080",
			path: "/hub/fileManager/upload",
			query: [
				"folder": "/"
			],
			headers: [
				"Cookie": cookie,
				"Content-Type": "multipart/form-data; boundary=----WebKitFormBoundaryDtoO2QfPwfhTjOuS"
			],
			body: """------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="folder"


------WebKitFormBoundaryDtoO2QfPwfhTjOuS--""",
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
