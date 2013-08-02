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

App.MainAdminHighAvailabilityController = Em.Controller.extend({
  name: 'mainAdminHighAvailabilityController',

  securityEnabled: false,

  tag: null,

  dataIsLoaded: false,

  enableHighAvailability: function () {
    //Prerequisite Checks
    if (App.Host.find().content.length < 3) {
      this.showErrorPopup(Em.I18n.t('admin.highAvailability.error.hostsNum'));
      return;
    }
    if (App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').length < 3) {
      this.showErrorPopup(Em.I18n.t('admin.highAvailability.error.zooKeeperNum'));
      return;
    }
    if (this.get('securityEnabled')) {
      this.showErrorPopup(Em.I18n.t('admin.highAvailability.error.security'));
      return;
    }
    App.router.transitionTo('enableHighAvailability');
  },

  setSecurityStatus: function () {
    if (App.testMode) {
      this.set('securityEnabled', !App.testEnableSecurity);
      this.set('dataIsLoaded', true);
    } else {
      //get Security Status From Server
      App.ajax.send({
        name: 'admin.security_status',
        sender: this,
        success: 'getSecurityStatusFromServerSuccessCallback',
        error: 'errorCallback'
      });
    }
  },

  errorCallback: function () {
    this.showErrorPopup(Em.I18n.t('admin.security.status.error'));
  },

  getSecurityStatusFromServerSuccessCallback: function (data) {
    var configs = data.Clusters.desired_configs;
    if ('global' in configs) {
      this.set('tag', configs['global'].tag);
      this.getServiceConfigsFromServer();
    }
    else {
      this.showErrorPopup(Em.I18n.t('admin.security.status.error'));
    }
  },

  getServiceConfigsFromServer: function () {
    App.ajax.send({
      name: 'admin.service_config',
      sender: this,
      data: {
        siteName: 'global',
        tagName: this.get('tag')
      },
      success: 'getServiceConfigsFromServerSuccessCallback',
      error: 'errorCallback'
    });
  },

  getServiceConfigsFromServerSuccessCallback: function (data) {
    var configs = data.items.findProperty('tag', this.get('tag')).properties;
    if (configs && (configs['security_enabled'] === 'true' || configs['security_enabled'] === true)) {
      this.set('securityEnabled', true);
      this.set('dataIsLoaded', true);
    }
    else {
      this.set('securityEnabled', false);
      this.set('dataIsLoaded', true);
    }
  },

  showErrorPopup: function (message) {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>' + message + '</p>')
      }),
      onPrimary: function () {
        this.hide();
      },
      secondary: false
    });
  }
});
