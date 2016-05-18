/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /*
 * The singleton class for App State model to be used globally
 */

define(['require',
	'models/BaseModel',
	'utils/Globals'
],function(require,BaseModel,Globals) {
	'use strict';

	var VAppState = BaseModel.extend({
		defaults : {
			currentTab : Globals.AppTabs.DASHBOARD.value
		},
		initialize : function() {
			this.modelName = 'VAppState';
		//	this.listenTo(this, 'change:currentAccount', this.accountChanged);
		}
		
	});

	// Make this a singleton!!
	return new VAppState();
});

