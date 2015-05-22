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
    'hdfs-site': ['dfs.namenode.secondary.http-address']
  },

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.serverConfigData = {};
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
    urlParams.push('(type=hdfs-site&tag=' + hdfsSiteTag + ')');
    urlParams.push('(type=core-site&tag=' + coreSiteTag + ')');
    this.set("hdfsSiteTag", {name : "hdfsSiteTag", value : hdfsSiteTag});
    this.set("coreSiteTag", {name : "coreSiteTag", value : coreSiteTag});

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


  tweakServiceConfigs: function(configs) {
    var nameServiceId = this.get('content.nameServiceId');
    var nameServiceConfig = configs.findProperty('name','dfs.nameservices');
    this.setConfigInitialValue(nameServiceConfig,nameServiceId);
    var defaultFsConfig = configs.findProperty('name','fs.defaultFS');
    this.setConfigInitialValue(defaultFsConfig, "hdfs://" + nameServiceId);
    this.tweakServiceConfigNames(configs,nameServiceId);
    this.tweakServiceConfigValues(configs,nameServiceId);
  },

  tweakServiceConfigNames: function(configs,nameServiceId) {
    var regex = new RegExp("\\$\\{dfs.nameservices\\}","g");
    configs.forEach(function(config) {
      if (config.name.contains("${dfs.nameservices}")) {
        config.name = config.name.replace(regex,nameServiceId);
        config.displayName = config.displayName.replace(regex,nameServiceId);
      }
    }, this);
  },

  tweakServiceConfigValues: function(configs,nameServiceId) {
    var currentNameNodeHost = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
    var newNameNodeHost = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).hostName;
    var journalNodeHosts = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    var zooKeeperHosts = this.get('content.masterComponentHosts').filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
    var config = configs.findProperty('name','dfs.namenode.rpc-address.' + nameServiceId + '.nn1');
    this.setConfigInitialValue(config,currentNameNodeHost + ':8020');
    config = configs.findProperty('name','dfs.namenode.rpc-address.' + nameServiceId + '.nn2');
    this.setConfigInitialValue(config,newNameNodeHost + ':8020');
    config = configs.findProperty('name','dfs.namenode.http-address.' + nameServiceId + '.nn1');
    this.setConfigInitialValue(config,currentNameNodeHost + ':50070');
    config = configs.findProperty('name','dfs.namenode.http-address.' + nameServiceId + '.nn2');
    this.setConfigInitialValue(config,newNameNodeHost + ':50070');
    config = configs.findProperty('name','dfs.namenode.https-address.' + nameServiceId + '.nn1');
    this.setConfigInitialValue(config,currentNameNodeHost + ':50470');
    config = configs.findProperty('name','dfs.namenode.https-address.' + nameServiceId + '.nn2');
    this.setConfigInitialValue(config,newNameNodeHost + ':50470');
    config = configs.findProperty('name','dfs.namenode.shared.edits.dir');
    this.setConfigInitialValue(config,'qjournal://' + journalNodeHosts[0] + ':8485;' + journalNodeHosts[1] + ':8485;' + journalNodeHosts[2] + ':8485/' + nameServiceId);
    config = configs.findProperty('name','ha.zookeeper.quorum');
    this.setConfigInitialValue(config,zooKeeperHosts[0] + ':2181,' + zooKeeperHosts[1] + ':2181,' + zooKeeperHosts[2] + ':2181');
    config = configs.findProperty('name','hbase.rootdir');
    if (App.Service.find().someProperty('serviceName', 'HBASE')) {
     var value = this.get('serverConfigData.items').findProperty('type', 'hbase-site').properties['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + nameServiceId);
     this.setConfigInitialValue(config,value);
    }
    config = configs.findProperty('name','instance.volumes');
    var config2 = configs.findProperty('name','instance.volumes.replacements');
    if (App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
      var oldValue = this.get('serverConfigData.items').findProperty('type', 'accumulo-site').properties['instance.volumes'];
      var value = oldValue.replace(/\/\/[^\/]*/, '//' + nameServiceId);
      var replacements = oldValue + " " + value;
      this.setConfigInitialValue(config,value);
      this.setConfigInitialValue(config2,replacements)
    }
    config = configs.findProperty('name','dfs.journalnode.edits.dir');
    if (App.get('isHadoopWindowsStack') && App.Service.find().someProperty('serviceName', 'HDFS')) {
     var value = this.get('serverConfigData.items').findProperty('type', 'hdfs-site').properties['dfs.journalnode.edits.dir'];
     this.setConfigInitialValue(config, value);
    }
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

  setConfigInitialValue: function(config,value) {
    config.value = value;
    config.recommendedValue = value;
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

  isNextDisabled: function () {
    return !this.get('isLoaded');
  }.property('isLoaded')

});

