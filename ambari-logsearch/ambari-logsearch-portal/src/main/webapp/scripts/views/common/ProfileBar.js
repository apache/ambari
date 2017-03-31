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

 
define(['require',
	'backbone',
	'App',
	'communicator',
	'hbs!tmpl/common/ProfileBar_tmpl'
],function(require,Backbone,App,Communicator,ProfileBar_tmpl){
    'use strict';
	
	var ProfileBar = Backbone.Marionette.ItemView.extend(
	/** @lends ProfileBar */
	{
		_viewName : ProfileBar,
		
    	template: ProfileBar_tmpl,
    	templateHelpers : function(){
    		return {
    			userProfile : this.userProfile
    		};
    	},
        
    	/** ui selector cache */
    	ui: {
    		logout : 'a[data-id="logout"]'
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click ' + this.ui.logout]  = 'onLogout';
			return events;
		},
		onLogout : function(){
			var url = 'security-admin-web/logout.html';
			$.ajax({
				url : url,
				type : 'GET',
				headers : {
					"cache-control" : "no-cache"
				},
				success : function() {
					window.location.replace('login.html');
				},
				error : function(jqXHR, textStatus, err ) {
				}
				
			});
		},
    	/**
		* intialize a new ProfileBar ItemView 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a ProfileBar ItemView");

			_.extend(this, _.pick(options, ''));

			this.userProfile = SessionMgr.getUserProfile();
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			//this.listenTo(this.userProfile, "change", this.render, this);
			this.listenTo(Communicator.vent,'ProfileBar:rerender', this.render, this);
		},

		/** on render callback */
		onRender: function() {

			this.initializePlugins();
		},

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		/** on close */
		onClose: function(){
		}

	});

	return ProfileBar;
});
