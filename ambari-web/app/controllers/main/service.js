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

App.MainServiceController = Em.ArrayController.extend({
  name:'mainServiceController',
  content: function(){
    if(!App.router.get('clusterController.isLoaded')){
      return [];
    }
    return App.Service.find();
  }.property('App.router.clusterController.isLoaded'),

  cluster: function () {
    if (!App.router.get('clusterController.isLoaded')) {
      return null;
    }
    return App.Cluster.find().objectAt(0);
  }.property('App.router.clusterController.isLoaded'),

  hdfsService: function () {
    var hdfsSvcs = App.HDFSService.find();
    if (hdfsSvcs && hdfsSvcs.get('length') > 0) {
      return hdfsSvcs.objectAt(0);
    }
    return null;
  }.property('App.router.clusterController.isLoaded', 'App.router.updateController.isUpdated'),

  isStartAllDisabled: function(){
    if(this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var stoppedServiceLength = this.get('content').filterProperty('healthStatus','red').length;
    return (stoppedServiceLength === 0); // all green status
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),
  isStopAllDisabled: function(){
    if(this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var startedService = this.get('content').filterProperty('healthStatus','green');
    var flag = true;
    startedService.forEach(function(item){
      if(!['HCATALOG', 'PIG', 'SQOOP'].contains(item.get('serviceName'))){
        flag = false;
      }
    })
    return flag;
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),
  isStartStopAllClicked: function(){
    return (App.router.get('backgroundOperationsController').get('allOperationsCount') !== 0);
  }.property('App.router.backgroundOperationsController.allOperationsCount'),

  /**
   * callback for <code>start all service</code> button
   */
  startAllService: function(event){
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }
    var self = this;
    App.showConfirmationPopup2(function() {
      self.startAllServicesCall('startAllService');
    });
  },

  /**
   * callback for <code>stop all service</code> button
   */
  stopAllService: function(event){
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }
    var self = this;
    App.showConfirmationPopup2(function() {
      self.startAllServicesCall('stopAllService');
    });
  },

  startAllServicesCall: function(state){
    var clusterName = App.router.get('applicationController').get('clusterName');
    var method = 'PUT';
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services?ServiceInfo';
    var data;
    if(state == 'stopAllService'){
      data = '{"RequestInfo": {"context" :"'+ Em.I18n.t('requestInfo.stopAllServices') +'"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
    }else{
      data = '{"RequestInfo": {"context" :"'+ Em.I18n.t('requestInfo.startAllServices') +'"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}';
    }

    if (App.testMode) {
      url = this.get('mockDataPrefix') + '/poll_6.json';
      method = 'GET';
      this.numPolls = 6;
    }

    $.ajax({
      type: method,
      url: url,
      async: false,
      data: data,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: Start/Stop all service -> In success function for the start/stop all Service call");
        console.log("TRACE: Start/Stop all service -> value of the url is: " + url);
        console.log("TRACE: Start/Stop all service -> value of the received data is: " + jsonData);
        var requestId = jsonData.Requests.id;
        console.log('requestId is: ' + requestId);

        App.router.get('backgroundOperationsController').showPopup();
      },
      error: function () {
        console.log("ERROR");
      },

      statusCode: require('data/statusCodes')
    });

  }
})