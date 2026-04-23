/*
 * Regions Demo
 * 
 *      Demo application to for the uiRegions library
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
 *    20260423			thebearmay				code cleanup, add app filter 
 */
static String version()	{  return '0.0.2'  }
import groovy.transform.Field

//#include thebearmay.uiRegions


definition (
	name: 			"HE Regions Shell - UI", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"HE Regions Shell - UI",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/heRegionShell.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      "",
    menu: 			"Apps"
) 

preferences {
    page name: "_mainPage"
    //page name: "_appsList"

}


def installed() {
    state?.isInstalled = true
    initialize()
}

def updated(){
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnabled) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def _mainPage(){
    //<h2 style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></h2>
    dynamicPage (name: "_mainPage", title: "", install: true, uninstall: true) {
        section (name:'cPageHndl', title:''){
            if(!settings.appFilter){
            	app.updateSetting('appFilter',[type:'enum',value:'All'])
                state.holdFilter = 'All'
            }
            if(!state.regionMax) state.regionMax = 3
            if(state.regionsList) 
            	regionsList = state.regionsList  
            else
                regionsList = [:]
            
            if(!regionsList.containsKey('region-1')){
            	r1Content = _buildMenu()
            	r1 = getRegion('region-1','Menu',"$r1Content")
            	regionsList.put('region-1', r1)
            }
            
            if(state?.holdFilter != settings.appFilter) {
                state.special = 'applications'
            }
            
            if(state.special == 'applications'){
                state.regionMax = state.regionMax++
            	r2Content =_appsList()
            	r2 = getRegion('region-2', 'Applications', "$r2Content")
            	regionsList.put('region-2', r2)
                state.remove('special')
            } else if(state.special == 'about'){
                state.regionMax = state.regionMax++
                rContent = "<p style='background-color:#e6ffff;border-radius:15px'>${app.getLabel()}<span style='font-size:xx-small'>&nbsp;v${version()}</span></p>"
                r=getRegion("region-${state.regionMax}",'About',"$rContent")
				regionsList.put("region-${state.regionMax}", r)
                state.remove('special')
            } else if(state.special) {
            	log.debug "Unknown Special Request - ${state.special}"
                state.remove('special')
            }
                              
            if(state.closeApp){
                state.remove('closeApp')
                paragraph "<script>location.href = '/';</script>"
            }
            
            if(state?.newAppLaunch > '0'){
                addAppRegion(state.newAppLaunch)
                state.remove('newAppLaunch')
            }
            if(closeReg != null) {
                removeRegion(closeReg)
            }
            if(state?.menuLaunch) {
                addMenuRegion(state.menuLaunch)
                state.remove('menuLaunch')
            }
            state.regionsList = regionsList
            pContent = getRegionsPage(regionsList, true)
            paragraph pContent
            //pname = "regionShellWork${app.id}.htm"
    		//uploadHubFile ("$pname",pContent.getBytes("UTF-8"))

                      
        }
    }
}

void removeRegion(regId){
    regionsList = state.regionsList
    app.removeSetting('closeReg')
    regionsList.remove(regId)
    state.regionsList = regionsList
    ArrayList saveList = savePos.split(';')
    String updReg = ''
    saveList.each {
       // log.debug "${it.substring(0,it.indexOf(':'))} $regId"
        if(it.substring(0,it.indexOf(':')) != regId)
           updReg += "$it;"
    }
    app.updateSetting('savePos', [type:'text',value:"$updReg"])
    
}

def addAppRegion(appId){
    regionsList = state.regionsList
    state.regionMax = state.regionMax++
    fContent = frameWrap("frame${state.regionMax}","/installedapp/configure/$appId")
    newReg = getRegion("region-${state.regionMax}", "Application-$appId", "$fContent")
    regionsList.put("region-${state.regionMax}", newReg)

    state.regionsList = regionsList
}

def addMenuRegion(refStr){
    regionsList = state.regionsList
   	state.regionMax = state.regionMax++
    refStr=refStr.replace('|','/')
    fContent = frameWrap("frame${state.regionMax}","http://${location.hub.localIP}:8080/$refStr")
    newReg = getRegion("region-${state.regionMax}", "$refStr", "$fContent")
    regionsList.put("region-${state.regionMax}", newReg)

    state.regionsList = regionsList
}

String _appsList(){
	String dList = "<style>td{border:1px gray dotted;}</style><table>"
    dList += "<tr><td>"+getInputElemStr(name:"appFilter", type:'enum', radius:'12px', style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:'Filter', width:'15em',options:['All','Apps','Automations','Integrations'],defaultValue:"${settings['appFilter']}" )+"</td></tr>"
    appList.each {
		dList+= "<tr><td>"+getInputElemStr(name:"app-${it.key}", type:'button', style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"${it.value}", noBorder:true )+"</td></tr>"
    }
    dList+="</table>"
    state.holdFilter = settings['appFilter']
    return dList
}


ArrayList getAppList(){
        ArrayList appList = []
    	try{		
        params = [
            uri: "http://127.0.0.1:8080/hub2/appsList",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp ->
            if(debugEnabled) 
            	log.debug "${resp.data.apps}"
            resp.data.apps.each { apps ->
            	fType = ''
                if(!apps.data.user) { 
                	resp.data.systemAppTypes.each{
                        if(it.id == apps.data.appTypeId)
                        	fType = it.menu
                	}
                } else {                	 
                    resp.data.userAppTypes.each{
                    	if(it.id == apps.data.appTypeId)
                        	fType = it.menu
                    }
                }
               
                if(appFilter == 'All' || appFilter == fType ){
                	HashMap wMap = [key:"${apps.id}",value:"${apps.data.name}"]
                    appList.add(wMap)
                }
            }
		}
    }catch (e){
        log.error "$e"
    }
    //log.debug appList
    return appList
    
}

String _buildMenu(){
	menuStr=getInputElemStr(name:"href-room|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-bed-front'></i> Rooms ")
    menuStr+=getInputElemStr(name:"href-dashboard|select", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-objects-column'></i> Dashboard ")
    menuStr+=getInputElemStr(name:"spec-applications", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-toolbox'></i> Apps ")
    menuStr+=getInputElemStr(name:"href-device|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-lightbulb'></i> Devices ")
    menuStr+=getInputElemStr(name:"href-hub|edit", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-sliders-up'></i> Settings ")
    menuStr+=getInputElemStr(name:"href-logs", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-memo-circle-info'></i> Logs ")+"<hr>"
    if(displayDevMenu()){
        menuStr+=getInputElemStr(name:"href-driver|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-lightbulb-gear'></i> Driver Code ")
    	menuStr+=getInputElemStr(name:"href-app|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-gear-complex-code'></i> App Code ")
        menuStr+=getInputElemStr(name:"href-library|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-books'></i> Library Code ")
        menuStr+=getInputElemStr(name:"href-bundle|list", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-folder-gear'></i> Bundles ")
    }
    menuStr+='<hr>'+getInputElemStr(name:"spec-about", type:'button', noBorder:true,style:'background-image:linear-gradient(to right, #a0d8ef, #b2f2e4, #ffffff);', color:'#000000', title:"<i class='fa-regular fa-circle-info'></i> About ")+"<hr><br>"
    menuStr+='<br>'+getInputElemStr(name:"closeApp", type:'button',width:'12em', radius:'12px',background:'#ff0707', color:'#000000', title:" [X] Close Shell ")
    return menuStr
}

boolean displayDevMenu() {
    boolean retVal = false 
	try{		
        params = [
            uri: "http://127.0.0.1:8080/hub2/hubData",
            headers: [
                "Accept": "application/json"
            ]
		]
        if(debugEnabled) 
        	log.debug "$params"
        httpGet(params){ resp -> 
            if(resp.data.showDeveloperMenu)
            	retVal = true
        }
    } catch (e) {
    }
    
    return retVal
}

def appButtonHandler(btn) {
    switch(btn) {
        case { it.startsWith('href-') }:
            state.menuLaunch = btn.substring(5,)
        	break
        case { it.startsWith('app-') }:
        	//addAppRegion(btn.substring(4,))
            state.newAppLaunch = btn.substring(4,)
        	break
        case { it.startsWith('spec-') }:
        	//addAppRegion(btn.substring(4,))
            state.special = btn.substring(5,)
        	break        
        case 'closeApp':
            state.closeApp = true
        	break
        default: 
            break
    }
}

String frameWrap (_frameId, _frameSrc) {
	String frameMod = """<style>iframe {height:85vh; width:90vw}</style>
		<script>
		iframe${_frameId} = document.getElementById("${_frameId}");
		iframe${_frameId}.addEventListener("load", _frameMod${_frameId});
		iframe${_frameId}.contentWindow.addEventListener('resize', _frameMod${_frameId});
		function _frameMod${_frameId}() {
  			iframe${_frameId}.contentDocument.getElementById('divSideMenu').setAttribute('style','display:none !important');
  			iframe${_frameId}.contentDocument.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');
			iframe${_frameId}.contentDocument.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');
			iframe${_frameId}.contentDocument.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white;');
  			contentHeight = Math.round(window.innerHeight * 1.2);
  			iframe${_frameId}.contentDocument.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');
  			iframe${_frameId}.contentDocument.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');
		}
		</script>"""
    return "$frameMod<iframe src='$_frameSrc' id='${_frameId}'></iframe>"
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
            <div class="region-header">
                <div class="region-title">${regionTitle}</div>
				<span style='display:inline-table'>
					<span class="toggleBtn" onclick="toggleRegion(this)" ontouchstart="toggleRegion(this)">-</span>
					<span class="maxRstBtn" onclick="maxRstRegion(this)" ontouchstart="maxRstRegion(this)">&#9713;</span>
					<span class="closeBtn" onclick="closeRegion(this)" ontouchstart="closeRegion(this)">&#x2612;</span>
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
	int l = 10
	int t = -20
	int w = 300
	int h = 250
	regionsList.each {
		regionsMerged += it.value
		if(regionsInx > 0) {
			dragList += ','
			defaultPos += ','
		}
		dragList += "'${it.key}'"
		defaultPos += "'${it.key}':{ left:'${l}px',top:'${t}px',width:'${w}px',height:'${h}px',zIndex: '${regionsInx+1}' }"
		regionsInx++
            if(!savePos) savePos = ''
            if(savePos.indexOf("${it.key}") == -1){
                saveStr = "${it.key}:{${l}px,${t}px,${w}px,${h}px,${regionsInx+1},0,0};"
                app.updateSetting('savePos', [type:'text',value:"$savePos$saveStr"])
            }
        t+= 44
		l+= 30
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

		.closeBtn {
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

		.closeBtn:hover {
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

		function closeRegion(btn) {
			//btn.parentNode.removeChild(btn)
			event.stopPropagation();
		    const region = document.getElementById(btn.parentElement.parentElement.parentElement.id);
			document.getElementById('settings[closeReg]').value = btn.parentElement.parentElement.parentElement.id;
			changeSubmit(document.getElementById('settings[closeReg]'));

		}	

		function savePos(){
			saveStr = '';
			const regionList = [${dragList}];
			//alert (regionList)
			for(i=0;i<regionList.length;i++){
				tRegion = document.getElementById(regionList[i]);
				const collapsed = tRegion.classList.contains('collapsed') ? '1' : '0';
				const maximized = tRegion.classList.contains('fullScreen') ? '1' : '0';
				saveStr += regionList[i]+':{'+tRegion.style.left+','+tRegion.style.top+','+tRegion.style.width+','+tRegion.style.height+','+tRegion.style.zIndex+ ',' + collapsed+','+maximized+'};';
				//alert("saving "+regionList[i]+":"+collapsed)
			}
			//alert(saveStr)
			document.getElementById('settings[savePos]').value = saveStr;
			changeSubmit(document.getElementById('settings[savePos]'));
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
    <div class='regContainer' id='regContainer'>
		${inputHiddenElem(name:'savePos', type:'hidden', width:'1em', radius:'12px', background:'#2596be', title:'', submitOnChange:true, defaultValue:'')}
		${regionsMerged}
		${inputHiddenElem(name:'closeReg', type:'hidden', width:'1em', radius:'12px', background:'#2596be', title:'', submitOnChange:true, defaultValue:'')}
    </div>
</div>
"""
     if(settings["savePos"])
    	bodyHtml+="<script>parseStr('${settings["savePos"]}');</script>"
    else 
        bodyHtml+="<script>loadRegionState();</script>"
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
@Field static String fullScrn = "<script>document.getElementById('divAppUIAppTitle').setAttribute('style','display:none !important');document.getElementById('divSideMenu').setAttribute('style','display:none !important');document.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');document.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');document.getElementById('divMainUIFooter').setAttribute('style','display:none !important');contentHeight = Math.round(window.innerHeight * 1.2);document.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');document.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');</script><style>overflow-y: scroll !important;</style>"


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
    if(opt.style) computedStyle += "$opt.style"
    if(opt.noBorder == true) computedStyle += "outline: none;box-shadow: none;text-align:left;"
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
//@Field static String fullScrn = "<script>document.getElementById('divSideMenu').setAttribute('style','display:none !important');document.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');document.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');document.getElementById('divMainUIFooter').setAttribute('style','display:none !important');contentHeight = Math.round(window.innerHeight * 1.2);document.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');document.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');</script><style>overflow-y: scroll !important;</style>"	
@Field static String ttStyleStr = "<style>.tTip {display:inline-block;}.tTip .tTipText {display:none;border-radius: 6px;padding: 5px 0;position: absolute;z-index: 1;}.tTip:hover .tTipText {display:inline-block;background-color:yellow;color:black;text-align:left;}</style>"
@Field static String tableStyle = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px; padding:8px 8px 8px 8px;white-space: nowrap;} tr {border-right:2px solid black;}</style>"
