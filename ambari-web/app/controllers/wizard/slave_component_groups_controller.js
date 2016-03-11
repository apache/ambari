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
var configPropertyHelper = require('utils/configs/config_property_helper');
/**
 * Used to manage slave component config. User could create different settings for separate group
 * @type {*}
 */
App.SlaveComponentGroupsController = Em.ArrayController.extend({

  name: 'slaveComponentGroupsController',

  contentBinding: 'App.router.wizardStep7Controller.slaveComponentHosts',

  stepConfigsBinding: 'App.router.wizardStep7Controller.stepConfigs',

  serviceBinding: 'App.router.wizardStep7Controller.selectedService',

  servicesBinding: 'App.router.wizardStep7Controller.content.services',

  clearStep: function () {

  },

  loadStep: function () {
    this.clearStep();
    this.loadGroups();
  },

  loadGroups: function () {

    this.get('stepConfigs').forEach(function (_serviceConfig) {
      var categoryConfig = _serviceConfig.get('configCategories');
      if (categoryConfig.someProperty('isForSlaveComponent', true)) {
        var slaveCategory = categoryConfig.findProperty('isForSlaveComponent', true);
        // this.get('content') -> Output of Step 6: Mapping of each slave component and set of hosts it runs on
        if (this.get('content')) {
          if (this.get('content').someProperty('componentName', slaveCategory.get('primaryName'))) {
            // component --> each column in Step 6 is a component ( slave component )
            var component = this.get('content').findProperty('componentName', slaveCategory.get('primaryName'));
            // slaveConfigs --> originally set as null in the class App.SlaveCategory in model/service_config.js
            var slaveConfigs = slaveCategory.get('slaveConfigs');
            
            slaveCategory.set('slaveConfigs', App.SlaveConfigs.create(component));
            var slaveGroups = [];
            if (component.groups) {
              component.groups.forEach(function (_group) {
                slaveGroups.pushObject(_group);
              }, this);
              slaveCategory.set('slaveConfigs.groups', slaveGroups);
            }
            slaveCategory.set('slaveConfigs.componentName', component.componentName);
            slaveCategory.set('slaveConfigs.displayName', component.displayName);
            /*slaveCategory.set('slaveConfigs.groups.name', component.get('name'));
             slaveCategory.set('slaveConfigs.groups.index', component.get('index'));
             slaveCategory.set('slaveConfigs.groups.type', component.get('type'));
             slaveCategory.set('slaveConfigs.groups.active', component.get('active'));*/
            if (!slaveCategory.get('slaveConfigs.groups')) {
              slaveCategory.set('slaveConfigs.groups', []);
              var componentProperties = this.componentProperties(_serviceConfig.serviceName);
              var defaultGroup = {name: 'Default', index: 'default', type: 'default', active: true, properties: componentProperties};
              slaveCategory.get('slaveConfigs.groups').pushObject(App.Group.create(defaultGroup));
            }
          }
        }
      }
    }, this);
  },

  // returns key-value pairs i.e. all fields for slave component for this specific service.
  componentProperties: function (serviceName) {

    var serviceConfigs = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', serviceName);

    var configs = [];
    var componentName = null;
    switch (serviceName) {
      case 'HDFS':
        componentName = 'DataNode';
        break;
      case 'HBASE':
        componentName = 'RegionServer';
    }
    var slaveConfigs = serviceConfigs.configs.filterProperty('category', componentName);
    slaveConfigs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);

      switch(serviceConfigProperty.name){
        case 'dfs_data_dir' :
          configPropertyHelper.initialValue(serviceConfigProperty);
          break;
        case 'mapred_local_dir' :
          configPropertyHelper.initialValue(serviceConfigProperty);
          break;
      }
      configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
    return configs;
  },

  selectedComponentName: function () {
    switch (App.router.get('wizardStep7Controller.selectedService.serviceName')) {
      case 'HDFS':
        return { name: 'DATANODE',
          displayName: 'DataNode'};
      case 'HBASE':
        return { name: 'HBASE_REGIONSERVER',
          displayName: 'RegionServer'};
      default:
        return null;
    }

  }.property('service'),

  selectedComponentDisplayName: function() {
    return App.format.role(this.get('selectedComponentName'), false);
  }.property('selectedComponentName'),

  selectedSlaveComponent: function () {
    var selectedComponentName = this.get('selectedComponentName') ? this.get('selectedComponentName').displayName : null;
    var configs = null;
    if (selectedComponentName) {
      App.router.get('wizardStep7Controller.stepConfigs').forEach(function (_serviceConfig) {
        var categoryConfig = _serviceConfig.get('configCategories');
        if (categoryConfig.someProperty('name', selectedComponentName)) {
          configs = categoryConfig.findProperty('name', selectedComponentName).get('slaveConfigs');
        }
      }, this);
    }
    return configs;
  }.property('selectedComponentName', 'stepConfigs.@each.configCategories', 'stepConfigs.@each.configCategories.@each.slaveConfigs'),

  hosts: function () {
    if (this.get('selectedSlaveComponent')) {
      return this.get('selectedSlaveComponent').get('hosts');
    }
  }.property('selectedSlaveComponent'),

  groups: function () {
    var hosts = this.get('hosts');
    if(hosts){
      return hosts.mapProperty('group').uniq();
    }
    return [];
  }.property('hosts'),

  componentGroups: function () {
    var component = this.get('selectedSlaveComponent');
    if (component && component.get('groups')) {
      return component.get('groups');
    }
    return [];
  }.property('selectedSlaveComponent', 'selectedSlaveComponent.groups', 'stepConfigs.@each.configCategories.@each.slaveConfigs.groups.@each.properties.@each.value'),


  getGroupsForDropDown: function () {
    return this.get('componentGroups').getEach('name');
  }.property('selectedComponentName', 'componentGroups.@each.name'),

  activeGroup: function () {
    var componentGroups = this.get('componentGroups');
    if (componentGroups) {
      var active = componentGroups.findProperty('active', true);
      if (active){
        return active;
      }
    }
    return null;
  }.property('componentGroups.@each.active', 'componentGroups.@each.name', 'componentGroups.@each.properties.@each.value'),


  /**
   * Show slave hosts to groups popup
   * @param event
   */
  showAddSlaveComponentGroup: function (event) {
    var componentName = event.context;
    var component = this.get('selectedSlaveComponent');
    App.ModalPopup.show({
      header: componentName + Em.I18n.t('installer.controls.slaveComponentGroups'),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.slaveComponentGroupsController',
        header: Em.I18n.t('installer.slaveComponentHostsPopup.header').format(this.get('selectedComponentDisplayName')),
        templateName: require('templates/wizard/slave_component_hosts_popup')
      }),
      onPrimary: function (event) {
        if (component.tempSelectedGroups && component.tempSelectedGroups.length) {
          component.tempSelectedGroups.forEach(function (item) {
            var changed = component.get('hosts').filterProperty('hostName', item.hostName);
            changed.setEach('group', item.groupName);
          })
        }
        delete component.tempSelectedGroups;
        this.hide();
      },
      onSecondary: function (event) {
        delete component.tempSelectedGroups;
        this.hide();
      },
      onClose: function (event) {
        delete component.tempSelectedGroups;
        this.hide();
      }
    });
  },

  /**
   * Utility method. Save temporary info about changes in <code>slave hosts to groups</code> popup
   * @param host
   * @param groupName
   */
  changeHostGroup: function (host, groupName) {
    var component = this.get('selectedSlaveComponent');
    if (component.tempSelectedGroups === undefined) {
      component.tempSelectedGroups = [];
    }
    var values = component.tempSelectedGroups.filterProperty('hostName', host.hostName);
    if (values.length === 0)
      component.tempSelectedGroups.pushObject({hostName: host.hostName, groupName: groupName});
    else
      values.setEach('groupName', groupName);

  },

  /**
   * add new group to component(click on button)
   */
  addSlaveComponentGroup: function () {
    var component = this.get('selectedSlaveComponent');
    var newGroupName = 'New Group';
    component.get('groups').setEach('active', false);
    var newGroups = component.get('groups').filterProperty('name', newGroupName);
    if (newGroups.length === 0){
      component.newGroupIndex = 0;
    }
    else {
      component.newGroupIndex = component.newGroupIndex || 0;
      this.checkGroupName();
      newGroupName = 'New Group ' + component.newGroupIndex;
    }
    var newGroup = {name: newGroupName, index: component.newGroupIndex, type: 'new', active: true, properties: this.componentProperties(App.router.get('wizardStep7Controller.selectedService.serviceName'))};
    component.groups.pushObject(App.Group.create(newGroup));
    $('.remove-group-error').hide();
  },

  checkGroupName: function () {
    var component = this.get('selectedSlaveComponent');
    component.newGroupIndex++;
    var newGroupName = 'New Group ' + component.newGroupIndex;
    var groups = component.get('groups').filterProperty('name', newGroupName);
    if (groups.length !== 0) {
      this.checkGroupName();
    }
  },

  /**
   * Onclick handler for <code>choose hosts for slave group</code> link
   * @param event
   */
  showEditSlaveComponentGroups: function (event) {
    this.showAddSlaveComponentGroup(event);
  },

  getHostsByGroup: function (group) {
    var hosts = this.get('hosts');
    if(hosts){
      return hosts.filterProperty('group', group.name);
    }
    return [];
  },

  /**
   * Change tab
   * @param event
   */
  showSlaveComponentGroup: function (event) {
    var component = this.get('selectedSlaveComponent');
    if(!component.groups){

    }
    component.get('groups').setEach('active', false);
    var group = component.get('groups').filterProperty('name', event.context.name);
    group.setEach('active', true);
    var assignedHosts = component.get('hosts').filterProperty('group', event.context.name);
    if (assignedHosts.length === 0) {
      $('.remove-group-error').hide();
    }
  },

  /**
   * Remove tab
   * @param event
   */
  removeSlaveComponentGroup: function (event) {
    var group = event.context;
    var component = this.get('selectedSlaveComponent');
    var assignedHosts = component.get('hosts').filterProperty('group', group.name);
    if (assignedHosts.length !== 0) {
      $('.remove-group-error').show();
    } else {
      $('.remove-group-error').hide();
      var key = component.groups.indexOf(group);
      component.groups.removeObject(component.groups[key]);

      var newGroups = component.groups.filterProperty('type', 'new');
      if (newGroups.length == 0)
        component.newGroupIndex = 0;
      else {
        var lastNewGroup = newGroups[newGroups.length - 1];
        component.newGroupIndex = lastNewGroup.index;
      }
      if (group.active) {
        var lastGroup;
        if (key === component.groups.length)
          lastGroup = component.groups.slice(key - 1, key);
        else lastGroup = component.groups.slice(key, key + 1);
        lastGroup.setEach('active', true);
      }
    }
  },

  /**
   * change group name of slave component
   * @param group
   * @param newGroupName
   * @return {Boolean}
   */
  changeSlaveGroupName: function (group, newGroupName) {
    var component = this.get('selectedSlaveComponent');
    var isExist = component.get('groups').filterProperty('name', newGroupName);
    if (isExist.length !== 0)
      return true;
    else {
      var assignedHosts = component.get('hosts').filterProperty('group', group.name);
      if (assignedHosts.length !== 0){
        assignedHosts.setEach('group', newGroupName);
      }
      var groupFilter = component.get('groups').filterProperty('name', group.name);
      groupFilter.setEach('name', newGroupName);
    }
    return false;
  }

});
