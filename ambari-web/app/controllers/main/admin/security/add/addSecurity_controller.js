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
    controllerName: 'addSecurityController'
  }),

  installedServices: function() {
    return App.Service.find().mapProperty('serviceName');
  }.property(),

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
        this.loadNnHastatus();
    }
  },

  clearServices: function () {
    if (this.get('content.services')) {
      this.get('content.services').clear();
    }
  },

  /**
   * Loads all installed services
   */
  loadServices: function () {
    this.clearServices();
    var secureServices;
    if(App.get('isHadoop2Stack')) {
      secureServices = $.extend(true, [], require('data/HDP2/secure_configs'));
    } else {
      secureServices = $.extend(true, [], require('data/secure_configs'));
    }

    var installedServices = this.get('installedServices');
    //General (only non service tab) tab is always displayed
    this.get('content.services').push(secureServices.findProperty('serviceName', 'GENERAL'));
    installedServices.forEach(function (_service) {
      var secureService = secureServices.findProperty('serviceName', _service);
      if (secureService) {
        this.get('content.services').push(secureService);
      }
    }, this);

  },

  loadNnHastatus: function() {
    var isNnHa = App.db.getIsNameNodeHa();
    this.set('content.isNnHa', isNnHa);
  },

  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_configProperties) {
        _configProperties.set('value', App.config.trimProperty(_configProperties,true));
        var overrides = _configProperties.get('overrides');
        var overridesArray = [];
        if(overrides!=null){
          overrides.forEach(function(override){
            var overrideEntry = {
              value: override.get('value'),
              hosts: []
            };
            override.get('selectedHostOptions').forEach(function(host){
              overrideEntry.hosts.push(host);
            });
            overridesArray.push(overrideEntry);
          });
        }
        overridesArray = (overridesArray.length) ? overridesArray : null;
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          serviceName: _configProperties.get('serviceName'),
          domain:  _configProperties.get('domain'),
          filename: _configProperties.get('filename'),
          unit: _configProperties.get('unit'),
          components: _configProperties.get('components'),
          component: _configProperties.get('component'),
          overrides: overridesArray
        };
        serviceConfigProperties.push(configProperty);
      }, this);
    }, this);
    App.db.setSecureConfigProperties(serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  /**
   * Loads all service config properties
   */

  loadServiceConfigs: function () {
    var serviceConfigProperties = App.db.getSecureConfigProperties();
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  }
});

