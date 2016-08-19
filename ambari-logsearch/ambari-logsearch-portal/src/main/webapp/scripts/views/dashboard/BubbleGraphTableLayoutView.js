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
	'moment',
	'utils/Globals',
	'utils/Utils',
	'utils/ViewUtils',
	'd3.tip',
	'collections/VLogLevelList',
	'collections/VLogList',
	'models/VGraphInfo',
	'hbs!tmpl/dashboard/BubbleGraphTableLayoutView_tmpl',
	'views/common/JBDialog',
	'select2'
],function(require,Backbone,moment,Globals,Utils,ViewUtils,tip,VLogLevel,VLogList,VGraphInfo,BubbleGraphTableLayoutViewTmpl,JBDialog){
	'use strict';

	return Backbone.Marionette.Layout.extend(
	/** @lends BubbleGraphTableLayoutView */
	{
		_viewName : 'BubbleGraphTableLayoutView',

		template: BubbleGraphTableLayoutViewTmpl,

		/** Layout sub regions */
		regions: {
			RLogTable : "#rLogTable",
			RComponentList : "#componentList",
			RHostList : "#hostList",
			RTimer : "[data-id='timer']"
		},

		/** ui selector cache */
		ui: {
			graph : "#graphAgg",
			viewType: "input[name='viewType']",
			hostList : "#hostList",
			componentList : "#componentList"
		},

		/** ui events hash */
		events: function() {
//			var that=this,
			var events = {};
//			events["mouseenter .logTime"] = function(e){
//				$(e.currentTarget).find("a").removeClass("hidden");
//			};
//			events["mouseleave .logTime"] = function(e){
//				$(e.currentTarget).find("a").addClass("hidden");
//			};
//			events["click .logTime a[data-type='C']"] = 'onNewTabIconClick';
			events['click [data-id="refresh-tab"]']  = 'onTabRefresh';
			events['change ' + this.ui.viewType]  = 'onViewTypeChange';
//			events["mouseenter .logTime"] = function(e){
//				var $el = $(e.currentTarget);
//				$el.append(dropdownMenu);
//				$el.find('.quickMenu li').click(function(e){
//					that.onQuickMenuClick(e);
//				});
//			};
//			events["mouseleave .logTime"] = function(e){
//				var $el = $(e.currentTarget);
//				//$el.find(".quickMenu").remove();
//			};
			events['click .quickMenu li']  = 'onQuickMenuClick';
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params','quickHelp','columns'));
			(_.isUndefined(this.quickHelp)) ? this.quickHelp = false : "";
			this.collection = new VLogList([], {
                state: {
                    firstPage: 0,
                    pageSize: 25
                }
            });
			this.collection.url = Globals.baseURL + "service/logs";
			this.graphModel = new VGraphInfo();
			this.bindEvents();
			this.graphParams = this.params;

		},
		bindEvents : function(){
			this.listenTo(this.collection,"reset",function(collection){
				//this.populateDetails();
			},this);
			this.listenTo(this.collection, "backgrid:refresh",function(){
				$(".contextMenuBody [data-id='F']").show();
            	$(".contextMenuBody").hide();
            	//this.$("#loaderGraph").hide();
				this.$(".loader").hide();
            	//this.ui.find.trigger("keyup");
//            	if (this.quickHelp)
//            		this.initializeContextMenu();
            },this);
			this.listenTo(this.collection, 'request', function(){
				this.$("#loader").show();
			},this);
            this.listenTo(this.collection, 'sync error', function(){
            	this.$("#loader").hide();
			},this);
            this.listenTo(this.vent,"main:search tree:search level:filter type:mustNot type:mustBe logtime:filter search:include:exclude " +
            		Globals.eventName.serviceLogsIncludeColumns+" "+Globals.eventName.serviceLogsExcludeColumns,function(value){
            	this.fetchAllTogether(value);
            	this.selectionText="";
            });
//            this.listenTo(this.vent,"tree:search",function(value){
//            	this.fetchAllTogether(value);
//            });
//            this.listenTo(this.vent,"level:filter",function(value){
//            	this.fetchAllTogether(value);
//            });
//            this.listenTo(this.vent,"type:mustNot",function(value){
//            	this.fetchAllTogether(value);
//            });
//            this.listenTo(this.vent,"type:mustBe",function(value){
//            	this.fetchAllTogether(value);
//            });
//            this.listenTo(this.vent,"logtime:filter",function(value){
//            	this.fetchAllTogether(value);
//            });
//            this.listenTo(this.vent,"search:include:exclude",function(value){
//            	this.fetchAllTogether(value);
//            });
            this.listenTo(this.vent,"reinitialize:filter:bubbleTable",function(value){
            	this.reinitializeBubbleTableFilter(value);
            });
            this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
                this.fetchAllTogether(value);
            }, this);
            this.listenTo(this.vent, "timer:end", function(value) {
            	//timer should start only after log table fetch is complete.
//            	var arr = Utils.dateUtil.getRelativeDateFromString(this.params.dateRangeLabel);
//            	if(_.isArray(arr)){
//            		this.params.from = arr[0].toJSON();
//                	this.params.to = arr[1].toJSON();
//            	}
            	ViewUtils.setLatestTimeParams(this.params);
            	this.vent.trigger("tab:refresh",this.params);
            	var that = this;
            	this.fetchTableCollection(this.params,{
            		complete : function(){
            			that.vent.trigger("start:timer");
            		}
            	});
            }, this);
            this.listenTo(this.vent, "tab:refresh", function(value) {
            	this.fetchTableCollection(value);
            },this);
		},
		fetchAllTogether : function(value){
			//this.$("#loaderGraph").show();
			this.fetchTableData(value);
        	_.extend(this.graphParams,value);
        	//this.fetchGraphData(this.graphParams);
		},
		onRender : function(){
			var that = this;
			this.fetchTableData((this.params) ? this.params : {q:"*:*"});
			this.renderComponentList();
			this.collection.getServiceLogFields({},{
				success : function(data){
					that.serverColumns = data;
				},
				complete : function(){
					that.renderTable();
				}
			});
			//this.renderTable();
			this.renderHostList();
			this.renderTimer();
			if(this.quickHelp){
				this.initializeContextMenu();
				this.bindContextMenuClick();
			}
		},
		onShow : function(){
			//this.fetchGraphData((this.params) ? this.params : {q:"*:*"});
		},
		onTabRefresh : function(){
//			this.fetchAllTogether({});
//			if(this.RHostList.currentView){
//				this.RHostList.currentView.fetchHosts({});
//			}
			ViewUtils.setLatestTimeParams(this.params);
			this.vent.trigger("tab:refresh",this.params);
		},
		onNewTabIconClick : function($el){
			var host,component,id,that=this;
			if($el.data("host") && $el.data("node")){
				host = $el.data("host");
				component = $el.data("node");
				id = $el.data("id");
				that.globalVent.trigger("render:tab",{
					params:_.extend({},{
						host :  host,
						component : component,
						sourceLogId: id
					},that.graphParams,{treeParams:null}),
					globalVent : that.globalVent
				});
			}
		},
		renderTable : function(){
			var that = this;
			var cols = new Backgrid.Columns(this.getColumns());
			require(['views/common/TableLayout','views/common/CustomBackgrid'],function(TableLayout,CustomBackgrid){
				var IdRow = Backgrid.Row.extend({
				    render: function() {
				        IdRow.__super__.render.apply(this, arguments);
				        if (this.model.has("id")) {
				            this.$el.attr("data-id", this.model.get('id'));
				        }
				        return this;
				    }
				});
				that.RLogTable.show(new TableLayout({
					columns: cols,
					collection: that.collection,
					includeFilter : false,
					includePagination : true,
					includePageSize : true,
					includeFooterRecords : true,
					includeColumnManager : true,
					columnOpts : {
				    	initialColumnsVisible: 3,
				    	saveState : false
					},
					gridOpts : {
						header : CustomBackgrid,
						row: IdRow,
						emptyText : 'No records found!',
						className: 'table table-bordered table-hover table-condensed backgrid table-quickMenu'
					},
					filterOpts : {},
					paginatorOpts : {}
				}));
			});
		},
		renderComponentList : function(){
			var that = this;
			require(['views/dashboard/ComponentListView'],function(ComponentListView){
				that.RComponentList.show(new ComponentListView({
					vent : that.vent,
					globalVent : that.globalVent,
					params : that.params
				}));
			})
		},
		renderHostList : function(){
			var that = this;
			require(['views/dashboard/HostListView'],function(HostListView){
				that.RHostList.show(new HostListView({
					vent : that.vent,
					globalVent : that.globalVent,
					params : that.params
				}));
			})
		},
		renderTimer : function(){
			var that = this;
			require(['views/common/TimerView'],function(TimerView){
				that.RTimer.show(new TimerView({
					vent : that.vent,
					globalVent : that.globalVent
				}));
			});
		},
		getColumns : function(){
			var timeZone = moment().zoneAbbr(),
			 	cols = {},
			 	that = this;
			_.each(this.columns,function(value,col){
				cols[col] = {
						label:value,
						cell: "String",
						sortType: 'toggle',
						editable: false,
				}
			});
			var columns = {
					logtime : {
						label: "Log Time "+(! _.isEmpty(timeZone) ? "("+timeZone+")":""),
						cell: "html",
						editable: false,
						sortType: 'toggle',
						direction: "descending",
						orderable : true,
						displayOrder :1,
						width : 17,
						className : "logTime",
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function(rawValue, model){
								var str="";
								if(rawValue)
									str += "<div style='position:relative'><p style='margin-left:20px'>"+moment(rawValue).format("YYYY-MM-DD HH:mm:ss,SSS")+"</p>";
								if(model.get("type"))
									str += "<p style='margin-left:20px'>"+(model.get("level") ? "<label class='label label-"+model.get("level")+"'>"+model.get("level")+"</label>" : "")+
											"<strong>"+model.get("type")+"</strong>" +
											"</p><!--a  style='width:9%' title='Open logs in new tab' data-type='C' data-host='"+model.get("host")+"' data-node='"+model.get("type")+"' data-id='"+model.get("id")+"' href='javascript:void(0)' class='pull-right hidden'><i class='fa fa-share'></i></a-->";
//								if(model.get("level"))
//									str += "<p style='float:left;'><label class='label label-"+model.get("level")+"'>"+model.get("level")+"</label></p>";
								str += '<div class="dropdown quickMenu">' +
								  '<a class="btn btn-success btn-xs btn-quickMenu" data-toggle="dropdown">' +
								  '<i class="fa fa-ellipsis-v"></i></span></a>' +
								  '<ul class="dropdown-menu dropupright">' +
								    '<li data-id="A_B"><a href="javascript:void(0)">Preview</a></li>' +
								    "<li data-id='N_T'><a title='Open logs in new tab' data-type='C' data-host='"+model.get("host")+"' data-node='"+model.get("type")+"' data-id='"+model.get("id")+"' href='javascript:void(0)' class=''>Go To Log</a></li>" +
								    "<li data-id='C_M'><a title='Add to compare' data-type='C' data-host='"+model.get("host")+"' data-node='"+model.get("type")+"' data-id='"+model.get("id")+"' href='javascript:void(0)' class=''>Add to Compare</a></li>" +
								  '</ul>' +
								'</div></div>';
								return str;
							}
						})
					},
					/*type: {
						label: "Type",
						cell: "String",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						orderable : true,
						displayOrder :2,
						width : 11
					},
					level : {
						label: "Level",
						cell: "html",
						editable: false,
						sortType: 'toggle',
						sortable : true,
						orderable : false,
						width : "6",
						displayOrder :3,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function(rawValue, model){
								return "<span class='"+rawValue+"'>"+rawValue+"</span>";
							}
						})
					},*/
					log_message : {
						label: "Message",
						cell: "html",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						//width : "50",
						orderable : true,
						displayOrder :4,
						className : "logMessage",
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function(rawValue, model){
								return (rawValue) ? "<pre>"+Utils.escapeHtmlChar(rawValue)+"</pre>" : "";
							}
						})
					},
					host : {
						label: "Host",
						cell: "String",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						orderable : true,
						displayOrder :5,
						width : 10
					},
					thread_name : {
						label: "Thread",
						cell: "String",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						orderable : true,
						displayOrder :5,
						width : 10
					},
					logger_name : {
						label : "Logger",
						cell: "String",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						orderable : true,
						displayOrder :6,
						width : 13
					},
					bundle_id : {
						label : "Bundle Id",
						cell: "String",
						editable: false,
						sortType: 'toggle',
						sortable : false,
						orderable : true,
						displayOrder :6,
						width : 6
					}

			};
			_.each(cols,function(c,k){
				if(columns[k] == undefined){
					columns[k] = c;
				}else{
					if(columns[k] && columns[k].label){
						columns[k].label = c.label;
					}
				}
			});
			return this.collection.constructor.getTableCols(columns, this.collection);
		},
		fetchTableData : function(params){
			var that = this;
			$.extend(this.collection.queryParams,params);
			this.collection.getFirstPage({
				reset:true,
				beforeSend : function(){
        			that.$("#loaderGraph").show();
        			that.$(".loader").show();
        		},
        		complete : function(){
					that.$("#loaderGraph").hide();
					that.$(".loader").hide();
				}
			});
		},
		fetchTableCollection : function(queryParams, param){
			var that = this;
			$.extend(this.collection.queryParams,queryParams);
			this.collection.fetch(_.extend({
				reset:true,
				beforeSend : function(){
        			that.$("#loaderGraph").show();
        			that.$(".loader").show();
        		},
        		complete : function(){
					that.$("#loaderGraph").hide();
					that.$(".loader").hide();
				}
			},param));
		},
		fetchGraphData : function(params){
			var that = this;
			//that.$("#loaderGraph").show();
			that.$(".loader").show();
			this.graphModel.fetch({
				dataType:"json",
				data : params,
				success : function(data,textStatus,jqXHR){
					that.renderGraph();
				},
				error : function(){
				},
				complete : function(){
					//that.$("#loaderGraph").hide();
					that.$(".loader").hide();
				}
			});
		},
		reinitializeBubbleTableFilter : function(values){
			this.fetchTableData(values);
			//this.fetchGraphData(values);
		},
		onViewTypeChange: function(e){
			var that = this;
			var val = that.$("[name='viewType']:checked").val();
			this.toggleViewType(val);
		},
		toggleViewType : function(val){
			this.$("[data-id='table']").hide();
			this.ui.graph.hide();
			this.ui.hostList.hide();
			this.ui.componentList.hide();
			this.$(".alert").hide();
			if(val === "G"){
				this.ui.graph.show();
				this.$(".alert").show();
			}else if(val === "T"){
				this.$(".alert").hide();
				this.$("[data-id='table']").show();
			}else if(val === "H"){
				this.ui.hostList.show();
			}else
				this.ui.componentList.show();

		},
		bindContextMenuClick : function(){
			var that = this;
			$("body").on("click",".contextMenuBody li a",function(e){
				that.onDropDownMenuClick(e);
			});
		},
		initializeContextMenu : function(){
			var that = this;
//			$('body').on("mouseup",function(e){
//				console.log(e);
//				if(! $(".contextMenuBody").is(":hidden")){
//					if(! $(e.target).parents(".contextMenuBody").length > 0){
//						$(".contextMenuBody").hide();
//					}
//				}
//			})
			$('body').on("mouseup.contextMenu",function(e){
				var selection;
				if (window.getSelection) {
			          selection = window.getSelection();
			        } else if (document.selection) {
			          selection = document.selection.createRange();
			        }
				if(_.isEmpty(selection.toString()) && ($(".contextMenuBody").is(":hidden")) ){
					that.selectionText ="";
				}
			});
			this.$el.on('mouseup contextmenu', ".logMessage", function(e){
		        var selection;
		        e.stopPropagation();

		        var range = window.getSelection().getRangeAt(0);
				var selectionContents = range.cloneContents();
				selection = selectionContents.textContent;

		        setTimeout(function(){
		        	that.selectionCallBack(selection,e)
		        },1);


		    });
		},
		selectionCallBack : function(selection,e){
			this.RLogTable.currentView.$el.removeHighlight(true);
			if(this.selectionText != selection.toString()){
				this.selectionText = selection.toString();
			}else{
				$(".contextMenuBody [data-id='F']").show();
				$(".contextMenuBody").hide();
				return;
			}
			if(selection.toString() && selection && (! _.isEmpty(selection.toString().trim())) ){
				this.RLogTable.currentView.$el.find(".logMessage").highlight(selection.toString().trim(),true,e.currentTarget);
				$(".contextMenuBody [data-id='F']").hide();
				$(".contextMenuBody").show();
				$(".contextMenuBody").css({
					'top':e.pageY - 40,
					'left':e.pageX
				});
			}else{
				this.RLogTable.currentView.$el.removeHighlight(true);
				$(".contextMenuBody [data-id='F']").show();
				$(".contextMenuBody").hide();
			}
		},
		onDropDownMenuClick : function(e){
			var $el = $(e.currentTarget),type=$el.data("id");
			if(! _.isEmpty(this.selectionText)){
//				if(type == "F"){
////					this.ui.find.val(this.selectionText);
////					this.ui.find.trigger("keyup");
//				}else{
//					//this.vent.trigger("add:include:exclude",{type:type,value:this.selectionText});
//					this.vent.trigger("toggle:facet",{viewName:((type === "I") ? "include" : "exclude") +"ServiceColumns",key:Globals.serviceLogsColumns["log_message"],value:this.selectionText});
//				}
				if(type === "I" || type === "E"){
					this.vent.trigger("toggle:facet",{viewName:((type === "I") ? "include" : "exclude") +"ServiceColumns",
						key:Globals.serviceLogsColumns["log_message"],value:this.selectionText});
				}else if(type === "IA" || type === "EA"){
					this.vent.trigger("toggle:facet",{viewName:((type === "IA") ? "include" : "exclude") +"ServiceColumns",
						key:Globals.serviceLogsColumns["log_message"],value:"*"+this.selectionText+"*"});
				}
				$(".contextMenuBody [data-id='F']").show();
				$(".contextMenuBody").hide();
			}else{
				$(".contextMenuBody").hide();
			}
		},
		onQuickMenuClick : function(e){
			var that = this,$el = $(e.currentTarget);
			if($el.data("id") === "A_B"){
				var model = this.collection.get($el.parents("tr").data("id"));
				require(["views/dialog/DetailLogFileView"],function(view){
					that.renderDetailLogFileView(new view({
						model : model,
						collection : that.collection
					}));
				});
			}else if ($el.data("id") === "N_T"){
				this.onNewTabIconClick($el.find('a'));
			}else if ($el.data("id") === "C_M"){
				this.globalVent.trigger("add:compare",$el.find('a'));
			}

		},
		renderDetailLogFileView : function(view){
			var that = this;
			var opts = {
                    title: view.model.get("host")+" -> "+view.model.get("type"),
                    className : "ui-dialog-content ui-widget-content logFile",
                    content: view,
                    viewType: 'logfile',
                    resizable: false,
                    appendTo: "body",
                    modal: true,
                    width: 950,
                    height: 572,
                    buttons: [{
                        id: "cancelBtn",
                        text: "Close",
                        "class": "btn btn-default",
                        click: function() {
                            that.onDialogClosed();
                        }
                    }]
                };
			var dialog = that.dialog = new JBDialog(opts).render();
            dialog.open();
            dialog.on("dialog:closing",function(){
            	$('body').css('overflow', 'auto');
            })
            $('body').css('overflow', 'hidden');
		},
		/** closing the movable/resizable popup */
        onDialogClosed: function() {
            if (this.dialog) {
                this.dialog.close && this.dialog.close();
                this.dialog.remove && this.dialog.remove();
                this.dialog = null;
            }
        },
		renderGraph : function() {
			var that =this,diameter;
			if(this.diameter)
				diameter = this.diameter;
			else
				diameter =this.diameter = this.ui.graph.width() === 0 ? 935 : this.ui.graph.width(); /*880;*///this.ui.graph.width();//960;
			if(! (_.isArray(this.graphModel.get("graphData")) && this.graphModel.get("graphData").length > 0)){
				this.ui.graph.text("no data");
				return
			}

			var root = {
				name : "",
				dataList : this.graphModel.get("graphData")
			};

			var margin = 20;
			this.ui.graph.empty();
			//		var color = d3.scale.linear()
			//		    .domain([-1, 5])
			//		    .range(["hsl(152,90%,90%)", "hsl(228,30%,40%)"])
			//		    .interpolate(d3.interpolateHcl);
			var color = d3.scale.ordinal().domain([ 0, 1 ])
			//.range(["#ECFCBD","#ECFCBD","#ECE78F","#f4f4c8"]);
			.range([ "#dddddd", "#cccccc", "#F5F5F5" ]);
			var pack = d3.layout.pack().padding(2).size(
					[ diameter - margin, diameter - margin ]).value(
					function(d) {
						return d.count;
					}).children(function(d) {
				return d.dataList;
			})

			var svg = d3.select(this.ui.graph[0]).append("svg")
						.attr("width",diameter)
						.attr("height", diameter)
						.attr("class", "bubbleGraph")
						.append("g")
						.attr("transform","translate(" + diameter / 2 + "," + diameter / 2 + ")");

			//d3.json("flare.json", function(error, root) {

			var focus = root, nodes = pack.nodes(root), view;
			/*
			 * Tip
			 */
			var tipCirclePack = tip().attr('class', 'd3-tip')
					.offset([ -10, 0 ]).html(function(d) {
						var tempName = "<div>";
						if (d.parent) {
							if (d.depth > 1)
								tempName += (d.parent.name.split(".")[0]) + " => ";
							tempName += d.name;
						}
						return tempName + "</div>";
					})
			svg.call(tipCirclePack);
			var circle = svg.selectAll("circle").data(nodes).enter().append(
					"circle").attr(
					"class",
					function(d) {
						return d.parent ? d.children ? "node"
								: "node node--leaf " + d.name
								: "node node--root";
					}).style("fill", function(d) {
				return d.children ? color(d.depth) : null;
			}).on("click", function(d) {
				if (d3.event.shiftKey && d.depth == 2) {
					that.globalVent.trigger("render:tab",/*new LogFileView(*/{
						params : _.extend({
							host :  d.parent.name,
							component : d.name,
//							level : that.collection.queryParams.level,
//							iMessage : that.collection.queryParams.iMessage,
//							eMessage : that.collection.queryParams.eMessage,
//							query : that.collection.queryParams.query
						},that.collection.queryParams,{treeParams:null}),
						globalVent : that.globalVent
					}/*)*/);
				} else {
					if (focus !== d)
						zoom(d), d3.event.stopPropagation();
				}

			}).on('mouseover', function(d, i) {
				if (d.x) {
					tipCirclePack.show(d);
				}
			}).on('mouseout', function(d, i) {
				if (d.x) {
					tipCirclePack.hide(d);
				}
			});

			var text = svg.selectAll("text").data(nodes).enter().append("text")
					.attr("class", "label").style("fill-opacity", function(d) {
						return d.parent === root ? 1 : 0;
					}).style("display", function(d) {
						return d.parent === root ? null : "none";
					}).text(function(d) {
						if (d.count) {
							if (d.count > 0){
								if(d.depth === 1){
									return d.name.split(".")[0];
								}else
									return d.name;
							}
							else
								return "";
						} else
							return d.name;

					});

			var node = svg.selectAll("circle,text");

			d3.select(this.ui.graph[0]).style("background", color(-1)).on(
					"click", function() {
						zoom(root);
					});

			zoomTo([ root.x, root.y, root.r * 2 + margin ]);
			function zoom(d) {
				var focus0 = focus;
				focus = d;

				var transition = d3.transition().duration(
						d3.event.altKey ? 7500 : 750).tween(
						"zoom",
						function(d) {
							var i = d3.interpolateZoom(view, [ focus.x,
									focus.y, focus.r * 2 + margin ]);
							return function(t) {
								zoomTo(i(t));
							};
						});

				transition.selectAll("#"+that.ui.graph.attr("id")+" text").filter(
						function(d) {
							return d.parent === focus
									|| this.style.display === "inline";
						}).style("fill-opacity", function(d) {
					return d.parent === focus ? 1 : 0;
				}).each("start", function(d) {
					if (d.parent === focus)
						this.style.display = "inline";
				}).each("end", function(d) {
					if (d.parent !== focus)
						this.style.display = "none";
				});
			}

			function zoomTo(v) {
				var k = diameter / v[2];
				view = v;
				node.attr("transform", function(d) {
					return "translate(" + (d.x - v[0]) * k + "," + (d.y - v[1])
							* k + ")";
				});
				circle.attr("r", function(d) {
					return d.r * k;
				});
			}
		},
		onClose : function(){
			$('body').unbind("mouseup.contextMenu");
		}
	});


});
