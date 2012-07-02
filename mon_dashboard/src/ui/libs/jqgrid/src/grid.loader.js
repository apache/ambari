/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

//This file should be used if you want to debug and develop
function jqGridInclude()
{
    var pathtojsfiles = "js/"; // need to be ajusted
    // set include to false if you do not want some modules to be included
    var modules = [
        { include: true, incfile:'i18n/grid.locale-en.js'}, // jqGrid translation
        { include: true, incfile:'grid.base.js'}, // jqGrid base
        { include: true, incfile:'grid.common.js'}, // jqGrid common for editing
        { include: true, incfile:'grid.formedit.js'}, // jqGrid Form editing
        { include: true, incfile:'grid.inlinedit.js'}, // jqGrid inline editing
        { include: true, incfile:'grid.celledit.js'}, // jqGrid cell editing
        { include: true, incfile:'grid.subgrid.js'}, //jqGrid subgrid
        { include: true, incfile:'grid.treegrid.js'}, //jqGrid treegrid
	{ include: true, incfile:'grid.grouping.js'}, //jqGrid grouping
        { include: true, incfile:'grid.custom.js'}, //jqGrid custom 
        { include: true, incfile:'grid.tbltogrid.js'}, //jqGrid table to grid 
        { include: true, incfile:'grid.import.js'}, //jqGrid import
        { include: true, incfile:'jquery.fmatter.js'}, //jqGrid formater
        { include: true, incfile:'JsonXml.js'}, //xmljson utils
        { include: true, incfile:'grid.jqueryui.js'}, //jQuery UI utils
        { include: true, incfile:'grid.filter.js'} // filter Plugin
    ];
    var filename;
    for(var i=0;i<modules.length; i++)
    {
        if(modules[i].include === true) {
        	filename = pathtojsfiles+modules[i].incfile;
			if(jQuery.browser.safari) {
				jQuery.ajax({url:filename,dataType:'script', async:false, cache: true});
			} else {
				if (jQuery.browser.msie) {
					document.write('<script charset="utf-8" type="text/javascript" src="'+filename+'"></script>');
				} else {
					IncludeJavaScript(filename);
				}
			}
		}
    }
	function IncludeJavaScript(jsFile)
    {
        var oHead = document.getElementsByTagName('head')[0];
        var oScript = document.createElement('script');
        oScript.setAttribute('type', 'text/javascript');
        oScript.setAttribute('language', 'javascript');
        oScript.setAttribute('src', jsFile);
        oHead.appendChild(oScript);
    }
}
jqGridInclude();
