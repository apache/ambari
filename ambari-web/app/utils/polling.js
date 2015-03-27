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
App.Poll = Em.Object.extend(App.ReloadPopupMixin, {
  name: '',
  stage: '',
  label: '',
  isVisible: true,
  isStarted: false,
  isPolling: true,
  clusterName: null,
  requestId: null,
  temp: false,
  progress: 0,
  url: null,
  testUrl: null,
  data: null,
  isError: false,
  isSuccess: false,
  POLL_INTERVAL: 4000,
  polledData: [],
  numPolls: 0,
  mockDataPrefix: '/data/wizard/deploy/5_hosts',
  currentTaskId: null,

  barWidth: function () {
    return 'width: ' + this.get('progress') + '%;';
  }.property('progress'),

  isCompleted: function () {
    return (this.get('isError') || this.get('isSuccess'));
  }.property('isError', 'isSuccess'),

  showLink: function () {
    return (this.get('isPolling') === true && this.get('isStarted') === true);
  }.property('isPolling', 'isStarted'),

  start: function () {
    if (Em.isNone(this.get('requestId'))) {
      this.setRequestId();
    } else {
      this.startPolling();
    }
  },

  setRequestId: function () {
    if (App.get('testMode')) {
      this.set('requestId', '1');
      this.doPolling();
      return;
    }
    var self = this;
    var url = this.get('url');
    var method = 'PUT';
    var data = this.get('data');

    $.ajax({
      type: method,
      url: url,
      data: data,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: Polling -> value of the url is: " + url);
        console.log("TRACE: Polling-> value of the sent data is: " + self.get('data'));
        console.log("TRACE: Polling-> value of the received data is: " + jsonData);
        if (Em.isNone(jsonData)) {
          self.set('isSuccess', true);
          self.set('isError', false);
        } else {
          var requestId = jsonData.Requests.id;
          self.set('requestId', requestId);
          self.doPolling();
          console.log('requestId is: ' + requestId);
        }
      },

      error: function () {
        console.log("ERROR");
        self.set('isError', true);
        self.set('isSuccess', false);
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * set current task id and send request
   * @param taskId
   */
  updateTaskLog: function (taskId) {
    this.set('currentTaskId', taskId);
    this.pollTaskLog();
  },

  doPolling: function () {
    if (this.get('requestId')) {
      this.startPolling();
    }
  },

  /**
   * server call to obtain task logs
   */
  pollTaskLog: function () {
    if (this.get('currentTaskId')) {
      App.ajax.send({
        name: 'background_operations.get_by_task',
        sender: this,
        data: {
          requestId: this.get('requestId'),
          taskId: this.get('currentTaskId')
        },
        success: 'pollTaskLogSuccessCallback'
      })
    }
  },

  /**
   * update logs of current task
   * @param data
   */
  pollTaskLogSuccessCallback: function (data) {
    var currentTask = this.get('polledData').findProperty('Tasks.id', data.Tasks.id);
    currentTask.Tasks.stdout = data.Tasks.stdout;
    currentTask.Tasks.stderr = data.Tasks.stderr;
    Em.propertyDidChange(this, 'polledData');
  },

  /**
   * start polling operation data
   * @return {Boolean}
   */
  startPolling: function () {
    if (!this.get('requestId')) return false;
    var self = this;

    this.pollTaskLog();
    App.ajax.send({
      name: 'background_operations.get_by_request',
      sender: this,
      data: {
        requestId: this.get('requestId')
      },
      success: 'startPollingSuccessCallback',
      error: 'startPollingErrorCallback'
    })
      .retry({times: App.maxRetries, timeout: App.timeout})
      .then(
        function () {
          self.closeReloadPopup();
        },
        function () {
          self.showReloadPopup();
          console.log('Install services all retries failed');
        }
      );
    return true;
  },

  startPollingSuccessCallback: function (data) {
    var self = this;
    var result = this.parseInfo(data);
    if (!result) {
      window.setTimeout(function () {
        self.startPolling();
      }, this.POLL_INTERVAL);
    }
  },

  startPollingErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: In error function for the GET data");
    console.log("TRACE: value of the url is: " + url);
    console.log("TRACE: error code status is: " + request.status);
    if (!this.get('isSuccess')) {
      this.set('isError', true);
    }
  },

  stopPolling: function () {
    //this.set('isSuccess', true);
  },

  replacePolledData: function (polledData) {
    var currentTaskId = this.get('currentTaskId');
    if (currentTaskId) {
      var task = this.get('polledData').findProperty('Tasks.id', currentTaskId);
      var currentTask = polledData.findProperty('Tasks.id', currentTaskId);
      if (task && currentTask) {
        currentTask.Tasks.stdout = task.Tasks.stdout;
        currentTask.Tasks.stderr = task.Tasks.stderr;
      }
    }
    this.set('polledData', polledData);
  },


  calculateProgressByTasks: function (tasksData) {
    var queuedTasks = tasksData.filterProperty('Tasks.status', 'QUEUED').length;
    var completedTasks = tasksData.filter(function (task) {
      return ['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT'].contains(task.Tasks.status);
    }).length;
    var inProgressTasks = tasksData.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    return Math.ceil(((queuedTasks * 0.09) + (inProgressTasks * 0.35) + completedTasks ) / tasksData.length * 100)
  },

  isPollingFinished: function (polledData) {
    var runningTasks;
    runningTasks = polledData.filterProperty('Tasks.status', 'QUEUED').length;
    runningTasks += polledData.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    runningTasks += polledData.filterProperty('Tasks.status', 'PENDING').length;
    if (runningTasks === 0) {
      if (polledData.everyProperty('Tasks.status', 'COMPLETED')) {
        this.set('isSuccess', true);
        this.set('isError', false);
      } else if (polledData.someProperty('Tasks.status', 'FAILED') || polledData.someProperty('Tasks.status', 'TIMEDOUT') || polledData.someProperty('Tasks.status', 'ABORTED')) {
        this.set('isSuccess', false);
        this.set('isError', true);
      }
      return true;
    } else {
      return false;
    }
  },


  parseInfo: function (polledData) {
    console.log('TRACE: Entering task info function');
    var self = this;
    var totalProgress = 0;
    var tasksData = polledData.tasks;
    console.log("The value of tasksData is: ", tasksData);
    if (!tasksData) {
      console.log("ERROR: NO tasks available to process");
    }
    var requestId = this.get('requestId');
    if (polledData.Requests && polledData.Requests.id && polledData.Requests.id != requestId) {
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      return false;
    }
    this.replacePolledData(tasksData);
    var totalProgress = this.calculateProgressByTasks(tasksData);
    this.set('progress', totalProgress.toString());
    console.log("INFO: right now the progress is: " + this.get('progress'));
    return this.isPollingFinished(tasksData);
  }

});

