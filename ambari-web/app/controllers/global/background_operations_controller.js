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
   * Number of operation to load
   */
  operationsCount: 10,
  /**
   * Possible levels:
   * OPS_LIST
   * HOSTS_LIST
   * TASKS_LIST
   * TASK_DETAILS
   */
  levelInfo: Em.Object.create({
    name: "OPS_LIST",
    requestId: null,
    taskId: null
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
    return !this.isInitLoading();
  },

  /**
   * indicate whether data for current level has already been loaded or not
   * @return {Boolean}
   */
  isInitLoading: function () {
    var levelInfo = this.get('levelInfo');
    var request = this.get('services').findProperty('id', levelInfo.get('requestId'));

    if (levelInfo.get('name') === 'HOSTS_LIST') {
      return !!(request && App.isEmptyObject(request.get('hostsMap')));
    }
    return false;
  },
  /**
   * construct params of ajax query regarding displayed level
   */
  getQueryParams: function () {
    var levelInfo = this.get('levelInfo');
    var count = this.get('operationsCount');
    var result = {
      name: 'background_operations.get_most_recent',
      successCallback: 'callBackForMostRecent',
      data: {
        'operationsCount': count
      }
    };
    if (levelInfo.get('name') === 'TASK_DETAILS' && !App.get('testMode')) {
      result.name = 'background_operations.get_by_task';
      result.successCallback = 'callBackFilteredByTask';
      result.data = {
        'taskId': levelInfo.get('taskId'),
        'requestId': levelInfo.get('requestId')
      };
    } else if (levelInfo.get('name') === 'TASKS_LIST' || levelInfo.get('name') === 'HOSTS_LIST') {
      result.name = 'background_operations.get_by_request';
      result.successCallback = 'callBackFilteredByRequest';
      result.data = {
        'requestId': levelInfo.get('requestId')
      };
    }
    return result;
  },

  /**
   * Push hosts and their tasks to request
   * @param data
   * @param ajaxQuery
   * @param params
   */
  callBackFilteredByRequest: function (data, ajaxQuery, params) {
    var requestId = data.Requests.id;
    var requestInputs = data.Requests.inputs;
    var request = this.get('services').findProperty('id', requestId);
    var hostsMap = {};
    var previousTaskStatusMap = request.get('previousTaskStatusMap');
    var currentTaskStatusMap = {};
    data.tasks.forEach(function (task) {
      var host = hostsMap[task.Tasks.host_name];
      task.Tasks.request_id = requestId;
      task.Tasks.request_inputs = requestInputs;
      if (host) {
        host.logTasks.push(task);
        host.isModified = (host.isModified) ? true : previousTaskStatusMap[task.Tasks.id] !== task.Tasks.status;
      } else {
        hostsMap[task.Tasks.host_name] = {
          name: task.Tasks.host_name,
          publicName: task.Tasks.host_name,
          logTasks: [task],
          isModified: previousTaskStatusMap[task.Tasks.id] !== task.Tasks.status
        };
      }
      currentTaskStatusMap[task.Tasks.id] = task.Tasks.status;
    }, this);
    /**
     * sync up request progress with up to date progress of hosts on Host's list,
     * to avoid discrepancies while waiting for response with latest progress of request
     * after switching to operation's list
     */
    if (request.get('isRunning')) {
      request.set('progress', App.HostPopup.getProgress(data.tasks));
      request.set('status', App.HostPopup.getStatus(data.tasks)[0]);
      request.set('isRunning', request.get('progress') !== 100);
    }
    request.set('previousTaskStatusMap', currentTaskStatusMap);
    request.set('hostsMap', hostsMap);
    this.set('serviceTimestamp', App.dateTime());
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

    // Put some command information to task object
    task.Tasks.command = data.Tasks.command;
    task.Tasks.custom_command_name = data.Tasks.custom_command_name;
    task.Tasks.structured_out = data.Tasks.structured_out;

    task.Tasks.output_log = data.Tasks.output_log;
    task.Tasks.error_log = data.Tasks.error_log;
    this.set('serviceTimestamp', App.dateTime());
  },

  /**
   * returns true if it's upgrade equest
   * use this flag to exclude upgrade requests from bgo
   * @param {object} request
   * @returns {boolean}
   */
  isUpgradeRequest: function(request) {
    var context = Em.get(request, 'Requests.request_context');
    return context ? /(upgrading|downgrading)/.test(context.toLowerCase()) : false;
  },
  /**
   * Prepare, received from server, requests for host component popup
   * @param data
   */
  callBackForMostRecent: function (data) {
    var runningServices = 0;
    var currentRequestIds = [];
    var countIssued = this.get('operationsCount');
    var countGot = data.itemTotal;
    var restoreUpgradeState = false;

    data.items.forEach(function (request) {
      if (this.isUpgradeRequest(request)) {
        if (!App.get('upgradeIsRunning') && !App.get('testMode')) {
          restoreUpgradeState = true;
        }
        return;
      }
      var rq = this.get("services").findProperty('id', request.Requests.id);
      var isRunning = request.Requests.request_status === 'IN_PROGRESS';
      var requestParams = this.parseRequestContext(request.Requests.request_context);
      this.assignScheduleId(request, requestParams);
      currentRequestIds.push(request.Requests.id);

      if (rq) {
        rq.setProperties({
          progress: Math.floor(request.Requests.progress_percent),
          status: request.Requests.request_status,
          isRunning: isRunning,
          startTime: App.dateTimeWithTimeZone(request.Requests.start_time),
          endTime: request.Requests.end_time > 0 ? App.dateTimeWithTimeZone(request.Requests.end_time) : request.Requests.end_time
        });
      } else {
        rq = Em.Object.create({
          id: request.Requests.id,
          name: requestParams.requestContext,
          displayName: requestParams.requestContext,
          progress: Math.floor(request.Requests.progress_percent),
          status: request.Requests.request_status,
          isRunning: isRunning,
          hostsMap: {},
          tasks: [],
          startTime: App.dateTimeWithTimeZone(request.Requests.start_time),
          endTime: request.Requests.end_time > 0 ? App.dateTimeWithTimeZone(request.Requests.end_time) : request.Requests.end_time,
          dependentService: requestParams.dependentService,
          sourceRequestScheduleId: request.Requests.request_schedule && request.Requests.request_schedule.schedule_id,
          previousTaskStatusMap: {},
          contextCommand: requestParams.contextCommand
        });
        this.get("services").unshift(rq);
        //To sort DESC by request id
        this.set("services", this.get("services").sortProperty('id').reverse());
      }
      runningServices += ~~isRunning;
    }, this);
    if (restoreUpgradeState) {
      App.router.get('clusterController').restoreUpgradeState();
    }
    this.removeOldRequests(currentRequestIds);
    this.set("allOperationsCount", runningServices);
    this.set('isShowMoreAvailable', countGot > countIssued);
    this.set('serviceTimestamp', App.dateTimeWithTimeZone());
  },

  isShowMoreAvailable: null,

  /**
   * remove old requests
   * as API returns 10, or  20 , or 30 ...etc latest request, the requests that absent in response should be removed
   * @param currentRequestIds
   */
  removeOldRequests: function (currentRequestIds) {
    var services = this.get('services');

    for (var i = 0, l = services.length; i < l; i++) {
      if (!currentRequestIds.contains(services[i].id)) {
        services.splice(i, 1);
        i--;
        l--;
      }
    }
  },

  /**
   * identify whether there is only one host in request
   * @param inputs
   * @return {Boolean}
   */
  isOneHost: function (inputs) {
    if (!inputs) {
      return false;
    }
    inputs = JSON.parse(inputs);
    if (inputs && inputs.included_hosts) {
      return inputs.included_hosts.split(',').length < 2;
    }
    return false
  },
  /**
   * assign schedule_id of request to null if it's Recommission operation
   * @param request
   * @param requestParams
   */
  assignScheduleId: function (request, requestParams) {
    var oneHost = this.isOneHost(request.Requests.inputs);
    if (request.Requests.request_schedule && oneHost && /Recommission/.test(requestParams.requestContext)) {
      request.Requests.request_schedule.schedule_id = null;
    }
  },

  /**
   * parse request context and if keyword "_PARSE_" is present then format it
   * @param {string} requestContext
   * @return {Object}
   */
  parseRequestContext: function (requestContext) {
    var context = {};
    if (requestContext) {
      if (requestContext.indexOf(App.BackgroundOperationsController.CommandContexts.PREFIX) !== -1) {
        context = this.getRequestContextWithPrefix(requestContext);
      } else {
        context.requestContext = requestContext;
      }
    } else {
      context.requestContext = Em.I18n.t('requestInfo.unspecified');
    }
    return context;
  },

  /**
   *
   * @param {string} requestContext
   * @returns {{requestContext: *, dependentService: *, contextCommand: *}}
   */
  getRequestContextWithPrefix: function (requestContext) {
    var contextSplits = requestContext.split('.'),
        parsedRequestContext,
        contextCommand = contextSplits[1],
        service = contextSplits[2];

    switch (contextCommand) {
      case "STOP":
      case "START":
        if (service === 'ALL_SERVICES') {
          parsedRequestContext = Em.I18n.t("requestInfo." + contextCommand.toLowerCase()).format(Em.I18n.t('common.allServices'));
        } else {
          parsedRequestContext = Em.I18n.t("requestInfo." + contextCommand.toLowerCase()).format(App.format.role(service, true));
        }
        break;
      case "ROLLING-RESTART":
        parsedRequestContext = Em.I18n.t("rollingrestart.rest.context").format(App.format.role(service, true), contextSplits[3], contextSplits[4]);
        break;
    }
    return {
      requestContext: parsedRequestContext,
      dependentService: service,
      contextCommand: contextCommand
    }
  },

  popupView: null,

  /**
   * Onclick handler for background operations number located right to logo
   */
  showPopup: function () {
    // load the checkbox on footer first, then show popup.
    var self = this;
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      App.updater.immediateRun('requestMostRecent');

      App.HostPopup.set("breadcrumbs", [ App.HostPopup.get("rootBreadcrumb") ]);

      if (self.get('popupView') && App.HostPopup.get('isBackgroundOperations')) {
        self.set('popupView.isNotShowBgChecked', !initValue);
        self.set('popupView.isOpen', true);
        var el = $(self.get('popupView.element'));
        el.appendTo('#wrapper');
        el.find('.modal').show();
      } else {
        self.set('popupView', App.HostPopup.initPopup("", self, true));
        self.set('popupView.isNotShowBgChecked', !initValue);
      }
    });
  },

  /**
   * Called on logout
   */
  clear: function () {
    // set operations count to default value
    this.set('operationsCount', 10);
  }

});

/**
 * Each background operation has a context in which it operates.
 * Generally these contexts are fixed messages. However, we might
 * want to associate semantics to this context - like showing, disabling
 * buttons when certain operations are in progress.
 *
 * To make this possible we have command contexts where the context
 * is not a human readable string, but a pattern indicating the command
 * it is running. When UI shows these, they are translated into human
 * readable strings.
 *
 * General pattern of context names is "_PARSE_.{COMMAND}.{ID}[.{Additional-Data}...]"
 */
App.BackgroundOperationsController.CommandContexts = {
  PREFIX : "_PARSE_",
  /**
   * Stops all services
   */
  STOP_ALL_SERVICES : "_PARSE_.STOP.ALL_SERVICES",
  /**
   * Starts all services
   */
  START_ALL_SERVICES : "_PARSE_.START.ALL_SERVICES",
  /**
   * Starts service indicated by serviceID.
   * @param {String} serviceID Parameter {0}. Example: HDFS
   */
  START_SERVICE : "_PARSE_.START.{0}",
  /**
   * Stops service indicated by serviceID.
   * @param {String} serviceID Parameter {0}. Example: HDFS
   */
  STOP_SERVICE : "_PARSE_.STOP.{0}",
  /**
   * Performs rolling restart of componentID in batches.
   * This context is the batchNumber batch out of totalBatchCount batches.
   * @param {String} componentID Parameter {0}. Example "DATANODE"
   * @param {Number} batchNumber Parameter {1}. Batch number of this batch. Example 3.
   * @param {Number} totalBatchCount Parameter {2}. Total number of batches. Example 10.
   */
  ROLLING_RESTART : "_PARSE_.ROLLING-RESTART.{0}.{1}.{2}"
};
