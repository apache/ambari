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
  registrationAttempt: 7,
  isSubmitDisabled: true,
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
    if (this.get('content.hosts.manualInstall') !== true) {
      if (App.db.getBootStatus() === false) {
        this.startBootstrap();
      }
    } else {
      // TODO: assume manually bootstrapped hosts are all successful for now
      this.get('hosts').forEach(function (_host) {
        _host.set('bootStatus', 'DONE');
        _host.set('bootLog', 'Success');
      });
      this.set('bootHosts', this.get('hosts'));
      this.isHostsRegistered(this.getHostInfo);
    }
  },

  clearStep: function () {
    this.hosts.clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    this.clearStep();
    var hosts = this.loadHosts();
    // hosts.setEach('bootStatus', 'RUNNING');
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
   * Parses and updates the content based on bootstrap API response.
   * Returns true if polling should continue (some hosts are in "RUNNING" state); false otherwise
   */
  parseHostInfo: function (hostsStatusFromServer) {
    hostsStatusFromServer.forEach(function (_hostStatus) {
      var host = this.get('bootHosts').findProperty('name', _hostStatus.hostName);
      if (host !== null && host !== undefined) { // check if hostname extracted from REST API data matches any hostname in content
        host.set('bootStatus', _hostStatus.status);
        host.set('bootLog', _hostStatus.log);
      }
    }, this);
    // if the data rendered by REST API has hosts in "RUNNING" state, polling will continue
    return this.get('bootHosts').length != 0 && this.get('bootHosts').someProperty('bootStatus', 'RUNNING');
  },

  /* Returns the current set of visible hosts on view (All, Succeeded, Failed) */
  visibleHosts: function () {
    if (this.get('category') === 'Success') {
      return (this.hosts.filterProperty('bootStatus', 'DONE'));
    } else if (this.get('category') === 'Error') {
      return (this.hosts.filterProperty('bootStatus', 'FAILED'));
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

  numPolls: 0,

  startBootstrap: function () {
    //this.set('isSubmitDisabled', true);    //TODO: uncomment after actual hookup
    this.numPolls = 0;
    this.set('bootHosts', this.get('hosts'));
    this.doBootstrap();
  },

  doBootstrap: function () {
    this.numPolls++;
    var self = this;
    var url = App.testMode ? '/data/wizard/bootstrap/poll_' + this.numPolls + '.json' : App.apiPrefix + '/bootstrap/' + this.get('content.hosts.bootRequestId');
    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      success: function (data) {
        if (data.hostsStatus !== null) {
          // in case of bootstrapping just one host, the server returns an object rather than an array...
          if (!(data.hostsStatus instanceof Array)) {
            data.hostsStatus = [ data.hostsStatus ];
          }
          console.log("TRACE: In success function for the GET bootstrap call");
          var result = self.parseHostInfo(data.hostsStatus);
          if (result) {
            window.setTimeout(function () {
              self.doBootstrap()
            }, 3000);
            return;
          }
        }
        console.log('Bootstrap failed');
        self.stopBootstrap();
      },

      error: function () {
        console.log('Bootstrap failed');
        self.stopBootstrap();
      },

      statusCode: require('data/statusCodes')
    });

  },

  stopBootstrap: function () {
    //TODO: uncomment following line after the hook up with the API call
    console.log('stopBootstrap() called');
    // this.set('isSubmitDisabled',false);
    this.startRegistration();
  },

  startRegistration: function () {
    this.isHostsRegistered(this.getHostInfo);
  },

  isHostsRegistered: function (callback) {
    var self = this;
    var hosts = this.get('bootHosts');
    var url = App.testMode ? '/data/wizard/bootstrap/single_host_registration.json' : App.apiPrefix + '/hosts';
    var method = 'GET';
    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      success: function (data) {
        var jsonData;
        if (App.testMode) {
          jsonData = data;
        } else {
          jsonData = jQuery.parseJSON(data);
        }
        if (!jsonData) {
          console.log("Error: jsonData is null");
          return;
        }
        if (jsonData.items.length === 0) {
          if (self.get('registrationAttempt') !== 0) {
            count--;
            window.setTimeout(function () {
              self.isHostsRegistered(callback);
            }, 3000);
            return;
          } else {
            self.registerErrPopup(Em.I18n.t('installer.step3.hostRegister.popup.header'), Em.I18n.t('installer.step3.hostRegister.popup.body'));
            return;
          }
        }
        if (hosts.length === jsonData.items.length) {
          callback.apply(self);
        } else {
          self.registerErrPopup(Em.I18n.t('installer.step3.hostRegister.popup.header'), Em.I18n.t('installer.step3.hostRegister.popup.body'));
        }
      },
      error: function () {
        console.log('Error: Getting registered host information from the server');
        self.stopBootstrap();
      },
      statusCode: require('data/statusCodes')
    });
  },

  registerErrPopup: function (header, message) {
    App.ModalPopup.show({
      header: header,
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>{{view.message}}</p>'].join('\n')),
        message: message
      })
    });
  },

  /**
   * Get disk info and cpu count of booted hosts from server
   */


  getHostInfo: function () {
    var self = this;
    var kbPerGb = 1024;
    var hosts = this.get('bootHosts');
    var url = App.testMode ? '/data/wizard/bootstrap/single_host_information.json' : App.apiPrefix + '/hosts?fields=Hosts/total_mem,Hosts/cpu_count';
    var method = 'GET';
    $.ajax({
      type: 'GET',
      url: url,
      contentType: 'application/json',
      timeout: App.timeout,
      success: function (data) {
        var jsonData;
        if (App.testMode) {
          jsonData = data;
        } else {
          jsonData = jQuery.parseJSON(data);
        }
        hosts.forEach(function (_host) {
          if (jsonData.items.someProperty('Hosts.host_name', _host.name)) {
            var host = jsonData.items.findProperty('Hosts.host_name', _host.name);
            _host.cpu = host.Hosts.cpu_count;
            _host.memory = ((parseInt(host.Hosts.total_mem))).toFixed(2);
            console.log("The value of memory is: " + _host.memory);
          }
        }, this);
        self.set('bootHosts', hosts);
        console.log("The value of hosts: " + JSON.stringify(hosts));
        self.stopRegistrataion();
      },

      error: function () {
        console.log('INFO: Getting host information(cpu_count and total_mem) from the server failed');
        self.registerErrPopup(Em.I18n.t('installer.step3.hostInformation.popup.header'), Em.I18n.t('installer.step3.hostInformation.popup.body'));
      },
      statusCode: require('data/statusCodes')
    });
  },

  stopRegistrataion: function () {
    this.set('isSubmitDisabled', false);
  },


  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.set('content.hostsInfo', this.get('bootHosts'));
      App.router.send('next');
    }
  },

  hostLogPopup: function (event, context) {
    var host = event.context;

    App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.hostLog.popup.header').format(host.get('name')),
      secondary: null,

      onPrimary: function () {
        this.hide();
      },

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_host_log_popup'),
        host: host
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

