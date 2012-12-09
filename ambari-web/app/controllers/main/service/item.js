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

App.MainServiceItemController = Em.Controller.extend({
  name: 'mainServiceItemController',

  /**
   * Send specific command to server
   * @param url
   * @param data Object to send
   */
  sendCommandToServer : function(url, postData){
    var url =  (App.testMode) ?
      '/data/wizard/deploy/poll_1.json' : //content is the same as ours
      '/api/clusters/' + App.router.getClusterName() + url; //'/services/' + this.get('content.serviceName').toUpperCase();

    var method = App.testMode ? 'GET' : (postData ? 'PUT' : 'POST');

    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(postData),
      dataType: 'json',
      timeout: 5000,
      success: function (data) {
        //do something
      },

      error: function (request, ajaxOptions, error) {
        //do something
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * On click callback for <code>start service</code> button
   * @param event
   */
  startService: function (event) {
    if($(event.target).hasClass('disabled')){
      return;
    }

    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('workStatus', true);

        self.sendCommandToServer('/services/' + self.get('content.serviceName').toUpperCase(),{
          ServiceInfo:{
            state: 'STARTED'
          }
        });

        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  /**
   * On click callback for <code>stop service</code> button
   * @param event
   */
  stopService: function (event) {
    if($(event.target).hasClass('disabled')){
      return;
    }

    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('workStatus', false);

        self.sendCommandToServer('/services/' + self.get('content.serviceName').toUpperCase(),{
          ServiceInfo:{
            state: 'INSTALLED'
          }
        });

        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  runRebalancer: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('runRebalancer', true);
        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  runCompaction: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('runCompaction', true);
        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  runSmokeTest: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('runSmokeTest', true);
      
        var serviceName = self.get('content.serviceName').toUpperCase();
        var smokeName = serviceName + "_SERVICE_CHECK";
        self.sendCommandToServer('/services/' + serviceName + '/actions/' + smokeName);

        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  doAction: function (event) {
    var methodName = event.context;
    switch (methodName) {
      case 'runRebalancer':
        this.runRebalancer();
        break;
      case 'runCompaction':
        this.runCompaction();
        break;
      case 'runSmokeTest':
        this.runSmokeTest();
        break;
    }
  }
})