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
        self.getHostsForComponents();
      });
    });
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
  },

  /**
   * Error callback for components-request
   * Save empty object to model
   * @method getComponentsErrorCallback
   */
  getComponentsErrorCallback: function() {
    App.HiveJob.store.push('component', {id: 'APP_TIMELINE_SERVER'});
  },

  /**
   * Get host name for ATS
   * @returns {$.ajax}
   * @method getHostsForComponents
   */
  getHostsForComponents: function() {
    if (!App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER').get('hostName')) {
      return App.ajax.send({
        name: 'components_hosts',
        sender: this,
        data: {
          componentName: 'APP_TIMELINE_SERVER'
        },
        success: 'getHostsForComponentsSuccessCallback'
      });
    }
  },

  /**
   * Success callback for hosts-request
   * Save host name to ATS-model
   * @param {Object} data
   * @method getHostsForComponentsSuccessCallback
   */
  getHostsForComponentsSuccessCallback: function(data) {
    App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER').set('hostName', Em.get(data.items[0], 'Hosts.host_name'));
  }

});