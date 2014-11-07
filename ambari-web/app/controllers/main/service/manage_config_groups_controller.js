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
var numberUtils = require('utils/number_utils');

App.ManageConfigGroupsController = Em.Controller.extend({
  name: 'manageConfigGroupsController',

  isLoaded: false,

  isInstaller: false,

  isAddService: false,

  serviceName: null,

  configGroups: [],

  originalConfigGroups: [],

  selectedConfigGroup: null,

  selectedHosts: [],

  clusterHosts: [],

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

  loadHosts: function() {
    this.set('isLoaded', false);
    if (this.get('isInstaller')) {
      var allHosts = this.get('isAddService') ? App.router.get('addServiceController').get('allHosts') : App.router.get('installerController').get('allHosts');
      this.set('clusterHosts', allHosts);
      this.loadConfigGroups(this.get('serviceName'));
    } else {
      this.loadHostsFromServer();
    }
  },

  /**
   * request all hosts directly from server
   */
  loadHostsFromServer: function() {
    App.ajax.send({
      name: 'hosts.config_groups',
      sender: this,
      data: {},
      success: 'loadHostsFromServerSuccessCallback',
      error: 'loadHostsFromServerErrorCallback'
    });
  },

  /**
   * parse hosts response and wrap them into Ember.Object
   * @param data
   */
  loadHostsFromServerSuccessCallback: function (data) {
    var wrappedHosts = [];

    data.items.forEach(function (host) {
      var hostComponents = [];
      var diskInfo = host.Hosts.disk_info.filter(function(item) {
        return /^ext|^ntfs|^fat|^xfs/i.test(item.type);
      });
      if (diskInfo.length) {
        diskInfo = diskInfo.reduce(function(a, b) {
          return {
            available: parseInt(a.available) + parseInt(b.available),
            size: parseInt(a.size) + parseInt(b.size)
          };
        });
      }
      host.host_components.forEach(function (hostComponent) {
        hostComponents.push(Em.Object.create({
          componentName: hostComponent.HostRoles.component_name,
          displayName: App.format.role(hostComponent.HostRoles.component_name)
        }));
      }, this);
      wrappedHosts.pushObject(Em.Object.create({
          id: host.Hosts.host_name,
          ip: host.Hosts.ip,
          osType: host.Hosts.os_type,
          osArch: host.Hosts.os_arch,
          hostName: host.Hosts.host_name,
          publicHostName: host.Hosts.public_host_name,
          cpu: host.Hosts.cpu_count,
          memory: host.Hosts.total_mem,
          diskTotal: numberUtils.bytesToSize(diskInfo.size, 0, undefined, 1024),
          diskFree: numberUtils.bytesToSize(diskInfo.available, 0, undefined, 1024),
          disksMounted: host.Hosts.disk_info.length,
          hostComponents: hostComponents
        }
      ));
    }, this);

    this.set('clusterHosts', wrappedHosts);
    this.loadConfigGroups(this.get('serviceName'));
  },

  loadHostsFromServerErrorCallback: function () {
    console.warn('ERROR: request to fetch all hosts failed');
    this.set('clusterHosts', []);
    this.loadConfigGroups(this.get('serviceName'));
  },

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
    var serviceDisplayName =  App.StackService.find().findProperty('serviceName', this.get('serviceName')).get('displayName');
    var defaultConfigGroup = App.ConfigGroup.create({
      name: serviceDisplayName + " Default",
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
        var publicHostNames = this.hostsToPublic(hostNames);
        var newConfigGroup = App.ConfigGroup.create({
          id: configGroup.id,
          name: configGroup.group_name,
          description: configGroup.description,
          isDefault: false,
          parentConfigGroup: defaultConfigGroup,
          service: App.Service.find().findProperty('serviceName', configGroup.tag),
          hosts: hostNames,
          publicHosts: publicHostNames,
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
      unusedHosts = this.get('clusterHosts').mapProperty('hostName');
      usedHosts.uniq().forEach(function (host) {
        unusedHosts = unusedHosts.without(host);
      }, this);
      defaultConfigGroup.set('childConfigGroups', configGroups);
      defaultConfigGroup.set('hosts', unusedHosts);
      defaultConfigGroup.set('publicHosts', this.hostsToPublic(unusedHosts));
      var allGroups = [defaultConfigGroup].concat(configGroups);
      this.set('configGroups', allGroups);
      var originalGroups = this.copyConfigGroups(allGroups);
      this.set('originalConfigGroups', originalGroups);
      this.loadProperties(groupToTypeToTagMap);
      this.set('isLoaded', true);
    }
  },
  /**
   * Get public_host_name by host_name.
   *
   * @param {Array|String} hostsList
   * @return {Array|String}
   **/
  hostsToPublic: function(hostsList) {
    return this.convertHostNames(hostsList, true);
  },
  /**
   * Get host_name by public_host_name
   *
   * @param {Array|String} hostsList
   * @return {Array|String}
   **/
  publicToHostName: function(hostsList) {
    return this.convertHostNames(hostsList, false);
  },
  /***
   * Switch between public_host_name and host_name
   *
   * @param {Array|String} hostsList
   * @param {Boolean} toPublic
   * @return {Array|String}
   **/
  convertHostNames: function(hostsList, toPublic) {
    var allHosts = this.get('clusterHosts');
    var convertTarget = !!toPublic ?
      { from: 'hostName', to: 'publicHostName' } : { from: 'publicHostName', to: 'hostName'};
    if (this.get('isInstaller')) {
      allHosts = App.router.get(!!this.get('isAddService') ? 'addServiceController' : 'installerController').get('allHosts');
    }
    if (typeof hostsList == 'string') return allHosts.findProperty(convertTarget.from, hostsList).get(convertTarget.to);
    return hostsList.map(function(hostName) {
      return allHosts.findProperty(convertTarget.from, hostName).get(convertTarget.to);
    }, this);
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
        typeTagConfigs.push(Em.Object.create({
          name: config,
          value: configs.properties[config]
        }));
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
      dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message').format(this.get('selectedConfigGroup.displayName'))
    };
    hostsManagement.launchHostsSelectionDialog(availableHosts, [], false, this.get('componentsForFilter'), this.addHostsCallback.bind(this), popupDescription);
  },

  /**
   * add hosts callback
   * @param {string[]} selectedHosts
   * @method addHostsCallback
   */
  addHostsCallback: function (selectedHosts) {
    var group = this.get('selectedConfigGroup');
    if (selectedHosts) {
      selectedHosts.forEach(function (hostName) {
        group.get('hosts').pushObject(hostName);
        group.get('publicHosts').pushObject(this.hostsToPublic(hostName));
        group.get('parentConfigGroup.hosts').removeObject(hostName);
        group.get('parentConfigGroup.publicHosts').removeObject(this.hostsToPublic(hostName));
      }, this);
    }
  },

  /**
   * delete hosts from group
   * @method deleteHosts
   */
  deleteHosts: function () {
    if (this.get('isDeleteHostsDisabled')) {
      return;
    }
    this.get('selectedHosts').slice().forEach(function (hostName) {
      this.get('selectedConfigGroup.parentConfigGroup.hosts').pushObject(this.publicToHostName(hostName));
      this.get('selectedConfigGroup.parentConfigGroup.publicHosts').pushObject(hostName);
      this.get('selectedConfigGroup.hosts').removeObject(this.publicToHostName(hostName));
      this.get('selectedConfigGroup.publicHosts').removeObject(hostName);
    }, this);
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
    return App.StackServiceComponent.find().filterProperty('serviceName', this.get('serviceName')).map(function (component) {
      return Em.Object.create({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName')),
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
    this.set('selectedHosts', selectedConfigGroup.get('publicHosts'));
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
      warningMessage: null,
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
        return !(this.get('configGroupName').trim().length > 0 && (this.get('warningMessage') !== null && !this.get('warningMessage')));
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
        this.$('input').focus();
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
          publicHosts: [],
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
    // remove default group
    originalGroups = originalGroups.without(originalGroups.findProperty('isDefault'));
    var originalGroupsIds = originalGroups.mapProperty('id');
    groups.forEach(function (group) {
      if (!group.get('isDefault')) {
        var originalGroup = originalGroups.findProperty('id', group.get('id'));
        if (originalGroup) {
          if (!(JSON.stringify(group.get('hosts').slice().sort()) === JSON.stringify(originalGroup.get('hosts').sort()))) {
            groupsToClearHosts.push(group.set('id', originalGroup.get('id')));
            if (group.get('hosts').length) {
              groupsToSetHosts.push(group.set('id', originalGroup.get('id')));
            }
          // should update name or description
          } else if (group.get('description') !== originalGroup.get('description') || group.get('name') !== originalGroup.get('name') ) {
            groupsToSetHosts.push(group.set('id', originalGroup.get('id')));
          }
          originalGroupsIds = originalGroupsIds.without(group.get('id'));
        } else {
          groupsToCreate.push(group);
        }
      }
    });
    originalGroupsIds.forEach(function (id) {
      groupsToDelete.push(originalGroups.findProperty('id', id));
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
