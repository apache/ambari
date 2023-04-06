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

/**
 * @typedef {object} nnHaConfigDependencies
 * @property {string} namespaceId
 * @property {object} serverConfigs
 * @property {string|number} nnHttpPort
 * @property {string|number} nnHttpsPort
 * @property {string|number} nnRpcPort
 * @property {string|number} zkClientPort
 */

var App = require('app');

require('utils/configs/nn_ha_config_initializer');

App.ManageJournalNodeWizardStep2Controller = Em.Controller.extend({
  name: "manageJournalNodeWizardStep2Controller",
  selectedService: null,
  stepConfigs: [],
  serverConfigData: {},
  moveJNConfig: $.extend(true, {}, require('data/configs/wizards/move_journal_node_properties').moveJNConfig),
  once: false,
  isLoaded: false,
  versionLoaded: true,
  hideDependenciesInfoBar: true,

  isNextDisabled: Em.computed.not('isLoaded'),

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.set('serverConfigData', {});
  },

  loadStep: function () {
    this.clearStep();
    this.loadConfigsTags();
  },

  loadConfigsTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },


  onLoadConfigsTags: function (data) {
    var urlParams = [];
    var hdfsSiteTag = data.Clusters.desired_configs['hdfs-site'].tag;
    urlParams.push('(type=hdfs-site&tag=' + hdfsSiteTag + ')');
    this.set("hdfsSiteTag", {name: "hdfsSiteTag", value: hdfsSiteTag});

    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    this.set('serverConfigData', data);
    this.set('content.nameServiceIds', data.items[0].properties['dfs.nameservices'].split(','));
    this.tweakServiceConfigs(this.get('moveJNConfig.configs'));
    this.renderServiceConfigs(this.get('moveJNConfig'));
    this.set('isLoaded', true);
  },

  /**
   * Generate set of data used to correctly initialize config values and names
   */
  _prepareDependencies: function (nameServiceId) {
    var ret = {};
    var configsFromServer = this.get('serverConfigData.items');
    ret.namespaceId = nameServiceId || this.get('content.nameServiceIds')[0];
    ret.serverConfigs = configsFromServer;
    return ret;
  },

  /**
   * Generate set of data with information about cluster topology
   * Used in the configs' initialization process
   *
   * @returns {extendedTopologyLocalDB}
   * @private
   * @method _prepareLocalDB
   */
  _prepareLocalDB: function () {
    var localDB = this.get('content').getProperties(['masterComponentHosts', 'slaveComponentHosts', 'hosts']);
    localDB.installedServices = App.Service.find().mapProperty('serviceName');
    return localDB;
  },

  tweakServiceConfigs: function (allConfigsDescriptor) {
    var hasNameNodeFederation = App.get('hasNameNodeFederation');
    var configs = hasNameNodeFederation
      ? allConfigsDescriptor.filterProperty('presentForFederatedHDFS')
      : allConfigsDescriptor.filterProperty('presentForNonFederatedHDFS');
    var nameSpaceDependentConfigs = configs.filterProperty('dependsOnNameServiceId');
    var nameSpaceIndependentConfigs = configs.rejectProperty('dependsOnNameServiceId');
    var localDB = this._prepareLocalDB();
    var commonDependencies = this._prepareDependencies();
    var generatedConfigs = [];
    var wizardController = App.router.get(this.get('content.controllerName'));
    var journalNodes = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE');

    nameSpaceIndependentConfigs.forEach(function (config) {
      App.NnHaConfigInitializer.initialValue(config, localDB, commonDependencies);
      config.isOverridable = false;
      generatedConfigs.push(config);
    });

    this.get('content.nameServiceIds').forEach(function (nameServiceId) {
      var dependencies = this._prepareDependencies(nameServiceId);
      dependencies.journalnodes = journalNodes.map(function (c) {
        return c.hostName + ':8485';
      }).join(';');
      nameSpaceDependentConfigs.forEach(function (config) {
        var generatedConfig = $.extend({}, config, {
          isOverridable: false,
          name: wizardController.replaceDependencies(config.name, dependencies),
          displayName: wizardController.replaceDependencies(config.displayName, dependencies),
          value: wizardController.replaceDependencies(config.value, dependencies),
          recommendedValue: wizardController.replaceDependencies(config.recommendedValue, dependencies)
        });
        generatedConfigs.push(generatedConfig);
      }, this);
    }, this);

    this.set('moveJNConfig.configs', generatedConfigs);

    return generatedConfigs;
  },

  renderServiceConfigs: function (_serviceConfig) {
    var serviceConfig = App.ServiceConfig.create({
      serviceName: _serviceConfig.serviceName,
      displayName: _serviceConfig.displayName,
      configCategories: [],
      showConfig: true,
      configs: []
    });

    _serviceConfig.configCategories.forEach(function (_configCategory) {
      if (App.Service.find().someProperty('serviceName', _configCategory.name)) {
        serviceConfig.configCategories.pushObject(_configCategory);
      }
    }, this);

    this.loadComponentConfigs(_serviceConfig, serviceConfig);

    this.get('stepConfigs').pushObject(serviceConfig);
    this.set('selectedService', this.get('stepConfigs').objectAt(0));
    this.set('once', true);
  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  loadComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
    }, this);
  }
});
