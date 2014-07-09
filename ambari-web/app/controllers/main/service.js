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
  }.property('App.router.clusterController.isLoaded').volatile(),

  cluster: function () {
    if (!App.router.get('clusterController.isLoaded')) {
      return null;
    }
    return App.Cluster.find().objectAt(0);
  }.property('App.router.clusterController.isLoaded'),

  isAllServicesInstalled: function() {
    if (!this.get('content.content')) return false;

    var availableServices = App.StackService.find().mapProperty('serviceName');
    if (!App.supports.hue) {
      availableServices = availableServices.without('HUE');
    }
    return this.get('content.content').length == availableServices.length;
  }.property('content.content.@each', 'content.content.length'),

  isStartAllDisabled: function(){
    if(this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var stoppedServices =  this.get('content').filter(function(_service){
      return (_service.get('healthStatus') === 'red' && !App.get('services.clientOnly').contains(_service.get('serviceName')));
    });
    return (stoppedServices.length === 0); // all green status
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),
  isStopAllDisabled: function(){
    if(this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var startedServiceLength = this.get('content').filterProperty('healthStatus','green').length;
    return (startedServiceLength === 0);
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
    App.showConfirmationFeedBackPopup(function(query) {
      self.allServicesCall('STARTED', query);
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
    App.showConfirmationFeedBackPopup(function(query) {
      self.allServicesCall('INSTALLED', query);
    });
  },

  allServicesCall: function(state, query) {
    var context = (state == 'INSTALLED') ? App.BackgroundOperationsController.CommandContexts.STOP_ALL_SERVICES :
       App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES
    App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        context: context,
        ServiceInfo: {
          state: state
        },
        query: query
      },
      success: 'allServicesCallSuccessCallback',
      error: 'allServicesCallErrorCallback'
    });
  },

  allServicesCallSuccessCallback: function(data, xhr, params) {
    console.log("TRACE: Start/Stop all service -> In success function for the start/stop all Service call");
    console.log("TRACE: Start/Stop all service -> value of the received data is: " + data);
    var requestId = data.Requests.id;
    params.query.set('status', 'SUCCESS');
    console.log('requestId is: ' + requestId);

    // load data (if we need to show this background operations popup) from persist
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },
  allServicesCallErrorCallback: function(request, ajaxOptions, error, opt, params) {
    console.log("ERROR");
    params.query.set('status', 'FAIL');
  },

  gotoAddService: function() {
    if (this.get('isAllServicesInstalled')) {
      return;
    }
    App.router.transitionTo('main.serviceAdd');
  }
});
