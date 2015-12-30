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

  hideDependenciesInfoBar: true,

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
    var urlParams = '(type=zoo.cfg&tag=' + data.Clusters.desired_configs['zoo.cfg'].tag + ')|' +
      '(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')|' +
      '(type=yarn-env&tag=' + data.Clusters.desired_configs['yarn-env'].tag + ')';
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams,
        serviceConfig: params.serviceConfig
      },
      success: 'loadConfigsSuccessCallback',
      error: 'loadConfigsSuccessCallback'
    });
  },

  loadConfigsSuccessCallback: function (data, opt, params) {
    var
      zooCfg = data && data.items ? data.items.findProperty('type', 'zoo.cfg') : null,
      yarnSite = data && data.items ? data.items.findProperty('type', 'yarn-site') : null,
      yarnEnv = data && data.items ? data.items.findProperty('type', 'yarn-env') : null,
      portValue = zooCfg && Em.get(zooCfg, 'properties.clientPort'),
      zkPort = portValue ? portValue : '2181',
      webAddressPort = yarnSite && yarnSite.properties ? yarnSite.properties['yarn.resourcemanager.webapp.address'] : null,
      httpsWebAddressPort = yarnSite && yarnSite.properties ? yarnSite.properties['yarn.resourcemanager.webapp.https.address'] : null,
      yarnUser = yarnEnv && yarnEnv.properties ? yarnEnv.properties['yarn_user'] : null,

    webAddressPort = webAddressPort && webAddressPort.match(/:[0-9]*/g) ? webAddressPort.match(/:[0-9]*/g)[0] : ":8088";
    httpsWebAddressPort = httpsWebAddressPort && httpsWebAddressPort.match(/:[0-9]*/g) ? httpsWebAddressPort.match(/:[0-9]*/g)[0] : ":8090";

    params = params.serviceConfig ? params.serviceConfig : arguments[4].serviceConfig;

    this.setDynamicConfigValues(params, zkPort, webAddressPort, httpsWebAddressPort, yarnUser);
    this.setProperties({
      selectedService: params,
      isLoaded: true
    });
  },

  /**
   * Set values dependent on host selection
   * @param configs
   * @param zkPort
   * @param webAddressPort
   * @param httpsWebAddressPort
   * @param yarnUser
   */
  setDynamicConfigValues: function (configs, zkPort, webAddressPort, httpsWebAddressPort, yarnUser) {
    var
      configProperties = configs.configs,
      currentRMHost = this.get('content.rmHosts.currentRM'),
      additionalRMHost = this.get('content.rmHosts.additionalRM'),
      rmHosts = currentRMHost + ',' + additionalRMHost,
      zooKeeperHostsWithPort = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').map(function (item) {
        return item.get('hostName') + ':' + zkPort;
      }).join(',');

    configProperties.findProperty('name', 'yarn.resourcemanager.hostname.rm1').set('value', currentRMHost).set('recommendedValue', currentRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.hostname.rm2').set('value', additionalRMHost).set('recommendedValue', additionalRMHost);
    configProperties.findProperty('name', 'yarn.resourcemanager.zk-address').set('value', zooKeeperHostsWithPort).set('recommendedValue', zooKeeperHostsWithPort);

    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.address.rm1')
      .set('value', currentRMHost + webAddressPort)
      .set('recommendedValue', currentRMHost + webAddressPort);

    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.address.rm2')
      .set('value', additionalRMHost + webAddressPort)
      .set('recommendedValue', additionalRMHost + webAddressPort);

    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm1')
      .set('value', currentRMHost + httpsWebAddressPort)
      .set('recommendedValue', currentRMHost + httpsWebAddressPort);

    configProperties.findProperty('name', 'yarn.resourcemanager.webapp.https.address.rm2')
      .set('value', additionalRMHost + httpsWebAddressPort)
      .set('recommendedValue', additionalRMHost + httpsWebAddressPort);

    var proxyUserConfig = App.ServiceConfigProperty.create(App.config.createDefaultConfig('hadoop.proxyuser.' + yarnUser + '.hosts',
      'MISC', 'core-site', false,  {category : "HDFS", isUserProperty: false, isEditable: false, isOverridable: false}));
    configProperties.pushObject(proxyUserConfig);

    proxyUserConfig.setProperties({'value': rmHosts, 'recommendedValue': rmHosts});

    if (App.Service.find().someProperty('serviceName', 'HAWQ')) {
      var yarnHAPort = 8032;
      var yarnHAHosts = currentRMHost + ':' + yarnHAPort.toString() + ',' + additionalRMHost + ':' + yarnHAPort.toString();
      configProperties.findProperty('name', 'yarn.resourcemanager.ha')
        .set('value', yarnHAHosts)
        .set('recommendedValue', yarnHAHosts);

      var yarnHASchPort = 8030;
      var yarnHASchHosts = currentRMHost + ':' + yarnHASchPort.toString() + ',' + additionalRMHost + ':' + yarnHASchPort.toString();
      configProperties.findProperty('name', 'yarn.resourcemanager.scheduler.ha')
        .set('value', yarnHASchHosts)
        .set('recommendedValue', yarnHASchHosts);
    }
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

