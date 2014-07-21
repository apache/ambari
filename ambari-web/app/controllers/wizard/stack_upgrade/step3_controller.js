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

App.StackUpgradeStep3Controller = Em.Controller.extend({
  name: 'stackUpgradeStep3Controller',

  POLL_INTERVAL: 4000,
  isPolling: false,
  /**
   * STOP_SERVICES internal statuses:
   * - PENDING
   * - IN_PROGRESS
   * - SUCCESS
   * - FAILED
   * UPGRADE_SERVICES internal statuses:
   * - PENDING
   * - IN_PROGRESS
   * - SUCCESS
   * - FAILED
   * - WARNING
   */
  processes:[
    Em.Object.create({
      name: 'STOP_SERVICES',
      displayName: Em.I18n.t('installer.stackUpgrade.step3.stop.header'),
      progress:0,
      status: 'PENDING',
      message: null,
      isRunning: false,
      hosts: [],
      isRetry: false
    }),
    Em.Object.create({
      name: 'UPGRADE_SERVICES',
      displayName: Em.I18n.t('installer.stackUpgrade.step3.upgrade.header'),
      progress: 0,
      status: 'PENDING',
      message:'',
      isRunning: false,
      hosts: [],
      isRetry: false
    })
  ],
  /**
   * pass processes as services to popup
   */
  services: function(){
    return this.get('processes');
  }.property('processes'),
  /**
   * save current requestId and clusterState
   * to localStorage and put it to server
   *
   * STOP_SERVICES cluster status:
   * - STOPPING_SERVICES,
   * UPGRADE_SERVICES cluster status:
   * - STACK_UPGRADING,
   * - STACK_UPGRADE_FAILED,
   * - STACK_UPGRADED,
   * - DEFAULT = STACK UPGRADE COMPLETED
   */
  saveClusterStatus: function(clusterStatus){
    var oldStatus = this.get('content.cluster');
    clusterStatus = jQuery.extend(oldStatus, clusterStatus);
    this.set('content.cluster', clusterStatus);
    App.router.get(this.get('content.controllerName')).save('cluster');
    if(!App.get('testMode')){
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: clusterStatus.status,
        wizardControllerName: 'stackUpgradeController',
        localdb: App.db.data
      });
    }
  },
  // provide binding for Host Popup data
  serviceTimestamp: null,
  /**
   * load hosts for each process
   */
  loadHosts: function () {//TODO replace App.Host.find() with content.hosts loaded directly from server
    var hosts = [];
    var installedHosts = App.Host.find();
    this.get('processes').forEach(function(process){
      var hosts = [];
      installedHosts.forEach(function (host) {
        hosts.push(Em.Object.create({
          name: host.get('hostName'),
          publicName: host.get('publicHostName'),
          logTasks: []
        }));
      });
      process.set('hosts', hosts);
    });
  }.observes('content.servicesInfo'),
  submitButton: null,
  /**
   * restart upgrade
   * restart stop services
   * @param event
   */
  retry: function(event){
    var processName = event.context;
    var process = this.get('processes').findProperty('name', processName);
    this.resetProgress(process);
    this.resetMockConfig();
    if(processName == 'STOP_SERVICES'){
      this.stopServices();
    } else {
      this.set('submitButton', false);
      this.runUpgrade();
    }
  },
  /**
   * reset progress and status to retry
   * @param process
   */
  resetProgress: function(process){
    process.set('isRetry', false);
    process.set('status', 'PENDING');
    process.set('progress', 0);
    process.get('hosts').forEach(function(host){host.get('logTasks').clear()});
  },
  /**
   * run stop services
   */
  stopServices: function () {
    var process = this.get('processes').findProperty('name', 'STOP_SERVICES');
    process.set('isRunning', true);
    if (App.get('testMode')) {
      this.startPolling();
      this.saveClusterStatus({
        requestId: 1,
        status: 'STOPPING_SERVICES',
        isCompleted: false
      });
    }
    else {
      App.ajax.send({
        name: 'common.services.update',
        sender: this,
        data: {
          "context": Em.I18n.t("requestInfo.stopAllServices"),
          "ServiceInfo": {
            "state": "INSTALLED"
          },
          urlParams: "ServiceInfo/state=STARTED"
        },
        success: 'stopServicesSuccessCallback',
        error: 'stopServicesErrorCallback'
      });
    }
  },
  stopServicesSuccessCallback: function (data) {
    var process = this.get('processes').findProperty('name', 'STOP_SERVICES');
    var requestId = data.Requests.id;
    var clusterStatus = {
      requestId: requestId,
      status: 'STOPPING_SERVICES',
      isCompleted: false
    };
    process.set('status', 'IN_PROGRESS');
    this.saveClusterStatus(clusterStatus);
    this.startPolling();
    console.log('Call to stop service successful')
  },
  stopServicesErrorCallback: function () {
    var process = this.get('processes').findProperty('name', 'STOP_SERVICES');
    this.finishProcess(process, 'FAILED');
    process.set('status', 'FAILED');
    console.log("Call to stop services failed");
  },
  /**
   * send request to run upgrade all services
   */
  runUpgrade: function () {
    var process = this.get('processes').findProperty('name', 'UPGRADE_SERVICES');
    process.set('isRunning', true);
    if (App.get('testMode')) {
      this.startPolling();
      this.saveClusterStatus({
        requestId: 1,
        status: 'STACK_UPGRADING',
        isCompleted: false
      });
    }
    else {
      var data = '{"Clusters": {"version" : "' + this.get('content.upgradeVersion') + '"}}';
      App.ajax.send({
        name: 'admin.stack_upgrade.run_upgrade',
        sender: this,
        data: {
          data: data
        },
        success: 'runUpgradeSuccessCallback',
        error: 'runUpgradeErrorCallback'
      });
    }
  },
  runUpgradeSuccessCallback: function (jsonData) {
    var process = this.get('processes').findProperty('name', 'UPGRADE_SERVICES');
    var requestId = jsonData.Requests.id;
    var clusterStatus = {
      status: 'STACK_UPGRADING',
      requestId: requestId,
      isCompleted: false
    };
    process.set('status', 'IN_PROGRESS');
    this.saveClusterStatus(clusterStatus);
    this.startPolling();
  },

  runUpgradeErrorCallback: function (request, ajaxOptions, error) {
    var process = this.get('processes').findProperty('name', 'UPGRADE_SERVICES');
    this.finishProcess(process, 'FAILED');
    process.set('status', 'FAILED');
  },

  /**
   * start polling tasks for current process
   */
  startPolling: function(){
    if(!this.get('isPolling')){
      this.set('isPolling', true);
      if (App.get('testMode')) {
        this.simulatePolling();
      } else {
        //pass an interval "1" to start poll immediately first time
        this.doPoll(1);
      }
    }
  },
  simulateAttempt:0,
  mockUrl:'',
  /**
   * simulate actual poll, using mock data
   */
  simulatePolling: function(){
    var simulateAttempt = this.get('simulateAttempt');
    var process = this.get('processes').findProperty('isRunning', true);
    var upgradeURLs = [
      '/upgrade/poll_1.json',
      '/upgrade/poll_2.json',
      '/upgrade/poll_3.json',
      '/upgrade/poll_4.json',
      '/upgrade/poll_5.json'
    ];
    var stopURLs = [
      '/stop_services/poll_1.json',
      '/stop_services/poll_2.json',
      '/stop_services/poll_3.json',
      '/stop_services/poll_4.json'
    ];
    if(process.get('name') == 'STOP_SERVICES'){
      if(simulateAttempt < 4){
        this.set('mockUrl', stopURLs[simulateAttempt]);
        this.doPoll();
        this.set('simulateAttempt', ++simulateAttempt);
      }
    } else {
      if(simulateAttempt < 5){
        this.set('mockUrl', upgradeURLs[simulateAttempt]);
        this.doPoll();
        this.set('simulateAttempt', ++simulateAttempt);
      }
    }
  },
  getUrl:function(){
    var requestId = this.get('content.cluster.requestId');
    var clusterName = this.get('content.cluster.name');
    if(App.get('testMode')){
      return this.get('mockUrl');
    }
    return App.apiPrefix + '/clusters/' + clusterName + '/requests/' + requestId + '?fields=tasks/*';
  },
  /**
   * poll server for tasks, which contain process progress data
   * @param interval
   */
  doPoll: function(interval) {
    var self = this;
    var pollInterval = interval || self.POLL_INTERVAL;
    if (self.get('isPolling')) {
      setTimeout(function () {

        App.ajax.send({
          name: 'admin.stack_upgrade.do_poll',
          sender: self,
          data: {
            cluster: self.get('content.cluster.name'),
            requestId: self.get('content.cluster.requestId'),
            mock: self.get('mockUrl')
          },
          success: 'doPollSuccessCallback',
          error: 'doPollErrorCallback'
        }).retry({
            times: App.maxRetries,
            timeout: App.timeout
          }).then(
            null,
            function () {
              App.showReloadPopup();
              console.log('Install services all retries failed');
            });
      }, pollInterval);
    }
  },

  doPollSuccessCallback: function (data) {
    var result = this.parseTasks(data);
    if(result){
      if (App.get('testMode')) {
        this.simulatePolling();
      }
      else {
        this.doPoll();
      }
    }
  },

  doPollErrorCallback: function () {
    console.log('ERROR: poll request failed')
  },

  /**
   * parse tasks from poll
   * change status, message, progress on services according to tasks
   * @param data
   * @return {Boolean}
   */
  parseTasks: function(data){
    var tasks = data.tasks || [];
    var process = this.get('processes').findProperty('isRunning', true);
    // if process was finished then it terminates next poll

    this.progressOnProcess(tasks, process);
    var continuePolling = this.statusOnProcess(tasks, process);
    if(process.get('hosts').length && tasks.length){
      process.get('hosts').forEach(function (host) {
        var tasksPerHost = tasks.filterProperty('Tasks.host_name', host.name);
        if (tasksPerHost.length) {
          this.setLogTasksStatePerHost(tasksPerHost, host);
        }
      }, this);
    }
    this.set('serviceTimestamp', App.dateTime());
    return continuePolling;
  },
  /**
   * calculate progress according to tasks status
   * @param actions
   * @param process
   */
  progressOnProcess: function(actions, process){
    var actionsNumber = actions.length;
    var completedActions = actions.filterProperty('Tasks.status', 'COMPLETED').length
      + actions.filterProperty('Tasks.status', 'FAILED').length
      + actions.filterProperty('Tasks.status', 'ABORTED').length
      + actions.filterProperty('Tasks.status', 'TIMEDOUT').length;
    var queuedActions = actions.filterProperty('Tasks.status', 'QUEUED').length;
    var inProgressActions = actions.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    var progress = Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsNumber * 100);
    console.log('INFO: progress is: ' + progress);
    process.set('progress', progress);
  },
  /**
   * evaluate status of process according to task status
   * @param actions
   * @param process
   */
  statusOnProcess: function(actions, process){
    var status = null;
    var message = '';
    var continuePolling = true;
    var errorActions = actions.filter(function (action) {
      if (action.Tasks.status == 'FAILED' || action.Tasks.status == 'ABORTED' || action.Tasks.status == 'TIMEDOUT') {
        return true;
      }
    });
    var masterComponents = ['NAMENODE', 'SECONDARY_NAMENODE', 'SNAMENODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER',
      'HIVE_METASTORE', 'MYSQL_SERVER', 'HBASE_MASTER', 'NAGIOS_SERVER', 'GANGLIA_SERVER', 'OOZIE_SERVER', 'WEBHCAT_SERVER'];
    var failedComponents = errorActions.mapProperty('Tasks.role');
    if (failedComponents.length) {
      for (var i = 0; i < failedComponents.length; i++) {
        if (masterComponents.contains(failedComponents[i])) {
          status = "FAILED";
          continuePolling = false;
          this.finishProcess(process, status);
          break;
        } else if(process.get('progress') == 100){
          status = "WARNING";
          if(process.get('name') == 'UPGRADE_SERVICES'){
            continuePolling = false;
            this.finishProcess(process, status);
          }
        }
      }
    }
    if(!status || ((status == 'WARNING') && (process.get('name') == 'STOP_SERVICES'))){
      if (actions.everyProperty('Tasks.status', 'COMPLETED')) {
        status = 'SUCCESS';
        continuePolling = false;
        this.finishProcess(process, status);
      } else {
        var activeAction = actions.findProperty('Tasks.status', 'IN_PROGRESS');
        status = 'IN_PROGRESS';
        if (activeAction === undefined || activeAction === null) {
          activeAction = actions.findProperty('Tasks.status', 'QUEUED');
          status = 'PENDING';
        }
        if (activeAction === undefined || activeAction === null) {
          activeAction = actions.findProperty('Tasks.status', 'PENDING');
          status = 'PENDING';
        }
        if (activeAction) {
          message = this.displayMessage(activeAction.Tasks);
        }
      }
    }
    console.log('INFO: status is: ' + status);
    process.set('status', status);
    process.set('message', message);
    return continuePolling;
  },
  /**
   * complete process phase
   * accept FAILED, SUCCESS, WARNING process status
   * @param process
   * @param status
   */
  finishProcess: function(process, status){
    this.set('isPolling', false);
    if(process.get('name') == 'STOP_SERVICES'){
      if(status == 'SUCCESS'){
        process.set('isRunning', false);
        this.resetMockConfig();
        this.runUpgrade();
      } else {
        process.set('isRetry', true);
      }
    }
    if(process.get('name') == 'UPGRADE_SERVICES'){
      if(status == 'SUCCESS'){
        this.set('submitButton', Em.I18n.t('common.done'));
        this.saveClusterStatus({
          status: 'STACK_UPGRADED',
          isCompleted: true
        })
      } else if(status == 'FAILED') {
        process.set('isRetry', true);
        this.set('submitButton', false);
        this.saveClusterStatus({
          status: 'STACK_UPGRADE_FAILED',
          isCompleted: false
        })
      } else if(status == 'WARNING'){
        this.set('submitButton', Em.I18n.t('installer.stackUpgrade.step3.ProceedWithWarning'));
        process.set('isRetry', true);
        this.saveClusterStatus({
          status: 'STACK_UPGRADED',
          isCompleted: true
        })
      }
    }
  },
  /**
   * set and update logTasks to each host
   * @param tasksPerHost
   * @param host
   */
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    tasksPerHost.forEach(function (_task) {
      var task = host.get('logTasks').findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        host.get('logTasks').removeObject(task);
      }
      host.get('logTasks').pushObject(_task);
      //}
    }, this);
  },
  /**
   * reset mock configs to run upgrade simulation again
   */
  resetMockConfig: function(retry){
    this.set('simulateAttempt', 0);
  },
  /**
   * resume wizard on last operation
   */
  resumeStep: function () {
    var clusterStatus = this.get('content.cluster.status');
    var upgrade = this.get('processes').findProperty('name', 'UPGRADE_SERVICES');
    var stop = this.get('processes').findProperty('name', 'STOP_SERVICES');
    if(App.get('testMode')){
      if(this.get('processes').everyProperty('isRunning', false)){
        stop.set('isRunning', true);
      }
      clusterStatus = (this.get('processes').findProperty('name', 'UPGRADE_SERVICES').get('isRunning'))?
       'STACK_UPGRADING':
       'STOPPING_SERVICES';
      upgrade.set('isRetry', false);
      stop.set('isRetry', false);
    }
    if (clusterStatus == 'STOPPING_SERVICES') {
      this.startPolling();
      stop.set('isRunning', true);
      upgrade.set('isRunning', false);
    } else if(clusterStatus != 'PENDING'){
      stop.set('status', 'SUCCESS');
      stop.set('progress', 100);
      stop.set('isRunning', false);
      upgrade.set('isRunning', true);
      if (clusterStatus == 'STACK_UPGRADING') {
        upgrade.set('status', 'IN_PROGRESS');
        this.startPolling();
      } else if (clusterStatus == 'STACK_UPGRADE_FAILED') {
        upgrade.set('status', 'FAILED');
        upgrade.set('isRetry', true);
      } else if (clusterStatus == 'STACK_UPGRADED') {
        upgrade.set('status', 'SUCCESS');
        upgrade.set('progress', 100);
        this.startPolling();
        this.set('isPolling', false);
      }
    }
  },
  /**
   * determine description of current running process
   * @param task
   * @return {*}
   */
  displayMessage: function (task) {
    var role = App.format.role(task.role);
    switch (task.command){
      case 'UPGRADE':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.upgrade.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.upgrade.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.upgrade.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.upgrade.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.upgrade.failed') + role;
        }
        break;
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
    }
  }
});
