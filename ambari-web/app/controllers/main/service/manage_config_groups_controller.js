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

  resortConfigGroup: function() {
    var configGroups = Ember.copy(this.get('configGroups'));
    if(configGroups.length < 2){
      return;
    }
    var defaultConfigGroup = configGroups.findProperty('isDefault');
    configGroups.removeObject(defaultConfigGroup);
    var sorted = configGroups.sort(function(configGroupA, configGroupB){
      return String(configGroupA.get('name')) >= String(configGroupB.get('name'));
    });
    sorted = [defaultConfigGroup].concat(sorted);

    this.removeObserver('configGroups.@each.name', this, 'resortConfigGroup');
    this.set('configGroups', sorted);
    this.addObserver('configGroups.@each.name', this, 'resortConfigGroup');
  }.observes('configGroups.@each.name'),

  loadConfigGroups: function (serviceName) {
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
  },

  onLoadConfigGroupsSuccess: function (data) {
    var loadedHostsToGroupMap = this.get('loadedHostsToGroupMap');
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
      }, this);
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
    var properies = this.get('selectedConfigGroup.propertiesList').htmlSafe();
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
      var defaultHosts = group.get('parentConfigGroup.hosts');
      var configGroupHosts = group.get('hosts');
      selectedHosts.forEach(function (hostName) {
        configGroupHosts.pushObject(hostName);
        defaultHosts.removeObject(hostName);
      });
    }
  },

  /**
   * delete hosts from group
   */
  deleteHosts: function () {
    if (this.get('isDeleteHostsDisabled')) {
      return false;
    }
    var groupHosts = this.get('selectedConfigGroup.hosts');
    var defaultGroupHosts = this.get('selectedConfigGroup.parentConfigGroup.hosts');
    this.get('selectedHosts').slice().forEach(function (hostName) {
      defaultGroupHosts.pushObject(hostName);
      groupHosts.removeObject(hostName);
    });
    this.set('selectedHosts', []);
  },

  isDeleteHostsDisabled: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (selectedConfigGroup) {
      if (selectedConfigGroup.isDefault || this.get('selectedHosts').length === 0) {
        return true;
      } else {
        return false;
      }
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
    if(this.get('selectedConfigGroup.isDefault')) {
      return;
    }
    var content = this;
    var self = this;
    this.renameGroupPopup = App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('services.service.config_groups.rename_config_group_popup.header'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/service/new_config_group')
      }),
      configGroupName: "",
      configGroupDesc: "",
      content: content,
      isDescriptionDirty: false,
      validate: function () {
        var warningMessage = '';
        var originalGroup = self.get('selectedConfigGroup');
        if (originalGroup.get('description') !== this.get('configGroupDesc') && !this.get('isDescriptionDirty')) {
          this.set('isDescriptionDirty', true);
        }
        if (originalGroup.get('name') === this.get('configGroupName')) {
          if (this.get('isDescriptionDirty')) {
            warningMessage = '';
          } else {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        } else {
          if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName'))) {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName', 'configGroupDesc'),
      enablePrimary: function () {
        return this.get('configGroupName').length > 0 && !this.get('warningMessage');
      }.property('warningMessage', 'configGroupName', 'configGroupDesc'),
      onPrimary: function () {
        if (!this.get('enablePrimary')) {
          return false;
        }
        var copyHsots = this.get('content.loadedHostsToGroupMap')[this.get('content.selectedConfigGroup.name')];
        delete this.get('content.loadedHostsToGroupMap')[this.get('content.selectedConfigGroup.name')];
        this.get('content.loadedHostsToGroupMap')[this.get('configGroupName')] = copyHsots;
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
      }
    });
    this.get('renameGroupPopup').set('configGroupName', this.get('selectedConfigGroup.name'));
    this.get('renameGroupPopup').set('configGroupDesc', this.get('selectedConfigGroup.description'));
  },

  /**
   * add new config group
   */
  addConfigGroup: function (isDuplicated) {
    isDuplicated = isDuplicated === true ? true : false;
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
      validate: function () {
        var warningMessage = '';
        if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName').trim())) {
          warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName'),
      enablePrimary: function () {
        return this.get('configGroupName').trim().length > 0 && !this.get('warningMessage');
      }.property('warningMessage', 'configGroupName'),
      onPrimary: function () {
        if (!this.get('enablePrimary')) {
          return false;
        }
        this.get('content').set('configGroupName', this.get('configGroupName').trim());
        this.get('content').set('configGroupDesc', this.get('configGroupDesc'));
        var desiredConfig = [];
         if (isDuplicated) {
          this.get('content.selectedConfigGroup.apiResponse.desired_configs').forEach(function(desired_config){
            var properties = {};
            this.get('content.selectedConfigGroup.properties').forEach(function(property){
              properties[property.name] = property.value;
            });
            desiredConfig.push({
              tag: 'version' + (new Date).getTime(),
              type: desired_config.type,
              properties : properties
            })
          }, this);
        }
        self.createNewConfigurationGroup(this.get('configGroupName').trim(),this.get('content.serviceName'),this.get('configGroupDesc'), desiredConfig, this.get('content'));
      }
    });
  },


  createNewConfigurationGroup: function(configGroupName, serviceName, configGroupDesc, desiredConfigs, sender) {
    App.ajax.send({
      name: 'config_groups.create',
      sender: sender,
      data: {
        'group_name': configGroupName,
        'service_id': serviceName,
        'description': configGroupDesc,
        'desired_configs': desiredConfigs
      },
      success: 'onAddNewConfigGroup',
      error: 'onAddNewConfigGroupError'
    });
  },
  /**
   * On successful api resonse for creating new config group
   */
  onAddNewConfigGroup: function (data,response) {
    var defaultConfigGroup = this.get('configGroups').findProperty('isDefault');
    var desiredConfigs = jQuery.parseJSON(response.data)[0].ConfigGroup.desired_configs;
    var properties = [];
    var configSiteTags = [];
    if (desiredConfigs && desiredConfigs.length > 0) {
      desiredConfigs.forEach(function(configs){
        var configSiteTag = App.ConfigSiteTag.create({
          site: configs.type,
          tag: configs.tag
        });
        configSiteTags.push(configSiteTag);
      });
      properties = this.get('selectedConfigGroup.properties');
    }

    var newConfigGroupData = App.ConfigGroup.create({
      id: data.resources[0].ConfigGroup.id,
      name: this.get('configGroupName'),
      description: this.get('configGroupDesc'),
      isDefault: false,
      parentConfigGroup: defaultConfigGroup,
      service: App.Service.find().findProperty('serviceName', this.get('serviceName')),
      hosts: [],
      configSiteTags: configSiteTags,
      properties: properties
    });
    this.get('loadedHostsToGroupMap')[newConfigGroupData.get('name')] = [];
    defaultConfigGroup.get('childConfigGroups').push(newConfigGroupData);
    this.get('configGroups').pushObject(newConfigGroupData);
    this.updateConfigGroup(data.resources[0].ConfigGroup.id);
    this.addGroupPopup.hide();
  },

  onAddNewConfigGroupError: function() {
    console.warn('Can\'t add configuration group');
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
    if(data.ConfigGroup) {
      var confGroup = this.get('configGroups').findProperty('id', data.ConfigGroup.id);
      confGroup.set('apiResponse', data.ConfigGroup);
    }
  },

  /**
   * duplicate config group
   */
  duplicateConfigGroup: function() {
    if(this.get('selectedConfigGroup.isDefault')) {
      return;
    }
    this.addConfigGroup(true);
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


App.InstallerManageConfigGroupsController = App.ManageConfigGroupsController.extend({
  name: 'installerManageConfigGroupsController',

  loadConfigGroups: function (serviceName) {
    this.set('serviceName', serviceName);
    var loadedHostsToGroupMap = this.get('loadedHostsToGroupMap');
    var configGroups = App.router.get('wizardStep7Controller.selectedService.configGroups');
    configGroups.forEach(function (configGroup) {
      if (!configGroup.get('isDefault')) {
        loadedHostsToGroupMap[configGroup.name] = configGroup.hosts.slice();
      }
    });
    this.set('configGroups', configGroups);
    this.set('isLoaded', true);
  },
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
    delete this.get('loadedHostsToGroupMap')[selectedConfigGroup.get('name')];
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
        if (originalGroup.get('name') === this.get('configGroupName')) {
          if (this.get('isDescriptionDirty')) {
            warningMessage = '';
          } else {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        } else {
          if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName'))) {
            warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
          }
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName', 'configGroupDesc'),
      enablePrimary: function () {
        return this.get('configGroupName').length > 0 && !this.get('warningMessage');
      }.property('warningMessage', 'configGroupName', 'configGroupDesc'),
      onPrimary: function () {
        if (!this.get('enablePrimary')) {
          return false;
        }
        var copyHsots = self.get('loadedHostsToGroupMap')[self.get('selectedConfigGroup.name')];
        delete self.get('loadedHostsToGroupMap')[self.get('selectedConfigGroup.name')];
        self.get('loadedHostsToGroupMap')[this.get('configGroupName')] = copyHsots;
        self.set('selectedConfigGroup.name', this.get('configGroupName'));
        self.set('selectedConfigGroup.description', this.get('configGroupDesc'));
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
      validate: function () {
        var warningMessage = '';
        if (self.get('configGroups').mapProperty('name').contains(this.get('configGroupName'))) {
          warningMessage = Em.I18n.t("config.group.selection.dialog.err.name.exists");
        }
        this.set('warningMessage', warningMessage);
      }.observes('configGroupName'),
      enablePrimary: function () {
        return this.get('configGroupName').length > 0 && !this.get('warningMessage');
      }.property('warningMessage', 'configGroupName'),
      onPrimary: function () {
        if (!this.get('enablePrimary')) {
          return false;
        }
        var defaultConfigGroup = self.get('configGroups').findProperty('isDefault');
        var properties = [];
        var newConfigGroupData = App.ConfigGroup.create({
          id: null,
          name: this.get('configGroupName'),
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
            var property = $.extend(true, {},property);
            property.set('group', newConfigGroupData);
            properties.push(App.ServiceConfigProperty.create(property));
          });
          newConfigGroupData.set('properties', properties);
        } else {
          newConfigGroupData.set('properties', []);
        }
        self.get('loadedHostsToGroupMap')[newConfigGroupData.get('name')] = [];
        self.get('configGroups').pushObject(newConfigGroupData);
        defaultConfigGroup.get('childConfigGroups').pushObject(newConfigGroupData);
        this.hide();
      }
    });
  },

  duplicateConfigGroup: function() {
    this.addConfigGroup(true);
  }
})
