/*
*
* Set of methods for UI elements
*
*
*
*
*
*
*
*
*/



String input3 = inputItem([name:"testItem4", type:"number", title:'Input Title3', defaultValue:12, width:'20em', background:'green',color:'white'])
paragraph input3
...

String inputItem(HashMap opt) {
	if(!opt.name || !opt.type) return "Error missing name or type"
    if(opt.type != 'number' && opt.type != 'text'){
        log.warn "${opt.type} type not currently supported - using text"
        opt.type = 'text'
    }
    if(settings[opt.name] != null) opt.defaultValue = settings[opt.name]
    String computedStyle = ''
    if(opt.width) computedStyle += "width:${opt.width};"
    if(opt.background) computedStyle += "background-color:${opt.background};"
    if(opt.color) computedStyle += "color:${opt.color};"
    String retVal = "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='${opt.type}'><input type='hidden' name='${opt.name}.multiple' value='false'></div>"
	retVal+="<div class='mdl-cell mdl-cell--4-col mdl-textfield mdl-js-textfield has-placeholder is-dirty is-upgraded' style='' data-upgraded=',MaterialTextfield'>"
	retVal+="<label for='settings[$opt.name]' class='control-label'>$opt.title</label><div class='flex'><input type='$opt.type' name='settings[$opt.name]' class='mdl-textfield__input submitOnChange' style='$computedStyle' value='$opt.defaultValue' placeholder='Click to set' id='settings[$opt.name]'>"
    retVal+="<div class='app-text-input-save-button-div' onclick=\"changeSubmit(document.getElementById('settings[$opt.name]'))\"><div class='app-text-input-save-button-text'>Save</div><div class='app-text-input-save-button-icon'>⏎</div></div></div></div>"
    return retVal
}

String buttonLink(HashMap opt) { 
    if(!opt.name || !opt.title ) 
    	return "Error missing name or title"
    if(!opt.color) opt.color = "#1A77C9"
    if(!opt.background) opt.background = "#FFFFFF"
    if(!opt.fontSize) opt.fontSize = "15px"
    if(!opt.width) opt.width = '10em'
    if(!opt.radius) opt.radius = '25px'
    "<div class='form-group'><input type='hidden' name='${opt.name}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='border-radius:25px;color:${opt.color};background-color:${opt.background};cursor:pointer;font-size:${opt.fontSize}; border-style:outset;width:${opt.width};'>${opt.title}</div></div><input type='hidden' name='settings[${opt.name}]' value=''>"
}

String btnIcon(String name) {
    return "<span class='p-button-icon p-button-icon-left pi " + name + "' data-pc-section='icon'></span>"
}


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
