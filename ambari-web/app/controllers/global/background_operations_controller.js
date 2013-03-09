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
   * Onclick handler for background operations number located right to logo
   * @return PopupObject For testing purposes
   */
  showPopup: function(){
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
