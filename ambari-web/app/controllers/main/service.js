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
  isAdmin: function(){
    return App.db.getUser().admin;
  }.property('App.router.loginController.loginName'),
  hdfsService: function () {
    var hdfsSvcs = App.HDFSService.find();
    if (hdfsSvcs && hdfsSvcs.get('length') > 0) {
      return hdfsSvcs.objectAt(0);
    }
    return null;
  }.property('App.router.clusterController.isLoaded', 'App.router.updateController.isUpdated')
})