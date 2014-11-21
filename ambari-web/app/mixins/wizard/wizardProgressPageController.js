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

/**
 * Mixin for wizard controller for showing command progress on wizard pages
 * This should
 * @type {Ember.Mixin}
 */
App.wizardProgressPageControllerMixin = Em.Mixin.create({
  controllerName: '',
  clusterDeployState: 'WIZARD_DEPLOY',
  status: 'IN_PROGRESS',
  tasks: [],
  commands: [],
  currentRequestIds: [], //todo: replace with using requestIds from tasks
  logs: [],
  currentTaskId: null,
  POLL_INTERVAL: 4000,
  isSubmitDisabled: true,
  isBackButtonDisabled: true,
  /**
   *  tasksMessagesPrefix should be overloaded by any controller including the mixin
   */
  tasksMessagesPrefix: '',

  loadStep: function () {
    this.clearStep();
    this.initializeTasks();
    this.loadTasks();
    this.addObserver('tasks.@each.status', this, 'onTaskStatusChange');
    this.onTaskStatusChange();
  },

  clearStep: function () {
    this.set('isSubmitDisabled', true);
    this.set('isBackButtonDisabled', true);
    this.set('tasks', []);
    this.set('currentRequestIds', []);
  },

  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get(this.get('content.controllerName') + '.currentStep');
    var tasksMessagesPrefix = this.get('tasksMessagesPrefix');
    for (var i = 0; i < commands.length; i++) {
      this.get('tasks').pushObject(Ember.Object.create({
        title: Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        displayName: Em.I18n.t(tasksMessagesPrefix + currentStep + '.task' + i + '.title'),
        progress: 0,
        isRunning: false,
        requestIds: []
      }));
    }
  },

  loadTasks: function () {
    var self = this;
    var loadedStatuses = this.get('content.tasksStatuses');
    var loadedRequestIds = this.get('content.tasksRequestIds');
    if (loadedStatuses && loadedStatuses.length === this.get('tasks').length) {
      this.get('tasks').forEach(function (task, i) {
        self.setTaskStatus(task.get('id'), loadedStatuses[i]);
        self.setRequestIds(task.get('id'), loadedRequestIds[i]);
      });
      if (loadedStatuses.contains('IN_PROGRESS')) {
        var curTaskId = this.get('tasks')[loadedStatuses.indexOf('IN_PROGRESS')].get('id');
        this.set('currentRequestIds', this.get('content.requestIds'));
        this.set('currentTaskId', curTaskId);
        this.doPolling();
      } else if (loadedStatuses.contains('QUEUED')) {
        var curTaskId = this.get('tasks')[loadedStatuses.indexOf('QUEUED')].get('id');
        this.set('currentTaskId', curTaskId);
        this.runTask(curTaskId);
      }
    }
  },

  setTaskStatus: function (taskId, status) {
    this.get('tasks').findProperty('id', taskId).set('status', status);
  },

  setRequestIds: function (taskId, requestIds) {
    this.get('tasks').findProperty('id', taskId).set('requestIds', requestIds);
  },

  retryTask: function () {
    var task = this.get('tasks').findProperty('status', 'FAILED');
    task.set('showRetry', false);
    task.set('showRollback', false);
    task.set('status', 'PENDING');
  },

  onTaskStatusChange: function () {
    var statuses = this.get('tasks').mapProperty('status');
    var tasksRequestIds = this.get('tasks').mapProperty('requestIds');
    var requestIds = this.get('currentRequestIds');
    // save task info
    App.router.get(this.get('content.controllerName')).saveTasksStatuses(statuses);
    App.router.get(this.get('content.controllerName')).saveTasksRequestIds(tasksRequestIds);
    App.router.get(this.get('content.controllerName')).saveRequestIds(requestIds);
    // call saving of cluster status asynchronous
    // synchronous executing cause problems in Firefox
    App.clusterStatus.setClusterStatus({
      clusterName: App.router.getClusterName(),
      clusterState: this.get('clusterDeployState'),
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    }, {successCallback: this.statusChangeCallback, sender: this});
  },

  /**
   * Method that called after saving persist data to server.
   * Switch task according its status.
   */
  statusChangeCallback: function () {
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
        this.set('isBackButtonDisabled', false);
      }
    } else if (this.get('tasks').someProperty('status', 'FAILED')) {
      this.set('status', 'FAILED');
      this.set('isBackButtonDisabled', false);
      this.get('tasks').findProperty('status', 'FAILED').set('showRetry', true);
      if (App.supports.autoRollbackHA) {
        this.get('tasks').findProperty('status', 'FAILED').set('showRollback', true);
      }
    }
    this.get('tasks').filterProperty('status', 'COMPLETED').setEach('showRetry', false);
    this.get('tasks').filterProperty('status', 'COMPLETED').setEach('showRollback', false);
  },

  /**
   * Run command of appropriate task
   */
  runTask: function (taskId) {
    this[this.get('tasks').findProperty('id', taskId).get('command')]();
  },

  onTaskError: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'FAILED');
  },

  onTaskCompleted: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'COMPLETED');
  },

  /**
   * check whether component installed on specified hosts
   * @param componentName
   * @param hostNames
   * @return {$.ajax}
   */
  checkInstalledComponents: function (componentName, hostNames) {
    return App.ajax.send({
      name: 'host_component.installed.on_hosts',
      sender: this,
      data: {
        componentName: componentName,
        hostNames: hostNames.join(',')
      },
      success: 'checkInstalledComponentsSuccessCallback'
    });
  },

  checkInstalledComponentsSuccessCallback: function (data, opt, params) {
    installedComponents = data.items;
  },

  createComponent: function (componentName, hostName, serviceName) {
    var hostNames = (Array.isArray(hostName)) ? hostName : [hostName];
    var self = this;

    this.checkInstalledComponents(componentName, hostNames).complete(function () {
      var result = [];

      hostNames.forEach(function (hostName) {
        result.push({
          componentName: componentName,
          hostName: hostName,
          hasComponent: installedComponents.someProperty('HostRoles.host_name', hostName)
        });
      });

      result.forEach(function (host, index, array) {
        if (!host.hasComponent) {
          App.ajax.send({
            name: 'admin.high_availability.create_component',
            sender: this,
            data: {
              hostName: host.hostName,
              componentName: host.componentName,
              serviceName: serviceName,
              taskNum: array.length
            },
            success: 'onCreateComponent',
            error: 'onCreateComponentError'
          });
        } else {
          // Simulates format returned from ajax.send
          this.onCreateComponent(null, null, {hostName: host.hostName, componentName: host.componentName, taskNum: array.length});
        }
      }, self)
    });
  },

  onCreateComponent: function () {
    var hostName = arguments[2].hostName;
    var componentName = arguments[2].componentName;
    var taskNum = arguments[2].taskNum;
    var serviceName = arguments[2].serviceName;
    this.updateComponent(componentName, hostName, serviceName, "Install", taskNum);
  },

  onCreateComponentError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException') !== -1) {
      this.onCreateComponent();
    } else {
      this.onTaskError();
    }
  },

  updateComponent: function (componentName, hostName, serviceName, context, taskNum) {
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    var state = context.toLowerCase() == "start" ? "STARTED" : "INSTALLED";
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'common.host.host_component.update',
        sender: this,
        data: {
          context: context + " " + App.format.role(componentName),
          hostName: hostName[i],
          serviceName: serviceName,
          componentName: componentName,
          taskNum: taskNum || hostName.length,
          HostRoles: {
            state: state
          }
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  startPolling: function (data) {
    if (data) {
      this.get('currentRequestIds').push(data.Requests.id);
      var tasksCount = arguments[2].taskNum || 1;
      if (tasksCount === this.get('currentRequestIds').length) {
        this.setRequestIds(this.get('currentTaskId'), this.get('currentRequestIds'));
        this.doPolling();
      }
    } else {
      this.onTaskCompleted();
    }
  },

  doPolling: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'IN_PROGRESS');
    var requestIds = this.get('currentRequestIds');
    this.set('logs', []);
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
    this.get('logs').pushObject(logs.tasks);
    if (this.get('currentRequestIds').length === this.get('logs').length) {
      var tasks = [];
      this.get('logs').forEach(function (logs) {
        tasks.pushObjects(logs);
      }, this);
      var self = this;
      var currentTaskId = this.get('currentTaskId');
      if (!tasks.someProperty('Tasks.status', 'PENDING') && !tasks.someProperty('Tasks.status', 'QUEUED') && !tasks.someProperty('Tasks.status', 'IN_PROGRESS')) {
        this.set('currentRequestIds', []);
        if (tasks.someProperty('Tasks.status', 'FAILED') || tasks.someProperty('Tasks.status', 'TIMEDOUT') || tasks.someProperty('Tasks.status', 'ABORTED')) {
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
    }
  },

  showHostProgressPopup: function (event) {
    var popupTitle = event.contexts[0].title;
    var requestIds = event.contexts[0].requestIds;
    var hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this, true);
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      App.router.send('next');
    }
  },

  back: function () {
    if (!this.get('isBackButtonDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      App.router.send('back');
    }
  }

});


