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

App.ApplicationController = Em.Controller.extend({

  name: 'applicationController',
  clusterName: function () {
    var name = App.router.get('clusterController.clusterName');
    if (name) {
      return name.capitalize();
    }
    return Em.I18n.t('installer.header');
  }.property('App.router.clusterController.clusterName'),
  isClusterDataLoaded: function() {
    return App.router.get('clusterController.isLoaded');
  }.property('App.router.clusterController.isLoaded'),

  init: function(){
    this._super();
  }
});