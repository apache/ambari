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

require('controllers/main/admin/serviceAccounts_controller');

App.RMHighAvailabilityWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "rMHighAvailabilityWizardStep4Controller",

  commands: ['stopRequiredServices', 'installResourceManager', 'reconfigureYARN', 'reconfigureHDFS', 'reconfigureHAWQ', 'startAllServices'],
  clusterDeployState: 'RM_HIGH_AVAILABILITY_DEPLOY',

  tasksMessagesPrefix: 'admin.rm_highAvailability.wizard.step',

  initializeTasks: function () {
    this._super();
    if (!App.Service.find().someProperty('serviceName', 'HAWQ')) {
      this.get('tasks').splice(this.get('tasks').findProperty('command', 'reconfigureHAWQ').get('id'), 1);
    }
  },

  stopRequiredServices: function () {
    this.stopServices(['HDFS']);
  },

  installResourceManager: function () {
    var hostName = this.get('content.rmHosts.additionalRM');
    this.createInstallComponentTask('RESOURCEMANAGER', hostName, "YARN");
  },

  reconfigureYARN: function () {
    this.loadConfigsTags('yarn-site');
  },

  reconfigureHAWQ: function () {
    this.loadConfigsTags("yarn-client");
  },

  reconfigureHDFS: function () {
    this.loadConfigsTags('core-site');
  },

  loadConfigsTags: function (type) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        type: type
      },
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },

  onLoadConfigsTags: function (data, opt, params) {
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=' + params.type +'&tag=' + data.Clusters.desired_configs[params.type].tag + ')',
        type: params.type
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data, opt, params) {
    var propertiesToAdd = this.get('content.configs').filterProperty('filename', params.type);
    propertiesToAdd.forEach(function (property) {
      data.items[0].properties[property.name] = property.value;
    });

    var configData = this.reconfigureSites([params.type], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RESOURCEMANAGER', false)));

    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'onSaveConfigs',
      error: 'onTaskError'
    });
  },

  onSaveConfigs: function () {
    this.onTaskCompleted();
  },

  startAllServices: function () {
    this.startServices(true);
  }
});
