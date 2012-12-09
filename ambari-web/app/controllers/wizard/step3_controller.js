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
  maxRegistrationAttempts: 20,
  registrationAttempts: null,
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
      this.set('bootHosts', this.get('hosts'));
      if (App.testMode && App.skipBootstrap) {
        this.get('bootHosts').setEach('bootStatus', 'REGISTERED');
        this.get('bootHosts').setEach('cpu', '2');
        this.get('bootHosts').setEach('memory', '2000000');
        this.getHostInfo();
      } else {
        this.isHostsRegistered();
      }
    }
  },

  clearStep: function () {
    this.hosts.clear();
    this.bootHosts.clear();
    this.set('isSubmitDisabled', true);
    this.set('registrationAttempts', 1);
  },

  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    if(!this.get('hosts').length){
      this.clearStep();
      var hosts = this.loadHosts();
      // hosts.setEach('bootStatus', 'RUNNING');
      this.renderHosts(hosts);
    } else {
      this.set('isSubmitDisabled', false);
    }
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
      return (this.hosts.filterProperty('bootStatus', 'REGISTERED'));
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
        self.set('bootHosts', hosts);
        if (self.get('content.hosts.manualInstall') !== true) {
          self.doBootstrap();
        } else {
          self.isHostsRegistered();
        }
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
    Ember.run.later(this, function(){
      this.startRegistration();
    }, 1000);
  },

  startRegistration: function () {
    this.isHostsRegistered();
  },

  isHostsRegistered: function () {
    var self = this;
    var hosts = this.get('bootHosts');
    var url = App.testMode ? '/data/wizard/bootstrap/single_host_registration.json' : App.apiPrefix + '/hosts';
    var method = 'GET';
    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      success: function (data) {
        console.log('registration attempt #' + self.get('registrationAttempts'));
        var jsonData = App.testMode ? data : jQuery.parseJSON(data);
        if (!jsonData) {
          console.log("Error: jsonData is null");
          return;
        }

        // keep polling until all hosts are registered
        var allRegistered = true;
        hosts.forEach(function (_host, index) {
          // Change name of first host for test mode.
          if (App.testMode === true) {
            if (index == 0) {
              _host.set('name', 'localhost.localdomain');
            }
          }
          if (jsonData.items.someProperty('Hosts.host_name', _host.name)) {
            if (_host.get('bootStatus') != 'REGISTERED') {
              _host.set('bootStatus', 'REGISTERED');
              _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistration with the server succeeded.');
            }
          } else if (_host.get('bootStatus') == 'FAILED') {
            // ignore FAILED hosts
          } else {
            // there are some hosts that are not REGISTERED or FAILED
            // we need to keep polling
            allRegistered = false;
            if (_host.get('bootStatus') != 'REGISTERING') {
              _host.set('bootStatus', 'REGISTERING');
              currentBootLog = _host.get('bootLog') != null ? _host.get('bootLog') : '';
              _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistering with the server...');
            }
          }
        }, this);
        if (allRegistered) {
          self.getHostInfo();
        } else if (self.get('maxRegistrationAttempts') - self.get('registrationAttempts') >= 0) {
          self.set('registrationAttempts', self.get('registrationAttempts') + 1);
          window.setTimeout(function () {
            self.isHostsRegistered();
          }, 3000);
        } else {
          // maxed out on registration attempts.  mark all REGISTERING hosts to FAILED
          hosts.filterProperty('bootStatus', 'REGISTERING').forEach(function (_host) {
            _host.set('bootStatus', 'FAILED');
            _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistration with the server failed.');
          });
          self.getHostInfo();
        }
      },
      error: function () {
        console.log('Error: Getting registered host information from the server');
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
        var jsonData = App.testMode ? data : jQuery.parseJSON(data);
        hosts.forEach(function (_host) {
          var host = jsonData.items.findProperty('Hosts.host_name', _host.name);
          if (host) {
            _host.cpu = host.Hosts.cpu_count;
            _host.memory = ((parseInt(host.Hosts.total_mem))).toFixed(2);
            console.log("The value of memory is: " + _host.memory);
          }
        });
        self.set('bootHosts', hosts);
        console.log("The value of hosts: " + JSON.stringify(hosts));
        self.stopRegistration();
      },

      error: function () {
        console.log('INFO: Getting host information(cpu_count and total_mem) from the server failed');
        self.registerErrPopup(Em.I18n.t('installer.step3.hostInformation.popup.header'), Em.I18n.t('installer.step3.hostInformation.popup.body'));
      },
      statusCode: require('data/statusCodes')
    });
  },

  stopRegistration: function () {
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
        host: host,
        didInsertElement: function () {
          var self = this;
          var button = $(this.get('element')).find('.textTrigger');
          button.click(function () {
            if(self.get('isTextArea')){
              $(this).text('click to highlight');
            } else {
              $(this).text('press CTRL+C');
            }
            self.set('isTextArea', !self.get('isTextArea'));
          });
          $(this.get('element')).find('.content-area').mouseenter(
            function () {
              var element = $(this);
              element.css('border', '1px solid #dcdcdc');
              button.css('visibility', 'visible');
            }).mouseleave(
            function () {
              var element = $(this);
              element.css('border', 'none');
              button.css('visibility', 'hidden');
            })
        },
        isTextArea: false,
        textArea: Em.TextArea.extend({
          didInsertElement: function(){
            var element = $(this.get('element'));
            element.width($(this.get('parentView').get('element')).width() - 10);
            element.height($(this.get('parentView').get('element')).height());
            element.select();
            element.css('resize', 'none');
          },
          readOnly: true,
          value: function(){
            return this.get('content');
          }.property('content')
        })
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

