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

define([
	'jquery',
	'underscore',
	'backbone',
	'utils/Utils',
	'App'
], function($, _, Backbone, Utils, App) {
	var AppRouter = Backbone.Router.extend({
		routes: {
			// Define some URL routes
			"": 'topologySummaryAction',
			"!/topology": 'topologySummaryAction',
			"!/topology/:id": 'topologyDetailAction',

			// Default
			'*actions': 'defaultAction'
		},

		initialize: function() {
			this.showRegions();
			this.bindRegions();
			this.listenTo(this, "route", this.postRouteExecute, this);
		},

		showRegions: function () {
			require(['views/site/Header'], function (HeaderView) {
				App.rHeader.show(new HeaderView());
			});
		},

		bindRegions: function () {
			var that = this;
			require(['modules/Vent'], function(vent){
				vent.on('Region:showTopologySection', function(){
					App.rCluster.$el.removeClass('active').hide();
					App.rTopology.$el.addClass('active').show();
					if(App.rTopology.$el.children().hasClass('topologyDetailView')){
						that.topologyDetailAction(App.rTopology.currentView.model.get('id'));
					} else {
						that.topologySummaryAction();
					}
				});

				vent.on('Region:showClusterSection', function(){
					require(['views/Cluster/ClusterSummary'], function(ClusterSummaryView){
						App.rCluster.show(new ClusterSummaryView());
					});
					App.rTopology.$el.removeClass('active').hide();
					App.rCluster.$el.addClass('active').show();
					vent.trigger('Breadcrumb:Hide');
				});

			});
		},

		/**
		 * Define route handlers here
		 */
		topologySummaryAction: function() {
			require(['views/Topology/TopologySummary'],function(TopologySummaryView){
        App.rTopology.show(new TopologySummaryView());
      });
		},

		topologyDetailAction: function(id) {
			require(['views/Topology/TopologyDetail'], function(TopologyDetailView){
				App.rTopology.show(new TopologyDetailView({
					id: id
				}));
			});
		},

		defaultAction: function(actions) {
			// We have no matching route, lets just log what the URL was
			console.log('No route:', actions);
		}
	});

	return AppRouter;

});