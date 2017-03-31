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
	'models/VNode'
],function(require,BaseCollection,Globals,VNode){
	'use strict';	

	var VNodeListBase = BaseCollection.extend(
	/** @lends VNodeListBase.prototype */
	{
		url: Globals.baseURL + 'service/logs/tree',

		model : VNode,

		/**
		 * VNodeListBase initialize method
		 * @augments BaseCollection
		 * @constructs
		 */
		initialize : function() {
			this.modelName = 'VNode';
			this.modelAttrName = 'vNodeList';
			this.bindErrorEvents();
		},
		
		/*************************
		 * Non - CRUD operations
		 *************************/

		cancelFindRequest : function(token, options){
			var url = Globals.baseURL  + 'service/logs/request/cancel';
			
			options = _.extend({
				data : $.param(token),
				contentType : 'application/json',
				dataType : 'json'
			}, options);

			return this.constructor.nonCrudOperation.call(this, url, 'GET', options);
		},
	},{
	/**
	* Table Cols to be passed to Backgrid
	* UI has to use this as base and extend this.
	*
	*/

		tableCols : {}
	});

    return VNodeListBase;
});


