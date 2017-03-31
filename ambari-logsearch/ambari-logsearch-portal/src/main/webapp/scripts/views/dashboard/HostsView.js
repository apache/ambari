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

define(['require',
	'backbone',
	'utils/Globals',
	'collections/VLogLevelList',
	'hbs!tmpl/dashboard/HostsView_tmpl',
	'select2'
],function(require,Backbone,Globals,VLogLevel,HostsTmpl){
    'use strict';
	
	return Backbone.Marionette.Layout.extend(
	/** @lends HostsView */
	{
		_viewName : 'HostsView',

		template: HostsTmpl,

		/** Layout sub regions */
		regions: {
		},

		/** ui selector cache */
		ui: {
		},

		/** ui events hash */
		events: function() {
			var events = {};
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent'));
			this.collection = new VLogLevel();
			this.collection.url = Globals.baseURL+"service/logs/hosts/count";
			this.bindEvents();
		},
		onRender : function(){
			this.fetchCollection();
		},
		bindEvents : function(){
			this.listenTo(this.collection,"reset",function(collection){
				this.populateDetails();
			},this);
			this.listenTo(this.collection, 'request', function(){
				this.$("#loader").show();
			},this);
            this.listenTo(this.collection, 'sync error', function(){
            	this.$("#loader").hide();
			},this);
            this.listenTo(this.vent,"main:search",function(value){
            	this.fetchCollection({q:value});
            });
		},
		fetchCollection : function(params){
			$.extend(this.collection.queryParams, params);
			this.collection.fetch({reset:true});
		},
		populateDetails : function(){
			var that = this;
			that.$("tbody").empty();
			var actions = '<td>'+
            				'<a href="javascript:void(0);"><i class="fa fa-search"></i></a>'+
            				//'<a href="javascript:void(0);"><i class="fa fa-ban"></i></a>'+
            				'</td>';
			this.collection.each(function(m,i){
				var html = "<tr>";
				html += "<td>"+m.get("name")+"</td>";
				html += "<td>"+m.get("count")+"</td>";
				html += actions;
				that.$("tbody").append(html);
			});
		}
	});
	
	
});