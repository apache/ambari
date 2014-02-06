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
var uiEffects = require('utils/ui_effects');

App.MainHostSummaryView = Em.View.extend({
  templateName: require('templates/main/host/summary'),

  isStopCommand:true,

  content: function () {
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  showGangliaCharts: function () {
    var name = this.get('content.hostName');
    var gangliaMobileUrl = App.router.get('clusterController.gangliaUrl') + "/mobile_helper.php?show_host_metrics=1&h=" + name + "&c=HDPNameNode&r=hour&cs=&ce=";
    window.open(gangliaMobileUrl);
  },

  needToRestartMessage: function() {
    var componentsCount, word;
    componentsCount = this.get('content.componentsWithStaleConfigsCount');
    if (componentsCount > 1) {
      word = Em.I18n.t('common.components').toLowerCase();
    } else {
      word = Em.I18n.t('common.component').toLowerCase();
    }
    return Em.I18n.t('hosts.host.details.needToRestart').format(this.get('content.componentsWithStaleConfigsCount'), word);
  }.property('content.componentsWithStaleConfigsCount'),

  didInsertElement: function () {
    this.addToolTip();
  },
  addToolTip: function() {
    if (this.get('addComponentDisabled')) {
      App.tooltip($('#add_component'), {title: Em.I18n.t('services.nothingToAdd')});
    }
  }.observes('addComponentDisabled'),
  sortedComponents: function () {
    var slaveComponents = [];
    var masterComponents = [];
    this.get('content.hostComponents').forEach(function (component) {
      if (component.get('isMaster')) {
        masterComponents.push(component);
      } else if (component.get('isSlave')) {
        slaveComponents.push(component);
      }
    }, this);
    return masterComponents.concat(slaveComponents);
  }.property('content', 'content.hostComponents.length'),
  clients: function () {
    var clients = [];
    this.get('content.hostComponents').forEach(function (component) {
      if (!component.get('componentName')) {
        //temporary fix because of different data in hostComponents and serviceComponents
        return;
      }
      if (!component.get('isSlave') && !component.get('isMaster')) {
        if (clients.length) {
          clients[clients.length - 1].set('isLast', false);
        }
        component.set('isLast', true);
        clients.push(component);
      }
    }, this);
    return clients;
  }.property('content'),

  /**
   * Check if some clients have stale configs
   * @type {bool}
   */
  areClientWithStaleConfigs: function() {
    return !!this.get('clients').filter(function(component) {
      return component.get('staleConfigs');
    }).length;
  }.property('clients.@each.staleConfigs'),


  addableComponentObject: Em.Object.extend({
    componentName: '',
    subComponentNames: null,
    displayName: function () {
      if (this.get('componentName') === 'CLIENTS') {
        return this.t('common.clients');
      }
      return App.format.role(this.get('componentName'));
    }.property('componentName')
  }),
  isAddComponent: function () {
    return this.get('content.healthClass') !== 'health-status-DEAD-YELLOW';
  }.property('content.healthClass'),

  addComponentDisabled: function() {
    return (!this.get('isAddComponent')) || (this.get('addableComponents.length') == 0);
  }.property('isAddComponent', 'addableComponents.length'),

  installableClientComponents: function() {
    var installableClients = [];
    if (!App.supports.deleteHost) {
      return installableClients;
    }
    App.Service.find().forEach(function(svc){
      switch(svc.get('serviceName')){
        case 'PIG':
          installableClients.push('PIG');
          break;
        case 'SQOOP':
          installableClients.push('SQOOP');
          break;
        case 'HCATALOG':
          installableClients.push('HCAT');
          break;
        case 'HDFS':
          installableClients.push('HDFS_CLIENT');
          break;
        case 'OOZIE':
          installableClients.push('OOZIE_CLIENT');
          break;
        case 'ZOOKEEPER':
          installableClients.push('ZOOKEEPER_CLIENT');
          break;
        case 'HIVE':
          installableClients.push('HIVE_CLIENT');
          break;
        case 'HBASE':
          installableClients.push('HBASE_CLIENT');
          break;
        case 'YARN':
          installableClients.push('YARN_CLIENT');
          break;
        case 'MAPREDUCE':
          installableClients.push('MAPREDUCE_CLIENT');
          break;
        case 'MAPREDUCE2':
          installableClients.push('MAPREDUCE2_CLIENT');
          break;
        case 'TEZ':
          installableClients.push('TEZ_CLIENT');
          break;
      }
    });
    this.get('content.hostComponents').forEach(function (component) {
      var index = installableClients.indexOf(component.get('componentName'));
      if (index > -1) {
        installableClients.splice(index, 1);
      }
    }, this);
    return installableClients;
  }.property('content', 'content.hostComponents.length', 'App.Service', 'App.supports.deleteHost'),
  
  addableComponents: function () {
    var components = [];
    var services = App.Service.find();
    var dataNodeExists = false;
    var taskTrackerExists = false;
    var regionServerExists = false;
    var zookeeperServerExists = false;
    var nodeManagerExists = false;
    var hbaseMasterExists = false;
    var supervisorExists = false;
    
    var installableClients = this.get('installableClientComponents');
    
    this.get('content.hostComponents').forEach(function (component) {
      switch (component.get('componentName')) {
        case 'DATANODE':
          dataNodeExists = true;
          break;
        case 'TASKTRACKER':
          taskTrackerExists = true;
          break;
        case 'HBASE_REGIONSERVER':
          regionServerExists = true;
          break;
        case 'ZOOKEEPER_SERVER':
          zookeeperServerExists = true;
          break;
        case 'NODEMANAGER':
          nodeManagerExists = true;
          break;
        case 'HBASE_MASTER':
          hbaseMasterExists = true;
          break;
        case 'SUPERVISOR':
          supervisorExists = true;
          break;
      }
    }, this);

    if (!dataNodeExists) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'DATANODE' }));
    }
    if (!taskTrackerExists && services.findProperty('serviceName', 'MAPREDUCE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'TASKTRACKER' }));
    }
    if (!regionServerExists && services.findProperty('serviceName', 'HBASE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'HBASE_REGIONSERVER' }));
    }
    if (!hbaseMasterExists && services.findProperty('serviceName', 'HBASE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'HBASE_MASTER' }));
    }
    if (!zookeeperServerExists && services.findProperty('serviceName', 'ZOOKEEPER')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'ZOOKEEPER_SERVER' }));
    }
    if (!nodeManagerExists && services.findProperty('serviceName', 'YARN')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'NODEMANAGER' }));
    }
    if (!supervisorExists && services.findProperty('serviceName', 'STORM')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'SUPERVISOR' }));
    }
    if (installableClients.length > 0) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'CLIENTS', subComponentNames: installableClients }));
    }
    return components;
  }.property('content', 'content.hostComponents.length', 'installableClientComponents'),

  timeSinceHeartBeat: function () {
    var d = this.get('content.lastHeartBeatTime');
    if (d) {
      return $.timeago(d);
    }
    return "";
  }.property('content.lastHeartBeatTime')
});
