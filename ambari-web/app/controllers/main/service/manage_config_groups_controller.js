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
var componentHelper = require('utils/component');
var serviceComponents = require('data/service_components');

App.ManageConfigGroupsController = Em.Controller.extend({
  name: 'manageConfigGroupsController',

  isLoaded: false,

  isInstaller: false,

  serviceName: null,

  configGroups: [],

  originalConfigGroups: [],

  selectedConfigGroup: null,

  selectedHosts: [],

  resortConfigGroup: function() {
    var configGroups = Ember.copy(this.get('configGroups'));
    if(configGroups.length < 2){
      return;
    }
    var defaultConfigGroup = configGroups.findProperty('isDefault');
    configGroups.removeObject(defaultConfigGroup);
    var sorted = [defaultConfigGroup].concat(configGroups.sortProperty('name'));

    this.removeObserver('configGroups.@each.name', this, 'resortConfigGroup');
    this.set('configGroups', sorted);
    this.addObserver('configGroups.@each.name', this, 'resortConfigGroup');
  }.observes('configGroups.@each.name'),

  loadConfigGroups: function (serviceName) {
    if (this.get('isInstaller')) {
      this.set('serviceName', serviceName);
      var configGroups = this.copyConfigGroups(App.router.get('wizardStep7Controller.selectedService.configGroups'));
      var originalConfigGroups = this.copyConfigGroups(configGroups);
      this.set('configGroups', configGroups);
      this.set('originalConfigGroups', originalConfigGroups);
      this.set('isLoaded', true);
    } else {
      this.set('serviceName', serviceName);
      App.ajax.send({
        name: 'service.load_config_groups',
        data: {
          serviceName: serviceName
        },
        sender: this,
        success: 'onLoadConfigGroupsSuccess',
        error: 'onLoadConfigGroupsError'
      });
    }
  },

  onLoadConfigGroupsSuccess: function (data) {
    var usedHosts = [];
    var unusedHosts = [];
    var serviceName = this.get('serviceName');
    var defaultConfigGroup = App.ConfigGroup.create({
      name: App.Service.DisplayNames[serviceName] + " Default",
      description: "Default cluster level " + this.get('serviceName') + " configuration",
      isDefault: true,
      parentConfigGroup: null,
      service: this.get('content'),
      configSiteTags: [],
      serviceName: serviceName
    });
    if (data && data.items) {
      var groupToTypeToTagMap = {};
      var configGroups = [];
      data.items.forEach(function (configGroup) {
        configGroup = configGroup.ConfigGroup;
        var hostNames = configGroup.hosts.mapProperty('host_name');
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
      }, this);
      unusedHosts = App.Host.find().mapProperty('hostName');
      usedHosts.uniq().forEach(function (host) {
        unusedHosts = unusedHosts.without(host);
      }, this);
      defaultConfigGroup.set('childConfigGroups', configGroups);
      defaultConfigGroup.set('hosts', unusedHosts);
      var allGroups = [defaultConfigGroup].concat(configGroups);
      this.set('configGroups', allGroups);
      var originalGroups = this.copyConfigGroups(allGroups);
      this.set('originalConfigGroups', originalGroups);
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
    var properies = this.get('selectedConfigGroup.propertiesList').htmlSafe();
    if (properies) {
      App.showAlertPopup(Em.I18n.t('services.service.config_groups_popup.properties'), properies);
    }
  },
  addHosts: function () {
    if (this.get('selectedConfigGroup.isAddHostsDisabled')){
      return false;
    }
    var availableHosts = this.get('selectedConfigGroup.availableHosts');
    var popupDescription = {
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message').format(App.Service.DisplayNames[this.get('serviceName')])
    };
    hostsManagement.launchHostsSelectionDialog(availableHosts, [], false, this.get('componentsForFilter'), this.addHostsCallback.bind(this), popupDescription);
  },

  /**
   * add hosts callback
   */
  addHostsCallback: function (selectedHosts) {
    var group = this.get('selectedConfigGroup');
    if (selectedHosts) {
      var defaultHosts = group.get('parentConfigGroup.hosts').slice();
      var configGroupHosts = group.get('hosts');
      selectedHosts.forEach(function (hostName) {
        configGroupHosts.pushObject(hostName);
        defaultHosts.removeObject(hostName);
      });
      group.set('parentConfigGroup.hosts', defaultHosts);
    }
  },

  /**
   * delete hosts from group
   */
  deleteHosts: function () {
    if (this.get('isDeleteHostsDisabled')) {
      return;
    }
    var groupHosts = this.get('selectedConfigGroup.hosts');
    var defaultGroupHosts = this.get('selectedConfigGroup.parentConfigGroup.hosts').slice();
    this.get('selectedHosts').slice().forEach(function (hostName) {
      defaultGroupHosts.pushObject(hostName);
      groupHosts.removeObject(hostName);
    });
    this.set('selectedConfigGroup.parentConfigGroup.hosts', defaultGroupHosts);
    this.set('selectedHosts', []);
  },

  isDeleteHostsDisabled: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (selectedConfigGroup) {
      return selectedConfigGroup.isDefault || this.get('selectedHosts').length === 0;
    }
    return true;
  }.property('selectedConfigGroup', 'selectedConfigGroup.hosts.length', 'selectedHosts.length'),

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
   * add hosts to group
   * @return {Array}
   */
  componentsForFilter: function () {
    return serviceComponents.filterProperty('service_name', this.get('serviceName')).map(function (component) {
      return Em.Object.create({
        displayName: component.display_name,
        componentName: component.isClient ? 'CLIENT' : component.component_name,
        selected: false
      });
    });
  }.property('serviceName'),

  /**
   * delete selected config group
   */
  deleteConfigGroup: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (this.get('isDeleteGroupDisabled')) {
      return;
    }
    //move hosts of group to default group (available hosts)
    this.set('selectedHosts', selectedConfigGroup.get('hosts'));
    this.deleteHosts();
    this.get('configGroups').removeObject(selectedConfigGroup);
    this.set('selectedConfigGroup', this.get('configGroups').findProperty('isDefault'));
  },
  /**
   * rename new config group
   */
  renameConfigGroup: function () {
    if(this.get('selectedConfigGroup.isDefault')) {
      return;
    }
    var self = this;
    this.renameGroupPopup = App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('services.service.config_groups.rename_config_group_popup.header'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),
      configGroupName: self.get('selectedConfigGroup.name'),
      configGroupDesc: self.get('selectedConfigGroup.description'),
      warningMessage: '',
      isDescriptionDirty: false,
      validate: function () {
        var warningMessage = '';
        var originalGroup = self.get('selectedConfigGroup');
        if (originalGroup.get('description') !== this.get('configGroupDesc') && !this.get('isDescriptionDirty')) {
          this.set('isDescriptionDirty', true);
        }
        if (originalGroup.get('name').trim() === this.get('configGroupName').trim()) {
          if (this.get('isDescriptionDirty')) {
            warningMessage = '';
          } else {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        } else {
          if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName').trim())) {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName', 'configGroupDesc'),
      disablePrimary: function () {
        return !(this.get('configGroupName').trim().length > 0 && !this.get('warningMessage'));
      }.property('warningMessage', 'configGroupName', 'configGroupDesc'),
      onPrimary: function () {
        self.set('selectedConfigGroup.name', this.get('configGroupName'));
        self.set('selectedConfigGroup.description', this.get('configGroupDesc'));
        self.get('selectedConfigGroup.properties').forEach(function(property){
          property.set('group', self.get('selectedConfigGroup'));
        });
        this.hide();
      }
    });
    this.get('renameGroupPopup').validate();
  },

  /**
   * add new config group
   */
  addConfigGroup: function (duplicated) {
    duplicated = (duplicated === true);
    var self = this;
    this.addGroupPopup = App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('services.service.config_groups.add_config_group_popup.header'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),
      configGroupName: duplicated ? self.get('selectedConfigGroup.name') + ' Copy' : "",
      configGroupDesc: duplicated ? self.get('selectedConfigGroup.description') + ' (Copy)' : "",
      warningMessage: '',
      didInsertElement: function(){
        this.validate();
      },
      validate: function () {
        var warningMessage = '';
        if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName').trim())) {
          warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName'),
      disablePrimary: function () {
        return !(this.get('configGroupName').trim().length > 0 && !this.get('warningMessage'));
      }.property('warningMessage', 'configGroupName'),
      onPrimary: function () {
        var defaultConfigGroup = self.get('configGroups').findProperty('isDefault');
        var properties = [];
        var newConfigGroupData = App.ConfigGroup.create({
          id: null,
          name: this.get('configGroupName').trim(),
          description: this.get('configGroupDesc'),
          isDefault: false,
          parentConfigGroup: defaultConfigGroup,
          service: Em.Object.create({id: self.get('serviceName')}),
          hosts: [],
          configSiteTags: [],
          properties: []
        });
        if (duplicated) {
          self.get('selectedConfigGroup.properties').forEach(function(property) {
            var property = App.ServiceConfigProperty.create($.extend(false, {}, property));
            property.set('group', newConfigGroupData);
            properties.push(property);
          });
          newConfigGroupData.set('properties', properties);
        } else {
          newConfigGroupData.set('properties', []);
        }
        self.get('configGroups').pushObject(newConfigGroupData);
        defaultConfigGroup.get('childConfigGroups').pushObject(newConfigGroupData);
        this.hide();
      }
    });
  },

  duplicateConfigGroup: function() {
    this.addConfigGroup(true);
  },

  hostsModifiedConfigGroups: function () {
    if (!this.get('isLoaded')) {
      return false;
    }
    var groupsToClearHosts = [];
    var groupsToDelete = [];
    var groupsToSetHosts = [];
    var groupsToCreate = [];
    var groups = this.get('configGroups');
    var originalGroups = this.get('originalConfigGroups');
    var originalGroupsNames = originalGroups.mapProperty('name').without(originalGroups.findProperty('isDefault').get('name'));
    groups.forEach(function (group) {
      if (!group.get('isDefault')) {
        var originalGroup = originalGroups.findProperty('name', group.get('name'));
        if (originalGroup) {
          if (!(JSON.stringify(group.get('hosts').slice().sort()) === JSON.stringify(originalGroup.get('hosts').sort()))) {
            groupsToClearHosts.push(group.set('id', originalGroup.get('id')));
            if (group.get('hosts').length) {
              groupsToSetHosts.push(group.set('id', originalGroup.get('id')));
            }
          } else if (group.get('description') !== originalGroup.get('description')) {
            groupsToSetHosts.push(group.set('id', originalGroup.get('id')));
          }
          originalGroupsNames = originalGroupsNames.without(group.get('name'));
        } else {
          groupsToCreate.push(group);
        }
      }
    });
    originalGroupsNames.forEach(function (groupName) {
      groupsToDelete.push(originalGroups.findProperty('name', groupName));
    }, this);
    return {
      toClearHosts: groupsToClearHosts,
      toDelete: groupsToDelete,
      toSetHosts: groupsToSetHosts,
      toCreate: groupsToCreate
    };
  }.property('selectedConfigGroup.hosts.@each', 'selectedConfigGroup.hosts.length', 'selectedConfigGroup.description', 'configGroups', 'isLoaded'),

  isHostsModified: function () {
    var modifiedGroups = this.get('hostsModifiedConfigGroups');
    if (!this.get('isLoaded')) {
      return false;
    }
    return !!(modifiedGroups.toClearHosts.length || modifiedGroups.toSetHosts.length || modifiedGroups.toCreate.length || modifiedGroups.toDelete.length);
  }.property('hostsModifiedConfigGroups'),

  /**
   * copy config groups to manage popup to give user choice whether or not save changes
   * @param originGroups
   * @return {Array}
   */
  copyConfigGroups: function (originGroups) {
    var configGroups = [];
    var result = [];
    var defaultConfigGroup = App.ConfigGroup.create($.extend(true, {}, originGroups.findProperty('isDefault')));
    originGroups.forEach(function (configGroup) {
      if (!configGroup.get('isDefault')) {
        var copiedGroup = App.ConfigGroup.create($.extend(true, {}, configGroup));
        copiedGroup.set('parentConfigGroup', defaultConfigGroup);
        configGroups.pushObject(copiedGroup);
      }
    });
    defaultConfigGroup.set('childConfigGroups', configGroups.slice());
    configGroups.pushObject(defaultConfigGroup);
    configGroups.forEach(function (group) {
      var groupCopy = {};
      for (var prop in group) {
        if (group.hasOwnProperty(prop)) {
          groupCopy[prop] = group[prop];
        }
      }
      groupCopy.properties.forEach(function(property){
        property.set('group', group);
      });
      result.push(App.ConfigGroup.create(groupCopy));
    }, this);
    return result;
  }
});
