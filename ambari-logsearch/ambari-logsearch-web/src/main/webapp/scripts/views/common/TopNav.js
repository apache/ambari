/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
define(['require','backbone','hbs!tmpl/common/TopNav_tmpl','jquery.cookie'],function(require,Backbone,TopNav_tmpl){
    'use strict';

	var TopNav = Backbone.Marionette.ItemView.extend(
	/** @lends TopNav */
	{
		_viewName : TopNav,
		
    	template: TopNav_tmpl,
    	templateHelpers : function(){
    		
    	},
        
    	/** ui selector cache */
    	ui: {},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click li']  = 'onNavMenuClick';
			return events;
		},

    	/**
		* intialize a new TopNav ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a TopNav ItemView");
			_.extend(this, _.pick(options, ''));
			this.bindEvents();
			this.appState = options.appState;
        	this.appState.on('change:currentTab', this.highlightNav,this);
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			var that = this;
			this.initializePlugins();
			$('.page-logo').on('click',function(){
				that.$('ul li').removeClass('active');
				that.$('ul li:first').addClass('active');
			});
			$.cookie('clientTimeOffset', new Date().getTimezoneOffset());
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		onNavMenuClick : function(e){
			var ele = $(e.currentTarget);
			this.$('ul li').removeClass('active');
			ele.addClass('active');
		},
		highlightNav : function(e){
			this.$('ul li').removeClass('active');
        	this.$('#nav' + this.appState.get('currentTab')).parent().addClass('active');
        },
		/** on close */
		onClose: function(){
		}

	});

	return TopNav;
});
