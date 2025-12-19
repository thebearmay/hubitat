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
 *    05Apr2025								 v0.1.1 - Freememory fix for MB
 *    07Apr2025								 v0.1.2 - Add Memory History
 *	  										 v0.1.3 - Fix graph sizes
 *	  08Apr2025								 v0.1.4 - Change Windy URL
 *	  10Apr2025								 v0.1.5	- Add CPU Temperature chart
 *    12May2025                              v0.1.6 - Add Full Screen option
 *	  25May2025								 v0.1.7 - Change capability to device.HubInformationDriverv3
 *	  19Dec2025								 v0.1.8 - Trap the start up error when the Hub Info device hasn't fully populated
 */
    


static String version()	{  return '0.1.8'  }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
//include thebearmay.uiInputElements


definition (
	name: 			"Hub Detailed Information Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"App to combine the Hub Details page with information collected by the Hub Information Driver v3",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubInfoDisp.groovy",
    installOnOpen:  true,
	oauth: 			false,
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
            db = getInputElemStr(name:'debugEnabled', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Debug Enabled</b>', defaultValue: "${settings['debugEnabled']}")
/*	        if(state.accessToken == null) createAccessToken()
    	    apiSection = getInputElemStr(name:'api', type:'divHide', width:'15em', radius:'12px', background:'#669999', title:'API Information', divName:'apiSection', hidden:true)
        	String pStr = "<div id='apiSection' ${divStyle}><p><b>Local Server API:</b> ${getFullLocalApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Cloud Server API: </b>${getFullApiServerUrl()}/refresh?access_token=${state.accessToken}</p>"
        	pStr+="<p><b>Access Token: </b>${state.accessToken}</p></div>"

        	paragraph "${apiSection}${pStr}"
*/
            fs = getInputElemStr(name:'fullScreen', type:'bool', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Display Full Screen</b>', defaultValue: "${settings['fullScreen']}")
            fileList = getFiles()
            state.jsInstalled = false
            fileList.each {
			if("$it" == "chart.js")
				state.jsInstalled = true                    
            }
            if(!state?.jsInstalled)
             	paragraph getInputElemStr(name:'jsInstall', type:'button', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Install ChartJS</b>')
            String sDev = getInputElemStr(name:'selectedDev', type:'device.HubInformationDriverv3', width:'15em', radius:'12px', background:'#e6ffff', title:'<b>Select Hub Info Device</b>')
			String aRename = getInputElemStr(name:"nameOverride", type:"text", title: "<b>New Name for Application</b>", multiple: false, defaultValue: app.getLabel(), width:'15em', radius:'12px', background:'#e6ffff')
            paragraph "<table><tr><td style='min-width:15em'>${db}</td><td>${fs}</td><td>${aRename}</td></tr><tr><td>${sDev}</td></tr></table>"
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
                            if(it.key != 'debugEnabled' && it.key != 'selectedDev' && it.key != 'nameOverride' && it.key !='fullScreen')
                            	app.removeSetting("${it.key}")
                        }
                    }
                }
                state.lastLoad = new Date().getTime()
            	paragraph "${buildPage()}"
                if(state.hiRefresh) {
                    state.hiRefresh = false
                    settings.each {
						if(it.key != 'debugEnabled' && it.key != 'selectedDev' && it.key != 'nameOverride' && it.key !='fullScreen')
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
	String c1 = '<p style="font-weight:bold;background-color:yellow">CPU Percentage Not Available - check Hub Info Device</p>'
	String c2 = '<p style="font-weight:bold;background-color:yellow">Free Memory Not Available - check Hub Info Device</p>'
	String c3 = '<p style="font-weight:bold;background-color:yellow">Hub Temperature Not Available - check Hub Info Device</p>'
	if(selectedDev.currentValue('cpuPct') != null)
		c1 = buildChart([attrSelect:'cpuPct',cList:['\"#0efb1c\"','\"#fdf503\"','\"#fd0a03\"'],wList:[8,20,100],i:0])
	else 
		c1 = '<p style="font-weight:bold;background-color:yellow">CPU Percentage Not Available - check Hub Info Device</p>'
	if(selectedDev.currentValue('freeMemory') != null){
		String fmUnit = selectedDev.currentState('freeMemory')?.unit
		if(fmUnit == 'MB') 
			cScale = 1 
		else if(fmUnit == 'GB')
			cScale = 0.001
		else
			cScale = 1000
		c2 = buildChart([attrSelect:'freeMemory',cList:['\"#fd0a03\"','\"#fdf503\"','\"#0efb1c\"'],wList:[100,200,2000], scale:cScale, i:1])
	} else 
		c2 = '<p style="font-weight:bold;background-color:yellow">Free Memory Not Available - check Hub Info Device</p>'
	if(selectedDev.currentValue('temperature') != null){
		String tUnit = selectedDev.currentState('temperature')?.unit
		if(tUnit.contains('C')){
			c3 = buildChart([attrSelect:'temperature',cList:['\"#0efb1c\"','\"#fdf503\"','\"#fd0a03\"'],wList:[65,80,104],i:2])
		}else {     
			c3 = buildChart([attrSelect:'temperature',cList:['\"#0efb1c\"','\"#fdf503\"','\"#fd0a03\"'],wList:[150,176,220],i:2])    
		}
	} else 
		c3 = '<p style="font-weight:bold;background-color:yellow">Hub Temperature Not Available - check Hub Info Device</p>'	
    String t1 = buildTrend()
    String lat = selectedDev.currentValue('latitude')
    String lon = selectedDev.currentValue('longitude')
    //String ifrm = "<iframe style='height:300px;padding:0;margin:0;border-radius:15px' src='https://embed.windy.com/embed.html?type=map&location=coordinates&metricRain=default&metricTemp=default&metricWind=default&zoom=5&overlay=radar&product=ecmwf&level=surface&lat=${lat}&lon=${lon}&detailLat=${lat}&detailLon=${lon}&marker=true' data-fs='false' onload='(() => {const body = this.contentDocument.body;const start = () => {if(this.dataset.fs == 'false') {this.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';this.dataset.fs = 'true';} else {this.style = 'width: 100%; height: 100%;';this.dataset.fs = 'false';}}body.addEventListener('dblclick', start);})()'></iframe>"
    String ifrm = "<iframe style='height:300px;padding:0;margin:0;border-radius:15px' src='https://embed.windy.com/embed2.html?type=map&location=coordinates&metricRain=default&metricTemp=default&metricWind=default&zoom=5&overlay=radar&product=ecmwf&level=surface&lat=${lat}&lon=${lon}&detailLat=${lat}&detailLon=${lon}&marker=true'></iframe>"
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
    
    String fullScrn = "<script>document.getElementById('divSideMenu').setAttribute('style','display:none !important');document.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');document.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');document.getElementById('divMainUIFooter').setAttribute('style','display:none !important');const contentHeight = Math.round(window.innerHeight * 1.2);document.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');document.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');</script><style>overflow-y: scroll !important;</style>"
    String pContent = "<div style='white-space:normal !important;'><table><caption>${hubDataMap.hubName}</caption><tr><td>${headDiv}</td><td>${basicDivB}</td><td>${aToFdivB}</td><td>${gToPdivB}</td><td>${qToZdivB}</td><td style='min-width:5em'>&nbsp;</td>"
    pContent += "<td>${hiRefreshBtn}</td><td>${cPage}</td></tr></table>"
    pContent += "<table id='chartSection', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'><tr><td>${c1}</td><td>&nbsp;</td><td>${c2}</td><td>&nbsp;</td><td>${c3}</td></tr>"
    pContent += "<tr style='max-height:270px !important;'><td colspan=4>${t1}</td><td style='vertical-align:top;padding:0;margin:0'>${ifrm}</td></tr></table>"
	pContent += genClockWidget(hubDataMap.timeFormat)
    pContent += "<div id='basicDiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${basicData}</div>"   
    pContent += "<div id='aToFdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${aToF}</div>"
    pContent += "<div id='gToPdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${gToP}</div>"
    pContent += "<div id='qToZdiv', style='padding:0;margin:0;background-color:#e6ffff;border-radius:12px;'>${qToZ}</div>"
    pContent += "<table><tr><td>${headDiv}</td><td>${basicDivB}</td><td>${aToFdivB}</td><td>${gToPdivB}</td><td>${qToZdivB}</td><td style='min-width:5em'>&nbsp;</td>"
    pContent += "<td>${hiRefreshBtn}</td><td>${cPage}</td></tr></table></div>"
    if(fullScreen)
    	pContent += "${fullScrn}"
	
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
        if(aRange.contains(attr.substring(0,1)) && it.toString() != 'html' && it.toString() != 'type'){
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
    if(!opts.scale || opts.scale == null) opts.scale = 1
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
document.getElementById('cValue${i}').innerHTML = '${selectedDev.currentValue(attrSelect, true)} '+'${selectedDev.currentState(attrSelect)?.unit}';
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
    
    //if(debugEnabled)
        uploadHubFile ("pageBuildWork.txt",visualRep.toString().getBytes("UTF-8"))    
    return(visualRep)
}

String getHistory(){
    def params = [
        uri: "http://127.0.0.1:8080",
        path: "/hub/advanced/freeOSMemoryHistory",
        contentType: "text/html",
        textParser: true  
    ]
    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                return """${resp.data}"""
            }
            else {
                log.error "getHistory - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "getHistory Error: ${exception.message}"
        return null
    }
}

def buildTrend() {
	hData = getHistory()
	ArrayList tLbl = []
	ArrayList mem = []
	ArrayList jvm = []    
	ArrayList splitRec = []
    String respData
	ArrayList tData = []
	tData = hData.split('\n')
	Boolean firstRec = true
	tData.each{
		splitRec = it.split(',')
		if(!firstRec){
			tLbl.add("\'${splitRec[0]}\'")
			mem.add(splitRec[1])
            if(cols > 3){
				jvm.add(splitRec[4])
            }
		} else {
			firstRec = false
			cols = splitRec.size()
		}
	}    
	Integer i = 4
	visualRep = """<div id='container${i}' style='padding:0;position:relative;margin:0;max-height:300px;width:vw;border:inset;border-radius:15px;background-color:#80b3ff'>
<canvas id="myChart${i}"></canvas>
	</div>

<script>
${insertJS()}
</script>

<script>
ctx = document.getElementById('myChart${i}');
  new Chart(ctx, {
    type: "line",
    data: {
	labels: ${tLbl.toString()},
    datasets: [     
      {
        label: " Free Memory",
		data: ${mem},
        borderWidth: 2,
		borderColor: "#ff0000",
		fill:{
			target:true
		}
      },      {
        label: " JVM Memory",
		data: ${jvm},
        borderWidth: 2,
		borderColor: "#0000ff",
		fill:{
			target:true
		}
      }
      ]
    },
    options: {
      scales: {
        y: {
          beginAtZero: true
        }
      },
	  elements: {
          point:{
            radius: 0
          }
      }
    }
  });
</script>
""" 
    if(debugEnabled)
    	uploadHubFile ("pageBuildWork2.txt",visualRep.toString().getBytes("UTF-8"))
	return visualRep
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

/*
*
* Set of methods for UI elements
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
*	Date			Who					Description
*	----------		--------------		-------------------------------------------------------------------------
*	11Mar2025		thebearmay			Add checkbox uiType, add trackColor and switchColor for type = bool
*	13Mar2025							Added hoverText, code cleanup
*	15Mar2025							Expand btnIcon to handle he- and fa- icons
*	18Mar2025							Add btnDivHide to hide/display div's (uiType='divHide')
* 	03Apr2025							Enable a default value for enums
*	04Apr2025							Size option for icons
*/

import groovy.transform.Field
import java.text.SimpleDateFormat
library (
    base: "app",
    author: "Jean P. May Jr.",
    category: "UI",
    description: "Set of methods that allow the customization of the INPUT UI Elements",
    name: "uiInputElements",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/uiInputElements.groovy",
    version: "0.0.7",
    documentationLink: ""
)

/************************************************************************
* Note: If using hoverText, you must add $ttStyleStr to at least one 	*
*			element display												*
************************************************************************/

String getInputElemStr(HashMap opt){
   switch (opt.type){
	case "text":
	   return inputItem(opt)
	   break
	case "number":
	   return inputItem(opt)
	   break
	case "decimal":
	   return inputItem(opt)
	   break
	case "date":
	   return inputItem(opt)
	   break
	case "time":
	   return inputItem(opt)
	   break	
	case "password":
	   return inputItem(opt)
	   break
	case "color":
	   return inputItem(opt)
	   break
   	case "enum":
	   return inputEnum(opt)
	   break
	case "mode":
	   return inputEnum(opt)
	   break
	case "bool":
	   return inputBool(opt)
	   break
	case "checkbox":
	   return inputCheckbox(opt)
	   break       
    case "button":
	   return buttonLink(opt)
	   break
	case "icon":
	   return btnIcon(opt)
	   break
	case "href":
	   return buttonHref(opt)
	   break
	case "divHide":
	   return btnDivHide(opt)
	   break
default:
       if(opt.type && (opt.type.contains('capability') || opt.type.contains('device')))
	       return inputCap(opt)
       else 
	       return "Type ${opt.type} is not supported"
	   break
   }
}

String appLocation() { return "http://${location.hub.localIP}/installedapp/configure/${app.id}/" }

/*****************************************************************************
* Returns a string that will create an input element for an app - limited to *
* text, password, number, date and time inputs currently                     *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) input type					     					 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/
String inputItem(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    if(settings[opt.name] != null){
        if(opt.type != 'time') {
        	opt.defaultValue = settings[opt.name]
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat('HH:mm')
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            opt.defaultValue = sdf.format(sdfIn.parse(settings[opt.name]))
        }
    }
    typeAlt = opt.type
    if(opt.type == 'number') {
    	step = ' step=\"1\" '
    } else if (opt.type == 'decimal') {
        step = ' step=\"any\" '
        typeAlt = 'number'
    } else {
        step = ' '
    }
        
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    
    if(opt.hoverText && opt.hoverText != 'null'){  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    }
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
	retVal+="<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield has-placeholder is-dirty is-upgraded' style='' data-upgraded=',MaterialTextfield'>"
    retVal+="<label for='settings[${opt.name}]' style='min-width:${opt.width}' class='control-label'>${opt.title}</label><div class='flex'><input type='${typeAlt}' ${step} name='settings[${opt.name}]' class='mdl-textfield__input submitOnChange' style='${computedStyle}' value='${opt.defaultValue}' placeholder='Click to set' id='settings[${opt.name}]'>"
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\"><div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div></div></div></div>"
    return retVal
}
	
/*****************************************************************************
* Returns a string that will create an input capability element for an app   *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     	 						 *
*	type - (required) capability type, 'capability.<capability or *>'    	 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color         *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputCap(HashMap opt) {
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize}"
    if(opt.radius) 
    	computedStyle += "border-radius:${opt.radius};"
    else 
    	opt.radius = '1px'
    if(!opt.multiple) opt.multiple = false
    String dList = ''
    String idList = ''
    int i=0
    if(settings["${opt.name}"]){
        ArrayList devNameId = []
        settings["${opt.name}"].each{
            devNameId.add([name:"${it.displayName}", devId:it.deviceId])
        }
        ArrayList devNameIdSorted = devNameId.sort(){it.name}
        devNameIdSorted.each{
			if(i>0) { 
                dList +='<br>'
                idList += ','
            }
            dList+="${it.name}"
            idList+="${it.devId}"
	        i++
	    }
    } else {
    	dList = 'Click to set'
    }
    String capAlt = opt.type.replace('.','')
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"

	String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<div class='capability ${capAlt} mdl-cell mdl-cell--4-col' style='margin: 8px 0; '>"//${computedStyle}
	//retVal += "<button type='button' class='btn btn-default btn-lg btn-block device-btn-filled btn-device mdl-button--raised mdl-shadow--2dp' style='text-align:left; width:100%;' data-toggle='modal' data-target='#deviceListModal' "
    retVal += "<button type='button' class='btn btn-lg btn-block btn-device' style='border-radius:${opt.radius};border:0px;text-align:left; width:100%; min-width:${opt.width}' data-toggle='modal' data-target='#deviceListModal' "
    retVal += "data-capability='${opt.type}' data-elemname='${opt.name}' data-multiple='${opt.multiple}' data-ignore=''>"
    	retVal += "<span style='white-space:pre-wrap;'>${opt.title}</span><br><div style='${computedStyle}'>"
	retVal += "<span id='${opt.name}devlist' class='device-text' style='text-align: left;'>${dList}</span></button>"
	retVal += "<input type='hidden' name='settings[${opt.name}]' class='form-control submitOnChange' value='${idList}' placeholder='Click to set' id='settings[${opt.name}]'>"
	retVal += "<input type='hidden' name='deviceList' value='${opt.name}'><div class='device-list' style='display:none'>"
	retVal += "<div id='deviceListModal' style='border:1px solid #ccc;padding:8px;max-height:300px;overflow:auto;min-width:${opt.width}'><div class='checkAllBoxes my-2'>"
	retVal += "<label class='mdl-checkbox mdl-js-checkbox mdl-js-ripple-effect checkall mdl-js-ripple-effect--ignore-events is-upgraded' id='${opt.name}-checkall' for='${opt.name}-checkbox-0' data-upgraded=',MaterialCheckbox,MaterialRipple'>"
    	retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"${opt.name}-checkall\").classList.contains(\"is-checked\")){document.getElementById(\"${opt.name}-checkall\").classList.remove(\"is-checked\");}else{document.getElementById(\"${opt.name}-checkall\").classList.add(\"is-checked\");}}</script>"    
    retVal += "<input type='checkbox' class='mdl-checkbox__input checkboxAll' id='${opt.name}-checkbox-0' onclick='toggleMe${opt.name}()'><span class='mdl-checkbox__label'>Toggle All On/Off</span>"
	retVal += "<span class='mdl-checkbox__focus-helper'></span><span class='mdl-checkbox__box-outline'><span class='mdl-checkbox__tick-outline'></span></span>"
	retVal += "<span class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple'></span></span></label></div>"
	retVal += "<div id='${opt.name}-options' class='modal-body' style='overflow:unset'></div></div></div>"
	retVal += "<div class='mdl-button mdl-js-button mdl-button--raised pull-right device-save' data-upgraded=',MaterialButton' style='${computedStyle};width:6em;min-width:6em;'>Update</div></div></div>"
    
    return retVal
}

/*****************************************************************************
* Returns a string that will create an input enum or mode element for an app *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	multiple - true/<false>						     						 *
*	options - list of values for the enum (modes will autofill)	     		 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/


String inputEnum(HashMap opt){
    String computedStyle = ''
    if(opt.type == 'mode') opt.options = location.getModes()
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"    
    if(!opt.multiple) {
    	opt.multiple = false
        mult = ' '
    } else {
        mult = 'multiple'
    }
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield' style='margin: 8px 0; padding-right: 8px;' data-upgraded=',MaterialTextfield'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' class='control-label'>${opt.title}</label><div class='SumoSelect sumo_settings[${opt.name}]' tabindex='0' role='button' aria-expanded='false'>"
    retVal += "<div style='${computedStyle}'><select id='settings[${opt.name}]' ${mult} name='settings[${opt.name}]' class='selectpicker form-control mdl-switch__input submitOnChange SumoUnder' placeholder='Click to set' data-default='' tabindex='-1' style='${computedStyle}'></div>"
    ArrayList selOpt = []
	if(settings["${opt.name}"]){
        if("${settings["${opt.name}"].class}" == 'class java.lang.String')
        	selOpt.add("${settings["${opt.name}"]}")
       else {               
        	settings["${opt.name}"].each{
            	selOpt.add("$it")
        	}
       }
    } else if(opt.defaultValue) selOpt.add("${opt.defaultValue}")
    if(mult != 'multiple') retVal+="<option value=''>Click to set</option>"
    opt.options.each{ option -> 
        if("$option".contains(':')){
            optSplit = "$option".replace('[','').replace(']','').split(':')
            optVal = optSplit[0]
            optDis = optSplit[1]
        } else {
            optVal = option
            optDis = option
        }
        sel = ' '
        selOpt.each{
            //log.debug "$it $optVal ${"$it" == "$optVal"}"
            if("$it" == "$optVal" ) 
            	sel = 'selected'
        }
        retVal += "<option value='${optVal}' ${sel}>${optDis}</option>"
    }
    retVal+= "</select></div></div>"
    return retVal
}

/*****************************************************************************
* Returns a string that will create an input boolean element for an app 	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	trackColor - CSS color descriptor for the switch track					 *
*	switchColor - CSS color descriptor for the switch knob					 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputBool(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    String trackColor = ' '
    String switchColor = ' '
    if(opt.trackColor) trackColor = "background-color:$opt.trackColor"
    if(opt.switchColor) switchColor = "background-color:$opt.switchColor"

    if(settings["${opt.name}"]) opt.defaultValue = settings["${opt.name}"]
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"lbl${opt.name}\").classList.contains(\"is-checked\")){document.getElementById(\"lbl${opt.name}\").classList.remove(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",false);}else{document.getElementById(\"lbl${opt.name}\").classList.add(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",true);}}</script>"
    retVal+="<div class='mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield' style='${computedStyle}' data-upgraded=',MaterialTextfield'><div class='w-fit'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' onmouseup=\"toggleMe${opt.name}();changeSubmit(document.getElementById('settings[$opt.name]'))\" id='lbl${opt.name}' class='mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded"
    if(opt.defaultValue == true) retVal += " is-checked"
    retVal += "' data-upgraded=',MaterialSwitch,MaterialRipple'>"
	retVal += "<input name='checkbox[${opt.name}]' id='settings[${opt.name}]' class='mdl-switch__input ' type='checkbox'><div class='mdl-switch__label w-fit'>${opt.title}"
	retVal += "</div><div class='mdl-switch__track' style='$trackColor'></div><div class='mdl-switch__thumb' style='$switchColor'><span class='mdl-switch__focus-helper'></span></div><span class='mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple is-animating' style='width: 137.765px; height: 137.765px; transform: translate(-50%, -50%) translate(24px, 24px);'></span></span></label>"
	retVal += "</div>"
    retVal += "<input id='hid${opt.name}' name='settings[${opt.name}]' type='hidden' value='${opt.defaultValue}'>"
	retVal += "</div>"

    return retVal
}

/*****************************************************************************
* Returns a string that will create an input checkbox element for an app 	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) capability type, <enum/mode>			     			 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	cBoxColor - CSS color descriptor for the checkbox color					 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String inputCheckbox(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
	opt.type = 'bool'
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.multiple) opt.multiple = false
    
    if(settings["${opt.name}"]) 
    	opt.defaultValue = settings["${opt.name}"]
    else
        opt.defaultValue = false
    if(!opt.cBoxColor) opt.cBoxColor = '#000000'
    String cbClass = 'he-checkbox-unchecked'
    if(opt.defaultValue) 
    	cbClass = 'he-checkbox-checked'    

    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
    retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"lbl${opt.name}\").classList.contains(\"is-checked\")){document.getElementById(\"lbl${opt.name}\").classList.remove(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",false);}else{document.getElementById(\"lbl${opt.name}\").classList.add(\"is-checked\");document.getElementById(\"hid${opt.name}\").setAttribute(\"value\",true);}}</script>"
    retVal+="<div class='mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield' style='${computedStyle}' data-upgraded=',MaterialTextfield'><div class='w-fit'>"
    retVal += "<label for='settings[${opt.name}]' style='min-width:${opt.width}' onmouseup=\"toggleMe${opt.name}();changeSubmit(document.getElementById('settings[$opt.name]'))\" id='lbl${opt.name}' class='mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded"
    if(opt.defaultValue == true) retVal += " is-checked"
    retVal += "' data-upgraded=',MaterialSwitch,MaterialRipple'>"
	retVal += "<input name='checkbox[${opt.name}]' id='settings[${opt.name}]' class='mdl-switch__input ' type='checkbox'><div><i class='${cbClass}' style='color:${opt.cBoxColor}'></i><span class='mdl-switch__label w-fit'>${opt.title}</span>"
	retVal += "</div><span class='mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple is-animating' style='width: 137.765px; height: 137.765px; transform: translate(-50%, -50%) translate(24px, 24px);'></span></span></label>"
	retVal += "</div>"
    retVal += "<input id='hid${opt.name}' name='settings[${opt.name}]' type='hidden' value='${opt.defaultValue}'>"
	retVal += "</div>"

    return retVal
}

/*****************************************************************************
* Returns a string that will create an button element for an app 	     	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/

String buttonLink(HashMap opt) { //modified slightly from jtp10181's code
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    String computedStyle = 'cursor:pointer;text-align:center;box-shadow: 2px 2px 4px #71797E;'
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button HREF element for an app     	 *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	destPage - (required unless using destUrl) name of the app page to go to *
*	destUrl - (required unless using destPage) URL for the external page	 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*****************************************************************************/
String buttonHref(HashMap opt) { //modified jtp10181's code
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    if(!opt.destPage && !opt.destUrl) 
    	return "Error missing Destination info"
    String computedStyle = 'cursor:pointer;text-align:center;box-shadow: 2px 2px 4px #71797E;'
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.destPage) {
    	inx = appLocation().lastIndexOf("/")
    	dest = appLocation().substring(0,inx)+"/${opt.destPage}"
    } else if(opt.destUrl) {
    	dest=opt.destUrl
    }
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='window.location.replace(\"$dest\")' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button element to hide/display a div	 *
*     for an app                                                             *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     	 *
*		special characters					     							 *
*	title - (required) button label					     					 *
*	divName - (require) name of the division to hide or display				 *
*	hidden - if true will hide the div immediately							 *
*	width - CSS descriptor for field width				     				 *
*	background - CSS color descriptor for the input background color		 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor										 *
*	radius - CSS border radius descriptor (corner rounding)		     		 *
*****************************************************************************/

String btnDivHide(HashMap opt) { 
    if(!opt.name || !opt.title || !opt.divName) 
    	return "Error missing name, title or division"
    String computedStyle = 'cursor:pointer;box-shadow: 2px 2px 4px #71797E;'
    if(!opt.width) opt.width = '100%'
    computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(opt.destPage) {
    	inx = appLocation().lastIndexOf("/")
    	dest = appLocation().substring(0,inx)+"/${opt.destPage}"
    } else if(opt.destUrl) {
    	dest=opt.destUrl
    }
    String btnElem = "<i id='btn${opt.name}' class='material-icons he-shrink2' style='font-size:smaller'></i>"
    String script= "<script>document.getElementById(\"${opt.divName}\").style.display=\"block\"</script>"
    if(opt.hidden){
    	btnElem = "<i id='btn${opt.name}' class='material-icons he-enlarge2' style='font-size:smaller'></i>"
        script="<script>document.getElementById(\"${opt.divName}\").style.display=\"none\"</script>"
    }
    
    opt.title = "${btnElem}&nbsp;${opt.title}"
	if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    return "$script<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='btn=document.getElementById(\"btn${opt.name}\");div=document.getElementById(\"${opt.divName}\");if(div.style.display==\"none\"){btn.classList.remove(\"he-enlarge2\");btn.classList.add(\"he-shrink2\");div.style.display=\"block\";} else {btn.classList.remove(\"he-shrink2\");btn.classList.add(\"he-enlarge2\");div.style.display=\"none\";}' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button icon element for an app from   *
*	the materials-icon font						     						 *
*                                                                            *
*	name - (required) name of the icon to create			     			 *
*****************************************************************************/

String btnIcon(HashMap opt) {  //modified from jtp10181's code
    String computedStyle = ' '
    if(opt.size) computedStyle += "font-size:${opt.size};"
    if(opt.color) computedStyle += "color:${opt.color};"
	if(opt.name.startsWith('he'))
    	return "<i class='p-button-icon p-button-icon-left pi ${opt.name}' data-pc-section='icon' style='${computedStyle}'></i>"
	else if(opt.name.startsWith('fa'))                               
        return "<i class='fa-regular ${opt.name}' style='${computedStyle}'></i>"//fa-circle-info
    else 
        return "<i class='material-icons ${opt.name}' style='${computedStyle}'>${opt.name}</i>"
}

@Field static String ttStyleStr = "<style>.tTip {display:inline-block;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;text-align:left;}</style>"
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;} tr {border-right:2px solid black;}</style>"
