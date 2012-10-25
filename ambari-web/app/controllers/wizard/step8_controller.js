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

App.WizardStep8Controller = Em.Controller.extend({
	name: 'wizardStep8Controller',
	rawContent: require('data/review_configs'),

  clusterInfo : [],
	services: [],

  selectedServices : function(){
    return this.get('content.services').filterProperty('isSelected', true);
  }.property('content.services').cacheable(),

	clearStep: function () {
		this.get('services').clear();
    this.get('clusterInfo').clear();
	},

	loadStep: function () {
		console.log("TRACE: Loading step8: Review Page");
		this.clearStep();
    this.loadClusterInfo();
		this.loadServices();
	},

  /**
   * Load all info about cluster to <code>clusterInfo</code> variable
   */
  loadClusterInfo : function(){

    // cluster name
    var cluster = this.rawContent.findProperty('config_name', 'cluster');
    cluster.config_value = this.get('content.cluster.name');
    this.get('clusterInfo').pushObject(Ember.Object.create(cluster));

    //hosts
    var masterHosts = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();
    var slaveHosts = this.get('content.slaveComponentHosts');

    var hostObj = [];
    slaveHosts.forEach(function (_hosts) {
      hostObj = hostObj.concat(_hosts.hosts);
    }, this);

    slaveHosts = hostObj.mapProperty('hostname').uniq();

    var totalHosts = masterHosts.concat(slaveHosts).uniq().length;

    var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
    totalHostsObj.config_value = totalHosts;
    this.get('clusterInfo').pushObject(Ember.Object.create(totalHostsObj));

    //repo
    var repoOption = this.get('content.hosts.localRepo');
    var repoObj = this.rawContent.findProperty('config_name', 'Repo');
    if (repoOption) {
      repoObj.config_value = 'Yes';
    } else {
      repoObj.config_value = 'No';
    }
    this.get('clusterInfo').pushObject(Ember.Object.create(repoObj));

  },


  /**
   * Load all info about services to <code>services</code> variable
   */
	loadServices: function () {
    var selectedServices = this.get('selectedServices');
		this.set('services', selectedServices.mapProperty('serviceName'));

    selectedServices.forEach(function (_service) {
			console.log('INFO: step8: Name of the service from getService function: ' + _service.serviceName);
			var reviewService = this.rawContent.findProperty('config_name', 'services');
			var serviceObj = reviewService.config_value.findProperty('service_name', _service.serviceName);

			if (serviceObj) {
				switch (serviceObj.service_name) {
					case 'HDFS':
						this.loadHDFS(serviceObj);
						break;
					case 'MAPREDUCE':
						this.loadMapReduce(serviceObj);
						break;
					case 'HIVE':
						this.loadHive(serviceObj);
						break;
					case 'HBASE':
						this.loadHbase(serviceObj);
						break;
					case 'ZOOKEEPER':
						this.loadZk(serviceObj);
						break;
					case 'OOZIE':
						this.loadOozie(serviceObj);
						break;
					case 'NAGIOS':
						this.loadNagios(serviceObj);
						break;
					case 'GANGLIA':
						this.loadGanglia(serviceObj);
					case 'HCATALOG':
						break;
					default:
				}
			}
			//serviceObj.displayName = tempObj.service_name;
			//serviceObj.componentNames =  tempObj.service_components;

		}, this);
	},

  /**
   * load all info about HDFS service
   * @param hdfsObj
   */
	loadHDFS: function (hdfsObj) {
		hdfsObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'NameNode':
					this.loadNnValue(_component);
					break;
				case 'SecondaryNameNode':
					this.loadSnnValue(_component);
					break;
				case 'DataNodes':
					this.loadDnValue(_component);
					break;
				default:
			}
		}, this);
		//var
		this.get('services').pushObject(hdfsObj);
	},

	loadNnValue: function (nnComponent) {
		var nnHostName = this.get('content.masterComponentHosts').findProperty('component', nnComponent.display_name);
		nnComponent.set('component_value', nnHostName.hostName);
	},

	loadSnnValue: function (snnComponent) {
		var snnHostName = this.get('content.masterComponentHosts').findProperty('component', 'SNameNode');
		snnComponent.set('component_value', snnHostName.hostName);
	},

	loadDnValue: function (dnComponent) {
		var dnHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'DataNode');
		var totalDnHosts = dnHosts.hosts.length;
		var dnHostGroups = [];
		dnHosts.hosts.forEach(function (_dnHost) {
			dnHostGroups.push(_dnHost.group);

		}, this);
		var totalGroups = dnHostGroups.uniq().length;
		var groupLabel;
		if (totalGroups == 1) {
			groupLabel = 'group';
		} else {
			groupLabel = 'groups';
		}
		dnComponent.set('component_value', totalDnHosts + ' hosts ' + '(' + totalGroups + ' ' + groupLabel + ')');
	},

  /**
   * Load all info about mapReduce service
   * @param mrObj
   */
	loadMapReduce: function (mrObj) {
		mrObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'JobTracker':
					this.loadJtValue(_component);
					break;
				case 'TaskTrackers':
					this.loadTtValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(mrObj);
	},

	loadJtValue: function (jtComponent) {
		var jtHostName = this.get('content.masterComponentHosts').findProperty('component', jtComponent.display_name);
		jtComponent.set('component_value', jtHostName.hostName);
	},

	loadTtValue: function (ttComponent) {
		var ttHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'TaskTracker');
		var totalTtHosts = ttHosts.hosts.length;
		var ttHostGroups = [];
		ttHosts.hosts.forEach(function (_ttHost) {
			ttHostGroups.push(_ttHost.group);
		}, this);
		var totalGroups = ttHostGroups.uniq().length;
		var groupLabel;
		if (totalGroups == 1) {
			groupLabel = 'group';
		} else {
			groupLabel = 'groups';
		}
		ttComponent.set('component_value', totalTtHosts + ' hosts ' + '(' + totalGroups + ' ' + groupLabel + ')');
	},

  /**
   * Load all info about Hive service
   * @param hiveObj
   */
	loadHive: function (hiveObj) {
		hiveObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'Hive Metastore Server':
					this.loadHiveMetaStoreValue(_component);
					break;
				case 'Database':
					this.loadHiveDbValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(hiveObj);

	},

	loadHiveMetaStoreValue: function (metaStoreComponent) {
		var hiveHostName = this.get('content.masterComponentHosts').findProperty('component', 'Hive Metastore');
		metaStoreComponent.set('component_value', hiveHostName.hostName);
	},

	loadHiveDbValue: function (dbComponent) {
		dbComponent.set('component_value', 'MySQL');
	},

  /**
   * Load all info about Hbase
   * @param hbaseObj
   */
	loadHbase: function (hbaseObj) {
		hbaseObj.service_components.forEach(function (_component) {
			switch (_component.display_name) {
				case 'Master':
					this.loadMasterValue(_component);
					break;
				case 'Region Servers':
					this.loadRegionServerValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(hbaseObj);
	},

	loadMasterValue: function (hbaseMaster) {
		var hbaseHostName = this.get('content.masterComponentHosts').findProperty('component', 'HBase Master');
		hbaseMaster.set('component_value', hbaseHostName.hostName);
	},

	loadRegionServerValue: function (rsComponent) {
		var rsHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'RegionServer');
		var totalRsHosts = rsHosts.hosts.length;
		var rsHostGroups = [];
		rsHosts.hosts.forEach(function (_ttHost) {
			rsHostGroups.push(_ttHost.group);
		}, this);
		var totalGroups = rsHostGroups.uniq().length;
		var groupLabel;
		if (totalGroups == 1) {
			groupLabel = 'group';
		} else {
			groupLabel = 'groups';
		}
		rsComponent.set('component_value', totalRsHosts + ' hosts '  + '(' + totalGroups + ' ' + groupLabel + ')');
	},

  /**
   * Load all info about ZooKeeper service
   * @param zkObj
   */
	loadZk: function (zkObj) {
		zkObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'Servers':
					this.loadZkServerValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(zkObj);
	},

	loadZkServerValue: function (serverComponent) {
		var zkHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'ZooKeeper').length;
		var hostSuffix;
		if (zkHostNames === 1) {
			hostSuffix = 'host';
		} else {
			hostSuffix = 'hosts';
		}
		serverComponent.set('component_value', zkHostNames + ' ' + hostSuffix);
	},

  /**
   * Load all info about Oozie services
   * @param oozieObj
   */
	loadOozie: function (oozieObj) {
		oozieObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'Server':
					this.loadOozieServerValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(oozieObj);
	},

	loadOozieServerValue: function (oozieServer) {
		var oozieServerName = this.get('content.masterComponentHosts').findProperty('component', 'Oozie Server');
		oozieServer.set('component_value', oozieServerName.hostName);
	},

  /**
   * Load all info about Nagios service
   * @param nagiosObj
   */
	loadNagios: function (nagiosObj) {
		nagiosObj.service_components.forEach(function (_component) {
			switch (_component.display_name) {
				case 'Server':
					this.loadNagiosServerValue(_component);
					break;
				case 'Administrator':
					this.loadNagiosAdminValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(nagiosObj);
	},

	loadNagiosServerValue: function (nagiosServer) {
		var nagiosServerName = this.get('content.masterComponentHosts').findProperty('component', 'Nagios Server');
		nagiosServer.set('component_value', nagiosServerName.hostName);
	},

	loadNagiosAdminValue: function (nagiosAdmin) {
    var config = this.get('content.serviceConfigProperties');
		var adminLoginName = config.findProperty('name', 'nagios_web_login');
		var adminEmail = config.findProperty('name', 'nagios_contact');
		nagiosAdmin.set('component_value', adminLoginName.value + ' / (' + adminEmail.value +')');
	},

  /**
   * Load all info about ganglia
   * @param gangliaObj
   */
	loadGanglia: function (gangliaObj) {
		gangliaObj.get('service_components').forEach(function (_component) {
			switch (_component.get('display_name')) {
				case 'Server':
					this.loadGangliaServerValue(_component);
					break;
				default:
			}
		}, this);
		this.get('services').pushObject(gangliaObj);
	},

	loadGangliaServerValue: function (gangliaServer) {
		var gangliaServerName = this.get('content.masterComponentHosts').findProperty('component', 'Ganglia Collector');
		gangliaServer.set('component_value', gangliaServerName.hostName);
	},


  /**
   * Onclick handler for <code>next</code> button
   */
	submit: function () {
		this.createCluster();
		this.createSelectedServices();
		this.createComponents();
		this.createHostComponents();
		App.router.send('next');
	},


	/* Following create* functions are called on submitting step8 */

	createCluster: function () {
		var self = this;
		var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
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
    var services = this.get('selectedServices').mapProperty('serviceName');
		services.forEach(function (_service) {
			this.createService(_service);
		}, this);
	},

	createService: function (service) {
		var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
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
		var services = this.get('selectedServices').mapProperty('serviceName');
		services.forEach(function (_service) {
			var components = serviceComponents.filterProperty('service_name', _service);
			components.forEach(function (_component) {
				console.log("value of component is: " + _component.component_name);
				this.createComponent(_service, _component.component_name);
			}, this);
		}, this);

	},

	createComponent: function (service, component) {
		var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
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
    var masterHosts = this.get('content.masterComponentHosts');
    var slaveHosts = this.get('content.slaveComponentHosts');

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
		var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
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





  
  
