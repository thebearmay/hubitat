 /*
 * Dashboard Variable Virtual Device
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
 *    Date        Who            What
 *    ----        ---            ----
 *    2020-12-28  thebearmay	 Original version 0.1.0, reversed engineered from the RM Hubitat driver
 *    2021-03-10  thebearmay	 Use Capability.Variable
 * 
 */

static String version()	{  return '0.2.0'  }

metadata {
    definition (
		name: "Dashboard Variable Device", 
		namespace: "thebearmay", 
		author: "Jean P. May, Jr.",
	        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/dashVariable.groovy"
	) {
        	capability "Actuator"
	        capability "Variable"
		

//This one works in webCoRE without UI error message
    command "setVariableAlt", [[name:"variable", type:"STRING", description:"Both commands store the same variable.  Dashboard tile type applies constraints at the dashboard."]]   
            
    }   
}

preferences {
	input("debugEnable", "bool", title: "Enable debug logging?")
}

def installed() {
	log.trace "installed()"
    setVariable("installed")
}

def updated(){
	log.trace "updated()"
	if(debugEnable) runIn(1800,logsOff)
}

def setVariable(varStr) {
    if(debugEnable) log.debug "setVariable() $varStr"
    sendEvent(name:"variable", value:varStr)
}

def setVariableAlt(varStr) {
    if(debugEnable) log.debug "setVarALt() $varStr"
    setVariable(varStr)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
