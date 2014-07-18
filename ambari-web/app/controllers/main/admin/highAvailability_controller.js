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

  /**
   * enable High Availability
   * @return {Boolean}
   */
  enableHighAvailability: function () {
    var message = [];
    var hostComponents = App.HostComponent.find();
    //Prerequisite Checks
    if (this.get('securityEnabled')) {
      this.showErrorPopup(Em.I18n.t('admin.highAvailability.error.security'));
      return false;
    } else {
      if (hostComponents.findProperty('componentName', 'NAMENODE').get('workStatus') !== 'STARTED') {
        message.push(Em.I18n.t('admin.highAvailability.error.namenodeStarted'));
      }
      if (hostComponents.filterProperty('componentName', 'ZOOKEEPER_SERVER').length < 3) {
        message.push(Em.I18n.t('admin.highAvailability.error.zooKeeperNum'));
      }

      if (App.router.get('mainHostController.hostsCountMap.TOTAL') < 3) {
        message.push(Em.I18n.t('admin.highAvailability.error.hostsNum'));
      }
      if (message.length > 0) {
        this.showErrorPopup(message);
        return false;
      }
    }
    App.router.transitionTo('main.admin.enableHighAvailability');
    return true;
  },

  disableHighAvailability: function () {
    App.router.transitionTo('main.admin.rollbackHighAvailability');
  },

  /**
   * enable ResourceManager High Availability
   * @return {Boolean}
   */
  enableRMHighAvailability: function () {
    //Prerequisite Checks
    var message = [];
    if (App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').length < 3) {
      message.push(Em.I18n.t('admin.rm_highAvailability.error.zooKeeperNum'));
    }

    if (App.router.get('mainHostController.hostsCountMap.TOTAL') < 3) {
      message.push(Em.I18n.t('admin.rm_highAvailability.error.hostsNum'));
    }
    if (message.length > 0) {
      this.showErrorPopup(message);
      return false;
    }
    App.router.transitionTo('main.admin.enableRMHighAvailability');
    return true;
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
    if ('hadoop-env' in configs) {
      this.set('tag', configs['hadoop-env'].tag);
      this.getServiceConfigsFromServer();
    } else {
      this.showErrorPopup(Em.I18n.t('admin.security.status.error'));
    }
  },

  /**
   * get service configs from server and
   * indicate whether security is enabled
   */
  getServiceConfigsFromServer: function () {
    var self = this;
    var tags = [
      {
        siteName: "hadoop-env",
        tagName: this.get('tag')
      }
    ];
    App.router.get('configurationController').getConfigsByTags(tags).done(function (data) {
      self.set('securityEnabled', self.isSecurityEnabled(data.findProperty('tag', self.get('tag')).properties));
      self.set('dataIsLoaded', true);
    });
  },

  /**
   * identify whether security is enabled by security config
   */
  isSecurityEnabled: function(properties){
    return !!(properties && (properties['security_enabled'] === 'true' || properties['security_enabled'] === true));
  },
  /**
   * join or wrap message depending on whether it is array or string
   * @param message
   * @return {*}
   */
  joinMessage: function (message) {
    if (Array.isArray(message)) {
      return message.join('<br/>');
    } else {
      return '<p>' + message + '</p>';
    }
  },

  showErrorPopup: function (message) {
    message = this.joinMessage(message);
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(message)
      }),
      secondary: false
    });
  }
});
