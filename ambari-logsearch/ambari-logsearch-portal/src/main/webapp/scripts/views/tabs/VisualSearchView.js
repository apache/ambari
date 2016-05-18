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
    'collections/VLogLevelList',
    'hbs!tmpl/tabs/VisualSearchView_tmpl',
    'select2'
],function(require,Backbone,Globals,Utils,VLogLevel,LogLevelTmpl){
    'use strict';

	return Backbone.Marionette.Layout.extend(
	/** @lends VisualSearchView */
	{
		_viewName : 'VisualSearchView',

		template: LogLevelTmpl,

		/** Layout sub regions */
		regions: {
		},

		/** ui selector cache */
		ui: {
			vsContainer : "#vs_container",
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click #searchLog'] = 'onSearchLogClick';
			return events;
		},

		/**
		 * intialize a new VisualSearchView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params','customOptions','eventName','myFormatData','placeholder', 'viewName'));
			this.bindEvents();
		},
		onRender : function(){
			var query="";
//			if(this.customOptions){
//				
//			}
//			if(this.params && this.params.query){
//				query = this.params.query;
//			}
			this.initializeSearch(this.formQueryDataFromParams(this.params));
		},
		bindEvents : function(){
			this.listenTo(this.vent,"reinitialize:filter:include:exclude",function(value){
            	this.reinitializeFilter(value);
            });
			this.listenTo(this.vent,"add:include:exclude",function(value){
            	this.addIncludeExclude(value);
            });
			this.listenTo(this.vent,"toggle:facet",function(obj){
            	this.toggleFacet(obj);
            });
		},
		initializeSearch : function(query) {
			var that = this;
			var opts = (this.customOptions) ? this.customOptions : [ "Include", "Exclude" ],that=this;
			this.visualSearch = VS.init({
				placeholder : (!this.placeholder) ? "Search String" : this.placeholder,
				container : this.ui.vsContainer,
				query : query,
				remainder : false,
				callbacks : {
					search : function(query, searchCollection) {
						var eventName = (! that.eventName) ? "search:include:exclude" : that.eventName;  
//						if(! that.customOptions)
//							that.vent.trigger("search:include:exclude",that.formatData(query,searchCollection));
//						else
//							that.vent.trigger("search:audit:query",that.formatData(query,searchCollection));
						that.vent.trigger(eventName,that.formatData(query,searchCollection));
					},
					facetMatches : function(callback) {
//						var searchOptTemp = $.extend(true, [], opts);
//						that.visualSearch.searchQuery.each(function(m) {
//							if ($.inArray(m.get('category'), searchOptTemp) >= 0) {
//								searchOptTemp.splice($.inArray(m.get('category'),
//										searchOptTemp), 1);
//							}
//						});
//						callback(searchOptTemp,{preserveOrder: true});
						callback(opts,{preserveOrder: true});
					},
					valueMatches : function(facet, searchTerm, callback) {
//						that.$('vsLoader').show();
//						$.ajax({
//							url: '/service/dashboard/getSuggestoins?fieldName='+facet+'&valueToSuggest='+searchTerm+'',
//							type: 'GET',
//							success: function(result){
//								console.log(result);
//								callback(result,{preserveOrder: false});
//							},
//							error:function(){},
//							complete:function(){
//								//that.$('vsLoader').hide();
//							}
//						});
					}
				}
			});
		},
		formatData : function(query,searchCollection){
			if(_.isFunction(this.myFormatData)){
				return this.myFormatData(query,searchCollection);
			}
			var include = [],exclude=[],obj=[],that=this;
			searchCollection.each(function(m){
				if(! that.customOptions){
					if(m.get("category") === "Exclude"){
						(! _.isEmpty(m.get("value"))) ? exclude.push(m.get("value")):'';
					}
					else{
						(! _.isEmpty(m.get("value"))) ? include.push(m.get("value")):'';
					}
				}else{
					var data = {};
					data[m.get("category")] = m.get("value");
					obj.push(data);
				}
				
			});
			if(! this.customOptions){
				return {
					iMessage : Utils.encodeIncludeExcludeStr(include,true),
					eMessage : Utils.encodeIncludeExcludeStr(exclude,true),
					query : query
				};
			}else{
				return {
					columnQuery : JSON.stringify(obj),
					query : query
				}
			}
				
					
		},
		reinitializeFilter : function(values){
			var query = "";
			if(this.viewName){
				query = this.formQueryDataFromParams(values);
			}
			if(! _.isUndefined(query)){
				this.initializeSearch(query);
				//this.vent.trigger("search:include:exclude",values);
			}
		},
		addIncludeExclude : function(value){
			//If this view is running for options other then Include/Exclude than return
			if(this.customOptions)
				return
			if(value){
				var e = $.Event("keydown");
				e.keyCode = 13;
				this.visualSearch.searchBox.addFacet((value.type == "I") ? "Include" : "Exclude",value.value);
				this.visualSearch.options.callbacks.search(this.visualSearch.searchBox.value(), this.visualSearch.searchQuery);
//				this.visualSearch.searchBox.searchEvent(e);
//				this.visualSearch.searchBox.removeFocus();
			}
		},
		toggleFacet : function(obj){
			if(_.isObject(obj)){
				if(obj.viewName == this.viewName){
//					var e = $.Event("click");
//					e.keyCode = 13;
//					var model = _.find(this.visualSearch.searchQuery.models,function(m){
//							return m.get("category") === obj.key && m.get("value") === obj.value;
//					});
					//obj.value = obj.value.replace(/:/, '\\\\:').replace(/\"/g, '\\"').replace(/\n/g, "\\n");
					var view = _.find(this.visualSearch.searchBox.facetViews,function(v){
						return v.model.get("category") === obj.key && v.model.get("value") === obj.value;
					});
					if(view){
						var e = $.Event("click");
						e.keyCode = 13;
						view.model.set("value",undefined);
						view.remove(e);
					}else
						this.visualSearch.searchBox.addFacet(obj.key,obj.value);
//					var keypress = $.Event("keydown");
//					keypress.keyCode = 13;
//					this.visualSearch.searchBox.searchEvent(keypress);
//					this.visualSearch.searchBox.removeFocus();
					this.visualSearch.options.callbacks.search(this.visualSearch.searchBox.value(), this.visualSearch.searchQuery);
				}
			}
		},
		formQueryDataFromParams : function(values){
			if(!values)
				return
			var query = "";
			try{
				var arr;
				if(this.viewName == "includeServiceColumns" && values.includeQuery){
					arr = JSON.parse(values.includeQuery);
				}else if(this.viewName == "excludeServiceColumns" && values.excludeQuery){
					arr = JSON.parse(values.excludeQuery);
				}
				if(_.isArray(arr)){
					for(var i=0; i<arr.length; i++){
						var key = _.keys(arr[i])[0]; 
						query += "'"+key+ "':'" +arr[i][key] +"' ";
					}
				}
				if(this.viewName === "includeExclude" && (values.iMessage || values.eMessage)){
					var valuesArr,key;
					if(values.iMessage){
						valuesArr = values.iMessage.split(Globals.splitToken);
						key = "Include: ";
						for(var i=0; i<valuesArr.length; i++){
							query += key+ '"'+valuesArr[i]+'" ';
						}
					}
					if(values.eMessage){
						valuesArr = values.eMessage.split(Globals.splitToken);
						key = "Exclude: ";
						for(var i=0; i<valuesArr.length; i++){
							query += key+ '"'+valuesArr[i]+'" ';
						}
					}
					
					
				}
			}catch(e){
				console.log("Error",e);
			}
			return query;
		}
	});
	
	
});