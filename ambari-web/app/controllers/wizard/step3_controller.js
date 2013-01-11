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
  registrationStartedAt: null,
  registrationTimeoutSecs: 120,
  stopBootstrap: false,
  isSubmitDisabled: true,
  categoryObject: Em.Object.extend({
    hostsCount: function () {
      var category = this;
      var hosts = this.get('controller.hosts').filterProperty('bootStatus', category.get('hostsBootStatus'));
      return hosts.get('length');
    }.property('controller.hosts.@each.bootStatus'), // 'hosts.@each.bootStatus'
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount')
  }),
  getCategory: function(field, value){
    return this.get('categories').find(function(item){
      return item.get(field) == value;
    });
  },
  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      controller: self,
      isActive: function(){
        return this.get('controller.category') == this;
      }.property('controller.category'),
      itemClass: function(){
        return this.get('isActive') ? 'active' : '';
      }.property('isActive')
    });

    var categories = [
      self.categoryObject.create({value: 'All', hostsCount: function () {
        return this.get('controller.hosts.length');
      }.property('controller.hosts.length') }),
      self.categoryObject.create({value: 'Installing', hostsBootStatus: 'RUNNING'}),
      self.categoryObject.create({value: 'Registering', hostsBootStatus: 'REGISTERING'}),
      self.categoryObject.create({value: 'Success', hostsBootStatus: 'REGISTERED' }),
      self.categoryObject.create({value: 'Fail', hostsBootStatus: 'FAILED', last: true })
    ];

    this.set('category', categories.get('firstObject'));

    return categories;
  }.property(),
  category: false,
  allChecked: false,

  onAllChecked: function () {
    var hosts = this.get('visibleHosts');
    hosts.setEach('isChecked', this.get('allChecked'));
  }.observes('allChecked'),

  noHostsSelected: function () {
    return !(this.hosts.someProperty('isChecked', true));
  }.property('hosts.@each.isChecked'),

  isRetryDisabled: true,

  mockData: require('data/mock/step3_hosts'),
  mockRetryData: require('data/mock/step3_pollData'),

  navigateStep: function () {
    this.loadStep();
    if (this.get('content.installOptions.manualInstall') !== true) {
      if (!App.db.getBootStatus()) {
        this.startBootstrap();
      }
    } else {
      this.set('bootHosts', this.get('hosts'));
      if (App.testMode) {
        this.get('bootHosts').setEach('bootStatus', 'REGISTERED');
        this.get('bootHosts').setEach('cpu', '2');
        this.get('bootHosts').setEach('memory', '2000000');
        this.getHostInfo();
      } else {
        this.set('registrationStartedAt', null);
        this.get('bootHosts').setEach('bootStatus', 'DONE');
        this.startRegistration();
      }
    }
  },

  clearStep: function () {
    this.set('stopBootstrap', false);
    this.hosts.clear();
    this.bootHosts.clear();
    App.db.setBootStatus(false);
    this.set('isSubmitDisabled', true);
    this.set('isRetryDisabled', true);
  },

  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    this.set('registrationStartedAt', null);

    this.clearStep();
    var hosts = this.loadHosts();
    // hosts.setEach('bootStatus', 'RUNNING');
    this.renderHosts(hosts);
  },

  /* Loads the hostinfo from localStorage on the insertion of view. It's being called from view */
  loadHosts: function () {
    var hostInfo = this.get('content.hosts');
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
      // check if hostname extracted from REST API data matches any hostname in content
      // also, make sure that bootStatus modified by isHostsRegistered call does not get overwritten
      // since these calls are being made in parallel
      if (host && !['REGISTERED', 'REGISTERING'].contains(host.get('bootStatus'))) {
        host.set('bootStatus', _hostStatus.status);
        host.set('bootLog', _hostStatus.log);
      }
    }, this);
    // if the data rendered by REST API has hosts in "RUNNING" state, polling will continue
    return this.get('bootHosts').length != 0 && this.get('bootHosts').someProperty('bootStatus', 'RUNNING');
  },

  /* Returns the current set of visible hosts on view (All, Succeeded, Failed) */
  visibleHosts: function () {
    var self = this;
    if (this.get('category.hostsBootStatus')) {
      return this.hosts.filterProperty('bootStatus', self.get('category.hostsBootStatus'));
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
        if (!self.hosts.length) {
          self.set('isSubmitDisabled', true);
        }
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

  retryHost: function (hostInfo) {
    this.retryHosts([hostInfo]);
  },

  retryHosts: function (hosts) {
    var bootStrapData = JSON.stringify({'verbose': true, 'sshKey': this.get('content.installOptions.sshKey'), hosts: hosts.mapProperty('name')});
    this.numPolls = 0;
    if (this.get('content.installOptions.manualInstall') !== true) {
      var requestId = App.router.get('installerController').launchBootstrap(bootStrapData);
      this.set('content.installOptions.bootRequestId', requestId);
      this.set('registrationStartedAt', null);
      this.doBootstrap();
    } else {
      this.set('registrationStartedAt', null);
      this.get('bootHosts').setEach('bootStatus', 'DONE');
      this.startRegistration();
    }
  },

  retrySelectedHosts: function () {
    if (!this.get('isRetryDisabled')) {
      this.set('isRetryDisabled', true);
      var selectedHosts = this.get('bootHosts').filterProperty('bootStatus', 'FAILED');
      selectedHosts.forEach(function (_host) {
        _host.set('bootStatus', 'RUNNING');
        _host.set('bootLog', 'Retrying ...');
      }, this);
      this.retryHosts(selectedHosts);
    }
  },

  numPolls: 0,

  startBootstrap: function () {
    //this.set('isSubmitDisabled', true);    //TODO: uncomment after actual hookup
    this.numPolls = 0;
    this.set('registrationStartedAt', null);
    this.set('bootHosts', this.get('hosts'));
    this.get('bootHosts').setEach('bootStatus', 'PENDING');
    this.doBootstrap();
  },

  doBootstrap: function () {
    if (this.get('stopBootstrap')) {
      return;
    }
    this.numPolls++;
    var self = this;
    var url = App.testMode ? '/data/wizard/bootstrap/poll_' + this.numPolls + '.json' : App.apiPrefix + '/bootstrap/' + this.get('content.installOptions.bootRequestId');
    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      cache: false,
      success: function (data) {
        if (data.hostsStatus !== null) {
          // in case of bootstrapping just one host, the server returns an object rather than an array, so
          // force into an array
          if (!(data.hostsStatus instanceof Array)) {
            data.hostsStatus = [ data.hostsStatus ];
          }
          console.log("TRACE: In success function for the GET bootstrap call");
          var keepPolling = self.parseHostInfo(data.hostsStatus);
          if (data.hostsStatus.someProperty('status', 'DONE') || data.hostsStatus.someProperty('status', 'FAILED')) {
            // kicking off registration polls after at least one host has succeeded
            self.startRegistration();
          }
          if (keepPolling) {
            window.setTimeout(function () {
              self.doBootstrap()
            }, 3000);
            return;
          }
        }
      },
      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
      function () {
        App.showReloadPopup();
        console.log('Bootstrap failed');
      }
    );

  },

  /*
   stopBootstrap: function () {
   console.log('stopBootstrap() called');
   Ember.run.later(this, function () {
   this.startRegistration();
   }, 1000);
   },
   */

  startRegistration: function () {
    if (this.get('registrationStartedAt') == null) {
      this.set('registrationStartedAt', new Date().getTime());
      console.log('registration started at ' + this.get('registrationStartedAt'));
      this.isHostsRegistered();
    }
  },

  isHostsRegistered: function () {
    if (this.get('stopBootstrap')) {
      return;
    }
    var self = this;
    var hosts = this.get('bootHosts');
    var url = App.testMode ? '/data/wizard/bootstrap/single_host_registration.json' : App.apiPrefix + '/hosts';

    $.ajax({
      type: 'GET',
      url: url,
      timeout: App.timeout,
      success: function (data) {
        console.log('registration attempt...');
        var jsonData = App.testMode ? data : jQuery.parseJSON(data);
        if (!jsonData) {
          console.log("Error: jsonData is null");
          return;
        }

        // keep polling until all hosts have registered/failed, or registrationTimeout seconds after the last host finished bootstrapping
        var stopPolling = true;
        hosts.forEach(function (_host, index) {
          // Change name of first host for test mode.
          if (App.testMode) {
            if (index == 0) {
              _host.set('name', 'localhost.localdomain');
            }
          }
          // actions to take depending on the host's current bootStatus
          // RUNNING - bootstrap is running; leave it alone
          // DONE - bootstrap is done; transition to REGISTERING
          // REGISTERING - bootstrap is done but has not registered; transition to REGISTERED if host found in polling API result
          // REGISTERED - bootstrap and registration is done; leave it alone
          // FAILED - either bootstrap or registration failed; leave it alone
          console.log(_host.name + ' bootStatus=' + _host.get('bootStatus'));
          switch (_host.get('bootStatus')) {
            case 'DONE':
              _host.set('bootStatus', 'REGISTERING');
              _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistering with the server...');
              // update registration timestamp so that the timeout is computed from the last host that finished bootstrapping
              self.get('registrationStartedAt', new Date().getTime());
              stopPolling = false;
              break;
            case 'REGISTERING':
              if (jsonData.items.someProperty('Hosts.host_name', _host.name)) {
                console.log(_host.name + ' has been registered');
                _host.set('bootStatus', 'REGISTERED');
                _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistration with the server succeeded.');
              } else {
                console.log(_host.name + ' is registering...');
                stopPolling = false;
              }
              break;
            case 'RUNNING':
              stopPolling = false;
              break;
            case 'REGISTERED':
            case 'FAILED':
            default:
              break;
          }
        }, this);

        if (stopPolling) {
          self.getHostInfo();
        } else if (new Date().getTime() - self.get('registrationStartedAt') < self.get('registrationTimeoutSecs') * 1000) {
          window.setTimeout(function () {
            self.isHostsRegistered();
          }, 3000);
        } else {
          // registration timed out.  mark all REGISTERING hosts to FAILED
          console.log('registration timed out');
          hosts.filterProperty('bootStatus', 'REGISTERING').forEach(function (_host) {
            _host.set('bootStatus', 'FAILED');
            _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + '\nRegistration with the server failed.');
          });
          self.getHostInfo();
        }
      },
      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null, function () {
        App.showReloadPopup();
        console.log('Error: Getting registered host information from the server');
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
    var url = App.testMode ? '/data/wizard/bootstrap/single_host_information.json' : App.apiPrefix + '/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info';
    var method = 'GET';
    $.ajax({
      type: 'GET',
      url: url,
      contentType: 'application/json',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = (App.testMode) ? data : jQuery.parseJSON(data);
        hosts.forEach(function (_host) {
          var host = (App.testMode) ? jsonData.items[0] : jsonData.items.findProperty('Hosts.host_name', _host.name);
          if (App.skipBootstrap) {
            _host.cpu = 2;
            _host.memory = ((parseInt(2000000))).toFixed(2);
            _host.disk_info = [{"mountpoint": "/", "type":"ext4"},{"mountpoint": "/grid/0", "type":"ext4"}, {"mountpoint": "/grid/1", "type":"ext4"}, {"mountpoint": "/grid/2", "type":"ext4"}];
          } else if (host) {
            _host.cpu = host.Hosts.cpu_count;
            _host.memory = ((parseInt(host.Hosts.total_mem))).toFixed(2);
            _host.disk_info = host.Hosts.disk_info;

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
    this.set('isSubmitDisabled', !this.get('bootHosts').someProperty('bootStatus', 'REGISTERED'));
    this.set('isRetryDisabled', !this.get('bootHosts').someProperty('bootStatus', 'FAILED'));
  },

  selectCategory: function(event, context){
    this.set('category', event.context);
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.set('content.hosts', this.get('bootHosts'));
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
            if (self.get('isTextArea')) {
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
          didInsertElement: function () {
            var element = $(this.get('element'));
            element.width($(this.get('parentView').get('element')).width() - 10);
            element.height($(this.get('parentView').get('element')).height());
            element.select();
            element.css('resize', 'none');
          },
          readOnly: true,
          value: function () {
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

