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

@Field static String fullScrn = "<script>document.getElementById('divSideMenu').setAttribute('style','display:none !important');document.getElementById('divMainUIHeader').setAttribute('style','height: 0 !important;');document.getElementById('divMainUIContent').setAttribute('style','padding: 0 !important;');document.getElementById('divMainUIFooter').setAttribute('style','display:none !important');contentHeight = Math.round(window.innerHeight * 1.2);document.getElementById('divMainUIContentContainer').setAttribute('style', 'background: white; height: ' + contentHeight + 'px !important;');document.getElementById('divLayoutControllerL2').setAttribute('style', 'height: ' + contentHeight + 'px !important;');</script><style>overflow-y: scroll !important;</style>"	
