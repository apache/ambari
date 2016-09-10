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
	'hbs!tmpl/dashboard/DashboardView_tmpl',
	'collections/VGroupList',
	'select2',
	'd3'
],function(require,Backbone,Globals,DashboardviewTmpl,VGroupList){
    'use strict';


	var DashboardView = Backbone.Marionette.Layout.extend(
	/** @lends DashboardView */
	{
		_viewName : 'DashboardView',

		template: DashboardviewTmpl,

		/** Layout sub regions */
		regions: {
			RLogDetail : "#r_LogDetail"
		},

		/** ui selector cache */
		ui: {
			hosts : "#hosts",
			components : "#components",
			time	: "#time"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click #searchLog'] = 'onSearchLogClick';
			//events['click #searchLog'] = 'onSearchLogClick';
			return events;
		},

		/**
		 * intialize a new DashboardView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options, 'collection'));
			this.setupCollections();
			this.bindEvents();
			this.hostCollection.fetch({reset:true});
			this.cComponents.fetch({reset:true});
		},
		setupCollections : function(){
			this.logRegions = new Backbone.Collection();
			this.hostCollection = new VGroupList([],{});
			this.cComponents = new VGroupList([],{});
			this.cComponents.url = Globals.baseURL + "service/logs/components";
			this.cTime = new VGroupList(Globals.timeQueryLOV,{});
		},
		/** all events binding here */
		bindEvents : function(){
			this.listenTo(this.hostCollection, "reset", function(col, abc){
				this.setupSelect2Fields(col,"host", 'host', 'hosts');
			}, this);
			this.listenTo(this.cComponents, "reset", function(col, abc){
				this.setupSelect2Fields(col,"type", 'type', 'components');
			}, this);
		},

		/** on render callback */
		onRender: function() {
			this.setupSelect2Fields(this.cTime,"value","text","time");
		},
		showLogDetail : function(){
			var that = this;
			require(['views/dashboard/LogDetailView'],function(LogDetailView){
				that.RLogDetail.show(new LogDetailView({}));
			})
		},
		setupSelect2Fields: function(col, idKey, textKey, selectTagId){
			var that = this, data = [];
			data = _.pluck(col.models, 'attributes');

			this.ui[selectTagId].select2({
				placeholder: 'Select',
				allowClear : true,
				width: '100%',
				data: { results: data, text: textKey },
				formatSelection: function(item){
					return item[textKey];
				},
    			formatResult: function(item){
    				return item[textKey];
				}
			});
		},
		onSearchLogClick : function(e){
			var searchParams = this.getSearchparams();

			if(this.logRegions.length == 0){
				var model = new Backbone.Model({
					id : 1,
					params : searchParams
				});
				this.generateView(model);
			}else{

				var existsMod = this.logRegions.find(function(m){
					return JSON.stringify(searchParams) === JSON.stringify(m.get('params'))
				});
				if(existsMod){
					$('html, body').animate({
				        'scrollTop' : this.$("#"+existsMod.get("tabName")).position().top
				    });
					return;
				}
				var lastModel = this.logRegions.last();
				var model = new Backbone.Model({
					id : parseInt(lastModel.get("id"),10) + 1,
					params : searchParams
				});
				this.generateView(model);
			}
		},
		generateView : function(model){
			var tabName = "r_LogDetail"+model.get("id");
			model.set("tabName",tabName);
			$('<div/>', {
				'id': tabName,
				'class': 'r-tab-content col-md-12',
			}).appendTo(this.$('#r_LogDetail'));
			var region = {};
			region[tabName] = '#' + tabName;
			this.addRegions(region);
			this.logRegions.add(model);
			this.renderRegion(model);
		},
		renderRegion : function(model){
			var region = this.getRegion(model.get("tabName"));
			require(['views/dashboard/LogDetailView'],function(LogDetailView){
				region.show(new LogDetailView({model: model}));
			})

		},
		getSearchparams : function(){
			var obj={hosts : null, components : null, time:null};
			if(this.ui.hosts.select2("data") != undefined || this.ui.hosts.select2("data") != null){
				if(this.ui.hosts.select2("data").host)
					obj.hosts = this.ui.hosts.select2("data").host;
			}
			if(this.ui.components.select2("data") != undefined || this.ui.components.select2("data") != null){
				if(this.ui.components.select2("data").host)
					obj.components = this.ui.components.select2("data").type;
			}
			if(this.ui.time.select2("data") != undefined || this.ui.time.select2("data") != null){
				if(this.ui.time.select2("data").id)
					obj.time = this.ui.time.select2("val");
			}
			return obj;

		},
		/** on close */
		onClose: function(){
		}

	});

	return DashboardView;
});
