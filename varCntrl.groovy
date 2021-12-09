 /*
 *  Variable Controller Device 
 *  This is an intermediary device to allow programatic get/set of hub variables 
 */


@SuppressWarnings('unused')
static String version() {return "0.0.1"}

metadata {
    definition (
        name: "Variable Controller Device", 
        namespace: "thebearmay", 
        author: "Jean P. May, Jr.",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/varCntrl.groovy"
    ) {
        
      
        capability "Actuator"

        attribute "varName","string"
        attribute "varValue","string"
        attribute "varCmd", "string"
        attribute "varReturn", "string"

        command "varControl", [[name:"vName*",type:"STRING"],[name:"vCmd*",type:"ENUM",constraints:["get","set"]],[name:"vValue",type:"STRING"]]
        command "varReturn",[[name:"returnVal",type:"STRING"]] 
    }   
}

preferences {

}

@SuppressWarnings('unused')
def installed() {

}
void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit,isStateChange:true)
}


def varControl(vName,vCmd,vValue=" ",vType=" "){
    updateAttr("varName",vName.trim())
    updateAttr("varCmd",vCmd.trim())
    updateAttr("varValue",vValue.trim())
}

def varReturn(rVal) {
    updateAttr("varReturn",rVal)
}
