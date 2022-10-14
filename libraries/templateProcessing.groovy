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
    if (fContents == null) return
    List fRecs=fContents.split("\n")
    String html = ""
    fRecs.each {
        int vCount = it.count("<%")
        if(vCount > 0){
            recSplit = it.split("<%")
            recSplit.each {
                if(it.indexOf("%>") == -1)
                    html+= it
                else {
                    vEnd = it.indexOf("%>")
                    if(it.indexOf(":") > -1 && it.indexOf(":") < vEnd){
                        devId = it.substring(0,it.indexOf(":")).toLong()
                        qryDevice.each{
                            if(it.deviceId == devId) dev=it
                        }
                        vName = it.substring(it.indexOf(":")+1,it.indexOf('%>'))
                        //log.debug "$devId $vName"
                    } else
                        vName = it.substring(0,it.indexOf('%>'))

                    if(vName == "@date")
                        aVal = new Date()
                    else if (vName == "@version")
                        aVal = version()
                    else if (vName == "@name" && dev != null)// requires a format of <%devId:attribute%>
                        aVal = dev.properties.displayName
                    else if (vName == "@room" && dev != null)
                        aVal = dev.properties.roomName
                    else if(dev != null) {
                        aVal = dev.currentValue("$vName",true)
                        String attrUnit = dev.currentState("vName")?.unit
                        if (attrUnit != null) aVal+=" $attrUnit"
                    }
                    html+= aVal
                    if(it.indexOf("%>")+2 != it.length()) {
                        html+=it.substring(it.indexOf("%>")+2)
                    }
                }                 
            }
        }
        else html += it
    } 
    return html

}
