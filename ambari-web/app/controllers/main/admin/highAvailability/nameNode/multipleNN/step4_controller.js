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

App.MultipleNameNodeWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

    name: "multipleNameNodeWizardStep4Controller",

    commands: ['installNameNode', 'installZKFC', 'enterSafeMode' , 'saveNamespace', 'leaveSafeMode', 'formatZKFC', 'bootstrapNameNode', 'startZKFC', 'startNameNode', 'refreshConfigs', 'refreshNamenodes'],

    tasksMessagesPrefix: 'admin.multipleNameNode.wizard.step',

    initializeTasks: function initializeTasks() {
        this._super();
    },

    newNameNodeHosts: function () {
        return this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').filterProperty('isInstalled', false).mapProperty('hostName');
    }.property('content.masterComponentHosts.@each.hostName'),

    allDatanodeHosts: function () {
        return this.get('content.masterComponentHosts').filterProperty('component', 'DATANODE').filterProperty('isInstalled', true).mapProperty('hostName');
    }.property('content.masterComponentHosts.@each.hostName'),

    oldNameNodeHosts: function () {
        return this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').filterProperty('isInstalled', true).mapProperty('hostName');
    }.property('content.masterComponentHosts.@each.hostName'),

    reconfigureServices: function reconfigureServices() {
        var servicesModel = App.Service.find();
        var configs = [];
        var data = this.get('content.serviceConfigProperties');
        var note = Em.I18n.t('admin.multipleNameNode.wizard.step4.save.configuration.note');
        configs.push({
            Clusters: {
                desired_config: this.reconfigureSites(['hdfs-site'], data, note)
            }
        });
        return App.ajax.send({
            name: 'common.service.multiConfigurations',
            sender: this,
            data: {
                configs: configs
            },
            success: 'onSaveConfigs',
            error: 'onTaskError',
        });
    },

    onSaveConfigs: function () {
        this.onTaskCompleted();
    },

    installHDFSClients: function installHDFSClients() {
        var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.createInstallComponentTask('HDFS_CLIENT', nnHostNames, 'HDFS');
    },

    installNameNode: function installNameNode() {
        this.createInstallComponentTask('NAMENODE', this.get('newNameNodeHosts'), "HDFS");
    },

    installZKFC: function installZKFC() {
        this.createInstallComponentTask('ZKFC', this.get('newNameNodeHosts'), "HDFS");
    },

    startNameNode: function () {
        this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[0], "HDFS", "Start");
    },

    formatZKFC: function() {
        var self = this;
        var nameNodeHosts = this.get('newNameNodeHosts');

        if (!Array.isArray(nameNodeHosts) || nameNodeHosts.length === 0) {
            console.error('No NameNode hosts found to format ZKFC.');
            return;
        }

        nameNodeHosts.forEach(function(host) {
            App.ajax.send({
                name: 'nameNode.federation.formatZKFC',
                sender: self,
                data: {
                    host: host
                },
                success: 'startPolling',
                error: 'onTaskError'
            });
        });
    },

    startZKFC: function () {
        this.updateComponent('ZKFC', this.get('newNameNodeHosts')[0], "HDFS", "Start");
    },

    startRangerAdmin: function () {
        var hostNames = App.HostComponent.find().filterProperty('componentName', 'RANGER_ADMIN').mapProperty('hostName');
        this.updateComponent('RANGER_ADMIN', hostNames, "RANGER", "Start");
    },

    startRangerUsersync: function () {
        var hostNames = App.HostComponent.find().filterProperty('componentName', 'RANGER_USERSYNC').mapProperty('hostName');
        this.updateComponent('RANGER_USERSYNC', hostNames, "RANGER", "Start");
    },

    startNameNode: function () {
        this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[0], "HDFS", "Start");
    },

    enterSafeMode: function enterSafeMode() {
        App.ajax.send({
            name: 'multipleNameNode.entersafeMode',
            sender: this,
            data: {
                host: this.get('oldNameNodeHosts')[1]
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    },

    leaveSafeMode: function leaveSafeMode() {
        App.ajax.send({
            name: 'multipleNameNode.leavesafeMode',
            sender: this,
            data: {
                host: this.get('oldNameNodeHosts')[1]
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    },

    saveNamespace: function saveNamespace() {
        App.ajax.send({
            name: 'multipleNameNode.saveNamespace',
            sender: this,
            data: {
                host: this.get('oldNameNodeHosts')[1]
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    },

    bootstrapNameNode: function bootstrapNameNode() {
        App.ajax.send({
            name: 'nameNode.federation.bootstrapNameNode',
            sender: this,
            data: {
                host: this.get('newNameNodeHosts')[0]
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    },

    refreshConfigs: function () {
        var allDatanodeHosts = App.SlaveComponent.find('DATANODE').get('hostNames');
        var resource_filters = [
            {
                "service_name" : "HDFS",
                "component_name" : "DATANODE",
                "hosts": allDatanodeHosts.join(",")
            }
        ];
        App.ajax.send({
            name: 'host.host_component.refresh_configs',
            sender: this,
            data: {
                resource_filters: resource_filters,
                context: "refresh configs"
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    },

    refreshNamenodes: function () {
        var allDatanodeHosts = App.SlaveComponent.find('DATANODE').get('hostNames');
        App.ajax.send({
            name: 'multipleNameNode.refreshNamenodes',
            sender: this,
            data: {
                host: allDatanodeHosts
            },
            success: 'startPolling',
            error: 'onTaskError'
        });
    }
});
