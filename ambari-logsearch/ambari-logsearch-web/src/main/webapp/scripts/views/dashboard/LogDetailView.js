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
	'collections/VLogList',
	'hbs!tmpl/dashboard/LogDetailView_tmpl'
],function(require,Backbone,VUserList,LogDetailViewTmpl){
    'use strict';
	
	var LogDetailView = Backbone.Marionette.ItemView.extend(
	/** @lends LogDetailView */
	{
		_viewName : 'LogDetailView',

		template: LogDetailViewTmpl,
		
		templateHelpers : function(){
//			return {
//				logData : this.logData
//			};
		},

		/** ui selector cache */
		ui: {
			btnPin : ".btn-pin",
			searchTags : "#tags"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click ' + this.ui.btnPin] = 'onBtnPinClick';
			events['click .btn-minimize'] = 'onBtnMinimizeClick';
			return events;
		},

		/**
		 * intialize a new LogDetailView ItemView
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options));
			
			this.logData = {};
			this.collection = new VUserList([], {
                state: {
                    firstPage: 0,
                    pageSize: 50
                }
            });
			var query = {q:""};
			if(this.model.get("params")){
				if(this.model.get("params").hosts){
					query.hosts = this.model.get("params").hosts; 
				}
				if(this.model.get("params").components){
					query.components = this.model.get("params").components
				}
				if(this.model.get("params").time){
					query.time = this.model.get("params").time
				}
			}
			this.mergeParams(query);
			this.isLoading = false;
			this.bindEvents();
		},
		/** all events binding here */
		bindEvents : function(){
			this.listenTo(this.collection, "reset", function(){
				this.renderLogLines(this.collection);
			}, this);
			this.listenTo(this.collection, 'request', function(){
				this.isLoading = true;
				this.$("#logLines").append('<div class="loading-lines" align="center"><img src="images/loading.gif"/></div>');
			},this);
            this.listenTo(this.collection, 'sync error', function(){
            	this.isLoading = false;
            	this.$(".loading-lines").remove();
			},this);
		},

		/** on render callback */
		onRender: function() {
			this.fetchCollection();
			this.initializePlugins();
			
		},
		initializePlugins : function(){
			var that = this;
			this.infiniteScrolling();
			this.ui.searchTags.select2({
				 placeholder: "Manual search",
				 closeOnSelect : true,
				 tags:true,
				 multiple: true,
				 minimumInputLength: 1,
				 tokenSeparators: [",", " "]
			}).on("change",function(){
				that.$("#logLines").empty();
				var q = "";
				if(  _.isObject(that.ui.searchTags) && (! _.isEmpty(that.ui.searchTags.select2('val'))) ){
					var arr = that.ui.searchTags.select2('val'),qArr=[];
					
					_.each(arr,function(d){
						qArr.push("*"+d+"*");
					});
					q = qArr.toString();
				}
				that.mergeParams({q : q});
				that.collection.getFirstPage({reset:true});
			});
		},
		initializeResize : function(){
			if(! this.$('.record').hasClass("ui-resizable")){
				var org = this.$('.record').height(),that=this;
				this.$('.record').resizable({ghost:false,minHeight: 386,handles: 's'}).on("resize",function(e,ui){
					var current = $(e.currentTarget).height();
					var height = (current - org) + that.$('.record-panel').height(); 
					that.$('.record').find('.record-panel').height(height);
					org += (current - org);
				});
			}
		},
		mergeParams : function(obj){
			$.extend(this.collection.queryParams, obj);
		},
		fetchCollection : function(){
			this.collection.fetch({reset:true});
		},
		infiniteScrolling : function(){
			var that = this;
			this.$('.record-panel').bind('scroll', function() {
		        if($(this).scrollTop() + $(this).innerHeight() >= this.scrollHeight) {
		        	if(! that.isLoading){
		        		that.infiniteScrollCallBack();
		        	}
		        		
		        }
		    })
		},
		infiniteScrollCallBack : function(){
			if(this.collection.hasNext())
				this.collection.getNextPage({reset:true});
		},
		onBtnPinClick : function(e){
			this.ui.btnPin.toggleClass("unpin");
		},
		onBtnMinimizeClick : function(e){
			e.preventDefault();
			var box = $(e.currentTarget).closest('div.record');
			var button = $(e.currentTarget).find('i');
			var content = box.find('div.record-content');
			content.slideToggle('fast');
			button.toggleClass('fa-chevron-up').toggleClass('fa-chevron-down');
			content.parent().css('height','auto');
			if(! button.hasClass('fa-chevron-up')){
				this.$('.record').resizable('disable');
			}else
				this.$('.record').resizable('enable');
		},
		/*getData : function(params){
			var that = this;
			that.$("#logLines").append('<div class="loading-lines" align="center"><img src="images/loading.gif"/></div>');
			this.isLoading = true;
			$.ajax({
				url:"service/test/solr",
				data : params,
				success : function(data,textStatus,jqXHR ){
					that.logData = data;
					that.$(".loading-lines").remove();
					that.renderLogLines(data);
					that.renderRecordsInfo();
					that.isLoading = false;
				},
				error : function(jqXHR,textStatus,errorThrown){
					console.log("ERROR:"+errorThrown);
				}
			});
		},
		renderRecordsInfo : function(){
			if(this.logData){
				this.$("#recFetched").text(this.logData.startIndex + this.logData.pageSize);
				this.$("#recTotal").text(this.logData.totalCount);
			}
		},
		getQueryParams : function(){
			var startIndex = this.logData.startIndex,
					pageSize = this.logData.pageSize ? this.logData.pageSize : 10,obj={};
			if(_.isUndefined(startIndex)){
				obj.startIndex = 0;
			}else
				obj.startIndex =  startIndex + pageSize;
			obj.pageSize = pageSize;
			return obj;
		},*/
		renderLogLines : function(collection){
			var $el = this.$("#logLines"),state=this.collection.state,recordsFetched =0;
			if(collection.length > 0){
				_.each(collection.models,function(d){
					$el.append("<p class='log-line'>["+d.get("message")+"]</p>");
				});
			}else{
				$el.append("<span class='no-record'>no records found !</span>");
			}
				
			
			if(this.collection.hasNext())
				recordsFetched = (state.currentPage + 1) * state.pageSize;
			else
				recordsFetched = state.totalRecords;
			this.$("#recFetched").text(recordsFetched);
			this.$("#recTotal").text(state.totalRecords);
			this.highlightSearch();
			this.initializeResize();
		},
		highlightSearch : function(){
			var str = this.ui.searchTags.select2('val').toString(),that=this;
			if(! _.isEmpty(str)){
				var arr = str.split(",");
				_.each(arr,function(v){
					var searchStr = v.replace(/\*/g,'');
					that.$("#logLines").highlight(searchStr);
				});
			}
		},
		/** on close */
		onClose: function(){
		}

	});

	return LogDetailView;
});
