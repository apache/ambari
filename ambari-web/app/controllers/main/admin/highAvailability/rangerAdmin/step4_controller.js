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
    var urlParams = [];
    urlParams.push('(type=admin-properties&tag=' + data.Clusters.desired_configs['admin-properties'].tag + ')');
    var siteNamesToFetch = [
      'ranger-hdfs-security',
      'ranger-yarn-security',
      'ranger-hbase-security',
      'ranger-hive-security',
      'ranger-knox-security',
      'ranger-kafka-security',
      'ranger-kms-security',
      'ranger-storm-security',
      'ranger-atlas-security'
    ];
    siteNamesToFetch.map(function(siteName) {
      if(siteName in data.Clusters.desired_configs) {
        urlParams.push('(type=' + siteName + '&tag=' + data.Clusters.desired_configs[siteName].tag + ')');
      }
    });

    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    var configs = [];
    var self = this;
    data.items.findProperty('type', 'admin-properties').properties['policymgr_external_url'] = this.get('content.loadBalancerURL');
    configs.push({
      Clusters: {
        desired_config: this.reconfigureSites(['admin-properties'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)))
      }
    });

    var configsToChange = [
      {
        siteName: 'ranger-hdfs-security',
        property: 'ranger.plugin.hdfs.policy.rest.url'
      },
      {
        siteName: 'ranger-yarn-security',
        property: 'ranger.plugin.yarn.policy.rest.url'
      },
      {
        siteName: 'ranger-hbase-security',
        property: 'ranger.plugin.hbase.policy.rest.url'
      },
      {
        siteName: 'ranger-hive-security',
        property: 'ranger.plugin.hive.policy.rest.url'
      },
      {
        siteName: 'ranger-knox-security',
        property: 'ranger.plugin.knox.policy.rest.url'
      },
      {
        siteName: 'ranger-kafka-security',
        property: 'ranger.plugin.kafka.policy.rest.url'
      },
      {
        siteName: 'ranger-kms-security',
        property: 'ranger.plugin.kms.policy.rest.url'
      },
      {
        siteName: 'ranger-storm-security',
        property: 'ranger.plugin.storm.policy.rest.url'
      },
      {
        siteName: 'ranger-atlas-security',
        property: 'ranger.plugin.atlas.policy.rest.url'
      }
    ];
    configsToChange.map(function(item) {
      var config = data.items.findProperty('type', item.siteName);
      if(config) {
        config.properties[item.property] = self.get('content.loadBalancerURL');
        configs.push({
          Clusters: {
            desired_config: self.reconfigureSites([item.siteName], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)))
          }
        });
      }
    });

    App.ajax.send({
      name: 'common.service.multiConfigurations',
      sender: this,
      data: {
        configs: configs
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
