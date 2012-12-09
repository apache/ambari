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
  totalHosts: [],
  clusterInfo: [],
  services: [],
  configs: [],
  globals: [],
  configMapping: require('data/config_mapping'),

  selectedServices: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false);
  }.property('content.services').cacheable(),

  clearStep: function () {
    this.get('services').clear();
    this.get('configs').clear();
    this.get('globals').clear();
    this.get('clusterInfo').clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step8: Review Page");
    this.clearStep();
    this.loadGlobals();
    this.loadConfigs();
    this.setCustomConfigs();
    this.loadClusterInfo();
    this.loadServices();
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    if (globals.someProperty('name', 'hive_database')) {
      //TODO: Hive host depends on the type of db selected. Change puppet variable name if postgress is not the default db
      var hiveDb = globals.findProperty('name', 'hive_database');
      if (hiveDb.value === 'New PostgreSQL Database') {
        globals.findProperty('name', 'hive_ambari_host').name = 'hive_mysql_host';
        globals = globals.without(globals.findProperty('name', 'hive_existing_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_database'));
      } else {
        globals.findProperty('name', 'hive_existing_host').name = 'hive_mysql_host';
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
      }
    }
    this.set('globals', globals);
  },

  loadConfigs: function () {
    var storedConfigs = this.get('content.serviceConfigProperties').filterProperty('id', 'site property').filterProperty('value');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('configs', storedConfigs.concat(uiConfigs));
  },

  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('configMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = this.getGlobConfigValue(_config.templateName, _config.value);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('configMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      this.setConfigValue(uiConfig, _config);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": _config.value,
        "filename": _config.filename
      });
    }, this);
    return uiConfig;
  },
  /**
   * Set all site property that are derived from other puppet-variable
   */

  getGlobConfigValue: function (templateName, expression) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return expression;
    }
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globals').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globValue = this.get('globals').findProperty('name', templateName[index]).value;
        value = value.replace(_express, globValue);
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from configMapping file has no corresponding variable in " +
         "content.serviceConfigProperties. Two possible reasons for the error could be: 1) The service is not selected. " +
         "and/OR 2) The service_config metadata file has no corresponding global var for the site property variable");
         */
        value = null;
      }
    }, this);
    return value;
  },
  /**
   * Set all site property that are derived from other site-properties
   */
  setConfigValue: function (uiConfig, config) {
    var fkValue = config.value.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.value = config.value.replace(_fkValue, globalValue);
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
          }
          config.value = config.value.replace(_fkValue, globalValue);
        }
      }, this);
    }
    if (fkValue = config.name.match(/<(foreignKey.*?)>/g)) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.name = config.name.replace(_fkValue, globalValue);
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
          }
          config.name = config.name.replace(_fkValue, globalValue);
        }
      }, this);
    }
    //For properties in the configMapping file having foreignKey and templateName properties.
    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        if (this.get('globals').someProperty('name', config.templateName[index])) {
          var globalValue = this.get('globals').findProperty('name', config.templateName[index]).value;
          config.value = config.value.replace(_value, globalValue);
        }
      }, this);
    }
  },

  /**
   * override site properties with the entered key-value pair in *-site.xml
   */
  setCustomConfigs: function () {
    var site = this.get('content.serviceConfigProperties').filterProperty('id', 'conf-site');
    site.forEach(function (_site) {
      var keyValue = _site.value.split(/\n+/);
      if (keyValue) {
        keyValue.forEach(function (_keyValue) {
          console.log("The value of the keyValue is: " + _keyValue.trim());
          _keyValue = _keyValue.trim();
          var key = _keyValue.match(/(.+)=/);
          var value = _keyValue.match(/=(.*)/);
          if (key) {
            this.setSiteProperty(key[1], value[1],_site.filename);
          }

        }, this);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function(key,value,filename) {
     if(this.get('configs').someProperty('name',key)) {
       this.get('configs').findProperty('name',key).value = value;
     } else {
       this.get('configs').pushObject({
         "id": "site property",
         "name": key,
         "value": value,
         "filename": filename
       });
     }
  },

  /**
   * Load all info about cluster to <code>clusterInfo</code> variable
   */
  loadClusterInfo: function () {

    // cluster name
    var cluster = this.rawContent.findProperty('config_name', 'cluster');
    cluster.config_value = this.get('content.cluster.name');
    console.log("STEP8: the value of content cluster name: " + this.get('content.cluster.name'));
    this.get('clusterInfo').pushObject(Ember.Object.create(cluster));

    //hosts
    var masterHosts = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();
    var slaveHosts = this.get('content.slaveComponentHosts');

    var hostObj = [];
    slaveHosts.forEach(function (_hosts) {
      hostObj = hostObj.concat(_hosts.hosts);
    }, this);

    slaveHosts = hostObj.mapProperty('hostName').uniq();

    var totalHosts = masterHosts.concat(slaveHosts).uniq();
    this.set('totalHosts', totalHosts);
    var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
    totalHostsObj.config_value = totalHosts.length;
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
    var nnHostName = this.get('content.masterComponentHosts').findProperty('display_name', nnComponent.display_name);
    nnComponent.set('component_value', nnHostName.hostName);
  },

  loadSnnValue: function (snnComponent) {
    var snnHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'SNameNode');
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
    var jtHostName = this.get('content.masterComponentHosts').findProperty('display_name', jtComponent.display_name);
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
    var hiveHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'Hive Metastore');
    metaStoreComponent.set('component_value', hiveHostName.hostName);
  },

  loadHiveDbValue: function (dbComponent) {
    var hiveDb = App.db.getServiceConfigProperties().findProperty('name', 'hive_database');

    if (hiveDb.value === 'New PostgreSQL Database') {

      dbComponent.set('component_value', 'PostgreSQL (New Database)');

    } else {

      var db = App.db.getServiceConfigProperties().findProperty('name', 'hive_existing_database');

      dbComponent.set('component_value', db.value + ' (' + hiveDb.value + ')');

    }
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
    var hbaseHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'HBase Master');
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
    rsComponent.set('component_value', totalRsHosts + ' hosts ' + '(' + totalGroups + ' ' + groupLabel + ')');
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
    var zkHostNames = this.get('content.masterComponentHosts').filterProperty('display_name', 'ZooKeeper').length;
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
        case 'Database':
          this.loadOozieDbValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(oozieObj);
  },

  loadOozieServerValue: function (oozieServer) {
    var oozieServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Oozie Server');
    oozieServer.set('component_value', oozieServerName.hostName);
  },

  loadOozieDbValue: function (dbComponent) {
    var oozieDb = App.db.getServiceConfigProperties().findProperty('name', 'oozie_database');
    if (oozieDb.value === 'New PostgreSQL Database') {
      dbComponent.set('component_value', 'PostgreSQL (New Database)');
    } else {
      var db = App.db.getServiceConfigProperties().findProperty('name', 'oozie_existing_database');
      dbComponent.set('component_value', db.value + ' (' + oozieDb.value + ')');
    }
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
    var nagiosServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Nagios Server');
    nagiosServer.set('component_value', nagiosServerName.hostName);
  },

  loadNagiosAdminValue: function (nagiosAdmin) {
    var config = this.get('content.serviceConfigProperties');
    var adminLoginName = config.findProperty('name', 'nagios_web_login');
    var adminEmail = config.findProperty('name', 'nagios_contact');
    nagiosAdmin.set('component_value', adminLoginName.value + ' / (' + adminEmail.value + ')');
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
    var gangliaServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Ganglia Collector');
    gangliaServer.set('component_value', gangliaServerName.hostName);
  },

  /**
   * Onclick handler for <code>next</code> button
   */
  submit: function () {
    debugger;

    if (App.testMode) {
      // App.router.send('next');
      //return;
    }

    this.createCluster();
    this.createSelectedServices();
    this.createConfigurations();
    this.applyCreatedConfToServices();
    this.createComponents();
    this.registerHostsToCluster();
    this.createHostComponents();

    App.router.send('next');
  },


  /* Following create* functions are called on submitting step8 */

  createCluster: function () {

    if (this.get('content.cluster.isCompleted')){
      return false;
    }

    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName;
    $.ajax({
      type: 'POST',
      url: url,
      async: false,
      //accepts: 'text',
      dataType: 'text',
      data: '{"Clusters": {"version" : "HDP-1.2.0"}}',
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
    console.log("Exiting createCluster");

  },

  createSelectedServices: function () {
    var services = this.get('selectedServices').mapProperty('serviceName');
    services.forEach(function (_service) {
      this.createService(_service, 'POST');
    }, this);
  },

  createService: function (service, httpMethod) {
    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName + '/services/' + service;
    $.ajax({
      type: httpMethod,
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
    //TODO: Uncomment following after hooking up with all services.
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
      type: 'POST',
      url: url,
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: STep8 -> In success function for createComponent");
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

  registerHostsToCluster: function() {
    var allHosts = this.get('content.hostsInfo');
    for(var hostName in allHosts){
      if(!allHosts[hostName].isInstalled){
        this.registerHostToCluster(hostName);
      }
    }
  },

  registerHostToCluster: function (hostName) {
    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName + '/hosts/' + hostName;
    $.ajax({
      type: 'POST',
      url: url,
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: STep8 -> In success function for registerHostToCluster");
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
    //TODO: Uncomment following after hooking up with all services.

    var masterHosts = this.get('content.masterComponentHosts');
    var slaveHosts = this.get('content.slaveComponentHosts');
    var clients = this.get('content.clients');
    var allHosts = this.get('content.hostsInfo');

    masterHosts.forEach(function (_masterHost) {
      this.createHostComponent(_masterHost);
    }, this);

    slaveHosts.forEach(function (_slaveHosts) {
      var slaveObj = {};
      if (_slaveHosts.componentName !== 'CLIENT') {
        slaveObj.component = _slaveHosts.componentName;
        _slaveHosts.hosts.forEach(function (_slaveHost) {
          slaveObj.hostName = _slaveHost.hostName;
          slaveObj.isInstalled = _slaveHost.isInstalled;
          this.createHostComponent(slaveObj);
        }, this);
      } else {
        this.get('content.clients').forEach(function (_client) {
          slaveObj.component = _client.component_name;
          _slaveHosts.hosts.forEach(function (_slaveHost) {
            slaveObj.hostName = _slaveHost.hostName;
            slaveObj.isInstalled = _slaveHost.isInstalled;
            this.createHostComponent(slaveObj);
          }, this);
        }, this);
      }
    }, this);

    // add Ganglia Monitor (Slave) to all hosts
    for (var hostName in allHosts) {
      // TODO: filter for only confirmed hosts?
      this.createHostComponent({ hostName: hostName, component: 'GANGLIA_MONITOR'});
    }

  },

  createHostComponent: function (hostComponent) {
    console.log(hostComponent);
    if (hostComponent.isInstalled) {
      return false;
    }

    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName + '/hosts/' + hostComponent.hostName + '/host_components/' + hostComponent.component;

    $.ajax({
      type: 'POST',
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
  },

  createConfigurations: function () {
    var selectedServices = this.get('selectedServices');
    if (!this.get('content.cluster.isCompleted')){
      this.createConfigSite(this.createGlobalSiteObj());
      this.createConfigSite(this.createCoreSiteObj());
      this.createConfigSite(this.createHdfsSiteObj('HDFS'));
    }
    if (selectedServices.someProperty('serviceName', 'MAPREDUCE')) {
      this.createConfigSite(this.createMrSiteObj('MAPREDUCE'));
    }
    if (selectedServices.someProperty('serviceName', 'HBASE')) {
      // TODO
      this.createConfigSite(this.createHbaseSiteObj('HBASE'));
    }
    if (selectedServices.someProperty('serviceName', 'OOZIE')) {
      this.createConfigSite(this.createOozieSiteObj('OOZIE'));
    }
    if (selectedServices.someProperty('serviceName', 'HIVE')) {
      // TODO
      // this.createConfigSite(this.createHiveSiteObj('HIVE'));
    }
  },

  createConfigSite: function (data) {
    console.log("Inside createConfigSite");
    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName + '/configurations';
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: STep8 -> In success function for the createConfigSite");
        console.log("TRACE: STep8 -> value of the url is: " + url);
        console.log("TRACE: STep8 -> value of the received data is: " + jsonData);
      },

      error: function (request, ajaxOptions, error) {
        console.log('Step8: In Error ');
        console.log('Step8: Error message is: ' + request.responseText);
        console.log("TRACE: STep8 -> value of the url is: " + url);
      },

      statusCode: require('data/statusCodes')
    });
    console.log("Exiting createConfigSite");
  },

  createGlobalSiteObj: function () {
    var globalSiteProperties = {};
    this.get('globals').forEach(function (_globalSiteObj) {
      // do not pass any globals whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
        // append "m" to JVM memory options
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name)) {
          _globalSiteObj.value += "m";
        }
        globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value;
        console.log("STEP8: name of the global property is: " + _globalSiteObj.name);
        console.log("STEP8: value of the global property is: " + _globalSiteObj.value);
      }
    }, this);
    return {"type": "global", "tag": "version1", "properties": globalSiteProperties};
  },

  createCoreSiteObj: function () {
    var coreSiteObj = this.get('configs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = this.get('selectedServices').someProperty('serviceName', 'OOZIE');
    coreSiteObj.forEach(function (_coreSiteObj) {
      if (isOozieSelected || _coreSiteObj.name != 'hadoop.proxyuser.oozie.hosts') {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      }
      console.log("STEP*: name of the property is: " + _coreSiteObj.name);
      console.log("STEP8: value of the property is: " + _coreSiteObj.value);
    }, this);
    return {"type": "core-site", "tag": "version1", "properties": coreSiteProperties};
  },

  createHdfsSiteObj: function (serviceName) {
    var hdfsSiteObj = this.get('configs').filterProperty('filename', 'hdfs-site.xml');
    var hdfsProperties = {};
    hdfsSiteObj.forEach(function (_configProperty) {
      hdfsProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": "hdfs-site", "tag": "version1", "properties": hdfsProperties };
  },

  createMrSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'mapred-site.xml');
    var mrProperties = {};
    configs.forEach(function (_configProperty) {
      mrProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'mapred-site', tag: 'version1', properties: mrProperties};
  },

  createHbaseSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'hbase-site.xml');
    var hbaseProperties = {};
    configs.forEach(function (_configProperty) {
      hbaseProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'hbase-site', tag: 'version1', properties: hbaseProperties};
  },

  createOozieSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'oozie-site.xml');
    var oozieProperties = {};
    configs.forEach(function (_configProperty) {
      oozieProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'oozie-site', tag: 'version1', properties: oozieProperties};
  },

  createHiveSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'hive-site.xml');
    var hiveProperties = {};
    configs.forEach(function (_configProperty) {
      hiveProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'hbase-site', tag: 'version1', properties: hiveProperties};
  },

  applyCreatedConfToServices: function () {
    var services = this.get('selectedServices').mapProperty('serviceName');
    services.forEach(function (_service) {
      var data = this.getConfigForService(_service);
      this.applyCreatedConfToService(_service, 'PUT', data);
    }, this);
  },

  applyCreatedConfToService: function (service, httpMethod, data) {
    console.log("Inside applyCreatedConfToService");
    var clusterName = this.get('clusterInfo').findProperty('config_name', 'cluster').config_value;
    var url = '/api/clusters/' + clusterName + '/services/' + service;

    $.ajax({
      type: httpMethod,
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(data),
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: STep8 -> In success function for the applyCreatedConfToService call");
        console.log("TRACE: STep8 -> value of the url is: " + url);
        console.log("TRACE: STep8 -> value of the received data is: " + jsonData);
      },

      error: function (request, ajaxOptions, error) {
        console.log('Step8: In Error ');
        console.log('Step8: Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });
    console.log("Exiting applyCreatedConfToService");
  },

  getConfigForService: function (serviceName) {
    switch (serviceName) {
      case 'HDFS':
        return {config: {'global': 'version1', 'core-site': 'version1', 'hdfs-site': 'version1'}};
      case 'MAPREDUCE':
        return {config: {'global': 'version1', 'core-site': 'version1', 'mapred-site': 'version1'}};
      case 'HBASE':
        return {config: {'global': 'version1', 'core-site': 'version1', 'hbase-site': 'version1'}};
      case 'OOZIE':
        return {config: {'global': 'version1', 'core-site': 'version1', 'oozie-site': 'version1'}};
    }
  }

})





  
  
