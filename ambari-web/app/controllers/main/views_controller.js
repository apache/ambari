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

App.MainViewsController = Em.Controller.extend({
  name:'mainViewsController',

  isDataLoaded: false,

  ambariViews: [],

  dataLoading: function () {
    var viewsController = this;
    var dfd = $.Deferred();
    if (this.get('isDataLoaded')) {
      dfd.resolve(this.get('ambariViews'));
    } else {
      var interval = setInterval(function () {
        if (viewsController.get('isDataLoaded')) {
          dfd.resolve(viewsController.get('ambariViews'));
          clearInterval(interval);
        }
      }, 50);
    }
    return dfd.promise();
  },


  loadAmbariViews: function () {
    if (!App.router.get('loggedIn')) {
      return;
    }
    App.ajax.send({
      name: 'views.info',
      sender: this,
      success: 'loadAmbariViewsSuccess',
      error: 'loadAmbariViewsError'
    });
  },

  loadAmbariViewsSuccess: function (data, opt, params) {
    if (data.items.length) {
      App.ajax.send({
        name: 'views.instances',
        sender: this,
        success: 'loadViewInstancesSuccess',
        error: 'loadViewInstancesError'
      });
    } else {
      this.set('ambariViews', []);
      this.set('isDataLoaded', true);
    }
  },

  loadAmbariViewsError: function () {
    this.set('ambariViews', []);
    this.set('isDataLoaded', true);
  },

  loadViewInstancesSuccess: function (data, opt, params) {
    this.set('ambariViews', []);
    var instances = [];
    data.items.forEach(function (view) {
      view.versions.forEach(function (version) {
        version.instances.forEach(function (instance) {
          var current_instance = Em.Object.create({
            iconPath: instance.ViewInstanceInfo.icon_path || "/img/ambari-view-default.png",
            label: instance.ViewInstanceInfo.label || version.ViewVersionInfo.label || instance.ViewInstanceInfo.view_name,
            visible: instance.ViewInstanceInfo.visible || false,
            version: instance.ViewInstanceInfo.version,
            description: instance.ViewInstanceInfo.description || Em.I18n.t('views.main.instance.noDescription'),
            viewName: instance.ViewInstanceInfo.view_name,
            instanceName: instance.ViewInstanceInfo.instance_name,
            href: instance.ViewInstanceInfo.context_path + "/"
          });
          if( current_instance.visible ){
            instances.push(current_instance);
          }
        }, this);
      }, this);
    }, this);
    this.get('ambariViews').pushObjects(instances);
    this.set('isDataLoaded', true);
  },

  loadViewInstancesError: function () {
    this.set('ambariViews', []);
    this.set('isDataLoaded', true);
  },

  setView: function(event) {
    if(event.context){
      App.router.route('main/views/' + event.context.viewName + '/' + event.context.version + '/' + event.context.instanceName);
    }
  }
});