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
var arrayUtils = require('utils/array_utils');

/**
 * Mixin with methods for config groups and overrides processing
 * Used in the installer step7, service configs page and others
 * @type {Em.Mixin}
 */
App.ConfigOverridable = Em.Mixin.create({

  /**
   *
   * @method createOverrideProperty
   */
  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var serviceConfigController = this.get('isView') ? this.get('controller') : this;
    var selectedConfigGroup = serviceConfigController.get('selectedConfigGroup');
    var isInstaller = this.get('controller.name') === 'wizardStep7Controller';
    var configGroups = (isInstaller) ? serviceConfigController.get('selectedService.configGroups') : serviceConfigController.get('configGroups');

    //user property is added, and it has not been saved, not allow override
    if (serviceConfigProperty.get('isUserProperty') && serviceConfigProperty.get('isNotSaved') && !isInstaller) {
      App.ModalPopup.show({
        header: Em.I18n.t('services.service.config.configOverride.head'),
        body: Em.I18n.t('services.service.config.configOverride.body'),
        secondary: false
      });
      return;
    }
    if (selectedConfigGroup.get('isDefault')) {
      // Launch dialog to pick/create Config-group
      this.launchConfigGroupSelectionCreationDialog(
        this.get('service.serviceName'),
        configGroups,
        serviceConfigProperty,
        function (selectedGroupInPopup) {
          if (selectedGroupInPopup) {
            serviceConfigController.set('overrideToAdd', serviceConfigProperty);
            serviceConfigController.set('selectedConfigGroup', selectedGroupInPopup);
          }
        },
        isInstaller
      );
    }
    else {
      var valueForOverride = (serviceConfigProperty.get('widget') || serviceConfigProperty.get('displayType') == 'checkbox') ? serviceConfigProperty.get('value') : '';
      var override = App.config.createOverride(serviceConfigProperty, { "value": valueForOverride, "isEditable": true }, selectedConfigGroup);
      if (isInstaller) {
        selectedConfigGroup.get('properties').pushObject(override);
      }
    }
    Em.$('body>.tooltip').remove();
  },

  /**
   * Open popup with list of config groups
   * User may select existing group or create a new one
   * @param {string} serviceId service name like 'HDFS', 'HBASE' etc
   * @param {App.ConfigGroup[]} configGroups
   * @param {App.ConfigProperty} configProperty
   * @param {Function} callback function called after config group is selected (or new one is created)
   * @param {Boolean} isInstaller determines if user is currently on the installer
   * @return {App.ModalPopup}
   * @method launchConfigGroupSelectionCreationDialog
   */
  launchConfigGroupSelectionCreationDialog: function (serviceId, configGroups, configProperty, callback, isInstaller) {
    var self = this;
    var availableConfigGroups = configGroups.slice();
    // delete Config Groups, that already have selected property overridden
    var alreadyOverriddenGroups = [];
    if (configProperty.get('overrides')) {
      alreadyOverriddenGroups = configProperty.get('overrides').mapProperty('group.name');
    }
    var result = [];
    availableConfigGroups.forEach(function (group) {
      if (!group.get('isDefault') && (!alreadyOverriddenGroups.length || !alreadyOverriddenGroups.contains(Em.get(group, 'name')))) {
        result.push(group);
      }
    }, this);
    availableConfigGroups = result;
    var selectedConfigGroup = availableConfigGroups && availableConfigGroups.length > 0 ?
      availableConfigGroups[0] : null;
    var serviceName = App.format.role(serviceId, true);

    return App.ModalPopup.show({
      classNames: ['sixty-percent-width-modal'],
      header: Em.I18n.t('config.group.selection.dialog.title').format(serviceName),
      subTitle: Em.I18n.t('config.group.selection.dialog.subtitle').format(serviceName),
      selectExistingGroupLabel: Em.I18n.t('config.group.selection.dialog.option.select').format(serviceName),
      noGroups: Em.I18n.t('config.group.selection.dialog.no.groups').format(serviceName),
      createNewGroupLabel: Em.I18n.t('config.group.selection.dialog.option.create').format(serviceName),
      createNewGroupDescription: Em.I18n.t('config.group.selection.dialog.option.create.msg').format(serviceName),
      warningMessage: '&nbsp;',
      isWarning: false,
      optionSelectConfigGroup: true,
      optionCreateConfigGroup: function () {
        return !this.get('optionSelectConfigGroup');
      }.property('optionSelectConfigGroup'),
      hasExistedGroups: function () {
        return !!this.get('availableConfigGroups').length;
      }.property('availableConfigGroups'),
      availableConfigGroups: availableConfigGroups,
      selectedConfigGroup: selectedConfigGroup,
      newConfigGroupName: '',
      disablePrimary: function () {
        return !(this.get('optionSelectConfigGroup') || (this.get('newConfigGroupName').trim().length > 0 && !this.get('isWarning')));
      }.property('newConfigGroupName', 'optionSelectConfigGroup', 'warningMessage'),
      onPrimary: function () {
        if (this.get('optionSelectConfigGroup')) {
          var selectedConfigGroup = this.get('selectedConfigGroup');
          this.hide();
          callback(selectedConfigGroup);
          if (!isInstaller) {
            App.get('router.mainServiceInfoConfigsController').doSelectConfigGroup({context: selectedConfigGroup});
          }
        } else {
          var newConfigGroupName = this.get('newConfigGroupName').trim();
          var newConfigGroup = App.ConfigGroup.create({
            id: null,
            name: newConfigGroupName,
            description: Em.I18n.t('config.group.description.default').format(new Date().toDateString()),
            isDefault: false,
            parentConfigGroup: null,
            service: (isInstaller) ? Em.Object.create({id: serviceId}) : App.Service.find().findProperty('serviceName', serviceId),
            hosts: [],
            configSiteTags: [],
            properties: []
          });
          if (!isInstaller) {
            self.postNewConfigurationGroup(newConfigGroup);
          }
          if (newConfigGroup) {
            newConfigGroup.set('parentConfigGroup', configGroups.findProperty('isDefault'));
            configGroups.pushObject(newConfigGroup);
            if (isInstaller) {
              self.persistConfigGroups();
            } else {
              self.saveGroupConfirmationPopup(newConfigGroupName);
            }
            this.hide();
            callback(newConfigGroup);
          }
        }
      },
      onSecondary: function () {
        this.hide();
        callback(null);
      },
      doSelectConfigGroup: function (event) {
        var configGroup = event.context;
        console.log(configGroup);
        this.set('selectedConfigGroup', configGroup);
      },
      validate: function () {
        var msg = '&nbsp;';
        var isWarning = false;
        var optionSelect = this.get('optionSelectConfigGroup');
        if (!optionSelect) {
          var nn = this.get('newConfigGroupName');
          if (nn && configGroups.mapProperty('name').contains(nn.trim())) {
            msg = Em.I18n.t("config.group.selection.dialog.err.name.exists");
            isWarning = true;
          }
        }
        this.set('warningMessage', msg);
        this.set('isWarning', isWarning);
      }.observes('newConfigGroupName', 'optionSelectConfigGroup'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/selectCreateConfigGroup'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        selectConfigGroupRadioButton: Em.Checkbox.extend({
          tagName: 'input',
          attributeBindings: ['type', 'checked', 'disabled'],
          checked: function () {
            return this.get('parentView.parentView.optionSelectConfigGroup');
          }.property('parentView.parentView.optionSelectConfigGroup'),
          type: 'radio',
          disabled: false,
          click: function () {
            this.set('parentView.parentView.optionSelectConfigGroup', true);
          },
          didInsertElement: function () {
            if (!this.get('parentView.parentView.hasExistedGroups')) {
              this.set('disabled', true);
              this.set('parentView.parentView.optionSelectConfigGroup', false);
            }
          }
        }),
        createConfigGroupRadioButton: Em.Checkbox.extend({
          tagName: 'input',
          attributeBindings: ['type', 'checked'],
          checked: function () {
            return !this.get('parentView.parentView.optionSelectConfigGroup');
          }.property('parentView.parentView.optionSelectConfigGroup'),
          type: 'radio',
          click: function () {
            this.set('parentView.parentView.optionSelectConfigGroup', false);
          }
        })
      })
    });
  },

  /**
   * Create a new config-group for a service.
   *
   * @param {App.ConfigGroup} newConfigGroupData config group to post to server
   * @param {Function} callback Callback function for Success or Error handling
   * @return {App.ConfigGroup} Returns the created config-group
   * @method postNewConfigurationGroup
   */
  postNewConfigurationGroup: function (newConfigGroupData, callback) {
    var dataHosts = [];
    newConfigGroupData.get('hosts').forEach(function (_host) {
      dataHosts.push({
        host_name: _host
      });
    }, this);
    var sendData = {
      name: 'config_groups.create',
      data: {
        'group_name': newConfigGroupData.get('name'),
        'service_id': newConfigGroupData.get('service.id'),
        'description': newConfigGroupData.get('description'),
        'hosts': dataHosts
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function (response) {
        newConfigGroupData.set('id', response.resources[0].ConfigGroup.id);
        if (callback) {
          callback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (callback) {
          callback(xhr, text, errorThrown);
        }
        console.error('Error in creating new Config Group');
      }
    };
    sendData.sender = sendData;
    App.ajax.send(sendData);
    return newConfigGroupData;
  },

  /**
   * PUTs the new configuration-group on the server.
   * Changes possible here are the name, description and
   * host memberships of the configuration-group.
   *
   * @param {App.ConfigGroup} configGroup Configuration group to update
   * @param {Function} successCallback
   * @param {Function} errorCallback
   * @return {$.ajax}
   * @method updateConfigurationGroup
   */
  updateConfigurationGroup: function (configGroup, successCallback, errorCallback) {
    var configSiteTags = configGroup.get('configSiteTags') || configGroup.get('desiredConfigs') || [];
    var putConfigGroup = {
      ConfigGroup: {
        group_name: configGroup.get('name'),
        description: configGroup.get('description'),
        tag: configGroup.get('service.id'),
        hosts: configGroup.get('hosts').map(function (h) {
          return {
            host_name: h
          };
        }),
        desired_configs: configSiteTags.map(function (cst) {
          return {
            type: Em.get(cst, 'site') || Em.get(cst, 'type'),
            tag: Em.get(cst, 'tag')
          };
        })
      }
    };

    var sendData = {
      name: 'config_groups.update',
      data: {
        id: Em.isNone(configGroup.get('configGroupId')) ? configGroup.get('id') : configGroup.get('configGroupId'),
        data: putConfigGroup
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    return App.ajax.send(sendData);
  },

  /**
   * launch dialog where can be assigned another group to host
   * @param {App.ConfigGroup} selectedGroup
   * @param {App.ConfigGroup[]} configGroups
   * @param {String} hostName
   * @param {Function} callback
   * @return {App.ModalPopup}
   * @method launchSwitchConfigGroupOfHostDialog
   */
  launchSwitchConfigGroupOfHostDialog: function (selectedGroup, configGroups, hostName, callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('config.group.host.switch.dialog.title'),
      configGroups: configGroups,
      selectedConfigGroup: selectedGroup,
      disablePrimary: function () {
        return !(this.get('selectedConfigGroup.name') !== selectedGroup.get('name'));
      }.property('selectedConfigGroup'),
      onPrimary: function () {
        var newGroup = this.get('selectedConfigGroup');
        if (selectedGroup.get('isDefault')) {
          selectedGroup.set('hosts.length', selectedGroup.get('hosts.length') - 1);
        } else {
          selectedGroup.get('hosts').removeObject(hostName);
        }
        if (!selectedGroup.get('isDefault')) {
          self.updateConfigurationGroup(selectedGroup, Em.K, Em.K);
        }

        if (newGroup.get('isDefault')) {
          newGroup.set('hosts.length', newGroup.get('hosts.length') + 1);
        } else {
          newGroup.get('hosts').pushObject(hostName);
        }
        callback(newGroup);
        if (!newGroup.get('isDefault')) {
          self.updateConfigurationGroup(newGroup, Em.K, Em.K);
        }
        this.hide();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/utils/config_launch_switch_config_group_of_host')
      })
    });
  },

  /**
   * Update config group's hosts list and leave only unmodified hosts in the group
   * Save updated config group on server
   * @param {App.ConfigGroup} configGroup
   * @param {App.ConfigGroup} initalGroupState
   * @param {Function} successCallback
   * @param {Function} errorCallback
   * @method clearConfigurationGroupHosts
   */
  clearConfigurationGroupHosts: function (configGroup, initalGroupState, successCallback, errorCallback) {
    configGroup = jQuery.extend({}, configGroup);
    var unmodifiedHosts = this.getUnmodifiedHosts(configGroup, initalGroupState);
    configGroup.set('hosts', unmodifiedHosts);
    this.updateConfigurationGroup(configGroup, successCallback, errorCallback);
  },

  /**
   * Get the list of hosts that is not modified in the group
   * @param configGroup - the new configuration of the group
   * @param initialGroupState - the initial configuration of the group
   * @returns {Array}
   */
  getUnmodifiedHosts: function (configGroup, initialGroupState) {
    var currentHosts = configGroup.get('hosts');
    var initialHosts = initialGroupState.get('hosts');

    return arrayUtils.intersect(currentHosts, initialHosts);
  },

  /**
   * Do request to delete config group
   * @param {App.ConfigGroup} configGroup
   * @param {Function} [successCallback]
   * @param {Function} [errorCallback]
   * @return {$.ajax}
   * @method deleteConfigurationGroup
   */
  deleteConfigurationGroup: function (configGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'common.delete.config_group',
      sender: this,
      data: {
        id: configGroup.get('id')
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    return App.ajax.send(sendData);
  },

  /**
   * Launches a dialog where an existing config-group can be selected, or a new
   * one can be created. This is different than the config-group management
   * dialog where host membership can be managed.
   *
   * The callback will be passed the created/selected config-group in the form
   * of {id:2, name:'New hardware group'}. In the case of dialog being cancelled,
   * the callback is provided <code>null</code>
   *
   * @param {String} groupName
   *  is closed, cancelled or OK is pressed.
   * @return {App.ModalPopup}
   * @method saveGroupConfirmationPopup
   */
  saveGroupConfirmationPopup: function (groupName) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('config.group.save.confirmation.header'),
      secondary: Em.I18n.t('config.group.save.confirmation.manage.button'),
      groupName: groupName,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/saveConfigGroup')
      }),
      onPrimary:function() {
        if (self.get('controller.name') == 'mainServiceInfoConfigsController') {
          self.get('controller').loadConfigGroups([self.get('controller.content.serviceName')]).done(function() {
            var group = App.ServiceConfigGroup.find().find(function(g) {
              return g.get('serviceName') == self.get('controller.content.serviceName') && g.get('name') == groupName;
            });
            self.get('controller').doSelectConfigGroup({context: group});
          });
        }
        this._super();
      },
      onSecondary: function () {
        App.router.get('manageConfigGroupsController').manageConfigurationGroups(null, self.get('controller.content'));
        this.hide();
      }
    });
  },

  /**
   * Persist config groups created in step7 wizard controller
   * @method persistConfigGroups
   */
  persistConfigGroups: function () {
    var installerController = App.router.get('installerController');
    var step7Controller = App.router.get('wizardStep7Controller');
    installerController.saveServiceConfigGroups(step7Controller, step7Controller.get('content.controllerName') == 'addServiceController');
    App.clusterStatus.setClusterStatus({
      localdb: App.db.data
    });
  }

});
