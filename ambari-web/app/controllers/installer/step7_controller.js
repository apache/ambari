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
var db = require('utils/db');

/**
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

App.InstallerStep7Controller = Em.ArrayController.extend({

  name: 'installerStep7Controller',

  content: [],

  selectedService: null,

  slaveHostToGroup: null,

  isSubmitDisabled: function () {
    return !this.everyProperty('errorCount', 0);
  }.property('@each.errorCount'),

  // TODO: set attributes from localStorage in router
  selectedServiceNames: [ 'HDFS', 'MAPREDUCE', 'GANGLIA', 'NAGIOS', 'HBASE', 'PIG', 'SQOOP', 'OOZIE', 'HIVE', 'ZOOKEEPER'],
  masterComponentHosts: require('data/mock/master_component_hosts'),
  slaveComponentHosts: [],
  serviceConfigs: require('data/service_configs'),

  clearStep: function () {
    this.clear();
    this.selectedServiceNames.clear();
    this.masterComponentHosts.clear();
    this.slaveComponentHosts.clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    this.clearStep();
    this.loadConfigs();
    this.renderServiceConfigs(this.serviceConfigs);
    var storedServices = db.getServiceConfigProperties();
    if (storedServices === undefined) {
      return;
    } else {
      var configs = new Ember.Set();
      var configProperties = new Ember.Set();
      this.forEach(function (_content) {
        _content.get('configs').forEach(function (_config) {
          configs.add(_config);
        }, this);
      }, this);

      var configProperties = new Ember.Set();
      configs.forEach(function (_config) {
        var temp = {name: _config.get('name'),
          value: _config.get('value')};
        configProperties.add(temp);
        if (storedServices.someProperty('name', _config.get('name'))) {
          var componentVal = storedServices.findProperty('name', _config.get('name'));
          _config.set('value', componentVal.value)
        }
      }, this);
    }
  },

  loadConfigs: function () {
    // load dependent data from the database
    var selectedServiceNamesInDB = db.getSelectedServiceNames();
    if (selectedServiceNamesInDB !== undefined) {
      this.set('selectedServiceNames', selectedServiceNamesInDB);
    }
    var masterComponentHostsInDB = db.getMasterComponentHosts();
    if (masterComponentHostsInDB != undefined) {
      this.set('masterComponentHosts', masterComponentHostsInDB);
    }
    var slaveComponentHostsInDB = db.getSlaveComponentHosts();
    if (slaveComponentHostsInDB != undefined) {
      this.set('slaveComponentHosts', slaveComponentHostsInDB);
    }
  },

  renderServiceConfigs: function (serviceConfigs) {
    var self = this;

    serviceConfigs.forEach(function (_serviceConfig) {
      var serviceConfig = App.ServiceConfig.create({
        serviceName: _serviceConfig.serviceName,
        displayName: _serviceConfig.displayName,
        configCategories: _serviceConfig.configCategories,
        configs: []
      });

      if (self.selectedServiceNames.contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
        self.renderComponentConfigs(_serviceConfig, serviceConfig);
      } else {
        console.log('skipping ' + serviceConfig.serviceName);
      }
    }, this);
  },

  renderComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      serviceConfigProperty.serviceConfig = componentConfig;
      serviceConfigProperty.initialValue();
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);

    console.log('pushing ' + componentConfig.serviceName);
    this.content.pushObject(componentConfig);
    this.set('selectedService', this.objectAt(0));
  },


  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      // TODO:
      // save service configs in App.db (localStorage)
      var serviceConfigProperties = [];
      this.content.forEach(function (_content) {
        var config = [];
        config = _content.get('configs');
        config.forEach(function (_configProperties) {
          var configProperty = {name: _configProperties.get('name'),
            value: _configProperties.get('value')};
          serviceConfigProperties.push(configProperty);
        }, this);

      }, this);
      db.setServiceConfigProperties(serviceConfigProperties);
      App.router.send('next');
    }
  },

  showMasterHosts: function (event) {
    var serviceConfig = event.context;
    App.ModalPopup.show({
      header: serviceConfig.category + ' Hosts',
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/installer/master_hosts_popup')
      })
    });
  },

  showSlaveHosts: function (event) {
    var serviceConfig = event.context;
    App.ModalPopup.show({
      header: serviceConfig.category + ' Hosts',
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/installer/slave_hosts_popup')
      })
    });
  }

})
;

App.SlaveComponentGroupsController = Ember.ArrayController.extend({

  name: 'slaveComponentGroupsController',

  contentBinding: 'App.router.installerStep7Controller.slaveComponentHosts',

  selectedComponentName: function () {
    switch (App.router.get('installerStep7Controller.selectedService.serviceName')) {
      case 'HDFS':
        return 'DATANODE';
      case 'MAPREDUCE':
        return 'TASKTRACKER';
      case 'HBASE':
        return 'HBASE_REGIONSERVER';
      default:
        return null;
    }

  }.property('App.router.installerStep7Controller.selectedService'),

  selectedSlaveComponent: function () {
    var component = this.findProperty('componentName', this.get('selectedComponentName'));
    return component;
  }.property('selectedComponentName'),

  showAddSlaveComponentGroup: function (event) {
    var componentName = event.context;
    App.ModalPopup.show({
      header: componentName + ' Groups',
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.slaveComponentGroupsController',
        templateName: require('templates/installer/slave_hosts_popup')
      }),
      onPrimary: function () {
      }
    });
  },

  addSlaveComponentGroup: function (event) {
    var componentName = event.context;
    var component = this.findProperty('componentName', componentName);
    var newGroupName;
    component.groups.setEach('active', false);
    var newGroups = component.groups.filterProperty('type', 'new');
    if (newGroups.length === 0) {
      component.newGroupIndex = 0;
      newGroupName = 'New Group';
    } else {
      component.newGroupIndex++;
      newGroupName = 'New Group ' + component.newGroupIndex;
    }
    var newGroup = {name: newGroupName, index: component.newGroupIndex, type: 'new', active: true};
    component.groups.pushObject(newGroup);
  },

  showEditSlaveComponentGroups: function (event) {
    this.showAddSlaveComponentGroup(event);
  },

  hosts: function () {
    if (this.get('selectedComponentName') !== null && this.get('selectedComponentName') !== undefined) {
      var component = this.findProperty('componentName', this.get('selectedComponentName'));
      if (component !== undefined && component !== null) {
        return component.hosts;
      }
    }
  }.property('@each.hosts', 'selectedComponentName'),

  groups: function () {
    if (this.get('selectedComponentName') !== null) {
      var component = this.findProperty('componentName', this.get('selectedComponentName'));
      if (component !== undefined && component !== null) {
        return component.hosts.mapProperty('group').uniq();
      }
    }
  }.property('@each.hosts', 'selectedComponentName'),

  componentGroups: function () {
    if (this.get('selectedComponentName') !== null) {
      var component = this.findProperty('componentName', this.get('selectedComponentName'));
      if (component !== undefined && component !== null) {
        if (component.groups === undefined){
          component.groups = [];
          var defaultGroup = {name: 'Default', index: 'default', type: 'default', active: true};
          component.groups.pushObject(defaultGroup);
        }
        return component.groups;
      }
    }
  }.property('selectedComponentName'),

//  activeGroup: function(){
//    return this.get('componentGroups').findProperty('active', true);
//  }.property('selectedComponentName', 'componentGroups.@each'),

  showSlaveComponentGroup: function(event){
    var component = this.findProperty('componentName', this.get('selectedComponentName'));
    component.groups.setEach('active', false);
    var group = component.groups.filterProperty('name', event.context.name);
    group.setEach('active', true);
  },

  removeSlaveComponentGroup: function(event){
    var group = event.context;
    var component = this.findProperty('componentName', this.get('selectedComponentName'));
    var assignedHosts = component.hosts.filterProperty('group', group.name);
    if (assignedHosts.length !== 0){
      $('.remove-group-error').show();
    } else {
      $('.remove-group-error').hide();
      var key = component.groups.indexOf(group);
      component.groups.removeObject(component.groups[key]);
//      $('#slave-group'+ group.index).remove();

      if(group.type === 'new' && component.newGroupIndex === group.index){
        component.newGroupIndex--;
      }
      if (group.active){
        var lastGroup;
        if (key === component.groups.length)
          lastGroup = component.groups.slice(key-1, key);
        else lastGroup = component.groups.slice(key, key+1);
        lastGroup.setEach('active', true);
      }
    }
  }

});
