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
	'model_bases/VLogBase',
	'utils/Enums'
],function(require,VLogBase,Enums){	
	'use strict';	

	var VLog = VLogBase.extend(
	/** @lends VLog.prototype */
	{
		/**
		 * VLogBase initialize method
		 * @augments FSBaseModel
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VLog';
			this.bindErrorEvents();
			this.toView();
		},

		toView : function(){
			if(!_.isUndefined(this.get('isVisible'))){
				var visible = (this.get('isVisible') == Enums.VisibilityStatus.STATUS_VISIBLE.value);
				this.set('isVisible', visible);
			}
		},

		toServer : function(){
			var visible = this.get('isVisible') ? Enums.VisibilityStatus.STATUS_VISIBLE.value : Enums.VisibilityStatus.STATUS_HIDDEN.value;
			this.set('isVisible', visible);
		},
		
		/** This models toString() */
		toString : function(){
			return this.get('name');
		}

	}, {
		// static class members
	});

    return VLog;
	
});


