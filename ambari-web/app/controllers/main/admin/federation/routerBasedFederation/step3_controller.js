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

App.RouterFederationWizardStep3Controller = Em.Controller.extend(App.BlueprintMixin, {
    name: "routerFederationWizardStep3Controller",
    selectedService: null,
    stepConfigs: [],
    serverConfigData: {},
    once: false,
    isLoaded: false,
    isConfigsLoaded: false,
    versionLoaded: true,
    hideDependenciesInfoBar: true,

    /**
     * Map of sites and properties to delete
     * @type Object
     */

    clearStep: function () {
        this.get('stepConfigs').clear();
        this.set('serverConfigData', {});
        this.set('isConfigsLoaded', false);
        this.set('isLoaded', false);
    },

    loadStep: function () {
        this.clearStep();
        this.loadConfigsTags();
    },

    loadConfigsTags: function () {
        return App.ajax.send({
            name: 'config.tags',
            sender: this,
            success: 'onLoadConfigsTags'
        });
    },


    onLoadConfigsTags: function (data) {
        var urlParams = '(type=hdfs-rbf-site&tag=' + data.Clusters.desired_configs['hdfs-rbf-site'].tag + ')|';
        urlParams += '(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')|';
        urlParams += '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')';
        App.ajax.send({
            name: 'admin.get.all_configurations',
            sender: this,
            data: {
                urlParams: urlParams
            },
            success: 'onLoadConfigs'
        });
    },

    onLoadConfigs: function (data) {
        this.set('serverConfigData', data);
        this.set('isConfigsLoaded', true);
    },

    onLoad: function () {
        if (this.get('isConfigsLoaded') && App.router.get('clusterController.isHDFSNameSpacesLoaded')) {
            var routerFederationConfig = $.extend(true, {}, require('data/configs/wizards/router_federation_properties').routerFederationConfig);
            if (App.get('hasNameNodeFederation')) {
                routerFederationConfig.configs = routerFederationConfig.configs.rejectProperty('firstRun');
            }
            routerFederationConfig.configs = this.tweakServiceConfigs(routerFederationConfig.configs);
            var configsFromServer = this.get('serverConfigData.items');
            var hdfsrbfConfigs = configsFromServer.findProperty('type', 'hdfs-rbf-site');
            var configToSave = {
                type: 'hdfs-rbf-site',
                properties: hdfsrbfConfigs&&hdfsrbfConfigs.properties,
            };
            if (hdfsrbfConfigs && hdfsrbfConfigs.properties_attributes) {
                configToSave.properties_attributes = hdfsrbfConfigs.properties_attributes;
            }
            for(const property of routerFederationConfig.configs){
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

            this.renderServiceConfigs(routerFederationConfig);
            this.set('isLoaded', true);
        }
    }.observes('isConfigsLoaded', 'App.router.clusterController.isHDFSNameSpacesLoaded'),

    prepareDependencies: function () {
        var ret = {};
        var configsFromServer = this.get('serverConfigData.items');
        var nameNodes = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE');
        var hdfsSiteConfigs = configsFromServer.findProperty('type', 'hdfs-site').properties;
        var coreSiteConfigs = configsFromServer.findProperty('type', 'core-site').properties;
        var nameServices = App.HDFSService.find().objectAt(0).get('masterComponentGroups').mapProperty('name');
        var modifiedNameServices = [];
        var nnCounter = 1;
        ret.nameServicesList = nameServices.join(',');
        ret.nameservice1 = nameServices[0];
        for (let i = 0; i < nameServices.length; i++) {
            let nameservice = nameServices[i];
            modifiedNameServices.push(`${nameservice}.nn${nnCounter}`);
            nnCounter++;
            modifiedNameServices.push(`${nameservice}.nn${nnCounter}`);
            nnCounter++;
        }
        ret.modifiedNameServices = modifiedNameServices.join(',');

        ret.zkAddress = coreSiteConfigs['ha.zookeeper.quorum'];

        return ret;
    },
    tweakServiceConfigs: function (configs) {
        var dependencies = this.prepareDependencies();
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

        return result;
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