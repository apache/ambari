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

  getOperationsForRequestId: function(requestId){
    return this.get('allOperations').filterProperty('request_id', requestId);
  },

  updateInterval: App.bgOperationsUpdateInterval,
  url : '',

  generateUrl: function(){
    var url = App.testMode ?
      '/data/background_operations/list_on_start.json' :
      App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/requests/?fields=tasks/*&tasks/Tasks/status!=COMPLETED';

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
   * Add new operations to <code>this.allOperations</code> variable
   * @param data json loaded from server
   */
  updateBackgroundOperations : function(data){
    var runningTasks = [];
    data.items.forEach(function (item) {
      item.tasks.forEach(function (task) {
        if (task.Tasks.status == 'QUEUED' || task.Tasks.status == 'PENDING') {
          runningTasks.push(task.Tasks);
        }
      });
    });

    runningTasks = runningTasks.sort(function(a,b){
      return a.id - b.id;
    });

    var currentTasks = this.get('allOperations');

    runningTasks.forEach(function(item){
      var task = currentTasks.findProperty('id', item.id);
      if(task){
        currentTasks[currentTasks.indexOf(task)] = item;
      } else {
        currentTasks.pushObject(item);
      }
    });

    for(var i = currentTasks.length-1; i>=0; i--){
      var isTaskFinished = !runningTasks.someProperty('id', currentTasks[i].id);
      if(isTaskFinished){
        currentTasks.removeAt(i);
      }
    }

    this.set('allOperationsCount', currentTasks.length);

    var eventsArray = this.get('eventsArray');
    if(eventsArray.length){

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
