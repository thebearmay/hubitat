/*
 * Attribute Gauge Chart
 * 
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
 *    20Jul2024        thebearmay            v0.0.1 - Original code
 */
    


static String version()	{  return '0.0.1'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field


definition (
	name: 			"Attribute Gauge Chart", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"App to show how ChartJS could be used to create gauge chart for an attribute",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/attrGauge.groovy",
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
         section(name:"gOpt",title:"Gauge Options", hideable: true, hidden: false){
             input("clearOptions","button",title:"Clear Gauge Options", width:4)
             if(state.clearReq) {
                 state.clearReq = false
                 settings.each {
                     if(it.toString().contains('band') || it.toString().contains('Band')){
                         log.debug "${it.key}"
                        app.removeSetting("${it.key}")
                     }
                 }
             }
             input("numBands","number", title:"Number of Bands", width:4, submitOnChange:true)
             for(i=0;i<numBands;i++){
                 input("band${i}Max","number",title:"Maximum value for band ${i+1}", width:4, submitOnChange:true)
                 input("band${i}Color","enum",title:"Color for band ${i+1}", options:bandColor, width:4, submitOnChange:true)
             }
         }            
         section(name:"visualData",title:"Meta Data", hideable: false, hidden: false){
             fileList = getFiles()
             state.jsInstalled = false
             fileList.each {
                 if("$it" == "chart.js")
                     state.jsInstalled = true                    
             }
             if(!state?.jsInstalled)
                 input("jsInstall","button", title:"Install ChartJS", width:4)
             input("selectedDev","capability.*", title:"Selected Device", width: 4, submitOnChange:true, multiple:false)
             if(selectedDev) {
                 input("attrSelect", "enum", title:"Attribute to Chart", options:getAttr(), width: 4)
                 href("pageRender", title:"Render Visualization", width:4)
             }
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

ArrayList getAttr(){
    attrList = []
    selectedDev.supportedAttributes.each{
        attrList.add(it.toString())
    }
    return attrList.sort()
    
}



def appButtonHandler(btn) {
    switch(btn) {
        case "jsInstall":
            fetchJS()
            state.jsInstalled = true
            break
        case "clearOptions":
            state.clearReq = true
            break
        default: 
              log.error "Undefined button $btn pushed"
              break
    }
}


HashMap jsonResponse(retMap){     
    return JsonOutput.toJson(retMap)
}

String buildPage(){
    ArrayList cList = []
    ArrayList wList = []
    settings.each{
        if(it.toString().contains('Color'))
            cList.add("\'${it.value}\'")
        else if (it.toString().contains('Max'))
            wList.add("${it.value}")                      
    }
    String visualRep = """<div id='container' style='padding:0;height:vh;width:vw;border:inset;border-radius:15px;background-color:#FCF6E9'>
	<div id='cTitle' style='text-align:center;font-weight:bold;padding:0;font-size:10px'></div>
	<div>
		<canvas id="myChart"></canvas>
	</div>
	<div id='cValue' style='text-align:center;font-weight:bold;padding:0;;font-size:8px'></div>
</div>

<script>
${insertJS()}
</script>

<script>
var bandColors = $cList;
var bandWidths = maxToWidths($wList);
var nValue = ${selectedDev.currentValue(attrSelect)};
var title = '${selectedDev}:${attrSelect}';

canvas = document.getElementById('myChart');
document.getElementById('cValue').style.width = canvas.width;
document.getElementById('cValue').innerHTML = nValue+'${selectedDev.currentState(attrSelect)?.unit}';
document.getElementById('cTitle').style.width = canvas.width;
document.getElementById('cTitle').innerHTML = title;
document.getElementById('container').style.width = canvas.width;
document.getElementById('container').style.height = canvas.height+18;

new Chart('myChart', config());


function config(){ 
	return {
	  type: 'doughnut',
	  plugins: [{
		afterDraw: chart => {
		  var needleValue = chart.config.data.datasets[0].needleValue;
		  var dataTotal = chart.config.data.datasets[0].data.reduce((a, b) => a + b, 0);
		  var angle = Math.PI + (1 / dataTotal * needleValue * Math.PI);
		  var ctx = chart.ctx;
		  var cw = chart.canvas.offsetWidth;
		  var ch = chart.canvas.offsetHeight;
		  var cx = cw / 2;
		  var cy = ch - 6;

		  ctx.translate(cx, cy);
		  ctx.rotate(angle);
		  ctx.beginPath();
		  ctx.moveTo(0, -3);
		  ctx.lineTo(ch - 20, 0);
		  ctx.lineTo(0, 3);
		  ctx.fillStyle = 'rgb(0, 0, 0)';
		  ctx.fill();
		  ctx.rotate(-angle);
		  ctx.translate(-cx, -cy);
		  ctx.beginPath();
		  ctx.arc(cx, cy, 5, 0, Math.PI * 2);
		  ctx.fill();
		}
	  }],
	  data: {
		labels: [],
		datasets: [{
		  data: bandWidths,
		  needleValue: nValue,
		  backgroundColor: bandColors
		}]
	  },
	  options: {
		responsive: false,
		aspectRatio: 2,
		layout: {
		  padding: {
			bottom: 3
		  }
		},
		rotation: -90,
		cutout: '30%',
		circumference: 180,
		legend: {
		  display: false
		},
		animation: {
		  animateRotate: false,
		  animateScale: true
		},
	  }
	};
}

function maxToWidths(maxVals){
	wArr = [maxVals[0]];
	ml = maxVals.length;
	for (i=1;i<ml;i++){
		wArr.push(maxVals[i]-maxVals[i-1])
	}	
	return wArr;
}
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

String cBlock(cStr) {
    if(cStr !='yellow')
        return "<span style='display:block;width:12em;background-color:$cStr;color:white'>$cStr</span>"
    else
        return "<span style='display:block;width:12em;background-color:$cStr;color:black'>$cStr</span>"
}

@Field static ArrayList bandColor = ['violet','indigo','blue','green','yellow','orange','red']
