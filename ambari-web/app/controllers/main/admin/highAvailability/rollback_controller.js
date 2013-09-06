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
require('controllers/main/admin/highAvailability/progress_controller');

App.HighAvailabilityRollbackController = App.HighAvailabilityProgressPageController.extend({

  name: "highAvailabilityRollbackController",

  commands: [
    'stopAllServices',
    'restoreHBaseConfigs',
    'stopFailoverControllers',
    'deleteFailoverControllers',
    'stopStandbyNameNode',
    'stopNamenode',
    'restoreHDFSConfigs',
    'enableSecondaryNameNode',
    'stopJournalNodes',
    'deleteJournalNodes',
    'deleteAdditionalNameNode',
    'startAllServices'
  ],

  getStartingPoint: function() {

  },

  clearStep: function () {
    this.set('isSubmitDisabled', true);
    this.set('tasks', []);
    this.set('logs', []);
    this.set('currentRequestIds', []);
    var commands = this.get('commands');
    for (var i = 0; i < commands.length; i++) {
      this.get('tasks').pushObject(Ember.Object.create({
        title: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        name: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        displayName: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }
  },

  onTaskStatusChange: function () {
    if (!this.get('tasks').someProperty('status', 'IN_PROGRESS') && !this.get('tasks').someProperty('status', 'QUEUED') && !this.get('tasks').someProperty('status', 'FAILED')) {
      var nextTask = this.get('tasks').findProperty('status', 'PENDING');
      if (nextTask) {
        this.set('status', 'IN_PROGRESS');
        this.setTaskStatus(nextTask.get('id'), 'QUEUED');
        this.set('currentTaskId', nextTask.get('id'));
        this.runTask(nextTask.get('id'));
      } else {
        this.set('status', 'COMPLETED');
        this.set('isSubmitDisabled', false);
      }
    } else if (this.get('tasks').someProperty('status', 'FAILED') || this.get('tasks').someProperty('status', 'TIMEDOUT') || this.get('tasks').someProperty('status', 'ABORTED')) {
      this.set('status', 'FAILED');
      this.get('tasks').findProperty('status', 'FAILED').set('showRetry', true);
    }

    var statuses = this.get('tasks').mapProperty('status');
    var requestIds = this.get('currentRequestIds');
    this.saveTasksStatuses(statuses);
    this.saveRequestIds(requestIds);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_ROLLBACK',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  },

  saveTasksStatuses: function(statuses){
    App.db.setHighAvailabilityWizardTasksStatuses(statuses);
    //this.set('content.tasksStatuses', statuses);
  },

  loadTasksStatuses: function(){
    var statuses = App.db.getHighAvailabilityWizardTasksStatuses();
    //this.set('content.tasksStatuses', statuses);
  },

  saveRequestIds: function(requestIds){
    App.db.setHighAvailabilityWizardRequestIds(requestIds);
    //this.set('content.requestIds', requestIds);
  },

  loadRequestIds: function(){
    var requestIds = App.db.getHighAvailabilityWizardRequestIds();
    //this.set('content.requestIds', requestIds);
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      this.get('popup').hide();
      App.router.transitionTo('main.admin.adminHighAvailability');
    }
  },

  stopAllServices: function(){

  },
  restoreHBaseConfigs: function(){

  },

  stopFailoverControllers: function(){

  },
  deleteFailoverControllers: function(){

  },
  stopStandbyNameNode: function(){

  },
  stopNamenode: function(){

  },
  restoreHDFSConfigs: function(){

  },
  enableSecondaryNameNode: function(){

  },
  stopJournalNodes: function(){

  },
  deleteJournalNodes: function(){

  },
  deleteAdditionalNameNode: function(){

  },
  startAllServices: function(){

  }

});
