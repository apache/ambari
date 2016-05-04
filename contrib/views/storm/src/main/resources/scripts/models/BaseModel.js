/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['require', 'backbone'], function (require, Backbone) {
  'use strict';

var BaseModel = Backbone.Model.extend(
	/** @lends BaseModel.prototype */
	{
		/**
		 * BaseModel's initialize function
		 * @augments Backbone.Model
		 * @constructs
		 */
		initialize : function() {
			
		},
		bindErrorEvents :function(){
			this.bind("error", function(model, error) {
			    if (error.status == 401) {
			      throw new Error("ERROR 401 occured.\n");
			    }
			  });
		},
		/**
		 * toString for a model. Every model should implement this function.
		 */
		toString : function() {},

		/**
		 * Silent'ly set the attributes. ( do not trigger events )
		 */
		silent_set: function(attrs) {
			return this.set(attrs, {
				silent: true
			});
		},
		parse:function(resp, options){
			return this.parseRecords(resp, options);
		},
		parseRecords: function(resp, options) {
			if (this.modelAttrName) {
				return Globalize.byString(resp, this.modelAttrName);
			}else{
				return resp;
			}
		},
	},
	/** BaseModel's Static Attributes */
	{

		/**
		 * [nonCrudOperation description]
		 * @param  {[type]} url           [description]
		 * @param  {[type]} requestMethod [description]
		 * @param  {[type]} options       [description]
		 * @return {[type]}               [description]
		 */
		nonCrudOperation : function(url, requestMethod, options){
			return Backbone.sync.call(this, null, this, _.extend({
				url: url,
				type: requestMethod
			}, options));
		}
	});

	return BaseModel;
});