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

  allOperationsCount : 0,

  /**
   * For host component popup
   */
  services:[],
  serviceTimestamp: null,

  /**
   * Start polling, when <code>isWorking</code> become true
   */
  startPolling: function(){
    if(this.get('isWorking')){
      this.requestMostRecent();
      App.updater.run(this, 'requestMostRecent', 'isWorking', App.bgOperationsUpdateInterval);
    }
  }.observes('isWorking'),

  /**
   * Get all requests from server
   * @param callback
   */
  requestMostRecent: function(callback){
    App.ajax.send({
      name: 'background_operations.get_most_recent',
      'sender': this,
      'success': 'callBackForMostRecent',
      'callback': callback
    });
  },

  /**
   * Prepare recived from server requests for host component popup
   * @param data
   */
  callBackForMostRecent: function(data){
    this.get("services").clear();
    var runningServices = 0;
    var self = this;
    data.items = data.items.sort(function(a,b){return b.Requests.id - a.Requests.id}).slice( 0, 10);
    data.items.forEach(function(request){
      var rq = Em.Object.create({
        id:request.Requests.id,
        name: 'Request name not specified',
        displayName: 'Request name not specified',
        progress:10,
        status: "",
        isRunning: false,
        hosts: []
      })
      if(request.Requests.request_context != ""){
        rq.name = request.Requests.request_context;
        rq.displayName = request.Requests.request_context;
      }

      var runningTasks = 0;
      runningTasks = request.tasks.filterProperty('Tasks.status', 'QUEUED').length;
      runningTasks += request.tasks.filterProperty('Tasks.status', 'IN_PROGRESS').length;
      runningTasks += request.tasks.filterProperty('Tasks.status', 'PENDING').length;
      if(runningTasks > 0){
        runningServices++;
      }

      var hostNames = request.tasks.mapProperty('Tasks.host_name').uniq();
      hostNames.forEach(function (name) {
        var tasks = request.tasks.filterProperty("Tasks.host_name",name);
        rq.get("hosts").push({
          name: name,
          publicName: name,
          logTasks: tasks
        });
      });
      self.get("services").push(rq);
    });
    self.set("allOperationsCount",runningServices);
    self.set('serviceTimestamp', new Date().getTime());
  },


  /**
   * Onclick handler for background operations number located right to logo
   * @return PopupObject For testing purposes
   */
  showPopup: function(){
    if(!App.testMode){
      App.updater.immediateRun('requestMostRecent');
    }
    return App.HostPopup.initPopup("", this, true);
  }

});
