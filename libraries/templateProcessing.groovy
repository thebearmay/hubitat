library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "localFileUtilities",
    description: "Template processing routine",
    name: "templateProcessing",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/templateProcessing.groovy",
    version: "0.0.1",
    documentationLink: ""
)

String genHtml(templateName) {
    String fContents = readFile("$templateName")
    List fRecs=fContents.split("\n")
    if(fContents == 'null' || fContents == null) {
        xferFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/hubInfoTemplate.res","hubInfoTemplate.res")
        device.updateSetting("alternateHtml",[value:"hubInfoTemplate.res", type:"string"]) 
        fContents = readFile("$alternateHtml")
    }
    String html = ""
    fRecs.each {
        int vCount = it.count("<%")
        if(debugEnable) log.debug "variables found: $vCount"
        if(vCount > 0){
            recSplit = it.split("<%")
            if(debugEnable) log.debug "$recSplit"
            recSplit.each {
                if(it.indexOf("%>") == -1)
                    html+= it
                else {
                    vName = it.substring(0,it.indexOf('%>'))
                    if(debugEnable) log.debug "${it.indexOf("%>")}<br>$it<br>${it.substring(0,it.indexOf("%>"))}"
                    if(vName == "date()" || vName == "@date")
                        aVal = new Date()
                    else if (vName == "@version")
                        aVal = version()
                    else {
                        aVal = device.currentValue("$vName",true)
                        String attrUnit = device.currentState("$vName")?.unit
                        if (attrUnit != null) aVal+=" $attrUnit"
                    }
                    html+= aVal
                    if(it.indexOf("%>")+2 != it.length()) {
                        if(debugEnable) log.debug "${it.substring(it.indexOf("%>")+2)}"
                        html+=it.substring(it.indexOf("%>")+2)
                    }
                }                 
            }
        }
        else html += it
    }
    if (debugEnable) log.debug html
    return html

}
