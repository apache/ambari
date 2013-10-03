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

App.WizardStep13Controller = App.HighAvailabilityProgressPageController.extend({

  commands: ['stopServices', 'createHostComponents', 'putHostComponentsInMaintenanceMode', 'reconfigure', 'installHostComponents', 'deleteHostComponents', 'startServices'],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  serviceNames: [],

  loadStep: function () {
    this.get('hostComponents').pushObject(this.get('content.reassign.component_name'));
    this.get('serviceNames').pushObject(this.get('content.reassign.service_id'));
    this._super();
  },

  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get('reassignMasterController.currentStep');
    var hostComponentsNames = '';
    var serviceNames = '';
    this.get('hostComponents').forEach(function (comp, index) {
      hostComponentsNames += index ? ', ' : '';
      hostComponentsNames += App.format.role(comp);
    }, this);
    this.get('serviceNames').forEach(function (service, index) {
      serviceNames += index ? ', ' : '';
      serviceNames += App.Service.find().findProperty('serviceName', service).get('displayName');
    }, this);
    for (var i = 0; i < commands.length; i++) {
      var title = Em.I18n.t('installer.step13.task' + i + '.title').format(hostComponentsNames, serviceNames);
      this.get('tasks').pushObject(Ember.Object.create({
        title: title,
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: title,
        displayName: title,
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }

    if (this.get('content.hasManualSteps')) {
      this.get('tasks').splice(5, 2);
    }
  },

  hideRollbackButton: function () {
    var failedTask = this.get('tasks').findProperty('showRollback');
    if (failedTask) {
      failedTask.set('showRollback', false)
    }
  }.observes('tasks.@each.showRollback'),

  onComponentsTasksSuccess: function () {
    this.set('multiTaskCounter', this.get('multiTaskCounter') + 1);
    if (this.get('multiTaskCounter') >= this.get('hostComponents').length) {
      this.onTaskCompleted();
    }
  },

  stopServices: function () {
    this.set('multiTaskCounter', 0);
    var serviceNames = this.get('serviceNames');
    for (var i = 0; i < serviceNames.length; i++) {
      App.ajax.send({
        name: 'reassign.stop_service',
        sender: this,
        data: {
          serviceName: serviceNames[i],
          displayName: App.Service.find().findProperty('serviceName', serviceNames[i]).get('displayName'),
          taskNum: serviceNames.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  createHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    for (var i = 0; i < hostComponents.length; i++) {
      this.createComponent(hostComponents[i], hostName);
    }
  },

  onCreateComponent: function () {
    this.onComponentsTasksSuccess();
  },

  putHostComponentsInMaintenanceMode: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassign.host_id');
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'reassign.maintenance_mode',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      });
    }
  },

  installHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    for (var i = 0; i < hostComponents.length; i++) {
      this.installComponent(hostComponents[i], hostName, hostComponents.length);
    }
  },

  reconfigure: function () {
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
    var componentName = this.get('content.reassign.component_name');
    var urlParams = [];
    switch (componentName) {
      case 'NAMENODE':
        urlParams.push('(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        if (App.Service.find().someProperty('serviceName', 'HBASE')) {
          urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
        }
        break;
      case 'SECONDARY_NAMENODE':
        urlParams.push('(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        break;
      case 'JOBTRACKER':
        urlParams.push('(type=mapred-site&tag=' + data.Clusters.desired_configs['mapred-site'].tag + ')');
        break;
      case 'RESOURCEMANAGER':
        urlParams.push('(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')');
        break;
    }
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

  configsSitesCount: null,

  configsSitesNumber: null,

  onLoadConfigs: function (data) {
    var isHadoop2Stack = App.get('isHadoop2Stack');
    var componentName = this.get('content.reassign.component_name');
    var targetHostName = this.get('content.masterComponentHosts').findProperty('component', this.get('content.reassign.component_name')).hostName;
    var configs = {};
    var componentDir = '';
    this.set('configsSitesNumber', data.items.length);
    this.set('configsSitesCount', 0);
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
    }, this);
    switch (componentName) {
      case 'NAMENODE':
        if (isHadoop2Stack) {
          componentDir = configs['hdfs-site']['dfs.namenode.name.dir'];
          configs['hdfs-site']['dfs.namenode.http-address'] = targetHostName + ':50070';
          configs['hdfs-site']['dfs.namenode.https-address'] = targetHostName + ':50470';
        } else {
          componentDir = configs['hdfs-site']['dfs.name.dir'];
          configs['hdfs-site']['dfs.http.address'] = targetHostName + ':50070';
          configs['hdfs-site']['dfs.https.address'] = targetHostName + ':50470';
        }
        configs['core-site']['fs.default.name'] = 'hdfs://' + targetHostName + ':8020';
        configs['hdfs-site']['dfs.safemode.threshold.pct'] = '1.1f';
        if (App.Service.find().someProperty('serviceName', 'HBASE')) {
          configs['hbase-site']['hbase.rootdir'] = configs['hbase-site']['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + targetHostName);
        }
        break;
      case 'SECONDARY_NAMENODE':
        componentDir = configs['core-site']['fs.checkpoint.dir'];
        configs['hdfs-site']['dfs.secondary.http.address'] = targetHostName + ':50090';
        break;
      case 'JOBTRACKER':
        componentDir = configs['mapred-site']['mapred.local.dir'];
        configs['mapred-site']['mapreduce.history.server.http.address'] = targetHostName + ':51111';
        configs['mapred-site']['mapred.job.tracker.http.address'] = targetHostName + ':50030';
        configs['mapred-site']['mapred.job.tracker'] = targetHostName + ':50300';
        break;
      case 'RESOURCEMANAGER':
        configs['yarn-site']['yarn.resourcemanager.address'] = targetHostName + ':8050';
        configs['yarn-site']['yarn.resourcemanager.admin.address'] = targetHostName + ':8141';
        configs['yarn-site']['yarn.resourcemanager.resource-tracker.address'] = targetHostName + ':8025';
        configs['yarn-site']['yarn.resourcemanager.scheduler.address'] = targetHostName + ':8030';
        configs['yarn-site']['yarn.resourcemanager.webapp.address'] = targetHostName + ':8088';
        configs['yarn-site']['yarn.resourcemanager.hostname'] = targetHostName;
        break;
    }
    if (componentDir) {
      App.router.get(this.get('content.controllerName')).saveComponentDir(componentDir);
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: this.get('clusterDeployState'),
        wizardControllerName: this.get('content.controllerName'),
        localdb: App.db.data
      });
    }
    for (var site in configs) {
      if (!configs.hasOwnProperty(site)) continue;
      App.ajax.send({
        name: 'reassign.save_configs',
        sender: this,
        data: {
          siteName: site,
          properties: configs[site]
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    }
  },

  onSaveConfigs: function () {
    this.set('configsSitesCount', this.get('configsSitesCount') + 1);
    if (this.get('configsSitesCount') === this.get('configsSitesNumber')) {
      this.onTaskCompleted();
    }
  },

  startServices: function () {
    this.set('multiTaskCounter', 0);
    var serviceNames = this.get('serviceNames');
    for (var i = 0; i < serviceNames.length; i++) {
      App.ajax.send({
        name: 'reassign.start_components',
        sender: this,
        data: {
          serviceName: serviceNames[i],
          displayName: App.Service.find().findProperty('serviceName', serviceNames[i]).get('displayName'),
          taskNum: serviceNames.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  deleteHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassign.host_id');
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'reassign.remove_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      });
    }
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      if (this.get('content.hasManualSteps')) {
        App.router.send('next');
      } else {
        App.router.send('complete');
      }
    }
  }
})
