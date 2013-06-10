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

  totalSteps: 3,

  content: Em.Object.create({
    services: [],
    serviceConfigProperties: null,
    controllerName: 'addSecurityController'
  }),

  /**
   * Loads all prior steps on refresh
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '3':
      case '2':
        this.loadServiceConfigs();
      case '1':
        this.loadServices();
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
    var secureServices = require('data/secure_configs');
    var installedServices = App.Service.find().mapProperty('serviceName');
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
   * Loads all service config properties
   */

  loadServiceConfigs: function () {
    var serviceConfigProperties = App.db.getServiceConfigProperties();
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  }
});

