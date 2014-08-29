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

App.HighAvailabilityWizardStep5Controller = App.HighAvailabilityProgressPageController.extend({

  name:"highAvailabilityWizardStep5Controller",

  isHA: true,

  commands: ['stopAllServices', 'installNameNode', 'installJournalNodes', 'reconfigureHDFS', 'startJournalNodes', 'disableSNameNode'],

  hdfsSiteTag : "",
  coreSiteTag : "",

  stopAllServices: function () {
    App.ajax.send({
      name: 'common.services.update',
      data: {
        context: "Stop all services",
        "ServiceInfo": {
          "state": "INSTALLED"
        }
      },
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  installNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('isAddNameNode').hostName;
    this.createComponent('NAMENODE', hostName, "HDFS");
  },

  installJournalNodes: function () {
    App.ajax.send({
      name: 'admin.high_availability.create_journalnode',
      sender: this,
      success: 'onJournalNodeCreate',
      error: 'onJournalNodeCreate'
    });
  },

  onJournalNodeCreate: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.createComponent('JOURNALNODE', hostNames, "HDFS");
  },

  startJournalNodes: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.updateComponent('JOURNALNODE', hostNames, "HDFS", "Start");
  },

  disableSNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    App.ajax.send({
      name: 'common.host.host_component.passive',
      sender: this,
      data: {
        hostName: hostName,
        passive_state: "ON",
        componentName: 'SECONDARY_NAMENODE'
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },

  reconfigureHDFS: function () {
    var data = this.get('content.serviceConfigProperties');
    var siteNames = ['hdfs-site','core-site'];
    var configData = this.reconfigureSites(siteNames, data);
    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      error: 'onTaskError',
      success: 'installHDFSClients'
    })
  },

  installHDFSClients: function () {
    var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    var jnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    var hostNames = nnHostNames.concat(jnHostNames).uniq();
    this.createComponent('HDFS_CLIENT', hostNames);
    App.router.get(this.get('content.controllerName')).saveHdfsClientHosts(hostNames);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_DEPLOY',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  }
});

