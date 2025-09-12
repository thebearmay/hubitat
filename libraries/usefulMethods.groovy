
// Find namespace based on driver name - works in both apps and drivers

String findNameSpace(String driverName) {
  String nSpace = ''
	this.installedDrivers.each{
    if(it.name == driverName) nSpace = it.namespace
  }
  return nSpace
}
