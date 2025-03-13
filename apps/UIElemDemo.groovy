/*
 * UI Elements Demo App
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
 *	----------		----------------	-------------------------------------------------------
 *	11Mar2025		thebearmay			Added Checkbox option
 *	13Mar2025							Added hoverText, code cleanup
 */

static String version()	{  return '0.0.3'  }
#include thebearmay.uiInputElements

definition (
	name: 			"UI Elements Demo", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"UI Elements Demo App",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/UIElemDemo.groovy",
    installOnOpen:  true,
	oauth: 			false,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
    page name: "page2"

}
mappings {
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
     app.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "UI Demo", install: true, uninstall: true) {
        section("Main"){
			String q1 = getInputElemStr( [name:"uiType", type:"enum", title:"<b>UI Type</b>", options:["text","number","decimal","date","time","password","color","enum","mode","capability.*","capability.switch","bool","checkbox","href","button","icon"], multiple:false, width:"15em", background:"#ADD8E6", radius:"15px"])
            String q2 =getInputElemStr( [name:"dTitle", type:"text", title:"<b>Title of UI Element</b>", width:"15em", background:"#ADD8E6", radius:"15px"])
            String q3 =getInputElemStr( [name:"tColor", type:"color", title:"<b>Text Color</b>", width:"15em", background:"#ADD8E6", radius:"15px"])
            String q4 =getInputElemStr( [name:"bColor", type:"color", title:"<b>Background Color</b>", width:"15em", background:"#ADD8E6", radius:"15px"])
            String q5 =getInputElemStr( [name:"rStr", type:"text", title:"<b>Radius Style String</b>", width:"15em", background:"#ADD8E6", radius:"15px", defaultValue:"15px"])
            String q6 =getInputElemStr( [name:"wStr", type:"text", title:"<b>Width Style String</b>", hoverText:"Hover Text Test", width:"15em", background:"#ADD8E6", radius:"15px", defaultValue:"15em"])
            String q7 =getInputElemStr( [name:"hText", type:"text", title:"<b>Tool Tip (Hover Text)</b>", width:"15em", background:"#ADD8E6", radius:"15px"])
            String tStr ="${ttStyleStr}<table><tr><td>$q1</td><td style='max-width:1em'>&nbsp;</td><td>$q2</td><td style='max-width:1em'>&nbsp;</td><td>$q3</td></tr>"
            tStr+= "<tr><td>$q4</td><td>&nbsp;</td><td>$q5</td><td>&nbsp;</td><td>$q6</td></tr>"
            tStr += "<tr><td>$q7</td></tr></table>"
            
            paragraph tStr
            if(uiType && dTitle && tColor && bColor && rStr){
                ArrayList optParse = []
                if(uiType == 'enum') eOpt = ['option1','option2', 'option3']
                if(uiType.contains('capability')) 
                	oMult=true
                else 
                    oMult=false
                String outPut
                if(uiType == 'icon') 
                	outPut =getInputElemStr([name:"${dTitle}", type:"${uiType}"])
                else if(uiType == 'href') 
                    outPut =getInputElemStr([name:"demo${uiType}",type:"${uiType}",title:"${dTitle}",destPage:"page2",width:"${wStr}",background:"${bColor}",color:"${tColor}",radius:"${rStr}", hoverText:"${hText}"])
                else 
                    outPut =getInputElemStr([name:"demo${uiType.replace('*','').replace('.','')}", type:"${uiType}", title:"${dTitle}", width:"${wStr}", background:"${bColor}", color:"$tColor",radius:"$rStr", multiple:oMult, options:eOpt, hoverText:"${hText}"])

                
                paragraph "<hr><h3>Result</h3>$outPut"

            }
        }
    }
}

def page2(){
    dynamicPage (name: "page2", title: "Page 2 Check", install: false, uninstall: false, next:mainPage) {
        section ("S1"){
            paragraph "Page Check"
            paragraph buttonHref([name:"gotoP1",title:"Home",destPage:"mainPage",width:'10em',background:"#AA11FF",color:"#FFFFFF",radius:"10px"])
        }
    }
}
