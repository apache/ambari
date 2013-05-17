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

  status: 'IN_PROGRESS',

  onStatusChange: function () {
    if (this.get('tasks').someProperty('status', 'FAILED')) {
      this.set('status', 'FAILED');
      if (this.get('tasks')[5].status == 'FAILED' || this.get('tasks')[6].status == 'FAILED') {
        this.set('showRetry', true);
      }
    } else if (this.get('tasks').everyProperty('status', 'COMPLETED')) {
      this.set('status', 'COMPLETED');
      this.set('isSubmitDisabled', false);
    } else {
      this.set('status', 'IN_PROGRESS')
    }
    var statuses = this.get('tasks').mapProperty('status');
    App.router.get(this.get('content.controllerName')).saveTasksStatuses(statuses);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'REASSIGN_MASTER_INSTALLING',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
    this.setTasksMessages();
    this.navigateStep();
  },

  tasks: [],

  /**
   * Set messages for tasks depending on their status
   */
  setTasksMessages: function () {
    var service = this.get('service.displayName');
    var master = this.get('masterComponent.display_name');
    if (this.get('isCohosted')) {
      service = 'Hive, WebHCat';
      master = Em.I18n.t('installer.step5.hiveGroup');
    }
    for (i = 0; i < this.get('tasks').length; i++) {
      var status = this.get('tasks')[i].status.toLowerCase().replace('initialize', 'pending').replace('_', ' ');
      if (i == 0 || i == 6) {
        this.get('tasks')[i].set('message', Em.I18n.t('installer.step14.task' + i).format(service) + ' ' + status);
      } else {
        this.get('tasks')[i].set('message', Em.I18n.t('installer.step14.task' + i).format(master) + ' ' + status);
      }
    }
  },

  configs: [],
  globals: [],
  configMapping: require('data/config_mapping').all(),
  newConfigsTag: null,
  createdConfigs: [],

  currentRequestId: [],

  isSubmitDisabled: true,

  showRetry: false,

  service: function () {
    return App.Service.find().findProperty('serviceName', this.get('masterComponent.service_id'));
  }.property('masterComponent'),

  masterComponent: function () {
    return this.get('content.reassign');
  }.property('content.reassign'),

  isCohosted: function () {
    return this.get('masterComponent.component_name') == 'HIVE_SERVER';
  }.property('masterComponent'),

  loadStep: function () {
    this.clearStep();
    this.loadTasks();
    this.addObserver('tasks.@each.status', this, 'onStatusChange');
    this.onStatusChange();
  },

  clearStep: function () {
    this.removeObserver('tasks.@each.status', this, 'onStatusChange');
    this.removeObserver('createdConfigs.length', this, 'onCreateConfigsCompleted');
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
    this.set('createdConfigsCount', 0);
    this.set('queueTasksCompleted', 0);
    this.set('dataPollCounter', 1);
    this.set('showRetry', false);
    this.set('isSubmitDisabled', true);
    this.get('configs').clear();
    this.get('globals').clear();
    this.get('createdConfigs').clear();
  },

  loadTasks: function () {
    var statuses = this.get('content.tasksStatuses');
    if (statuses) {
      statuses.forEach(function (status, index) {
        this.get('tasks')[index].status = status;
      }, this)
    }
    var statusesForRequestId = ['PENDING', 'QUEUED', 'IN_PROGRESS'];
    if (statusesForRequestId.contains(statuses[0]) || statusesForRequestId.contains(statuses[5]) || statusesForRequestId.contains(statuses[6])) {
      this.set('currentRequestId', this.get('content.cluster.requestId'));
      this.getLogsByRequest();
    }
  },

  /**
   * Run tasks in proper way
   */
  navigateStep: function () {
    if (this.get('tasks')[0].status == 'INITIALIZE') {
      this.stopService();
    }
    else if (this.taskIsReady(1)) {
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
  },

  /**
   * Determine preparedness to run task
   * @param task
   * @return {Boolean}
   */
  taskIsReady: function (task) {
    if (this.get('tasks')[task].status != 'INITIALIZE') {
      return false;
    }
    var tempArr = this.get('tasks').mapProperty('status').slice(0, task).uniq();
    return tempArr.length == 1 && tempArr[0] == 'COMPLETED';
  },

  queueTasksCompleted: 0,

  /**
   * Change status of the task
   * @param task
   * @param status
   */
  setTasksStatus: function (task, status) {
    if (status == 'COMPLETED' && this.get('isCohosted') && [1, 4, 7].contains(task) && this.get('queueTasksCompleted') < 2) {
      this.set('queueTasksCompleted', this.get('queueTasksCompleted') + 1);
    } else {
      this.get('tasks')[task].set('status', status);
    }
  },

  saveClusterStatus: function (requestId, status) {
    var clusterStatus = {
      status: status,
      requestId: requestId
    };
    App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
  },

  stopService: function () {
    this.set('currentRequestId', []);
    var serviceNames = [this.get('masterComponent.service_id')];
    if (this.get('isCohosted')) {
      serviceNames = ['HIVE', 'WEBHCAT'];
    }
    serviceNames.forEach(function (serviceName) {
      App.ajax.send({
        name: 'reassign.stop_service',
        sender: this,
        data: {
          serviceName: serviceName,
          displayName: App.Service.find().findProperty('serviceName', serviceName).get('displayName')
        },
        beforeSend: 'onStopServiceBeforeSend',
        success: 'onStopServiceSuccess',
        error: 'onStopServiceError'
      });
    }, this);
  },

  onStopServiceBeforeSend: function () {
    this.setTasksStatus(0, 'PENDING');
  },

  onStopServiceSuccess: function (data) {
    if (data) {
      var requestId = data.Requests.id;
      this.get('currentRequestId').push(requestId);
      this.saveClusterStatus(this.get('currentRequestId'), 'PENDING');
      if ((this.get('isCohosted') && this.get('currentRequestId.length') == 2) || !this.get('isCohosted')) {
        this.getLogsByRequest();
      }
    } else {
      this.setTasksStatus(0, 'FAILED');
    }
  },

  onStopServiceError: function () {
    this.setTasksStatus(0, 'FAILED');
  },

  createMasterComponent: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    var componentNames = [this.get('masterComponent.component_name')];
    if (this.get('isCohosted')) {
      this.set('queueTasksCompleted', 0);
      componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
    }
    componentNames.forEach(function (componentName) {
      if (App.testMode) {
        this.setTasksStatus(1, 'COMPLETED');
      } else {
        App.ajax.send({
          name: 'reassign.create_master',
          sender: this,
          data: {
            hostName: hostName,
            componentName: componentName
          },
          beforeSend: 'onCreateMasterComponentBeforeSend',
          success: 'onCreateMasterComponentSuccess',
          error: 'onCreateMasterComponentError'
        });
      }
    }, this);
  },

  onCreateMasterComponentBeforeSend: function () {
    this.setTasksStatus(1, 'PENDING');
  },

  onCreateMasterComponentSuccess: function () {
    this.setTasksStatus(1, 'COMPLETED');
  },

  onCreateMasterComponentError: function () {
    this.setTasksStatus(1, 'FAILED');
  },

  createConfigs: function () {
    if (this.get('service.serviceName') == 'GANGLIA' || App.testMode) {
      this.setTasksStatus(2, 'COMPLETED');
    } else {
      this.setTasksStatus(2, 'PENDING');
      this.loadGlobals();
      this.loadConfigs();
      this.set('newConfigsTag', 'version' + (new Date).getTime());
      var serviceName = this.get('service.serviceName');
      this.createConfigSite(this.createGlobalSiteObj());
      this.createConfigSite(this.createCoreSiteObj());
      if (serviceName == 'HDFS') {
        this.createConfigSite(this.createSiteObj('hdfs-site'));
      }
      if (serviceName == 'MAPREDUCE') {
        this.createConfigSite(this.createSiteObj('mapred-site'));
      }
      if (serviceName == 'HBASE') {
        this.createConfigSite(this.createSiteObj('hbase-site'));
      }
      if (serviceName == 'OOZIE') {
        this.createConfigSite(this.createSiteObj('oozie-site'));
      }
      if (serviceName == 'HIVE' || this.get('isCohosted')) {
        this.createConfigSite(this.createSiteObj('hive-site'));
      }
      if (serviceName == 'WEBHCAT' || this.get('isCohosted')) {
        this.createConfigSite(this.createSiteObj('webhcat-site'));
      }
      this.addObserver('createdConfigs.length', this, 'onCreateConfigsCompleted');
      this.onCreateConfigsCompleted();
    }
  },

  createConfigSite: function (configs) {
    configs.tag = this.get('newConfigsTag');
    App.ajax.send({
      name: 'reassign.create_configs',
      sender: this,
      data: {
        configs: configs
      },
      beforeSend: 'onCreateConfigsBeforeSend',
      success: 'onCreateConfigsSuccess',
      error: 'onCreateConfigsError'
    });
  },

  onCreateConfigsBeforeSend: function () {
    this.set('createdConfigsCount', this.get('createdConfigsCount') + 1);
  },

  onCreateConfigsSuccess: function (data, opts) {
    this.get('createdConfigs').pushObject(opts.configs.type);
  },

  onCreateConfigsError: function () {
    this.setTasksStatus(2, 'FAILED');
  },

  createdConfigsCount: 0,

  onCreateConfigsCompleted: function () {
    if (this.get('createdConfigs.length') == this.get('createdConfigsCount')) {
      this.setTasksStatus(2, 'COMPLETED');
    }
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
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globals').someProperty('name', templateName[index])) {
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
    }, this);
    return {"type": "core-site", "properties": coreSiteProperties};
  },

  createSiteObj: function (name) {
    var fileName = name + '.xml';
    var configs = this.get('configs').filterProperty('filename', fileName);
    var properties = {};
    configs.forEach(function (_configProperty) {
      properties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: name, properties: properties};
  },

  applyConfigs: function () {
    if (this.get('service.serviceName') == 'GANGLIA' || App.testMode) {
      this.setTasksStatus(3, 'COMPLETED');
    } else {
      var serviceName = this.get('service.serviceName');
      App.ajax.send({
        name: 'reassign.check_configs',
        sender: this,
        data: {
          serviceName: serviceName
        },
        success: 'onCheckConfigsSuccess',
        error: 'onCheckConfigsError'
      });
    }
  },

  onCheckConfigsSuccess: function (configs) {
    var configTags = configs.ServiceInfo.desired_configs;
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
    var serviceName = this.get('service.serviceName');
    App.ajax.send({
      name: 'reassign.apply_configs',
      sender: this,
      data: {
        serviceName: serviceName,
        configs: data
      },
      beforeSend: 'onApplyConfigsBeforeSend',
      success: 'onApplyConfigsSuccess',
      error: 'onApplyConfigsError'
    });
  },

  onCheckConfigsError: function () {
    this.setTasksStatus(3, 'FAILED');
  },

  onApplyConfigsBeforeSend: function () {
    this.setTasksStatus(3, 'PENDING');
  },

  onApplyConfigsSuccess: function () {
    this.setTasksStatus(3, 'COMPLETED');
  },

  onApplyConfigsError: function () {
    this.setTasksStatus(3, 'FAILED');
  },

  putInMaintenanceMode: function () {
    if (App.testMode) {
      this.setTasksStatus(4, 'COMPLETED');
    } else {
      var hostName = this.get('content.reassign.host_id');
      var componentNames = [this.get('masterComponent.component_name')];
      if (this.get('isCohosted')) {
        componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
        this.set('queueTasksCompleted', 0);
      }
      componentNames.forEach(function (componentName) {
        App.ajax.send({
          name: 'reassign.maintenance_mode',
          sender: this,
          data: {
            hostName: hostName,
            componentName: componentName
          },
          beforeSend: 'onPutInMaintenanceModeBeforeSend',
          success: 'onPutInMaintenanceModeSuccess',
          error: 'onPutInMaintenanceModeError'
        });
      }, this);
    }
  },

  onPutInMaintenanceModeBeforeSend: function () {
    this.setTasksStatus(4, 'PENDING');
  },

  onPutInMaintenanceModeSuccess: function () {
    this.setTasksStatus(4, 'COMPLETED');
  },

  onPutInMaintenanceModeError: function () {
    this.setTasksStatus(4, 'FAILED');
  },

  installComponent: function () {
    this.set('currentRequestId', []);
    var componentNames = [this.get('masterComponent.component_name')];
    if (this.get('isCohosted')) {
      componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
    }
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    componentNames.forEach(function (componentName) {
      App.ajax.send({
        name: 'reassign.install_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: componentName,
          displayName: App.format.role(componentName)
        },
        beforeSend: 'onInstallComponentBeforeSend',
        success: 'onInstallComponentSuccess',
        error: 'onInstallComponentError'
      });
    }, this);
  },

  onInstallComponentBeforeSend: function () {
    this.setTasksStatus(5, 'PENDING');
  },

  onInstallComponentSuccess: function (data) {
    if (data) {
      var requestId = data.Requests.id;
      this.get('currentRequestId').push(requestId);
      this.saveClusterStatus(this.get('currentRequestId'), 'PENDING');
      if ((this.get('isCohosted') && this.get('currentRequestId.length') == 3) || !this.get('isCohosted')) {
        this.getLogsByRequest();
      }
    } else {
      this.setTasksStatus(5, 'FAILED');
    }
  },

  onInstallComponentError: function () {
    this.setTasksStatus(5, 'FAILED');
  },

  startComponents: function () {
    this.set('currentRequestId', []);
    var serviceNames = [this.get('masterComponent.service_id')];
    if (this.get('isCohosted')) {
      serviceNames = ['HIVE', 'WEBHCAT'];
    }
    serviceNames.forEach(function (serviceName) {
      App.ajax.send({
        name: 'reassign.start_components',
        sender: this,
        data: {
          serviceName: serviceName,
          displayName: App.Service.find().findProperty('serviceName', serviceName).get('displayName')
        },
        beforeSend: 'onStartComponentsBeforeSend',
        success: 'onStartComponentsSuccess',
        error: 'onStartComponentsError'
      });
    }, this);
  },

  onStartComponentsBeforeSend: function () {
    this.setTasksStatus(6, 'PENDING');
  },

  onStartComponentsSuccess: function (data) {
    if (data) {
      var requestId = data.Requests.id;
      this.get('currentRequestId').push(requestId);
      this.saveClusterStatus(this.get('currentRequestId'), 'PENDING');
      if ((this.get('isCohosted') && this.get('currentRequestId.length') == 2) || !this.get('isCohosted')) {
        this.getLogsByRequest();
      }
    } else {
      this.setTasksStatus(6, 'FAILED');
    }
  },

  onStartComponentsError: function () {
    this.setTasksStatus(6, 'FAILED');
  },

  /**
   * Parse logs to define status of Start, Stop ot Install task
   * @param logs
   */
  parseLogs: function (logs) {
    var self = this;
    var task;
    var stopPolling = false;
    var polledData = [];
    logs.forEach(function (item) {
      polledData = polledData.concat(item.tasks);
    }, this);
    if (this.get('tasks')[0].status == 'COMPLETED') {
      task = this.get('tasks')[5].status == 'COMPLETED' ? 6 : 5;
    } else {
      task = 0;
    }
    if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      if (polledData.someProperty('Tasks.status', 'FAILED')) {
        this.setTasksStatus(task, 'FAILED');
      } else {
        this.setTasksStatus(task, 'COMPLETED');
      }
      stopPolling = true;
    } else {
      if (polledData.length == 1) {
        this.get('tasks')[task].set('progress', 50);
      } else {
        var progress = polledData.filterProperty('Tasks.status', 'COMPLETED').length / polledData.length * 100;
        this.get('tasks')[task].set('progress', Math.round(progress));
      }
      this.setTasksStatus(task, 'IN_PROGRESS');
    }
    if (!stopPolling) {
      window.setTimeout(function () {
        self.getLogsByRequest()
      }, self.POLL_INTERVAL);
    }
  },

  POLL_INTERVAL: 4000,
  dataPollCounter: 1,

  getLogsByRequest: function () {
    this.set('logs', []);
    if (App.testMode) {
      var data = require('data/mock/step14PolledData/tasks_poll' + this.get('dataPollCounter'));
      this.set('dataPollCounter', this.get('dataPollCounter') + 1);
      if (this.get('dataPollCounter') == 6) {
        this.set('dataPollCounter', 1);
      }
      this.onGetLogsByRequestSuccess(data);
    } else {
      var requestIds = this.get('currentRequestId');
      requestIds.forEach(function (requestId) {
        App.ajax.send({
          name: 'reassign.get_logs',
          sender: this,
          data: {
            requestId: requestId
          },
          success: 'onGetLogsByRequestSuccess',
          error: 'onGetLogsByRequestError'
        });
      }, this);
    }
  },

  logs: [],

  onGetLogsByRequestSuccess: function (data) {
    this.get('logs').push(data);
    if (this.get('logs.length') == this.get('currentRequestId.length') || App.testMode) {
      this.parseLogs(this.get('logs'))
    }
  },

  onGetLogsByRequestError: function () {
    this.set('status', 'FAILED');
  },

  removeComponent: function () {
    if (App.testMode) {
      this.setTasksStatus(7, 'COMPLETED');
    } else {
      var hostName = this.get('content.reassign.host_id');
      var componentNames = [this.get('masterComponent.component_name')];
      if (this.get('isCohosted')) {
        componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
        this.set('queueTasksCompleted', 0);
      }
      componentNames.forEach(function (componentName) {
        App.ajax.send({
          name: 'reassign.remove_component',
          sender: this,
          data: {
            hostName: hostName,
            componentName: componentName
          },
          beforeSend: 'onRemoveComponentBeforeSend',
          success: 'onRemoveComponentSuccess',
          error: 'onRemoveComponentError'
        });
      }, this);
    }
  },

  onRemoveComponentBeforeSend: function () {
    this.setTasksStatus(7, 'PENDING');
  },

  onRemoveComponentSuccess: function () {
    this.setTasksStatus(7, 'COMPLETED');
  },

  onRemoveComponentError: function () {
    this.setTasksStatus(7, 'FAILED');
  },

  retry: function () {
    if (this.get('tasks')[5].status == 'FAILED') {
      this.installComponent();
    } else {
      this.startComponents();
    }
    this.set('showRetry', false);
  },

  abort: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    var componentNames = [this.get('masterComponent.component_name')];
    if (this.get('isCohosted')) {
      componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
      this.set('queueTasksCompleted', 0);
    }
    componentNames.forEach(function (componentName) {
      App.ajax.send({
        name: 'reassign.maintenance_mode',
        sender: this,
        data: {
          hostName: hostName,
          componentName: componentName
        },
        success: 'onAbortMaintenance',
        error: 'onAbortError'
      });
    }, this);
  },


  onAbortMaintenance: function () {
    if (this.get('isCohosted') && this.get('queueTasksCompleted') < 2) {
      this.set('queueTasksCompleted', this.get('queueTasksCompleted') + 1);
    } else {
      var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
      var componentNames = [this.get('masterComponent.component_name')];
      if (this.get('isCohosted')) {
        componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
        this.set('queueTasksCompleted', 0);
      }
      componentNames.forEach(function (componentName) {
        App.ajax.send({
          name: 'reassign.remove_component',
          sender: this,
          data: {
            hostName: hostName,
            componentName: componentName
          },
          success: 'onAbortRemoveComponent',
          error: 'onAbortError'
        });
      }, this);
    }
  },

  onAbortRemoveComponent: function () {
    if (this.get('isCohosted') && this.get('queueTasksCompleted') < 2) {
      this.set('queueTasksCompleted', this.get('queueTasksCompleted') + 1);
    } else {
      var hostName = this.get('content.reassign.host_id');
      var componentNames = [this.get('masterComponent.component_name')];
      if (this.get('isCohosted')) {
        componentNames = ['HIVE_SERVER', 'WEBHCAT_SERVER', 'MYSQL_SERVER'];
        this.set('queueTasksCompleted', 0);
      }
      componentNames.forEach(function (componentName) {
        App.ajax.send({
          name: 'reassign.install_component',
          sender: this,
          data: {
            hostName: hostName,
            componentName: componentName
          },
          success: 'onAbortCompleted',
          error: 'onAbortError'
        });
      }, this);
    }
  },

  onAbortCompleted: function () {
    if (this.get('isCohosted') && this.get('queueTasksCompleted') < 2) {
      this.set('queueTasksCompleted', this.get('queueTasksCompleted') + 1);
    } else {
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_ABORTED',
        wizardControllerName: this.get('content.controllerName'),
        localdb: App.db.data
      });
      App.router.send('back');
    }
  },

  onAbortError: function () {
    App.ModalPopup.show({
      header: Em.I18n.translations['common.error'],
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t installer.step14.abortError}}</p>')
      })
    });
  }
})
