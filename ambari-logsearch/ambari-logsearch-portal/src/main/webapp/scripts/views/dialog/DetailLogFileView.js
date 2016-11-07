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
    'utils/Utils',
    'utils/ViewUtils',
    'hbs!tmpl/dialog/DetailLogFileView_tmpl',
    'collections/VLogList'
],function(require,Backbone,Utils,ViewUtils,DetailLogFileView_tmpl, VLogList) {
    'use strict';

    return Backbone.Marionette.ItemView.extend(
        /** @lends DetailLogFileView */
        {
            _viewName: 'DetailLogFileView',

            template: DetailLogFileView_tmpl,


            /** ui selector cache */
            ui: {
                logsContainer: ".logsContainer",
                prevBtn : "[data-id='prev']",
                nextBtn : "[data-id='next']"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click .logsDetail button"] = 'onButtonClick';
                return events;
            },

            /**
             * intialize a new TimeZoneChangeView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'model'));
                this.collection = new VLogList([],{
                	state: {
                        firstPage: 0,
                        pageSize: 9999
                    }
                });
                this.defaultRecords = 10;
                this.params = {};
            },
            bindEvents: function() {
                var that = this;
            },
            onRender: function() {
            	var that = this;
            	this.fetchLogs({
            		host_name : this.model.get("host"),
            		component_name : this.model.get("type"),
            		numberRows : this.defaultRecords,
            		id : this.model.get("id")
            	},{
            		beforeSend : function(){
            			that.ui.logsContainer.append("<b>Loading...</b>");
            		},
            		success : function(data){
            			that.collection.reset(data.logList);
            			that.renderLogs();
            		}
            	});
            },
            fetchLogs : function(params,options){
            	$.extend(this.params,params);
            	this.collection.getTruncatedLogs(this.params,_.extend({
            		error :function(error,data,status){
            			var obj = JSON.parse(error.responseText);
            			Utils.notifyError({content:obj.msgDesc});
            			that.ui.logsContainer.html("");
            		}
            	},options));
            },
            renderLogs: function() {
            	var that = this;
            	that.ui.logsContainer.html("<hr>")
            	this.collection.each(function(model){
            		var highlightClass = "highlightLog";
            		that.ui.logsContainer.append("<div data-id='"+model.get("id")+"' class='"+(that.model.get("id") === model.get("id") ? highlightClass : "")+"'><pre>"+ViewUtils.foramtLogMessageAsLogFile(model, "mess")+"</pre></div>");
            	});
            	that.ui.logsContainer.append("<hr>")
            	var top = this.$("[data-id="+this.model.get("id")+"]").position().top;
            	this.scrollToLogEntry(top -200,300);
            	this.$("button[data-id]").removeClass("hidden");
            },
            appendLogs : function(data,type){
            	var isNext = (type === "next") ? true : false,that=this;
            	if(data.length == 0 && (!isNext)){
            		that.ui.logsContainer.prepend("<span>no records found!</span>");
            		that.ui.prevBtn.hide();
            	}
            	_.each(data,function(log,i){
            		var html = "<div data-id='"+log.id+"'><pre>"+ViewUtils.foramtLogMessageAsLogFile(new Backbone.Model(log),"mess")+"</pre></div>";
            		if(isNext)
                		that.ui.logsContainer.append(html);
            		else
            			that.ui.logsContainer.prepend(html);
            	});
            	if(isNext)
            		that.ui.logsContainer.append("<hr>");
            	else
            		that.ui.logsContainer.prepend("<hr>");
            },
            onButtonClick : function(e){
            	var $el = $(e.currentTarget),that=this,type = $el.data("id"),isNext = (type === "next") ? true : false;
            	$el.addClass("disabled");
            	$el.find("span").text("loading more "+this.defaultRecords);
            	this.toggleLoadMoreBtn($el);
            	var $row = isNext ? this.ui.logsContainer.find("[data-id]").last() : this.ui.logsContainer.find("[data-id]").first();
            	if($row){
            		this.fetchLogs({
            			id : $row.data("id"),
            			scrollType : isNext ? "after" : "before"
            		},{
            			success : function(data){
            				that.appendLogs(data.logList,type);
            			},
            			complete : function(){
            				$el.removeClass("disabled");
            				$el.find("span").text("Load more");
            				that.toggleLoadMoreBtn($el);
            				if(! isNext){
            					var top = that.$("[data-id="+$row.data("id")+"]").position().top;
            					that.scrollToLogEntry(top - 50,0);
            				}
            			}
            		});
            	}
            },
            toggleLoadMoreBtn : function($btn){
            	var $i = $btn.find('i'),className = ($btn.data("id") === "next") ? "fa-arrow-down" : "fa-arrow-up";
            	if($i.hasClass(className)){
            		$i.removeClass(className).addClass("fa-spinner fa-spin");
            	}else
            		$i.removeClass("fa-spinner fa-spin").addClass(className);
            },
            scrollToLogEntry : function(top,speed){
            	this.$(".logsDetail").animate({ scrollTop : (top) }, speed);
            },
            onClose : function(){
            }

        });


});
