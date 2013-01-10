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
  
  clusterName: function () {
    var name = App.router.get('clusterController.clusterName');
    if (name) {
      return name;
    }
    return 'My Cluster';
  }.property('App.router.clusterController.clusterName'),
  
  clusterDisplayName: function () {
    var name = App.router.get('clusterController.clusterName');
    if (name) {
      var displayName = name.length > 13 ? name.substr(0, 10) + "..." : name;
      return displayName.capitalize();
    }
    return 'My Cluster';
  }.property('App.router.clusterController.clusterName'),

  updateTitle: function(){
    var name = App.router.get('clusterController.clusterName');
    if (name) {
      name = name.length > 13 ? name.substr(0, 10) + "..." : name;
      name = name.capitalize();
    } else {
      name = 'Loading';
    }
    $('title').text('Ambari - ' + name);
  }.observes('App.router.clusterController.clusterName'),

  isClusterDataLoaded: function(){
      return App.router.get('clusterController.isLoaded');
  }.property('App.router.clusterController.isLoaded'),
  /**
   * run all processes and cluster's data loading
   */
  initialize: function(){
    App.router.get('clusterController').loadClusterData();
    this.startPolling();
  },
  startPolling: function(){
    App.router.get('updateController').set('isWorking', true);
    App.router.get('backgroundOperationsController').set('isWorking', true);
    App.router.get('clusterController').startLoadUpdatedStatus();
  },
  stopPolling: function(){
    App.router.get('updateController').set('isWorking', false);
    App.router.get('backgroundOperationsController').set('isWorking', false);
    App.router.get('clusterController').set('isWorking', false);
  }
})