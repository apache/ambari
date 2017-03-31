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

 
define(['require',
	'collections/BaseCollection',
	'utils/Globals',
	'models/VLogLevel'
],function(require,BaseCollection,Globals,VLogLevel){
	'use strict';	

	var VLogLevelListBase = BaseCollection.extend(
	/** @lends VLogLevelListBase.prototype */
	{
		url: Globals.baseURL + 'service/logs/levels/count',

		model : VLogLevel,

		/**
		 * VLogLevelListBase initialize method
		 * @augments BaseCollection
		 * @constructs
		 */
		initialize : function() {
			this.modelName = 'VLogLevel';
			this.modelAttrName = 'vCounts';
			this.bindErrorEvents();
            this._changes = { };
			this.on('change', this._onChange);
		},
		
		_onChange : function(m){
            this._changes[m.id] = m;
		},

		changed_models: function() {
            return _.chain(this._changes).values();
        },

		/*************************
		 * Non - CRUD operations
		 *************************/

		getUsersOfGroup : function(groupId, options){
			var url = Globals.baseURL  + 'xusers/'  + groupId + '/users';
			
			options = _.extend({
				//data : JSON.stringify(postData),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
		},

		setUsersVisibility : function(postData , options){
			var url = Globals.baseURL  + 'xusers/secure/users/visibility';

			options = _.extend({
				data : JSON.stringify(postData),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'PUT', options);
		},
	},{
	/**
	* Table Cols to be passed to Backgrid
	* UI has to use this as base and extend this.
	*
	*/

		tableCols : {}
	});

    return VLogLevelListBase;
});


