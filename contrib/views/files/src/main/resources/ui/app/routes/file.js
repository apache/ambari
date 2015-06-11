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
    loading:function  (argument) {
      var target = this.controllerFor('files');
      target.showSpinner();
      this.router.one('didTransition', target, 'hideSpinner');
    },
    error:function (error,transition,e) {
      if (this.router._lookupActiveView('files')) {
        this.send('showAlert',error);
      } else {
        return true;
      }
    },
    dirUp: function () {
      var currentPath = this.controllerFor('files').get('path');
      var upDir = currentPath.substring(0,currentPath.lastIndexOf('/'));
      var target = upDir || '/';
      return this.transitionTo('files',{queryParams: {path: target}});
    },
    willTransition:function (argument) {
      var hasModal = this.router._lookupActiveView('modal.chmod'),
          hasAlert = this.router._lookupActiveView('files.alert');

      Em.run.next(function(){
        if (hasAlert) this.send('removeAlert');
        if (hasModal) this.send('removeChmodModal');
      }.bind(this));
    },
    showChmodModal:function (content) {
      this.controllerFor('chmodModal').set('content',content);
      this.render('modal.chmod',{
        into:'files',
        outlet:'modal',
        controller:'chmodModal'
      });
    },
    removeChmodModal:function () {
      this.disconnectOutlet({
        outlet: 'modal',
        parentView: 'files'
      });
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
    var model = this.store.listdir(path);
    this.set('prevModel',model);
    return model;
  },
  prevModel:null,
  beforeModel:function () {
    if (this.get('prevModel.isPending')) {
      this.get('prevModel').then(function (files) {
        files.forEach(function (file) {
          file.store.unloadRecord(file);
        });
      });
    }
  },
  afterModel: function (model) {
    this.store.all('file').forEach(function (file) {
      if (!model.contains(file)) {
        file.unloadRecord();
      }
    });
  }
});
