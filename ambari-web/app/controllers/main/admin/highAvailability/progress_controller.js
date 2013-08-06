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

App.HighAvailabilityProgressPageController = Em.Controller.extend({

  tasks: [],
  commands: [],
  currentRequestIds: [],
  logs: [],
  currentTaskId: null,
  POLL_INTERVAL: 4000,
  isSubmitDisabled: true,

  loadStep: function () {
    this.clearStep();
    this.loadTasks();
    this.addObserver('tasks.@each.status', this, 'onTaskStatusChange');
    this.onTaskStatusChange();
  },

  clearStep: function () {
    this.set('isSubmitDisabled', true);
    this.get('tasks').clear();
    this.get('logs').clear();
    var commands = this.get('commands');
    for (var i = 0; i < commands.length; i++) {
      this.get('tasks').pushObject(Ember.Object.create({
        title: Em.I18n.t('admin.highAvailability.wizard.step5.task' + i + '.title'),
        status: 'PENDING',
        id: i,
        command: commands[i]
      }));
    }
  },

  loadTasks: function () {
    //load and set tasks statuses form server
  },

  setTaskStatus: function (taskId, status) {
    this.get('tasks').findProperty('id', taskId).set('status', status)
  },

  showRetry: function (taskId) {
    //show retry button for selected task
  },

  onTaskStatusChange: function () {
    if (!this.get('tasks').someProperty('status', 'IN_PROGRESS') && !this.get('tasks').someProperty('status', 'QUEUED') && !this.get('tasks').someProperty('status', 'FAILED')) {
      var nextTask = this.get('tasks').findProperty('status', 'PENDING');
      if (nextTask) {
        this.setTaskStatus(nextTask.get('id'), 'QUEUED');
        this.set('currentTaskId', nextTask.get('id'));
        this.runTask(nextTask.get('id'));
      } else {
        this.set('isSubmitDisabled', false);
      }
    }
  },

  /*
   run command of appropriate task
   */
  runTask: function (taskId) {
    this[this.get('tasks').findProperty('id', taskId).get('command')]();
  },

  onTaskError: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'FAILED');
    this.showRetry(this.get('currentTaskId'));
  },

  onTaskCompleted: function () {
    this.setTaskStatus(this.get('currentTaskId'), 'COMPLETED');
  },

  createComponent: function (componentName, hostName) {
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.create_component',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: componentName,
          taskNum: hostName.length
        },
        success: 'installComponent',
        error: 'onTaskError'
      });
    }
  },

  installComponent: function (data, params) {
    var hostName = params.data.hostName;
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.install_component',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: params.data.componentName,
          displayName: App.format.role(params.data.componentName),
          taskNum: params.data.taskNum || hostName.length
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  startComponent: function (componentName, hostName) {
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
          displayName: App.format.role(componentName)
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

  startPolling: function (data, params) {
    if (data) {
      this.get('currentRequestIds').push(data.Requests.id);
      var tasksCount = params.data.taskNum || 1;
      if (tasksCount === this.get('currentRequestIds').length) {
        this.doPolling();
      }
    } else {
      this.onTaskError();
    }
  },

  doPolling: function () {
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
    this.get('logs').push(logs.tasks);
    if (this.get('currentRequestIds').length === this.get('logs').length) {
      var tasks = this.get('logs');
      var self = this;
      var currentTaskId = this.get('currentTaskId');
      if (!tasks.someProperty('Tasks.status', 'PENDING') && !tasks.someProperty('Tasks.status', 'QUEUED') && !tasks.someProperty('Tasks.status', 'IN_PROGRESS')) {
        if (tasks.someProperty('Tasks.status', 'FAILED')) {
          this.setTaskStatus(currentTaskId, 'FAILED');
        } else {
          this.setTaskStatus(currentTaskId, 'COMPLETED');
        }
      } else {
        var progress = Math.round(tasks.filterProperty('Tasks.status', 'COMPLETED').length / tasks.length * 100);
        this.get('tasks').findProperty('id', currentTaskId).set('progress', progress);
        this.setTaskStatus(currentTaskId, 'IN_PROGRESS');
        window.setTimeout(function () {
          self.doPolling()
        }, self.POLL_INTERVAL);
      }
      this.get('logs').clear();
    }
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});

