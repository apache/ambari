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
var blueprintUtils = require('utils/blueprint');
//should handle
require('utils/configs/multiple_nn_config_initializer');

App.MultipleNameNodeWizardStep3Controller = Em.Controller.extend(App.BlueprintMixin, {
    name: "multipleNameNodeWizardStep3Controller",

    selectedService: null,
    stepConfigs: [],
    serverConfigData: {},
    haConfig: $.extend(true, {}, require('data/configs/wizards/multiple_nn_haproperties').haConfig),
    once: false,
    isLoaded: false,
    isNextDisabled: Em.computed.not('isLoaded'),
    versionLoaded: true,

    hideDependenciesInfoBar: true,

    /**
     * Map of sites and properties to delete
     * @type Object
     */
    configsToRemove: {
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
        return App.ajax.send({
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
        if (App.Service.find().someProperty('serviceName', 'AMBARI_METRICS')) {
            var amsHbaseSiteTag = data.Clusters.desired_configs['ams-hbase-site'].tag;
            urlParams.push('(type=ams-hbase-site&tag=' + amsHbaseSiteTag + ')');
            this.set("amsHbaseSiteTag", {name : "amsHbaseSiteTag", value : amsHbaseSiteTag});
        }
        if(App.Service.find().someProperty('serviceName', 'RANGER')) {
            var rangerEnvTag = data.Clusters.desired_configs['ranger-env'].tag;
            urlParams.push('(type=ranger-env&tag=' + rangerEnvTag  + ')');
            this.set("rangerEnvTag", {name : "rangerEnvTag", value : rangerEnvTag});
            if('ranger-hdfs-plugin-properties' in data.Clusters.desired_configs) {
                var rangerHdfsPluginPropertiesTag = data.Clusters.desired_configs['ranger-hdfs-plugin-properties'].tag;
                urlParams.push('(type=ranger-hdfs-plugin-properties&tag=' + rangerHdfsPluginPropertiesTag + ')');
                this.set("rangerHdfsPluginPropertiesTag", {
                    name: "rangerHdfsPluginPropertiesTag",
                    value: rangerHdfsPluginPropertiesTag
                });
            }
            if('ranger-hdfs-audit' in data.Clusters.desired_configs) {
                var rangerHdfsAuditTag = data.Clusters.desired_configs['ranger-hdfs-audit'].tag;
                urlParams.push('(type=ranger-hdfs-audit&tag=' + rangerHdfsAuditTag + ')');
                this.set("rangerHdfsAuditTag", {name: "rangerHdfsAuditTag", value: rangerHdfsAuditTag});
            }
            if('ranger-yarn-audit' in data.Clusters.desired_configs) {
                var yarnAuditTag = data.Clusters.desired_configs['ranger-yarn-audit'].tag;
                urlParams.push('(type=ranger-yarn-audit&tag=' + yarnAuditTag + ')');
                this.set("yarnAuditTag", {name: "yarnAuditTag", value: yarnAuditTag});
            }
            if (App.Service.find().someProperty('serviceName', 'HBASE')) {
                if('ranger-hbase-audit' in data.Clusters.desired_configs) {
                    var rangerHbaseAuditTag = data.Clusters.desired_configs['ranger-hbase-audit'].tag;
                    urlParams.push('(type=ranger-hbase-audit&tag=' + rangerHbaseAuditTag + ')');
                    this.set("rangerHbaseAuditTag", {name: "rangerHbaseAuditTag", value: rangerHbaseAuditTag});
                }
                if('ranger-hbase-plugin-properties' in data.Clusters.desired_configs) {
                    var rangerHbasePluginPropertiesTag = data.Clusters.desired_configs['ranger-hbase-plugin-properties'].tag;
                    urlParams.push('(type=ranger-hbase-plugin-properties&tag=' + rangerHbasePluginPropertiesTag + ')');
                    this.set("rangerHbasePluginPropertiesTag", {
                        name: "rangerHbasePluginPropertiesTag",
                        value: rangerHbasePluginPropertiesTag
                    });
                }
            }
            if (App.Service.find().someProperty('serviceName', 'KAFKA')) {
                if('ranger-kafka-audit' in data.Clusters.desired_configs) {
                    var rangerKafkaAuditTag = data.Clusters.desired_configs['ranger-kafka-audit'].tag;
                    urlParams.push('(type=ranger-kafka-audit&tag=' + rangerKafkaAuditTag + ')');
                    this.set("rangerKafkaAuditTag", {name: "rangerKafkaAuditTag", value: rangerKafkaAuditTag});
                }
            }
            if (App.Service.find().someProperty('serviceName', 'HIVE')) {
                if('ranger-hive-audit' in data.Clusters.desired_configs) {
                    var rangerHiveAuditTag = data.Clusters.desired_configs['ranger-hive-audit'].tag;
                    urlParams.push('(type=ranger-hive-audit&tag=' + rangerHiveAuditTag + ')');
                    this.set("rangerHiveAuditTag", {name: "rangerHiveAuditTag", value: rangerHiveAuditTag});
                }
                if('ranger-hive-plugin-properties' in data.Clusters.desired_configs) {
                    var rangerHivePluginPropertiesTag = data.Clusters.desired_configs['ranger-hive-plugin-properties'].tag;
                    urlParams.push('(type=ranger-hive-plugin-properties&tag=' + rangerHivePluginPropertiesTag + ')');
                    this.set("rangerHivePluginPropertiesTag", {
                        name: "rangerHivePluginPropertiesTag",
                        value: rangerHivePluginPropertiesTag
                    });
                }
            }
            if (App.Service.find().someProperty('serviceName', 'RANGER_KMS')) {
                if('ranger-kms-audit' in data.Clusters.desired_configs) {
                    var rangerKMSAuditTag = data.Clusters.desired_configs['ranger-kms-audit'].tag;
                    urlParams.push('(type=ranger-kms-audit&tag=' + rangerKMSAuditTag + ')');
                    this.set("rangerKMSAuditTag", {name: "rangerKMSAuditTag", value: rangerKMSAuditTag});
                }
            }
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

    onLoadConfigs: function onLoadConfigs(data) {
        var self = this;
        this.set('serverConfigData', data);
        this.removeConfigs(this.get('configsToRemove'), data);
        this.tweakServiceConfigs(this.get('haConfig.configs'));
        this.renderServiceConfigs(this.get('haConfig'));
        //Construct Configs Object
        var siteNames = ['hdfs-site'];
        var configsFromServer = this.get('serverConfigData.items');
        var hdfsConfigs = configsFromServer.findProperty('type', 'hdfs-site');
        var configToSave = {
            type: 'hdfs-site',
            properties: hdfsConfigs && hdfsConfigs.properties
        };
        if (hdfsConfigs && hdfsConfigs.properties_attributes) {
            configToSave.properties_attributes = hdfsConfigs.properties_attributes;
        }
        for(const property of this.get('haConfig.configs')){
            configToSave.properties[property.name]=property.value
        }
        App.ajax.send({
            name: 'common.service.configurations',
            sender: self,
            data: {
                desired_config: configToSave
            },
            error: 'onTaskError'
        });

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
        ret.serverConfigs = configsFromServer;
        var hdfsConfigs = configsFromServer.findProperty('type', 'hdfs-site').properties;
        var zkConfigs = configsFromServer.findProperty('type', 'zoo.cfg').properties;
        var nameNodes = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE');
        ret.newNamenodeIndex = 'nn' + nameNodes.length;
        ret.newNameNode = nameNodes.filterProperty('isInstalled', false).mapProperty('hostName')[0];
        var listNameNodes = [];
        for(let i=0 ;i<nameNodes.length; i++){
            listNameNodes.push(`nn${i + 1}`);

        }
        ret.listNameNodes = listNameNodes.join(',');

        var dfsHttpA = hdfsConfigs['dfs.namenode.http-address'];
        ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(':')[1] : 50070;

        var dfsHttpsA = hdfsConfigs['dfs.namenode.https-address'];
        ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(':')[1] : 50470;

        var dfsRpcA = hdfsConfigs['dfs.namenode.rpc-address'];
        ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(':')[1] : 8020;

        ret.zkClientPort = zkConfigs['clientPort'] ? zkConfigs['clientPort'] : 2181;

        ret.namespaceId = hdfsConfigs['dfs.nameservices'];

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

    tweakServiceConfigs: function (configs) {
        var localDB = this._prepareLocalDB();
        var dependencies = this._prepareDependencies();

        var result = [];
        var configsToRemove = [];
        var hdfsSiteConfigs = this.get('serverConfigData').items.findProperty('type', 'hdfs-site').properties;
        var wizardController = App.router.get(this.get('content.controllerName'));
        configs.forEach(function (config) {
            config.isOverridable = false;
            config.name = wizardController.replaceDependencies(config.name, dependencies);
            config.displayName = wizardController.replaceDependencies(config.displayName, dependencies);
            config.value = wizardController.replaceDependencies(config.value, dependencies);
            config.recommendedValue = wizardController.replaceDependencies(config.recommendedValue, dependencies);
            result.push(config);

        }, this);

        return configs;
    },

    /**
     * Find and remove config properties in <code>serverConfigData</code>
     * @param configsToRemove - map of config sites and properties to remove
     * @param configs - configuration object
     * @returns {Object}
     */
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
    }
});
