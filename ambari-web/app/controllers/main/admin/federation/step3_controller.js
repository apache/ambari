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

App.NameNodeFederationWizardStep3Controller = Em.Controller.extend(App.BlueprintMixin, {
  name: "nameNodeFederationWizardStep3Controller",
  selectedService: null,
  stepConfigs: [],
  serverConfigData: {},
  federationConfig: {},
  once: false,
  isLoaded: false,
  isConfigsLoaded: false,
  versionLoaded: true,
  hideDependenciesInfoBar: true,

  /**
   * Map of sites and properties to delete
   * @type Object
   */
  configsToRemove: {
    'hdfs-site': ['dfs.namenode.shared.edits.dir', 'dfs.journalnode.edits.dir']
  },

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.set('serverConfigData', {});
    this.set('isConfigsLoaded', false);
    this.set('isLoaded', false);
    this.set('federationConfig', $.extend(true, {}, require('data/configs/wizards/federation_properties').federationConfig))
  },

  loadStep: function () {
    this.clearStep();
    this.loadConfigsTags();
  },

  loadConfigsTags: function () {
    return App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },


  onLoadConfigsTags: function (data) {
    var urlParams = '(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')';
    if (App.Service.find().someProperty('serviceName', 'RANGER')) {
      urlParams += '|(type=ranger-tagsync-site&tag=' + data.Clusters.desired_configs['ranger-tagsync-site'].tag + ')' +
          '|(type=ranger-hdfs-security&tag=' + data.Clusters.desired_configs['ranger-hdfs-security'].tag + ')'
    }
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: urlParams
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    this.set('serverConfigData', data);
    this.set('isConfigsLoaded', true);
  },

  onLoad: function () {
    if (this.get('isConfigsLoaded') && App.router.get('clusterController.isHDFSNameSpacesLoaded')) {
      var federationConfig = this.get('federationConfig');
      federationConfig.configs = this.tweakServiceConfigs(federationConfig.configs);
      this.removeConfigs(this.get('configsToRemove'), this.get('serverConfigData'));
      this.renderServiceConfigs(federationConfig);
      this.set('isLoaded', true);
    }
  }.observes('isConfigsLoaded', 'App.router.clusterController.isHDFSNameSpacesLoaded'),

  prepareDependencies: function () {
    var ret = {};
    var configsFromServer = this.get('serverConfigData.items');
    var journalNodes = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE');
    var nameNodes = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE');
    var hdfsSiteConfigs = configsFromServer.findProperty('type', 'hdfs-site').properties;
    ret.nameservice1 = App.HDFSService.find().objectAt(0).get('masterComponentGroups')[0].name;
    ret.nameservice2 = this.get('content.nameServiceId');
    ret.namenode1 = hdfsSiteConfigs['dfs.namenode.rpc-address.' + ret.nameservice1 + '.nn1'].split(':')[0];
    ret.namenode2 = hdfsSiteConfigs['dfs.namenode.rpc-address.' + ret.nameservice1 + '.nn2'].split(':')[0];
    ret.namenode3 = nameNodes.filterProperty('isInstalled', false).mapProperty('hostName')[0];
    ret.namenode4 = nameNodes.filterProperty('isInstalled', false).mapProperty('hostName')[1];
    ret.journalnodes = journalNodes.map(function (c) {
      return c.get('hostName') + ':8485'
    }).join(';');
    ret.clustername = App.get('clusterName');

    var dfsHttpA = hdfsSiteConfigs['dfs.namenode.http-address'];
    ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(':')[1] : 50070;

    var dfsHttpsA = hdfsSiteConfigs['dfs.namenode.https-address'];
    ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(':')[1] : 50470;

    var dfsRpcA = hdfsSiteConfigs['dfs.namenode.rpc-address'];
    ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(':')[1] : 8020;

    ret.journalnode_edits_dir = hdfsSiteConfigs['dfs.journalnode.edits.dir'];

    if (App.Service.find().someProperty('serviceName', 'RANGER')) {
      var hdfsRangerConfigs = configsFromServer.findProperty('type', 'ranger-hdfs-security').properties;

      if (hdfsRangerConfigs['ranger.plugin.hdfs.service.name'] === '{{repo_name}}') {
        ret.ranger_service_name_ns1 = ret.clustername + '_hadoop_' + ret.nameservice1;
        ret.ranger_service_name_ns2 = ret.clustername + '_hadoop_' + ret.nameservice2;
      } else {
        ret.ranger_service_name_ns1 = hdfsRangerConfigs['ranger.plugin.hdfs.service.name'] + '_' + ret.nameservice1;
        ret.ranger_service_name_ns2 = hdfsRangerConfigs['ranger.plugin.hdfs.service.name'] + '_' + ret.nameservice2;
      }
    }

    return ret;
  },

  tweakServiceConfigs: function (configs) {
    var dependencies = this.prepareDependencies();
    var result = [];
    var configsToRemove = [];
    var hdfsSiteConfigs = this.get('serverConfigData').items.findProperty('type', 'hdfs-site').properties;

    if (!hdfsSiteConfigs['dfs.namenode.servicerpc-address.' + dependencies.nameservice1 + '.nn1'] && !hdfsSiteConfigs['dfs.namenode.servicerpc-address.' + dependencies.nameservice1 + '.nn2']) {
      configsToRemove = configsToRemove.concat([
        'dfs.namenode.servicerpc-address.{{nameservice1}}.nn1',
        'dfs.namenode.servicerpc-address.{{nameservice1}}.nn2',
        'dfs.namenode.servicerpc-address.{{nameservice2}}.nn3',
        'dfs.namenode.servicerpc-address.{{nameservice2}}.nn4'
      ]);
    }

    configs.forEach(function (config) {
      if (!configsToRemove.contains(config.name)) {
        config.isOverridable = false;
        config.name = this.replaceDependencies(config.name, dependencies);
        config.displayName = this.replaceDependencies(config.displayName, dependencies);
        config.value = this.replaceDependencies(config.value, dependencies);
        config.recommendedValue = this.replaceDependencies(config.recommendedValue, dependencies);
        result.push(config);
      }
    }, this);

    return result;
  },

  replaceDependencies: function (value, dependencies) {
    Em.keys(dependencies).forEach(function (key) {
      value = value.replace(new RegExp('{{' + key + '}}', 'g'), dependencies[key]);
    });
    return value;
  },

  removeConfigs: function (configsToRemove, configs) {
    Em.keys(configsToRemove).forEach(function (site) {
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
    this.set('once', true);
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
    }, this);
  },

  isNextDisabled: function () {
    return !this.get('isLoaded') || (this.get('isLoaded') && this.get('selectedService.configs').someProperty('isValid', false));
  }.property('selectedService.configs.@each.isValid', 'isLoaded')
});
