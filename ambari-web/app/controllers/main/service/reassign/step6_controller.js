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

  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get('reassignMasterController.currentStep');
    for (var i = 0; i < commands.length; i++) {
      var title = Em.I18n.t('services.reassign.step6.task' + i + '.title').format(App.format.role(this.get('content.reassign.component_name')),
          App.Service.find().findProperty('serviceName', this.get('content.reassign.service_id')).get('displayName'));
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

  startServices: function () {
    var serviceName = this.get('content.reassign.service_id');
    App.ajax.send({
      name: 'reassign.start_components',
      sender: this,
      data: {
        serviceName: serviceName,
        displayName: App.Service.find().findProperty('serviceName', serviceName).get('displayName')
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  deleteHostComponents: function () {
    var hostName = this.get('content.reassignHosts.source');
    App.ajax.send({
      name: 'reassign.remove_component',
      sender: this,
      data: {
        hostName: hostName,
        componentName: this.get('content.reassign.component_name')
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  }
})
