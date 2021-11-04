Boolean installBundle(bundle) {
    //from the github make sure that the url ends with "?raw=true"
    if(security) cookie = securityLogin().cookie
    //adapted from BPTWorld code 
    try {
        def params = [
            uri: 'http://127.0.0.1:8080/bundle/uploadZipFromUrl',
            headers: [
                "Accept": '/',
                "ContentType": 'text/html; charset=utf-8',
                "Cookie": cookie
            ],
            body: bundle,
            timeout: 30,
            ignoreSSLIssues: true
        ]
        httpPost(params) { resp ->
            log.debug "In installBundleHandler - Receiving file: ${bundle}"
            log.debug "In installBundleHandler - Hopefully installing Bundle"
        }
        return true
    } catch (e) {
        log.error(getExceptionMessageWithLine(e))
        return false
    }
}
