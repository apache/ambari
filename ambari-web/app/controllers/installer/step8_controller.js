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

var App = require('app');

App.InstallerStep8Controller = Em.ArrayController.extend({
	name: 'installerStep8Controller',
	rawContent: require('data/review_configs'),
	content: [],
	services: [],

	clearStep: function () {
		this.clear();
		this.services.clear();
	},

	loadStep: function () {
		console.log("TRACE: Loading step8: Review Page");
		this.clearStep();
		var configObj = new Ember.Set();
		this.loadClusterName();
		this.loadHosts();
		this.loadServices();
		// this.doConfigsUneditable();
	},

	loadClusterName: function () {
		var obj = {};
		var cluster = this.rawContent.findProperty('config_name', 'cluster');
		cluster.config_value = App.db.getClusterName();
		this.pushObject(Ember.Object.create(cluster));
	},

	loadHosts: function () {
		var masterHosts = App.db.getMasterComponentHosts().mapProperty('hostName').uniq();
		var slaveHosts = App.db.getSlaveComponentHosts();
		var hostObj = [];
		slaveHosts.forEach(function (_hosts) {
			hostObj = hostObj.concat(_hosts.hosts);
		}, this);
		slaveHosts = hostObj.mapProperty('hostname').uniq();
		console.log('The value of slaveHosts is: ' + slaveHosts);
		var totalHosts = masterHosts.concat(slaveHosts).uniq().length;
		var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
		totalHostsObj.config_value = totalHosts;
		this.pushObject(Ember.Object.create(totalHostsObj));
	},

	loadServices: function () {
		this.set('services', App.db.getSelectedServiceNames());
	},

	submit: function () {
		this.createCluster();
		this.createSelectedServices();
		this.createComponents();
		this.createHostComponents();
		App.router.send('next');

	},

	createCluster: function () {
		var self = this;
		var clusterName = this.findProperty('config_name', 'cluster').config_value;
		var url = '/api/clusters/' + clusterName;
		$.ajax({
			type: 'PUT',
			url: url,
			async: false,
			//accepts: 'text',
			dataType: 'text',
			timeout: 5000,
			success: function (data) {
				var jsonData = jQuery.parseJSON(data);
				console.log("TRACE: STep8 -> In success function for createCluster call");
				console.log("TRACE: STep8 -> value of the received data is: " + jsonData);

			},

			error: function (request, ajaxOptions, error) {
				console.log('Step8: In Error ');
				console.log('Step8: Error message is: ' + request.responseText);
			},

			statusCode: require('data/statusCodes')
		});

	},

	createSelectedServices: function () {
		var services = App.db.getSelectedServiceNames();
		services.forEach(function (_service) {
			this.createService(_service);
		}, this);
	},

	createService: function (service) {
		var self = this;
		var clusterName = this.findProperty('config_name', 'cluster').config_value;
		var url = '/api/clusters/' + clusterName + '/services/' + service;
		$.ajax({
			type: 'PUT',
			url: url,
			async: false,
			dataType: 'text',
			timeout: 5000,
			success: function (data) {
				var jsonData = jQuery.parseJSON(data);
				console.log("TRACE: STep8 -> In success function for the createService call");
				console.log("TRACE: STep8 -> value of the url is: " + url);
				console.log("TRACE: STep8 -> value of the received data is: " + jsonData);

			},

			error: function (request, ajaxOptions, error) {
				console.log('Step8: In Error ');
				console.log('Step8: Error message is: ' + request.responseText);
			},

			statusCode: require('data/statusCodes')
		});
	},

	createComponents: function () {
		var serviceComponents = require('data/service_components');
		var services = App.db.getSelectedServiceNames();
		services.forEach(function (_service) {
			var components = serviceComponents.filterProperty('service_name', _service);
			components.forEach(function (_component) {
				console.log("value of component is: " + _component.component_name);
				this.createComponent(_service, _component.component_name);
			}, this);
		}, this);

	},

	createComponent: function (service, component) {
		var self = this;
		var clusterName = this.findProperty('config_name', 'cluster').config_value;
		var url = '/api/clusters/' + clusterName + '/services/' + service + '/components/' + component;
		$.ajax({
			type: 'PUT',
			url: url,
			async: false,
			dataType: 'text',
			timeout: 5000,
			success: function (data) {
				var jsonData = jQuery.parseJSON(data);
				console.log("TRACE: STep8 -> value of the url is: " + url);
				console.log("TRACE: STep8 -> value of the received data is: " + jsonData);

			},

			error: function (request, ajaxOptions, error) {
				console.log('Step8: In Error ');
				console.log('Step8: Error message is: ' + request.responseText);
			},

			statusCode: require('data/statusCodes')
		});
	},

	createHostComponents: function () {
		var masterHosts = App.db.getMasterComponentHosts();
		var slaveHosts = App.db.getSlaveComponentHosts();
		masterHosts.forEach(function (_masterHost) {
			this.createHostComponent(_masterHost);
		}, this);
		slaveHosts.forEach(function (_slaveHosts) {
			var slaveObj = {};
			slaveObj.component = _slaveHosts.componentName;
			_slaveHosts.hosts.forEach(function (_slaveHost) {
				slaveObj.hostName = _slaveHost.hostname;
				this.createHostComponent(slaveObj);
			}, this);
		}, this);
	},

	createHostComponent: function (hostComponent) {
		var self = this;
		var clusterName = this.findProperty('config_name', 'cluster').config_value;
		var url = '/api/clusters/' + clusterName + '/hosts/' + hostComponent.hostName + '/host_components/' + hostComponent.component;

		$.ajax({
			type: 'PUT',
			url: url,
			async: false,
			dataType: 'text',
			timeout: 5000,
			success: function (data) {
				var jsonData = jQuery.parseJSON(data);
				console.log("TRACE: STep8 -> In success function for the createComponent with new host call");
				console.log("TRACE: STep8 -> value of the url is: " + url);
				console.log("TRACE: STep8 -> value of the received data is: " + jsonData);

			},

			error: function (request, ajaxOptions, error) {
				console.log('Step8: In Error ');
				console.log('Step8: Error message is: ' + request.responseText);
			},

			statusCode: require('data/statusCodes')
		});
	}
})


/*
 doConfigsUneditable: function () {
 this.content.forEach(function (_service) {
 _service.get('configs').forEach(function (_serviceConfig) {
 console.log('value of isEditable before for: ' + _serviceConfig.name);
 console.log('value of isEditable before: ' + _serviceConfig.isEditable);
 console.log('value of displayType before: ' + _serviceConfig.displayType);
 _serviceConfig.set('isEditable', false);
 _serviceConfig.set('displayType', 'string');
 console.log('value of isEditable after for: ' + _serviceConfig.name);
 console.log('value of isEditable after: ' + _serviceConfig.isEditable);
 console.log('value of displayType after: ' + _serviceConfig.displayType);
 }, this);
 }, this);
 }
 + 
 */
  
  
