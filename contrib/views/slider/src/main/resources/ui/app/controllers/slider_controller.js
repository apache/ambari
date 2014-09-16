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
 * Checks Slider-properties.
 * If they are not available, uses Ambari-configs to populate them:
 *  - Load cluster name
 *  - Load hostName for GANGLIA_SERVER
 *  - Load hostName for NAGIOS_SERVER
 *  - Load hostNames for ZOOKEEPER_SERVER
 *  - Load config tags
 *  - Load configs
 *  - Save Slider-properties
 * If Slider-properties exists:
 *  - Load cluster name
 *  - Load hostNames
 * After whole data has been loaded set <code>App.sliderConfigs</code> and enable/disable Slider
 * @type {Ember.Controller}
 */
App.SliderController = Ember.Controller.extend(App.RunPeriodically, {

  /**
   * Map for Slider-errors
   * If some config is empty, service isn't installed
   * @type {object}
   */
  serviceConfigMap: {
    HDFS: 'hdfsAddress',
    YARN: 'yarnResourceManager',
    ZOOKEEPER: 'zookeeperQuorum'
  },

  /**
   * List of Slider-properties mapped from Ambari-configs
   * Key-names used in Slider-Title-Popup, so don't change it pls
   * @type {Em.Object}
   */
  initialValuesToLoad: Em.Object.create({
    ambariAddress: null,
    clusterName: null,
    hdfsAddress: null,
    yarnResourceManager: null,
    yarnResourceManagerScheduler: null,
    zookeeperQuorum: null,
    gangliaServer: null,
    gangliaClusters: null
  }),

  /**
   * List of host names with ZOOKEEPER_SERVER installed
   * @type {string[]}
   */
  zookeeperHosts: [],

  /**
   *  Load resources on controller initialization
   * @method initResources
   */
  initResources: function () {
    this.getParametersFromViewProperties();
  },

  /**
   * Get Slider properties from View-parameters (set in the Ambari Admin View)
   * If parameters can't be found, use Ambari-configs to populate Slider properties
   * @returns {$.ajax}
   * @method getParametersFromViewProperties
   */
  getParametersFromViewProperties: function() {
    return App.ajax.send({
      name: 'slider.getViewParams',
      sender: this,
      success: 'getParametersFromViewPropertiesSuccessCallback',
      error: 'getParametersFromViewPropertiesErrorCallback'
    });
  },

  /**
   * Check if Slider-properties exist
   * If exist - set Slider properties using view-configs
   * If not - get Ambari configs to populate Slider properties
   * @param {object} data
   * @method getParametersFromViewPropertiesSuccessCallback
   */
  getParametersFromViewPropertiesSuccessCallback: function(data) {
    var properties = Em.get(data, 'ViewInstanceInfo.properties'),
      initialValuesToLoad = this.get('initialValuesToLoad');
    if (Em.isEmpty(properties)) {
      this.getClusterName();
    }
    else {
      initialValuesToLoad.setProperties({
        ambariAddress: location.protocol + "//" + document.location.host,
        hdfsAddress: properties['hdfs.address'],
        yarnResourceManager: properties['yarn.resourcemanager.address'],
        yarnResourceManagerScheduler: properties['yarn.resourcemanager.scheduler.address'],
        zookeeperQuorum: properties['zookeeper.quorum'],
        gangliaServer: properties['ganglia.server.hostname'],
        gangliaClusters: properties['ganglia.custom.clusters']
      });
      App.set('gangliaHost', properties['ganglia.server.hostname']);
      this.finishSliderConfiguration();
    }
  },

  /**
   * Error-callback for Slider-parameters request
   * @method getParametersFromViewPropertiesErrorCallback
   */
  getParametersFromViewPropertiesErrorCallback: function() {
    this.getClusterName();
  },

  /**
   * Get cluster name from server
   * @returns {$.ajax}
   * @method getClusterName
   */
  getClusterName: function () {
    return App.ajax.send({
      name: 'cluster_name',
      sender: this,
      data: {
        urlPrefix: '/api/v1/'
      },
      success: 'getClusterNameSuccessCallback'
    });
  },

  /**
   * Success callback for clusterName-request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method getClusterNameSuccessCallback
   */
  getClusterNameSuccessCallback: function (data, opt, params) {
    var clusterName = Em.get(data.items[0], 'Clusters.cluster_name');
    App.set('clusterName', clusterName);
    App.ApplicationStatusMapper.loop('load');
    this.loadComponentHost({componentName: "GANGLIA_SERVER", callback: "loadGangliaHostSuccessCallback"});
    this.loadComponentHost({componentName: "NAGIOS_SERVER", callback: "loadNagiosHostSuccessCallback"});
    this.loadComponentHost({componentName: "ZOOKEEPER_SERVER", callback: "setZookeeperQuorum"});
    this.loadConfigsTags();
  },

  /**
   * Load config tags from server
   * @returns {$.ajax}
   * @method loadConfigsTags
   */
  loadConfigsTags: function () {
    return App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        urlPrefix: '/api/v1/'
      },
      success: 'onLoadConfigsTags'
    });
  },

  /**
   * Success callback for <code>loadConfigsTags</code>
   * Get configs for selected tags
   * @param {object} data
   * @method onLoadConfigsTags
   */
  onLoadConfigsTags: function (data) {
    var urlParams = [];
    if (data.Clusters.desired_configs['yarn-site'] && data.Clusters.desired_configs['zookeeper-env']) {
      var coreSiteTag = data.Clusters.desired_configs['core-site'].tag;
      var yarnSiteTag = data.Clusters.desired_configs['yarn-site'].tag;
      var zookeeperTag = data.Clusters.desired_configs['zookeeper-env'].tag;
      urlParams.push('(type=core-site&tag=' + coreSiteTag + ')');
      urlParams.push('(type=yarn-site&tag=' + yarnSiteTag + ')');
      urlParams.push('(type=zookeeper-env&tag=' + zookeeperTag + ')');

      App.ajax.send({
        name: 'get_all_configurations',
        sender: this,
        data: {
          urlParams: urlParams.join('|'),
          urlPrefix: '/api/v1/'
        },
        success: 'onLoadConfigs'
      });
    }
  },

  /**
   * Success callback for <code>onLoadConfigs</code>
   * Set properties for <code>initialValuesToLoad</code> using loaded configs
   * @param {object} data
   * @method onLoadConfigs
   */
  onLoadConfigs: function (data) {
    var hdfs = data.items.findProperty('type', 'core-site'),
      yarn = data.items.findProperty('type', 'yarn-site'),
      zookeeper = data.items.findProperty('type', 'zookeeper-env'),
      initialValuesToLoad = this.get('initialValuesToLoad');
    initialValuesToLoad.set('ambariAddress', location.protocol + "//" + document.location.host);
    initialValuesToLoad.set('clusterName', App.get('clusterName'));
    initialValuesToLoad.set('hdfsAddress', hdfs.properties['fs.defaultFS']);
    initialValuesToLoad.set('yarnResourceManager', yarn.properties['yarn.resourcemanager.address']);
    initialValuesToLoad.set('yarnResourceManagerScheduler', yarn.properties['yarn.resourcemanager.scheduler.address']);
    initialValuesToLoad.set('zookeeperQuorum', zookeeper.properties.clientPort);
    this.setZookeeperQuorum();
  },

  /**
   * Set value for <code>initialValuesToLoad.zookeeperQuorum</code>
   * Also do request to save Slider-properties
   * @param {object} data
   * @method setZookeeperQuorum
   */
  setZookeeperQuorum: function (data) {
    var zookeeperHosts = this.get('zookeeperHosts'),
      hosts = [],
      initialValuesToLoad = this.get('initialValuesToLoad');

    //done
    if (!Em.isNone(initialValuesToLoad.zookeeperQuorum)) {
      if (data) {
        hosts = data.items.map(function (item) {
          return item.Hosts.host_name + ":" + initialValuesToLoad.zookeeperQuorum;
        });
        initialValuesToLoad.set('zookeeperQuorum', hosts.join(','));
        this.sendInitialValues();
      }
      else {
        if (zookeeperHosts.length > 0) {
          hosts = zookeeperHosts.map(function (host) {
            return host + ":" + initialValuesToLoad.zookeeperQuorum;
          });
          initialValuesToLoad.set('zookeeperQuorum', hosts.join(','));
          this.sendInitialValues();
        }
      }
    }
    else {
      this.set('zookeeperHosts', data.items.mapProperty('Hosts.host_name'));
    }
  },

  /**
   * Send request to server to save initialValues
   * @return {$.ajax}
   * @method sendInitialValues
   */
  sendInitialValues: function () {
    var initialValues = this.get('initialValuesToLoad');
    return App.ajax.send({
      name: 'saveInitialValues',
      sender: this,
      data: {
        data: {
          ViewInstanceInfo: {
            properties: {
              'hdfs.address': initialValues.get('hdfsAddress'),
              'yarn.resourcemanager.address': initialValues.get('yarnResourceManager'),
              'yarn.resourcemanager.scheduler.address': initialValues.get('yarnResourceManagerScheduler'),
              'zookeeper.quorum': initialValues.get('zookeeperQuorum')
            }
          }
        }
      },
      success: 'finishSliderConfiguration'
    });
  },

  /**
   * After all Slider-configs are loaded, application should check self status
   * @method finishSliderConfiguration
   */
  finishSliderConfiguration: function() {
    //check if all services exist
    var serviceConfigMap = this.get('serviceConfigMap'),
      initialValuesToLoad = this.get('initialValuesToLoad'),
      services = Em.keys(serviceConfigMap),
      errors = [];
    services.forEach(function(serviceName) {
      var configName = Em.get(serviceConfigMap, serviceName);
      if (Em.isEmpty(initialValuesToLoad[configName])) {
        errors.push(Em.I18n.t('error.no' + serviceName));
      }
    });
    App.setProperties({
      viewErrors: errors,
      viewEnabled: errors.length === 0,
      sliderConfigs: initialValuesToLoad,
      mapperTime: new Date().getTime()
    });
  },

  /**
   * Load host for component
   * @param {{componentName: string, callback: string}} params
   * @return {$.ajax}
   * @method loadGangliaHost
   */
  loadComponentHost: function (params) {
    return App.ajax.send({
      name: 'components_hosts',
      sender: this,
      data: {
        componentName: params.componentName,
        urlPrefix: '/api/v1/'
      },
      success: params.callback
    });

  },

  /**
   * Success callback for hosts-request
   * Save host name to GANGLIA_SERVER (set in <code>App.gangliaHost</code>)
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadGangliaHostSuccessCallback: function (data) {
    if (data.items[0]) {
      App.set('gangliaHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  },

  /**
   * Success callback for hosts-request
   * Save host name to NAGIOS_SERVER (set in <code>App.nagiosHost</code>)
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadNagiosHostSuccessCallback: function (data) {
    if (data.items[0]) {
      App.set('nagiosHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  }

});
