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

App.ReassignMasterWizardStep6Controller = App.HighAvailabilityProgressPageController.extend({

  isReassign: true,

  commands: ['deleteHostComponents', 'startServices'],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  loadStep: function () {
    if (this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled')) {
      this.set('hostComponents', ['NAMENODE', 'ZKFC']);
    } else {
      this.set('hostComponents', [this.get('content.reassign.component_name')]);
    }
    this._super();
  },

  initializeTasks: function () {
    var commands = this.get('commands');
    var hostComponentsNames = '';
    this.get('hostComponents').forEach(function (comp, index) {
      hostComponentsNames += index ? '+' : '';
      hostComponentsNames += comp === 'ZKFC' ? comp : App.format.role(comp);
    }, this);
    var currentStep = App.router.get('reassignMasterController.currentStep');
    for (var i = 0; i < commands.length; i++) {
      var title = Em.I18n.t('services.reassign.step6.task' + i + '.title').format(hostComponentsNames);
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

  startServices: function () {
    App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        "context": "Start all services",
        "ServiceInfo": {
          "state": "STARTED"
        },
        urlParams: "params/run_smoke_test=true"
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  deleteHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onDeleteHostComponentsError'
      });
    }
  },

  onDeleteHostComponentsError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onComponentsTasksSuccess();
    } else {
      this.onTaskError();
    }
  }
});
