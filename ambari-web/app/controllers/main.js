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
require('models/background_operation');

App.MainController = Em.Controller.extend({
  name: 'mainController',
  backgroundOperations: [],
  backgroundOperationsCount : 0,
  backgroundOperationsUrl : '',
  intervalId: false,
  updateOperationsInterval: 6000,
  clusters: App.Cluster.find(),
  cluster: function(){
    var clusters = this.get('clusters');
    if(clusters){
      var cluster = clusters.objectAt(0);
      return cluster;
    }
  }.property('clusters'),
  
  startLoadOperationsPeriodically: function() {
    this.loadBackgroundOperations();
    this.intervalId = setInterval(this.loadBackgroundOperations, this.get('updateOperationsInterval'));
  },
  stopLoadOperationsPeriodically:function () {
    if(this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.intervalId = false;
  },
  loadBackgroundOperations: function(){
    var self = App.router.get('mainController');

    var url = self.get('backgroundOperationsUrl');
    if(!url){
      //cache url, not to execute <code>getClusterName</code> everytime
      url = (App.testMode) ?
        '/data/background_operations/list_on_start.json' :
        '/api/clusters/' + App.router.getClusterName() + '/requests/?fields=tasks/*&tasks/Tasks/status!=COMPLETED';
      self.set('backgroundOperationsUrl', url);
    }

    $.ajax({
      type: "GET",
      url: url,
      dataType: 'json',
      timeout: 5000,
      success: function (data) {
        self.updateBackgroundOperations(data);
      },

      error: function (request, ajaxOptions, error) {
        //do something
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * Add new operations to <code>this.backgroundOperations</code> variable
   * @param data json loaded from server
   */
  updateBackgroundOperations : function(data){
    var runningTasks = [];
    data.items.forEach(function (item) {
      item.tasks.forEach(function (task) {
        if (task.Tasks.status == 'QUEUED') {
          runningTasks.push(task.Tasks);
        }
      });
    });

    var currentTasks = this.get('backgroundOperations');

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

    this.set('backgroundOperationsCount', currentTasks.length);

  },

  showBackgroundOperationsPopup: function(){
    App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controllerBinding: 'App.router.mainController',
        template:Ember.Handlebars.compile('{{backgroundOperationsCount}} Background Operations Running')
      }),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.mainController',
        templateName: require('templates/main/background_operations_popup')
      }),
      onPrimary: function() {
        this.hide();
      }
    });
  }
})