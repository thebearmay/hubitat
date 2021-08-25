 /*
 * bb2Html 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *

library (
    base: "driver",
    author: "Jean P. May Jr.",
    category: "driverUtilities",
    description: "Converts a string containing BBCode syntax to HTML",
    name: "bb2Html",
    namespace: "thebearmay",
    importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/bb2Html.groovy",
    version: "0.0.2",
    documentationLink: ""
)


String bb2Html(String htmlStr) {
    htmlStr=htmlStr.replace("[b]","<b>")
    htmlStr=htmlStr.replace("[/b]","</b>")
    htmlStr=htmlStr.replace("[i]","<i>")
    htmlStr=htmlStr.replace("[/i]","</i>")
    htmlStr=htmlStr.replace("[u]","<u>")
    htmlStr=htmlStr.replace("[/u]","</u>")
    htmlStr=htmlStr.replace("[s]","<s>")
    htmlStr=htmlStr.replace("[/s]","</s>")
    htmlStr=htmlStr.replace("[sup]","<sup>")
    htmlStr=htmlStr.replace("[/sup]","</sup>")  
    htmlStr=htmlStr.replace("[sub]","<sub>")
    htmlStr=htmlStr.replace("[/sub]","</sub>")  
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
