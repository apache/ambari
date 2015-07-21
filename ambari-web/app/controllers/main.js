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

  updateTitle: function(){
    var name = App.router.get('clusterController.clusterName');
    if(App.router.get('clusterInstallCompleted')) {
      if (name && App.router.get('clusterController').get('isLoaded')) {
        name = name.length > 13 ? name.substr(0, 10) + "..." : name;
      } else {
        name = Em.I18n.t('common.loading');
      }
      $('title').text(Em.I18n.t('app.name.subtitle').format(name));
    }
  }.observes('App.router.clusterController.clusterName, App.router.clusterInstallCompleted', 'App.router.clusterController.isLoaded'),

  isClusterDataLoaded: function(){
    return App.router.get('clusterController.isLoaded');
  }.property('App.router.clusterController.isLoaded'),

  clusterDataLoadedPercent: function(){
    return App.router.get('clusterController.clusterDataLoadedPercent');
  }.property('App.router.clusterController.clusterDataLoadedPercent'),
  /**
   * run all processes and cluster's data loading
   */
  initialize: function(){
    App.router.get('clusterController').loadClusterData();
  },

  dataLoading: function () {
    var self = this;
    var dfd = $.Deferred();
    if (App.router.get('clusterController.isLoaded')) {
      dfd.resolve();
    } else {
      var interval = setInterval(function () {
        if (self.get('isClusterDataLoaded')) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, 50);
    }
    return dfd.promise();
  },

  /**
   *
   * @param isLoaded {Boolean}
   * @param opts {Object}
   * {
   *   period {Number}
   * }
   * @return {*|{then}}
   */
  isLoading: function(isLoaded, opts) {
    var dfd = $.Deferred();
    var self = this;
    opts = opts || {};
    var period =  opts.period || 20;
    if (this.get(isLoaded)) {
      dfd.resolve();
    } else {
      var interval = setInterval(function () {
        if (self.get(isLoaded)) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, period);
    }
    return dfd.promise();
  },

  startPolling: function () {
    if (App.router.get('applicationController.isExistingClusterDataLoaded')) {
      App.router.get('updateController').set('isWorking', true);
      App.router.get('backgroundOperationsController').set('isWorking', true);
    }
  }.observes('App.router.applicationController.isExistingClusterDataLoaded'),
  stopPolling: function(){
    App.router.get('updateController').set('isWorking', false);
    App.router.get('backgroundOperationsController').set('isWorking', false);
  },

  reloadTimeOut: null,

  pageReload: function () {

    clearTimeout(this.get("reloadTimeOut"));

    this.set('reloadTimeOut',
        setTimeout(function () {
          if (App.clusterStatus.get('isInstalled')) {
            location.reload();
          }
        }, App.pageReloadTime)
    );
  }.observes("App.router.location.lastSetURL", "App.clusterStatus.isInstalled"),

  scRequest: function(request) {
    return App.router.get('mainServiceController').get(request);
  },

  isAllServicesInstalled: function() {
    return this.scRequest('isAllServicesInstalled');
  }.property('App.router.mainServiceController.content.content.@each',
      'App.router.mainServiceController.content.content.length'),

  isStartAllDisabled: function() {
    return this.scRequest('isStartAllDisabled');
  }.property('App.router.mainServiceController.isStartStopAllClicked',
      'App.router.mainServiceController.content.@each.healthStatus'),

  isStopAllDisabled: function() {
    return this.scRequest('isStopAllDisabled');
  }.property('App.router.mainServiceController.isStartStopAllClicked',
      'App.router.mainServiceController.content.@each.healthStatus'),

  gotoAddService: function() {
    App.router.get('mainServiceController').gotoAddService();
  },

  startAllService: function(event){
    App.router.get('mainServiceController').startAllService(event);
  },

  stopAllService: function(event){
    App.router.get('mainServiceController').stopAllService(event);
  },

  /**
   * check server version and web client version
   */
  checkServerClientVersion: function () {
    var dfd = $.Deferred();
    var self = this;
    self.getServerVersion().done(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },
  getServerVersion: function(){
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields: '?fields=RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true'
      },
      success: 'getServerVersionSuccessCallback',
      error: 'getServerVersionErrorCallback'
    });
  },
  getServerVersionSuccessCallback: function (data) {
    var clientVersion = App.get('version');
    var serverVersion = (data.RootServiceComponents.component_version).toString();
    this.set('ambariServerVersion', serverVersion);
    if (clientVersion) {
      this.set('versionConflictAlertBody', Em.I18n.t('app.versionMismatchAlert.body').format(serverVersion, clientVersion));
      this.set('isServerClientVersionMismatch', clientVersion != serverVersion);
    } else {
      this.set('isServerClientVersionMismatch', false);
    }
    App.set('isManagedMySQLForHiveEnabled', App.config.isManagedMySQLForHiveAllowed(data.RootServiceComponents.properties['server.os_family']));
  },
  getServerVersionErrorCallback: function () {
    console.log('ERROR: Cannot load Ambari server version');
  }

});
