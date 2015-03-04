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

App.RMHighAvailabilityWizardStep3Controller = Em.Controller.extend({
  name: "rMHighAvailabilityWizardStep3Controller",

  selectedService: null,

  versionLoaded: true,

  isLoaded: false,

  isSubmitDisabled: function () {
    return !this.get('isLoaded');
  }.property('isLoaded'),

  loadStep: function () {
    this.renderConfigs();
  },

  /**
   * Render configs to show them in <code>App.ServiceConfigView</code>
   */
  renderConfigs: function () {

    var configs = $.extend(true, {}, require('data/HDP2/rm_ha_properties').haConfig);

    var serviceConfig = App.ServiceConfig.create({
      serviceName: configs.serviceName,
      displayName: configs.displayName,
      configCategories: [],
      showConfig: true,
      configs: []
    });

    configs.configCategories.forEach(function (configCategory) {
      if (App.Service.find().someProperty('serviceName', configCategory.name)) {
        serviceConfig.configCategories.pushObject(configCategory);
      }
    }, this);

    this.renderConfigProperties(configs, serviceConfig);
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'loadConfigTagsSuccessCallback',
      error: 'loadConfigsErrorCallback',
      data: {
        serviceConfig: serviceConfig
      }
    });

  },

  loadConfigTagsSuccessCallback: function (data, opt, params) {
    var urlParams = '(type=zoo.cfg&tag=' + data.Clusters.desired_configs['zoo.cfg'].tag + ')';
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams,
        serviceConfig: params.serviceConfig
      },
      success: 'loadConfigsSuccessCallback',
      error: 'loadConfigsErrorCallback'
    });
  },

  loadConfigsSuccessCallback: function (data, opt, params) {
    var zooCfg = data.items.findProperty('type', 'zoo.cfg');
    var portValue = zooCfg && Em.get(zooCfg, 'properties.clientPort');
    var zkPort = typeof portValue === 'undefined' ? '2181' : portValue;
    this.setDynamicConfigValues(params.serviceConfig, zkPort);
    this.setProperties({
      selectedService: params.serviceConfig,
      isLoaded: true
    });
  },

  loadConfigsErrorCallback: function (request, ajaxOptions, error, data, params) {
    this.setDynamicConfigValues(params.serviceConfig, '2181');
    this.setProperties({
      selectedService: params.serviceConfig,
      isLoaded: true
    });
  },

  /**
   * Set values dependent on host selection
   * @param configs
   * @param zkPort
   */
  setDynamicConfigValues: function (configs, zkPort) {
    var configProperties = configs.configs;
    var currentRMHost = this.get('content.rmHosts.currentRM');
    var additionalRMHost = this.get('content.rmHosts.additionalRM');
    var zooKeeperHostsWithPort = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').map(function (item) {
      return item.get('host.hostName') + ':' + zkPort;
    }).join(',');
    configProperties.findProperty('name', 'yarn.resourcemanager.hostname.rm1').set('value', currentRMHost).set('defaultValue', currentRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.hostname.rm2').set('value', additionalRMHost).set('defaultValue', additionalRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.zk-address').set('value', zooKeeperHostsWithPort).set('defaultValue', zooKeeperHostsWithPort);

    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.address.rm1').set('value', additionalRMHost).set('defaultValue', additionalRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.address.rm2').set('value', additionalRMHost).set('defaultValue', additionalRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm1').set('value', additionalRMHost).set('defaultValue', additionalRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm2').set('value', additionalRMHost).set('defaultValue', additionalRMHost);

  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  renderConfigProperties: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      serviceConfigProperty.validate();
    }, this);
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
        App.router.send("next");
      });
    }
  }
});

