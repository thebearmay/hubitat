    if (init == null)
        return null;

    String ret = ""
    List word = init.split(" ")
    if(word.size == 1)
        return init
    word.each{
        ret+=Character.toUpperCase(it.charAt(0))
        ret+=it.substring(1).toLowerCase()        
    }
    ret="${Character.toLowerCase(ret.charAt(0))}${ret.substring(1)}"

    if(debugEnabled) log.debug "toCamelCase return $ret"
    return ret;
}

def appButtonHandler(btn) {
    switch(btn) {
	case "addNote":
	    if(!custNote) break
        atomicState.meshedDeviceMsg = ""
		qryDevice.each{
            it.updateDataValue(noteName, custNote)
            if(it.controllerType == "LNK") {
                atomicState.meshedDeviceMsg+="<span style='background-color:red;font-weight:bold;color:white;'>$it is a Hub Mesh Device, note must be added to the <i>REAL</i> device to be retained</span><br>"
            }
		}
        if(atomicState.meshedDeviceMsg == "") atomicState.meshedDeviceMsg = "<span style='background-color:green;font-weight:bold;color:white;'>Update Successful</span>"
		break
	case "remNote":
		qryDevice.each{
			it.removeDataValue(noteName)
		}
		break	
    default: 
		log.error "Undefined button $btn pushed"
		break
	}
}
def intialize() {

}
