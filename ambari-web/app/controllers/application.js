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

App.ApplicationController = Em.Controller.extend(App.UserPref, {

  name: 'applicationController',

  isPollerRunning: false,

  clusterName: function () {
    return (App.router.get('clusterController.clusterName') || 'My Cluster');
  }.property('App.router.clusterController.clusterName'),

  /**
   * set ambari server version from installerController or mainController, making sure version shown up all the time
   */
  ambariVersion: function () {
    return (App.router.get('installerController.ambariServerVersion') || App.router.get('mainController.ambariServerVersion') || Em.I18n.t('common.notAvailable'));
  }.property('App.router.installerController.ambariServerVersion', 'App.router.mainController.ambariServerVersion'),

  clusterDisplayName: function () {
    var name = this.get('clusterName');
    return name.length > 13 ? name.substr(0, 10) + "..." : name;
  }.property('clusterName'),

  isClusterDataLoaded: function() {
    return App.router.get('clusterController.isLoaded') && App.router.get('loggedIn');
  }.property('App.router.clusterController.isLoaded','App.router.loggedIn'),

  isExistingClusterDataLoaded: function () {
    return App.router.get('clusterInstallCompleted') && this.get('isClusterDataLoaded');
  }.property('App.router.clusterInstallCompleted', 'isClusterDataLoaded'),

  /**
   * Determines if "Exit" menu-item should be shown
   * It should if cluster isn't installed
   * If cluster is installer, <code>isClusterDataLoaded</code> is checked
   * @type {boolean}
   */
  showExitLink: function () {
    if (App.router.get('clusterInstallCompleted')) {
      return this.get('isClusterDataLoaded');
    }
    return true;
  }.property('App.router.clusterInstallCompleted', 'isClusterDataLoaded'),

  init: function(){
    this._super();
  },

  startKeepAlivePoller: function() {
    if (!this.get('isPollerRunning')) {
     this.set('isPollerRunning',true);
      App.updater.run(this, 'getStack', 'isPollerRunning', App.sessionKeepAliveInterval);
    }
  },

  getStack: function(callback) {
    App.ajax.send({
      name: 'router.login.clusters',
      sender: this,
      callback: callback
    });
  },

  goToAdminView: function () {
    App.router.route("adminView");
  },

  showAboutPopup: function() {

    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('common.aboutAmbari'),
      secondary: false,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/about'),
        ambariVersion: this.get('ambariVersion')
      })
    });    
  }

});