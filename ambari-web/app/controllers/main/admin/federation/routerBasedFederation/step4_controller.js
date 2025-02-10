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

App.RouterFederationWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

    name: "routerFederationWizardStep4Controller",

    commands: ['installRouter', 'startRouters'],

    tasksMessagesPrefix: 'admin.routerFederation.wizard.step',

    initializeTasks: function () {
        this._super();
        this.removeUnneededTasks();
    },

    removeUnneededTasks: function () {
        var installedServices = App.Service.find().mapProperty('serviceName');
        if (!installedServices.contains('RANGER')) {
            this.removeTasks(['startInfraSolr', 'startRangerAdmin', 'startRangerUsersync']);
        }
        if (!installedServices.contains('AMBARI_INFRA_SOLR')) {
            this.removeTasks(['startInfraSolr']);
        }
    },

    reconfigureServices: function () {
        var servicesModel = App.Service.find();
        var configs = [];
        var data = this.get('content.serviceConfigProperties');
        var note = Em.I18n.t('admin.routerFederation.wizard,step4.save.configuration.note');
        configs.push({
            Clusters: {
                desired_config: this.reconfigureSites(['hdfs-rbf-site'], data, note)
            }
        });
        return App.ajax.send({
            name: 'common.service.multiConfigurations',
            sender: this,
            data: {
                configs: configs
            },
            error: 'onTaskError',
            success: 'installHDFSClients'
        });
    },

    installHDFSClients: function () {
        var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
        var jnHostNames = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').mapProperty('hostName');
        var hostNames = nnHostNames.concat(jnHostNames).uniq();
        this.createInstallComponentTask('HDFS_CLIENT', hostNames, 'HDFS');
    },

    installRouter: function () {
        this.createInstallComponentTask('ROUTER', this.get('content.masterComponentHosts').filterProperty('component', 'ROUTER').mapProperty('hostName'), "HDFS");
    },

    startRouters: function () {
        var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'ROUTER').mapProperty('hostName');
        this.updateComponent('ROUTER', hostNames, "HDFS", "Start");
    }
});