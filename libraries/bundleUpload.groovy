Boolean installBundle(bundle) {
    //from the github make sure that the url ends with "?raw=true"
    if(security) cookie = securityLogin().cookie
    //adapted from BPTWorld code 
    def jsonData =  JsonOutput.toJson([url:"$bundle",installer:FALSE, pwd:''])
    try {
        def params = [
            uri: 'http://127.0.0.1:8080/bundle/uploadZipFromUrl',
            headers: [
                "Accept": '*/*',
                "ContentType": 'text/plain; charset=utf-8',
                "Cookie": cookie
            ],
            body: "$jsonData",
            timeout: 30,
            ignoreSSLIssues: true
        ]
        log.debug "In installBundleHandler - Getting data ($params)"
        httpPost(params) { resp ->
             log.debug "In installBundleHandler - Receiving file: ${bundle}"
        }
   } catch (e) {
        log.error(getExceptionMessageWithLine(e))
   }
}
