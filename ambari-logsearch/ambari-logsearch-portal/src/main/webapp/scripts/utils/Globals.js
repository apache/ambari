/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

define(['require','App'], function(require, App){
	'use strict';
	
	var Globals = {};
	
	Globals.settings = {};
	Globals.settings.PAGE_SIZE = 25;
	Globals.settings.uploadDefaultOpts = {
		disableImageResize: false,
		maxFileSize: 5000000,
		autoUpload : false
		//maxNumberOfFiles : 2
	};
	Globals.settings.MAX_VALUE = 2147483647;

	Globals.keys = {};
	Globals.keys.ENTER_KEY = 13;
	Globals.keys.ESC_KEY = 27;
	
	Globals.EventHistory = {
			totalCount : 50
	};

	Globals.baseURL = '../api/v1/';
	//Globals.baseURL = App.baseUrl;

	Globals.AppTabs = {
			DASHBOARD 			: { value:1, valStr: 'Dashboard'},
			MAINVIEW 			: { value:2, valStr: 'Main View'}
		};

	Globals.BooleanValue = {
		BOOL_TRUE:{value:"true", label:'True'},
		BOOL_FALSE:{value:"false", label:'False'}
	};
	Globals.paramsNameMapping = {
		q:{label:'Query'},
		from:{label:'From'},
		to:{label:'To'},
		unit:{label:'Unit'},
		level:{label:'Level'},
		mustNot:{label:'Exclude Component'},
		mustBe:{label:'Include Component'},
		iMessage:{label:'Include Message'},
		eMessage:{label:'Exclude Message'},
		time:{label:''},
		includeQuery : {label:"Include Column"},
		excludeQuery : {label:"Exclude Column"},
		dateRangeLabel : {label : "Date Range"}
	};

	Globals.graphType =  {
		MULTILINE:{value:1},
		HISTOGRAM:{value:2},
		PIE:{value:3},
		TABLE:{value:4}
	};

	Globals.dateFormat = "YYYY-MM-DD HH:mm:ss.SSS";
	Globals.splitToken = "|i::e|";
	Globals.eventName = {
			serviceLogsIncludeColumns : "search:serviceLogs:include",
			serviceLogsExcludeColumns : "search:serviceLogs:exclude"
	};
	Globals.serviceLogsColumns = [];
	return Globals;
});
