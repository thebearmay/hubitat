metadata {
	definition (name: "Virtual Power Source with Switch", namespace: "thebearmay", author: "Jean P. May, Jr.") {
		capability "Power Source"
        capability "Switch"
	}   
}

def on() {
    sendEvent(name: "powerSource", value: "mains")
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "powerSource", value: "battery")
    sendEvent(name: "switch", value: "off")
}

def installed() {
    on()
}
