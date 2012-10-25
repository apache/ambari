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

App.MainHostDetailsController = Em.Controller.extend({
  name: 'mainHostDetailsController',
  content: null,
  isFromHosts: false,
  isStarting: true,
  isStopping: function(){
    return !this.get('isStarting');
  }.property('isStarting'),

  setBack: function(isFromHosts){
    this.set('isFromHosts', isFromHosts);
  },

  startComponent: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('workStatus', true);
        var stopped = self.get('content.components').filterProperty('workStatus', false);
        if (stopped.length == 0)
          self.set('isStarting', true);
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  stopComponent: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('workStatus', false);
        var started = self.get('content.components').filterProperty('workStatus', true);
        if (started.length == 0)
          self.set('isStarting', false);
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });

  },

  startConfirmPopup: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.get('content.components').setEach('workStatus', true);
        self.set('isStarting', !self.get('isStarting'));
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  stopConfirmPopup: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.stop.popup.header'),
      body: Em.I18n.t('hosts.host.stop.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.get('content.components').setEach('workStatus', false);
        self.set('isStarting', !self.get('isStarting'));
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  deleteButtonPopup: function(event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.header'),
      body: Em.I18n.t('hosts.delete.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.removeHost(event);
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  removeHost: function () {
    var clientId = this.get('content.clientId');
    var host_ids = this.get('content.store.clientIdToId');
    var host_id = host_ids[clientId];
    App.router.get('mainHostController').checkRemoved(host_id);
    App.router.transitionTo('hosts');

  }
})