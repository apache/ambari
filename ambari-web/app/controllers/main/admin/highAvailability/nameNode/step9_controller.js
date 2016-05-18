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

App.HighAvailabilityWizardStep9Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name:"highAvailabilityWizardStep9Controller",

  commands: ['startSecondNameNode', 'installZKFC', 'startZKFC', 'installPXF', 'reconfigureHBase', 'reconfigureAccumulo', 'reconfigureHawq', 'deleteSNameNode', 'startAllServices'],

  hbaseSiteTag: "",
  accumuloSiteTag: "",
  hawqSiteTag: "",
  secondNameNodeHost: "",

  initializeTasks: function () {
    this._super();
    var numSpliced = 0;

    // find hostname where second namenode will be installed
    this.set('secondNameNodeHost', this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).hostName);

    if (!App.Service.find().someProperty('serviceName', 'PXF') || this.isPxfComponentInstalled()) {
      this.get('tasks').splice(this.get('tasks').findProperty('command', 'installPXF').get('id'), 1);
      numSpliced = 1;
    }
    if (!App.Service.find().someProperty('serviceName', 'HBASE')) {
      this.get('tasks').splice(this.get('tasks').findProperty('command', 'reconfigureHBase').get('id') - numSpliced, 1);
      numSpliced++;
    }
    if (!App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
      this.get('tasks').splice(this.get('tasks').findProperty('command', 'reconfigureAccumulo').get('id') - numSpliced, 1);
      numSpliced++ ;
    }
    if (!App.Service.find().someProperty('serviceName', 'HAWQ')) {
      this.get('tasks').splice(this.get('tasks').findProperty('command', 'reconfigureHawq').get('id') - numSpliced, 1);
    }
  },

  startSecondNameNode: function () {
    var hostName = this.get('secondNameNodeHost');
    this.updateComponent('NAMENODE', hostName, "HDFS", "Start");
  },

  installZKFC: function () {
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.createInstallComponentTask('ZKFC', hostName, "HDFS");
  },

  startZKFC: function () {
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.updateComponent('ZKFC', hostName, "HDFS", "Start");
  },

  isPxfComponentInstalled: function () {
    var pxfComponent = this.getSlaveComponentHosts().findProperty('componentName', 'PXF');

    if (pxfComponent !== undefined) {
      var host;
      // check if PXF is already installed on the host assigned for additional NameNode
      for (var i = 0; i < pxfComponent.hosts.length; i++) {
        host = pxfComponent.hosts[i];
        if (host.hostName === this.get('secondNameNodeHost'))
          return true;
      }
    }

    return false;
  },

  installPXF: function () {
    this.createInstallComponentTask('PXF', this.get('secondNameNodeHost'), "PXF");
  },

  reconfigureHBase: function () {
    var data = this.get('content.serviceConfigProperties');
    var configData = this.reconfigureSites(['hbase-site'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)));
    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'saveConfigTag',
      error: 'onTaskError'
    });
  },

  reconfigureAccumulo: function () {
    var data = this.get('content.serviceConfigProperties');
    var configData = this.reconfigureSites(['accumulo-site'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)));
    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'saveConfigTag',
      error: 'onTaskError'
    });
  },

  reconfigureHawq: function () {
    var data = this.get('content.serviceConfigProperties');
    var configData = this.reconfigureSites(['hawq-site', 'hdfs-client'], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('NAMENODE', false)));
    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'saveConfigTag',
      error: 'onTaskError'
    });
  },

  saveConfigTag: function () {
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_DEPLOY',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
    this.onTaskCompleted();
  },

  startAllServices: function () {
    this.startServices(false);
  },

  deleteSNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        componentName: 'SECONDARY_NAMENODE',
        hostName: hostName
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  }

});
