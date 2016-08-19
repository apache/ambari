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
    'moment',
    'utils/ViewUtils',
    'collections/VLogList',
    'collections/VGroupList',
    'hbs!tmpl/tabs/HierarchyTabLayoutView_tmpl'
],function(require,Backbone,Globals,Utils,moment,ViewUtils,VLogList,VGroupList,HierarchyTabLayoutViewTmpl){
    'use strict';

	return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelView */
	{
		_viewName : 'HierarchyTabLayoutView',

		template: HierarchyTabLayoutViewTmpl,

		/** Layout sub regions */
		regions: {
			RTreeView : "#r_TreeView",
			RBubbleTable : "#r_BubbleTable",
			RLogLevel	: "#r_LogLevel",
			RHistogram  : "#r_Histogram",
			RVisualSearch : "#r_vsSearch",
			REventHistory : "#r_EventHistory",
			RVisualSearchIncCol : "#r_vsSearchIncCol",
			RVisualSearchExCol : "#r_vsSearchExCol",
			RDatePicker:"#r_DatePicker",
			RLogSnapShot:"#r_LogSnapShot",
			RAdvanceSearch:"#r_AdvanceSearch"
		},

		/** ui selector cache */
		ui: {
			applySearch : '#applySearch',
			searchBoxBtn : '[data-id="hierarchySearchBtn"]',
			searchBox : '[data-id="hierarchySearch"]',
			excludeComponents : "#excludeComponents",
			includeComponents : "#includeComponents",
			basicSearch:'[data-id="basicSearch"]',
			advanceSearch:'[data-id="advanceSearch"]',
			toggleTableAccessLog:'[data-id="toggleTableAccessLog"]'
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events["click "+this.ui.applySearch] = 'applySearchBtn';
			events["click "+this.ui.searchBoxBtn] = 'onSearchLogClick';
			events["click .server-info a"] = 'onLogLevelClick';
			events["change "+ this.ui.toggleTableAccessLog] = 'onSearchSwitch';

			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'globalVent'));
//			this.logLevelList = new VLogLevelList();
//			this.logLevelList.url = Globals.baseURL + "service/logs/levels/counts/namevalues";
//			this.logLevelList.modelAttrName = "vNameValues";
			this.columnCollection = new VLogList([],{
				state: {
                    firstPage: 0,
                    pageSize: 99999
                }
			});
			this.componentsList = new VGroupList([],{
				state: {
                    firstPage: 0,
                    pageSize: 99999
                }
			});
			this.componentsList.url = Globals.baseURL + "service/logs/components";
			this.vent = new Backbone.Wreqr.EventAggregator();

			this.defaultParams = ViewUtils.getDefaultParamsForHierarchy();
			this.bindEvents();
		},
		applyParamsDate:function(date){
			if (date) {
				var dateString  = date.split(',');
				 if(dateString.length){
				 	var checkDate = Utils.dateUtil.getMomentUTC(dateString[0]);
				 	if(checkDate.isValid()){
				 		if(dateString[1]){
				 			checkDate.millisecond(dateString[1])
				 		}else{
				 			checkDate.millisecond('000')
				 		}
				 		return  checkDate.toJSON();
				 	}
				 }
			}
		},
		bindEvents : function(){
//			this.listenTo(this.logLevelList,"reset",function(){
//				this.renderLogLevelCounts();
//			},this);
//			this.listenTo(this.vent,"main:search",function(value){
//				this.fetchLogLevelCounts(value);
//			},this);
//			this.listenTo(this.vent,"tree:search",function(value){
//            	this.fetchLogLevelCounts(value);
//            });
//			this.listenTo(this.vent,"type:mustNot",function(value){
//            	this.fetchLogLevelCounts(value);
//            });
			this.listenTo(this.componentsList, "reset", function(col, abc){
				this.setupSelect2Fields(col,"type", 'type', 'excludeComponents', 'Exclude Components');
				this.setupSelect2Fields(col,"type", 'type', 'includeComponents', 'Include Components');
			}, this);
			this.listenTo(this.vent,"reinitialize:filter:mustBe",function(value){
            	this.reinitializeFilterMustBe(value);
            },this);
			this.listenTo(this.vent,"reinitialize:filter:mustNot",function(value){
            	this.reinitializeFilterMustNot(value);
            },this);
			this.listenTo(this.vent,"add:include:exclude",function(value){
				//this.$(".vs-box").find(".fa-chevron-down").click();
			},this);
			this.listenTo(this.vent,"tab:refresh",function(params){
				this.reRenderComponents(params);
			},this);

			this.listenTo(this.globalVent,"reinitialize:serviceLogs",function(options){
            	this.vent.trigger("reinitialize:filter:tree reinitialize:filter:include:exclude reinitialize:filter:bubbleTable"+
            			" reinitialize:filter:mustNot reinitialize:filter:mustBe reinitialize:filter:level reinitialize:filter:logtime",options);
            },this);
		},
		onRender : function(){
			//this.renderTreeView();
			this.fetchServiceLogsColumns();
			this.renderLogLevel();
			this.renderEventHistory();
			this.renderHistogram();
			this.renderDatePicker();
			//this.renderVSSearch();
			this.renderLogSnapShot();
//			this.fetchLogLevelCounts({q:"*:*"});
			this.componentsList.fetch({reset:true});
		},
		onShow:function(){

			//this.REventHistory.currentView.genrateTimeline();

		},
		fetchServiceLogsColumns : function(){
			var that = this;
			this.columnCollection.getServiceLogSchemaFields({},{
//				beforeSend : function(){
//					that.$("#loaderAudit").show();
//				},
				success : function(data){
					Globals.serviceLogsColumns = data;
				},
				error : function(error,data,status){
					var obj = JSON.parse(error.responseText);
					if(obj)
						Utils.notifyError({content:obj.msgDesc});
				},
				complete : function(){
					that.renderServiceColumnsVSSearch();
					that.renderBubbleTableView();
				}
			});
		},
//		fetchLogLevelCounts : function(params){
//			$.extend(this.logLevelList.queryParams,params);
//			this.logLevelList.fetch({reset:true});
//		},
		renderTreeView : function(){
			var that = this;
			require(['views/tabs/TreeView'],function(TreeView){
    			that.RTreeView.show(new TreeView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams
				}));
            });
		},
		renderBubbleTableView : function(){
			var that = this;
			require(['views/dashboard/BubbleGraphTableLayoutView'],function(BubbleTableLayoutView){
    			that.RBubbleTable.show(new BubbleTableLayoutView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
					columns:Globals.serviceLogsColumns,
					quickHelp : true
				}));
            });
		},
		renderLogLevel : function(){
			var that = this;
			require(['views/dashboard/LogLevelBoxView'],function(LogLevelBoxView){
	    		that.RLogLevel.show(new LogLevelBoxView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams
				}));
            });
		},
		renderHistogram : function(){
			var that = this;
			require(['views/graphs/GraphLayoutView'],function(GraphLayoutView){
	    		that.RHistogram.show(new GraphLayoutView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
					showUnit : true,
					futureDate : true
				}));
            });
		},
		renderDatePicker:function(){
			var that = this;
			require(['views/common/DatePickerLayout'],function(DatePickerLayout){
	    		that.RDatePicker.show(new DatePickerLayout({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
					datePickerPosition:'left',
					rangeLabel: true,
					parentEl: that.$el.find('.topLevelFilter')
				}));
            });
		},
		renderLogSnapShot:function(){
			var that = this;
			require(['views/common/LogSnapShotLayout'],function(LogSnalShopLayout){
	    		that.RLogSnapShot.show(new LogSnalShopLayout({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
				}));
            });
		},
		renderAdvanceSearch:function(){
			var that = this;
			require(['views/common/AdvanceSearchLayout'],function(AdvanceSearchLayout){
	    		that.RAdvanceSearch.show(new AdvanceSearchLayout({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams,
				}));
            });


		},
		renderVSSearch : function(){
			var that = this;
			require(['views/tabs/VisualSearchView'],function(VisualSearchView){
	    		that.RVisualSearch.show(new VisualSearchView({
					viewName : "includeExclude",
					vent : that.vent,
					globalVent:that.globalVent,
					eventName : "search:include:exclude",
					myFormatData : function(query,searchCollection){
						var include = [],exclude=[];
						searchCollection.each(function(m){
								if(m.get("category") === "Exclude"){
									(! _.isEmpty(m.get("value"))) ? exclude.push(m.get("value")):'';
								}
								else{
									(! _.isEmpty(m.get("value"))) ? include.push(m.get("value")):'';
								}
							});
							return {
								iMessage : Utils.encodeIncludeExcludeStr(include,true),
								eMessage : Utils.encodeIncludeExcludeStr(exclude,true),
								query : query
							};
						}

				}));
            });
		},
		renderServiceColumnsVSSearch : function(){
			//Columns include exclude
			var that = this;
			require(['views/tabs/VisualSearchView'],function(VisualSearchView){
				var data = _.values(Globals.serviceLogsColumns);
				var columns = _.without( data, _.findWhere( data, "logtime"));
				that.RVisualSearchIncCol.show(new VisualSearchView({
					viewName : "includeServiceColumns",
					placeholder : "Include Search",
					vent : that.vent,
					globalVent:that.globalVent,
					customOptions : columns,
					eventName : Globals.eventName.serviceLogsIncludeColumns,
					myFormatData : function(query,searchCollection){
						var obj=[];
						searchCollection.each(function(m){
							var data = {};
							data[m.get("category")] = m.get("value");
							obj.push(data);
						});
						return {
							includeQuery : JSON.stringify(obj),
							query : query
						}
					}
				}));
				that.RVisualSearchExCol.show(new VisualSearchView({
					viewName : "excludeServiceColumns",
					placeholder : "Exclude Search",
					vent : that.vent,
					globalVent:that.globalVent,
					customOptions :  columns,
					eventName : Globals.eventName.serviceLogsExcludeColumns,
					myFormatData : function(query,searchCollection){
						var obj=[];
						searchCollection.each(function(m){
							var data = {};
							data[m.get("category")] = m.get("value");
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
		renderEventHistory:function(){
			var that = this;
			require(['views/common/EventHistoryLayout'],function(EventHistoryLayoutView){
				that.REventHistory.show(new EventHistoryLayoutView({
					vent : that.vent,
					globalVent:that.globalVent,
					params : that.defaultParams
				}));
			});
		},
		fetchCollection : function(params){
			$.extend(this.collection.queryParams, params);
			this.collection.fetch({reset:true});
		},
		onSearchLogClick : function(){
			var value = this.ui.searchBox.val();
			if(_.isEmpty(value)){
				this.ui.searchBox.val("*:*");
				value = "*:*";
			}
			this.vent.trigger("main:search",{q:value});
		},
//		renderLogLevelCounts : function(){
//			var that = this;
//			this.logLevelList.each(function(model){
//				that.$("[data-total='"+model.get("name")+"']").text(model.get("value"));
//			});
//		},
//		onLogLevelClick : function(e){
//			var $el = $(e.currentTarget);
//			if($el.hasClass("active")){
//				$el.removeClass("active");
//			}else{
//				$el.addClass("active");
//			}
//			var params = [];
//			_.each(this.$(".server-info a.active"),function(e){
//				params.push($(e).find("strong").data("total"));
//			});
//			this.vent.trigger("level:filter",{level:params.toString()});
//		},
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
				dropdownParent: that.$el,
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
			}).off("change").on("change",function(e){
				var data = that.ui[selectTagId].select2("data").map(function(d){return d.type});
				if(selectTagId === "excludeComponents"){
					that.vent.trigger("tree:strike:component",data);
					that.vent.trigger("type:mustNot",{mustNot:data.toString()});
				}
				if(selectTagId === "includeComponents")
					that.vent.trigger("type:mustBe",{mustBe:data.toString()});
			});
		},
		reinitializeFilterMustBe : function(values){
			if(values.mustBe)
				this.ui.includeComponents.select2('val',values.mustBe.split(","));
			else
				this.ui.includeComponents.select2('val',[]);
		},
		reinitializeFilterMustNot : function(values){
			if(values.mustNot)
				this.ui.excludeComponents.select2('val',values.mustNot.split(","));
			else
				this.ui.excludeComponents.select2('val',[]);

		},
		reRenderComponents : function(params){
			var iComponents = this.ui.includeComponents.val(),eComponents = this.ui.excludeComponents.val(),that=this;
			this.componentsList.fetch({
				reset : true,
				complete : function(){
					that.ui.includeComponents.select2('val',iComponents.split(","));
					that.ui.excludeComponents.select2('val',eComponents.split(","));
				}
			});
		},
		onSearchSwitch:function(e){
			var obj = {}
			if(e.target.checked){
				this.ui.advanceSearch.show();
				this.ui.applySearch.show();
				this.ui.basicSearch.hide();
				obj.advanceSearch = this.RAdvanceSearch.currentView.ui.searchArea.val();
				obj.includeQuery = null;
				obj.excludeQuery = null;
			} else{
				this.ui.advanceSearch.hide();
				this.ui.applySearch.hide();
				this.ui.basicSearch.show();
				obj = this.getIncludeExcludeColValues();
				obj.advanceSearch = null;
			}

			this.vent.trigger('main:search',obj);

		},
		applySearchBtn : function(){
			var obj = {}
			obj.advanceSearch = this.RAdvanceSearch.currentView.ui.searchArea.val();

				this.vent.trigger('main:search',obj);

		},
		getIncludeExcludeColValues : function(){
			return _.extend(this.RVisualSearchIncCol.currentView.formatData(this.RVisualSearchIncCol.currentView.visualSearch.searchBox.value(),this.RVisualSearchIncCol.currentView.visualSearch.searchQuery),
			           this.RVisualSearchExCol.currentView.formatData(this.RVisualSearchExCol.currentView.visualSearch.searchBox.value(),this.RVisualSearchExCol.currentView.visualSearch.searchQuery));
		},
		toggleSearchBackToBasicSearch : function(options){
			if(! this.ui.advanceSearch.is(":hidden")){
				this.ui.advanceSearch.hide();
				this.ui.basicSearch.show();
				this.ui.toggleTableAccessLog[0].checked = false;
				_.extend(options,this.getIncludeExcludeColValues(),{advanceSearch:null});
			}
		}
	});


});
