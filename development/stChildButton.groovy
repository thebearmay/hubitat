/**
 *  Child Button
 *
 *  Copyright 2017 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Ported from ST thebearmay
 */
metadata {
	definition (name: "ST Child Button", namespace: "hubitat", author: "SmartThings", ocfDeviceType: "x.com.st.d.remotecontroller") {
		capability "PushableButton"
		capability "HoldableButton"
		capability "Sensor"
        
        attribute "supportedButtonValues", "ENUM"
        attribute "button", "string"
        attribute "numberOfButtons", "number"
	}

}

def installed() {
	sendEvent(name: "numberOfButtons", value: 1)
}

def push(buttonNumber){
    sendEvent(name: "pushed", value:buttonNumber)
}

def hold(buttonNumber){
    sendEvent(name:"held", value:buttonNumber)
}
