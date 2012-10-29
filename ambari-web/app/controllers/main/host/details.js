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
  backgroundOperations: [],
  isStarting: true,
  isStopping: function(){
    return !this.get('isStarting');
  }.property('isStarting'),
  intervalId: false,
  checkOperationsInterval: 5000,
  init: function(){
    this._super();
    this.startCheckOperationsLifeTime();
  },

  setBack: function(isFromHosts){
    this.set('isFromHosts', isFromHosts);
  },

  startCheckOperationsLifeTime: function() {
    this.intervalId = setInterval(this.checkOperationsLifeTime, this.get('checkOperationsInterval'));
  },
  stopCheckOperationsLifeTime:function () {
    if(this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.intervalId = false;
  },

  checkOperationsLifeTime: function(){
    var self = App.router.get('mainHostDetailsController');
    var backgroundOperations = self.get('backgroundOperations');
    var time = new Date().getTime();
    if(backgroundOperations.length){
      backgroundOperations.forEach(function(operation){
        if (time - operation.time >= 60*1000){
          backgroundOperations.removeObject(operation);
        }
      })
    }
  },

  hostOperations: function(){
    var hostName = this.get('content.hostName');
    return this.get('backgroundOperations').filterProperty('hostName', hostName);
  }.property('backgroundOperations.length', 'content'),

  hostOperationsCount: function() {
    return this.get('hostOperations.length');
  }.property('backgroundOperations.length', 'content'),

  showBackgroundOperationsPopup: function(){
    App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controllerBinding: 'App.router.mainHostDetailsController',
        template:Ember.Handlebars.compile('{{hostOperationsCount}} Background Operations Running')
      }),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.mainHostDetailsController',
        templateName: require('templates/main/host/background_operations_popup')
      }),
      onPrimary: function() {
        this.hide();
      }
    });
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
        var backgroundOperations = self.get('backgroundOperations');
        backgroundOperations.pushObject({
          "hostName": self.get('content.hostName'),
          "role":component.get('componentName'),
          "command": "START",
          "time": new Date().getTime(),
          "details": [
            {"startTime":"4 min ago", "name":"Some intermediate operation"},
            {"startTime":"5 min ago", "name":"Component started"}
          ],
          "logs":{"exitcode":"404", "stdout":27, "stderror":501}
        });
        self.showBackgroundOperationsPopup();
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
        var backgroundOperations = self.get('backgroundOperations');
        backgroundOperations.pushObject({
          "hostName": self.get('content.hostName'),
          "role": component.get('componentName'),
          "command": "STOP",
          "time": new Date().getTime(),
          "details": [
            {"startTime":"4 min ago", "name":"Some intermediate operation"},
            {"startTime":"5 min ago", "name":"Component stopped"}
          ],
          "logs":{"exitcode":"404", "stdout":15, "stderror":501}
        });
        self.showBackgroundOperationsPopup();
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

  decommission: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('decommissioned', true);
        var backgroundOperations = self.get('backgroundOperations');
        backgroundOperations.pushObject({
          "hostName": self.get('content.hostName'),
          "role":component.get('componentName'),
          "command": "DECOMMISSION",
          "time": new Date().getTime(),
          "details": [
            {"startTime":"4 min ago", "name":"Some intermediate operation"},
            {"startTime":"5 min ago", "name":"Component decommissioned"}
          ],
          "logs":{"exitcode":"404", "stdout":27, "stderror":501}
        });
        self.showBackgroundOperationsPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  recommission: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('decommissioned', false);
        var backgroundOperations = self.get('backgroundOperations');
        backgroundOperations.pushObject({
          "hostName": self.get('content.hostName'),
          "role":component.get('componentName'),
          "command": "RECOMMISSION",
          "time": new Date().getTime(),
          "details": [
            {"startTime":"4 min ago", "name":"Some intermediate operation"},
            {"startTime":"5 min ago", "name":"Component recommissioned"}
          ],
          "logs":{"exitcode":"404", "stdout":27, "stderror":501}
        });
        self.showBackgroundOperationsPopup();
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

  validateDeletion: function() {
    var slaveComponents = ['DataNode', 'TaskTracker', 'RegionServer'];
    var masterComponents = [];
    var workingComponents = [];

    var components = this.get('content.components');
    components.forEach(function(cInstance){
      var cName = cInstance.get('componentName');
      if(slaveComponents.contains(cName)) {
        if(cInstance.get('workStatus') &&
          !cInstance.get('decommissioned')){
          workingComponents.push(cName);
        }
      } else {
        masterComponents.push(cName);
      }
    });
    //debugger;
    if(workingComponents.length || masterComponents.length) {
      this.raiseWarning(workingComponents, masterComponents);
    } else {
      this.deleteButtonPopup();
    }
  },

  raiseWarning: function (workingComponents, masterComponents) {
    var self = this;
    var masterString = '';
    var workingString = '';
    if(masterComponents && masterComponents.length) {
      var masterList = masterComponents.join(', ');
      var ml_text = Em.I18n.t('hosts.cant.do.popup.masterList.body');
      masterString = ml_text.format(masterList);
    }
    if(workingComponents && workingComponents.length) {
      var workingList = workingComponents.join(', ');
      var wl_text = Em.I18n.t('hosts.cant.do.popup.workingList.body');
      workingString = wl_text.format(workingList);
    }
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.cant.do.popup.header'),
      html: true,
      body: masterString + workingString,
      primary: "OK",
      secondary: null,
      onPrimary: function() {
        this.hide();
      }
    })
  },

  deleteButtonPopup: function() {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.header'),
      body: Em.I18n.t('hosts.delete.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.removeHost();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  removeHost: function () {
    App.router.get('mainHostController').checkRemoved(this.get('content.id'));
    App.router.transitionTo('hosts');
  }

})