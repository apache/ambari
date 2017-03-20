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
	'model_bases/VGroupBase',
	'utils/Enums'
],function(require,VGroupBase,Enums){
	'use strict';	

	var VGroup = VGroupBase.extend(
	/** @lends VGroup.prototype */
	{
		/**
		 * VGroupBase initialize method
		 * @augments BaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VGroup';
			this.bindErrorEvents();
		},

		/** This models toString() */
		toString : function(){
			return this.get('message');
		}

	}, {
		// static class members
	});

    return VGroup;
	
});


