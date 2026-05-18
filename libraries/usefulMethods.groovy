
// Find namespace based on driver name - works in both apps and drivers

String findNameSpace(String driverName) {
	String nSpace = ''
	this.installedDrivers.each{
    	if(it.name == driverName) nSpace = it.namespace
  	}
	return nSpace
}

Boolean fileExists(fName) {
    Boolean fExist = false
    this.hubFiles.each{
        if(it.name == fName)
        	fExist = true
    }
    return fExist
}



this.definitionData -> the fields in the app defintion
this.hubFiles -> list of files with size, date, id, type[file,dir] (both apps and drivers)
this.installedBundlesList -> list of of bundles with id,name,namespace
this.installedDrivers
this.rooms 
this.TTSVoices -> name,gender,language
this.wiFiNetworks
