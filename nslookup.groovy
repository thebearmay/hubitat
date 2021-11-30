 /*
 *  Nslookup
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
 */
import groovy.json.JsonSlurper

@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "Nslookup", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/nslookup.groovy"
    ) {
        
      
        capability "Actuator"
        attribute "lookupResp","STRING"
        attribute "lookupRespList","STRING"
        command "nslookup",[[name:"dnsName",type:"STRING",description:"DNS Lookup Name"],[name:"dnsServer",type:"STRING",description:"DNS Server (default: 8.8.8.8)"]]
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


void nslookup(dnsName, dnsServer="8.8.8.8") {

    Map params = [
        uri: "https://$dnsServer/resolve?name=$dnsName&type=A",
        contentType: "text/plain",
        timeout: 10
    ]

    asynchttpGet("nsCallback", params)

}

String nsCallback(resp, data) {
    
    def jSlurp = new JsonSlurper()
    Map ipData = (Map)jSlurp.parseText((String)resp.data)
    updateAttr("lookupResp", ipData.Answer.data[0])
    updateAttr("lookupRespList", ipData.Answer.data)
    return ipData.Answer.data[0]
}
