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
	'hbs!tmpl/dashboard/LogLevelView_tmpl',
	'select2'
],function(require,Backbone,Globals,VLogLevel,LogLevelTmpl){
    'use strict';
	
	return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelView */
	{
		_viewName : 'LogLevelView',

		template: LogLevelTmpl,

		/** Layout sub regions */
		regions: {
			RLogLevelPieChart : "#r_logLevelPieChart",
		},

		/** ui selector cache */
		ui: {
			INFO : "[data-id='INFO']",
			WARN : "[data-id='WARN']",
			ERROR : "[data-id='ERROR']",
			DEBUG : "[data-id='DEBUG']",
			FATAL : "[data-id='FATAL']",
			TRACE : "[data-id='TRACE']",
			UNKNOWN : "[data-id='UNKNOWN']",
			togglePieViewButton:'#logToggle',
			pieRegionId:'#r_logLevelPieChart',
			logTable:'#logTable'
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click #searchLog'] = 'onSearchLogClick';
			events['click ' + this.ui.togglePieViewButton] = 'onToggaleView';
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent'));
			this.collection = new VLogLevel();
			this.bindEvents();
		},
		onRender : function(){
			this.fetchCollection();
			this.ui.pieRegionId.hide();
		},
		fetchCollection : function(params){
			$.extend(this.collection.queryParams, params);
			this.collection.fetch({reset:true});
		},
		bindEvents : function(){
			this.listenTo(this.collection,"reset",function(collection){
				this.populateDetails();
				this.renderLogLevelPieChart();
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
		populateDetails : function(){
			var that = this;
			this.collection.each(function(m,i){
				that.ui[m.get("name")].text(m.get("count"));
			});
		},
		renderLogLevelPieChart:function(){
			var that = this;
			require(['views/graphs/PieChartGraphLayoutView'],function(PieChartGraphLayoutView){
				that.RLogLevelPieChart.show(new PieChartGraphLayoutView({
					vent : that.vent,
					collection:that.collection
					/*parentView:this*/
				}));
			})
		},
		onToggaleView:function(){
		    this.ui.togglePieViewButton.children().toggleClass('fa-pie-chart fa-th');
		    this.ui.pieRegionId.toggle();
		    this.ui.logTable.toggle()
		}
	});
	
	
});