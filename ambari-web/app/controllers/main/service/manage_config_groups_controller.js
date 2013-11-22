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
var hostsManagement = require('utils/hosts');
var serviceComponents = require('data/service_components');

App.ManageConfigGroupsController = Em.Controller.extend({
  name: 'manageConfigGroupsController',

  isLoaded: false,

  serviceName: null,

  configGroups: [],

  selectedConfigGroup: null,

  selectedHosts: [],

  loadedHostsToGroupMap: {},

  allConfigGroupsNames: [],

  loadConfigGroups: function (serviceName) {
    this.set('serviceName', serviceName);
    App.ajax.send({
      name: 'service.load_config_groups',
      sender: this,
      success: 'onLoadConfigGroupsSuccess',
      error: 'onLoadConfigGroupsError'
    });
  },

  onLoadConfigGroupsSuccess: function (data) {
    var loadedHostsToGroupMap = this.get('loadedHostsToGroupMap');
    var usedHosts = [];
    var unusedHosts = [];
    var defaultConfigGroup = App.ConfigGroup.create({
      name: "Default",
      description: "Default cluster level " + this.get('serviceName') + " configuration",
      isDefault: true,
      parentConfigGroup: null,
      service: this.get('content'),
      configSiteTags: []
    });
    if (data && data.items) {
      var groupToTypeToTagMap = {};
      var configGroups = [];
      var serviceName = this.get('serviceName');
      var allConfigGroupsNames = [];
      data.items.forEach(function (configGroup) {
        configGroup = configGroup.ConfigGroup;
        allConfigGroupsNames.push(configGroup.group_name);
        if (configGroup.tag === serviceName) {
          var hostNames = configGroup.hosts.mapProperty('host_name');
          loadedHostsToGroupMap[configGroup.group_name] = hostNames.slice();
          var newConfigGroup = App.ConfigGroup.create({
            id: configGroup.id,
            name: configGroup.group_name,
            description: configGroup.description,
            isDefault: false,
            parentConfigGroup: defaultConfigGroup,
            service: App.Service.find().findProperty('serviceName', configGroup.tag),
            hosts: hostNames,
            configSiteTags: [],
            properties: [],
            apiResponse: configGroup
          });
          usedHosts = usedHosts.concat(newConfigGroup.get('hosts'));
          configGroups.push(newConfigGroup);
          var newConfigGroupSiteTags = newConfigGroup.get('configSiteTags');
          configGroup.desired_configs.forEach(function (config) {
            newConfigGroupSiteTags.push(App.ConfigSiteTag.create({
              site: config.type,
              tag: config.tag
            }));
            if (!groupToTypeToTagMap[configGroup.group_name]) {
              groupToTypeToTagMap[configGroup.group_name] = {}
            }
            groupToTypeToTagMap[configGroup.group_name][config.type] = config.tag;
          });
        }
      }, this);
      this.set('allConfigGroupsNames', allConfigGroupsNames);
      unusedHosts = App.Host.find().mapProperty('hostName');
      usedHosts.uniq().forEach(function (host) {
        unusedHosts = unusedHosts.without(host);
      }, this);
      defaultConfigGroup.set('childConfigGroups', configGroups);
      defaultConfigGroup.set('hosts', unusedHosts);
      this.set('configGroups', [defaultConfigGroup].concat(configGroups));
      this.loadProperties(groupToTypeToTagMap);
      this.set('isLoaded', true);
    }
  },

  onLoadConfigGroupsError: function () {
    console.error('Unable to load config groups for service.');
  },

  loadProperties: function (groupToTypeToTagMap) {
    var typeTagToGroupMap = {};
    var urlParams = [];
    for (var group in groupToTypeToTagMap) {
      var overrideTypeTags = groupToTypeToTagMap[group];
      for (var type in overrideTypeTags) {
        var tag = overrideTypeTags[type];
        typeTagToGroupMap[type + "///" + tag] = group;
        urlParams.push('(type=' + type + '&tag=' + tag + ')');
      }
    }
    var params = urlParams.join('|');
    if (urlParams.length) {
      App.ajax.send({
        name: 'config.host_overrides',
        sender: this,
        data: {
          params: params,
          typeTagToGroupMap: typeTagToGroupMap
        },
        success: 'onLoadPropertiesSuccess'
      });
    }
  },

  onLoadPropertiesSuccess: function (data, opt, params) {
    data.items.forEach(function (configs) {
      var typeTagConfigs = [];
      App.config.loadedConfigurationsCache[configs.type + "_" + configs.tag] = configs.properties;
      var group = params.typeTagToGroupMap[configs.type + "///" + configs.tag];
      for (var config in configs.properties) {
        typeTagConfigs.push({
          name: config,
          value: configs.properties[config]
        });
      }
      this.get('configGroups').findProperty('name', group).get('properties').pushObjects(typeTagConfigs);
    }, this);
  },

  showProperties: function () {
    var properies = this.get('selectedConfigGroup.propertiesList');
    if (properies) {
      App.showAlertPopup(Em.I18n.t('services.service.config_groups_popup.properties'), properies);
    }
  },
  /**
   * add hosts to group
   * @return {Array}
   */
  componentsForFilter: function() {
    var components = serviceComponents.filterProperty('service_name',this.get('serviceName'));
    return components.map(function(component) {
      return Em.Object.create({
        displayName: component.display_name,
        componentName: component.component_name,
        selected: false
      });
    });
  }.property('serviceName'),

  addHosts: function () {
    var availableHosts = this.get('selectedConfigGroup.availableHosts');
    var group = this.get('selectedConfigGroup');
    hostsManagement.launchHostsSelectionDialog(availableHosts, [], false, this.get('componentsForFilter'), function (selectedHosts) {
      if (selectedHosts) {
        var defaultHosts = group.get('parentConfigGroup.hosts');
        var configGroupHosts = group.get('hosts');
        selectedHosts.forEach(function (hostName) {
          configGroupHosts.pushObject(hostName);
          defaultHosts.removeObject(hostName);
        });
      }
    });
  },

  /**
   * delete hosts from group
   */
  deleteHosts: function () {
    var groupHosts = this.get('selectedConfigGroup.hosts');
    var defaultGroupHosts = this.get('selectedConfigGroup.parentConfigGroup.hosts');
    this.get('selectedHosts').slice().forEach(function (hostName) {
      defaultGroupHosts.pushObject(hostName);
      groupHosts.removeObject(hostName);
    });
    this.set('selectedHosts', []);
  },

  /**
   * confirm delete config group
   */
  confirmDelete : function () {
    var self = this;
    App.showConfirmationPopup(function() {
      self.deleteConfigGroup();
    });
  },

  /**
   * delete selected config group
   */
  deleteConfigGroup: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (this.get('isDeleteGroupDisabled')) {
      return;
    }
    App.ajax.send({
      name: 'config_groups.delete_config_group',
      sender: this,
      data: {
        id: selectedConfigGroup.get('id')
      }
    });
    //move hosts of group to default group (available hosts)
    this.set('selectedHosts', selectedConfigGroup.get('hosts'));
    this.deleteHosts();
    this.get('configGroups').removeObject(selectedConfigGroup);
    delete this.get('loadedHostsToGroupMap')[selectedConfigGroup.get('name')];
    this.set('selectedConfigGroup', this.get('configGroups').findProperty('isDefault'));
  },

  /**
   * rename new config group
   */
  renameConfigGroup: function () {
    if(this.get('selectedConfigGroup.name') == "Default") {
      return;
    }
    var content = this;
    this.renameGroupPopup = App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('services.service.config_groups.rename_config_group_popup.header'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),
      configGroupName: "",
      content: content,
      onPrimary: function () {
        this.get('content.selectedConfigGroup').set('name', this.get('configGroupName'));
        this.get('content.selectedConfigGroup').set('description', this.get('configGroupDesc'));
        this.get('content.selectedConfigGroup.apiResponse').group_name = this.get('configGroupName');
        this.get('content.selectedConfigGroup.apiResponse').description = this.get('configGroupDesc');
        var configGroup = {
          ConfigGroup: this.get('content.selectedConfigGroup.apiResponse')
        };
        App.ajax.send({
          name: 'config_groups.update_config_group',
          sender: this,
          data: {
            id: this.get('content.selectedConfigGroup.id'),
            configGroup: configGroup
          }
        });
        this.hide();
      },
      onSecondary: function () {
        this.hide();
      }
    });
    this.get('renameGroupPopup').set('configGroupName', this.get('selectedConfigGroup.name'));
    this.get('renameGroupPopup').set('configGroupDesc', this.get('selectedConfigGroup.description'));
  },

  /**
   * add new config group
   */
  addConfigGroup: function () {
    var content = this;
    var self = this;
    this.addGroupPopup = App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('services.service.config_groups.add_config_group_popup.header'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),
      configGroupName: "",
      configGroupDesc: "",
      content: content,
      warningMessage: '',
      vaildate: function () {
        var warningMessage = '';
        if (self.get('allConfigGroupsNames').contains(this.get('configGroupName'))) {
          warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName'),
      enablePrimary: function () {
        return this.get('configGroupName').length > 0 && !this.get('warningMessage');
      }.property('warningMessage', 'configGroupName'),
      onPrimary: function () {
        this.get('content').set('configGroupName', this.get('configGroupName'));
        this.get('content').set('configGroupDesc', this.get('configGroupDesc'));
        App.ajax.send({
          name: 'config_groups.create',
          sender: this.get('content'),
          data: {
            'group_name': this.get('configGroupName'),
            'service_id': this.get('content.serviceName'),
            'description': this.get('configGroupDesc')
          },
          success: 'onAddNewConfigGroup'
        });
      },
      onSecondary: function () {
        this.hide();
      }
    });
  },

  /**
   * On successful api resonse for creating new config group
   */
  onAddNewConfigGroup: function (data) {
    var defaultConfigGroup = this.get('configGroups').findProperty('isDefault');
    var newConfigGroupData = App.ConfigGroup.create({
      id: data.resources[0].ConfigGroup.id,
      name: this.get('configGroupName'),
      description: this.get('configGroupDesc'),
      isDefault: false,
      parentConfigGroup: defaultConfigGroup,
      service: App.Service.find().findProperty('serviceName', this.get('serviceName')),
      hosts: [],
      configSiteTags: []
    });
    this.get('loadedHostsToGroupMap')[newConfigGroupData.get('name')] = [];
    defaultConfigGroup.get('childConfigGroups').push(newConfigGroupData);
    this.get('configGroups').pushObject(newConfigGroupData);
    this.updateConfigGroup(data.resources[0].ConfigGroup.id);
    this.addGroupPopup.hide();
  },

  /**
   * update config group apiResponse property
   */
  updateConfigGroup: function (id) {
    App.ajax.send({
      name: 'config_groups.get_config_group_by_id',
      sender: this,
      data: {
        'id': id
      },
      success: 'successLoadingConfigGroup'
    });
  },

  successLoadingConfigGroup: function (data) {
    var confGroup = this.get('configGroups').findProperty('id', data.ConfigGroup.id);
    confGroup.set('apiResponse', data.ConfigGroup);
  },

  /**
   * duplicate config group
   */
  duplicateConfigGroup: function() {
    this.addConfigGroup();
    this.get('addGroupPopup').set('header',Em.I18n.t('services.service.config_groups.duplicate_config_group_popup.header'));
    this.get('addGroupPopup').set('configGroupName', this.get('selectedConfigGroup.name') + ' Copy');
    this.get('addGroupPopup').set('configGroupDesc', this.get('selectedConfigGroup.description') + ' (Copy)');
  },

  hostsModifiedConfigGroups: function () {
    var groupsToClearHosts = [];
    var groupsToSetHosts = [];
    var groups = this.get('configGroups');
    var loadedHostsToGroupMap = this.get('loadedHostsToGroupMap');
    groups.forEach(function (group) {
      if (!group.get('isDefault')) {
        if (!(JSON.stringify(group.get('hosts').slice().sort()) === JSON.stringify(loadedHostsToGroupMap[group.get('name')].sort()))) {
          groupsToClearHosts.push(group);
          if (group.get('hosts').length) {
            groupsToSetHosts.push(group);
          }
        }
      }
    });
    return {
      toClearHosts: groupsToClearHosts,
      toSetHosts: groupsToSetHosts
    };
  }.property('selectedConfigGroup', 'selectedConfigGroup.hosts.@each'),
  
  isHostsModified: function () {
    var modifiedGroups = this.get('hostsModifiedConfigGroups');
    return !!(modifiedGroups.toClearHosts.length || modifiedGroups.toSetHosts.length);
  }.property('hostsModifiedConfigGroups', 'hostsModifiedConfigGroups.length')
});
