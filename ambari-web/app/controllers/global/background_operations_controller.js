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

App.BackgroundOperationsController = Em.Controller.extend({
  name: 'backgroundOperationsController',

  /**
   * Whether we need to refresh background operations or not
   */
  isWorking : false,

  allOperationsCount : 0,

  /**
   * For host component popup
   */
  services:[],
  serviceTimestamp: null,
  /**
   * Possible levels:
   * REQUESTS_LIST
   * HOSTS_LIST
   * TASKS_LIST
   * TASK_DETAILS
   */
  levelInfo: Em.Object.create({
    name: 'REQUESTS_LIST',
    requestId: null,
    taskId: null,
    sync: false
  }),

  /**
   * Start polling, when <code>isWorking</code> become true
   */
  startPolling: function(){
    if(this.get('isWorking')){
      this.requestMostRecent();
      App.updater.run(this, 'requestMostRecent', 'isWorking', App.bgOperationsUpdateInterval);
    }
  }.observes('isWorking'),

  /**
   * Get requests data from server
   * @param callback
   */
  requestMostRecent: function (callback) {
    var queryParams = this.getQueryParams();
    App.ajax.send({
      'name': queryParams.name,
      'sender': this,
      'success': queryParams.successCallback,
      'callback': callback,
      'data': queryParams.data
    });
  },
  /**
   * construct params of ajax query regarding displayed level
   */
  getQueryParams: function () {
    var levelInfo = this.get('levelInfo');
    var result = {
      name: 'background_operations.get_most_recent',
      successCallback: 'callBackForMostRecent',
      data: {}
    };
    if (levelInfo.get('name') === 'TASK_DETAILS' && !App.testMode) {
      result.name = 'background_operations.get_by_task';
      result.successCallback = 'callBackFilteredByTask';
      result.data = {
        'taskId': levelInfo.get('taskId'),
        'requestId': levelInfo.get('requestId'),
        'sync': levelInfo.get('sync')
      };
    } else if (levelInfo.get('name') === 'TASKS_LIST' || levelInfo.get('name') === 'HOSTS_LIST') {
      result.name = 'background_operations.get_by_request';
      result.successCallback = 'callBackFilteredByRequest';
      result.data = {
        'requestId': levelInfo.get('requestId'),
        'sync': levelInfo.get('sync')
      };
    }
    levelInfo.set('sync', false);
    return result;
  },

  /**
   * Push hosts and their tasks to request
   * @param data
   * @param ajaxQuery
   * @param params
   */
  callBackFilteredByRequest: function (data, ajaxQuery, params) {
    var hostsMap = {};
    var request = this.get('services').findProperty('id', data.Requests.id);
    data.tasks.forEach(function (task) {
      if (hostsMap[task.Tasks.host_name]) {
        hostsMap[task.Tasks.host_name].logTasks.push(task);
      } else {
        hostsMap[task.Tasks.host_name] = {
          name: task.Tasks.host_name,
          publicName: task.Tasks.host_name,
          logTasks: [task]
        };
      }
    }, this);
    request.set('hostsMap', hostsMap);
    this.set('serviceTimestamp', new Date().getTime());
  },
  /**
   * Update task, with uploading two additional properties: stdout and stderr
   * @param data
   * @param ajaxQuery
   * @param params
   */
  callBackFilteredByTask: function (data, ajaxQuery, params) {
    var request = this.get('services').findProperty('id', data.Tasks.request_id);
    var host = request.get('hostsMap')[data.Tasks.host_name];
    var task = host.logTasks.findProperty('Tasks.id', data.Tasks.id);
    task.Tasks.status = data.Tasks.status;
    task.Tasks.stdout = data.Tasks.stdout;
    task.Tasks.stderr = data.Tasks.stderr;
    this.set('serviceTimestamp', new Date().getTime());
  },

  /**
   * Prepare, received from server, requests for host component popup
   * @param data
   */
  callBackForMostRecent: function (data) {
    var runningServices = 0;
    var self = this;
    var currentRequestIds = [];

    data.items.forEach(function (request) {
      var rq = self.get("services").findProperty('id', request.Requests.id);
      var isRunning = (request.Requests.task_count -
        (request.Requests.aborted_task_count + request.Requests.completed_task_count + request.Requests.failed_task_count
         + request.Requests.timed_out_task_count - request.Requests.queued_task_count)) > 0;
      var requestParams = this.parseRequestContext(request.Requests.request_context);
      currentRequestIds.push(request.Requests.id);
      if (rq) {
        rq.set('progress', Math.ceil(request.Requests.progress_percent));
        rq.set('status', request.Requests.request_status);
        rq.set('isRunning', isRunning);
      } else {
        rq = Em.Object.create({
          id: request.Requests.id,
          name: requestParams.requestContext,
          displayName: requestParams.requestContext,
          progress: Math.ceil(request.Requests.progress_percent),
          status: request.Requests.request_status,
          isRunning: isRunning,
          hostsMap: {},
          tasks: [],
          dependentService: requestParams.dependentService
        });
        self.get("services").unshift(rq);
      }
      runningServices += ~~isRunning;
    }, this);
    //remove old request if it's absent in API response
    self.get('services').forEach(function(service, index, services){
      if(!currentRequestIds.contains(service.id)) {
        services.splice(index, 1);
      }
    });
    self.set("allOperationsCount", runningServices);
    self.set('serviceTimestamp', new Date().getTime());
  },

  /**
   * parse request context and if keyword "_PARSE_" is present then format it
   * @param requestContext
   * @return {Object}
   */
  parseRequestContext: function (requestContext) {
    var parsedRequestContext;
    var service;
    var command;
    if (requestContext) {
      if (requestContext.indexOf("_PARSE_") !== -1) {
        command = requestContext.split('.')[1];
        service = requestContext.split('.')[2];
        if (service === 'ALL_SERVICES') {
          parsedRequestContext = Em.I18n.t("requestInfo." + command.toLowerCase()).format(Em.I18n.t('common.allServices'));
        } else {
          parsedRequestContext = Em.I18n.t("requestInfo." + command.toLowerCase()).format(App.Service.DisplayNames[service]);
        }
      } else {
        parsedRequestContext = requestContext;
      }
    } else {
      parsedRequestContext = Em.I18n.t('requestInfo.unspecified');
    }
    return {
      requestContext: parsedRequestContext,
      dependentService: service
    }
  },

  popupView: null,

  /**
   * Onclick handler for background operations number located right to logo
   * @return PopupObject For testing purposes
   */
  showPopup: function(){
    // load the checkbox on footer first, then show popup.
    var self = this;
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      App.updater.immediateRun('requestMostRecent');
      if(self.get('popupView') && App.HostPopup.get('isBackgroundOperations')){
        self.set ('popupView.isNotShowBgChecked', !initValue);
        self.set('popupView.isOpen', true);
        $(self.get('popupView.element')).appendTo('#wrapper');
      } else {
        self.set('popupView', App.HostPopup.initPopup("", self, true));
        self.set ('popupView.isNotShowBgChecked', !initValue);
      }
    });
  }

});
