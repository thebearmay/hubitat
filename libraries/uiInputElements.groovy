/*
*
* Set of methods for UI elements
*
*/

library (
    base: "app",
    author: "Jean P. May Jr.",
    category: "UI",
    description: "Set of methods that allow the customization of the INPUT UI Elements",
    name: "uiInputElements",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/uiInputElements.groovy",
    version: "0.0.1",
    documentationLink: ""
)

/*****************************************************************************
* Returns a string that will create an input element for an app - limited to *
* text, password, and number inputs currently                                *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or *
*		special characters					     *
*	type - (required) input type, 'text', 'password', or 'number' only   *
*	title - displayed description for the input element		     * 
*	width - CSS descriptor for field width				     *
*	background - CSS color descriptor for the input background color     *
*	color - CSS color descriptor for text color			     *
*	fontSize - CSS text size descriptor				     *
*	multiple - true/<false>						     *
*	defaultValue - default for the field				     *
*****************************************************************************/

String inputItem(HashMap opt) {
    if(!opt.name || !opt.type) return "Error missing name or type"
    if(opt.type != 'number' && opt.type != 'text' && opt.type != 'password'){
        log.warn "${opt.type} type not currently supported - using text"
        opt.type = 'text'
    }
    if(settings[opt.name] != null) opt.defaultValue = settings[opt.name]
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='false'></div>"
	retVal+="<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield has-placeholder is-dirty is-upgraded' style='' data-upgraded=',MaterialTextfield'>"
	retVal+="<label for='settings[$opt.name]' class='control-label'>$opt.title</label><div class='flex'><input type='$opt.type' name='settings[$opt.name]' class='mdl-textfield__input submitOnChange' style='$computedStyle' value='$opt.defaultValue' placeholder='Click to set' id='settings[$opt.name]'>"
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\"><div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div></div></div></div>"
    return retVal
}

/*****************************************************************************
* Returns a string that will create an input capability element for an app   *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to store the input as a setting, no spaces or *
*		special characters					     *
*	type - (required) capability type, 'capability.<capability or *>'    *
*	title - displayed description for the input element		     * 
*	width - CSS descriptor for field width				     *
*	background - CSS color descriptor for the input background color     *
*	color - CSS color descriptor for text color			     *
*	fontSize - CSS text size descriptor				     *
*	multiple - true/<false>						     *
*****************************************************************************/

String inputCap(HashMap opt) {
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"    
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

	String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='${opt.multiple}'></div>"
	retVal += "<div class='capability ${capAlt} mdl-cell mdl-cell--4-col' style='margin: 8px 0; $computedStyle'>"//width: ${opt.width}'>"
	retVal += "<button type='button' class='btn btn-default btn-lg btn-block device-btn-filled btn-device mdl-button--raised mdl-shadow--2dp' style='text-align:left; width:100%;' data-toggle='modal' data-target='#deviceListModal' "
	retVal += "data-capability='${opt.type}' data-elemname='${opt.name}' data-multiple='${opt.multiple}' data-ignore=''>"
    	retVal += "<span style='white-space:pre-wrap;'>${opt.title}</span><br>"
	retVal += "<span id='${opt.name}devlist' class='device-text' style='text-align: left;'>${dList}</span></button>"
	retVal += "<input type='hidden' name='settings[${opt.name}]' class='form-control submitOnChange' value='${idList}' placeholder='Click to set' id='settings[${opt.name}]'>"
	retVal += "<input type='hidden' name='deviceList' value='${opt.name}'><div class='device-list' style='display:none'>"
	retVal += "<div id='deviceListModal' style='border:1px solid #ccc;padding:8px;max-height:300px;overflow:auto'><div class='checkAllBoxes my-2'>"
	retVal += "<label class='mdl-checkbox mdl-js-checkbox mdl-js-ripple-effect checkall mdl-js-ripple-effect--ignore-events is-upgraded' id='${opt.name}-checkall' for='${opt.name}-checkbox-0' data-upgraded=',MaterialCheckbox,MaterialRipple'>"
    	retVal += "<script>function toggleMe${opt.name}(){if(document.getElementById(\"${opt.name}-checkall\").classList.contains(\"is-checked\")){document.getElementById(\"${opt.name}-checkall\").classList.remove(\"is-checked\");}else{document.getElementById(\"${opt.name}-checkall\").classList.add(\"is-checked\");}}</script>"    
	retVal += "<input type='checkbox' class='mdl-checkbox__input checkboxAll' id='${opt.name}-checkbox-0' onclick='toggleMe${opt.name}()'><span class='mdl-checkbox__label'>Toggle All On/Off</span>"
	retVal += "<span class='mdl-checkbox__focus-helper'></span><span class='mdl-checkbox__box-outline'><span class='mdl-checkbox__tick-outline'></span></span>"
	retVal += "<span class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center' data-upgraded=',MaterialRipple'><span class='mdl-ripple'></span></span></label></div>"
	retVal += "<div id='${opt.name}-options' class='modal-body' style='overflow:unset'></div></div>"
	retVal += "<div class='mdl-button mdl-js-button mdl-button--raised pull-right device-save' data-upgraded=',MaterialButton'>Update</div></div></div>"
    
    return retVal
}



/*****************************************************************************
* Returns a string that will create an button element for an app 	     *
*                                                                            *
* HashMap fields:                                                            *
*	name - (required) name to identify the button, no spaces or 	     *
*		special characters					     *
*	title - (required) button label					     *
*	width - CSS descriptor for field width				     *
*	background - CSS color descriptor for the input background color     *
*	color - CSS color descriptor for text color			     *
*	fontSize - CSS text size descriptor				     *
*	radius - CSS border radius descriptor (corner rounding)		     *
*****************************************************************************/

String buttonLink(HashMap opt) { 
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    if(opt.fontSize) computedStyle += "font-size:${opt.fontSize};"
    if(opt.radius) computedStyle += "border-radius:${opt.radius};"
    return "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='$computedStyle'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

/*****************************************************************************
* Returns a string that will create an button icon element for an app from   *
*	the materials-icon font						     *
*                                                                            *
*	name - (required) name of the icon to create			     *
*****************************************************************************/

String btnIcon(String name) {
    return "<span class='p-button-icon p-button-icon-left pi " + name + "' data-pc-section='icon'></span>"
}


/*****************************************************************************
* Code sample that returns a string that will create a standard HE table     *
*****************************************************************************/

String listTable() {
    ArrayList<String> tHead = ["","Disable","Name","Device","Attributes","Interval","Output File","<i style='font-size:1.125rem' class='material-icons he-bin'></i>"]
    String X = "<i class='he-checkbox-checked'></i>"
    String O = "<i class='he-checkbox-unchecked'></i>"
    String settingsIcon = "<i class='material-icons app-column-info-icon' style='font-size: 24px;'>settings_applications</i>"
    String removeIcon = "<i class='material-icons he-bin'></i>"


    String str = "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px } tr {border-right:2px solid black;}" +
            "</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style='border-left:2px solid black;border-top:2px solid black;'>" +
            "<thead><tr style='border-bottom:2px solid black'>"
    tHead.each { str += "<th><strong>${it}</strong></th>" }
    str += "</tr></thead>"
  
    ...
}
