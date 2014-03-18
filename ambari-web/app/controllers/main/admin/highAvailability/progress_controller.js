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

App.HighAvailabilityProgressPageController = App.HighAvailabilityWizardController.extend({

  name: 'highAvailabilityProgressPageController',

  status: 'IN_PROGRESS',
  clusterDeployState: 'HIGH_AVAILABILITY_DEPLOY',
  tasks: [],
  commands: [],
  currentRequestIds: [],
  logs: [],
  currentTaskId: null,
  POLL_INTERVAL: 4000,
  isSubmitDisabled: true,
  serviceTimestamp: null,
  isRollback: false,

  loadStep: function () {
    console.warn('func: loadStep');
    this.clearStep();
    this.initializeTasks();
    this.loadTasks();
    this.addObserver('tasks.@each.status', this, 'onTaskStatusChange');
    this.onTaskStatusChange();
  },

  clearStep: function () {
    console.warn('func: clearStep');
    this.set('isSubmitDisabled', true);
    this.set('tasks', []);
    this.set('logs', []);
    this.set('currentRequestIds', []);
  },

  initializeTasks: function() {
    console.warn('func: initializeTasks');
    var commands = this.get('commands');
    var currentStep = App.router.get('highAvailabilityWizardController.currentStep');
    for (var i = 0; i < commands.length; i++) {
      this.get('tasks').pushObject(Ember.Object.create({
        title: Em.I18n.t('admin.highAvailability.wizard.step' + currentStep + '.task' + i + '.title'),
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: Em.I18n.t('admin.highAvailability.wizard.step' + currentStep + '.task' + i + '.title'),
        displayName: Em.I18n.t('admin.highAvailability.wizard.step' + currentStep + '.task' + i + '.title'),
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }
  },

  services: function(){
    return this.get('tasks');
  }.property('tasks'),

  loadTasks: function () {
    console.warn('func: loadTasks');
    var self = this;
    var loadedStauses = this.get('content.tasksStatuses');
    var loadedLogs = this.get('content.logs');
    if (loadedStauses && loadedLogs && loadedStauses.length === this.get('tasks').length) {
      this.get('tasks').forEach(function(task,i){
        self.setTaskStatus(task.get('id'), loadedStauses[i]);
        self.restoreTaskLog(task.get('id'), loadedLogs[i]);
      });
      if (loadedStauses.contains('IN_PROGRESS')) {
        var curTaskId = this.get('tasks')[loadedStauses.indexOf('IN_PROGRESS')].get('id');
        this.set('currentRequestIds', this.get('content.requestIds'));
        this.set('currentTaskId', curTaskId);
        this.doPolling();
      }else if (loadedStauses.contains('QUEUED')){
        var curTaskId = this.get('tasks')[loadedStauses.indexOf('QUEUED')].get('id');
        this.set('currentTaskId', curTaskId);
        this.runTask(curTaskId);
      }
    }
  },

  setTaskStatus: function (taskId, status) {
    console.warn('func: setTaskStatus');
    this.get('tasks').findProperty('id', taskId).set('status', status);
  },

  restoreTaskLog: function (taskId, log) {
    console.warn('func: restoreTaskLog');
    this.get('tasks').findProperty('id', taskId).set('hosts', log);
  },

  setTaskLogs: function (taskId, tasks) {
    console.warn('func: setTaskLogs');
    var hosts = [];
    var uniqHosts = tasks.mapProperty('Tasks.host_name').uniq();
    uniqHosts.forEach(function (host) {
      var curHostTasks = tasks.filterProperty('Tasks.host_name', host);
      hosts.push(
       {
          name: host,
          publicName: host,
          logTasks: curHostTasks
        }
      );
    });
    this.get('tasks').findProperty('id', taskId).set('hosts', hosts);
    this.set('serviceTimestamp', App.dateTime());
  },

  retryTask: function () {
    console.warn('func: retryTask');
    var task = this.get('tasks').findProperty('status', 'FAILED');
    task.set('showRetry', false);
    task.set('showRollback', false);
    task.set('status', 'PENDING');
  },

  manualRollback: function () {
    console.warn('func: manualRollback');
    App.ModalPopup.show({
      header: Em.I18n.t('admin.highAvailability.confirmRollbackHeader'),
      primary: Em.I18n.t('yes'),
      showCloseButton: false,
      onPrimary: function () {
        var controller = App.router.get('highAvailabilityWizardController');
        controller.clearTasksData();
        controller.clearStorageData();
        controller.setCurrentStep('1');
        App.router.get('updateController').set('isWorking', true);
        App.clusterStatus.setClusterStatus({
          clusterName: App.router.get('content.cluster.name'),
          clusterState: 'DEFAULT',
          wizardControllerName: App.router.get('highAvailabilityRollbackController.name'),
          localdb: App.db.data
        });
        this.hide();
        App.router.transitionTo('main.admin.index');
        location.reload();
      },
      secondary : Em.I18n.t('no'),
      onSecondary: function(){
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile( Em.I18n.t('admin.highAvailability.confirmManualRollbackBody'))
      })
    });
  },

  rollback: function () {
    console.warn('func: rollback');
    var task = this.get('tasks').findProperty('status', 'FAILED');
    App.router.get(this.get('content.controllerName')).saveFailedTask(task);
    App.ModalPopup.show({
      header: Em.I18n.t('admin.highAvailability.confirmRollbackHeader'),
      primary: Em.I18n.t('common.confirm'),
      showCloseButton: false,
      onPrimary: function () {
        App.router.get('highAvailabilityWizardController').clearTasksData();
        App.router.transitionTo('main.admin.highAvailabilityRollback');
        this.hide();
      },
      secondary : Em.I18n.t('common.cancel'),
      body: Em.I18n.t('admin.highAvailability.confirmRollbackBody')
    });
  },

  onTaskStatusChange: function () {
    console.warn('func: onTaskStatusChange1');
    if (!this.get('tasks').someProperty('status', 'IN_PROGRESS') && !this.get('tasks').someProperty('status', 'QUEUED') && !this.get('tasks').someProperty('status', 'FAILED')) {
      var nextTask = this.get('tasks').findProperty('status', 'PENDING');
      if (nextTask) {
        console.warn('func: onTaskStatusChange2');
        this.set('status', 'IN_PROGRESS');
        this.setTaskStatus(nextTask.get('id'), 'QUEUED');
        this.set('currentTaskId', nextTask.get('id'));
        this.runTask(nextTask.get('id'));
      } else {
        console.warn('func: onTaskStatusChange3');
        this.set('status', 'COMPLETED');
        this.set('isSubmitDisabled', false);
      }
    } else if (this.get('tasks').someProperty('status', 'FAILED')) {
      console.warn('func: onTaskStatusChange4');
      this.set('status', 'FAILED');
      this.get('tasks').findProperty('status', 'FAILED').set('showRetry', true);
      if(App.supports.autoRollbackHA){
        this.get('tasks').findProperty('status', 'FAILED').set('showRollback', true);
      }
    }
    this.get('tasks').filterProperty('status','COMPLETED').setEach('showRetry', false);
    this.get('tasks').filterProperty('status','COMPLETED').setEach('showRollback', false);

    var statuses = this.get('tasks').mapProperty('status');
    var logs = this.get('tasks').mapProperty('hosts');
    var requestIds = this.get('currentRequestIds');
    console.warn('func: onTaskStatusChange5',statuses, logs, requestIds);
    App.router.get(this.get('content.controllerName')).saveTasksStatuses(statuses);
    App.router.get(this.get('content.controllerName')).saveRequestIds(requestIds);
    App.router.get(this.get('content.controllerName')).saveLogs(logs);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: this.get('clusterDeployState'),
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  },

  /*
   run command of appropriate task
   */
  runTask: function (taskId) {
    console.warn('func: runTask',taskId);
    this[this.get('tasks').findProperty('id', taskId).get('command')]();
  },

  onTaskError: function () {
    console.warn('func: onTaskError');
    this.setTaskStatus(this.get('currentTaskId'), 'FAILED');
  },

  onTaskCompleted: function () {
    console.warn('func: onTaskCompleted');
    this.setTaskStatus(this.get('currentTaskId'), 'COMPLETED');
  },

  createComponent: function (componentName, hostName) {
    console.warn('func: createComponent');
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    var hostComponents = [];
    for (var i = 0; i < hostName.length; i++) {
      hostComponents = App.HostComponent.find().filterProperty('componentName', componentName);
      if (!hostComponents.length || !hostComponents.mapProperty('host.hostName').contains(hostName[i])) {
        App.ajax.send({
          name: 'admin.high_availability.create_component',
          sender: this,
          data: {
            hostName: hostName[i],
            componentName: componentName,
            taskNum: hostName.length
          },
          success: 'onCreateComponent',
          error: 'onCreateComponentError'
        });
      } else {
        // Simulates format returned from ajax.send
        this.onCreateComponent(null, null, {hostName: hostName[i], componentName: componentName, taskNum: hostName.length});
      }
    }
  },

  onCreateComponent: function () {
    console.warn('func: onCreateComponent');
    var hostName = arguments[2].hostName;
    var componentName = arguments[2].componentName;
    var taskNum = arguments[2].taskNum;
    this.installComponent(componentName, hostName, taskNum);
  },

  onCreateComponentError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException') !== -1) {
      this.onCreateComponent();
    } else {
      this.onTaskError();
    }
  },

  installComponent: function (componentName, hostName, taskNum) {
    console.warn('func: installComponent');
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.install_component',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: componentName,
          displayName: App.format.role(componentName),
          taskNum: taskNum || hostName.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  startComponent: function (componentName, hostName) {
    console.warn('func: startComponent');
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.start_component',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: componentName,
          displayName: App.format.role(componentName),
          taskNum: hostName.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  stopComponent: function (componentName, hostName) {
    console.warn('func: stopComponent');
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.stop_component',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: componentName,
          displayName: App.format.role(componentName),
          taskNum: hostName.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  startPolling: function (data) {
    if (data) {
      console.warn('func: startPolling1');
      this.get('currentRequestIds').push(data.Requests.id);
      var tasksCount = arguments[2].taskNum || 1;
      if (tasksCount === this.get('currentRequestIds').length) {
        console.warn('func: startPolling2');
        this.doPolling();
      }
    } else {
      console.warn('func: startPolling3');
      this.setTaskLogs(this.get('currentTaskId'), []);
      this.onTaskCompleted();
    }
  },

  doPolling: function () {
    console.warn('func: doPolling');
    this.setTaskStatus(this.get('currentTaskId'), 'IN_PROGRESS');
    var requestIds = this.get('currentRequestIds');
    for (var i = 0; i < requestIds.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.polling',
        sender: this,
        data: {
          requestId: requestIds[i]
        },
        success: 'parseLogs',
        error: 'onTaskError'
      });
    }
  },

  parseLogs: function (logs) {
    console.warn('func: parseLogs');
    this.get('logs').push(logs.tasks);
    if (this.get('currentRequestIds').length === this.get('logs').length) {
      var tasks = [];
      this.get('logs').forEach(function (logs) {
        tasks.pushObjects(logs);
      }, this);
      var self = this;
      var currentTaskId = this.get('currentTaskId');
      this.setTaskLogs(currentTaskId, tasks);
      if (!tasks.someProperty('Tasks.status', 'PENDING') && !tasks.someProperty('Tasks.status', 'QUEUED') && !tasks.someProperty('Tasks.status', 'IN_PROGRESS')) {
        this.set('currentRequestIds', []);
        if (tasks.someProperty('Tasks.status', 'FAILED')  || tasks.someProperty('Tasks.status', 'TIMEDOUT') || tasks.someProperty('Tasks.status', 'ABORTED')) {
          this.setTaskStatus(currentTaskId, 'FAILED');
        } else {
          this.setTaskStatus(currentTaskId, 'COMPLETED');
        }
      } else {
        var actionsPerHost = tasks.length;
        var completedActions = tasks.filterProperty('Tasks.status', 'COMPLETED').length
          + tasks.filterProperty('Tasks.status', 'FAILED').length
          + tasks.filterProperty('Tasks.status', 'ABORTED').length
          + tasks.filterProperty('Tasks.status', 'TIMEDOUT').length;
        var queuedActions = tasks.filterProperty('Tasks.status', 'QUEUED').length;
        var inProgressActions = tasks.filterProperty('Tasks.status', 'IN_PROGRESS').length;
        var progress = Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 100);
        this.get('tasks').findProperty('id', currentTaskId).set('progress', progress);
        window.setTimeout(function () {
          self.doPolling()
        }, self.POLL_INTERVAL);
      }
      this.set('logs', []);
    }
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      App.router.send('next');
    }
  }
});

