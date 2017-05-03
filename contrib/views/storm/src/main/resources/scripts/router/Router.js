/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define([
	'require',
	'backbone',
	'react',
	'react-dom',
	'utils/Utils'
], function(require, Backbone, React, ReactDOM, Utils) {
	'use strict';
	var rRender;
	var AppRouter = Backbone.Router.extend({
		routes: {
			'' 													: 'dashboardAction',
			'!/dashboard' 										: 'dashboardAction',
			'!/topology' 										: 'topologyAction',
			'!/topology/:id'									: 'topologyDetailsAction',
			'!/topology/:id/component/:name'					: 'componentDetailsAction',
			'!/nimbus' 											: 'nimbusAction',
			'!/supervisor' 										: 'supervisorAction',
			'*actions'											: 'defaultAction'
		},
		initialize: function() {
                        App.baseURL = location.pathname+'proxy?url=';
			this.showRegions();
			this.listenTo(this, "route", this.postRouteExecute, this);
		},
		showRegions: function() {
			this.renderFooter();
			if(window != window.parent){
				var viewPath = this.getParameterByName("viewpath");
				location.hash = viewPath ? viewPath : '';
			}
		},
		renderFooter: function(){
			require(['jsx!views/Footer'], function(Footer){
				ReactDOM.render(React.createElement(Footer), App.Footer);
			});
		},
		execute: function(callback, args) {
			this.preRouteExecute();
			if (callback) callback.apply(this, args);
			this.postRouteExecute();
		},
		preRouteExecute: function() {},
		postRouteExecute: function(name, args) {
			this.shareUrl();
		},

		getParameterByName: function(name) {
			name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
			var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
				results = regex.exec(location.search);
			return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
		},

		shareUrl : function(){
			if(window != window.parent){
				var parentWindow = window.parent;
				var parentHash = parentWindow.location.hash.split("?")[0];
				var newurl = parentWindow.location.protocol + "//" + parentWindow.location.host + parentHash + '?viewpath='+encodeURIComponent(location.hash);
				parentWindow.history.replaceState({path:newurl},'',newurl);
			}
		},

		/**
		 * Define route handlers here
		 */

		dashboardAction: function(){
			require(['jsx!views/Dashboard'], function(DashboardView){
				ReactDOM.render(React.createElement(DashboardView), App.Container);
			});
		},
		topologyAction: function(){
			require(['jsx!views/TopologyListingView'], function(TopologyListingView){
				ReactDOM.render(React.createElement(TopologyListingView), App.Container);
			});
		},
		topologyDetailsAction: function(id){
			require(['jsx!views/TopologyDetailView'], function(TopologyDetailView){
				ReactDOM.render(React.createElement(TopologyDetailView, _.extend({}, this.props, {id: id})), App.Container);
			}.bind(this));
		},
		componentDetailsAction: function(id, name){
			require(['jsx!views/ComponentDetailView'], function(ComponentDetailView){
				ReactDOM.render(React.createElement(ComponentDetailView, _.extend({}, this.props, {id: id, name: name})), App.Container);
			}.bind(this));
		},
		nimbusAction: function(){
			require(['jsx!views/NimbusSummaryView'], function(NimbusSummaryView){
				ReactDOM.render(React.createElement(NimbusSummaryView), App.Container);
			});
		},
		supervisorAction: function(){
			require(['jsx!views/SupervisorSummaryView'], function(SupervisorSummaryView){
				ReactDOM.render(React.createElement(SupervisorSummaryView), App.Container);
			});
		},
		defaultAction: function(actions) {
			throw new Error("No such route found in the application: "+actions);
		},
	});

	return AppRouter;

});
