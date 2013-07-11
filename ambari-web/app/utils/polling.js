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
App.Poll = Em.Object.extend({
  name: '',
  stage: '',
  label: '',
  isStarted: false,
  isPolling: true,
  clusterName: null,
  requestId: undefined,
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

  barWidth: function () {
    var barWidth = 'width: ' + this.get('progress') + '%;';
    return barWidth;
  }.property('progress'),

  isCompleted: function () {
    return (this.get('isError') || this.get('isSuccess'));
  }.property('isError', 'isSuccess'),

  showLink: function () {
    return (this.get('isPolling') === true && this.get('isStarted') === true);
  }.property('isPolling', 'isStarted'),

  start: function () {
    if (this.get('requestId') === undefined) {
      this.setRequestId();
    } else {
      this.startPolling();
    }
  },

  setRequestId: function () {
    if (App.testMode) {
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
      async: false,
      data: data,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: Polling -> value of the url is: " + url);
        console.log("TRACE: Polling-> value of the sent data is: " + self.get('data'));
        console.log("TRACE: Polling-> value of the received data is: " + jsonData);
        if (jsonData === null) {
          self.set('isSuccess', true);
          self.set('isError', false);
          self.set('requestId',undefined);
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

  doPolling: function () {
    if (this.get('requestId')) {
      this.startPolling();
    }
  },

  startPolling: function () {
    if (!this.get('requestId')) {
      return;
    }
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/requests/' + this.get('requestId') + '?fields=tasks/*';
    if (App.testMode) {
      this.set('POLL_INTERVAL', 1);
      this.numPolls++;
      url = this.get('mockDataPrefix') + '/poll_' + this.get('numPolls') + '.json';
    }

    $.ajax({
      type: 'GET',
      url: url,
      async: true,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        console.log("TRACE: In success function for the GET logs data");
        console.log("TRACE: The value is: ", jQuery.parseJSON(data));
        var result = self.parseInfo(jQuery.parseJSON(data));
        if (result !== true) {
          window.setTimeout(function () {
            self.startPolling();
          }, self.POLL_INTERVAL);
        } else {
          self.set('requestId', undefined);
        }
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the GET data");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);
        if (!self.get('isSuccess')) {
          self.set('isError', true);
        }
      },

      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
      function () {
        App.showReloadPopup();
        console.log('Install services all retries failed');
      }
    );
  },

  stopPolling: function () {
    //this.set('isSuccess', true);
  },

  replacePolledData: function (polledData) {
    this.polledData.clear();
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
      // We dont want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we dont want to keep polling if it is not the
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

