/*
 * Hub Information Display
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
 *    23Mar2025        thebearmay            v0.0.1 - Original code
 */
    


static String version()	{  return '0.0.1'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
#include thebearmay.uiInputElements


definition (
	name: 			"Hub Information Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"App to show details collected by the Hub Information Driver v3",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubInfoDisp.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "pageRender"
    page name: "configPage"

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
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def configPage(){
    dynamicPage (name: "configPage", title: "", install: true, uninstall: true) {
        section (name:'cPageHndl', title:'Configuration Page'){
            paragraph getInputElemStr(name:'debugEnabled', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'Debug Enabled')
	        if(state.accessToken == null) createAccessToken()
    	    apiSection = getInputElemStr(name:'api', type:'divHide', width:'15em', radius:'12px', background:'#e6ffff', title:'API Information', divName:'apiSection', hidden:true)
        	String pStr = "<div id='apiSection' ${divStyle}><p><b>Local Server API:</b> ${getFullLocalApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Cloud Server API: </b>${getFullApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Access Token: </b>${state.accessToken}</p></div>"

        	paragraph "${apiSection}${pStr}"
            fileList = getFiles()
            state.jsInstalled = false
            fileList.each {
			if("$it" == "chart.js")
				state.jsInstalled = true                    
            }
            if(!state?.jsInstalled)
             	paragraph getInputElemStr(name:'jsInstall', type:'button', width:'15em', radius:'12px', background:'#e6ffff', title:'Install ChartJS')
            paragraph getInputElemStr(name:'selectedDev', type:'capability.*', width:'15em', radius:'12px', background:'#e6ffff', title:'Hub Info Device')
            if(selectedDev) {
                state.configured = true
				paragraph getInputElemStr(name:"pRender", type:'href', title:"Render Visualization", destPage:'pageRender', width:'15em', radius:'12px', background:'#e6ffff')
             } else
                state.configured = false
            	
            
        }

         section("Change Application Name", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
         }
    }
}

def pageRender(){
    dynamicPage (name: "pageRender", title: "", install: false, uninstall: false) { 
        section(name:"visualDisp",title:"", hideable: false, hidden: false){
            if(!state.configured)
            	configPage()
            else {
            	paragraph "${buildPage()}"
            }
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
    String headDiv = getInputElemStr(name:'headDiv', type:'divHide', width:'15em', radius:'12px', background:'#e6ffff', title:'Information Charts', divName:'chartSection', hidden:false)
    String c1 = buildChart([attrSelect:'cpuPct',cList:['\"#0efb1c\"','\"#fdf503\"','\"#fd0a03\"'],wList:[8,20,100],i:0])
    String c2 = buildChart([attrSelect:'freeMemory',cList:['\"#fd0a03\"','\"#fdf503\"','\"#0efb1c\"'],wList:[100,200,2000], scale:1000, i:1])
    String lat = selectedDev.currentValue('latitude')
    String lon = selectedDev.currentValue('longitude')
    String ifrm = "<iframe style='height:370px;padding:0;margin:0;border-radius:15px' src='https://embed.windy.com/embed.html?type=map&location=coordinates&metricRain=default&metricTemp=default&metricWind=default&zoom=5&overlay=radar&product=ecmwf&level=surface&lat=${lat}&lon=${lon}' data-fs='false' onload='(() => {const body = this.contentDocument.body;const start = () => {if(this.dataset.fs == 'false') {this.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';this.dataset.fs = 'true';} else {this.style = 'width: 100%; height: 100%;';this.dataset.fs = 'false';}}body.addEventListener('dblclick', start);})()'></iframe>"
    String aToF = getAttr('a'..'f')
    String gToP = getAttr('g'..'p')
    String qToZ = getAttr('q'..'z')
    String aToFdivB = getInputElemStr(name:'aToFdiv', type:'divHide', width:'10em', radius:'12px', background:'#e6ffff', title:'A-F', divName:'aToFdiv', hidden:true)
    String gToPdivB = getInputElemStr(name:'gToPdiv', type:'divHide', width:'10em', radius:'12px', background:'#e6ffff', title:'G-P', divName:'gToPdiv', hidden:true)
    String qToZdivB = getInputElemStr(name:'qToZdiv', type:'divHide', width:'10em', radius:'12px', background:'#e6ffff', title:'Q-Z', divName:'qToZdiv', hidden:true)
	String pContent = "<table><tr><td>${headDiv}</td><td>${aToFdivB}</td><td>${gToPdivB}</td><td>${qToZdivB}</td></tr></table>"
    pContent+= "<table id='chartSection', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;height:371px;'><tr><td style='height:371px'>${c1}</td><td>>&nbsp;</td><td style='height:371px'>${c2}</td><td>&nbsp;</td><td style='vertical-align:top;padding:0;margin:0'>${ifrm}</td></tr></table>"
    pContent += "<div id='aToFdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${aToF}</div>"
    pContent += "<div id='gToPdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${gToP}</div>"
    pContent += "<div id='qToZdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${qToZ}</div>"
    return pContent
}

String getAttr(aRange){
    ArrayList attrList = []
    selectedDev.supportedAttributes.each{
        String attr = it.toString()
        if(aRange.contains(attr.substring(0,1))){
        	tMap = [key:"${it}", value:"${selectedDev.currentValue("$it")}"]
        	attrList.add(tMap)
        }
    }
    attrSort = attrList.sort{ it.key }
    String retVal = '<table>'
    i=0
    attrSort.each{
        if(i==0) {
        	retVal += "<tr><td style='font-size:small;font-weight:bold;'>${it.key}:</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${it.value}</td><td>&nbsp;</td>"
        	i = 1
        } else {
            retVal += "<td style='font-size:small;font-weight:bold'>${it.key}:</td><td style='min-width:10em;overflow-wrap:anywhere;'>${it.value}</td></tr>"
			i=0
        }
    } 
    if(i==1)
    	retVal += "</tr>"
    retVal+="</table>"
    return retVal
    
}
String buildChart(opts) {
    ArrayList cList = opts.cList
    ArrayList wList = opts.wList
    attrSelect = opts.attrSelect
    if(!opts.scale) opts.scale = 1
    i=opts.i
    valueScaled = selectedDev.currentValue(attrSelect)/opts.scale
    String visualRep = """<div id='container${i}' style='padding:0;position:relative;margin:0;height:vh;width:vw;border:inset;border-radius:15px;background-color:#e6ffff'>
	<p id='cTitle${i}' style='text-align:center;font-weight:bold;margin:0;padding:0;font-size:10px;height:12px'></p>
	<canvas id="myChart${i}"></canvas>
	<p id='cValue${i}' style='text-align:center;font-weight:bold;margin:0;padding:0;font-size:8px,height:10px'></p>
	</div>

<script>
${insertJS()}
</script>

<script>
var bandColors = $cList;
var bandWidths = maxToWidths($wList);
var nValue = ${valueScaled};
var title = '${attrSelect}';

canvas = document.getElementById('myChart${i}');
document.getElementById('cValue${i}').style.width = canvas.width+'px';
document.getElementById('cValue${i}').innerHTML = nValue+'${selectedDev.currentState(attrSelect)?.unit}';
document.getElementById('cTitle${i}').style.width = canvas.width+'px';
document.getElementById('cTitle${i}').innerHTML = title;
document.getElementById('container${i}').style.width = canvas.width+'px';
//document.getElementById('container${i}').style.height = (canvas.height+18)+'px';

new Chart('myChart${i}', config());


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
@Field static String divStyle = 'style=\"padding:0;margin:0;background-color:#e6ffff;border-radius:12px;\"'
@Field static ArrayList bandColor = ['violet','indigo','blue','green','yellow','orange','red']
