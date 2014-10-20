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
 * Mapper for YARN and ATS
 * Get their statuses and save it to proper models
 * Should be called only from application initializer (and it's already done)
 * @type {Ember.Object}
 */
App.ApplicationStatusMapper = Em.Object.createWithMixins(App.RunPeriodically, {

  /**
   * Map for Service-model
   * @type {Object}
   */
  yarnMap: {
    workStatus: {
      key: 'ServiceInfo.state',
      default: 'UNKNOWN'
    },
    maintenanceState: {
      key: 'ServiceInfo.maintenance_state',
      default: 'OFF'
    },
    id: {
      default: 'YARN'
    }
  },

  /**
   * Map for Component-model
   * @type {Object}
   */
  atsMap: {
    workStatus: {
      key: 'ServiceComponentInfo.state',
      default: 'UNKNOWN'
    },
    componentName: {
      default: 'APP_TIMELINE_SERVER'
    },
    id: {
      default: 'APP_TIMELINE_SERVER'
    }
  },

  /**
   * Is ATS and RESOURCEMANAGER hosts loaded
   * @type {bool}
   */
  hostForComponentIsLoaded: false,

  /**
   * Is <code>ahsWebPort</code> loaded
   * @type {bool}
   */
  portIsLoaded: false,

  /**
   * Array of component names that need to be loaded
   * @type {Array}
   */
  componentsToLoad: [
    "APP_TIMELINE_SERVER",
    "RESOURCEMANAGER"
  ],

  /**
   * Start mapping when <code>App.clusterName</code> is loaded
   * @method mapInit
   */
  mapInit: function() {
    var clusterName = App.get('clusterName');
    if (Em.isNone(clusterName)) return;
    this.loop('map');
  }.observes('App.clusterName'),

  /**
   * Map service and component periodically
   * Map host name for component only once
   * @method map
   */
  map: function() {
    var self = this;
    this.getServices().then(function() {
      self.getComponents().then(function() {
        if (!self.get('hostForComponentIsLoaded'))
          self.get('componentsToLoad').forEach(function (componentName) {
            self.getHostsForComponents(componentName);
          })
        if (!self.get('portIsLoaded'))
          self.getDesiredConfigs();
      });
    });
  },

  /**
   * Get View instance properties provided by user
   * @returns {$.ajax}
   * @method getInstanceParameters
   */
  getInstanceParameters: function () {
    var hashArray = location.pathname.split('/');
    var view = hashArray[2];
    var version = hashArray[3];
    var instanceName = hashArray[4];
    return App.ajax.send({
      name: 'instance_parameters',
      sender: this,
      data: {
        view: view,
        version: version,
        instanceName: instanceName
      },
      success: 'getInstanceParametersSuccessCallback',
      error: 'getInstanceParametersErrorCallback'
    });
  },

  /**
   * Success callback for getInstanceParameters-request
   * @param {object} data
   * @method getInstanceParametersSuccessCallback
   */
  getInstanceParametersSuccessCallback: function (data) {
    var atsURLParameter = data.parameters['yarn.ats.url'];
    var resourceManagerURLParameter = data.parameters['yarn.resourcemanager.url'];
    if (atsURLParameter) {
      App.set('atsURL', atsURLParameter);
      App.set('resourceManagerURL', resourceManagerURLParameter);
    } else {
      this.getClusterName();
    }
  },

  /**
   * Success callback for getInstanceParameters-request
   * @method getInstanceParametersErrorCallback
   */
  getInstanceParametersErrorCallback: function () {
    this.getClusterName();
  },

  /**
   * Get cluster name from server
   * @returns {$.ajax}
   * @method getClusterName
   */
  getClusterName: function() {
    return App.ajax.send({
      name: 'cluster_name',
      sender: this,
      success: 'getClusterNameSuccessCallback'
    });
  },

  /**
   * Success callback for clusterName-request
   * @param {object} data
   * @method getClusterNameSuccessCallback
   */
  getClusterNameSuccessCallback: function(data) {
    App.set('clusterName', Em.get(data.items[0], 'Clusters.cluster_name'));
  },

  /**
   * Get list of installed services (YARN is needed)
   * @returns {$.ajax}
   * @method getServices
   */
  getServices: function() {
    return App.ajax.send({
      name: 'services',
      sender: this,
      success: 'getServicesSuccessCallback',
      error: 'getServicesErrorCallback'
    });
  },

  /**
   * Success callback for services-request
   * Map YARN-service to model (if YARN not available - save empty object)
   * @param {Object} data
   * @method getServicesSuccessCallback
   */
  getServicesSuccessCallback: function(data) {
    var map = this.get('yarnMap'),
      yarn = data.items.findBy('ServiceInfo.service_name', 'YARN'),
      yarnModel = Em.isNone(yarn) ? {id: 'YARN'} : Em.JsonMapper.map(yarn, map);
    App.HiveJob.store.push('service', yarnModel);
  },

  /**
   * Error callback for services-request
   * Save empty object to model
   * @method getServicesErrorCallback
   */
  getServicesErrorCallback: function() {
    App.HiveJob.store.push('yarn', {id: 'YARN'});
  },

  /**
   * Get list of components from server
   * @returns {$.ajax}
   * @method getComponents
   */
  getComponents: function() {
    return App.ajax.send({
      name: 'components',
      sender: this,
      success: 'getComponentsSuccessCallback',
      error: 'getComponentsErrorCallback'
    });
  },

  /**
   * Success callback for components-request
   * Save ATS to model (if ATS not available - save empty object)
   * @param {object} data
   * @method getComponentsSuccessCallback
   */
  getComponentsSuccessCallback: function(data) {
    var map = this.get('atsMap'),
      ats = data.items.findBy('ServiceComponentInfo.component_name', 'APP_TIMELINE_SERVER'),
      atsModel = Em.isNone(ats) ? {id: 'APP_TIMELINE_SERVER'} : Em.JsonMapper.map(ats, map);
    App.HiveJob.store.push('component', atsModel);
    App.HiveJob.store.push('component', {id: 'RESOURCEMANAGER'});
  },

  /**
   * Error callback for components-request
   * Save empty object to model
   * @method getComponentsErrorCallback
   */
  getComponentsErrorCallback: function() {
    App.HiveJob.store.push('component', {id: 'APP_TIMELINE_SERVER'});
    App.HiveJob.store.push('component', {id: 'RESOURCEMANAGER'});
  },

  /**
   * Get host name for ATS
   * @returns {$.ajax}
   * @method getHostsForComponents
   */
  getHostsForComponents: function(componentName) {
    return App.ajax.send({
      name: 'components_hosts',
      sender: this,
      data: {
        componentName: componentName
      },
      success: 'getHostsForComponentsSuccessCallback'
    });
  },

  /**
   * Success callback for hosts-request
   * Save host name to ATS-model
   * @param {Object} data
   * @method getHostsForComponentsSuccessCallback
   */
  getHostsForComponentsSuccessCallback: function(data) {
    App.HiveJob.store.getById('component', arguments[2].componentName).set('hostName', Em.get(data.items[0], 'Hosts.host_name'));
    this.set('componentsToLoad', this.get('componentsToLoad').without(arguments[2].componentName))

    if(this.get('componentsToLoad').length === 0){
      this.set('hostForComponentIsLoaded', true);
    }
  },

  /**
   * Get Ambari desired configs
   * @returns {$.ajax}
   * @method getDesiredConfigs
   */
  getDesiredConfigs: function() {
    return App.ajax.send({
      name: 'config_tags',
      sender: this,
      success: 'getDesiredConfigsSuccessCallback'
    });
  },

  /**
   * Success callback for Ambari desired configs request
   * Make request for YARN configs
   * @param {object} data
   * @returns {$.ajax|null}
   * @method getDesiredConfigsSuccessCallback
   */
  getDesiredConfigsSuccessCallback: function(data) {
    var c = Em.get(data, 'Clusters.desired_configs')['yarn-site'];
    if (!Em.isNone(c)) {
      return this.getConfigurations(c);
    }
  },

  /**
   * Get YARN configs
   * @param {{user: string, tag: string}} config
   * @returns {$.ajax}
   * @method getConfigurations
   */
  getConfigurations: function(config) {
    return App.ajax.send({
      name: 'configurations',
      sender: this,
      data: {
        params: '(type=yarn-site&tag=%@1)'.fmt(config.tag)
      },
      success: 'getConfigurationSuccessCallback'
    });
  },

  /**
   * Success callback for YARN configs
   * Set <code>ahsWebPort</code> property using <code>yarn.timeline-service.webapp.address</code> or '8188' as default
   * @param {object} data
   * @method getConfigurationSuccessCallback
   */
  getConfigurationSuccessCallback: function(data) {
    var c = data.items.findBy('type', 'yarn-site');
    if (!Em.isNone(c)) {
      var properties = Em.get(c, 'properties'),
        port = '8188';
      if (!Em.isNone(properties)) {
        port = properties['yarn.timeline-service.webapp.address'].match(/:(\d+)/)[1];
      }
      App.HiveJob.store.getById('service', 'YARN').set('ahsWebPort', port);
      this.set('portIsLoaded', true);
    }
  }

});
