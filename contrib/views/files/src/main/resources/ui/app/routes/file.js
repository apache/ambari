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

App.FilesRoute = Em.Route.extend({
  queryParams: {
    path: {
      refreshModel: true
    }
  },
  actions:{
    refreshDir:function () {
      this.refresh();
    },
    error:function (error,transition,e) {
      if (this.router._lookupActiveView('files')) {
        this.send('showAlert',error);
      } else {
        return true;
      };
    },
    dirUp: function () {
      var currentPath = this.controllerFor('files').get('path');
      var upDir = currentPath.substring(0,currentPath.lastIndexOf('/'));
      var target = upDir || '/';
      return this.transitionTo('files',{queryParams: {path: target}});
    },
    willTransition:function (argument) {
      this.send('removeAlert');
    },
    showAlert:function (error) {
      this.controllerFor('filesAlert').set('content',error);
      this.render('files.alert',{
        into:'files',
        outlet:'error',
        controller:'filesAlert'
      });
    },
    removeAlert:function () {
      this.disconnectOutlet({
        outlet: 'error',
        parentView: 'files'
      });
    }
  },
  model:function (params) {
    var path = (Em.isEmpty(params.path))?'/':params.path;
    return this.store.listdir(path);
  },
  afterModel:function (model) {
    var self = this;
    var model = model;
    this.store.filter('file',function(file) {
      if (!model.contains(file)) {
        return true;
      };
    }).then(function (files) {
      files.forEach(function (file) {
        file.store.unloadRecord(file);
      })
    });
  }
});
