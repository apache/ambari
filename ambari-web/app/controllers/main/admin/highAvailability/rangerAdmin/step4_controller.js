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

App.RAHighAvailabilityWizardStep4Controller = App.HighAvailabilityProgressPageController.extend({

  name: "rAHighAvailabilityWizardStep4Controller",

  clusterDeployState: 'RA_HIGH_AVAILABILITY_DEPLOY',

  commands: ['stopAllServices', 'installRangerAdmin', 'reconfigureRanger', 'startAllServices'],

  tasksMessagesPrefix: 'admin.ra_highAvailability.wizard.step',

  stopAllServices: function () {
    this.stopServices();
  },

  installRangerAdmin: function () {
    var hostNames = this.get('content.raHosts.additionalRA');
    this.createInstallComponentTask('RANGER_ADMIN', hostNames, "RANGER");
  },

  reconfigureRanger: function () {
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
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=admin-properties&tag=' + data.Clusters.desired_configs['admin-properties'].tag + ')'
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    data.items.findProperty('type', 'admin-properties').properties['policymgr_external_url'] = this.get('content.loadBalancerURL');
    var configData = this.reconfigureSites(['admin-properties'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)));

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
