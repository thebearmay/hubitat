/*
 * CSV Visualization 
 *  Description: Demostration of utilizing ChartJS to create graphs using CSV files from the Device Attribute Iterative Storage app 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
    


static String version()	{  return '0.0.1'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field


definition (
	name: 			"CSV Visualization", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Demonstraion app to show how ChartJS could be used to create graphs using CSV files from Device Attribute Iterative Storage app",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/csvVisual.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "pageRender"

}
mappings {
    path("/refresh") {
        action: [POST: "refresh",
                 GET: "refresh"]
    }
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
      	if (app.getInstallationState() == 'COMPLETE') {   
         section(name:"lhi",title:"API Information", hideable: true, hidden: true){
             
                paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}/refresh?access_token=${state.accessToken}"
                paragraph "<b>Cloud Server API: </b>${getFullApiServerUrl()}/refresh?access_token=${state.accessToken}"
                if(state.accessToken == null) createAccessToken()
                paragraph "<b>Access Token: </b>${state.accessToken}"
         }
         section(name:"visualData",title:"Meta Data", hideable: false, hidden: false){
             if(!state?.jsInstalled)
                 input("jsInstall","button", title:"Install ChartJS", width:4)
             input("csvFile","string", title:"Name of Local CSV File for Visualization", width: 4, submitOnChange:true)
             input("chartType","enum", title:"Type of Chart to Render", width: 4, options: cOptions, submitOnChange:true)
             href("pageRender", title:"Render Visualization", width:4)
         }
         section("Change Application Name", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
         }
	    } else {
		    section("") {
			    paragraph title: "Click Done", "Please click Done to install app before continuing"
		    }
	    }
    }
}

def pageRender(){
    dynamicPage (name: "pageRender", title: "", install: false, uninstall: false) { 
        section(name:"visualDisp",title:"", hideable: false, hidden: false){
            paragraph "${buildPage()}"
        }
    }
}

def toCamelCase(init) {
    if (init == null)
        return null;
    init = init.replaceAll("[^a-zA-Z0-9]+","")
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
        case "jsInstall":
            fetchJS()
            state.jsInstalled = true
            break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}


def jsonResponse(retMap){     
    return JsonOutput.toJson(retMap)
}

def csvParse(fName) {
    fileRecords = (new String (downloadHubFile("${fName}"))).split("\n")
    r=0
    tsCol = -1
    fileRecords.each {
        col = it.split(",")
        if(r==0){ //initialize arrays
          i=0
          dataSet = []
          col.each{
             dataSet[i]=[]
             if(it == '"timeStamp"')
                 tsCol = i
             i++
          }      
        }
        i=0
        col.each{
            it = it.replace('\"','')
            if(r > 0 && i == tsCol){
                dataSet[i].add("\"${new Date(it.toLong()).toString()}\"")
            } else
                dataSet[i].add(it)
            i++
        }
        r++
    }
    return dataSet
}

def buildPage(){
    cols = csvParse("$csvFile")
    labelData = []
    lCol=-1
    lr=0
    cols.each {
        if(it[0] == "timeStamp"){
            lCol = lr
            j=0
            it.each{
                if(j>0)
                    labelData.add("$it")
                j++
            }
        }
        lr++
    }
    if(!labelData) labelData="[]"
    i=0
    k=-1
    dataPart=""
    cols.each{ 
        if(k > 12) k=0
        if(i != lCol) {
            cData = []
            j=0
            dLabel = it[0]
            it.each{
                if(j> 0){
                    cData.add("$it")
                }
                j++
            }
            if(i > 1) dataPart+=",\n"
            dataPart+=                    
 """
      {
        label: "$dLabel",
        data: $cData,
        borderWidth: 2,
borderColor: "${lineColor[k]}",
		fill:{
			target:true
		}
      }
"""
            j++
        }
        i++
        k++
    }
    String visualRep = """
<div style='height:vh;width:vw'>
  <canvas id="myChart"></canvas>
</div>

<script src="/local/chart.js"></script>

<script>
  ctx = document.getElementById('myChart');
  new Chart(ctx, {
    type: "$chartType",
    data: {
      labels: $labelData,
      datasets: [
        $dataPart
      ]
    },
    options: {
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
</script>
"""
    return(visualRep)
}

def refresh(){
    visualRep = buildPage()
    contentBlock = [
        contentType: 'text/html',
        data: "$visualRep",
        status:200
    ]        

    render(contentBlock)

}

String readExtFile(fName){  
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true  
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               if(debugEnable) log.info "Read External File result: delim"
               return delim
            }
            else {
                log.error "Read External - Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

def fetchJS(){
    jsFile = readExtFile("https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/chart.js")
    if(jsFile){
        bArray = (jsFile.getBytes("UTF-8"))
	    uploadHubFile("chart.js",bArray)
    } else
        log.error "chart.js not found"
}

@Field static ArrayList cOptions = ['bar','line','pie','doughnut','radar','polarArea']
@Field static ArrayList lineColor = ["#ff0000","#0000ff","#00ff00","#000f0f","#0f000f","#0f0f00","#0f0f0f","#0000cc","#cc0000","#00cc00","#000c0c","#0c000c","#0c0c00"]
