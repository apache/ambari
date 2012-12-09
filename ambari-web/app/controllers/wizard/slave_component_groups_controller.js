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
/**
 * Used to manage slave component config. User could create different settings for separate group
 * @type {*}
 */
App.SlaveComponentGroupsController = Em.ArrayController.extend({

  name: 'slaveComponentGroupsController',

  contentBinding: 'App.router.wizardStep7Controller.slaveComponentHosts',

  serviceBinding: 'App.router.wizardStep7Controller.selectedService',

  selectedComponentName: function () {
    switch (App.router.get('wizardStep7Controller.selectedService.serviceName')) {
      case 'HDFS':
        return 'DATANODE';
      case 'MAPREDUCE':
        return 'TASKTRACKER';
      case 'HBASE':
        return 'HBASE_REGIONSERVER';
      default:
        return null;
    }

  }.property('App.router.wizardStep7Controller.selectedService'),

  selectedComponentDisplayName: function() {
    return App.format.role(this.get('selectedComponentName'));
  }.property('selectedComponentName'),

  selectedSlaveComponent: function () {
    var selectedComponentName = this.get('selectedComponentName');
    if (selectedComponentName) {
      return this.findProperty('componentName', selectedComponentName);
    }
  }.property('selectedComponentName'),

  hosts: function () {
    var selectedComponentName = this.get('selectedComponentName');
    if (selectedComponentName) {
      var component = this.findProperty('componentName', selectedComponentName);
      if(component){
        return component.hosts;
      }
    }
  }.property('@each.hosts', 'selectedComponentName'),

  groups: function () {
    var hosts = this.get('hosts');
    if(hosts){
      return hosts.mapProperty('group').uniq();
    }
  }.property('hosts'),

  componentGroups: function () {
    var component = this.get('selectedSlaveComponent');
    if(component){
        if (!component.groups) {
          component.groups = [];
          var defaultGroup = {name: 'Default', index: 'default', type: 'default', active: true};
          component.groups.pushObject(defaultGroup);
        }
        return component.groups;
    }
    return [];
  }.property('selectedSlaveComponent'),

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
  }.property('componentGroups.@each.active', 'componentGroups.@each.name'),


  /**
   * Show slave hosts to groups popup
   * @param event
   */
  showAddSlaveComponentGroup: function (event) {
    var componentName = event.context;
    var component = this.get('selectedSlaveComponent');
    App.ModalPopup.show({
      header: componentName + ' Groups',
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.slaveComponentGroupsController',
        templateName: require('templates/wizard/slave_component_hosts_popup')
      }),
      onPrimary: function (event) {
        if (component.tempSelectedGroups && component.tempSelectedGroups.length) {
          component.tempSelectedGroups.forEach(function (item) {
            var changed = component.hosts.filterProperty('hostName', item.hostName);
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
    component.groups.setEach('active', false);
    var newGroups = component.groups.filterProperty('name', newGroupName);
    if (newGroups.length === 0){
      component.newGroupIndex = 0;
    }
    else {
      component.newGroupIndex = component.newGroupIndex || 0;
      this.checkGroupName();
      newGroupName = 'New Group ' + component.newGroupIndex;
    }
    var newGroup = {name: newGroupName, index: component.newGroupIndex, type: 'new', active: true};
    component.groups.pushObject(newGroup);
    $('.remove-group-error').hide();
  },

  checkGroupName: function () {
    var component = this.get('selectedSlaveComponent');
    component.newGroupIndex++;
    var newGroupName = 'New Group ' + component.newGroupIndex;
    var groups = component.groups.filterProperty('name', newGroupName);
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
  },

  /**
   * Change tab
   * @param event
   */
  showSlaveComponentGroup: function (event) {
    var component = this.get('selectedSlaveComponent');
    if(!component.groups){
      debugger;
    }
    component.groups.setEach('active', false);
    var group = component.groups.filterProperty('name', event.context.name);
    group.setEach('active', true);
    var assignedHosts = component.hosts.filterProperty('group', event.context.name);
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
    var assignedHosts = component.hosts.filterProperty('group', group.name);
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
    var isExist = component.groups.filterProperty('name', newGroupName);
    if (isExist.length !== 0)
      return true;
    else {
      var assignedHosts = component.hosts.filterProperty('group', group.name);
      if (assignedHosts.length !== 0){
        assignedHosts.setEach('group', newGroupName);
      }
      var groupFilter = component.groups.filterProperty('name', group.name);
      groupFilter.setEach('name', newGroupName);
    }
    return false;
  }

});
