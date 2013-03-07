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
  isUpgradeStarted: false,
  servicesOrder: [
    'HDFS',
    'MAPREDUCE',
    'ZOOKEEPER',
    'HBASE',
    'HIVE',
    'OOZIE',
    'NAGIOS',
    'GANGLIA',
    'PIG',
    'SQOOP'
  ],

  /**
   * overall status of Upgrade
   * FAILED - some service is FAILED
   * SUCCESS - every services are SUCCESS
   * WARNING - some service is WARNING and all the rest are SUCCESS
   * IN_PROGRESS - when all services is stopped
   */
  status: 'PENDING',
  onStatus: function () {
    var services = this.get('services');
    var status = this.get('isServicesStopped') ? 'IN_PROGRESS' : 'PENDING';
    var withoutWarning = [];
    if (services.someProperty('status', 'FAILED')) {
      this.set('isPolling', false);
      status =  'FAILED';
      this.setClusterStatus('STACK_UPGRADE_FAILED');
    }
    if (services.someProperty('status', 'WARNING')) {
      withoutWarning = services.filter(function(service){
        if(service.get('status') !== "WARNING"){
          return true;
        }
      });
      if(withoutWarning.everyProperty('status', 'SUCCESS')){
        this.set('isPolling', false);
        status = "WARNING";
        this.setClusterStatus('STACK_UPGRADED');
      }
    }
    if (services.everyProperty('status', 'SUCCESS')) {
      this.set('isPolling', false);
      status = 'SUCCESS';
      this.set('content.cluster.isCompleted', true);
      App.router.get(this.get('content.controllerName')).save('cluster');
      this.setClusterStatus('STACK_UPGRADED');
    }
    this.set('status', status);
  }.observes('services.@each.status'),
  setClusterStatus: function(state){
    if(!App.testMode){
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: state,
        wizardControllerName: 'stackUpgradeController',
        localdb: App.db.data
      });
    }
  },
  // provide binding for Host Popup data
  serviceTimestamp: null,
  /**
   * The dependence of the status of service to status of the tasks
   * FAILED - any task is TIMEDOUT, ABORTED, FAILED (depends on component is master)
   * WARNING - any task is TIMEDOUT, ABORTED, FAILED (depends on component is slave or client)
   * SUCCESS - every tasks are COMPLETED
   * IN_PROGRESS - any task is UPGRADING(IN_PROGRESS)
   * PENDING - every tasks are QUEUED or PENDING
   */
  services: [],
  /**
   * load services, which installed on cluster
   */
  loadServices: function(){
    var installedServices = App.testMode ? this.get('mockServices') : this.get('content.servicesInfo');
    var services = [];
    var order = this.get('servicesOrder');
    installedServices.sort(function(a, b){
      return order.indexOf(a.get('serviceName')) - order.indexOf(b.get('serviceName'));
    });
    installedServices.forEach(function(_service){
      services.push(Em.Object.create({
        name: _service.get('serviceName'),
        displayName: _service.get('displayName'),
        hosts: this.loadHosts(_service),
        progress: 0,
        message: function(){
          switch(this.get('status')){
            case "FAILED":
              return Em.I18n.t('installer.stackUpgrade.step3.service.failedUpgrade').format(this.get('name'));
              break;
            case "WARNING":
              return Em.I18n.t('installer.stackUpgrade.step3.service.upgraded').format(this.get('name'));
              break;
            case "SUCCESS":
              return Em.I18n.t('installer.stackUpgrade.step3.service.upgraded').format(this.get('name'));
              break;
            case "IN_PROGRESS":
              return Em.I18n.t('installer.stackUpgrade.step3.service.upgrading').format(this.get('name'));
              break;
            case "PENDING":
            default:
              return Em.I18n.t('installer.stackUpgrade.step3.service.pending').format(this.get('name'));
              break;
          }
        }.property('status'),
        status: "PENDING",
        detailMessage:''
      }));
    }, this);
    this.set('services', services);
  }.observes('content.servicesInfo'),
  /**
   * load hosts as services property
   * @param service
   * @return {Array}
   */
  loadHosts: function(service){
    var hostComponents = App.HostComponent.find().filterProperty('service.serviceName', service.get('serviceName'));
    var hosts = hostComponents.mapProperty('host').uniq();
    var result = [];
    hosts.forEach(function(host){
      result.push(Em.Object.create({
        name: host.get('hostName'),
        publicName: host.get('publicHostName'),
        logTasks: [],
        components: hostComponents.filterProperty('host.hostName', host.get('hostName')).mapProperty('componentName')
      }));
    });
    return result;
  },
  /**
   * upgrade status SUCCESS - submit button enabled with label "Done"
   * upgrade status WARNING - submit button enabled with label "Proceed with Warning"
   * upgrade status FAILED or IN_PROGRESS - submit button disabled
   */
  submitButton: function(){
    if(this.get('status') == 'SUCCESS'){
      return Em.I18n.t('common.done');
    } else if(this.get('status') == 'WARNING'){
      return Em.I18n.t('installer.stackUpgrade.step3.ProceedWithWarning');
    } else {
      return false;
    }
  }.property('status'),
  showRetry: function () {
    return (this.get('status') === 'FAILED' || this.get('status') === 'WARNING');
  }.property('status'),
  isServicesStopped: function(){
    return this.get('servicesStopProgress') === 100;
  }.property('servicesStopProgress'),
  isServicesStopFailed: function(){
    return this.get('servicesStopProgress') === false;
  }.property('servicesStopProgress'),
  installedServices: App.Service.find(),
  /**
   * progress of stopping services process
   * check whether service stop fails
   */
  servicesStopProgress: function(){
    var services = App.testMode ? this.get('mockServices') : this.get('installedServices').toArray();
    var progress = 0;
    var stopFailed = false;
    services.forEach(function(service){
      if(!stopFailed){
        stopFailed = service.get('hostComponents').filterProperty('isMaster').someProperty('workStatus', 'STOP_FAILED');
      }
    });
    if(stopFailed){
      return false;
    } else {
      progress = (services.filterProperty('workStatus', 'STOPPING').length / services.length) * 0.2;
      return Math.round((progress + services.filterProperty('workStatus', 'INSTALLED').length / services.length) * 100);
    }
  }.property('installedServices.@each.workStatus', 'mockServices.@each.workStatus'),
  retryStopService: function(){
    App.router.get(this.get('content.controllerName')).stopServices();
  },
  /**
   * restart upgrade if fail or warning occurred
   * @param event
   */
  retry: function(event){
    this.set('isUpgradeStarted', false);
    this.resetMockConfig(true);
    this.loadServices();
    this.runUpgrade();
  },
  /**
   * send request to run upgrade all services
   */
  runUpgrade: function(){
    // call to run upgrade on server
    var method = App.testMode ? "GET" : "PUT";
    var url = '';
    var data = '';
    var self = this;
    if(this.get('isServicesStopped') && !this.get('isUpgradeStarted')){
      //TODO remove assignment isUpgradeStarted true to Ajax success callback
      this.set('isUpgradeStarted', true);
      /* $.ajax({
       type: method,
       url: url,
       data: data,
       async: false,
       dataType: 'text',
       timeout: App.timeout,
       success: function (data) {

       },

       error: function (request, ajaxOptions, error) {

       },

       statusCode: require('data/statusCodes')
       });*/
      /*App.clusterStatus.setClusterStatus({
       clusterName: this.get('clusterName'),
       clusterState: 'UPGRADING_STACK',
       wizardControllerName: 'stackUpgradeController',
       localdb: App.db.data
       });*/
      this.startPolling();
    }
  }.observes('isServicesStopped'),
  /**
   * start polling on upgrade progress
   */
  startPolling: function(){
    //TODO set actual URL to poll upgrade progress
    var url = '';
    if(!this.get('isPolling')){
      this.set('isPolling', true);
      if (App.testMode) {
        this.simulatePolling();
      } else {
        //pass an interval "1" to start poll immediately first time
        this.doPoll(url, 1);
      }
    }
  },

  mockServices: [
    Em.Object.create({
      serviceName: 'GANGLIA',
      displayName: 'Ganglia',
      workStatus: 'STARTED',
      hostComponents: []
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      displayName: 'HDFS',
      workStatus: 'STARTED',
      hostComponents: []
    })
  ],
  simulateAttempt:0,
  /**
   * simulate actual poll, using mock data
   */
  simulatePolling: function(){
    var simulateAttempt = this.get('simulateAttempt');
    var URLs = [
      '/data/wizard/upgrade/poll_1.json',
      '/data/wizard/upgrade/poll_2.json',
      '/data/wizard/upgrade/poll_3.json',
      '/data/wizard/upgrade/poll_4.json',
      '/data/wizard/upgrade/poll_5.json'
    ];
    if(simulateAttempt < 5){
      this.doPoll(URLs[simulateAttempt]);
      this.set('simulateAttempt', ++simulateAttempt);
    }
  },
  /**
   * simulate stopping services before upgrade,
   * using mockServices data
   */
  simulateStopService: function(){
    var services = this.get('mockServices');
    var self = this;
    setTimeout(function(){
      services[0].set('workStatus', 'STOPPING');
    }, 4000);
    setTimeout(function(){
      services[0].set('workStatus', 'INSTALLED');
    }, 8000);
    setTimeout(function(){
      services[1].set('workStatus', 'STOPPING');
    }, 12000);
    setTimeout(function(){
      services[1].set('workStatus', 'INSTALLED');
      services.setEach('workStatus', 'INSTALLED');
    }, 16000);
  },

  /**
   * poll server for tasks, which contain upgrade progress data
   * @param url
   * @param interval
   */
  doPoll: function(url, interval){
    var self = this;
    var pollInterval = interval || self.POLL_INTERVAL;
    if (self.get('isPolling')) {
      setTimeout(function () {
        $.ajax({
          utype: 'GET',
          url: url,
          async: true,
          timeout: App.timeout,
          dataType: 'json',
          success: function (data) {
            var result = self.parseTasks(data);
            if (App.testMode) {
              self.simulatePolling();
            } else {
              self.doPoll(url);
            }
          },
          error: function () {

          },
          statusCode: require('data/statusCodes')
        }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
          function () {
            App.showReloadPopup();
            console.log('Install services all retries failed');
          }
        );
      }, pollInterval);
    }
  },
  /**
   * parse tasks from poll
   * change status, message, progress on services according to tasks
   * @param data
   * @return {Boolean}
   */
  parseTasks: function(data){
    var tasks = data.tasks || [];
    this.get('services').forEach(function (service) {
      var hosts = service.get('hosts');
      var tasksPerService = [];
      if(hosts.length){
        hosts.forEach(function (host) {
          var tasksPerHost = tasks.filter(function(task){
            if(task.Tasks.host_name == host.name && host.get('components').contains(task.Tasks.role)){
              return true;
            }
          });
          if (tasksPerHost.length) {
            this.setLogTasksStatePerHost(tasksPerHost, host);
            tasksPerService = tasksPerService.concat(tasksPerHost);
          }
        }, this);
        this.progressOnService(service, tasksPerService);
        this.statusOnService(service, tasksPerService);
      } else {
        service.set('status', 'PENDING');
        service.set('detailedMessage', Em.I18n.t('installer.stackUpgrade.step3.host.nothingToUpgrade'));
        console.log('None tasks matched to service ' + service);
      }
    }, this);
    this.set('serviceTimestamp', new Date().getTime());
    return true;
  },
  /**
   * evaluate status of service depending on the tasks
   * also set detailMessage that show currently running process
   * @param service
   * @param actions
   */
  statusOnService: function(service, actions){
    var status;
    var errorActions = actions.filter(function(action){
      if(action.Tasks.status == 'FAILED' || action.Tasks.status == 'ABORTED' || action.Tasks.status == 'TIMEDOUT'){
        return true;
      }
    });
    var masterComponents = ['NAMENODE', 'SECONDARY_NAMENODE', 'SNAMENODE', 'JOBTRACKER', 'ZOOKEEPER_SERVER', 'HIVE_SERVER',
      'HIVE_METASTORE', 'MYSQL_SERVER', 'HBASE_MASTER', 'NAGIOS_SERVER', 'GANGLIA_SERVER', 'OOZIE_SERVER','WEBHCAT_SERVER'];
    var failedComponents = errorActions.mapProperty('Tasks.role');
    if(failedComponents.length){
      for(var i = 0; i < failedComponents.length; i++){
        if(masterComponents.contains(failedComponents[i])){
          status = "FAILED";
          break;
        } else {
          status = "WARNING";
        }
      }
    } else if(actions.everyProperty('Tasks.status', 'COMPLETED')){
      status = 'SUCCESS';
    } else {
      var activeAction = actions.findProperty('Tasks.status', 'UPGRADING');
      status = 'IN_PROGRESS';
      if (activeAction === undefined || activeAction === null) {
        activeAction = actions.findProperty('Tasks.status', 'QUEUED');
        status = 'PENDING';
      }
      if (activeAction === undefined || activeAction === null) {
        activeAction = actions.findProperty('Tasks.status', 'PENDING');
        status = 'PENDING';
      }
      if(activeAction){
        service.set('detailMessage', this.displayMessage(activeAction.Tasks));
      }
    }
    service.set('status', status);
  },
  /**
   * calculate progress of service depending on the tasks
   * @param service
   * @param actions
   */
  progressOnService: function(service, actions){
    var progress = 0;
    var actionsNumber = actions.length;
    var completedActions = actions.filterProperty('Tasks.status', 'COMPLETED').length
      + actions.filterProperty('Tasks.status', 'FAILED').length
      + actions.filterProperty('Tasks.status', 'ABORTED').length
      + actions.filterProperty('Tasks.status', 'TIMEDOUT').length;
    var queuedActions = actions.filterProperty('Tasks.status', 'QUEUED').length;
    var inProgressActions = actions.filterProperty('Tasks.status', 'UPGRADING').length;
    progress = Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsNumber * 100);
    console.log('INFO: progressPerService is: ' + progress);
    service.set('progress', progress);
  },
  /**
   * determine description of current running process
   * @param task
   * @return {*}
   */
  displayMessage: function (task) {
    var role = App.format.role(task.role);
    // accept only default command - "UPGRADE"
    console.log("In display message with task command value: " + task.command);
    switch (task.status) {
      case 'PENDING':
        return Em.I18n.t('installer.step9.serviceStatus.upgrade.pending') + role;
      case 'QUEUED' :
        return Em.I18n.t('installer.step9.serviceStatus.upgrade.queued') + role;
      case 'UPGRADING':
        return Em.I18n.t('installer.step9.serviceStatus.upgrade.inProgress') + role;
      case 'COMPLETED' :
        return Em.I18n.t('installer.step9.serviceStatus.upgrade.completed') + role;
      case 'FAILED':
        return Em.I18n.t('installer.step9.serviceStatus.upgrade.failed') + role;
    }
  },
  /**
   * set and update logTasks to each host
   * @param tasksPerHost
   * @param host
   */
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    console.log('In step3 setTasksStatePerHost function.');
    tasksPerHost.forEach(function (_task) {
      console.log('In step3 _taskPerHost function.');
      var task = host.get('logTasks').findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        host.get('logTasks').removeObject(task);
      }
      host.get('logTasks').pushObject(_task);
      //}
    }, this);
  },
  /**
   * clear config and data after completion of upgrade
   */
  clearStep: function(){
    this.get('services').clear();
    this.set('isUpgradeStarted', false);
    this.resetMockConfig(false);
  },
  /**
   * reset mock configs to run upgrade simulation again
   */
  resetMockConfig: function(retry){
    if(!retry){
      this.get('mockServices').setEach('workStatus', 'STARTED');
    }
    this.set('simulateAttempt', 0);
  },
  /**
   * navigate to show current process depending on cluster status
   */
  navigateStep: function(){
    var clusterStatus = this.get('content.cluster.status');
    var status = 'PENDING';
    if (this.get('content.cluster.isCompleted') === false) {
      if (this.get('isServicesStopped')){
        this.startPolling();
        if (clusterStatus === 'STACK_UPGRADING'){
          // IN_PROGRESS
          status = 'IN_PROGRESS';
        } else if (clusterStatus === 'STACK_UPGRADE_FAILED'){
          // FAILED
          status = 'FAILED';
          // poll only one time
          this.set('isPolling', false);
        } else {
          // WARNING
          status = 'WARNING';
          // poll only one time
          this.set('isPolling', false);
        }
      } else {
        // services are stopping yet
      }
    } else {
      status = 'SUCCESS';
    }
    if (App.testMode) {
      if(!this.get('isServicesStopped')){
        this.simulateStopService();
      }
    } else {
      this.set('status', status);
    }
  }
});