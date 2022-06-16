 /*

 */
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.0"}

metadata {
    definition (
        name: "Find Hub IP", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/findHubIP.groovy"
    ) {
        
        
        capability "Actuator"
        attribute "hubList", "string"
        attribute "ipList", "string"
        attribute "status", "string"

        command "findIP"

    }   
}

preferences {


}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def findIP(){
    netBase=location.hub.localIP
    bp=netBase.lastIndexOf(".")
    netBase=netBase.substring(0,bp+1)
    ipList = []
    updateAttr("ipList", "[]")
    updateAttr("status","running")
    updateAttr("hubList","[]")
    for(int i=2; i<254; i++){
        serverAddr="$netBase$i"
        hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(serverAddr, 1)
        pingDataS = pingData.toString()
        pLoss = pingDataS.substring(pingDataS.lastIndexOf(":")+1,pingDataS.length()-1).toInteger()
        if(pLoss == 0) {
            ipList.add(serverAddr)
            checkForHub(serverAddr)
        }
        if(i%20==0) updateAttr("status", "running..$i")
    }
    updateAttr("status", "complete")
    updateAttr("ipList", ipList)
 
}

def checkForHub(ipAddr){
    Map params = [
        uri    : "http://$ipAddr:8080/hub2/hubData"
    ]
    if (debugEnable) log.debug params
    asynchttpGet("getHubData", params, [ip:ipAddr])
}

void getHubData(resp, data){
    try{
        if (resp.getStatus() == 200){        
            def jSlurp = new JsonSlurper()
            Map h2Data = (Map)jSlurp.parseText((String)resp.data)
            work=device.currentValue("hubList",true)
            work=work.substring(0,work.length()-1)
            if(work.length() > 1 && h2Data.name != null)
            work+=", $h2Data.name:${data['ip']}]"
            else
                work+="$h2Data.name:${data['ip']}]"
            updateAttr("hubList",work)
        }
    } catch (Exception ex){
        if (!warnSuppress) log.warn ex
    }
}               
