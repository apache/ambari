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

App.WizardStep3Controller = Em.Controller.extend({
  name: 'wizardStep3Controller',
  hosts: [],
  content: [],
  bootHosts: [],
  isSubmitDisabled: false,
  categories: ['All Hosts', 'Success', 'Error'],
  category: 'All Hosts',
  allChecked: false,

  onAllChecked: function () {
    var hosts = this.get('visibleHosts');
    hosts.setEach('isChecked', this.get('allChecked'));
  }.observes('allChecked'),

  noHostsSelected: function () {
    return !(this.hosts.someProperty('isChecked', true));
  }.property('hosts.@each.isChecked'),

  mockData: require('data/mock/step3_hosts'),
  mockRetryData: require('data/mock/step3_pollData'),

  navigateStep: function () {
    this.loadStep();
    if (App.db.getBootStatus() === false) {
      this.startBootstrap();
    }
  },

  clearStep: function () {
    this.hosts.clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    this.clearStep();
    var hosts = this.loadHosts();
    // hosts.setEach('bootStatus', 'pending');
    this.renderHosts(hosts);
  },

  /* Loads the hostinfo from localStorage on the insertion of view. It's being called from view */
  loadHosts: function () {
    var hostInfo = [];
    hostInfo = this.get('content.hostsInfo');
    var hosts = new Ember.Set();
    for (var index in hostInfo) {
      hosts.add(hostInfo[index]);
      console.log("TRACE: host name is: " + hostInfo[index].name);
    }

    return hosts;
  },

  /* Renders the set of passed hosts */
  renderHosts: function (hostsInfo) {
    var self = this;
    hostsInfo.forEach(function (_hostInfo) {
      var hostInfo = App.HostInfo.create({
        name: _hostInfo.name,
        bootStatus: _hostInfo.bootStatus,
        isChecked: false
      });

      console.log('pushing ' + hostInfo.name);
      self.hosts.pushObject(hostInfo);
    });
  },

  /**
   * Parses and updates the content, and governs the possibility
   * of the next doBootstrap (polling) call.
   * Returns true if polling should stop (no hosts are in "pending" state); false otherwise
   */
  parseHostInfo: function (hostsFromServer, hostsFromContent) {
    var result = true;  // default value as true implies
    hostsFromServer.forEach(function (_hostFromServer) {
      var host = hostsFromContent.findProperty('name', _hostFromServer.name);
      if (host !== null && host !== undefined) { // check if hostname extracted from REST API data matches any hostname in content
        host.set('bootStatus', _hostFromServer.status);
        host.set('cpu', _hostFromServer.cpu);
        host.set('memory', _hostFromServer.memory);
      }
    });
    // if the data rendered by REST API has no hosts or no hosts are in "pending" state, polling will stop
    return this.hosts.length == 0 || !this.hosts.someProperty('bootStatus', 'pending');
  },

  /* Returns the current set of visible hosts on view (All, Succeeded, Failed) */
  visibleHosts: function () {
    if (this.get('category') === 'Success') {
      return (this.hosts.filterProperty('bootStatus', 'success'));
    } else if (this.get('category') === 'Error') {
      return (this.hosts.filterProperty('bootStatus', 'error'));
    } else { // if (this.get('category') === 'All Hosts')
      return this.hosts;
    }
  }.property('category', 'hosts.@each.bootStatus'),

  removeHosts: function (hosts) {
    var self = this;

    App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hosts.remove.popup.header'),
      onPrimary: function () {
        App.router.send('removeHosts', hosts);
        self.hosts.removeObjects(hosts);
        this.hide();
      },
      body: Em.I18n.t('installer.step3.hosts.remove.popup.body')
    });

  },

  /* Removes a single element on the trash icon click. Called from View */
  removeHost: function (hostInfo) {
    this.removeHosts([hostInfo]);
  },

  removeSelectedHosts: function () {
    if (!this.get('noHostsSelected')) {
      var selectedHosts = this.get('visibleHosts').filterProperty('isChecked', true);
      selectedHosts.forEach(function (_hostInfo) {
        console.log('Removing:  ' + _hostInfo.name);
      });
      this.removeHosts(selectedHosts);
    }
  },

  retryHosts: function (hosts) {
    var self = this;

    App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hosts.retry.popup.header'),
      onPrimary: function () {
        hosts.forEach(function (_host) {
          console.log('Retrying:  ' + _host.name);
        });

        //TODO: uncomment below code to hookup with @GET bootstrap API
        /*
         self.set('bootHosts',selectedHosts);
         self.doBootstrap();
         */
        this.hide();
      },
      body: Em.I18n.t('installer.step3.hosts.retry.popup.body')
    });
  },

  retryHost: function (hostInfo) {
    this.retryHosts([hostInfo]);
  },

  retrySelectedHosts: function () {
    if (!this.get('noHostsSelected')) {
      var selectedHosts = this.get('visibleHosts').filterProperty('isChecked', true);
      this.retryHosts(selectedHosts);
    }
  },

  startBootstrap: function () {
    //this.set('isSubmitDisabled', true);    //TODO: uncomment after actual hookup
    this.set('bootHosts', this.get('hosts'));
    this.doBootstrap();
  },

  doBootstrap: function () {
    var self = this;
    var url = '/api/bootstrap';
    $.ajax({
      type: 'GET',
      url: url,
      timeout: 5000,
      success: function (data) {
        console.log("TRACE: In success function for the GET bootstrap call");
        var result = self.parseHostInfo(data, this.get('bootHosts'));
        window.setTimeout(self.doBootstrap, 3000);
      },

      error: function () {
        console.log("ERROR");
        self.stopBootstrap();
      },

      statusCode: require('data/statusCodes')
    });

  },

  stopBootstrap: function () {
    //TODO: uncomment following line after the hook up with the API call
    // this.set('isSubmitDisabled',false);
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.set('content.hostsInfo', this.get('hosts'));
      App.router.send('next');
    }
  },

  hostLogPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hostLog.popup.header'),
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3HostLogPopup')
      })
    });
  },

  // TODO: dummy button. Remove this after the hook up with actual REST API.
  mockBtn: function () {
    this.set('isSubmitDisabled', false);
    this.hosts.clear();
    var hostInfo = this.mockData;
    this.renderHosts(hostInfo);
  },

  pollBtn: function () {
    if (this.get('isSubmitDisabled')) {
      return;
    }
    var hosts = this.get('visibleHosts');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_host) {
      console.log('Retrying:  ' + _host.name);
    });

    var mockHosts = this.mockRetryData;
    mockHosts.forEach(function (_host) {
      console.log('Retrying:  ' + _host.name);
    });
    if (this.parseHostInfo(mockHosts, selectedHosts)) {
      // this.saveHostInfoToDb();
    }
  }

});

