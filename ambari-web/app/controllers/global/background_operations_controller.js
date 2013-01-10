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

  getTasksByRole: function (role) {
    return this.get('allOperations').filterProperty('role', role);
  },

  getOperationsForRequestId: function(requestId){
    return this.get('allOperations').filterProperty('request_id', requestId);
  },

  updateInterval: App.bgOperationsUpdateInterval,
  url : '',

  generateUrl: function(){
    var url = App.testMode ?
      '/data/background_operations/list_on_start.json' :
      App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/requests/?fields=tasks/*';

    this.set('url', url);
    return url;
  },

  timeoutId : null,

  /**
   * Background operations will not be working if receive <code>attemptsCount</code> response with errors
   */
  attemptsCount: 20,

  errorsCount: 0,

  /**
   * Call this.loadOperations with delay
   * @param delay time in milliseconds (updateInterval by default)
   * @param reason reason why we call it(used to calculate count of errors)
   */
  loadOperationsDelayed: function(delay, reason){
    delay = delay || this.get('updateInterval');
    var self = this;

    if(reason && reason.indexOf('error:clusterName:') === 0){
      var errors = this.get('errorsCount') + 1;
      this.set('errorsCount', errors);
      if(errors > this.get('attemptsCount')){
        console.log('Stop loading background operations: clusterName is undefined');
        return;
      }
    }

    this.set('timeoutId',
      setTimeout(function(){
        self.loadOperations();
      }, delay)
    );
  },

  /**
   * Reload operations
   * We can call it manually <code>controller.loadOperations();</code>
   * or it fires automatically, when <code>isWorking</code> becomes <code>true</code>
   */
  loadOperations : function(){

    var timeoutId = this.get('timeoutId');
    if(timeoutId){
      clearTimeout(timeoutId);
      this.set('timeoutId', null);
    }

    if(!this.get('isWorking')){
      return;
    }
    var self = this;

    if(!App.router.getClusterName()){
      this.loadOperationsDelayed(this.get('updateInterval')/2, 'error:clusterName');
      return;
    }

    var url = this.get('url');
    if(!url){
      url = this.generateUrl();
    }

    $.ajax({
      type: "GET",
      url: url,
      dataType: 'json',
      timeout: App.timeout,
      success: function (data) {
        //refresh model
        self.updateBackgroundOperations(data);

        self.loadOperationsDelayed();
      },

      error: function (request, ajaxOptions, error) {
        self.loadOperationsDelayed(null, 'error:response error');
      },

      statusCode: require('data/statusCodes')
    });

  }.observes('isWorking'),

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
        if (task.Tasks.command == 'EXECUTE') {
          if (!executeTasks.someProperty('id', task.Tasks.id)) {
            executeTasks.push(task.Tasks);
          }
        } else {
          if (task.Tasks.status == 'QUEUED' || task.Tasks.status == 'PENDING' || task.Tasks.status == 'IN_PROGRESS') {
            runningTasks.push(task.Tasks);
          }
        }
      });
    });

    for (var i = 0; i < executeTasks.length; i++) {
      if (executeTasks[i].status == 'QUEUED' || executeTasks[i].status == 'PENDING' || executeTasks[i].status == 'IN_PROGRESS') {
        var url = App.testMode ? '/data/background_operations/list_on_start.json' :
            App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/requests/' + executeTasks[i].request_id + '/tasks/' + executeTasks[i].id;
        var j = i;
        $.ajax({
          type: "GET",
          url: url,
          dataType: 'json',
          timeout: App.timeout,
          success: function (data) {
            if (data) {
              executeTasks[j] = data.Tasks;
            }
          },
          error: function () {
            console.log('ERROR: error during executeTask update');
          },

          statusCode: require('data/statusCodes')
        });
      }
    }
    ;
    var currentTasks;
    currentTasks = runningTasks.concat(executeTasks);
    currentTasks = currentTasks.sort(function (a, b) {
      return a.id - b.id;
    });

    // If the server is returning 999 as the return code, display blank and not 999
    currentTasks.forEach( function (task) {
      if (task.exit_code == 999) {
        task.display_exit_code = false;
      } else {
        task.display_exit_code = true;
      }
    });

    this.get('allOperations').filterProperty('isOpen').mapProperty('id').forEach(function(id){
      if (currentTasks.someProperty('id', id)) {
        currentTasks.findProperty('id', id).isOpen = true;
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
   * Onclick handler for background operations number located right to logo
   */
  showPopup: function(){
    this.set('executeTasks', []);
    this.loadOperations();
    App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controllerBinding: 'App.router.backgroundOperationsController',
        template:Ember.Handlebars.compile('{{allOperationsCount}} Background Operations Running')
      }),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.backgroundOperationsController',
        templateName: require('templates/main/background_operations_popup')
      }),
      onPrimary: function() {
        this.hide();
      },
      secondary : null
    });
  },

  /**
   * Exaple of data inside:
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
