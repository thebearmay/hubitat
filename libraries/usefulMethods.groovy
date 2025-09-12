
// Find namespace based on driver name - app usage only
String findNameSpace(String driverName) {
  String nSpace = ''
	this.installedDrivers.each{
    if(it.name == driverName) nSpace = it.namespace
  }
  return nSpace
}
