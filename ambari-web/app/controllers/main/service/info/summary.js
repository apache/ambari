/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.MainServiceInfoSummaryController = Em.Controller.extend({
  name: 'mainServiceInfoSummaryController',
  allAlerts: function(){
    return App.router.get('clusterController.alerts');
  }.property('App.router.clusterController.alerts'),

  alerts: function () {
    var serviceId = this.get('content.serviceName');
    if (serviceId) {
      return this.get('allAlerts').filter(function (item) {
        return item.get('serviceType').toLowerCase() == serviceId.toLowerCase() && !item.get('ignoredForServices');
      });
    }
    return [];
  }.property('allAlerts', 'content.serviceName'),
  
  nagiosUrl: function(){
    return App.router.get('clusterController.nagiosUrl');
  }.property('App.router.clusterController.nagiosUrl'),
  
  isNagiosInstalled: function(){
    return App.router.get('clusterController.isNagiosInstalled');
  }.property('App.router.clusterController.isNagiosInstalled'),

  isGangliaInstalled: function(){
    return App.router.get('clusterController.isGangliaInstalled');
  }.property('App.router.clusterController.isGangliaInstalled')
});