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

  allOperations: [],
  allOperationsCount : 0,
  executeTasks: [],

  /**
   * For host component popup
   */
  services:[],
  serviceTimestamp: null,

  /**
   * Task life time after finishing
   */
  taskLifeTime: 5*60*1000,

  getOperationsForRequestId: function(requestId){
    return this.get('allOperations').filterProperty('request_id', requestId);
  },

  /**
   * Start polling, when <code>isWorking</code> become true
   */
  startPolling: function(){
    if(this.get('isWorking')){
      App.updater.run(this, 'loadOperations', 'isWorking', App.bgOperationsUpdateInterval);
    }
  }.observes('isWorking'),

  /**
   * Reload operations
   * @param callback on done Callback. Look art <code>App.updater.run</code> for more information
   * @return jquery ajax object
   */
  loadOperations : function(callback){

    if(!App.get('clusterName')){
      callback();
      return null;
    }

    return App.ajax.send({
      'name': 'background_operations',
      'sender': this,
      'success': 'updateBackgroundOperations', //todo provide interfaces for strings and functions
      'callback': callback
    });
  },

  /**
   * Callback for update finished task request.
   * @param data Json answer
   */
  updateFinishedTask: function(data){
    var executeTasks = this.get('executeTasks');
    if (data) {
      var _oldTask = executeTasks.findProperty('id', data.Tasks.id);
      if(_oldTask){
        data.Tasks.finishedTime = new Date().getTime();
        $.extend(_oldTask, data.Tasks);
      }
    }
  },

  /**
   * Update info about background operations
   * Put all tasks with command 'EXECUTE' into <code>executeTasks</code>, other tasks with it they are still running put into <code>runningTasks</code>
   * Put all task that should be shown in popup modal window into <code>this.allOperations</code>
   * @param data json loaded from server
   */
  updateBackgroundOperations: function (data) {
    var runningTasks = [];
    var executeTasks = this.get('executeTasks');
    data.items.forEach(function (item) {
      item.tasks.forEach(function (task) {
        task.Tasks.display_exit_code = (task.Tasks.exit_code !== 999);

        if (task.Tasks.command == 'EXECUTE') {

          var _oldTask = executeTasks.findProperty('id', task.Tasks.id);
          if (!_oldTask) {
            executeTasks.push(task.Tasks);
          } else {
            $.extend(_oldTask, task.Tasks);
          }

        } else if(['QUEUED', 'PENDING', 'IN_PROGRESS'].contains(task.Tasks.status)){
          runningTasks.push(task.Tasks);
        }
      });
    });

    var time = new Date().getTime() - this.get('taskLifeTime');
    var tasksToRemove = [];
    executeTasks.forEach(function(_task, index){
      if(['FAILED', 'COMPLETED', 'TIMEDOUT', 'ABORTED'].contains(_task.status) && _task.finishedTime && _task.finishedTime < time){
        tasksToRemove.push(index);
      }

      if(['QUEUED', 'PENDING', 'IN_PROGRESS'].contains(_task.status)){
        App.ajax.send({
          name: 'background_operations.update_task',
          data: {
            requestId: _task.request_id,
            taskId: _task.id
          },
          'sender': this,
          'success': 'updateFinishedTask'
        });
      }
    }, this);


    tasksToRemove.reverse().forEach(function(index){
      executeTasks.removeAt(index);
    });


    var currentTasks;
    currentTasks = runningTasks.concat(executeTasks);
    currentTasks = currentTasks.sort(function (a, b) {
      return a.id - b.id;
    });

    this.get('allOperations').filterProperty('isOpen').forEach(function(task){
      var _task = currentTasks.findProperty('id', task.id);
      if (_task) {
        _task.isOpen = true;
      }
    });

    this.set('allOperations', currentTasks);
    this.set('allOperationsCount', runningTasks.length + executeTasks.filterProperty('status', 'PENDING').length + executeTasks.filterProperty('status', 'QUEUED').length + executeTasks.filterProperty('status', 'IN_PROGRESS').length);

    var eventsArray = this.get('eventsArray');
    if (eventsArray.length) {

      var itemsToRemove = [];
      eventsArray.forEach(function(item){
        //if when returns true
        if(item.when(this)){
          //fire do method
          item.do();
          //and remove it
          itemsToRemove.push(item);
        }
      }, this);

      itemsToRemove.forEach(function(item){
        eventsArray.splice(eventsArray.indexOf(item), 1);
      });
    }
  },

  /**
   * Start code for hostcomponent popup in test mode for now
   */
  POLL_INTERVAL: 10,
  isPolling: false,
  installedServices: App.Service.find(),
  simulateAttempt:0,

  startPolling1: function(){
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

  simulatePolling: function(){
    var simulateAttempt = this.get('simulateAttempt');
    var URLs = [
      '/data/wizard/upgrade/poll_1.json',
      '/data/wizard/upgrade/poll_2.json',
      '/data/wizard/upgrade/poll_3.json',
      '/data/wizard/upgrade/poll_4.json',
      '/data/wizard/upgrade/poll_5.json',
    ];
    if(simulateAttempt < 5){
      if(this.get("simulateAttempt")==4){
        this.set("POLL_INTERVAL",4000);
      }
      this.doPoll(URLs[simulateAttempt]);
      this.set('simulateAttempt', ++simulateAttempt);
    }
  },

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
      } else {
        service.set('status', 'PENDING');
        service.set('detailedMessage', Em.I18n.t('installer.stackUpgrade.step3.host.nothingToUpgrade'));
      }
    }, this);
    this.set('serviceTimestamp', new Date().getTime());
    return true;
  },

  setLogTasksStatePerHost: function (tasksPerHost, host) {
    tasksPerHost.forEach(function (_task) {
      var task = host.get('logTasks').findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        host.get('logTasks').removeObject(task);
      }
      host.get('logTasks').pushObject(_task);
    }, this);
  },

  mockServices: [
    Em.Object.create({
      serviceName: 'GANGLIA',
      displayName: 'Ganglia Update',
      workStatus: 'STARTED',
      hostComponents: []
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      displayName: 'HDFS Update',
      workStatus: 'STARTED',
      hostComponents: []
    })
  ],

  loadServices: function(){
    var installedServices = App.testMode ? this.get('mockServices') : this.get('content.servicesInfo');
    var services = [];
    installedServices.forEach(function(_service){
      services.push(Em.Object.create({
        name: _service.get('serviceName'),
        displayName: _service.get('displayName'),
        hosts: this.loadHosts(_service),
        progress: 0,
        status: "PENDING",
        detailMessage:''
      }));
    }, this);
    this.set('services', services);
  }.observes('content.servicesInfo'),

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
   * End code for hostcomponent popup
   */


  /**

  /**
   * Onclick handler for background operations number located right to logo
   * @return PopupObject For testing purposes
   */
  showPopup: function(){
    if(App.testMode){
      this.set("POLL_INTERVAL",10);
      this.set("isPolling",false);
      this.set("simulateAttempt",0);
      this.loadServices();
      this.startPolling1();
      App.HostPopup.initPopup("", this, true);
    }else{
      this.set('executeTasks', []);
      App.updater.immediateRun('loadOperations');
      return App.ModalPopup.show({
        headerClass: Ember.View.extend({
          controller: this,
          template:Ember.Handlebars.compile('{{allOperationsCount}} Background Operations Running')
        }),
        bodyClass: Ember.View.extend({
          controller: this,
          templateName: require('templates/main/background_operations_popup')
        }),
        onPrimary: function() {
          this.hide();
        },
        secondary : null
      });
    }
  },

  /**
   * Example of data inside:
   * {
   *   when : function(backgroundOperationsController){
   *     return backgroundOperationsController.getOperationsForRequestId(requestId).length == 0;
   *   },
   *   do : function(){
   *     component.set('status', 'cool');
   *   }
   * }
   *
   * Function <code>do</code> will be fired once, when <code>when</code> returns true.
   * Example, how to use it, you can see in app\controllers\main\host\details.js
   */
  eventsArray : []

});
