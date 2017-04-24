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


define(['require', 'backbone', 'hbs!tmpl/common/SelectClusterDropdown_tmpl'], function (require, Backbone, SelectClusterDropdownTmpl) {
	'use strict';

	var SelectClusterDropdown = Backbone.Marionette.ItemView.extend(
		{
			_viewName: "SelectClusterDropdown",

			template: SelectClusterDropdownTmpl,
			templateHelpers : function(){
				return {
					clusterNames: this.clusterNames
				};
			},
			/** ui selector cache */
			ui: {
				menuItem: ".dropdown-menu a",
				applyButton: ".apply-button"
			},
			className: "select-cluster-dropdown",

			events: function () {
				var events = {};
				events['click ' + this.ui.menuItem] = 'onClusterSelectChange';
				events['click ' + this.ui.applyButton] = 'applyClusterSelection';
				return events;
			},

			clusterNames: [],

			initialize: function(options) {
				_.extend(this, _.pick(options, 'clustersUrl', 'vent'));
        this.loadClusters();
      },

			loadClusters: function () {
        var self = this;
				$.ajax(this.clustersUrl).then(function (data) {
					self.clusterNames = data;
					self.render();
				});
			},

			onClusterSelectChange: function (e) {
				var self = this;
				var item = e.currentTarget;
				setTimeout(function () {
					self.selectCluster(item);
				}, 0);
				return false;
			},

			selectCluster: function (item) {
				var cluster = item.innerText.trim();
				var dropdown = $(item.parentElement.parentElement);
				var checkbox = $(item).find('input');
				var newValue = !checkbox.prop('checked');
				checkbox.prop('checked', newValue);
				if (cluster === 'All Clusters') {
					dropdown.find('input').prop('checked', newValue);
				} else {
					dropdown.find('input:first').prop('checked', !dropdown.find('input.cluster-checkbox:not(:checked)').length);
				}
			},

			applyClusterSelection: function (event) {
				var clusterNames = [];
				var clustersParam = '';
				$(event.target).parent().find('.cluster-checkbox:checked').each(function(i, element){
					clusterNames.push($(element).parent().text().trim());
				});

				clustersParam = clusterNames.length ? clusterNames.join(',') : 'NONE';

				this.vent.trigger("logtime:filter", {
					clusters: clustersParam
				});
			}

		});

	return SelectClusterDropdown;
});
