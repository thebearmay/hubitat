String bb2Html(String htmlStr) {
    htmlStr=htmlStr.replace("[b]","<b>")
    htmlStr=htmlStr.replace("[/b]","</b>")
    htmlStr=htmlStr.replace("[i]","<i>")
    htmlStr=htmlStr.replace("[/i]","</i>")
    htmlStr=htmlStr.replace("[u]","<u>")
    htmlStr=htmlStr.replace("[/u]","</u>")
    htmlStr=htmlStr.replace("[br]","<br>")    
    while(htmlStr.indexOf("[color=")>=0) {
        htmlStr=htmlStr.replace("[/color]","</font>")
        int startPos = htmlStr.indexOf("[color=")
        String colorCode = htmlStr.substring(startPos+7,startPos+13)
        htmlStr=htmlStr.replace("[color=$colorCode]","<font color=\"$colorCode\">")
    }
    while(htmlStr.indexOf("[size=")>=0) {
        htmlStr=htmlStr.replace("[/size]","</font>")
        int startPos2 = htmlStr.indexOf("[size=")
        int endPos2 = htmlStr.indexOf("]",startPos2+6)
        String fSize = htmlStr.substring(startPos2+6,endPos2)
        htmlStr=htmlStr.replace("[size=$fSize]","<font size=\"$fSize\">")
    }
    
    return htmlStr
}
