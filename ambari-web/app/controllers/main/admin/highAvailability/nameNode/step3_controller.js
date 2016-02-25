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

App.HighAvailabilityWizardStep3Controller = Em.Controller.extend({
  name: "highAvailabilityWizardStep3Controller",
  selectedService: null,
  stepConfigs: [],
  serverConfigData: {},
  haConfig: $.extend(true, {}, require('data/HDP2/ha_properties').haConfig),
  once: false,
  isLoaded: false,
  versionLoaded: true,
  hideDependenciesInfoBar: true,

  /**
   * Map of sites and properties to delete
   * @type Object
   */
  configsToRemove: {
    'hdfs-site': ['dfs.namenode.secondary.http-address', 'dfs.namenode.rpc-address', 'dfs.namenode.http-address', 'dfs.namenode.https-address']
  },

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
    var coreSiteTag = data.Clusters.desired_configs['core-site'].tag;
    var zkSiteTag = data.Clusters.desired_configs['zoo.cfg'].tag;
    urlParams.push('(type=hdfs-site&tag=' + hdfsSiteTag + ')');
    urlParams.push('(type=core-site&tag=' + coreSiteTag + ')');
    urlParams.push('(type=zoo.cfg&tag=' + zkSiteTag  + ')');
    this.set("hdfsSiteTag", {name : "hdfsSiteTag", value : hdfsSiteTag});
    this.set("coreSiteTag", {name : "coreSiteTag", value : coreSiteTag});
    this.set("zkSiteTag", {name : "zkSiteTag", value : zkSiteTag});

    if (App.Service.find().someProperty('serviceName', 'HBASE')) {
      var hbaseSiteTag = data.Clusters.desired_configs['hbase-site'].tag;
      urlParams.push('(type=hbase-site&tag=' + hbaseSiteTag + ')');
      this.set("hbaseSiteTag", {name : "hbaseSiteTag", value : hbaseSiteTag});
    }
    if (App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
      var accumuloSiteTag = data.Clusters.desired_configs['accumulo-site'].tag;
      urlParams.push('(type=accumulo-site&tag=' + accumuloSiteTag + ')');
      this.set("accumuloSiteTag", {name : "accumuloSiteTag", value : accumuloSiteTag});
    }
    if (App.Service.find().someProperty('serviceName', 'AMBARI_METRICS')) {
      var amsHbaseSiteTag = data.Clusters.desired_configs['ams-hbase-site'].tag;
      urlParams.push('(type=ams-hbase-site&tag=' + amsHbaseSiteTag + ')');
      this.set("amsHbaseSiteTag", {name : "amsHbaseSiteTag", value : amsHbaseSiteTag});
    }
    if (App.Service.find().someProperty('serviceName', 'HAWQ')) {
      var hawqSiteTag = data.Clusters.desired_configs['hawq-site'].tag;
      urlParams.push('(type=hawq-site&tag=' + hawqSiteTag + ')');
      this.set("hawqSiteTag", {name : "hawqSiteTag", value : hawqSiteTag});
      var hdfsClientTag = data.Clusters.desired_configs['hdfs-client'].tag;
      urlParams.push('(type=hdfs-client&tag=' + hdfsClientTag + ')');
      this.set("hdfsClientTag", {name : "hdfsClientTag", value : hdfsClientTag});
    }
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
    this.set('serverConfigData',data);
    this.removeConfigs(this.get('configsToRemove'), this.get('serverConfigData'));
    this.tweakServiceConfigs(this.get('haConfig.configs'));
    this.renderServiceConfigs(this.get('haConfig'));
    this.set('isLoaded', true);
  },

  /**
   * Generate set of data used to correctly initialize config values and names
   *
   * @returns {nnHaConfigDependencies}
   * @private
   * @method _prepareDependencies
   */
  _prepareDependencies: function () {
    var ret = {};
    var configsFromServer = this.get('serverConfigData.items');
    ret.namespaceId = this.get('content.nameServiceId');
    ret.serverConfigs = configsFromServer;
    var hdfsConfigs = configsFromServer.findProperty('type','hdfs-site').properties;
    var zkConfigs = configsFromServer.findProperty('type','zoo.cfg').properties;

    var dfsHttpA = hdfsConfigs['dfs.namenode.http-address'];
    ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(':')[1] : 50070;

    var dfsHttpsA = hdfsConfigs['dfs.namenode.https-address'];
    ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(':')[1] : 50470;

    var dfsRpcA = hdfsConfigs['dfs.namenode.rpc-address'];
    ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(':')[1] : 8020;

    ret.zkClientPort = zkConfigs['clientPort'] ? zkConfigs['clientPort'] : 2181;

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

  tweakServiceConfigs: function(configs) {
    var localDB = this._prepareLocalDB();
    var dependencies = this._prepareDependencies();

    configs.forEach(function (config) {
      App.NnHaConfigInitializer.initialValue(config, localDB, dependencies);
      config.isOverridable = false;
    });

    return configs;
  },

  /**
   * Find and remove config properties in <code>serverConfigData</code>
   * @param configsToRemove - map of config sites and properties to remove
   * @param configs - configuration object
   * @returns {Object}
   */
  removeConfigs:function (configsToRemove, configs) {
    Em.keys(configsToRemove).forEach(function(site){
      var siteConfigs = configs.items.findProperty('type', site);
      if (siteConfigs) {
        configsToRemove[site].forEach(function (property) {
          delete siteConfigs.properties[property];
        });
      }
    });
    return configs;
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
    this.once = true;
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
      serviceConfigProperty.validate();
    }, this);
  },

  isNextDisabled: Em.computed.not('isLoaded')

});

