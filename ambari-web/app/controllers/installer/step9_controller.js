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

App.InstallerStep9Controller = Em.ArrayController.extend({
  name: 'installerStep9Controller',
  content: [],
  progress: '0',
  // result: 'pending', // val = pending or success or failed
  isStepCompleted: false,
  isSubmitDisabled: function () {
    return !this.get('isStepCompleted');
  }.property('isStepCompleted'),

  // status: 'info',
  mockHostData: require('data/mock/step9_hosts'),
  pollData_1: require('data/mock/step9_pollData_1'),
  pollData_2: require('data/mock/step9_pollData_2'),
  pollDataCounter: 0,

  status: function () {
    if (this.everyProperty('status', 'success')) {
      return 'success';
    } else if (this.someProperty('status', 'failed')) {
      return 'failed';
    } else if (this.someProperty('status', 'warning')) {
      return 'warning';
    } else {
      return 'info';
    }
  }.property('@each.status'),

  navigateStep: function () {
    if (App.router.get('isFwdNavigation') === true && !App.router.get('backBtnForHigherStep')) {
      this.loadStep(true);
      //TODO: uncomment following line after the hook up with the API call
      //this.startPolling();
    } else {
      this.loadStep(false);
    }
    App.router.set('backBtnForHigherStep', false);
  },

  clearStep: function () {
    this.clear();
    this.set('status', 'info');
    this.set('progress', '0');
    this.set('isStepCompleted', false);
  },

  loadStep: function (restart) {
    console.log("TRACE: Loading step9: Install, Start and Test");
    this.clearStep();
    this.renderHosts(this.loadHosts(restart));
  },

  loadHosts: function (restart) {
    var hostInfo = [];
    hostInfo = App.db.getHosts();
    var hosts = new Ember.Set();
    for (var index in hostInfo) {
      if (restart === true) {
        //this.setInitialHostCondn(hostInfo[index]);
        hostInfo[index].status = "pending";
        hostInfo[index].message = 'Information';
        hostInfo[index].progress = '0';
      } else {
        this.set('isStepCompleted', true);
        this.set('progress', '100');
      }
      hosts.add(hostInfo[index]);
      console.log("TRACE: host name is: " + hostInfo[index].name);
    }
    return hosts.filterProperty('bootStatus', 'success');
  },

  renderHosts: function (hostsInfo) {
    var self = this;
    hostsInfo.forEach(function (_hostInfo) {
      var hostInfo = App.HostInfo.create({
        name: _hostInfo.name,
        status: _hostInfo.status,
        message: _hostInfo.message,
        progress: _hostInfo.progress
      });

      console.log('pushing ' + hostInfo.name);
      self.content.pushObject(hostInfo);
    });
  },

  onSuccessPerHost: function (actions, contentHost) {
    if (actions.everyProperty('status', 'completed')) {
      contentHost.set('status', 'success');
    }
  },

  onWarningPerHost: function (actions, contentHost) {
    if (actions.findProperty('status', 'failed') || actions.findProperty('status', 'aborted')) {
      contentHost.set('status', 'warning');
      this.set('status', 'warning');
    }
  },

  onInProgressPerHost: function (actions, contentHost) {
    var runningAction = actions.findProperty('status', 'inprogress');
    if (runningAction !== null && runningAction !== undefined) {
      contentHost.set('message', runningAction.message);
    }
  },

  progressPerHost: function (actions, contentHost) {
    var totalProgress = 0;
    var actionsPerHost = actions.length;
    var completedActions = actions.filterProperty('status', 'completed').length
      + actions.filterProperty('status', 'failed').length +
      actions.filterProperty('status', 'aborted').length;
    var progress = (completedActions / actionsPerHost) * 100;
    console.log('INFO: progressPerHost is: ' + progress);
    contentHost.set('progress', progress.toString());
    return progress;
  },

  isSuccess: function (polledData) {
    return polledData.everyProperty('status', 'success');
  },

  isStepFailed: function (polledData) {
    var self = this;
    var result = false;
    polledData.forEach(function (_polledData) {
      var successFactor = _polledData.sf;
      var actionsPerRole = polledData.filterProperty('role', _polledData.role);
      var actionsFailed = actionsPerRole.filterProperty('status', 'failed');
      var actionsAborted = actionsPerRole.filterProperty('status', 'aborted');
      if ((((actionsFailed.length + actionsAborted.length) / actionsPerRole.length) * 100) <= successFactor) {
        console.log('TRACE: Entering success factor and result is failed');
        result = true;
      }
    });
    return result;
  },

  getFailedHostsForFailedRoles: function (polledData) {
    var hostArr = new Ember.Set();
    polledData.forEach(function (_polledData) {
      var successFactor = _polledData.sf;
      var actionsPerRole = polledData.filterProperty('role', _polledData.role);
      var actionsFailed = actionsPerRole.filterProperty('status', 'failed');
      var actionsAborted = actionsPerRole.filterProperty('status', 'aborted');
      if ((((actionsFailed.length + actionsAborted.length) / actionsPerRole.length) * 100) <= successFactor) {
        actionsFailed.forEach(function (_actionFailed) {
          hostArr.add(_actionFailed.name);
        });
        actionsAborted.forEach(function (_actionFailed) {
          hostArr.add(_actionFailed.name);
        });
      }
    });
    return hostArr;
  },

  setHostsStatus: function (hosts, status) {
    var self = this;
    hosts.forEach(function (_host) {
      var host = self.findProperty('name', _host);
      host.set('status', status);
    });
  },

  // polling from ui stops only when no action has 'pending', 'queued' or 'inprogress' status

  finishStep: function (polledData) {
    var self = this;
    if (!polledData.someProperty('status', 'pending') && !polledData.someProperty('status', 'queued') && !polledData.someProperty('status', 'inprogress')) {
      this.set('progress', '100');
      if (this.isSuccess(polledData)) {
        this.set('status', 'success');
      } else {
        if (this.isStepFailed(polledData)) {
          self.set('status', 'failed');
          this.setHostsStatus(this.getFailedHostsForFailedRoles(polledData), 'failed');
        }
      }
      this.set('isStepCompleted', true);
    }
  },


  parseHostInfo: function (polledData) {
    console.log('TRACE: Entering host info function');
    var self = this;
    var result = false;
    var totalProgress = 0;
    this.forEach(function (_content) {
      var actions = polledData.filterProperty('name', _content.name);
      if (actions.length === 0) {
        alert('For testing with mockData follow the sequence: hit referesh,"mockData btn", "pollData btn", again "pollData btn"');
        //exit();
      }
      if (actions !== null && actions !== undefined && actions.length !== 0) {
        this.onSuccessPerHost(actions, _content);    // every action should be a success
        this.onWarningPerHost(actions, _content);    // any action should be a faliure
        this.onInProgressPerHost(actions, _content); // current running action for a host
        totalProgress = totalProgress + self.progressPerHost(actions, _content);
      }
    }, this);
    totalProgress = totalProgress / this.content.length;
    this.set('progress', totalProgress.toString());
    console.log("INFO: right now the progress is: " + this.get('progress'));
    this.finishStep(polledData);
    return this.get('isStepCompleted');
  },


  retry: function () {
    if (this.get('isSubmitDisabled')) {
      return;
    }
    this.clear();
    this.renderHosts(this.loadHosts());
    //this.startPolling();
  },

  startPolling: function () {
    this.set('isSubmitDisabled', true);
    this.doPolling();
  },

  doPolling: function () {
    var self = this;
    $.ajax({
      type: 'GET',
      url: '/ambari_server/api/polling',
      async: false,
      timeout: 5000,
      success: function (data) {
        console.log("TRACE: In success function for the GET bootstrap call");
        var result = self.parseHostInfo(data);
        if (result !== true) {
          window.setTimeout(self.doPolling, 3000);
        } else {
          self.stopPolling();
        }
      },

      error: function () {
        console.log("ERROR");
        self.stopPolling();
      },

      statusCode: {
        404: function () {
          console.log("URI not found.");
        }
      },

      dataType: 'application/json'
    });

  },

  stopPolling: function () {
    //TODO: uncomment following line after the hook up with the API call
    // this.set('isStepCompleted',true);
  },

  saveHostInfoToDb: function () {
    var hostInfo = App.db.getHosts();
    for (var index in hostInfo) {
      hostInfo[index].status = "pending";
      if (this.someProperty('name', hostInfo[index].name)) {
        var host = this.findProperty('name', hostInfo[index].name);
        hostInfo[index].status = host.status;
        hostInfo[index].message = host.message;
        hostInfo[index].progress = host.progress;
      }
      console.log("TRACE: host name is: " + hostInfo[index].name);
    }
    App.db.setHosts(hostInfo);
  },


  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.saveHostInfoToDb();
      App.get('router').transitionTo('step10');
    }
  },

  back: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('back');
    }
  },

  hostLogPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hostLog.popup.header'),
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/installer/step3HostLogPopup')
      })
    });
  },
  mockBtn: function () {
    this.set('isSubmitDisabled', false);
    this.clear();
    var hostInfo = this.mockHostData;
    this.renderHosts(hostInfo);

  },
  pollBtn: function () {
    this.set('isSubmitDisabled', false);
    var data1 = this.pollData_1;
    var data2 = this.pollData_2;
    if ((this.get('pollDataCounter') / 2) === 0) {
      console.log("TRACE: In pollBtn function data1");
      var counter = parseInt(this.get('pollDataCounter')) + 1;
      this.set('pollDataCounter', counter.toString());
      this.parseHostInfo(data1);
    } else {
      console.log("TRACE: In pollBtn function data2");
      var counter = parseInt(this.get('pollDataCounter')) + 1;
      this.set('pollDataCounter', counter.toString());
      this.parseHostInfo(data2);
    }

  }

});