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
    data.items = data.items.sort(function(a,b){return b.Requests.id - a.Requests.id});
    data.items.forEach(function(request){
      var hostsMap = {};
      var isRunningTasks = false;
      request.tasks.forEach(function (task) {
        if (!isRunningTasks && (['QUEUED', 'IN_PROGRESS', 'PENDING'].contains(task.Tasks.status))) {
          isRunningTasks = true;
        }
        if (hostsMap[task.Tasks.host_name]) {
          hostsMap[task.Tasks.host_name].logTasks.push(task);
        } else {
          hostsMap[task.Tasks.host_name] = {
            name: task.Tasks.host_name,
            publicName: task.Tasks.host_name,
            logTasks: [task]
          };
        }
      }, this);

      var rq = Em.Object.create({
        id: request.Requests.id,
        name: request.Requests.request_context || 'Request name not specified',
        displayName: request.Requests.request_context || 'Request name not specified',
        progress: 10,
        status: "",
        isRunning: isRunningTasks,
        hostsMap: hostsMap,
        tasks: request.tasks
      });
      runningServices += ~~isRunningTasks;
      self.get("services").push(rq);
    });
    self.set("allOperationsCount",runningServices);
    self.set('serviceTimestamp', new Date().getTime());
  },

  popupView: null,

  /**
   * Onclick handler for background operations number located right to logo
   * @return PopupObject For testing purposes
   */
  showPopup: function(){
    App.updater.immediateRun('requestMostRecent');
    if(this.get('popupView') && App.HostPopup.get('showServices')){
      this.set('popupView.isOpen', true);
      $(this.get('popupView.element')).appendTo('#wrapper');
    } else {
      this.set('popupView', App.HostPopup.initPopup("", this, true));
    }
  }

});
