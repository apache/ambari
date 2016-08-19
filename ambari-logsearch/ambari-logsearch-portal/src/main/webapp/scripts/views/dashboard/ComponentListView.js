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
	'utils/ViewUtils',
	'collections/VNodeList',
	'hbs!tmpl/dashboard/ComponentListView_tmpl'
],function(require,Backbone,Globals,ViewUtils,VNodeList,ComponentListViewTmpl){
    'use strict';
	
	return Backbone.Marionette.ItemView.extend(
			/** @lends ComponentListView */
			{
				_viewName : 'ComponentListView',

				template: ComponentListViewTmpl,
				
				/** ui selector cache */
				ui: {
					componentsList : "[data-id='componentContainer']",
					content : "[data-id='content']"
				},

				/** ui events hash */
				events: function() {
					var events={};
					events["click li"] = 'onComponentClick';
					events["click .host-info a[data-host]"] = 'onNewTabIconClick';
					events["mouseenter .host-info"] = function(e){
						$(e.currentTarget).children("a[data-host]").removeClass("hidden");
					};
					events["mouseleave .host-info"] = function(e){
						$(e.currentTarget).children("a[data-host]").addClass("hidden");
					};
					return events;
				},
				/**
				 * intialize a new ComponentListView ItemView
				 * @constructs
				 */
				initialize: function(options) {
					_.extend(this, _.pick(options,'vent','globalVent','params'));
					this.searchParams = (this.params)? this.params :{};
					this.initializeCollection();
					this.bindEvents();
				},
				initializeCollection : function(){
					this.componentsList = new VNodeList([],{
						state: {
		                    firstPage: 0,
		                    pageSize: 99999
		                }
					});
					this.componentsList.url = Globals.baseURL + "service/logs/components/level/counts";
					this.hostList = new VNodeList([],{
						state: {
		                    firstPage: 0,
		                    pageSize: 99999
		                }
					});
					this.hostList.url = Globals.baseURL + "service/logs/hosts/components";
				},
				/** all events binding here */
				bindEvents : function(){
					this.listenTo(this.componentsList,"reset",function(collection){
						this.renderComponents();
						this.$("#dashboard_tabs").height(this.ui.componentsList.height());
						//this.renderGraph();
					},this);
					this.listenTo(this.hostList,"reset",function(collection){
						this.renderHostDetails();
					},this);
					this.listenTo(this.vent,"main:search level:filter type:mustNot type:mustBe search:include:exclude " +
							"logtime:filter reinitialize:filter:tree tab:refresh " +
							Globals.eventName.serviceLogsIncludeColumns+" "+Globals.eventName.serviceLogsExcludeColumns,function(value){
		            	_.extend(this.searchParams,value);
		            	this.fetchComponents(this.searchParams);
		            	this.fetchComponentsHost(this.searchParams);
		            },this);
					this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
						_.extend(this.searchParams,value);
		            	this.fetchComponents(this.searchParams);
		            	this.fetchComponentsHost(this.searchParams);
					},this);
				},
				/** on render callback */
				onRender: function() {
					this.fetchComponents((this.params) ? this.params : {q:"*:*"});
				},
				fetchComponents : function(params){
					var that = this;
					$.extend(this.componentsList.queryParams,params);
					this.componentsList.fetch({
						beforeSend : function(){
							that.ui.componentsList.siblings(".loader").show();
						},
						reset:true,
						complete : function(){
							that.ui.componentsList.siblings(".loader").hide();
						}
					});
				},
				fetchComponentsHost : function(params){
					var that = this;
					$.extend(this.hostList.queryParams,params);
					this.hostList.fetch({
						beforeSend : function(){
							that.ui.content.siblings(".loader").show()
						},
						reset:true,
						complete : function(){
							that.ui.content.siblings(".loader").hide()
						}
					});
				},
				renderComponents : function(){
					var that = this;
					that.ui.componentsList.find('.nodebar').popover('destroy');
					that.ui.componentsList.empty();
					if(this.componentsList.length == 0){
						this.$("#dashboard_tabs").hide();
					}else{
						this.$("#dashboard_tabs").show();
						this.componentsList.each(function(model){
							var total=0,logLevelCount = model.get("logLevelCount");
							for(var i=0;i < logLevelCount.length;i++){
								if(logLevelCount[i].value)
									total = total +parseInt(logLevelCount[i].value,10);
							}
							that.ui.componentsList.append('<li data-name="'+model.get("name")+'"><a href="javascript:void(0);" class="tab-link" id="clients">'+model.get("name")+' ('+total+')</a>'+ViewUtils.getCountDistributionHTML(model.attributes)+'</li>');
							that.appendPopover(that.ui.componentsList.find("li").last(),model.attributes);
						});
					}
					
					if(that.lastComponentLI && that.ui.componentsList.find("li[data-name='"+that.lastComponentLI+"']").length){
						that.ui.componentsList.find("li").removeClass("active");
						that.ui.componentsList.find("li[data-name='"+that.lastComponentLI+"']").addClass("active");
					}else{
						if(that.ui.componentsList.find("li").first().length > 0){
							that.ui.componentsList.find("li").first().click();
						}
					}
				},
				renderHostDetails : function(){
					var that=this;
					that.ui.content.find('.nodebar').popover('destroy');
					that.ui.content.empty();
					that.ui.content.append('<div id="dashboard-overview" class="row" style="visibility: visible; position: relative;"></div>');
					if(this.hostList.length > 0){
						var model = this.hostList.first();
						_.each(model.get("childs"),function(m){
							var html = '<div class="col-md-3"><div class="host-info">';
							html += '<a data-host="'+m.name+'" title="'+m.name+' -> '+model.get("name")+'" data-type = "'+model.get("name")+'" href="javascript:void(0)" class="pull-right hidden"><i class="fa fa-share"></i></a>';
							html += '<h5>'+m.name.split(".")[0] + ' ('+m.value+')</h5>';
							html += ViewUtils.getCountDistributionHTML(m);
							html += '</div></div>';
							that.ui.content.append(html);
							that.appendPopover(that.ui.content.find('.col-md-3').last(),m);
						});
					}
				},
				onComponentClick : function(e){
					var $el = $(e.currentTarget);
					this.lastComponentLI = $el.data("name");
					this.ui.componentsList.find("li").removeClass("active");
					$el.addClass("active");
					this.fetchComponentsHost(_.extend({componentName:$el.data("name")},this.searchParams));
				},
				onNewTabIconClick : function(e){
					var $el = $(e.currentTarget),host,component,that=this;
					host = $el.data("host");
					component = $el.data("type");
					that.globalVent.trigger("render:tab",{
						params:_.extend({},{
							host :  host,
							component : component
						},that.searchParams,{treeParams:null}),
						globalVent : that.globalVent
					});
				},
				appendPopover : function(node,data){
					node.find('.nodebar').popover({
						trigger: 'hover',
						placement: "top",
						html: true,
						container: 'body',
						template : '<div class="popover log-count" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>',
						content: this.getPopoverHTML(data)
					});
				},
				getPopoverHTML : function(node){
					if(! node.logLevelCount)
						return "";
					var html="";
					//if count for all elements is zero then popover should not appear
					if(_.find(node.logLevelCount,function(d){ return parseInt(d.value,10) > 0})){
						_.each(node.logLevelCount,function(data){
							html += "<span class='"+data.name+"'><i class='fa fa-circle'></i> "+data.name+": <strong>"+data.value+"</strong></span>";
						});
					}
					return html;
				},
				renderGraph : function(){
					var fatalData=[],errorData=[],warnData=[],infoData=[],debugData=[],traceData=[],
						fatalIndex={},errorIndex={},warnIndex={},infoIndex={},debugIndex={},traceIndex={},tooltipIndex={};
					this.componentsList.each(function(m,i){
						var attr = m.get("logLevelCount");
						tooltipIndex[i] =m.get("name");
						//fatal
						var fatal = _.findWhere(attr,{name:"FATAL"});
						if(fatal){
							fatalData.push(fatal.value);
							fatalIndex[fatalData.length -1] =m.get("name");
						}
						//error
						var error = _.findWhere(attr,{name:"ERROR"});
						if(error){
							errorData.push(error.value);
							errorIndex[errorData.length -1]=m.get("name");
						}
						//warn
						var warn = _.findWhere(attr,{name:"WARN"});
						if(warn){
							warnData.push(warn.value);
							warnIndex[warnData.length -1] =m.get("name");
						}
						//info
						var info = _.findWhere(attr,{name:"INFO"});
						if(info){
							infoData.push(info.value);
							infoIndex[infoData.length -1] =m.get("name");
						}
						//debug
						var debug = _.findWhere(attr,{name:"DEBUG"});
						if(debug){
							debugData.push(debug.value);
							debugIndex[debugData.length -1] =m.get("name");
						}
						//trace
						var trace = _.findWhere(attr,{name:"TRACE"});
						if(trace){
							traceData.push(trace.value);
							traceIndex[traceData.length -1] =m.get("name");
						}
					});
					var barOptions = {
						type : "bar",
						barWidth : 8,
						highlightColor : '#353535',
						barSpacing : 2,
						height : 30,
						tooltipFormat : '{{offset:offset}} {{value}}'
					};
					this.$(".g-fatal").sparkline(fatalData,
							_.extend({}, barOptions, {
								barColor : '#830A0A',
								tooltipValueLookups : {
									'offset' : fatalIndex
								},
							}));
					this.$(".g-error").sparkline(errorData,
							_.extend({}, barOptions, {
								barColor : '#E81D1D',
								tooltipValueLookups : {
									'offset' : errorIndex
								},
							}));
					this.$(".g-warn").sparkline(warnData,
							_.extend({}, barOptions, {
								barColor : '#FF8916',
								tooltipValueLookups : {
									'offset' : warnIndex
								},
							}));

					this.$(".g-info").sparkline(infoData,
							_.extend({}, barOptions, {
								barColor : '#2577B5',
								tooltipValueLookups : {
									'offset' : infoIndex
								},
							}));

					this.$(".g-debug").sparkline(debugData,
							_.extend({}, barOptions, {
								barColor : '#65E8FF',
								tooltipValueLookups : {
									'offset' : debugIndex
								},
							}));

					this.$(".g-trace").sparkline(debugData,
							_.extend({}, barOptions, {
								barColor : '#65E8FF',
								tooltipValueLookups : {
									'offset' : traceIndex
								},
							}));
					
				}
			});
});