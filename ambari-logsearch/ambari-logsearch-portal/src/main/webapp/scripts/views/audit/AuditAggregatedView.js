/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

define(['require',
	'backbone',
	'utils/Globals',
	'utils/Utils',
	'utils/ViewUtils',
	'collections/VNameValueList',
    'moment',
	'hbs!tmpl/audit/AuditAggregatedView_tmpl',
    'views/common/JBDialog',
    'views/tabs/ExportLogFileView',
	'select2',
	'nv'
],function(require,Backbone,Globals,Utils,ViewUtils,VNameValueList,moment,AuditAggregatedTmpl,JBDialog,ExportLogFileView){
    'use strict';

	return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelView */
	{
		_viewName : 'AuditAggregatedView',

		template: AuditAggregatedTmpl,

		/** Layout sub regions */
		regions: {
			RtableUsers : "[data-id='topUsers']",
			RtableResources : "[data-id='topResources']"
		},

		/** ui selector cache */
		ui: {
			tableView: "[data-id='aggregatedTable']",
			graphView:"[data-id='aggregatedGraph']"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['change [data-id="toggleTableAccessLog"]']  = 'onToggleTableAggregated';
			events['click [data-id="export-aggregated-text"]']  = 'onExportAggregatedClick';
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * 
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent', 'globalVent', 'params'));
			this.initializeCollection();
			this.bindEvents();
			this.initializeAggregatedTable();
		},
		bindEvents : function(){
			this.listenTo(this.vent,"search:audit:query auditComponents:include auditComponents:exclude "+
					"search:audit:include search:audit:exclude logtime:filter tab:refresh",function(value){
				this.fetchTopUsers(value);
				this.fetchTopResources(value);
            },this);
			this.listenTo(this.topUsers,"reset",function(){
				this.createUsersTable();
				this.renderGraphUsers();
			},this);
			this.listenTo(this.topResources,"reset",function(){
				this.createResourceTable();
				this.renderGraphResources();
			},this);
			this.listenTo(this.vent,"reinitialize:TopTenGraph",function(value){
				this.fetchTopUsers(value);
				this.fetchTopResources(value);
            },this);
            this.listenTo(this,"button:min:max",function(){
            	this.renderGraphUsers();
            	this.renderGraphResources();
            },this);
		},
		initializeCollection : function(){
			this.topUsers = new VNameValueList([],{
				state: {
                    firstPage: 0,
                    pageSize: 9999
                }
			});
			this.topUsers.url = Globals.baseURL + "audit/logs/users";
			this.topUsers.modelAttrName = "graphData";
			this.topResources = new VNameValueList([],{
				state: {
                    firstPage: 0,
                    pageSize: 9999
                }
			});
			this.topResources.url = Globals.baseURL + "audit/logs/resources";
			this.topResources.modelAttrName = "graphData";		
			//initialize colors
			this.colors = (new d3.scale.category20c().range().slice().reverse()).concat(new d3.scale.category20b().range().slice().reverse());
		},
		onRender : function(){
			//this.renderTables();
			this.fetchTopUsers(_.extend({field : "reqUser"},this.params));
			this.fetchTopResources(_.extend({field : "resource"},this.params));
		},
		renderTables : function(){
			var that = this;
			var opts = {
					includeFilter : false,
					includePagination : false,
					includePageSize : false,
					includeFooterRecords : false,
					includeColumnManager : false,
					columnOpts : {
				    	initialColumnsVisible: 0,
				    	saveState : false
					},
					gridOpts : {
						className : "table m-table table-bordered table-hover table-heading",
						emptyText : 'No records found!'
					},
					filterOpts : {},
					paginatorOpts : {}
			}
			var cols = {
				name : {
					label : "User",
					cell: "String",
					sortType: false,
					editable: false,
					sortable : false,
				},
				value : {
					label : "Hits",
					cell: "String",
					sortType: false,
					sortable : false,
					editable: false
				}
			};
			require(['views/common/TableLayout'], function(TableLayout){
				var userCols = new Backgrid.Columns(that.topUsers.constructor.getTableCols(cols, that.topUsers));
				that.RTopUsers.show(new TableLayout(_.extend({},opts,{
					columns: userCols,
					collection: that.topUsers,
				})));
				cols.name.label = "Resources";
				var resourcesCols = new Backgrid.Columns(that.topResources.constructor.getTableCols(cols, that.topResources));
				that.RTopResources.show(new TableLayout(_.extend({},opts,{
					columns: resourcesCols,
					collection: that.topResources,
				})));
			});
		},
		fetchTopResources : function(params){
			var that = this;
			$.extend(this.topResources.queryParams, params);
			this.topResources.fetch({
				reset:true,
				beforeSend : function(){
					that.$("[data-id='resourcesLoader']").removeClass("hidden");
				},
				complete : function(){
					that.$("[data-id='resourcesLoader']").addClass("hidden");
				}
			});
		},
		fetchTopUsers : function(params){
			var that = this;
			$.extend(this.topUsers.queryParams, params);
			this.topUsers.fetch({
				reset:true,
				beforeSend : function(){
					that.$("[data-id='usersLoader']").removeClass("hidden");
				},
				complete : function(){
					that.$("[data-id='usersLoader']").addClass("hidden");
				}
			});
		},
		renderHorizontalBar : function(el,data,margin,columnKey){
			var that = this;
			nv.addGraph({generate : function() {
				  var chart = nv.models.multiBarHorizontalChart()
				      .x(function(d) { return d.label })
				      .y(function(d) { return d.value })
				      .margin(margin)
				      .showValues(true)
				      .valueFormat(d3.format('.0f'))
				      .showControls(false);
				  chart.tooltip.enabled();
				  chart.yAxis
				      .tickFormat(d3.format('d'));
				  chart.multibar.dispatch.on("elementClick", function(e) {
					  that.vent.trigger("toggle:facet",{viewName : "includeColumns",key :columnKey,value :e.data.label});
				  });
				  d3.select(el)
				  .datum(data)
				    .transition().duration(500)
				      .call(chart);
				  return chart;
			},
			callback : function(graph){
				d3.select(el)
					.selectAll("rect")
					.style("cursor","pointer");
				that.$el.resize(function(){
                    d3.select(el)
//                        .attr('width', width)
//                        .attr('height', height)
                        .transition().duration(0)
                        .call(graph);
				});
			}
			});
		},
		renderGraphUsers : function(){
//			var d= [{
//				"key": "Top Ten Users",
//				"color": "#4281DA",          
//			}];
			var obj = this.formatBarData(this.topUsers);
			//d[0].values = obj.arr;
			this.renderHorizontalBar(this.$('[data-id="topUsersGraph"] svg')[0],obj.arr, {top: 5,right:10, bottom: 20,left:(obj.max * 7)+25},"reqUser");
		},
		renderGraphResources : function(){
//			var d= [{
//				"key": "Top Ten Res	ources",
//				"color": "#C7504B",          
//			}];
			var obj = this.formatBarData(this.topResources);
			//d[0].values = obj.arr;
			var marginleft = obj.max * 7;
			this.renderHorizontalBar(this.$('[data-id="topResourcesGraph"] svg')[0],obj.arr, {top: 5,right:10, bottom: 20,left:(marginleft > 300) ? 300 : marginleft },"resource");
		},
		formatBarData : function(collection){
//			var obj=[],len=0;
//			collection.each(function(m){
//				var val = parseInt(m.get("value"),10)
//				if(len < m.get("name").length )
//					len = m.get("name").length;
//				obj.push({
//					label : m.get("name"),
//					value : (_.isNaN(val)) ? 0 : val
//				});
//			});
//			return {
//				max : len,
//				arr : obj
//			}
			return ViewUtils.formatAuditGraphData(collection);
		},

		onToggleTableAggregated: function(e){
			if(e.target.checked){
				this.ui.tableView.addClass("showContent").removeClass('hideContent');
				this.ui.graphView.addClass("hideContent").removeClass('showContent');
			} else{
				this.ui.tableView.addClass("hideContent").removeClass('showContent');
				this.ui.graphView.addClass("showContent").removeClass('hideContent');
			}
		},
		initializeAggregatedTable: function(){
			var that = this;
			that.opts = {
					includeFilter : false,
					includePagination : false,
					includePageSize : false,
					includeFooterRecords : false,
					includeColumnManager : false,
					columnOpts : {
				    	initialColumnsVisible: 0,
				    	saveState : false
					},
					gridOpts : {
						className : "table m-table table-bordered table-hover table-heading",
						emptyText : 'No records found!'
					},
					filterOpts : {},
					paginatorOpts : {}
			}
			that.cols = {
				name : {
					label : "User",
					cell: "String",
					sortType: false,
					editable: false,
					sortable : false,
				},
				dataCount : {
					label : "Components/Access",
					cell: "html",
					sortType: false,
					editable: false,
					sortable : false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function(rawValue, model){
							var str = "<table class='m-table'>";
							_.each(model.get('dataCount'),function(obj){
								str +="<tr><td>"+obj.name+"</td><td>"+obj.value+"</td></tr>"
							});
							return str + "</table>";
						}
					})
				},
			/*	value : {
					label : "Access",
					cell: "html",
					sortType: false,
					editable: false,
					sortable : false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function(rawValue, model){
							var str="";
							_.each(model.get('dataCount'),function(obj){
								str = obj.value;
							});
							return str;
						}
					})
				},*/
			};
			
		},

		createUsersTable:function(){
			var that = this;
			require(['views/common/TableLayout'], function(TableLayout){
				that.cols.name.label = "Users";
				var userCols = new Backgrid.Columns(that.topUsers.constructor.getTableCols(that.cols, that.topUsers));
				that.RtableUsers.show(new TableLayout(_.extend({},that.opts,{
					columns: userCols,
					collection: that.topUsers,
				})));
			});
		},
		createResourceTable:function(){
			var that = this;
			require(['views/common/TableLayout'], function(TableLayout){
			that.cols.name.label = "Resources";
				var resourcesCols = new Backgrid.Columns(that.topResources.constructor.getTableCols(that.cols, that.topResources));
				that.RtableResources.show(new TableLayout(_.extend({},that.opts,{
					columns: resourcesCols,
					collection: that.topResources,
				})));
			});
		},
		onExportAggregatedClick:function(){
				var that = this;
			require(['views/common/JBDialog',],function(JBDialog){
				var view = new ExportLogFileView({viewType:"aggregatView"});
				var opts = _.extend({
					title: "Export",
					content:view,
					viewType: 'Update',
					appendTo: 'body',
					modal: true,
					resizable: false,
					width: 550,
					height:200,
					beforeClose: function(event, ui) {
						that.onDialogClosed();
					},
					buttons: [{
						id: "okBtn",
						text: "Export",
						"class": "btn btn-primary",
						click: function() {
							that.onDialogSubmitted();
						}
					}, {
						id: "cancelBtn",
						text: "Cancel",
						"class": "btn btn-default",
						click: function() {
							that.onDialogClosed();
						}
					}]
				});
				var dialog = that.dialog = new JBDialog(opts).render().open();
			})
		},
		onDialogClosed: function() {
            if (this.dialog) {
                this.dialog.close && this.dialog.close();
                this.dialog.remove && this.dialog.remove();
                this.dialog = null;
            }
        },
        onDialogSubmitted : function(){
			var obj = Utils.getFormData(this.dialog.$(".form-horizontal").serializeArray());
			this.downloadAggregateFile(obj);
		},
		downloadAggregateFile : function(obj){
			obj.utcOffset = moment().utcOffset();
			obj.startIndex =  this.topUsers.state.currentPage * this.topUsers.state.pageSize;
			var params = $.param(_.extend({},this.topUsers.queryParams,obj));
			var url = "api/v1/audit/logs/users/export?"+ params;
			window.open(url);
			this.onDialogClosed();
		}
	});
})