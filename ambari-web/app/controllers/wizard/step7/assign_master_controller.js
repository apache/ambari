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

var stringUtils = require('utils/string_utils');
var numberUtils = require('utils/number_utils');

App.AssignMasterOnStep7Controller = Em.Controller.extend(App.BlueprintMixin, App.AssignMasterComponents, {

  name: "assignMasterOnStep7Controller",

  useServerValidation: false,

  showInstalledMastersFirst: false,

  configWidgetContext: {},

  configActionComponent: {},

  content: function () {
    return this.get('configWidgetContext.controller.content') || {};
  }.property('configWidgetContext.controller.content'),

  popup: null,

  mastersToCreate: [],

  markSavedComponentsAsInstalled: true,

  /**
   * Marks component add/delete action to be performed ahead.
   * @param context {Object} Context of the calling function
   * @param action {String} ADD|DELETE
   * @param hostComponent {Object}
   * @public
   * @method {execute}
   */
  execute: function (context, action, hostComponent) {
    this.set('configWidgetContext', context);
    this.set('content', context.get('controller.content'));
    this.set('configActionComponent', hostComponent);
    var missingDependentServices = this.getAllMissingDependentServices();
    var isNonWizardPage = !this.get('content.controllerName');
    switch (action) {
      case 'ADD':
        if (missingDependentServices.length && isNonWizardPage) {
          this.showInstallServicesPopup(missingDependentServices);
        } else {
          this.set('mastersToCreate', [hostComponent.componentName]);
          this.showAssignComponentPopup();
        }
        break;
      case 'DELETE':
        this.set('mastersToCreate', [hostComponent.componentName]);
        this.removeMasterComponent();
        break;
    }
  },

  /**
   * Used to set showAddControl/showRemoveControl flag
   * @param componentName
   * @override
   */
  updateComponent: function(componentName) {
    this._super(componentName);

    if (!this.get('mastersToCreate').contains(componentName)) {
      this.get("selectedServicesMasters").filterProperty("component_name", componentName).forEach(function(c) {
        c.set('showAddControl', false);
        c.set('showRemoveControl', false);
      });
    }
  },

  /**
   * Assign Master page will be displayed in the popup
   * @private
   * @method
   */
  showAssignComponentPopup: function () {
    var self = this;
    // Master component hosts should be loaded only when content.controller name is not defined i.e non-wizard pages
    if (!this.get('content.controllerName')) {
      this.loadMasterComponentHosts();
    }
    var popup = App.ModalPopup.show({
      classNames: ['full-width-modal', 'add-service-wizard-modal'],
      header: Em.I18n.t('admin.highAvailability.wizard.step2.header'),
      bodyClass: App.AssignMasterOnStep7View.extend({
        controller: self
      }),
      primary: Em.I18n.t('form.cancel'),
      showFooter: false,
      secondary: null,
      showCloseButton: false,
      didInsertElement: function () {
        this._super();
        this.fitHeight();
        self.set('configWidgetContext.controller.saveInProgress', false);
      }
    });
    this.set('popup', popup);
  },

  /**
   * Displays the popup to install required service dependencies for being added component with this config change
   * @param missingDependentServices {String[]}   Array of service display names
   */
  showInstallServicesPopup: function (missingDependentServices) {
    var displayServices = stringUtils.getFormattedStringFromArray(missingDependentServices);
    var configWidgetContext = this.get('configWidgetContext');
    var config = this.get('configWidgetContext.config');
    var configDisplayName = config.get('displayName').toLowerCase();
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step7.missing.service.header'),
      body: Em.I18n.t('installer.step7.missing.service.body').format(displayServices, configDisplayName),
      primaryClass: 'btn-danger',
      onPrimary: function () {
        configWidgetContext.toggleProperty('controller.forceUpdateBoundaries');
        var value = config.get('initialValue');
        config.set('value', value);
        configWidgetContext.setValue(value);
        this._super();
      },
      secondary: null,
      showCloseButton: false,
      didInsertElement: function () {
        this._super();
        configWidgetContext.set('controller.saveInProgress', false);
      }
    });
  },

  /**
   * This method is used while installing or adding a service
   * Removes the masterComponent that was previously being tracked to be added to the cluster
   * @private
   * @method {removeMasterComponent}
   */
  removeMasterComponent: function () {
    var componentsToDelete = this.get('mastersToCreate');
    if (this.get('content.controllerName')) {
      var parentController = App.router.get(this.get('content.controllerName'));
      var masterComponentHosts = this.get('content.masterComponentHosts');
      var recommendationsHostGroups = this.get('content.recommendationsHostGroups');
      componentsToDelete.forEach(function (_componentName) {
        masterComponentHosts = masterComponentHosts.rejectProperty('component', _componentName);
        recommendationsHostGroups.blueprint.host_groups.forEach(function(hostGroup){
          hostGroup.components = hostGroup.components.rejectProperty('name', _componentName);
        }, this);
      }, this);
      this.get('content').set('masterComponentHosts', masterComponentHosts);
      parentController.setDBProperty('masterComponentHosts', masterComponentHosts);
      parentController.setDBProperty('recommendationsHostGroups', recommendationsHostGroups);
    } else {
      this.clearComponentsToBeAdded(componentsToDelete[0]);
      var hostComponent = App.HostComponent.find().findProperty('componentName', componentsToDelete[0]);
      if (hostComponent) {
        App.set('componentToBeDeleted', Em.Object.create({
          componentName: componentsToDelete[0],
          hostName: hostComponent.get('hostName')
        }));
      }
    }
    var configActionComponent = this.get('configActionComponent');
    this.get('configWidgetContext.config').set('configActionComponent', configActionComponent);
  },

  /**
   * Load active host list to <code>hosts</code> variable
   * @override
   * @method renderHostInfo
   */
  renderHostInfo: function () {
    var parentController = this.get('content.controllerName');
    if (parentController) {
      this._super();
    } else {
      var hosts = App.Host.find().toArray();
      var result = [];
      for (var p = 0; p < hosts.length; p++) {
        result.push(Em.Object.create({
          host_name: hosts[p].get('hostName'),
          cpu: hosts[p].get('cpu'),
          memory: hosts[p].get('memory'),
          maintenance_state: hosts[p].get('maintenance_state'),
          disk_info: hosts[p].get('diskInfo'),
          host_info: Em.I18n.t('installer.step5.hostInfo').fmt(hosts[p].get('hostName'), numberUtils.bytesToSize(hosts[p].get('memory'), 1, 'parseFloat', 1024), hosts[p].get('cpu'))
        }));
      }

      this.set("hosts", result);
      this.sortHosts(result);
      this.set('isLoaded', true);
    }
  },

  /**
   *  This method is called on Service->config page and is responsible to load the "Assign Master popup"
   *  with the installed master component hosts.
   * @private
   * @method {loadMasterComponentHosts}
   */
  loadMasterComponentHosts: function () {
    var stackMasterComponents = App.get('components.masters').uniq();
    var masterComponentHosts = [];
    App.HostComponent.find().filter(function (component) {
      return stackMasterComponents.contains(component.get('componentName'));
    }).forEach(function (item) {
      masterComponentHosts.push({
        component: item.get('componentName'),
        hostName: item.get('hostName'),
        isInstalled: true,
        serviceId: item.get('service.id'),
        display_name: item.get('displayName')
      })
    });
    this.set("masterComponentHosts", masterComponentHosts);
  },

  /**
   * Returns array of dependent services that are yet not installed in the cluster
   * @private
   * @method getAllMissingDependentServices
   * @return  missingDependentServices {Array}
   */
  getAllMissingDependentServices: function () {
    var configActionComponentName = this.get('configActionComponent').componentName;
    var componentStackService = App.StackServiceComponent.find(configActionComponentName).get('stackService');
    var dependentServices = componentStackService.get('requiredServices');

    return dependentServices.filter(function (item) {
      return !App.Service.find().findProperty('serviceName', item);
    }).map(function (item) {
      return App.StackService.find(item).get('displayName');
    });
  },

  /**
   * This method saves masterComponent layout that is used on subsequent "Review" and "Install start and Test services" pages.
   * @private
   * @method {saveMasterComponentHosts}
   */
  saveMasterComponentHosts: function() {
    var controller = App.router.get(this.get('content.controllerName'));
    controller.saveMasterComponentHosts(this);
    controller.loadMasterComponentHosts();
  },

  /**
   * This method saves host group layout that is used for blueprint validation call made while transitioning to "Review" page.
   * @private
   * @method {saveRecommendationsHostGroups}
   */
  saveRecommendationsHostGroups: function() {
    var controller = App.router.get(this.get('content.controllerName'));
    var recommendationsHostGroups = this.get('content.recommendationsHostGroups');
    var mastersToCreate = this.get('mastersToCreate');
    mastersToCreate.forEach(function(componentName) {
      var hostName = this.getSelectedHostName(componentName);
      if (hostName && recommendationsHostGroups) {
        var hostGroups = recommendationsHostGroups.blueprint_cluster_binding.host_groups;
        var isHostPresent = false;
        var i = 0;
        while (i < hostGroups.length) {
          var hosts = hostGroups[i].hosts;
          isHostPresent = hosts.someProperty('fqdn', hostName);
          if (isHostPresent) break;
          i++;
        }
        if (isHostPresent) {
          var hostGroupName = hostGroups[i].name;
          var hostGroup = recommendationsHostGroups.blueprint.host_groups.findProperty('name', hostGroupName);
          var addHostComponentInGroup = !hostGroup.components.someProperty('name', componentName);
          if (addHostComponentInGroup) {
            hostGroup.components.pushObject({name: componentName});
          }
        }
      }
    }, this);
    controller.setDBProperty('recommendationsHostGroups', recommendationsHostGroups);
  },

  /**
   * Get the fqdn hostname as selected by the user for the component.
   * @param componentName
   * @return {String}
   */
  getSelectedHostName: function(componentName) {
    var selectedServicesMasters = this.get('selectedServicesMasters');
    return selectedServicesMasters.findProperty('component_name', componentName).selectedHost;
  },

  /**
   * set App.componentToBeAdded to use it on subsequent validation call while saving configuration
   * @param componentName {String}
   * @param hostName {String}
   * @method {setGlobalComponentToBeAdded}
   */
  setGlobalComponentToBeAdded: function(componentName, hostName) {
    var componentToBeAdded = Em.Object.create({
       componentName: componentName,
       hostNames: [hostName]
    });
    App.set('componentToBeAdded', componentToBeAdded);
  },

  /**
   * clear 'componentToBeDeleted' object
   * @param componentName {String}
   * @public
   * @method {clearComponentsToBeDeleted}
   */
  clearComponentsToBeDeleted: function(componentName) {
    var componentsToBeDeleted = App.get('componentToBeDeleted');
    if (!App.isEmptyObject(componentsToBeDeleted) && componentsToBeDeleted.get('componentName') === componentName) {
      App.set('componentToBeDeleted', {});
    }
  },

  /**
   * clear 'componentToBeAdded' object
   * @param componentName  {String}
   */
  clearComponentsToBeAdded: function(componentName) {
    var componentsToBeAdded = App.get('componentToBeAdded');
    if (!App.isEmptyObject(componentsToBeAdded) && componentsToBeAdded.get('componentName') === componentName) {
      App.set('componentToBeAdded', {});
    }
  },

  /**
   * Submit button click handler
   * @method submit
   */
  submit: function () {
    this.get('popup').hide();
    var context = this.get('configWidgetContext');
    context.toggleProperty('controller.forceUpdateBoundaries');
    var configActionComponent = this.get('configActionComponent');
    var componentHostName = this.getSelectedHostName(configActionComponent.componentName);
    if (this.get('content.controllerName')) {
      this.saveMasterComponentHosts();
      this.saveRecommendationsHostGroups();
    } else {
      this.setGlobalComponentToBeAdded(configActionComponent.componentName, componentHostName);
      this.clearComponentsToBeDeleted(configActionComponent.componentName);
    }

    var hostComponentConfig = context.get('config.configAction.hostComponentConfig');
    var serviceConfigs = context.get('controller.stepConfigs').findProperty('serviceName', context.get('config.serviceName')).get('configs');
    var config = serviceConfigs.filterProperty('filename', hostComponentConfig.fileName).findProperty('name', hostComponentConfig.configName);
    config.set('value', componentHostName);
    config.set('recommendedValue', componentHostName);
    configActionComponent.hostName = componentHostName;
    this.get('configWidgetContext.config').set('configActionComponent', configActionComponent);
  }
});