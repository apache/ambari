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
	'hbs!tmpl/dashboard/LogLevelBoxView_tmpl',
	'select2'
],function(require,Backbone,Globals,VLogLevelList,LogLevelBoxTmpl){
    'use strict';
	
	return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelBoxView */
	{
		_viewName : 'LogLevelBoxView',

		template: LogLevelBoxTmpl,

		/** Layout sub regions */
		regions: {
		},

		/** ui selector cache */
		ui: {
			INFO : "[data-id='INFO']",
			WARN : "[data-id='WARN']",
			ERROR : "[data-id='ERROR']",
			DEBUG : "[data-id='DEBUG']",
			FATAL : "[data-id='FATAL']",
			TRACE : "[data-id='TRACE']",
			loader:".server-info .fa-spin"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events["click .server-info a"] = 'onLogLevelClick';
			return events;
		},

		/**
		 * intialize a new LogLevelBoxView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params'));
			this.logLevelList = new VLogLevelList();
			this.logLevelList.url = Globals.baseURL + "service/logs/levels/counts/namevalues";
			this.logLevelList.modelAttrName = "vNameValues";
			this.bindEvents();
		},
		onRender : function(){
//			this.fetchLogLevelCounts({level:(this.params) ? this.params.level : null});
			this.fetchLogLevelCounts((this.params) ? this.params : {});
			if(this.params && this.params.level){
				var levels = this.params.level.split(",");
				this.highlightLevels(levels);
			}
		},
		highlightLevels : function(levels){
			this.$(".node").removeClass("active")
			for(var i=0;i<levels.length;i++){
				this.$(".node."+levels[i]).addClass("active");
			}
		},
		fetchLogLevelCounts : function(params){
			$.extend(this.logLevelList.queryParams,params,{level: "FATAL,ERROR,WARN,INFO,DEBUG,TRACE"});
			this.ui.loader.show();
			this.logLevelList.fetch({reset:true});
		},
		bindEvents : function(){
			this.listenTo(this.logLevelList,"reset",function(){
				this.ui.loader.hide();
				this.renderLogLevelCounts();
			},this);
			this.listenTo(this.vent,"main:search tree:search type:mustNot type:mustBe logtime:filter" +
					" search:include:exclude "+Globals.eventName.serviceLogsIncludeColumns+" "+Globals.eventName.serviceLogsExcludeColumns,function(value){
				this.fetchLogLevelCounts(value);
			},this);
//			this.listenTo(this.vent,"tree:search",function(value){
//            	this.fetchLogLevelCounts(value);
//            },this);
//			this.listenTo(this.vent,"type:mustNot",function(value){
//            	this.fetchLogLevelCounts(value);
//            },this);
//			this.listenTo(this.vent,"type:mustBe",function(value){
//            	this.fetchLogLevelCounts(value);
//            },this);
//			this.listenTo(this.vent,"logtime:filter",function(value){
//            	this.fetchLogLevelCounts(value);
//            },this);
//			this.listenTo(this.vent,"search:include:exclude",function(value){
//            	this.fetchLogLevelCounts(value);
//            },this);
			this.listenTo(this.vent,"reinitialize:filter:level",function(value){
            	this.reinitializeFilter(value);
            },this);
            this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
                this.fetchLogLevelCounts(value);
            },this);
            this.listenTo(this.vent,"tab:refresh",function(params){
            	this.reRenderView(params);
            },this);
		},
		renderLogLevelCounts : function(){
			var that = this;
			this.logLevelList.each(function(model){
				that.$("[data-total='"+model.get("name")+"']").parent().prop('title',model.get("value"))
				that.$("[data-total='"+model.get("name")+"']").text(model.get("value"));
			});
		},
		onLogLevelClick : function(e){
			var $el = $(e.currentTarget);
			if($el.hasClass("active")){
				$el.removeClass("active");
			}else{
				$el.addClass("active");
			}
			var params = [];
			_.each(this.$(".server-info a.active"),function(e){
				params.push($(e).find("strong").data("total"));
			});
			this.fetchLogLevelCounts({level:params.toString()});
			this.vent.trigger("level:filter",{level:params.toString()});
		},
		reinitializeFilter : function(value){
			this.fetchLogLevelCounts(value);
			if(value.level)
				this.highlightLevels(value.level.split(","));
			
		},
		reRenderView : function(params){
			this.fetchLogLevelCounts(params);
		}
	});
	
	
});