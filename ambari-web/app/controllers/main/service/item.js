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
  backgroundOperations: [],
  taskId: 0,
  intervalId: false,
  checkOperationsInterval: 5000,
  init: function(){
    this._super();
    this.startCheckOperationsLifeTime();
  },
  startCheckOperationsLifeTime: function () {
    this.intervalId = setInterval(this.checkOperationsLifeTime, this.get('checkOperationsInterval'));
  },
  stopCheckOperationsLifeTime:function () {
    if(this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.intervalId = false;
  },

  checkOperationsLifeTime: function () {
    var self = App.router.get('mainServiceItemController');
    var backgroundOperations = self.get('backgroundOperations');
    var time = new Date().getTime();
    if(backgroundOperations.length){
      backgroundOperations.forEach(function (operation) {
        if (time - operation.startTime >= 60*1000){
          backgroundOperations.removeObject(operation);
        }
      })
    }
  },
  createBackgroundOperation: function (role, command) {
    var newTaskId = this.get('taskId') + 1;
    this.set('taskId', newTaskId);
    var operation = Em.Object.create({
      taskId: newTaskId,
      stageId: null,
      serviceName: this.content.get('serviceName'),
      role: role,
      command: command,
      status: null,
      exitcode: 404,
      stderror: 27,
      stdout: 501,
      startTime: new Date().getTime(),
      attemptCount: null
    })

    return operation;
  },
  startService: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('workStatus', true);
        var newOperation = self.createBackgroundOperation('Service', 'Start');
        newOperation.detail = "Another detail info";
        self.addBackgroundOperation(newOperation);
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  stopService: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.confirmation.header'),
      body: Em.I18n.t('services.service.confirmation.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.content.set('workStatus', false);
        var newOperation = self.createBackgroundOperation('Service', 'Stop');
        newOperation.detail = "Another detail info";
        self.addBackgroundOperation(newOperation);
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
        var newOperation = self.createBackgroundOperation('Service', 'Run Rebalancer');
        newOperation.detail = "Some detail info";
        self.addBackgroundOperation(newOperation);
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
        var newOperation = self.createBackgroundOperation('Service', 'Run Compaction');
        self.addBackgroundOperation(newOperation);
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
        var newOperation = self.createBackgroundOperation('Service', 'Run Smoke Test');
        self.addBackgroundOperation(newOperation);
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
  },
  serviceOperations: function(){
    var serviceName = this.get('content.serviceName');
    return this.get('backgroundOperations').filterProperty('serviceName', serviceName);
  }.property('backgroundOperations.length', 'content'),
  serviceOperationsCount: function() {
    return this.get('serviceOperations.length');
  }.property('serviceOperations'),
  showBackgroundOperationsPopup: function(){
    console.log(this.get('backgroundOperations'));
    App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controllerBinding: 'App.router.mainServiceItemController',
        template:Ember.Handlebars.compile('{{serviceOperationsCount}} Background Operations Running')
      }),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.mainServiceItemController',
        templateName: require('templates/main/service/background_operations_popup')
      }),
      onPrimary: function() {
        this.hide();
      }
    });
  },
  addBackgroundOperation: function (operation) {
    var backgroundOperations = this.get('backgroundOperations');
    backgroundOperations.pushObject(operation);
    this.showBackgroundOperationsPopup();
  }
})