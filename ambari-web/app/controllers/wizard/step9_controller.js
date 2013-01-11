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

App.WizardStep9Controller = Em.Controller.extend({
  name: 'wizardStep9Controller',
  hosts: [],
  progress: '0',

  isStepCompleted: false,

  isSubmitDisabled: function () {
    // return !this.get('isStepCompleted');
    return !['STARTED','START FAILED'].contains(this.get('content.cluster.status'));
  }.property('content.cluster.status'),

  mockHostData: require('data/mock/step9_hosts'),
  mockDataPrefix: '/data/wizard/deploy/5_hosts',
  pollDataCounter: 0,
  polledData: [],

  status: function () {
    if(this.get('progress') != '100') {
      return 'info';
    }

    if (this.hosts.someProperty('status', 'failed')) {
      return 'failed';
    } else if (this.hosts.someProperty('status', 'warning')) {
      return 'warning';
    }

    return 'success';
  }.property('hosts.@each.status', 'progress'),

  showRetry: function () {
    return this.get('content.cluster.status') == 'INSTALL FAILED';
  }.property('content.cluster.status'),

  // content.cluster.status can be:
  // PENDING: set upon successful transition from step 1 to step 2
  // INSTALLED: set upon successful completion of install phase as well as successful invocation of start services API
  // STARTED: set up on successful completion of start phase
  // INSTALL FAILED: set up upon encountering a failure in install phase
  // START FAILED: set upon unsuccessful invocation of start services API and also upon encountering a failure
  // during start phase

  // content.cluster.isCompleted
  // set to false upon successful transition from step 1 to step 2
  // set to true upon successful start of services in this step
  // note: looks like this is the same thing as checking content.cluster.status == 'STARTED'


  // navigateStep is called by App.WizardStep9View's didInsertElement and "retry" from router.
  navigateStep: function () {
    if (App.testMode) {
      // this is for repeatedly testing out installs in test mode
      this.set('content.cluster.status', 'PENDING');
      this.set('content.cluster.isCompleted', false);
    }
    var clusterStatus = this.get('content.cluster.status');
    console.log('navigateStep: clusterStatus = ' + clusterStatus);
    if (this.get('content.cluster.isCompleted') === false) {
      // the cluster has not yet successfully installed and started
      if (clusterStatus === 'INSTALL FAILED') {
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
        this.set('content.cluster.isStepCompleted', true);
      } else if (clusterStatus === 'START FAILED') {
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
        // this.hosts.setEach('status', 'info');
        this.set('isStepCompleted', true);
      } else {
        // handle PENDING, INSTALLED
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
        this.startPolling();
      }
    } else {
      // handle STARTED
      // the cluster has successfully installed and started
      this.loadStep();
      this.loadLogData(this.get('content.cluster.requestId'));
      this.set('isStepCompleted', true);
      this.set('progress', '100');
    }
  },
  clearStep: function () {
    this.hosts.clear();
    this.set('status', 'info');
    this.set('progress', '0');
    this.set('isStepCompleted', false);
    this.numPolls = 0;
  },

  loadStep: function () {
    console.log("TRACE: Loading step9: Install, Start and Test");
    this.clearStep();
    this.renderHosts(this.loadHosts());
  },

  loadHosts: function () {
    var hostInfo = this.get('content.hosts');
    var hosts = new Ember.Set();
    for (var index in hostInfo) {
      var obj = Em.Object.create(hostInfo[index]);
      obj.message = '';
      obj.progress = 0;
      obj.status = 'info';
      obj.tasks = [];
      obj.logTasks = [];
      hosts.add(obj);
      console.log("TRACE: host name is: " + hostInfo[index].name);
    }
    return hosts.filterProperty('bootStatus', 'REGISTERED');
  },

  // sets this.hosts, where each element corresponds to a status and progress info on a host
  renderHosts: function (hostsInfo) {
    hostsInfo.forEach(function (_hostInfo) {
      var hostInfo = App.HostInfo.create({
        name: _hostInfo.name,
        status: _hostInfo.status,
        tasks: _hostInfo.tasks,
        logTasks: _hostInfo.logTasks,
        message: _hostInfo.message,
        progress: _hostInfo.progress
      });
      console.log('pushing ' + hostInfo.name);
      this.hosts.pushObject(hostInfo);
    }, this);
  },

  replacePolledData: function (polledData) {
    this.polledData.clear();
    this.set('polledData', polledData);
  },

  displayMessage: function (task) {
    var role = App.format.role(task.role);
    console.log("In display message with task command value: " + task.command);
    switch (task.command) {
      case 'INSTALL':
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to install ' + role;
          case 'QUEUED' :
            return 'Waiting to install ' + role;
          case 'IN_PROGRESS':
            return 'Installing ' + role;
          case 'COMPLETED' :
            return 'Successfully installed ' + role;
          case 'FAILED':
            return 'Failed to install ' + role;
        }
      case 'UNINSTALL':
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to uninstall ' + role;
          case 'QUEUED' :
            return 'Waiting to uninstall ' + role;
          case 'IN_PROGRESS':
            return 'Uninstalling ' + role;
          case 'COMPLETED' :
            return 'Successfully uninstalled ' + role;
          case 'FAILED':
            return 'Failed to uninstall ' + role;
        }
      case 'START' :
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to start ' + role;
          case 'QUEUED' :
            return 'Waiting to start ' + role;
          case 'IN_PROGRESS':
            return 'Starting ' + role;
          case 'COMPLETED' :
            return role + ' started successfully';
          case 'FAILED':
            return role + ' failed to start';
        }
      case 'STOP' :
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to stop ' + role;
          case 'QUEUED' :
            return 'Waiting to stop ' + role;
          case 'IN_PROGRESS':
            return 'Stopping ' + role;
          case 'COMPLETED' :
            return role + ' stopped successfully';
          case 'FAILED':
            return role + ' failed to stop';
        }
      case 'EXECUTE' :
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to execute ' + role;
          case 'QUEUED' :
            return 'Waiting to execute ' + role;
          case 'IN_PROGRESS':
            return 'Executing ' + role;
          case 'COMPLETED' :
            return role + ' executed successfully';
          case 'FAILED':
            return role + ' failed to execute';
        }
      case 'ABORT' :
        switch (task.status) {
          case 'PENDING':
            return 'Preparing to abort ' + role;
          case 'QUEUED' :
            return 'Waiting to abort ' + role;
          case 'IN_PROGRESS':
            return 'Aborting ' + role;
          case 'COMPLETED' :
            return role + ' aborted successfully';
          case 'FAILED':
            return role + ' failed to abort';
        }
    }
  },

  launchStartServices: function () {
    var self = this;
    var clusterName = this.get('content.cluster.name');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services?ServiceInfo/state=INSTALLED';
    var data = '{"ServiceInfo": {"state": "STARTED"}}';
    var method = 'PUT';

    if (this.get('content.controllerName') === 'addHostController') {
      url = App.apiPrefix + '/clusters/' + clusterName + '/host_components?HostRoles/component_name=GANGLIA_MONITOR|HostRoles/component_name=HBASE_REGIONSERVER|HostRoles/component_name=DATANODE|HostRoles/component_name=TASKTRACKER&HostRoles/state=INSTALLED';
      data = '{"HostRoles": {"state": "STARTED"}}';
    }

    if (App.testMode) {
      url = this.get('mockDataPrefix') + '/poll_6.json';
      method = 'GET';
      this.numPolls = 6;
    }

    $.ajax({
      type: method,
      url: url,
      async: false,
      data: data,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: Step9 -> In success function for the startService call");
        console.log("TRACE: Step9 -> value of the url is: " + url);
        console.log("TRACE: Step9 -> value of the received data is: " + jsonData);
        var requestId = jsonData.Requests.id;
        console.log('requestId is: ' + requestId);
        var clusterStatus = {
          status: 'INSTALLED',
          requestId: requestId,
          isStartError: false,
          isCompleted: false
        };

        App.router.get(self.get('content.controllerName')).saveClusterStatus(clusterStatus);
        self.startPolling();
      },

      error: function () {
        console.log("ERROR");
        var clusterStatus = {
          status: 'START FAILED',
          isStartError: true,
          isCompleted: false
        };

        App.router.get(self.get('content.controllerName')).saveClusterStatus(clusterStatus);
      },

      statusCode: require('data/statusCodes')
    });
  },

  // marks a host's status as "success" if all tasks are in COMPLETED state
  onSuccessPerHost: function (actions, contentHost) {
    if (actions.everyProperty('Tasks.status', 'COMPLETED') && this.get('content.cluster.status') === 'INSTALLED') {
      contentHost.set('status', 'success');
    }
  },

  // marks a host's status as "warning" if at least one of the tasks is FAILED, ABORTED, or TIMEDOUT.
  onWarningPerHost: function (actions, contentHost) {
    if (actions.someProperty('Tasks.status', 'FAILED') || actions.someProperty('Tasks.status', 'ABORTED') || actions.someProperty('Tasks.status', 'TIMEDOUT')) {
      console.log('step9: In warning');
      contentHost.set('status', 'warning');
      this.set('status', 'warning');
    }
  },

  onInProgressPerHost: function (tasks, contentHost) {
    var runningAction = tasks.findProperty('Tasks.status', 'IN_PROGRESS');
    if (runningAction === undefined || runningAction === null) {
      runningAction = tasks.findProperty('Tasks.status', 'QUEUED');
    }
    if (runningAction === undefined || runningAction === null) {
      runningAction = tasks.findProperty('Tasks.status', 'PENDING');
    }
    if (runningAction !== null && runningAction !== undefined) {
      contentHost.set('message', this.displayMessage(runningAction.Tasks));
    }
  },

  progressPerHost: function (actions, contentHost) {
    var progress = 0;
    var actionsPerHost = actions.length;
    // TODO: consolidate to a single filter function for better performance
    var completedActions = actions.filterProperty('Tasks.status', 'COMPLETED').length
      + actions.filterProperty('Tasks.status', 'FAILED').length
      + actions.filterProperty('Tasks.status', 'ABORTED').length
      + actions.filterProperty('Tasks.status', 'TIMEDOUT').length;

    // for the install phase (PENDING), % completed per host goes up to 33%; floor(100 / 3)
    // for the start phase (INSTALLED), % completed starts from 34%
    switch (this.get('content.cluster.status')) {
      case 'PENDING':
        progress = Math.floor(((completedActions / actionsPerHost) * 100) / 3);
        break;
      case 'INSTALLED':
        progress = 34 + Math.floor(((completedActions / actionsPerHost) * 100 * 2) / 3);
        break;
      default:
        progress = 100;
        break;
    }
    console.log('INFO: progressPerHost is: ' + progress);
    contentHost.set('progress', progress.toString());
    return progress;
  },

  isSuccess: function (polledData) {
    return polledData.everyProperty('Tasks.status', 'COMPLETED');
  },

  // for DATANODE, TASKTRACKER, HBASE_REGIONSERVER, and GANGLIA_MONITOR, if more than 50% fail, then it's a fatal error;
  // otherwise, it's only a warning and installation/start can continue
  getSuccessFactor: function (role) {
    return ['DATANODE', 'TASKTRACKER', 'HBASE_REGIONSERVER', 'GANGLIA_MONITOR'].contains(role) ? 50 : 100;
  },

  isStepFailed: function (polledData) {
    var failed = false;
    polledData.forEach(function (_polledData) {
      var successFactor = this.getSuccessFactor(_polledData.Tasks.role);
      console.log("Step9: isStepFailed sf value: " + successFactor);
      var actionsPerRole = polledData.filterProperty('Tasks.role', _polledData.Tasks.role);
      var actionsFailed = actionsPerRole.filterProperty('Tasks.status', 'FAILED');
      var actionsAborted = actionsPerRole.filterProperty('Tasks.status', 'ABORTED');
      var actionsTimedOut = actionsPerRole.filterProperty('Tasks.status', 'TIMEDOUT');
      if ((((actionsFailed.length + actionsAborted.length + actionsTimedOut.length) / actionsPerRole.length) * 100) > (100 - successFactor)) {
        console.log('TRACE: Entering success factor and result is failed');
        failed = true;
      }
    }, this);
    return failed;
  },

  getFailedHostsForFailedRoles: function (polledData) {
    var hostArr = new Ember.Set();
    polledData.forEach(function (_polledData) {
      var successFactor = this.getSuccessFactor(_polledData.Tasks.role);
      var actionsPerRole = polledData.filterProperty('Tasks.role', _polledData.Tasks.role);
      var actionsFailed = actionsPerRole.filterProperty('Tasks.status', 'FAILED');
      var actionsAborted = actionsPerRole.filterProperty('Tasks.status', 'ABORTED');
      var actionsTimedOut = actionsPerRole.filterProperty('Tasks.status', 'TIMEDOUT');
      if ((((actionsFailed.length + actionsAborted.length + actionsTimedOut.length) / actionsPerRole.length) * 100) > (100 - successFactor)) {
        actionsFailed.forEach(function (_actionFailed) {
          hostArr.add(_actionFailed.Tasks.host_name);
        });
        actionsAborted.forEach(function (_actionFailed) {
          hostArr.add(_actionFailed.Tasks.host_name);
        });
        actionsTimedOut.forEach(function (_actionFailed) {
          hostArr.add(_actionFailed.Tasks.host_name);
        });
      }
    }, this);
    return hostArr;
  },

  setHostsStatus: function (hostNames, status) {
    hostNames.forEach(function (_hostName) {
      var host = this.hosts.findProperty('name', _hostName);
      if (host) {
        host.set('status', status).set('progress', '100');
      }
    }, this);
  },

  // makes a state transition
  // PENDING -> INSTALLED
  // PENDING -> INSTALL FAILED
  // INSTALLED -> STARTED
  // INSTALLED -> START_FAILED
  // returns true if polling should stop; false otherwise
  // polling from ui stops only when no action has 'PENDING', 'QUEUED' or 'IN_PROGRESS' status
  finishState: function (polledData) {
    var clusterStatus = {};
    var requestId = this.get('content.cluster.requestId');
    if (this.get('content.cluster.status') === 'INSTALLED') {
      if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
        this.set('progress', '100');
        clusterStatus = {
          status: 'INSTALLED',
          requestId: requestId,
          isCompleted: true
        };
        if (this.isSuccess(polledData)) {
          clusterStatus.status = 'STARTED';
          var serviceStartTime = new Date().getTime();
          var timeToStart = ((parseInt(serviceStartTime) - parseInt(this.get('content.cluster.installStartTime'))) / 60000).toFixed(2);
          clusterStatus.installTime = timeToStart;
          this.set('status', 'success');
        } else {
          if (this.isStepFailed(polledData)) {
            clusterStatus.status = 'START FAILED'; // 'START FAILED' implies to step10 that installation was successful but start failed
            this.set('status', 'failed');
            this.setHostsStatus(this.getFailedHostsForFailedRoles(polledData), 'failed');
          } else {
            clusterStatus.status = 'START FAILED';
            this.set('status', 'warning');
          }
        }
        App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
        this.set('isStepCompleted', true);
        this.setTasksPerHost();
        App.router.get(this.get('content.controllerName')).saveInstalledHosts(this);
        return true;
      }
    } else if (this.get('content.cluster.status') === 'PENDING') {
      if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
        clusterStatus = {
          status: 'PENDING',
          requestId: requestId,
          isCompleted: false
        }
        if (this.isStepFailed(polledData)) {
          console.log("In installation failure");
          clusterStatus.status = 'INSTALL FAILED';
          this.set('progress', '100');
          this.set('status', 'failed');
          this.setHostsStatus(this.getFailedHostsForFailedRoles(polledData), 'failed');
          App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
          this.set('isStepCompleted', true);
        } else {
          clusterStatus.status = 'INSTALLED';
          this.set('progress', '34');
          this.launchStartServices();
        }
        this.setTasksPerHost();
        App.router.get(this.get('content.controllerName')).saveInstalledHosts(this);
        return true;
      }
    } else if (this.get('content.cluster.status') === 'INSTALL FAILED') {
      this.set('progress', '100');
      this.set('status', 'failed');
      return true;
    } else if (this.get('content.cluster.status') === 'START FAILED') {
      this.set('progress', '100');
      this.set('status', 'failed');
      return true;
    } else if (this.get('content.cluster.status') === 'STARTED') {
      this.set('progress', '100');
      this.set('status', 'success');
      return true;
    }

    return false;
  },

  setTasksPerHost: function () {
    var tasksData = this.get('polledData');
    this.get('hosts').forEach(function (_host) {
      var tasksPerHost = tasksData.filterProperty('Tasks.host_name', _host.name); // retrieved from polled Data
      if (tasksPerHost.length === 0) {
        //alert('For testing with mockData follow the sequence: hit referesh,"mockData btn", "pollData btn", again "pollData btn"');
        //exit();
      }
      if (tasksPerHost !== null && tasksPerHost !== undefined && tasksPerHost.length !== 0) {
        tasksPerHost.forEach(function (_taskPerHost) {
          console.log('In step9 _taskPerHost function.');
          //if (_taskPerHost.Tasks.status !== 'PENDING' && _taskPerHost.Tasks.status !== 'QUEUED' &&  _taskPerHost.Tasks.status !== 'IN_PROGRESS') {
          _host.tasks.pushObject(_taskPerHost);
          //}
        }, this);
      }
    }, this);
  },

  // This is done at HostRole level.
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    console.log('In step9 setTasksStatePerHost function.');
    tasksPerHost.forEach(function (_task) {
      console.log('In step9 _taskPerHost function.');
      //if (_task.Tasks.status !== 'PENDING' && _task.Tasks.status !== 'QUEUED') {
      var task = host.logTasks.findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        host.logTasks.removeObject(task);
      }
      host.logTasks.pushObject(_task);
      //}
    }, this);
  },

  parseHostInfo: function (polledData) {
    console.log('TRACE: Entering host info function');
    var self = this;
    var totalProgress = 0;
    /* if (this.get('content.cluster.status') === 'INSTALLED') {
     totalProgress = 34;
     } else {
     totalProgress = 0;
     }  */
    var tasksData = polledData.tasks;
    console.log("The value of tasksData is: ", tasksData);
    if (!tasksData) {
      console.log("Step9: ERROR: NO tasks available to process");
    }
    var requestId = this.get('content.cluster.requestId');
    if(polledData.Requests && polledData.Requests.id && polledData.Requests.id!=requestId){
      // We dont want to use non-current requestId's tasks data to 
      // determine the current install status. 
      // Also, we dont want to keep polling if it is not the 
      // current requestId.
      return false;
    }
    this.replacePolledData(tasksData);
    this.hosts.forEach(function (_host) {
      var actionsPerHost = tasksData.filterProperty('Tasks.host_name', _host.name); // retrieved from polled Data
      if (actionsPerHost.length === 0) {
        _host.set('message', this.t('installer.step9.host.status.nothingToInstall'));
        console.log("INFO: No task is hosted on the host");
      }
      if (actionsPerHost !== null && actionsPerHost !== undefined && actionsPerHost.length !== 0) {
        this.setLogTasksStatePerHost(actionsPerHost, _host);
        this.onSuccessPerHost(actionsPerHost, _host);     // every action should be a success
        this.onWarningPerHost(actionsPerHost, _host);     // any action should be a failure
        this.onInProgressPerHost(actionsPerHost, _host);  // current running action for a host
        totalProgress += self.progressPerHost(actionsPerHost, _host);
      }
    }, this);
    totalProgress = Math.floor(totalProgress / this.hosts.length);
    this.set('progress', totalProgress.toString());
    console.log("INFO: right now the progress is: " + this.get('progress'));
    return this.finishState(tasksData);
  },

  startPolling: function () {
    this.set('isSubmitDisabled', true);
    this.doPolling();
  },

  numPolls: 0,

  getUrl: function (requestId) {
    var clusterName = this.get('content.cluster.name');
    var requestId = requestId || this.get('content.cluster.requestId');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/requests/' + requestId + '?fields=tasks/*';
    console.log("URL for step9 is: " + url);
    return url;
  },

  POLL_INTERVAL: 4000,

  loadLogData: function(requestId) {
    var url = this.getUrl(requestId);
    var requestsId = App.db.getCluster().oldRequestsId;
    if (App.testMode) {
      this.POLL_INTERVAL = 1;
      this.numPolls++;
    }

    requestsId.forEach(function(requestId) {
      url = this.getUrl(requestId);
      if (App.testMode) {
        this.POLL_INTERVAL = 1;

        url = this.get('mockDataPrefix') + '/poll_' + this.numPolls + '.json';
      }
      this.getLogsByRequest(url, false);
    }, this);
  },

  // polling: whether to continue polling for status or not
  getLogsByRequest: function(url, polling){
    var self = this;
    $.ajax({
      type: 'GET',
      url: url,
      async: true,
      timeout: App.timeout,
      dataType: 'text',
      success: function (data) {
        console.log("TRACE: In success function for the GET logs data");
        console.log("TRACE: STep9 -> The value is: ", jQuery.parseJSON(data));
        var result = self.parseHostInfo(jQuery.parseJSON(data));
        if (!polling) {
          return;
        }
        if (result !== true) {
          window.setTimeout(function () {
            self.doPolling();
          }, self.POLL_INTERVAL);
        } else {
          self.stopPolling();
        }
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep9 -> In error function for the GET logs data");
        console.log("TRACE: STep9 -> value of the url is: " + url);
        console.log("TRACE: STep9 -> error code status is: " + request.status);
        self.stopPolling();
      },

      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
      function () {
        App.showReloadPopup();
        console.log('Install services all retries failed');
      }
    );
  },

  doPolling: function () {
    var url = this.getUrl();

    if (App.testMode) {
      this.numPolls++;
      url = this.get('mockDataPrefix') + '/poll_' + this.get('numPolls') + '.json';

    }
    this.getLogsByRequest(url, true);
  },

  stopPolling: function () {
    //TODO: uncomment following line after the hook up with the API call
    // this.set('isStepCompleted',true);
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  },

  back: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('back');
    }
  },

  mockBtn: function () {
    this.set('isSubmitDisabled', false);
    this.hosts.clear();
    var hostInfo = this.mockHostData;
    this.renderHosts(hostInfo);
  },

  pollBtn: function () {
    this.set('isSubmitDisabled', false);
    var data1 = require('data/mock/step9PolledData/pollData_1');
    var data2 = require('data/mock/step9PolledData/pollData_2');
    var data3 = require('data/mock/step9PolledData/pollData_3');
    var data4 = require('data/mock/step9PolledData/pollData_4');
    var data5 = require('data/mock/step9PolledData/pollData_5');
    var data6 = require('data/mock/step9PolledData/pollData_6');
    var data7 = require('data/mock/step9PolledData/pollData_7');
    var data8 = require('data/mock/step9PolledData/pollData_8');
    var data9 = require('data/mock/step9PolledData/pollData_9');
    console.log("TRACE: In pollBtn function data1");
    var counter = parseInt(this.get('pollDataCounter')) + 1;
    this.set('pollDataCounter', counter.toString());
    switch (this.get('pollDataCounter')) {
      case '1':
        this.parseHostInfo(data1);
        break;
      case '2':
        this.parseHostInfo(data2);
        break;
      case '3':
        this.parseHostInfo(data3);
        break;
      case '4':
        this.parseHostInfo(data4);
        break;
      case '5':
        this.parseHostInfo(data5);
        break;
      case '6':
        this.set('content.cluster.status', 'INSTALLED');
        this.parseHostInfo(data6);
        break;
      case '7':
        this.parseHostInfo(data7);
        break;
      case '8':
        this.parseHostInfo(data8);
        break;
      case '9':
        this.parseHostInfo(data9);
        break;
      default:
        break;
    }
  }

});
