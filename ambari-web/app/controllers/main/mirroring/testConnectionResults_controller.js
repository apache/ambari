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

App.TestConnectionResultsController = Ember.Controller.extend({
  name: 'testConnectionResultsController',

  connectStatuses: [
    Ember.Object.create({
      type: 'readonly',
      isConnectionSuccess: false
    }),
    Ember.Object.create({
      type: 'write',
      isConnectionSuccess: false
    }),
    Ember.Object.create({
      type: 'workflow',
      isConnectionSuccess: false
    })

  ],

  isNameNodeWebUIConnected: function () {
    var connectStatus = this.get('connectStatuses').findProperty('type', 'readonly');
    return connectStatus.get('isConnectionSuccess');
  }.property('connectStatuses.@each.isConnectionSuccess'),

  isNameNodeRpcConnected: function () {
    var connectStatus = this.get('connectStatuses').findProperty('type', 'write');
    return connectStatus.get('isConnectionSuccess');
  }.property('connectStatuses.@each.isConnectionSuccess'),

  isOozieServerConnected: function () {
    var connectStatus = this.get('connectStatuses').findProperty('type', 'workflow');
    return connectStatus.get('isConnectionSuccess');
  }.property('connectStatuses.@each.isConnectionSuccess'),

  isConnectionSuccessful: function () {
    return this.get('isNameNodeWebUIConnected') && this.get('isNameNodeRpcConnected') && this.get('isOozieServerConnected');
  }.property('isNameNodeWebUIConnected', 'isNameNodeRpcConnected', 'isOozieServerConnected'),

  shouldBeDisabled: function () {
    return !this.get('isConnectionSuccessful');
  }.property('isConnectionSuccessful'),

  mockDataPrefix: '/data/mirroring/poll/',

  tryConnecting: function () {
    var types = ["readonly", "write", "workflow"];
    var arrayOfPollData = ["testConnection_poll1", "testConnection_poll2", "testConnection_poll3", "testConnection_poll4"];

    var shouldContinuePolling = true;

    var poll_count = 0;

    var interval_id = 0;

    var self = this;

    var connect = function () {
      var method = 'GET';
      console.debug('poll_count : ' + poll_count);
      var url = self.get('mockDataPrefix') + arrayOfPollData[poll_count++] + ".json";
      $.ajax({
        type: method,
        url: url,
        async: true, // shd be chnaged to true
        data: null, // temporarily .this depends upon what the api is expecting.
        dataType: 'text',
        timeout: App.timeout,
        success: function (data) {
          var jsonData = jQuery.parseJSON(data);
          var connectStatuses = self.get('connectStatuses');
          jsonData.tasks.forEach(function (task) {
            var type = task.Tasks.type;
            var status = task.Tasks.status;
            var connectStatus = connectStatuses.findProperty("type", type);
            connectStatus.set('isConnectionSuccess', status === "SUCCESS");
          });

          var totalNum = connectStatuses.length;
          var succeededStatuses = connectStatuses.filterProperty("isConnectionSuccess", true);
          var succeededNum = succeededStatuses.length;

          if (totalNum == succeededNum) {
            clearInterval(interval_id);
            console.debug('Cleared function id ' + interval_id);
          }
          else {
            clearInterval(interval_id);
            console.debug('Cleared function id ' + interval_id + "totalNum : " + totalNum + ', succeededNum : ' + succeededNum);
            interval_id = setInterval(connect, 100);
            console.debug('Generated function id ' + interval_id);
          }

        }
      });
    };

    connect();
  },

  saveClusterName: function () {
    var targetCluster = this.get('content.targetCluster');
    var isEditing = this.get('content.isPopupForEdit');
    if (isEditing) {
      var targetClusterRecord = this.get('content.originalRecord');
      var targetClusterEdited = this.get('content.targetCluster');

      targetClusterRecord.set('id', targetClusterEdited.get('id'));
      targetClusterRecord.set('clusterName', targetClusterEdited.get('clusterName'));
      targetClusterRecord.set('nameNodeWebUrl', targetClusterEdited.get('nameNodeRpcUrl'));
      targetClusterRecord.set('nameNodeRpcUrl', targetClusterEdited.get('id'));
      targetClusterRecord.set('oozieServerUrl', targetClusterEdited.get('oozieServerUrl'));


    } else {
      var targetClusterRecord = App.TargetCluster.createRecord(targetCluster);

      // refresh main page model
      var mainMirroringController = App.router.get('mainMirroringController');
      mainMirroringController.notifyPropertyChange("targetClusters");

      // refresh add/edit dataset model
      var addDataSetController = App.router.get('mainMirroringDataSetController');
      var dataSet = addDataSetController.get('model.newDataSet');
      if (dataSet)  // this may be undefined or null if we try to add cluster from main page. Hence the if check.
        dataSet.set('targetCluster', targetClusterRecord);
    }

    var popup = this.get('controllers.mainMirroringTargetClusterController.popup');
    popup.hide();
  }






});