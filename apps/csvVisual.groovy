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
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
 *    08Jul2024        thebearmay            v0.0.2 - Remove button if chart.js already in File Manager
 *    13Jul2024                              v0.0.3 - Handle non-long timestamp
 *    15Jul2024                              v0.0.4 - handle the device column, if present
 *    16Jul2024                              v0.0.5 - Allow multiple CSVs
 */
    


static String version()	{  return '0.0.5'  }

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
             fileList = getFiles()
             state.jsInstalled = false
             csvList = []
             fileList.each {
                 if("$it" == "chart.js")
                     state.jsInstalled = true
                 else if("$it".contains('.csv'))
                     csvList.add("$it")                     
             }
             if(!state?.jsInstalled)
                 input("jsInstall","button", title:"Install ChartJS", width:4)
             input("csvFile","enum", title:"Name of Local CSV File for Visualization", options:csvList, width: 4, submitOnChange:true, multiple:true)
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

ArrayList getFiles(){
    fileList =[]
    params = [
        uri    : "http://127.0.0.1:8080",
        path   : "/hub/fileManager/json",
        headers: [
            accept : "application/json"
        ],
    ]
    httpGet(params) { resp ->
        resp.data.files.each {
            fileList.add(it.name)
        }
    }
    
    return fileList.sort()
}

String toCamelCase(init) {
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


HashMap jsonResponse(retMap){     
    return JsonOutput.toJson(retMap)
}

ArrayList csvCombine() {
    csvArr = [[]]
    fArray = []
    numCols = []
    tsCol = []
    i=0
    csvFile.each{
        fArray[i] = (new String (downloadHubFile("${csvFile[i]}"))).split("\n")
        cols=fArray[i][0].split(",")
        numCols[i] = cols.size()
        j=0
        cols.each{
            if("$it" == '\"timeStamp\"')
               tsCol[i] = j 
            j++
        }
        i++
    }
    fInx = 0
    narrInx = 0
    fArray.each { files ->
        rInx = 0
        files.each { records ->
            cols=records.split(',')
            cInx=0
            cols.each{
                if(cInx >= tsCol[fInx]){
                    if(debugEnabled)
                        log.debug "$fInx:$rInx:$cInx $it"
                    if(rInx == 0) { //Header Row
                        if (cInx >= tsCol[fInx]  && !(fInx > 0 && "$it".contains('timeStamp'))) { 
                            if(debugEnabled)
                                log.debug "Header Before: ${csvArr[0]}"
                            csvArr[0].add("$it")
                            if(debugEnabled)
                                log.debug "Header After: ${csvArr[0]}"
                        }
                    } else {
                        if(debugEnabled) 
                            log.debug "cInx: $cInx cols:${numCols[fInx]}"
                        if(cInx == tsCol[fInx]){
                            if(debugEnabled) log.debug "ts col"
                            csvArr[narrInx]=[]
                            csvArr[narrInx][0]="$it"
                            if(debugEnabled)
                                log.debug "$fInx Before"
                            for(i=0;i<fInx;i++){ //handle cols before this file
                                for(j=tsCol[fInx]+1;j<numCols[fInx];j++){
                                    if(debugEnabled)
                                        log.debug "file:$fInx cols:${numCols[fInx]} index:$j"
                                    csvArr[narrInx].add("")
                                }
                            }                        
                        } else if (cInx > tsCol[fInx] && cInx < numCols[fInx]-1){
                            if(debugEnabled)log.debug "middle"
                            if(!csvArr[narrInx]) csvArr[narrInx] = []
                            csvArr[narrInx].add("$it")
                        } else if(cInx == numCols[fInx]-1){
                            if(debugEnabled)log.debug "end"
                            csvArr[narrInx].add("$it")
                            if(debugEnabled)
                                log.debug "$fInx After"
                            for(i=fInx;i<csvFile.size();i++){
                                for(j=tsCol[i]+1;j<numCols[i];j++){
                                    if(debugEnabled)
                                        log.debug "file:$fInx cols:${numCols[fInx]} index:$j"
                                    csvArr[narrInx].add("")
                                }
                            }
                        }
                    }
                }                 
                cInx++
            }
            if(rInx > 0 || fInx == 0)
                narrInx++
            rInx++
        }
        fInx++
    }
    if (debugEnabled)
        uploadHubFile ("csvCombWork.txt",csvArr.toString().getBytes("UTF-8"))
                   
    csvWork = "${csvArr[0].toString().replace('[','').replace(']','')}\n"
    csvArr.remove(0) //
    csvArr = dSort(csvArr)
    /// replace nulls logic
    rInx = 0
    csvArr.each{ row ->
        cInx = 0
        row.each { col ->
            if("$col" == null || "$col" == ""){
                if(debugEnabled) log.debug "null found $rInx $cInx"
                if (rInx == 0){
                    for(i=rInx+1;i<csvArr.size();i++){
                        if(csvArr[i][cInx] != null){
                            csvArr[rInx][cInx] = csvArr[i][cInx]
                            break
                        }
                    }
                } else 
                    csvArr[rInx][cInx] = csvArr[rInx-1][cInx]                   
            }
            cInx++ 
        }
        if(debugEnabled) log.debug "${csvArr[rInx]}"
        rInx++
    }
    
    csvArr.each{
        if(it){
            csvWork += "${it.toString().replace('[','').replace(']','')}\n"  
        }
    }
    uploadHubFile ("csvWork${app.id}.txt",csvWork.getBytes("UTF-8"))
    return csvParse("csvWork${app.id}.txt")
}

ArrayList dSort(aLst){
    bLst=[]
    Long minLst = 0
    while(aLst.size() > 0){
        minInx = -1
        minLst = Long.MAX_VALUE
        for(i=0;i<aLst.size();i++){
            //log.debug "for:$i ${aLst[i]}"
            if(aLst[i]){
                if(debugEnabled) log.debug "$i ${aLst[i]}"
                try{
                    c1 = aLst[i][0].split(',').toString().replace('[\"','').replace('\"]','').toLong()
                    if(debugEnabled)
                        log.debug "$c1 $minLst"
                    if(c1 < minLst){
                        if (debugEnabled) log.debug "replacing $minLst with $c1"
                        minLst=c1
                        minInx=i                   
                    }
                } catch (e){
                    log.error "Invalid TimeStamp found - must be Long"
                    return null
                }
            } 
        }
        if(minInx == -1)
            break
        if(aLst[minInx])
            bLst.add(aLst[minInx])
        if(minInx < aLst.size())
            aLst.remove(minInx)
        
        if(minLst == Long.MAX_VALUE){
            log.warn "Over run break"
            break
        }
    }
    return bLst
    
}

ArrayList csvParse(fName) {
    fileRecords = (new String (downloadHubFile("${fName}"))).split("\n")
    r=0
    tsCol = -1
    devCol = -1
    fileRecords.each {
        col = it.split(",")
        if(r==0){ //initialize arrays
          i=0
          dataSet = []
          col.each{              
             if(it == '"timeStamp"')
                 tsCol = i
             else if(it == '"device"')
                 devCol = i
             if(i != devCol)
                 dataSet[i]=[]
             i++
          }      
        }
        i=0
        col.each{
            if(i != devCol){
                it = it.replace('\"','')
                try {
                    if(r > 0 && i == tsCol){
                        dataSet[i].add("\"${new Date(it.toLong()).toString()}\"")
                    } else {
                        dataSet[i].add(it)
                    }
                } catch (e) {
/*                    if(it != null)
                        if(!dataSet[i]) dataSet[i]=[]
                        dataSet[i].add("\"$it\"")
*/
                }
            }
            i++
        }
        r++
    }
    if(devCol > -1)
        dataSet.remove(devCol)
    if(debugEnabled)
        uploadHubFile ("csvWork.txt",dataSet.toString().getBytes("UTF-8"))
    return dataSet
}

String buildPage(){
    if(csvFile.size == 1)
        cols = csvParse("${csvFile[0]}")
    else {
        cols = csvCombine()
        deleteHubFile("csvWork${app.id}.txt")
    }
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

<script>${insertJS()}</script>

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
    
    if(debugEnabled)
        uploadHubFile ("pageBuildWork.txt",visualRep.toString().getBytes("UTF-8"))    
    return(visualRep)
}

def refresh(){
    visualRep = buildPage()
    contentBlock = [
        contentType: 'text/html',
        data: "$visualRep",
        gzipContent: true,
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
/*
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
*/
                return """${resp.data}"""
            }
            else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
}

String insertJS(){
    return """${new String(downloadHubFile('chart.js'))}"""
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
