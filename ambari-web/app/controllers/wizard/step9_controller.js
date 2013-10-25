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
var serviceComponents = require('data/service_components');

App.WizardStep9Controller = Em.Controller.extend({
  name: 'wizardStep9Controller',
  hosts: [],
  progress: '0',

  isStepCompleted: false,

  isSubmitDisabled: function () {
    var validStates = ['STARTED','START FAILED'];
    var controllerName = this.get('content.controllerName');
    if (controllerName == 'addHostController' || controllerName == 'addServiceController') {
      validStates.push('INSTALL FAILED');
    }
    return !validStates.contains(this.get('content.cluster.status'));
  }.property('content.cluster.status'),

  // links to previous steps are enabled iff install failed in installer
  togglePreviousSteps: function () {
    if ('INSTALL FAILED' === this.get('content.cluster.status') && this.get('content.controllerName') == 'installerController') {
      App.router.get('installerController').setStepsEnable();
    } else {
      App.router.get('installerController').setLowerStepsDisable(9);
    }
  }.observes('content.cluster.status', 'content.controllerName'),

  mockDataPrefix: '/data/wizard/deploy/5_hosts',
  pollDataCounter: 0,
  polledData: [],
  numPolls: 1,
  POLL_INTERVAL: 4000,

  status: function () {
    if (this.get('hosts').someProperty('status', 'failed')) {
      return 'failed';
    }
    if (this.get('hosts').someProperty('status', 'warning')) {
      if (this.isStepFailed()) {
        return 'failed';
      } else {
        return 'warning';
      }
    }
    if(this.get('progress') == '100') {
      this.set('isStepCompleted', true);
      return 'success';
    }
    return 'info';
  }.property('hosts.@each.status', 'progress'),

  showRetry: function () {
    return this.get('content.cluster.status') == 'INSTALL FAILED';
  }.property('content.cluster.status'),

  categoryObject: Em.Object.extend({
    hostsCount: function () {
      var category = this;
      var hosts = this.get('controller.hosts').filter(function(_host) {
        if(category.get('hostStatus') == 'inProgress'){   // queued, pending, in_progress map to inProgress
          return (_host.get('status') !== 'success' && _host.get('status') !== 'failed' && _host.get('status') !== 'warning');
        }
        return (_host.get('status') == category.get('hostStatus'));
      }, this);
      return hosts.get('length');
    }.property('controller.hosts.@each.status'),
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount')
  }),
  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      controller: self,
      isActive: function(){
        return this.get('controller.category') == this;
      }.property('controller.category'),
      itemClass: function(){
        return this.get('isActive') ? 'active' : '';
      }.property('isActive')
    });

    var categories = [
      self.categoryObject.create({value: Em.I18n.t('common.all'), hostStatus:'all', hostsCount: function () {
        return this.get('controller.hosts.length');
      }.property('controller.hosts.length') }),
      self.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.inProgress'), hostStatus: 'inProgress'}),
      self.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.warning'), hostStatus: 'warning'}),
      self.categoryObject.create({value: Em.I18n.t('common.success'), hostStatus: 'success'}),
      self.categoryObject.create({value: Em.I18n.t('common.fail'), hostStatus: 'failed', last: true })
    ];

    this.set('category', categories.get('firstObject'));
    return categories;
  }.property(),
  category: false,
  visibleHosts: function(){
    var targetStatus = this.get('category.hostStatus');
    var visibleHosts =  this.get('hosts').filter(function(_host) {
      if (targetStatus == 'all') {
        return true;
      }
      if (targetStatus == 'inProgress') {   // queued, pending, in_progress map to inProgress
        return (_host.get('status') !== 'success' && _host.get('status') !== 'failed' && _host.get('status') !== 'warning');
      }
      return (_host.get('status') == targetStatus);
    }, this);
    return visibleHosts;
  }.property('category', 'hosts.@each.status'),

  logTasksChangesCounter: 0,

  selectCategory: function(event){
    this.set('category', event.context);
  },

  getCategory: function(field, value){
    return this.get('categories').find(function(item){
      return item.get(field) == value;
    });
  },

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
      this.set('content.cluster.requestId',1);
    }
    var clusterStatus = this.get('content.cluster.status');
    console.log('navigateStep: clusterStatus = ' + clusterStatus);
    if (this.get('content.cluster.isCompleted') === false) {
      // the cluster has not yet successfully installed and started
      if (clusterStatus === 'INSTALL FAILED') {
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
        this.set('isStepCompleted', true);
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
    this.get('hosts').clear();
    this.set('status', 'info');
    this.set('progress', '0');
    this.set('isStepCompleted', false);
    this.set('numPolls', 1);
  },

  loadStep: function () {
    console.log("TRACE: Loading step9: Install, Start and Test");
    this.clearStep();
    this.renderHosts(this.loadHosts());
  },
  /**
   * reset status and message of all hosts when retry install
   */
  resetHostsForRetry: function(){
    var hosts = this.get('content.hosts');
    for (var name in hosts) {
      hosts[name].status = "pending";
      hosts[name].message = 'Waiting';
    }
    this.set('content.hosts', hosts);
  },

  loadHosts: function () {
    var hostInfo = this.get('content.hosts');
    var hosts = new Ember.Set();
    for (var index in hostInfo) {
      var obj = Em.Object.create(hostInfo[index]);
      obj.message = (obj.message) ? obj.message : 'Waiting';
      obj.progress = 0;
      obj.status = (obj.status) ? obj.status : 'info';
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
      this.get('hosts').pushObject(hostInfo);
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
            return Em.I18n.t('installer.step9.serviceStatus.install.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.install.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.install.failed') + role;
        }
      case 'UNINSTALL':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.failed') + role;
        }
      case 'START' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.start.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.start.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.start.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.start.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.start.failed');
        }
      case 'STOP' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.stop.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.stop.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.stop.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.failed');
        }
      case 'EXECUTE' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.execute.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.execute.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.execute.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.failed');
        }
      case 'ABORT' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.abort.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.abort.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.abort.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.failed');
        }
    }
    return '';
  },

  /**
   * run start/check services after installation phase
   */
  launchStartServices: function () {
    var data = {
      "RequestInfo": {
        "context": Em.I18n.t("requestInfo.startServices")
      },
      "Body": {
        "ServiceInfo": { "state": "STARTED" }
      }
    };
    var name = 'wizard.step9.installer.launch_start_services';

    if (this.get('content.controllerName') === 'addHostController') {
      var hostnames = [];
      for (var hostname in this.get('wizardController').getDBProperty('hosts')) {
        hostnames.push(hostname);
      }
      data = {
        "RequestInfo": {
          "context": Em.I18n.t("requestInfo.startHostComponents"),
          "query": "HostRoles/component_name.in(GANGLIA_MONITOR,HBASE_REGIONSERVER,DATANODE,TASKTRACKER,NODEMANAGER)&HostRoles/state=INSTALLED&HostRoles/host_name.in(" + hostnames.join(',') + ")"
        },
        "Body": {
          "HostRoles": { "state": "STARTED" }
        }
      };
      name = 'wizard.step9.add_host.launch_start_services';
    }
    data = JSON.stringify(data);
    if (App.testMode) {
      this.set('numPolls', 6);
    }

    App.ajax.send({
      name: name,
      sender: this,
      data: {
        data: data,
        cluster: this.get('content.cluster.name')
      },
      success: 'launchStartServicesSuccessCallback',
      error: 'launchStartServicesErrorCallback'
    });
  },

  launchStartServicesSuccessCallback: function (jsonData) {
    var clusterStatus = {};
    if (jsonData) {
      console.log("TRACE: Step9 -> In success function for the startService call");
      console.log("TRACE: Step9 -> value of the received data is: " + jsonData);
      var requestId = jsonData.Requests.id;
      console.log('requestId is: ' + requestId);
      clusterStatus = {
        status: 'INSTALLED',
        requestId: requestId,
        isStartError: false,
        isCompleted: false
      };
      this.hostHasClientsOnly(false);
      App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
    } else {
      console.log('ERROR: Error occurred in parsing JSON data');
      this.hostHasClientsOnly(true);
      clusterStatus = {
        status: 'STARTED',
        isStartError: false,
        isCompleted: true
      };
      App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
      this.set('status', 'success');
      this.set('progress', '100');
      this.set('isStepCompleted', true);
    }
    // We need to do recovery if there is a browser crash
    App.clusterStatus.setClusterStatus({
      clusterState: 'SERVICE_STARTING_3',
      localdb: App.db.data
    });

    if(jsonData) {
      this.startPolling();
    }
  },

  hostHasClientsOnly: function(jsonError) {
    this.get('hosts').forEach(function(host){
      var OnlyClients = true;
      var tasks = host.get('logTasks');
      tasks.forEach(function(task){
        var component = serviceComponents.findProperty('component_name',task.Tasks.role);
        if(!(component && component.isClient)) {
          OnlyClients = false;
        }
      });
      if (OnlyClients || jsonError) {
        host.set('status', 'success');
        host.set('progress', '100');
      }
    });
  },

  launchStartServicesErrorCallback: function () {
    console.log("ERROR");
    var clusterStatus = {
      status: 'START FAILED',
      isStartError: true,
      isCompleted: false
    };
    App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
  },

  // marks a host's status as "success" if all tasks are in COMPLETED state
  onSuccessPerHost: function (actions, contentHost) {
    if (!actions) return;
    if (actions.everyProperty('Tasks.status', 'COMPLETED') && this.get('content.cluster.status') === 'INSTALLED') {
      contentHost.set('status', 'success');
    }
  },

  // marks a host's status as "warning" if at least one of the tasks is FAILED, ABORTED, or TIMEDOUT and marks host's status as "failed" if at least one master component install task is FAILED.
  // note that if the master failed to install because of ABORTED or TIMEDOUT, we don't mark it as failed, because this would mark all hosts as "failed" and makes it difficult for the user
  // to find which host FAILED occurred on, if any
  onErrorPerHost: function (actions, contentHost) {
    if (!actions) return;
    if (actions.someProperty('Tasks.status', 'FAILED') || actions.someProperty('Tasks.status', 'ABORTED') || actions.someProperty('Tasks.status', 'TIMEDOUT')) {
      contentHost.set('status', 'warning');
    }
    if ((this.get('content.cluster.status') === 'PENDING' && actions.someProperty('Tasks.status', 'FAILED')) || (this.isMasterFailed(actions))) {
      contentHost.set('status', 'failed');
    }
  },
  //return true if there is at least one FAILED task of master component install
  isMasterFailed: function(polledData) {
    var result = false;
    polledData.filterProperty('Tasks.command', 'INSTALL').filterProperty('Tasks.status', 'FAILED').mapProperty('Tasks.role').forEach (
      function (task) {
        if (!['DATANODE', 'TASKTRACKER', 'HBASE_REGIONSERVER', 'GANGLIA_MONITOR'].contains(task)) {
          result = true;
        }
      }
    );
    return result;
  },

  onInProgressPerHost: function (actions, contentHost) {
    if (!actions) return;
    var runningAction = actions.findProperty('Tasks.status', 'IN_PROGRESS');
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'QUEUED');
    }
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'PENDING');
    }
    if (runningAction !== null && runningAction !== undefined) {
      contentHost.set('status', 'in_progress');
      contentHost.set('message', this.displayMessage(runningAction.Tasks));
    }
  },

  /**
   * calculate progress of tasks per host
   * @param actions
   * @param contentHost
   * @return {Number}
   */
  progressPerHost: function (actions, contentHost) {
    var progress = 0;
    var actionsPerHost = actions.length;
    // TODO: consolidate to a single filter function for better performance
    var completedActions = actions.filterProperty('Tasks.status', 'COMPLETED').length
      + actions.filterProperty('Tasks.status', 'FAILED').length
      + actions.filterProperty('Tasks.status', 'ABORTED').length
      + actions.filterProperty('Tasks.status', 'TIMEDOUT').length;
    var queuedActions = actions.filterProperty('Tasks.status', 'QUEUED').length;
    var inProgressActions = actions.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    /** for the install phase (PENDING), % completed per host goes up to 33%; floor(100 / 3)
     * for the start phase (INSTALLED), % completed starts from 34%
     * when task in queued state means it's completed on 9%
     * in progress - 35%
     * completed - 100%
     */
    switch (this.get('content.cluster.status')) {
      case 'PENDING':
        progress = actionsPerHost?(Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 33)):33;
        break;
      case 'INSTALLED':
        progress = actionsPerHost?(34 + Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 66)):100;
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

  /**
   * return true if:
   *  1. any of the master/client components failed to install
   *  OR
   *  2. at least 50% of the slave host components for the particular service component fails to install
   */
  isStepFailed: function () {
    var failed = false;
    var polledData = this.get('polledData');
    polledData.filterProperty('Tasks.command', 'INSTALL').mapProperty('Tasks.role').uniq().forEach(function (role) {
      if (failed) {
        return;
      }
      var actionsPerRole = polledData.filterProperty('Tasks.role', role);
      if (['DATANODE', 'TASKTRACKER', 'HBASE_REGIONSERVER', 'GANGLIA_MONITOR'].contains(role)) {
        // check slave components for success factor.
        // partial failure for slave components are allowed.
        var actionsFailed = actionsPerRole.filterProperty('Tasks.status', 'FAILED');
        var actionsAborted = actionsPerRole.filterProperty('Tasks.status', 'ABORTED');
        var actionsTimedOut = actionsPerRole.filterProperty('Tasks.status', 'TIMEDOUT');
        if ((((actionsFailed.length + actionsAborted.length + actionsTimedOut.length) / actionsPerRole.length) * 100) > 50) {
          failed = true;
        }
      } else if (actionsPerRole.someProperty('Tasks.status', 'FAILED') || actionsPerRole.someProperty('Tasks.status', 'ABORTED') ||
        actionsPerRole.someProperty('Tasks.status', 'TIMEDOUT')) {
        // check non-salve components (i.e., masters and clients).  all of these must be successfully installed.
        failed = true;
      }
    }, this);
    return failed;
  },

  // makes a state transition
  // PENDING -> INSTALLED
  // PENDING -> INSTALL FAILED
  // INSTALLED -> STARTED
  // INSTALLED -> START_FAILED
  // returns true if polling should stop; false otherwise
  // polling from ui stops only when no action has 'PENDING', 'QUEUED' or 'IN_PROGRESS' status
  finishState: function (polledData) {
    if (this.get('content.cluster.status') === 'INSTALLED') {
      return this.finishStateInstalled(polledData);
    }
    else
      if (this.get('content.cluster.status') === 'PENDING') {
        return this.finishStatePending(polledData);
      }
      else
        if (this.get('content.cluster.status') === 'INSTALL FAILED' ||
            this.get('content.cluster.status') === 'START FAILED' ||
            this.get('content.cluster.status') === 'STARTED') {
          this.set('progress', '100');
          return true;
        }
    return false;
  },

  finishStateInstalled: function(polledData) {
    var clusterStatus = {};
    if (!polledData.someProperty('Tasks.status', 'PENDING') &&
        !polledData.someProperty('Tasks.status', 'QUEUED') &&
        !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      this.set('progress', '100');
      clusterStatus = {
        status: 'INSTALLED',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: true
      };
      if (this.isSuccess(polledData)) {
        clusterStatus.status = 'STARTED';
        var serviceStartTime = new Date().getTime();
        var timeToStart = ((parseInt(serviceStartTime) - parseInt(this.get('content.cluster.installStartTime'))) / 60000).toFixed(2);
        clusterStatus.installTime = timeToStart;
      } else {
        clusterStatus.status = 'START FAILED'; // 'START FAILED' implies to step10 that installation was successful but start failed
      }
      App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
      this.set('isStepCompleted', true);
      this.setTasksPerHost();
      App.router.get(this.get('content.controllerName')).saveInstalledHosts(this);
      return true;
    }
    return false;
  },

  finishStatePending: function(polledData) {
    var clusterStatus = {};
    if (!polledData.someProperty('Tasks.status', 'PENDING') &&
        !polledData.someProperty('Tasks.status', 'QUEUED') &&
        !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      clusterStatus = {
        status: 'PENDING',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: false
      };
      if (this.get('status') === 'failed') {
        clusterStatus.status = 'INSTALL FAILED';
        this.set('progress', '100');
        this.get('hosts').forEach(function(host){
          host.get('status') != 'failed' ? host.set('message',Em.I18n.t('installer.step9.host.status.startAborted')) : null;
          host.set('progress','100');
        });
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
    return false;
  },

  setTasksPerHost: function () {
    var tasksData = this.get('polledData');
    this.get('hosts').forEach(function (_host) {
      var tasksPerHost = tasksData.filterProperty('Tasks.host_name', _host.name); // retrieved from polled Data
      if (tasksPerHost !== null && tasksPerHost !== undefined && tasksPerHost.length !== 0) {
        tasksPerHost.forEach(function (_taskPerHost) {
          console.log('In step9 _taskPerHost function.');
          _host.tasks.pushObject(_taskPerHost);
        }, this);
      }
    }, this);
  },

  // This is done at HostRole level.
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    if (!tasksPerHost) return;
    console.log('In step9 setTasksStatePerHost function.');
    tasksPerHost.forEach(function (_task) {
      console.log('In step9 _taskPerHost function.');
      var task = host.logTasks.findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        host.logTasks.removeObject(task);
      }
      host.logTasks.pushObject(_task);
    }, this);
    this.set('logTasksChangesCounter', this.get('logTasksChangesCounter') + 1);
  },

  parseHostInfo: function (polledData) {
    console.log('TRACE: Entering host info function');
    var self = this;
    var totalProgress = 0;
    var tasksData = polledData.tasks;
    console.log("The value of tasksData is: ", tasksData);
    if (!tasksData) {
      console.log("Step9: ERROR: NO tasks available to process");
    }
    var requestId = this.get('content.cluster.requestId');
    if(polledData.Requests && polledData.Requests.id && polledData.Requests.id!=requestId){
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      return false;
    }
    this.replacePolledData(tasksData);
    this.get('hosts').forEach(function (_host) {
      var actionsPerHost = tasksData.filterProperty('Tasks.host_name', _host.name); // retrieved from polled Data
      if (actionsPerHost.length === 0) {
        if(this.get('content.cluster.status') === 'PENDING') {
          _host.set('progress', '33');
          _host.set('status', 'pending');
        }
        if(this.get('content.cluster.status') === 'INSTALLED' || this.get('content.cluster.status') === 'FAILED') {
          _host.set('progress', '100');
          _host.set('status', 'success');
        }
        console.log("INFO: No task is hosted on the host");
      }
      this.setLogTasksStatePerHost(actionsPerHost, _host);
      this.onSuccessPerHost(actionsPerHost, _host);     // every action should be a success
      this.onErrorPerHost(actionsPerHost, _host);     // any action should be a failure
      this.onInProgressPerHost(actionsPerHost, _host);  // current running action for a host
      totalProgress += self.progressPerHost(actionsPerHost, _host);
      if (_host.get('progress') == '33' && _host.get('status') != 'failed' && _host.get('status') != 'warning') {
        _host.set('message', this.t('installer.step9.host.status.nothingToInstall'));
        _host.set('status', 'pending');
      }
    }, this);
    totalProgress = Math.floor(totalProgress / this.get('hosts.length'));
    this.set('progress', totalProgress.toString());
    console.log("INFO: right now the progress is: " + this.get('progress'));
    return this.finishState(tasksData);
  },

  startPolling: function () {
    this.set('isSubmitDisabled', true);
    this.doPolling();
  },

  getUrl: function (requestId) {
    var clusterName = this.get('content.cluster.name');
    var requestId = requestId || this.get('content.cluster.requestId');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/requests/' + requestId + '?fields=tasks/*';
    console.log("URL for step9 is: " + url);
    return url;
  },

  loadLogData: function(requestId) {
    var url = this.getUrl(requestId);
    var requestsId = this.get('wizardController').getDBProperty('cluster').oldRequestsId;
    if (App.testMode) {
      this.POLL_INTERVAL = 1;
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
        var parsedData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the GET logs data");
        console.log("TRACE: Step9 -> The value is: ", parsedData);
        var result = self.parseHostInfo(parsedData);
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
  }
});