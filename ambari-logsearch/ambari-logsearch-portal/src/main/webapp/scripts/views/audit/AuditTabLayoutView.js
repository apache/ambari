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
	'utils/Utils',
	'utils/ViewUtils',
	'collections/VGroupList',
	'collections/VAuditLogList',
	'models/VAuditLog',
	'hbs!tmpl/audit/AuditTabLayoutView_tmpl',
	'moment'
],function(require,Backbone,Globals,Utils,ViewUtils,VGroupList,VAuditLogList,VAuditLog,AuditTabLayoutViewTmpl,moment){

    'use strict';

    return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelView */
	{
		_viewName : 'AuditTabLayoutView',

		template: AuditTabLayoutViewTmpl,

		/** Layout sub regions */
		regions: {
			RAuditTable : "[data-id='auditTable']",
			RVisualSearch : "#r_vsSearch",
			RVisualSearchInc : "#r_vsSearchInc",
			RVisualSearchEx : "#r_vsSearchEx",
			RAuditLine  : "#r_AuditLine",
			RAuditAggregated  : "[data-id='auditAggregated']",
		},

		/** ui selector cache */
		ui: {
			viewType: "input[name='viewTypeAudit']",
			excludeComponents : "#excludeComponents",
			includeComponents : "#includeComponents",
			collapseArrowClick : "a.collapse-link.chkArrow"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click [data-id="refresh-tab-audit"]']  = 'onAuditTabRefresh';
			events['change ' + this.ui.viewType]  = 'onViewTypeChange';
			events['click ' +this.ui.collapseArrowClick] = function(e){
				if($(e.currentTarget).find('i').hasClass('fa-chevron-down')){
					if(this.RAuditAggregated.currentView){
						this.RAuditAggregated.currentView.trigger("button:min:max");
					}
				}
			}
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'globalVent'));
			this.defaultParams = ViewUtils.getDefaultParamsForHierarchy();
			delete this.defaultParams.level;
			this.vent = new Backbone.Wreqr.EventAggregator();
			this.initializeCollections();
			this.columns = [];
			this.bindEvents();
		},
		initializeCollections : function(){
			this.auditModel = new VAuditLog();
			this.collection = new VAuditLogList([],{
				state: {
                    firstPage: 0,
                    pageSize: 25
                }
			});

			this.componentsList = new VGroupList([],{
				state: {
                    firstPage: 0,
                    pageSize: 99999
                }
			});
			this.componentsList.url = Globals.baseURL + "audit/logs/components";
		},
		bindEvents : function(){
			this.listenTo(this.componentsList, "reset", function(col, abc){
				this.setupSelect2Fields(col,"type", 'type', 'excludeComponents', 'Exclude Components');
				this.setupSelect2Fields(col,"type", 'type', 'includeComponents', 'Include Components');
			}, this);
			this.listenTo(this.vent,"search:audit:query auditComponents:include auditComponents:exclude search:audit:include search:audit:exclude logtime:filter",function(value){
				_.extend(this.defaultParams,value);
            	this.fetchAuditLogs(value);
            }, this);
			this.listenTo(this.vent,"reinitialize:filter:mustBe",function(value){
            	this.reinitializeFilterMustBe(value);
            },this);
			this.listenTo(this.vent,"reinitialize:filter:mustNot",function(value){
            	this.reinitializeFilterMustNot(value);
            },this);

			this.listenTo(this.globalVent,"reinitialize:auditLogs",function(options){
            	this.vent.trigger("reinitialize:filter:mustNot reinitialize:filter:mustBe reinitialize:filter:logtime "+
            			"reinitialize:TopTenGraph",options);
            	this.fetchAuditLogs(options);
            },this);

		},
		onRender : function(){
			this.renderHistogram();
			this.renderAuditAggregatedInfo();
			this.fetchAuditColumns();
			this.fetchAuditLogs((this.defaultParams) ? this.defaultParams : {q:"*:*"});
			this.componentsList.fetch({reset:true});
			//this.startPoll();
		},
		renderHistogram : function(){
			var that = this;
			require(['views/graphs/GraphLayoutView'],function(GraphLayoutView){
				that.RAuditLine.show(new GraphLayoutView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
					//parentView : this,
					viewType : Globals.graphType.MULTILINE.value,
					showDatePicker:true,
					futureDate : false
				}));
			})
		},
		renderAuditAggregatedInfo : function(){
			var that = this;
			require(['views/audit/AuditAggregatedView'],function(AuditAggregatedView){
				that.RAuditAggregated.show(new AuditAggregatedView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams
				}));
			})

		},
		fetchAuditColumns : function(){
			var that =this;
			this.collection.getAuditSchemaFieldsName({},{
				beforeSend : function(){
					that.$("#loaderAudit").show();
				},
				success : function(data){
					that.columns = data;
				},
				error : function(error,data,status){
					var obj = JSON.parse(error.responseText);
					if(obj)
						Utils.notifyError({content:obj.msgDesc});
				},
				complete : function(){
					that.renderAuditTable();
					that.renderVSSearch();
					that.$("#loaderAudit").hide();
				}
			});
		},
		fetchAuditLogs : function(params){
			$.extend(this.collection.queryParams,params);
			this.collection.getFirstPage({reset:true});
		},
		renderVSSearch : function(){
			var that = this;
			require(['views/tabs/VisualSearchView'], function(VisualSearchView){

				_.each(that.columns,function(v,i){
					if(v.toLowerCase().indexOf("time") > 0 ){
						//that.columns.splice(i, 1);
						delete that.columns[v]
					}
				});
				that.RVisualSearchInc.show(new VisualSearchView({
					viewName : "includeColumns",
					placeholder : "Include Search",
					vent : that.vent,
					globalVent:that.globalVent,
					customOptions : _.values(that.columns),
					eventName : "search:audit:include",
					myFormatData : function(query,searchCollection){
						var obj=[];
						searchCollection.each(function(m){
							var data = {};
							data[m.get("category")] = Utils.manipulateValueForAddingAstrik(m.get("value"));
							obj.push(data);
						});
						return {
							includeQuery : JSON.stringify(obj),
							query : query
						}
					}
				}));
				that.RVisualSearchEx.show(new VisualSearchView({
					placeholder : "Exclude Search",
					vent : that.vent,
					globalVent:that.globalVent,
					customOptions : _.values(that.columns),
					eventName : "search:audit:exclude",
					myFormatData : function(query,searchCollection){
						var obj=[];
						searchCollection.each(function(m){
							var data = {};
							data[m.get("category")] = Utils.manipulateValueForAddingAstrik(m.get("value"));
							obj.push(data);
						});
						return {
							excludeQuery : JSON.stringify(obj),
							query : query
						}
					}
				}));
			});
		},
		renderAuditTable : function(){
			var that = this;
			require(['views/common/TableLayout'], function(TableLayout){
				var cols = new Backgrid.Columns(that.getColumns());
				that.RAuditTable.show(new TableLayout({
					columns: cols,
					collection: that.collection,
					includeFilter : false,
					includePagination : true,
					includePageSize : true,
					includeFooterRecords : true,
					includeColumnManager : true,
					columnOpts : {
				    	initialColumnsVisible: 9,
				    	saveState : false
					},
					gridOpts : {
						className : "table table-bordered table-hover table-condensed backgrid",
						emptyText : 'No records found!'
					},
					filterOpts : {},
					paginatorOpts : {}
				}));
			});
		},
		getColumns : function(){
			var cols = {};
			var that = this;
			_.each(this.columns,function(value,col){
				cols[col] = {
						label:value,
						cell: "String",
						sortType: 'toggle',
						editable: false,
				}
			});
			var columns = {
					evtTime : {
						label : "Event Time",
						cell: "String",
						sortType: 'toggle',
						editable: false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function(rawValue, model){
								return moment(rawValue).format("YYYY-MM-DD HH:mm:ss,SSS");
							}
						})
					},
					reqUser : {
						label : "User",
						cell: "String",
						sortType: 'toggle',
						editable: false
					},
					repo : {
						label : 'Repo',
						cell: "String",
						sortType: 'toggle',
						editable:false
					},
					resource : {
						label : 'Resource',
						cell: "String",
						sortType: 'toggle',
						editable:false
					},
					access : {
						label : 'Access Type',
						cell: "String",
						sortType: 'toggle',
						editable:false
					},
					result : {
						label : 'Result',
						cell: "String",
						sortType: 'toggle',
						editable:false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function(rawValue, model){
								return (rawValue === 1) ? "Allowed" : "Denied";
							}
						})
					},
					enforcer : {
						label : 'Access Enforcer',
						cell: "String",
						sortType: 'toggle',
						editable:false
					},
					cliIP : {
						label : 'Client IP',
						cell: "String",
						sortType: 'toggle',
						editable:false
					},
					event_count : {
						label : 'Event Count',
						cell: "String",
						sortType: 'toggle',
						editable:false
					}
			};
			_.each(cols,function(c,k){
				if(columns[k] == undefined && k != "_version_"){
					columns[k] = c;
				}else{
					if(columns[k] && columns[k].label){
						columns[k].label = c.label;
					}
				}
			})
			/*_.each(columns,function(c,k){
				if(columns[k] == undefined && k != "_version_"){
					columns[k] = c;
				}
			})*/
			return this.collection.constructor.getTableCols(columns, this.collection);
		},
		onAuditTabRefresh : function(e){
			ViewUtils.setLatestTimeParams(this.defaultParams);
			//this.fetchAuditColumns();
			$.extend(this.collection.queryParams,this.defaultParams);
			this.collection.fetch({reset:true});
			this.vent.trigger("tab:refresh",this.defaultParams);
		},
		onViewTypeChange: function(e){
			var that = this;
			var val = that.$("[name='viewTypeAudit']:checked").val();
			this.toggleViewType(val);
		},
		toggleViewType : function(val){
			if(val === "A"){
				this.$("[data-id='auditTable']").show();
				this.$('[data-id="auditAggregated"]').hide();
			}else{
				this.$('[data-id="auditAggregated"]').show();
				this.$("[data-id='auditTable']").hide();
				if(this.RAuditAggregated.currentView)
					this.RAuditAggregated.currentView.$el.resize();
			}
		},
		setupSelect2Fields : function(col, idKey, textKey, selectTagId, placeHolder){
			var that = this, data = [];
			data = _.pluck(col.models, 'attributes');
//			data = data.map(function(obj){
//				return {id : obj[idKey], text : obj[textKey]}
//			})
			//data.unshift({'id': null, 'text': null});
			for(var i=0;i < data.length;i++){
				data[i].id = data[i].type;
			}
			this.ui[selectTagId].select2({
				placeholder: (placeHolder) ? placeHolder :'Select',
				tags:true,
				allowClear : true,
				width: '100%',
				data: { results: data, text: textKey},
				formatSelection: function(item){
					return item[textKey];
				},
    			formatResult: function(item){
    				return item[textKey];
				}
			}).on("change",function(e){
				var data = that.ui[selectTagId].select2("data").map(function(d){return d.type});
				if(selectTagId === "excludeComponents"){
					that.vent.trigger("auditComponents:exclude",{mustNot:data.toString()});
				}
				if(selectTagId === "includeComponents")
					that.vent.trigger("auditComponents:include",{mustBe:data.toString()});
			});
		},
		startPoll : function(){
			var that = this;
			setInterval(function(){
				that.pollLiveFeed();
			},5000);
		},
		pollLiveFeed : function(){
			var that = this;
			if(this.pollXhr){
				if(this.pollXhr.readyState > 0 && this.pollXhr.readyState < 4)
					return
			}
			this.pollXhr = this.auditModel.auditLiveFeed({},{
				beforeSend : function(){
					that.$("#loaderFeed").show();
				},
				success : function(data){
					var dd=[];
					that.$("#spark").parent().show();
					_.each(data.vnameValues,function(d){
						dd.push(d.value);
					});
					that.$("#spark").sparkline(dd, {lineColor: '#5A8DB6',width:"100px",
						/*tooltipFormatter : function(){
							console.log(arguments)
						}*/});
				},
				error : function(){
					that.$("#spark").parent().hide();
				},
				complete : function(){
					setTimeout(function(){
						that.$("#loaderFeed").hide();
					},1000);
				}
			});
		},
		reinitializeFilterMustBe : function(values){
			if(values.mustBe){
				this.ui.includeComponents.select2('val',values.mustBe.split(","));
			}else{
				this.ui.includeComponents.select2('val',[]);
			}
		},
		reinitializeFilterMustNot : function(values){
			if(values.mustNot){
				this.ui.excludeComponents.select2('val',values.mustNot.split(","));
			}else{
				this.ui.excludeComponents.select2('val',[]);
			}
		}
	});
});
