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

App.upgradeTaskView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_task'),

  /**
   * view observed directly
   * @type {boolean}
   */
  outsideView: false,

  /**
   * @type {boolean}
   */
  showContent: function () {
    return this.get('outsideView') || this.get('content.isExpanded');
  }.property('content.isExpanded'),

  /**
   * @type {boolean}
   */
  errorLogOpened: false,

  /**
   * @type {boolean}
   */
  outputLogOpened: false,

  /**
   * @type {App.upgradeEntity}
   * @default null
   */
  content: null,

  /**
   * @type {Array}
   */
  tasks: [],

  /**
   * poll timer
   * @type {number|null}
   */
  timer: null,

  /**
   * @type {Array}
   */
  taskDetailsProperties: ['status', 'stdout', 'stderr', 'error_log', 'host_name', 'output_log'],

  didInsertElement: function () {
    if (this.get('outsideView')) this.doPolling();
  },

  /**
   * poll for task details when task is expanded
   */
  doPolling: function () {
    var self = this;

    if (this.get('content.isExpanded') || this.get('outsideView')) {
      this.getTaskDetails();
      this.set('timer', setTimeout(function () {
        self.doPolling();
      }, App.bgOperationsUpdateInterval));
    } else {
      clearTimeout(this.get('timer'));
    }
  }.observes('content.isExpanded', 'outsideView'),

  /**
   * request task details from server
   * @return {$.ajax|null}
   */
  getTaskDetails: function () {
    if (Em.isNone(this.get('content'))) return null;
    return App.ajax.send({
      name: 'admin.upgrade.task',
      sender: this,
      data: {
        upgradeId: this.get('content.request_id'),
        taskId: this.get('content.id')
      },
      success: 'getTaskDetailsSuccessCallback'
    });
  },

  /**
   * success callback of <code>getTaskDetails</code>
   * @param {object} data
   */
  getTaskDetailsSuccessCallback: function (data) {
    //TODO change request to get only one task when API ready
    var task = data.items[0].upgrade_items[0].tasks[0].Tasks;
    this.get('taskDetailsProperties').forEach(function (property) {
      this.set('content.' + property, task[property]);
    }, this);
  },

  /**
   * open error log in textarea to give ability to cope content
   * @param {object} event
   */
  copyErrLog: function(event) {
    this.toggleProperty('errorLogOpened');
  },

  /**
   * open stdout log in textarea to give ability to cope content
   * @param {object} event
   */
  copyOutLog: function(event) {
    this.toggleProperty('outputLogOpened');
  },

  /**
   * open error log in new window
   */
  openErrorLog: function () {
    this.openLogWindow(this.get('content.stderr'));
  },

  /**
   * open stdout log in new window
   */
  openOutLog: function () {
    this.openLogWindow(this.get('content.stdout'));
  },

  /**
   * open logs in new window
   * @param {string} log
   */
  openLogWindow: function(log) {
    var newWindow = window.open();
    var newDocument = newWindow.document;
    newDocument.write(log);
    newDocument.close();
  }
});
