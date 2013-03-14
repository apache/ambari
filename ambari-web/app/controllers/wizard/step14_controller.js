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

App.WizardStep14Controller = Em.Controller.extend({

  status: function () {
    if (this.get('tasks').someProperty('status', 'FAILED')) {
      return 'FAILED';
    }
    if (this.get('tasks').everyProperty('status', 'COMPLETED')) {
      return 'COMPLETED';
    }
    return 'IN_PROGRESS';
  }.property('tasks.@each.status'),

  tasks: [],

  /**
   * Set messages for tasks depending on their status
   */
  setTasksMessages: function () {
    var service = this.get('service.displayName');
    var master = this.get('masterComponent.display_name');
    for (i = 0; i < this.get('tasks').length; i++) {
      var status = this.get('tasks')[i].status.toLowerCase().replace('initialize', 'pending').replace('_', ' ');
      if (i == 0 || i == 6) {
        this.get('tasks')[i].set('message', Em.I18n.t('installer.step14.task' + i).format(service) + ' ' + status);
      } else {
        this.get('tasks')[i].set('message', Em.I18n.t('installer.step14.task' + i).format(master) + ' ' + status);
      }
    }
  }.observes('tasks.@each.status'),

  configs: [],
  globals: [],
  configMapping: require('data/config_mapping'),
  newConfigsTag: null,
  createdConfigs: [],

  currentRequestId: null,

  isSubmitDisabled: true,

  service: function () {
    return App.Service.find().findProperty('serviceName', this.get('masterComponent.service_id'));
  }.property('masterComponent'),

  masterComponent: function () {
    return this.get('content.reassign');
  }.property('content.reassign'),

  loadStep: function () {
    this.clearStep();
    this.loadTasks();
    this.navigateStep();
  },

  clearStep: function () {
    var tasks = [];
    for (var i = 0; i < 8; i++) {
      tasks.pushObject(Ember.Object.create({
        status: 'INITIALIZE',
        logs: '',
        message: '',
        progress: 0
      }));
    }
    this.set('tasks', tasks);
    this.set('isSubmitDisabled', true);
    this.get('configs').clear();
    this.get('globals').clear();
    this.get('createdConfigs').clear();
  },

  loadTasks: function () {

  },

  /**
   * Run tasks in proper way
   */
  navigateStep: function () {
    if (this.get('tasks')[0].status == 'INITIALIZE') {
      this.stopService();
    }
    else if (this.get('tasks')[1].status == 'INITIALIZE') {
      this.createMasterComponent();
    }
    else if (this.taskIsReady(2)) {
      this.createConfigs();
    }
    else if (this.taskIsReady(3)) {
      this.applyConfigs();
    }
    else if (this.taskIsReady(4)) {
      this.putInMaintenanceMode();
    }
    else if (this.taskIsReady(5)) {
      this.installComponent();
    }
    else if (this.taskIsReady(6)) {
      this.startComponents();
    }
    else if (this.taskIsReady(7)) {
      this.removeComponent();
    }
  }.observes('tasks.@each.status'),

  /**
   * Determine preparedness to run task
   * @param task
   * @return {Boolean}
   */
  taskIsReady: function (task) {
    var startIndex = (task == 5) ? 0 : 1;
    var tempArr = this.get('tasks').mapProperty('status').slice(startIndex, task).uniq();
    return this.get('tasks')[task].status == 'INITIALIZE' && tempArr.length == 1 && tempArr[0] == 'COMPLETED';
  },

  /**
   * Change status of the task
   * @param task
   * @param status
   */
  setTasksStatus: function (task, status) {
    this.get('tasks')[task].set('status', status);
  },

  stopService: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var serviceName = this.get('masterComponent.service_id');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + serviceName;
    var data = '{"ServiceInfo": {"state": "INSTALLED"}}';
    var method = 'PUT';
    $.ajax({
      type: method,
      url: url,
      data: data,
      dataType: 'text',
      timeout: App.timeout,

      beforeSend: function () {
        self.setTasksStatus(0, 'PENDING');
      },

      success: function (data) {
        if (jQuery.parseJSON(data)) {
          self.set('currentRequestId', jQuery.parseJSON(data).Requests.id);
          self.getLogsByRequest();
        } else {
          self.setTasksStatus(0, 'FAILED');
        }
      },

      error: function () {
        self.setTasksStatus(0, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });
  },

  createMasterComponent: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    var componentName = this.get('masterComponent.component_name');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/hosts?Hosts/host_name=' + hostName;
    var data = {
      "host_components": [
        {
          "HostRoles": {
            "component_name": componentName
          }
        }
      ]
    };
    var method = 'POST';
    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(data),
      dataType: 'text',
      timeout: App.timeout,

      beforeSend: function () {
        self.setTasksStatus(1, 'PENDING');
      },

      success: function () {
        self.setTasksStatus(1, 'COMPLETED');
      },

      error: function () {
        self.setTasksStatus(1, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });

  },

  createConfigs: function () {
    this.loadGlobals();
    this.loadConfigs();
    this.set('newConfigsTag', 'version' + (new Date).getTime());
    var serviceName = this.get('service.serviceName');
    this.createConfigSite(this.createGlobalSiteObj());
    this.createConfigSite(this.createCoreSiteObj());
    if (serviceName == 'HDFS') {
      this.createConfigSite(this.createHdfsSiteObj());
    }
    if (serviceName == 'MAPREDUCE') {
      this.createConfigSite(this.createMrSiteObj());
    }
    if (serviceName == 'HBASE') {
      this.createConfigSite(this.createHbaseSiteObj());
    }
    if (serviceName == 'OOZIE') {
      this.createConfigSite(this.createOozieSiteObj());
    }
    if (serviceName == 'HIVE') {
      this.createConfigSite(this.createHiveSiteObj());
    }
    if (serviceName == 'WEBHCAT') {
      this.createConfigSite(this.createWebHCatSiteObj());
    }
    if (this.get('tasks')[2].status !== 'FAILED') {
      this.setTasksStatus(2, 'COMPLETED');
    }
  },

  createConfigSite: function (data) {
    var self = this;
    data.tag = this.get('newConfigsTag');
    var clusterName = this.get('content.cluster.name');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/configurations';
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      dataType: 'text',
      timeout: 5000,

      beforeSend: function () {
        self.setTasksStatus(2, 'PENDING');
      },

      success: function () {
        self.get('createdConfigs').push(data.type);
      },

      error: function () {
        self.setTasksStatus(2, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    if (globals.someProperty('name', 'hive_database')) {
      //TODO: Hive host depends on the type of db selected. Change puppet variable name if postgres is not the default db
      var hiveDb = globals.findProperty('name', 'hive_database');
      if (hiveDb.value === 'New MySQL Database') {
        if (globals.someProperty('name', 'hive_ambari_host')) {
          globals.findProperty('name', 'hive_ambari_host').name = 'hive_mysql_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_existing_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_database'));
      } else {
        globals.findProperty('name', 'hive_existing_host').name = 'hive_mysql_hostname';
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
      var value = this.getGlobConfigValue(_config.templateName, _config.value, _config.name);
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

  getGlobConfigValue: function (templateName, expression, name) {
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
        // Hack for templeton.zookeeper.hosts
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          if (name === "templeton.zookeeper.hosts" || name === 'hbase.zookeeper.quorum') {
            // globValue is an array of ZooKeeper Server hosts
            var zooKeeperPort = '2181';
            if (name === "templeton.zookeeper.hosts") {
              var zooKeeperServers = globValue.map(function (item) {
                return item + ':' + zooKeeperPort;
              }).join(',');
              value = value.replace(_express, zooKeeperServers);
            } else {
              value = value.replace(_express, globValue.join(','));
            }
          } else {
            value = value.replace(_express, globValue);
          }
        }
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
    if (config.value == null) {
      return;
    }
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
        } else {
          config.value = null;
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
          _keyValue = _keyValue.trim();
          console.log("The value of the keyValue is: " + _keyValue);
          // split on the first = encountered (the value may contain ='s)
          var matches = _keyValue.match(/^([^=]+)=(.*)$/);
          if (matches) {
            var key = matches[1];
            var value = matches[2];
            if (key) {
              this.setSiteProperty(key, value, _site.name + '.xml');
            }
          }
        }, this);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    this.get('configs').pushObject({
      "id": "site property",
      "name": key,
      "value": value,
      "filename": filename
    });
  },

  createGlobalSiteObj: function () {
    var globalSiteProperties = {};
    //this.get('globals').filterProperty('domain', 'global').forEach(function (_globalSiteObj) {
    this.get('globals').forEach(function (_globalSiteObj) {
      // do not pass any globals whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
        // append "m" to JVM memory options except for hadoop_heapsize
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name) && _globalSiteObj.name !== 'hadoop_heapsize') {
          globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value + "m";
        } else {
          globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value;
        }
        console.log("STEP8: name of the global property is: " + _globalSiteObj.name);
        console.log("STEP8: value of the global property is: " + _globalSiteObj.value);
      }
    }, this);
    return {"type": "global", "properties": globalSiteProperties};
  },

  createCoreSiteObj: function () {
    var serviceName = this.get('service.serviceName');
    var coreSiteObj = this.get('configs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = serviceName == 'OOZIE';
    var oozieUser = this.get('globals').someProperty('name', 'oozie_user') ? this.get('globals').findProperty('name', 'oozie_user').value : null;
    var isHiveSelected = serviceName == 'HIVE';
    var hiveUser = this.get('globals').someProperty('name', 'hive_user') ? this.get('globals').findProperty('name', 'hive_user').value : null;
    var isHcatSelected = serviceName == 'WEBHCAT';
    var hcatUser = this.get('globals').someProperty('name', 'hcat_user') ? this.get('globals').findProperty('name', 'hcat_user').value : null;
    coreSiteObj.forEach(function (_coreSiteObj) {
      if ((isOozieSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.groups')) && (isHiveSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.groups')) && (isHcatSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.groups'))) {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      }
      console.log("STEP*: name of the property is: " + _coreSiteObj.name);
      console.log("STEP8: value of the property is: " + _coreSiteObj.value);
    }, this);
    return {"type": "core-site", "properties": coreSiteProperties};
  },

  createHdfsSiteObj: function () {
    var hdfsSiteObj = this.get('configs').filterProperty('filename', 'hdfs-site.xml');
    var hdfsProperties = {};
    hdfsSiteObj.forEach(function (_configProperty) {
      hdfsProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": "hdfs-site", "properties": hdfsProperties };
  },

  createMrSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'mapred-site.xml');
    var mrProperties = {};
    configs.forEach(function (_configProperty) {
      mrProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'mapred-site', properties: mrProperties};
  },

  createHbaseSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'hbase-site.xml');
    var hbaseProperties = {};
    configs.forEach(function (_configProperty) {
      hbaseProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'hbase-site', properties: hbaseProperties};
  },

  createOozieSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'oozie-site.xml');
    var oozieProperties = {};
    configs.forEach(function (_configProperty) {
      oozieProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'oozie-site', properties: oozieProperties};
  },

  createHiveSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'hive-site.xml');
    var hiveProperties = {};
    configs.forEach(function (_configProperty) {
      hiveProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'hive-site', properties: hiveProperties};
  },

  createWebHCatSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'webhcat-site.xml');
    var webHCatProperties = {};
    configs.forEach(function (_configProperty) {
      webHCatProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'webhcat-site', properties: webHCatProperties};
  },

  applyConfigs: function () {
    var self = this;
    var configTags;
    var clusterName = this.get('content.cluster.name');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + this.get('service.serviceName');
    $.ajax({
      type: 'GET',
      url: url,
      async: false,
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        configTags = jsonData.ServiceInfo.desired_configs;
      },

      error: function () {
        self.setTasksStatus(3, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });

    if (!configTags) {
      this.setTasksStatus(0, 'FAILED');
      return;
    }

    for (var tag in configTags) {
      if (this.get('createdConfigs').contains(tag)) {
        configTags[tag] = this.get('newConfigsTag');
      }
    }
    var data = {config: configTags};

    $.ajax({
      type: 'PUT',
      url: url,
      dataType: 'text',
      data: JSON.stringify(data),
      timeout: 5000,

      beforeSend: function () {
        self.setTasksStatus(3, 'PENDING');
      },

      success: function () {
        self.setTasksStatus(3, 'COMPLETED');
      },

      error: function () {
        self.setTasksStatus(3, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });
  },

  putInMaintenanceMode: function () {
    //todo after API providing
    this.setTasksStatus(4, 'COMPLETED');
  },

  installComponent: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/host_components?HostRoles/state=INIT';
    var data = '{"HostRoles": {"state": "INSTALLED"}}';
    var method = 'PUT';
    $.ajax({
      type: method,
      url: url,
      data: data,
      dataType: 'text',
      timeout: App.timeout,

      beforeSend: function () {
        self.setTasksStatus(5, 'PENDING');
      },

      success: function (data) {
        if (jQuery.parseJSON(data)) {
          self.set('currentRequestId', jQuery.parseJSON(data).Requests.id);
          self.getLogsByRequest();
        } else {
          self.setTasksStatus(5, 'FAILED');
        }
      },

      error: function () {
        self.setTasksStatus(5, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });
  },

  startComponents: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var serviceName = this.get('masterComponent.service_id');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + serviceName;
    var data = '{"ServiceInfo": {"state": "STARTED"}}';
    var method = 'PUT';
    $.ajax({
      type: method,
      url: url,
      data: data,
      dataType: 'text',
      timeout: App.timeout,

      beforeSend: function () {
        self.setTasksStatus(6, 'PENDING');
      },

      success: function (data) {
        if (jQuery.parseJSON(data)) {
          self.set('currentRequestId', jQuery.parseJSON(data).Requests.id);
          self.getLogsByRequest();
        }
      },

      error: function () {
        self.setTasksStatus(6, 'FAILED');
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * Parse logs to define status of Start, Stop ot Install task
   * @param logs
   */
  parseLogs: function (logs) {
    var self = this;
    var task;
    var stopPolling = false;
    var starting = false;
    var polledData = logs.tasks;
    var status;
    if ((this.get('tasks')[5].status != 'COMPLETED' && this.get('tasks')[6].status != 'COMPLETED' && this.get('tasks')[0].status != 'COMPLETED') ||
        ((this.get('tasks')[5].status == 'COMPLETED') && this.get('tasks')[0].status == 'COMPLETED' && this.get('tasks')[6].status != 'COMPLETED')) {
      //stopping or starting components
      if (this.get('tasks')[0].status == 'COMPLETED') {
        task = 6;
        starting = true;
      } else {
        task = 0;
      }
      if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
        if (polledData.someProperty('Tasks.status', 'FAILED')) {
          this.setTasksStatus(task, 'FAILED');
          status = 'FAILED'
        } else {
          this.setTasksStatus(task, 'COMPLETED');
          status = 'COMPLETED';
        }
        stopPolling = true;
      } else if (polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
        var progress = polledData.filterProperty('Tasks.status', 'COMPLETED').length / polledData.length * 100;
        this.setTasksStatus(task, 'IN_PROGRESS');
        this.get('tasks')[task].set('progress', Math.round(progress));
      }
    } else {
      //installing component
      status = polledData[0].Tasks.status;
      this.setTasksStatus(5, status);
      if (status == 'IN_PROGRESS') {
        this.get('tasks')[5].set('progress', '50');
      }
      if (status == 'COMPLETED' || status == 'FAILED') {
        stopPolling = true;
      }
    }
    if (!stopPolling) {
      window.setTimeout(function () {
        self.getLogsByRequest()
      }, self.POLL_INTERVAL);
    } else {
      if (status == 'FAILED') {
        //todo show retry
      }
      if (starting && status == 'COMPLETED') {
        this.set('isSubmitDisabled', false);
      }
    }
  },

  POLL_INTERVAL: 4000,

  getLogsByRequest: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var requestId = this.get('currentRequestId');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/requests/' + requestId + '?fields=tasks/*';
    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      dataType: 'text',
      success: function (data) {
        self.parseLogs(jQuery.parseJSON(data));
      },

      error: function () {
        this.set('status', 'FAILED');
      },

      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
        function () {
          App.showReloadPopup();
          console.log('Install services all retries FAILED');
        }
    );
  },

  removeComponent: function () {
    //todo after API providing
    this.setTasksStatus(7, 'COMPLETED');
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
})
