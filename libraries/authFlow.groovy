library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "authorization",
    description: "Authorization flow methods.",
    name: "authFlow",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/authFlow.groovy",
    version: "0.0.1",
    documentationLink: ""
)

/// first call
def getSecret(userName, password){
   salt = app.id
   userSecret = “$userName:$password:$salt”.bytes.encodeBase64().toString()
   return [status:”200”, secret:”$userSecret”]
}

// request token with secret
def firstToken(secret){
   	decodeCheck = secret.decodeBase64()
   	if(!decodeCheck.endsWith(“${app.id})
    	return [status:“403”]
   	tokenSplit = decodeCheck.split(“:”)
   	return refreshToken(tokenSplit)
 }  	
 
 // refresh token 	
 def refreshToken(tokenSplit)
 	Long now = new Date().getTime()
    Long refreshSeconds = 10800 //3 hours
   	token = “${tokenSplit[1]}:${tokenSplit[2]}:$refreshTime:${app.id}”.bytes.encodeBase64().toString()
   	refreshToken = “${tokenSplit[1]}:${tokenSplit[2]}:$refresh:$now:${app.id}”.bytes.encodeBase64().toString()
	return [status:"200",access_token:"$token",refresh_token:"refreshToken"]
}	

// normal token check
def tokenCheck(tokenRec){  
   	decodeCheck = tokenRec.decodeBase64()
   	Long now = new Date().getTime()
   	if(!decodeCheck.endsWith(“${app.id})
    	return [status:“403”]// invalid app
	tokenSplit = decodeCheck.split(“:”)
	if(tokenSplit[3] == ‘refresh’
		return refreshToken(tokenSplit)
	if(tokenSplit[3].toLong() < now)
		return [status:”401”]// time expired
	return [status:"200"]
}
