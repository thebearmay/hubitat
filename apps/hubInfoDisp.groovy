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
 *	  04Apr2025								 v0.1.0 - Beta Ready Code
 */
    


static String version()	{  return '0.1.0'  }

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
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/xxx.groovy",
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
/*    path("/refresh") {
        action: [POST: "refresh",
                 GET: "refresh"]
    }
*/
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
    dynamicPage (name: "configPage", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:'cPageHndl', title:'Configuration Page'){
            db = getInputElemStr(name:'debugEnabled', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Debug Enabled</b>')
/*	        if(state.accessToken == null) createAccessToken()
    	    apiSection = getInputElemStr(name:'api', type:'divHide', width:'15em', radius:'12px', background:'#669999', title:'API Information', divName:'apiSection', hidden:true)
        	String pStr = "<div id='apiSection' ${divStyle}><p><b>Local Server API:</b> ${getFullLocalApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Cloud Server API: </b>${getFullApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Access Token: </b>${state.accessToken}</p></div>"

        	paragraph "${apiSection}${pStr}"
*/
            fileList = getFiles()
            state.jsInstalled = false
            fileList.each {
			if("$it" == "chart.js")
				state.jsInstalled = true                    
            }
            if(!state?.jsInstalled)
             	paragraph getInputElemStr(name:'jsInstall', type:'button', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Install ChartJS</b>')
            String sDev = getInputElemStr(name:'selectedDev', type:'capability.*', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Select Hub Info Device</b>')
			String aRename = getInputElemStr(name:"nameOverride", type:"text", title: "<b>New Name for Application</b>", multiple: false, defaultValue: app.getLabel(), width:'15em', radius:'12px', background:'#e6ffff')
            paragraph "<table><tr><td style='min-width:15em'>${db}</td><td style='position:relative;top:0px;padding:0px;margin:0px'>${aRename}</td></tr><tr><td>${sDev}</td></tr></table>"
            if(selectedDev) {
                state.configured = true
                ci = btnIcon(name:'computer', size:'14px')
                paragraph getInputElemStr(name:"pRender", type:'href', title:"${ci} Hub Information", destPage:'pageRender', width:'10em', radius:'15px', background:'#669999')
            } else
                state.configured = false
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
        }
    }
}

def pageRender(){
    dynamicPage (name: "pageRender", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: false, uninstall: false) { 
        section(name:"visualDisp",title:"", hideable: false, hidden: false){
            if(!state.configured)
            	configPage()
            else {
                //log.debug "${state.lastLoad} : ${new Date().getTime() - (5*60*1000)}  ${state.lastLoad < (new Date().getTime() - (5*60*1000))}" 
                if(state.lastLoad) {
                    tNow = new Date().getTime()
                    if(state.lastLoad < (tNow - (5*60*1000))){
                        settings.each {
                            if(it.key != 'debugEnabled' && it.key != 'selectedDev' && it.key != 'nameOverride')
                            	app.removeSetting("${it.key}")
                        }
                    }
                }
                state.lastLoad = new Date().getTime()
            	paragraph "${buildPage()}"
                if(state.hiRefresh) {
                    state.hiRefresh = false
                    settings.each {
						if(it.key != 'debugEnabled' && it.key != 'selectedDev' && it.key != 'nameOverride')
							app.removeSetting("${it.key}")
					}
                    selectedDev.refresh()
                    paragraph "<script>window.location.reload();</script>"
                }
                if(state.saveBasic){
                    state.saveBasic = false
                    saveBaseData()
                }
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
        case 'getRefresh':
        	state.hiRefresh = true
        	break
        case 'saveBasic':
        	state.saveBasic = true
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
    HashMap hubDataMap = getHubJson()
    String basicData = buildBase(hubDataMap)    
    String c1 = buildChart([attrSelect:'cpuPct',cList:['\"#0efb1c\"','\"#fdf503\"','\"#fd0a03\"'],wList:[8,20,100],i:0])
    String c2 = buildChart([attrSelect:'freeMemory',cList:['\"#fd0a03\"','\"#fdf503\"','\"#0efb1c\"'],wList:[100,200,2000], scale:1000, i:1])
    String lat = selectedDev.currentValue('latitude')
    String lon = selectedDev.currentValue('longitude')
    String ifrm = "<iframe style='height:370px;padding:0;margin:0;border-radius:15px' src='https://embed.windy.com/embed.html?type=map&location=coordinates&metricRain=default&metricTemp=default&metricWind=default&zoom=5&overlay=radar&product=ecmwf&level=surface&lat=${lat}&lon=${lon}' data-fs='false' onload='(() => {const body = this.contentDocument.body;const start = () => {if(this.dataset.fs == 'false') {this.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';this.dataset.fs = 'true';} else {this.style = 'width: 100%; height: 100%;';this.dataset.fs = 'false';}}body.addEventListener('dblclick', start);})()'></iframe>"
    String aToF = getAttr('a'..'f')
    String gToP = getAttr('g'..'p')
    String qToZ = getAttr('q'..'z')
    
    String headDiv = getInputElemStr(name:'headDiv', type:'divHide', width:'5em', radius:'12px', background:'#669999', title:'Charts', divName:'chartSection', hidden:false)
    String basicDivB = getInputElemStr(name:'basicDiv', type:'divHide', width:'5em', radius:'12px', background:'#669999', title:'Basic', divName:'basicDiv', hidden:false)
    String aToFdivB = getInputElemStr(name:'aToFdiv', type:'divHide', width:'5em', radius:'12px', background:'#669999', title:'A-F', divName:'aToFdiv', hidden:true)
    String gToPdivB = getInputElemStr(name:'gToPdiv', type:'divHide', width:'5em', radius:'12px', background:'#669999', title:'G-P', divName:'gToPdiv', hidden:true)
    String qToZdivB = getInputElemStr(name:'qToZdiv', type:'divHide', width:'5em', radius:'12px', background:'#669999', title:'Q-Z', divName:'qToZdiv', hidden:true)
    String hiRefreshBtn = getInputElemStr(name:'getRefresh', type:'button', width:'5em', radius:'12px', background:'#669999', title:'Refresh')
    String cPage = getInputElemStr(name:'cPage',type:'href', radius:'15px', width:'2em',title:"<i class='material-icons app-column-info-icon' style='font-size: 24px; color:#669999;'>settings_applications</i>", destPage:'configPage')
    
    String pContent = "<table><tr><td>${headDiv}</td><td>${basicDivB}</td><td>${aToFdivB}</td><td>${gToPdivB}</td><td>${qToZdivB}</td><td style='min-width:5em'>&nbsp;</td>"
    pContent += "<td>${hiRefreshBtn}</td><td>${cPage}</td></tr></table>"
    pContent+= "<table id='chartSection', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;height:371px;'><tr><td style='height:371px'>${c1}</td><td>&nbsp;</td><td style='height:371px'>${c2}</td><td>&nbsp;</td><td style='vertical-align:top;padding:0;margin:0'>${ifrm}</td></tr></table>"//<td style='vertical-align:top;padding:0;margin:0'>${ifrm2}</td>
	pContent += genClockWidget(hubDataMap.timeFormat)
    pContent += "<div id='basicDiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${basicData}</div>"   
    pContent += "<div id='aToFdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${aToF}</div>"
    pContent += "<div id='gToPdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${gToP}</div>"
    pContent += "<div id='qToZdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${qToZ}</div>"
    pContent += "<table><tr><td>${headDiv}</td><td>${basicDivB}</td><td>${aToFdivB}</td><td>${gToPdivB}</td><td>${qToZdivB}</td><td style='min-width:5em'>&nbsp;</td>"
    pContent += "<td>${hiRefreshBtn}</td><td>${cPage}</td></tr></table>"
	
    return pContent
}

String buildBase(hubDataMap){
	String tVal = "${hubDataMap.mdnsName}"
    if(settings['mdnsName']) tVal = settings['mdnsName']
    String mDNS = getInputElemStr( [name:"mdnsName", type:"text", title:"<span style='font-size:small;font-weight:bold;'>mDNS Name</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}"])
	tVal = "${hubDataMap.hubName}"
    if(settings['hubName']) tVal = settings['hubName']
    String hubName = getInputElemStr( [name:"hubName", type:"text", title:"<span style='font-size:small;font-weight:bold;'>Hub Name</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}"])
    tVal = "${hubDataMap.tempScale}"
    if(settings['temperatureScale']) tVal = settings['temperatureScale']
    String tempScale = getInputElemStr( [name:"temperatureScale", type:"enum", title:"<span style='font-size:small;font-weight:bold;'>Temperature Scale</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}",options:["F","C"]])  
    
    tVal = "${hubDataMap.zipCode}"
    if(settings['zipCode']) tVal = settings['zipCode']
    String zipCode = getInputElemStr( [name:"zipCode", type:"text", title:"<span style='font-size:small;font-weight:bold;'>Postal Code</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}"])
    tVal = "${hubDataMap.latitude}"
    if(settings['latitude']) tVal = settings['latitude']
    String latitude = getInputElemStr( [name:"latitude", type:"text", title:"<span style='font-size:small;font-weight:bold;'>Latitude</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}"])
    tVal = "${hubDataMap.longitude}"
    if(settings['longitude']) tVal = settings['longitude']
    String longitude = getInputElemStr( [name:"longitude", type:"text", title:"<span style='font-size:small;font-weight:bold;'>Longitude</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}"])

    tVal = "${hubDataMap.timeFormat}"
    if(settings['clock']) tVal = settings['clock']
    String timeFormat = getInputElemStr( [name:"clock", type:"enum", title:"<span style='font-size:small;font-weight:bold;'>Time Format</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}",options:["12","24"]])  
    tVal = "${hubDataMap.timeZone}"
    if(settings['timeZone']) tVal = settings['timeZone']
    tzOpt = []
    hubDataMap.timeZones.each{
        tzOpt.add(["${it.id}":"${it.label}"])
    }
    String timeZone = getInputElemStr( [name:"timeZone", type:"enum", title:"<span style='font-size:small;font-weight:bold;'>Time Zone</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}",options:tzOpt])  
    tVal = "${hubDataMap.ttsCurrent}"
    if(settings['voice']) tVal = settings['voice']
    ttsOpt = []
    hubDataMap.ttsVoices.each{
        ttsOpt.add(["${it.id}":"${it.label}"])
    }
    String ttsCurrent = getInputElemStr( [name:"voice", type:"enum", title:"<span style='font-size:small;font-weight:bold;'>Default text to speech (TTS) Voice</span>", width:"15em", background:"#ADD8E6", radius:"15px",defaultValue:"${tVal}",options:ttsOpt])  
    
    
    String basicData ="<table style='border:solid 1px black;border-radius:15px'><tr><td>${hubName}</td><td>${mDNS}</td><td>${tempScale}</td><td></td></tr>"
    basicData += "<tr><td>${zipCode}</td><td>${latitude}</td><td>${longitude}</td><td></td></tr>"
    basicData += "<tr><td>${timeFormat}</td><td>${timeZone}</td><td>${ttsCurrent}</td><td></td></tr>"
    String bi = btnIcon([name:'save', size:'14px'])
    basicData += "<tr><td></td><td></td><td></td><td>${getInputElemStr(name:'saveBasic', type:'button', width:'5em', radius:'12px', background:'#00cc00', title:"<span style='font-weight:bold;color:white'>${bi} Save</span>")}</td></tr></table>"
    
    basicData += "<table><tr><td colspan='7'>&nbsp;</td></tr><tr><td style='font-size:small;font-weight:bold;'>MAC Address</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${hubDataMap.macAddress}</td><td>&nbsp;</td>"
    basicData += "<td style='font-size:small;font-weight:bold;'>Hub UID</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${hubDataMap.hubUID}</td></tr>"
    basicData += "<tr><td style='font-size:small;font-weight:bold;'>Hub Registered</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${hubDataMap.hubRegistered}</td><td>&nbsp;</td><td style='font-size:small;font-weight:bold;'>IP Address</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${hubDataMap.ipAddress}</td></tr>"

    basicData += "<tr><th colspan = '7'><hr></th></tr>"
    basicData += "<tr><th colspan = '7'>Associated Emails</th></tr>"
    basicData += "<tr><th style='font-size:small;font-weight:bold;'>Email</th><td>&nbsp;</td><th style='font-size:small;font-weight:bold;'>Role</th></tr>"
    hubDataMap.users.each {
		String itRole = 'unknown'
        if(it.admin == true)
        	itRole = 'admin'
        else
            itRole = 'user'
        basicData += "<tr><td style='min-width:10em;overflow-wrap:anywhere;'>${it.email}</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${itRole}</td></tr>"
    }
    basicData += "<tr><th colspan = '7'><hr></th></tr></table>"  

    return basicData
}

void saveBaseData(){
    bodyMap = [:]
    settings.each { 
        //log.debug "${it.properties}"
        if(it.key != 'debugEnabled' && it.key != 'selectedDev' && it.key != 'nameOverride' && it.key != 'hubName') {
            bodyMap.put(it.key, "${it.value}")
        } else if(it.key == 'hubName')
        	bodyMap.put('name', "${it.value}")
    }
    if(debugEnabled) log.debug bodyMap
    
    try{
        params = [
            uri: "http://127.0.0.1:8080/location/update",
            headers: [
				"Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json"
            ],
			body:bodyMap
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpPost(params){ resp ->
            if(debugEnabled) 
            	log.debug "$resp.data"
            if(resp.data.message != null)
            	log.error resp.data.message
		}
    }catch (e){
        log.error "$e"
    }
    
}

String getAttr(aRange){
    ArrayList attrList = []
    selectedDev.supportedAttributes.each{
        String attr = it.toString()
        if(aRange.contains(attr.substring(0,1)) && it.toString() != 'html'){
        	tMap = [key:"${it}", value:"${selectedDev.currentValue("$it", true)}"]
        	attrList.add(tMap)
        }
    }
    ArrayList attrSort = attrList.sort{ it.key }
    String retVal = '<table>'
    Integer i=0
    String prevKey = ''
    attrSort.each{
        if( it.key != prevKey) {
        	if(i==0) {
        		retVal += "<tr><td style='font-size:small;font-weight:bold;'>${it.key}:</td><td>&nbsp;</td><td style='min-width:10em;overflow-wrap:anywhere;'>${it.value}</td><td>&nbsp;</td>"
        		i = 1
        	} else {
            	retVal += "<td style='font-size:small;font-weight:bold'>${it.key}:</td><td style='min-width:10em;overflow-wrap:anywhere;'>${it.value}</td></tr>"
				i=0
        	}
        }
        prevKey = it.key
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
    String visualRep = """<div id='container${i}' style='padding:0;position:relative;margin:0;height:vh;width:vw;border:inset;border-radius:15px;background-color:#80b3ff'>
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

String genClockWidget(tFormat){
    String htmlStr = """
<div style='border-radius:15px;background-color:#e6ffff'><span id='clock'></span><span id='upTimeElement'>Initializing...</span></div>
<script>
tRemain = 300;
function rFresh() {
	window.location.reload();
}

function checkTime(i) {
	if (i < 10) {i = "0" + i}
    return i;
}

setInterval(utimer,1000,parseInt(${now()-(location.hub.uptime*1000)}/1000));

function utimer(ht){
	tnow = Math.floor(Date.now()/1000)
	ut = Math.floor( (tnow - ht))
    days = Math.floor(ut/(3600*24));
    hrs = Math.floor((ut - (days * (3600*24))) /3600);
    min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60);
    sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60)));
    oString = days+'days, '+hrs+'hrs, '+min+'min, '+sec+'sec';
	tRemain--;
	if (tRemain <= 0) rFresh();
	oString += "<span style='font-weight:bold;font-size:smaller;'>&nbsp;&nbsp;&nbsp;Page Refresh in&nbsp;</span>"+tRemain+" seconds";
    document.getElementById('upTimeElement').innerHTML = updateClock()+"<span style='font-weight:bold;font-size:smaller;'>&nbsp;&nbsp;&nbsp;Hub Up Time&nbsp;</span>"+oString;
}


"""
    if(tFormat.toString() == '24'){
		htmlStr += """
function updateClock() {
      const today = new Date();
      let hours = today.getHours();
      let minutes = today.getMinutes();
      let seconds = today.getSeconds();

      minutes = checkTime(minutes);
      seconds = checkTime(seconds);

      return "<span style='font-weight:bold;font-size:smaller;'>Current Time&nbsp;</span>"+hours + ":" + minutes + ":" + seconds;
}
</script>
"""
    } else {
        htmlStr += """
function updateClock() {
  const now = new Date();
  let hours = checkTime(now.getHours());
  const minutes = checkTime(now.getMinutes());
  const seconds = checkTime(now.getSeconds());
  const ampm = hours >= 12 ? ' pm' : ' am';

  hours = hours % 12;
  hours = hours ? hours : 12; // the hour '0' should be '12'

  formattedTime = hours+':'+minutes+':'+seconds+ampm;
  return "<span style='font-weight:bold;font-size:smaller;'>Current Time&nbsp;</span>"+formattedTime;
}

</script>
"""
    }
	//uploadHubFile("clockWidget.html",htmlStr.getBytes("UTF-8"))
    return htmlStr
    
}


HashMap getHubJson(){
        def params = [
        uri: 'http://127.0.0.1:8080/hub/details/json',
        contentType: "application/json"
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return resp.data
            }
            else {
                log.error "Read Json - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read JSON error ${exception.message}"
    }
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
