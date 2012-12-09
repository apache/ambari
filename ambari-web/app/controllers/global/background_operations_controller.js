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

  /**
   * Update it every time when background operations for services are changed
   */
  serviceOperationsChangeTime: function(){
    return (new Date().getTime());
  }.property('hdfsOperations', 'mapReduceOperations'),

  hdfsOperations : function(){
    var all = this.get('allOperations');
    var result = [];

    all.forEach(function(item){
      if( ['NAMENODE', 'SECONDARY_NAMENODE', 'DATANODE', 'HDFS_CLIENT', 'HDFS_SERVICE_CHECK'].contains(item.role)){
        result.push(item);
      }
    })
    return result;
  }.property('allOperations.@each'),

  mapReduceOperations : function(){
    var all = this.get('allOperations');
    var result = [];

    all.forEach(function(item){
      if( ['MAPREDUCE_CLIENT', 'JOBTRACKER', 'TASKTRACKER', 'MAPREDUCE_SERVICE_CHECK'].contains(item.role)){
        result.push(item);
      }
    })
    return result;

  }.property('allOperations.@each'),

  getOperationsFor: function(serviceName){
    switch(serviceName.toUpperCase()){
      case 'HDFS':
        return this.get('hdfsOperations');
      case 'MAPREDUCE':
        return this.get('mapReduceOperations');
      default:
        return [];
    }
  },

  updateInterval: 6000,
  url : '',

  generateUrl: function(){
    var url = App.testMode ?
      '/data/background_operations/list_on_start.json' :
      '/api/clusters/' + App.router.getClusterName() + '/requests/?fields=tasks/*&tasks/Tasks/status!=COMPLETED';

    this.set('url', url);
    return url;
  },

  /**
   * Reload operations
   * We can call it manually <code>controller.loadOperations();</code>
   * or it fires automatically, when <code>isWorking</code> becomes <code>true</code>
   */
  loadOperations : function(){

    if(!this.get('isWorking')){
      return;
    }
    var self = this;

    var url = this.get('url');
    if(!url){
      url = this.generateUrl();
    }

    $.ajax({
      type: "GET",
      url: url,
      dataType: 'json',
      timeout: 5000,
      success: function (data) {
        //refresh model
        self.updateBackgroundOperations(data);

        //load data again if isWorking = true
        if(self.get('isWorking')){
          setTimeout(function(){
            self.loadOperations();
          }, self.get('updateInterval'));
        }
      },

      error: function (request, ajaxOptions, error) {
        console.log('cannot load background operations array');

        //next code is temporary code to fix testMode issues
        self.set('url', '/data/background_operations/list_on_start.json');
        //load data again if isWorking = true
        if(self.get('isWorking')){
          setTimeout(function(){
            self.loadOperations();
          }, self.get('updateInterval'));
        }
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
  },

  /**
   * Onclick handler for background operations number located right to logo
   */
  showPopup: function(){
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
  }

});
