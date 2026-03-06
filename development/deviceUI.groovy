 /*
 * Device UI
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
 *    
 */
static String version()	{  return '0.1.2'  }
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
//include thebearmay.uiInputElements


definition (
	name: 			"Device UI", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"Alternate Device UI",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/deviceUI.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "deviceList"
    page name: "uiRender"

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

def deviceList(){
    dynamicPage (name: "deviceList", title: "<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>", install: true, uninstall: true) {
        section (name:'cPageHndl', title:''){
		            
            paragraph getInputElemStr(name:"selDev", type:'capability.*', width:'15em', radius:'12px', color:'#000000', background:'#2596be', title:"<b>Device to View</b>")
            //log.debug "${getInputElemStr(name:"selDev", type:'capability.*', width:'15em', radius:'12px', color:'#000000', background:'#2596be', title:"<b>Device to View</b>").replace('<','&lt;')}"
            paragraph getInputElemStr(name:"debugEnabled", type:'bool', width:'15em', radius:'12px', color:'#000000', background:'#2596be', title:"<b>Enable Debug</b>")
            paragraph getInputElemStr(name:"nameOverride", type:'text', width:'15em', radius:'12px', color:'#000000', background:'#2596be', title:"<b>New Name for Application</b>", defaultValue:app.getLabel())
            if(selDev){
                inx = appLocation().lastIndexOf("/")
                paragraph "<script>window.location.replace('${appLocation().substring(0,inx)}/uiRender')</script>"
            }
            
            if(debugEnabled) runIn(1800, 'logsOff')
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)

        }
    }
}

def uiRender(){
    dynamicPage (name: "uiRender", title: "", install: false, uninstall: false) { 
        section(name:"visualDisp",title:"", hideable: false, hidden: false){
            if(state.message){
                paragraph "<span style='background-color:lightGreen;font-weight:bold;'>${state.message}</span>"
                state.message =''
            }
            if(state.refreshNeeded){
                state.refreshNeeded = false
                paragraph "<script>window.location.reload()</script>"
            }
            ndBtn = getInputElemStr(name:"newDev", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Change Device</span>")
            resetLO = getInputElemStr(name:"resetLayout", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Reset Layout</span>")
            

            if(selDev){
            	devId = selDev.id
                deviceMap = getDevice(devId)
            } else {
                inx = appLocation().lastIndexOf("/")
                paragraph "<script>window.location.replace('${appLocation().substring(0,inx)}')</script>"
                return
            }
            
			pContent= buildPage(deviceMap)
            if(debugEnable) {
           		pname = "devUIWork${app.id}.htm"
    			uploadHubFile ("$pname",pContent.getBytes("UTF-8"))
            }
            
            if(selDev){
            	paragraph "${fullScrn}<style>table.listTable  {border-radius:15px;border:1px solid black; border-radius:10px; border-collapse:collapse;} .listTable td, .listTable th{padding:5px;border:1px solid lightGray; border-radius:10px;}.listTable tr:nth-child(odd) { background-color: #f2f2f2; }</style><table><tr><td><span class='font-bold text-xl'>${selDev.displayName}(${selDev.id})</span></td><td>${ndBtn}</td><td>${resetLO}</td></tr></table>"
            	paragraph pContent
            }    
            if(state.prefDirty)
            	paragraph '<script>document.getElementById("region-3-header").classList.add("region-header-dirty")</script>'
            if(state.devInfoDirty)
            	paragraph '<script>document.getElementById("region-4-header").classList.add("region-header-dirty")</script>'
            
           
            if(state.ndBtnPushed){
                state.ndBtnPushed = false
                stateList = []
                state.each{
                	stateList.add(it.key)                    	
            	}
                stateList.each{
                    if(it != "isInstalled"  && it!= null ){
	        			app.state.remove(it)
        			}
            	}
            	settings.each {
                    if(it.key != 'savePos')
                    	app.removeSetting(it.key)
            	}

                inx = appLocation().lastIndexOf("/")
                paragraph "<script>window.location.replace('${appLocation().substring(0,inx)}')</script>"
            }
            if(state.resetLayoutPushed){
                state.resetLayoutPushed = false
                app.removeSetting("savePos")
                paragraph "<script>window.location.reload();</script>"
            }
            if(state.savePrefPushed){
                state.prefDirty = false
                state.savePrefPushed = false               
				settings.each{                 
                    if(it.key.startsWith('pref')) {
                        nKey = it.key.substring(4,).trim()
                        sType = app.getSettingType("${it.key}")
                        //log.debug "$nKey $sType ${it.value}"
                        selDev.updateSetting("${nKey}",[value:"${it.value}".trim(),type:"$sType"])
                    }
            	}
                paragraph "<script>window.location.reload();</script>"
            }
            if(state.resetPrefPushed){
                state.resetPrefPushed = false
                settings.each{                 
                    if(it.key.startsWith('pref')) {
 						app.removeSetting("${it.key}")
                    }
            	}
                paragraph '<script>document.getElementById("prefHeader").classList.remove("region-header-dirty");window.location.reload();</script>'
            }
            
            if(state.saveDevInfPushed){
                state.devInfoDirty = false
                state.saveDevInfPushed = false               
				saveDevInfo(deviceMap)
                paragraph "<script>window.location.reload();</script>"
            }
            if(state.resetDevInfPushed){
                state.resetDevInfPushed = false
                settings.each{                 
                    if(it.key.startsWith('devInf')) {
 						app.removeSetting("${it.key}")
                    }
            	}
                paragraph '<script>document.getElementById("devInfHeader").classList.remove("region-header-dirty");window.location.reload();</script>'
            }
            if(state.unschedPushed) {
                state.unschedPushed = false
                jId = state.unschedId
                state.remove('unschedId')
                removeJob(jId)             
            }

        }
    }
}

def removeJob(jId){
    String jobId= "dev${selDev.id}${state["job$jId"].type}.${state["job$jId"].method}"
    try{		
        params = [
            uri: "http://127.0.0.1:8080/hub/advanced/deleteScheduledJob?id=$jobId",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) 
            	log.debug "$resp.data"
            return resp.data
		}
    }catch (e){
        log.error "$e"
    }
    
}

def appButtonHandler(btn) {
    switch(btn) {
        case 'newDev':
        	state.ndBtnPushed = true
        	break
        case 'resetLayout':
        	state.resetLayoutPushed = true
        	break
        case 'savePref':
        	state.message = 'Saving'
        	state.savePrefPushed = true
        	break
        case "resetPref":
        	state.resetPrefPushed = true
        	break
        case 'saveDevInf':
        	state.message = 'Saving'
        	state.saveDevInfPushed = true
        	break
        case "resetDevInf":
        	state.resetDevInfPushed = true
        	break
        default: 
            //state."${btn}Pushed" = true
            if(btn.startsWith('unsched')){
                state.unschedPushed = true
                state.unschedId = btn.substring(7,)
            } else {
	            bParms = ''
    	    	pList = []
        		inx=0
        		settings.sort().each {               
                	if(it.key.startsWith("$btn")){
                 //   log.debug "${it.properties}"
                    
                		if(inx>0) bParms += ','
                		bParms=it.value
                   		pList.add(it.value)
	                    app.removeSetting("$btn$inx")
    	                inx++
        	        }
        		}
        
	        	if(inx > 1)
    	    		selDev."$btn"(pList.each{it.value})
        		else if(inx==1)
            	    selDev."$btn"(bParms)
	            else
    	            selDev."$btn"()
       			//log.debug "command selDev.$btn(${pList.each{it.value}}) requested"
                state.message ="Command Executed"
            }
        	state.refreshNeeded = true
            break
    }
}


HashMap jsonResponse(retMap){     
    return JsonOutput.toJson(retMap)
}

String buildPage(deviceMap){
    if(!selDev)
    	return
    if (debugEnabled)
    	log.debug "Build Page"
    String region1 = ''
    String region2 = '<p class="region-subheader">Current States</p>'
    String region3 = ''
    String region4 = ''
    String region5 = ''              
    String region6 = ''
    
    deviceMap.commands.sort{it.name}.each {
	   	region1 += buildCommandBlock(it)
    }
    
    deviceMap.device.currentStates.sort().each{
	    it.value.stringValue=it.value.stringValue.replace('\"','\\\"')
        region2 += "<b>${it.value.attributeName}:</b> ${it.value.stringValue}<br>"
    }
    region2 += '<br><p class="region-subheader">State Variables</p>'
    deviceMap.deviceState.sort().each{
        it.value=it.value.toString().replace('\"','\\\"')
        region2 += "<p><b>${it.key}:</b> ${it.value}</p>"
    }
        
    region3 = buildPreference(deviceMap.settings)
    region3 += "<br><br><br>"
    savePref = getInputElemStr(name:"savePref", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Save Prefs</span>")
    revPref = getInputElemStr(name:"resetPref", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Reset Prefs</span>")
    region3 += "<table><tr><td>${savePref}</td><td>${revPref}</td></tr></table>"
    
    region4 = buildDevInfo(deviceMap)
    region4 += "<br><br><br>"
    saveDevInf = getInputElemStr(name:"saveDevInf", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Save Dev Info</span>")
    revDevInf = getInputElemStr(name:"resetDevInf", type:'button', width:'10em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Reset Dev Info</span>")
    region4 += "<table><tr><td>${saveDevInf}</td><td>${revDevInf}</td></tr></table>"
    
   
   	region5 = buildEventList()
    region6 = buildJobList(deviceMap.scheduledJobs)
    
    String regionDef1 = getRegion('region-1', 'Commands', region1)
    String regionDef2 = getRegion('region-2', 'States', region2)
    String regionDef3 = getRegion('region-3', 'Preferences', region3)
    String regionDef4 = getRegion('region-4', 'Device Information', region4)
    String regionDef5 = getRegion('region-5', 'Events', region5)
    String regionDef6 = getRegion('region-6', 'Scheduled Jobs', region6)
    
	pContent = getRegionsPage(['region-1':regionDef1,'region-2':regionDef2,'region-3':regionDef3,'region-4':regionDef4,'region-5':regionDef5,'region-6':regionDef6], true )

    pContent+="<script>try {parseStr('${settings["savePos"]}'); }catch (e) {loadRegionState();}</script>"

    return pContent
}

HashMap getDevice(devId){

	try{		
        params = [
            uri: "http://127.0.0.1:8080/device/fullJson/$devId",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) 
            	log.debug "$resp.data"
            return resp.data
		}
    }catch (e){
        log.error "$e"
    }
    
}

String buildCommandBlock(parms){
    if(debugEnabled)
    	log.debug "buildCommandBlock"
    
    LinkedList textType=["text","number","decimal","date","time","password","color"]
    String cBlock = ''
    String title = reverseCamel(parms.name)
    String btn = getInputElemStr(name:"${parms.name}", type:'button', width:'5em', radius:'12px', background:'#2596be', title:"<span style='font-weight:bold;color:white'>Submit</span>")
    cBlock = "<div style='width:15em;border:1px black solid;border-radius:12px;float:left;padding:4px;'><p style='font-weight:bold;'>$title</p>"
    String params = ''
    if(parms.parameters){
        if(debugEnabled)
        	log.debug "Found ${parms.parameters.size()} parameters"
        int inx = 0
	    parms.parameters.each {
    	    pType = it.type.toLowerCase().trim()
        	if("$pType" == "string") 
        		pType = 'text'
			String nameBlock = ''
            if(it.name) 
            	nameBlock += "<b>${it.name}</b><br>"
            if(it.description)
            	nameBlock += "${it.description}<br>"
            if(nameBlock == '')
            	nameBlock = "Enter a <em>($pType)</em> value<br>"
            if(textType.contains(pType)) {
	       		params += getInputElemStr( [name:"${parms.name}${inx}", type:"${pType}", title:"<span style='font-size:small'>${nameBlock}</span>", width:"12em", background:"#ADD8E6", radius:"15px", defaultValue:""])
            } else if(pType == 'enum') {
                params += getInputElemStr( [name:"${parms.name}${inx}", type:"${pType}", title:"<span style='font-size:small'>${nameBlock}</span>", width:"12em", background:"#ADD8E6", radius:"15px", defaultValue:"", options:it.constraints])
            } else {
                params += "Unknown pType - $pType"
            }
           	params += "<br>"
        	inx++
        }    
		cBlock+= "$params"
    } else if(parms.arguments){
        if(debugEnabled)
        	log.debug "Found ${parms?.arguments?.size()} arguments"
    }
	cBlock+="$btn</div>"
	if(debugEnabled)
		log.debug "$cBlock<br>"
    return cBlock
}


String buildPreference(pSet){
    if(debugEnabled)
    	log.debug "buildPreferenceList($pSet)"
    ArrayList textType=["text","number","decimal","date","time","password","color"]
    pBlock = ''
    state.prefDirty = false

    pSet.each {
        if (it.options) {
            //optsWork = "${it.options}".replace('[','').replace(']','').split(',')
            optsWork = "${it.options}".split(',')
            //log.debug optsWork
        }
        
        if(settings["pref${it.name}"] != null)
        	defVal = settings["pref${it.name}"]
        else if(it.value)
        	defVal = it.value
        else
            defVal = it.defaultValue
        if(debugEnabled)
        	log.debug "${it.name} ${settings["pref${it.name}"]} ${it.value} ${it.defaultValue} => $defVal"
        
        if("$defVal".trim() != "${it.value}".trim()){
            if(debugEnabled)
            	log.debug "${it.name} is dirty: $defVal / ${it.value}"
        	state.prefDirty = true
        }
        
        if (it.type == 'string') 
        	it.type = 'text'
        if(it.type == 'bool')
        	itemWidth = "${it.title.size()*0.70}em"
        else
            itemWidth = '12em'
        
		pBlock +=getInputElemStr( [name:"pref${it.name}", type:"${it.type}", title:"<b>${it.title}</b>", background:"#ADD8E6", width:itemWidth, radius:"15px", options:optsWork, defaultValue:defVal, submitOnChange:true ])
 
    }
    return pBlock
    
}

String buildDevInfo(deviceMap) {
    if(debugEnabled)
    	log.debug "buildDevInfo"
    dMap = deviceMap.device
	state.devInfoDirty = false
    diBlock = ''
    driverList = getDrivers().drivers
    roomsList = getRooms()
    ArrayList dList = []
    ArrayList rList = []
    ArrayList dashList = []
    ArrayList dashSelList = []
    driverList.sort{it.name}.each {
        dList.add("${it.id}:${it.name}")
    }
    if(roomsList) {
    	roomsList.sort{it.name}.each {
        	rList.add("${it.id}:${it.name}")
    	}
    }
    deviceMap.dashboards.sort{it.name}.each{
        dashList.add("${it.id}:${it.name}")
        if(it.selected)
        	dashSelList.add("${it.id}")
    }
    dVal = settings["devInfName"]?:dMap.name
    if(dVal != dMap.name) {
        state.lastDirty = 'displayName'
    	state.devInfoDirty = true
	}
    diBlock += "<table><tr><td>"+getInputElemStr( [name:"devInfName", type:"text", title:"<b>Device Name</b>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:"${dVal}" ])
    dVal = settings["devInfLabel"]?:dMap.label?:""
	if(dVal != dMap.label && dMap.label != null) {
        state.lastDirty = 'label'
    	state.devInfoDirty = true
	}
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfLabel", type:"text", title:"<b>Device Label</b>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:"${dVal}" ])
    dVal = settings["devInfDNI"]?:dMap.deviceNetworkId
    if(dVal != dMap.deviceNetworkId) {
        state.lastDirty = 'DNI'
    	state.devInfoDirty = true
	}
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfDNI", type:"text", title:"<b>Device Network ID</b>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:"${dVal}" ])
    dVal = settings["devInfDevTypeId"]?:dMap.deviceTypeId
    if("$dVal" != "${dMap.deviceTypeId}") {
        state.lastDirty = 'deviceTypeId'
    	state.devInfoDirty = true
	} 
    //log.debug "${getInputElemStr( [name:"devInfDevTypeId", type:"enum", title:"<b>Device Type</b>", width:"20em", background:"#ADD8E6", radius:"15px", options:dList, submitOnChange:true, defaultValue:"${dVal}" ]).replace('<','&lt;')}"
    diBlock += "</td></tr><tr><td>"+getInputElemStr( [name:"devInfDevTypeId", type:"enum", title:"<b>Device Type</b>", width:"20em", background:"#ADD8E6", radius:"15px", options:dList, submitOnChange:true, defaultValue:"${dVal}" ])//+"</div>"
    //dVal = "${settings["devInfRoom"]}"?:"${dMap.roomId}"?:""
    if(settings["devInfRoom"])
    	dVal = settings["devInfRoom"]
    else if (dMap.roomId)
        dVal = dMap.roomId
    else
        dVal = ''
    //log.debug "${settings["devInfRoom"]} ${dMap.roomId} $dVal"
    
    if("$dVal" != "${dMap.roomId}" && dMap.roomId != null) {
        state.lastDirty = 'roomId'
    	state.devInfoDirty = true
	} 
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfRoom", type:"enum", title:"<b>Room</b>", width:"20em", background:"#ADD8E6", radius:"15px", options:rList, submitOnChange:true, defaultValue:"${dVal}" ])//+"</div>"
    dVal = settings["devInfDash"]?:dashSelList?:""
    if(dVal != dashSelList && dashSelList != []) {
        state.lastDirty = "dashSelList $dval $dashSelList"
    	state.devInfoDirty = true
	} 
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfDash", type:"enum", title:"<b>Dashboards</b>", width:"20em", background:"#ADD8E6", radius:"15px", options:dashList, submitOnChange:true, defaultValue:dVal, multiple:true ])//+"</div>"
    dVal = settings["devInfMaxEvent"]?:dMap.maxEvents?:11
    if(dVal != dMap.maxEvents && dMap.maxEvents != null) {
        state.lastDirty = 'maxEvents'
    	state.devInfoDirty = true
	}
    diBlock += "</td></tr><tr><td>"+getInputElemStr( [name:"devInfMaxEvent", type:"number", title:"<b>Max Events</b><br><span style='font-size:smaller'>* per event type (1-2000)</span>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:dVal ])
    dVal = settings["devInfMaxStates"]?:dMap.maxStates?:30
    if(dVal != dMap.maxStates && dMap.maxStates != null) {
        state.lastDirty = 'maxStates'
    	state.devInfoDirty = true
	}
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfMaxStates", type:"number", title:"<b>State History Size</b><br><span style='font-size:smaller'>* per attribute (1-2000)</span>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:dVal ])    
    dVal = settings["devInfSpammy"]?:dMap.spammyThreshold?:300
    if(dVal != dMap.spammyThreshold && dMap.spammyThreshold != null) {
        state.lastDirty = 'spammyThreshold'
    	state.devInfoDirty = true
	}
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfSpammy", type:"number", title:"<b>Too many events alert threshold</b><br><span style='font-size:smaller'>* per hour (100-2000)</span>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:dVal ])
    //dVal = "${settings['devInfMeshEnabled']}"?:"${dMap.meshEnabled}"?:false
    if(settings['devInfMeshEnabled'])
    	dVal = settings['devInfMeshEnabled']
    else if(dMap.meshEnabled)
        dVal = dMap.meshEnabled
    else
        dVal = false
    if("${dVal}" != "${dMap.meshEnabled}" && (dVal != false && dMap.meshEnabled == null)) {
        state.lastDirty = 'meshEnabled'
    	state.devInfoDirty = true
       	//log.debug "${settings["devInfMeshEnabled"]} ${dMap.meshEnabled} $dVal"
	}
    diBlock += "</td></tr><tr><td>"+getInputElemStr( [name:"devInfMeshEnabled", type:"bool", title:"<a href='https://docs2.hubitat.com/en/user-interface/settings/hub-mesh'>Hub Mesh</a> Enabled", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:dVal ])
    dVal = "${settings["devInfHomeKitEnabled"]}"?:"${dMap.homeKitEnabled}"?:false
    if("${dVal}" != "${dMap.homeKitEnabled}" && (dVal == false && dMap.homeKitEnabled != null) ) {
        state.lastDirty = 'homeKitEnabled'
    	state.devInfoDirty = true 
        //log.debug "${settings["devInfHomeKitEnabled"]} ${dMap.homeKitEnabled} $dVal"
	}
    diBlock += "</td><td>"+getInputElemStr( [name:"devInfHomeKitEnabled", type:"bool", title:"<a href='https://docs2.hubitat.com/en/apps/homekit-integration'>HomeKit</a> Enabled", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:dVal ])
    dVal = settings["devInfNotes"]?:dMap.notes?:''
    if(dVal != dMap.notes && dMap.notes != null){ 
        state.lastDirty = 'notes'
    	state.devInfoDirty = true
	}
    diBlock += "</td></tr></table>"+getInputElemStr( [name:"devInfNotes", type:"text", title:"<b>Device Note</b>", width:"20em", background:"#ADD8E6", radius:"15px", submitOnChange:true, defaultValue:"${dVal}" ]) 
    String notesBlock = ''
    dMap.data.each{
        notesBlock += "<b>${it.key}</b>:${it.value}<br>"
    }
    
    String usedBy = '<u><b>In Use By</b></u><br>'
    deviceMap.appsUsing.each {
        usedBy+="<a href='/installedapp/configure/${it.id}'>${it.label}</a><br>"
    }
    String dateBlock = ''
    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    if(dMap.createTime)
    	cDate = sdfIn.parse(dMap.createTime)
    dateBlock += "<b><u>Dates</u></b><br><b>Create Date</b>: $cDate<br>"
    if(dMap.updateTime)
       cDate = sdfIn.parse(dMap.updateTime)
	dateBlock += "<b>Last Update</b>: $cDate<br>"
    if(dMap.lastActivityTime)
    	cDate = sdfIn.parse(dMap.lastActivityTime)
	dateBlock += "<b>Last Activity</b>: $cDate"
    
    notesBlock += "<br><br><style>.nbTable td{vertical-align:top;text-align:left;padding-right:5px;}</style><table class='nbTable'><tr><td>$usedBy</td><td>$dateBlock</td></tr></table>"

    diBlock += "<br><b>Device Data</b><br><div style='border:1px black solid; border-radius:15px;padding:5px;'>$notesBlock</div>"
    
    return diBlock
}

def saveDevInfo(deviceMap) {
    //log.debug "saveDevInfo"
    driverData = getDriverData(settings['devInfDevTypeId'])
    //log.debug "Driver Data:<br>$driverData"
	bodyContent = [
        name: "${settings['devInfName']}",
		label: "${settings['devInfLabel']}",
		zigbeeId: "${deviceMap.device.zigbeeId}",
		maxEvents: "${settings['devInfMaxEvent']}",
		maxStates: "${settings['devInfMaxStates']}",
		spammyThreshold: "${settings['devInfSpammy']}",
		deviceNetworkId: "${settings['devInfDNI']}",
		deviceTypeId: "${settings['devInfDevTypeId']}",
		deviceTypeReadableType: (driverData.type == 'sys') ? 'System':'User',
		roomId: "${settings['devInfRoom']}",
		meshEnabled: "${settings['devInfMeshEnabled']}",
        retryEnabled: "${deviceMap.device.retryEnabled}",
		meshFullSync: "${deviceMap.device.meshFullSync}",
		homeKitEnabled: "${settings['devInfHomeKitEnabled']}",
		locationId: "${deviceMap.device.locationId}",
		hubId: "${deviceMap.device.hubId}",
		groupId: "${deviceMap.device.groupId}",
		dashboardIds: "${settings['devInfDash']}",
		tags: (deviceMap.device.tags != null) ? "${deviceMap.device.tags}" : "",
		defaultIcon: "${deviceMap.device.defaultIcon}",
		notes: "${settings['devInfNotes']}",
		id: "${selDev.id}",
        version: "${driverData.version}",
		controllerType: "${deviceMap.device.controllerType}"	
	]
  
	try{		
        params = [
            uri: "http://127.0.0.1:8080/device/update",
            headers: [
                "Content-Type": "application/x-www-form-urlencoded"
            ],
            followRedirects:false,
            body: bodyContent
		]
        if(debugEnabled)
    		log.debug params
    	httpPost(params) { resp ->  }
    }catch (e){
        log.error "$e"
    }
    state.resetDevInfPushed = true


}

String buildEventList() {
    eList = selDev.events([max:settings['devInfMaxEvent']])
    eTable = '<table class="listTable"><tr><th>Date</th><th>Name</th><th>Value</th><th>Unit</th><th>Description</th><th>Descriptive Text</th><th>Source</th></tr>'
    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS a")
    eList.each {
        eDate = sdfIn.parse("${it.date}")
        eTable += "<tr><td>${sdfOut.format(eDate)}</td><td>${it.name}</td><td>${it.value}</td><td>${it.unit?:''}</td><td>${it.description?.replace('<','&lt;')?:''}</td><td>${it.descriptionText?.replace('<','&lt;')?:''}</td><td>${it.source}</td></tr>"
    }
    eTable +='</table>'
    
    return eTable
    
}

String buildJobList(jList) {
    String removeIcon = "<i class='material-icons he-bin'></i>"
    jTable = '<table class="listTable"><tr><th>Name</th><th>Schedule</th><th>Next</th><th>Previous</th><th>Status</th><th></th></tr>'
    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS a")
    inx = 0
    jList.each {
        state["job$inx"] = [id:inx, method:"${it.handler}", nRun:"${it.nextRunTime}", type:("${it.schedule}" == "Once") ? "Once" : "Recur"] 
        delBtn = getInputElemStr(type:"button",name:"unsched$inx", title:"$removeIcon", color:"#ff0000", width:"26px")
        jTable+="<tr><td>${it.handler}</td><td>${it.schedule}</td><td>${sdfOut.format(sdfIn.parse(it.nextRunTime))}</td><td>${(it.prevRunTime) ? sdfOut.format(sdfIn.parse(it.prevRunTime)):""}</td><td>${it.status}</td><td>$delBtn</td></tr>"
        inx++
    }
    jTable+='<table>'
    return jTable
}

def getDriverData(dId){
    //log.debug "getDriverData($dId)"
    dMap = getDrivers().drivers
    def holdIt
    inx = 0
    dMap.each {
        if("${it.id}" == "$dId"){
        	holdIt = it     
        }
    }
    return holdIt
}

def getDrivers(){
	try{		
        params = [
            uri: "http://127.0.0.1:8080/device/drivers",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) 
            	log.debug "$resp.data"
            return resp.data
		}
    }catch (e){
        log.error "$e"
    }
    
}
                                 
ArrayList getRooms(){
	try{		
        params = [
            uri: "http://127.0.0.1:8080/room/listRoomsJson",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) 
            	log.debug "$resp.data"
            return resp.data
		}
    }catch (e){
        log.error "$e"
    }
    
}                                 

String reverseCamel(sVal){
    String result = '' 
    for(i=0;i<sVal.size();i++){
        if(i==0) {
        	result = sVal.charAt(i).toUpperCase()
        } else if(sVal.charAt(i) == '_' || sVal.charAt(i) == '-') {
            result += ' '
        } else if(Character.isUpperCase(sVal.charAt(i))){
            result += " ${sVal.charAt(i)}"
        } else {
            result += sVal.charAt(i)
        }            
    }
    return result
}

/*
 * UI Regions
 * 
 * Library to produce an html block with dragable/resizable regions
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
 *    
 */

import java.text.SimpleDateFormat
import groovy.transform.Field


library (
    base: "app",
    author: "Jean P. May Jr.",
    category: "UI",
    description: "Set of methods that allow the customization of the UI ",
    name: "uiRegions",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/refs/heads/main/libraries/uiRegions.groovy",
    version: "0.0.1",
    documentationLink: ""
)

String getRegion(regionName, regionTitle, regionContent){

	String region = """<div class="region" id="${regionName}">
            <div class="region-header" id="${regionName}-header">
                <div class="region-title">${regionTitle}</div>
				<span style='display:inline-table'>
					<span class="toggleBtn" onclick="toggleRegion(this)" ontouchstart="toggleRegion(this)">-</span>
					<span class="maxRstBtn" onclick="maxRstRegion(this)" ontouchstart="maxRstRegion(this)">&#9713;</span>
				</span>
            </div>
            <div class="region-content">
				${regionContent}
			</div>
            <div class="resize-handle se"></div>
        </div>
		"""
		
	return region

}

String getRegionsPage( regionsList, fullScreen ){
	// regionsList should be a list of map elements [regionName:regionContentString]
	String regionsMerged = ''
	String dragList = ''
	String defaultPos = ''
	int regionsInx = 0
	int l = 50
	int t = 0
	int w = 300
	int h = 250
	regionsList.each {
		regionsMerged += it.value
		if(regionsInx > 0) {
			dragList += ','
			defaultPos += ','
		}
		dragList += "'${it.key}'"
		defaultPos += "'${it.key}': { left: '${l}px', top: '${t}px', width: '${w}px', height: '${h}px', zIndex: '${regionsInx+1}' }"
		t+= 44
		l+= 30
		regionsInx++
	}
    
	String bodyHtml = """
<div>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background: linear-gradient(135deg, #ffffff 0%, #2596be 100%);
            min-height: 100vh;
            padding: 20px;
        }

        .regContainer {
			position: relative;
            width: 100%;
            height: calc(100vh - 40px);
        }

		.toggleBtn {
			width: 1em;
    		background: none;
		    border: none;
		    color: white;
    		cursor: pointer;
    		font-size: 16px;
    		padding: 0 5px;
    		line-height: 1;
    		font-weight: bold;
		}

		.toggleBtn:hover {
    		color:red;
		}
		
		.maxRstBtn {
			width: 1em;
    		background: none;
		    border: none;
		    color: white;
    		cursor: pointer;
    		font-size: 16px;
    		padding: 0 5px;
    		line-height: 1;
    		font-weight: bold;
		}

		.maxRstBtn:hover {
    		color:red;
		}

		.region.collapsed .region-content {
    		display: none;
		}

		.region.collapsed .resize-handle {
    		display: none;
		}

        .region {
			touch-action: none;
            position: absolute;
            background: white;
            border-radius: 8px;
			border: 1px solid gray;
            box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06);
            overflow: hidden;
            min-width: 200px;
            min-height: 44px;
			line-height:1.25em;
            transition: box-shadow 0.2s;
        }

		.region {
    		display: flex !important;
    		flex-direction: column !important;
    		padding: 0 !important;
		}

		.region-content {
    		flex: 1;
    		overflow: auto;
			padding-left:5px;
		}

        .region:hover {
            box-shadow: 0 10px 15px rgba(0, 0, 0, 0.15), 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .region.active {
            box-shadow: 0 20px 25px rgba(0, 0, 0, 0.2), 0 10px 10px rgba(0, 0, 0, 0.15);
        }

        .region.collapsed {
            height: 44px !important;
        }
		
		.region.fullScreen {
            top: 0px !important;
			height: 95vh !important;
			min-width: 80vw;
			left: 0px !important;
        }

        .region-header {
			-webkit-user-select: none;
			touch-action: none;
            background: linear-gradient(135deg, #2596be 0%, #bbbbcc 100%);
            color: white;
            padding: 0px 15px;
            cursor: move;
            user-select: none;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

		
		.region-header-dirty {
			background: linear-gradient(135deg, #ff9625 0%, #ccbbbb 100%);
		}

        .region-title {
            font-weight: 600;
            font-size: 14px;
        }

		.region-subheader {
			font-size:16px;
			font-weight:bold;
			text-decoration:underline;
		}

		.region-content {
            padding: 0px 15px;
            height: calc(100% - 44px);
            overflow: auto;
            transition: opacity 0.2s;
        }

        .resize-handle {
			touch-action: none;
            position: absolute;
            background: transparent;
        }

        .resize-handle.se {
            bottom: 0;
            right: 0;
            width: 44px;
            height: 44px;
            cursor: nwse-resize;
        }

        .resize-handle.se::after {
            content: '';
            position: absolute;
            bottom: 2px;
            right: 2px;
            width: 12px;
            height: 12px;
            border-right: 2px solid #cbd5e0;
            border-bottom: 2px solid #cbd5e0;
        }

        .reset-btn {
            //position: fixed;
            bottom: 20px;
            //right: 20px;
            background: white;
            color: #667eea;
            border: none;
            padding: 10px 20px;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            transition: all 0.2s;
        }

        .reset-btn:hover {
            background: #667eea;
            color: white;
            transform: translateY(-2px);
            box-shadow: 0 6px 8px rgba(0, 0, 0, 0.15);
        }



    </style>
	<script>
		function toggleRegion(btn) {
			event.stopPropagation()
		    const region = document.getElementById(btn.parentElement.parentElement.parentElement.id);
			if(region.classList.contains('collapsed')){
				//alert("case collasped")
				btn.innerText = '-';
				region.classList.remove('collapsed');
				savePos()
			} else { 
				//alert("case expanded")
				btn.innerText = '+';
				region.classList.add('collapsed');
				region.classList.remove('fullScreen');
				savePos();
			}
		}
		
		function maxRstRegion(btn) {
			event.stopPropagation()
		    const region = document.getElementById(btn.parentElement.parentElement.parentElement.id);
			if(region.classList.contains('fullScreen')){
				region.classList.remove('fullScreen');
				savePos()
			} else { 
				region.classList.add('fullScreen');
				region.classList.remove('collapsed');
				savePos();
			}
		}

		function savePos(){
			saveStr = '';
			const regionList = [${dragList}];
			for(i=0;i<regionList.length;i++){
				tRegion = document.getElementById(regionList[i]);
				const collapsed = tRegion.classList.contains('collapsed') ? '1' : '0';
				const maximized = tRegion.classList.contains('fullScreen') ? '1' : '0';
				saveStr += regionList[i]+':{'+tRegion.style.left+','+tRegion.style.top+','+tRegion.style.width+','+tRegion.style.height+','+tRegion.style.zIndex+ ',' + collapsed+','+maximized+'};';
				//alert("saving "+regionList[i]+":"+collapsed)
			}
			document.getElementById('settings[savePos]').value = saveStr;
			changeSubmit(document.getElementById('settings[savePos]'))
		}
						

		function parseStr(parmStr){
			items=parmStr.split(';')
			for(i=0;i<items.length-1;i++){
				id=items[i].substring(0,items[i].indexOf(':'))
				xyVals=items[i].substring(items[i].indexOf('{')+1,items[i].indexOf('}')).split(',');
				elem=document.getElementById(id);
				elem.style.left = xyVals[0];
				elem.style.top = xyVals[1];
				elem.style.width = xyVals[2];
				elem.style.height = xyVals[3];
				elem.style.zIndex = xyVals[4];
				maxZIndex = Math.max(maxZIndex, parseInt(xyVals[4]) || 0);
				// Restore collapsed state if saved
        		if (xyVals[5] == '1') {
            		elem.classList.add('collapsed');
	            	const btn = elem.querySelector('.toggleBtn');
    	        	if (btn) btn.textContent = '+';
        		} else {
            		elem.classList.remove('collapsed');
            		const btn = elem.querySelector('.toggleBtn');
		           	if (btn) btn.textContent = '−';
    	    	}
        		if (xyVals[6] == '1') {
            		elem.classList.add('fullScreen');
					elem.classList.remove('collapsed');
				} else {
            		elem.classList.remove('fullScreen');
    	    	} 
			}


		}

        draggableRegions = [${dragList}];
        var maxZIndex = ${regionsInx};
        
        // Default positions for initial load

		var defaultPositions = {
			${defaultPos}
        };

        // Bring region to front
        function bringToFront(region) {
            maxZIndex++;
            region.style.zIndex = maxZIndex;
        }

        // Load saved states on page load 
        //loadRegionState();//- only runs if no saved setting value
        function loadRegionState() {
            draggableRegions.forEach(regionId => {
                region = document.getElementById(regionId);
                defaults = defaultPositions[regionId];
                region.style.left = defaults.left;
                region.style.top = defaults.top;
                region.style.width = defaults.width;
                region.style.height = defaults.height;
                region.style.zIndex = defaults.zIndex;
            });
        }
      
	draggableRegions.forEach(regionId => {
			const region = document.getElementById(regionId);
			const header = region.querySelector('.region-header');
			const resizeHandle = region.querySelector('.resize-handle.se');

			let isDragging = false;
			let isResizing = false;
			let currentX, currentY, initialX, initialY;
			let initialWidth, initialHeight, resizeStartX, resizeStartY;

			function getClient(e) {
				return e.touches ? e.touches[0] : e;
			}

			region.addEventListener('mousedown', () => bringToFront(region));
			region.addEventListener('touchstart', () => bringToFront(region), { passive: true });

			function onDragStart(e) {
				if (e.target === resizeHandle || resizeHandle.contains(e.target)) return;
				e.preventDefault();
				isDragging = true;
				const client = getClient(e);
				initialX = client.clientX - (parseInt(region.style.left) || 0);
				initialY = client.clientY - (parseInt(region.style.top) || 0);
				region.classList.add('active');
				bringToFront(region);
			}

			function onDragMove(e) {
				if (!isDragging) return;
				e.preventDefault();
				const client = getClient(e);
				currentX = client.clientX - initialX;
				currentY = client.clientY - initialY;
				region.style.left = currentX + 'px';
				region.style.top = currentY + 'px';
			}

			function onDragEnd() {
				if (isDragging) { isDragging = false; savePos(); }
			}

			header.addEventListener('mousedown', onDragStart);
			header.addEventListener('touchstart', onDragStart, { passive: false });
			document.addEventListener('mousemove', onDragMove);
			document.addEventListener('touchmove', onDragMove, { passive: false });
			document.addEventListener('mouseup', onDragEnd);
			document.addEventListener('touchend', onDragEnd);

			function onResizeStart(e) {
				e.stopPropagation();
				e.preventDefault();
				isResizing = true;
				const client = getClient(e);
				resizeStartX = client.clientX;
				resizeStartY = client.clientY;
				initialWidth = region.offsetWidth;
				initialHeight = region.offsetHeight;
				region.classList.add('active');
			}

			function onResizeMove(e) {
				if (!isResizing) return;
				e.preventDefault();
				const client = getClient(e);
				const width = initialWidth + (client.clientX - resizeStartX);
				const height = initialHeight + (client.clientY - resizeStartY);
				if (width > 200) region.style.width = width + 'px';
				if (height > 100) region.style.height = height + 'px';
			}

			function onResizeEnd() {
				if (isResizing) { isResizing = false; savePos(); }
			}

			resizeHandle.addEventListener('mousedown', onResizeStart);
			resizeHandle.addEventListener('touchstart', onResizeStart, { passive: false });
			document.addEventListener('mousemove', onResizeMove);
			document.addEventListener('touchmove', onResizeMove, { passive: false });
			document.addEventListener('mouseup', onResizeEnd);
			document.addEventListener('touchend', onResizeEnd);
	});
	</script>
    <div class='regContainer' id='container'>
		${inputHiddenElem(name:'savePos', type:'hidden', width:'1em', radius:'12px', background:'#2596be', title:'', submitOnChange:true, defaultValue:'')}
		${regionsMerged}
    </div>
</div>
"""
    if(settings["savePos"])
    	posStr = "<script>parseStr('${settings["savePos"]}');</script>"
    else 
    	posStr = "<script>loadRegionState(); </script>"
    bodyHtml+=posStr
	if(fullScreen)
		return bodyHtml + fullScrn
	else
		return bodyHtml
}

String inputHiddenElem(HashMap opt) {
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
    if(opt.type == 'hidden'){
        opt.type='text'
        typeAlt = 'hidden'
        computedStyle += 'visibility:hidden;'
    }        
        
    if (opt.float) computedStyle +="float:${opt.float};"
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
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\">"
    if(typeAlt != 'hidden')
    	retVal +="<div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div>"
    retVal +="</div></div></div>"
    return retVal
}

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
*	23May2025							Add device.<driverName> to capability
*   07Oct2025							Added textarea uiType
*	20Feb2026							Minor enhancements and added 'hidden' as a valid input field type
*	06Mar2026							Fixed mis-matched <div></div> in capability element
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
    version: "0.0.9",
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
	case "hidden":
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
	case "textarea":
	   return inputTarea(opt)
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
    if(opt.type == 'hidden'){
        opt.type='text'
        typeAlt = 'hidden'
        computedStyle += 'visibility:hidden;'
    }        
        
    if (opt.float) computedStyle +="float:${opt.float};"
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
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\">"
    if(typeAlt != 'hidden')
    	retVal +="<div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div>"
    retVal +="</div></div></div>"
    return retVal
}


/*****************************************************************************
* Returns a string that will create an textArea element for an app -         *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or 	 *
*		special characters					     							 *
*	type - (required) input type					     					 *
*	title - displayed description for the input element		     			 * 
*	width - CSS descriptor for field width									 *
*	height - CSS descriptor for field height								 *
*	background - CSS color descriptor for the input background color     	 *
*	color - CSS color descriptor for text color			     				 *
*	fontSize - CSS text size descriptor				     					 *
*	defaultValue - default for the field				     				 *
*	radius - CSS border radius value (rounded corners)						 *
*	hoverText - Text to display as a tool tip								 *
*****************************************************************************/
String inputTarea(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    typeAlt = opt.type
        
    String computedStyle = 'resize:both;'
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.height) computedStyle += "height:${opt.height};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
	if(opt.radius) computedStyle += "border-radius:${opt.radius};"
        if (opt.float) computedStyle +="float:${opt.float};"
    
    
    if(opt.hoverText && opt.hoverText != 'null'){  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon([name:'fa-circle-info'])}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
    }
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='false'></div>"
	retVal+="<div class='mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield' style='' data-upgraded=',MaterialTextfield'><div style='display: inline-flex'>"
    retVal+="<label for='settings[${opt.name}]' class='control-label'>${opt.title}</label></div><div><textarea type='textarea' name='settings[${opt.name}]' class='form-control submitOnChange' style='${computedStyle}'  placeholder='Click to set' id='settings[${opt.name}]' >${opt.defaultValue}</textarea></div>"
    //retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\"><div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div></div></div></div>"
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
    if (opt.float) computedStyle +="float:${opt.float};"
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

    retVal += "<button type='button' class='btn btn-lg btn-block btn-device' style='border-radius:${opt.radius};border:0px;text-align:left; width:100%; min-width:${opt.width}' data-toggle='modal' data-target='#deviceListModal' "
    retVal += "data-capability='${opt.type}' data-elemname='${opt.name}' data-multiple='${opt.multiple}' data-ignore=''>"
    	retVal += "<span style='white-space:pre-wrap;'>${opt.title}</span><br><div style='${computedStyle}'>"
	retVal += "<span id='${opt.name}devlist' class='device-text' style='text-align: left;'>${dList}</span></div></button>"
    
	retVal += "<input type='hidden' name='settings[${opt.name}]' class='form-control submitOnChange' value='${idList}' placeholder='Click to set' id='settings[${opt.name}]'>"
	retVal += "<input type='hidden' name='deviceList' value='${opt.name}'><div class='device-list' style='display:none'>"
	retVal += "<div id='deviceListModal' style='border:1px solid #ccc;padding:8px;max-height:300px;overflow:auto;min-width:${opt.width}'><div class='checkAllBoxes my-2'>"
	retVal += "<label class='mdl-checkbox mdl-js-checkbox mdl-js-ripple-effect checkall mdl-js-ripple-effect--ignore-events is-upgraded' id='${opt.name}-checkall' for='${opt.name}-checkbox-0' data-upgraded=',MaterialCheckbox,MaterialRipple'>"
    	retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"${opt.name}-checkall\").classList.contains(\"is-checked\")){document.getElementById(\"${opt.name}-checkall\").classList.remove(\"is-checked\");}else{document.getElementById(\"${opt.name}-checkall\").classList.add(\"is-checked\");}}</script>"    
    retVal += "<input type='checkbox' class='mdl-checkbox__input checkboxAll' id='${opt.name}-checkbox-0' onclick='toggleMe${opt.name}()'><span class='mdl-checkbox__label'>Toggle All On/Off</span>"
	retVal += "<span class='mdl-checkbox__focus-helper'></span><span class='mdl-checkbox__box-outline'><span class='mdl-checkbox__tick-outline'></span></span>"
	retVal += "<span class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple'></span></span></label></div>"
	retVal += "<div id='${opt.name}-options' class='modal-body' style='overflow:unset'></div></div></div>"
	retVal += "<div class='mdl-button mdl-js-button mdl-button--raised pull-right device-save' data-upgraded=',MaterialButton' style='${computedStyle};width:6em;min-width:6em;'>Update</div></div>"
    
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
    String computedStyle = opt.style ? opt.style:''
    if (opt.float) computedStyle +="float:${opt.float};"
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
    retVal += "<div style='${computedStyle}'><select id='settings[${opt.name}]' ${mult} name='settings[${opt.name}]' class='selectpicker form-control mdl-switch__input submitOnChange SumoUnder' placeholder='Click to set' data-default='' tabindex='-1' style='${computedStyle}'>"
    ArrayList selOpt = []
	if(settings["${opt.name}"]){
        if("${settings["${opt.name}"].class}" == 'class java.lang.String' || "${settings["${opt.name}"].class}" == 'class org.codehaus.groovy.runtime.GStringImpl'){
           	selOpt.add("${settings["${opt.name}"]}")
        } else {
        	settings["${opt.name}"].each{
            	selOpt.add("$it")
        	}
       }
    } else if(opt.defaultValue) {
        if("${opt.defaultValue.class}" == 'class java.lang.String' || "${opt.defaultValue.class}" == 'class org.codehaus.groovy.runtime.GStringImpl')
        	selOpt.add("${opt.defaultValue}")
        else {
 			opt.defaultValue.each {
              	selOpt.add("$it")
            }
        }
    }
    if(mult != 'multiple') retVal+="<option value=''>Click to set</option>"
    opt.options.each{ option -> 
        colonCount = "$option".count(':')
        if(colonCount == 1 ){
            optSplit = "$option".replace('[','').replace(']','').split(':')
            optVal = optSplit[0]
            optDis = optSplit[1]
        } else if (colonCount > 1 ) {
            ord = colonCount%2
            inx = ordinalIndexOf(option.toString(),':',ord)
            if(inx > -1){
            	optVal = "$option".substring(0,inx)
            	optDis = "$option".substring(inx+1,)
            }
        } else {
            optVal = option
            optDis = option
        }
        sel = ' '
        selOpt.each{
            //	log.debug "|$it| |$optVal| ${"$it".trim() == "$optVal".trim()}"
            if("$it".trim() == "$optVal".trim() ) 
            	sel = 'selected'
        }
        retVal += "<option value='${optVal}' ${sel}>${optDis}</option>"
    }
    retVal+= "</select></div></div></div>"
    return retVal
}

int ordinalIndexOf(String str, String substr, int n) {
    int pos = -1
    (0..n).each { 
        if (pos != -1 || it == 0) pos = str.indexOf(substr, pos + 1) 
    }
    pos
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
    if (opt.float) computedStyle +="float:${opt.float};"
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
    if (opt.float) computedStyle +="float:${opt.float};"
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
    if (opt.float) computedStyle +="float:${opt.float};"
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.icon) opt.icon = [name:'fa-circle-info']
    if(opt.hoverText && opt.hoverText != 'null')  
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon(opt.icon)}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
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
    if (opt.float) computedStyle +="float:${opt.float};"
    if(opt.width) computedStyle += "width:${opt.width};min-width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.icon) opt.icon = [name:'fa-circle-info']
    if(opt.destPage) {
    	inx = appLocation().lastIndexOf("/")
    	dest = appLocation().substring(0,inx)+"/${opt.destPage}"
    } else if(opt.destUrl) {
    	dest=opt.destUrl
    }
	if(opt.hoverText && opt.hoverText != 'null')  
    opt.title ="${opt.title}<div class='tTip'> ${btnIcon(opt.icon)}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
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
    if (opt.float) computedStyle +="float:${opt.float};"
    if(!opt.width) opt.width = '100%'
    computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    if(!opt.icon) opt.icon = [name:'fa-circle-info']
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
    	opt.title ="${opt.title}<div class='tTip'> ${btnIcon(opt.icon)}<span class='tTipText' style='width:${opt.hoverText.size()/2}em'>${opt.hoverText}</span></div>"
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

/*****************************************************************************
* Code sample that returns a string that will create a standard HE table     *
*****************************************************************************/
/*
String listTable() {
    ArrayList<String> tHead = ["","Disable","Name","Device","Attributes","Interval","Output File","<i style='font-size:1.125rem' class='material-icons he-bin'></i>"]
    String X = "<i class='he-checkbox-checked'></i>"
    String O = "<i class='he-checkbox-unchecked'></i>"
    String settingsIcon = "<i class='material-icons app-column-info-icon' style='font-size: 24px;'>settings_applications</i>"
    String removeIcon = "<i class='material-icons he-bin'></i>"

                 
    String str = "$tableStyle<div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style='border-left:2px solid black;border-top:2px solid black;'>" +
            "<thead><tr style='border-bottom:2px solid black'>"
    tHead.each { str += "<th><strong>${it}</strong></th>" }
    str += "</tr></thead>"
  
    ...
}
*/
@Field static String fullScrn = "<script>document.getElementById('divSideMenu').setAttribute('style','display:none !important');document.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');document.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');document.getElementById('divMainUIFooter').setAttribute('style','display:none !important');contentHeight = Math.round(window.innerHeight * 1.2);document.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');document.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');</script><style>overflow-y: scroll !important;</style>"	
@Field static String ttStyleStr = "<style>.tTip {display:inline-block;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;text-align:left;}</style>"
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;} tr {border-right:2px solid black;}</style>"
