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
App.AddSecurityController = App.WizardController.extend({

  name: 'addSecurityController',
  securityEnabled: false,

  totalSteps: 4,

  content: Em.Object.create({
    services: [],
    isNnHa: 'false',
    serviceConfigProperties: null,
    controllerName: 'addSecurityController',
    isATSInstalled: function() {
      // Because the ATS component can be installed/removed at will, the check has to happen every time that security is added.
      var yarnService = App.Service.find().findProperty('serviceName','YARN');
      return !!yarnService && yarnService.get('hostComponents').someProperty('componentName', 'APP_TIMELINE_SERVER');
    }.property('App.router.clusterController.isLoaded')
  }),

  /**
   * installed services on cluster
   */
  installedServices: function () {
    return App.Service.find().mapProperty('serviceName');
  }.property(),

  /**
   * services with security configurations
   */
  secureServices: function () {
    var configCategories = require('data/HDP2/secure_configs');
    if (this.get('content.isATSInstalled') && App.get('doesATSSupportKerberos')) {
      var yarnConfigCategories = configCategories.findProperty('serviceName', 'YARN').configCategories;
      yarnConfigCategories.push(App.ServiceConfigCategory.create({ name: 'AppTimelineServer', displayName : 'Application Timeline Service'}));
    }
    return configCategories;
  }.property('App.router.clusterController.isLoaded'),

  /**
   * Loads all prior steps on refresh
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '4':
      case '3':
      case '2':
        this.loadServiceConfigs();
      case '1':
        this.loadServices();
        this.loadNnHaStatus();
    }
  },
  /**
   * Load installed services, which match secure services, to content
   */
  loadServices: function () {
    var secureServices = this.get('secureServices');
    var installedServices = this.get('installedServices');

    this.get('content.services').clear();
    //General (only non service tab) tab is always displayed
    this.get('content.services').push(secureServices.findProperty('serviceName', 'GENERAL'));
    installedServices.forEach(function (_service) {
      var secureService = secureServices.findProperty('serviceName', _service);
      if (secureService) {
        this.get('content.services').push(secureService);
      }
    }, this);
  },
  /**
   * identify whether NameNode in high availability mode
   */
  loadNnHaStatus: function () {
    this.set('content.isNnHa', App.db.getIsNameNodeHa());
  },

  /**
   * save service config properties to localStorage
   * @param stepController
   */
  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_configProperties) {
        _configProperties.set('value', App.config.trimProperty(_configProperties, true));
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          serviceName: _configProperties.get('serviceName'),
          domain: _configProperties.get('domain'),
          filename: _configProperties.get('filename'),
          unit: _configProperties.get('unit'),
          components: _configProperties.get('components'),
          component: _configProperties.get('component'),
          overrides: this.getConfigOverrides(_configProperties)
        };
        serviceConfigProperties.push(configProperty);
      }, this);
    }, this);
    App.db.setSecureConfigProperties(serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  /**
   * get overrides of config
   * @param _configProperties
   * @return {Array}
   */
  getConfigOverrides: function (_configProperties) {
    var overrides = _configProperties.get('overrides');
    var overridesArray = [];
    if (Array.isArray(overrides)) {
      overrides.forEach(function (override) {
        var overrideEntry = {
          value: override.get('value'),
          hosts: []
        };
        override.get('selectedHostOptions').forEach(function (host) {
          overrideEntry.hosts.push(host);
        });
        overridesArray.push(overrideEntry);
      });
    }
    return (overridesArray.length > 0) ? overridesArray : null;
  },

  /**
   * Load service config properties from localStorage
   */
  loadServiceConfigs: function () {
    this.set('content.serviceConfigProperties', App.db.getSecureConfigProperties());
  },

  /**
   * Clear all local storage data for Add security wizard namespace
   */
  finish: function () {
    this.resetDbNamespace();
  }
});

