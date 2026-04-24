
// Find namespace based on driver name - works in both apps and drivers

String findNameSpace(String driverName) {
  String nSpace = ''
	this.installedDrivers.each{
    if(it.name == driverName) nSpace = it.namespace
  }
  return nSpace
}

this.definitionData -> the fields in the app defintion
this.hubFiles -> list of files with size, date, etc. (both apps and drivers)
