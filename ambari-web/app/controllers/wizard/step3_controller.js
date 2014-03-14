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
var lazyloading = require('utils/lazy_loading');
var numberUtils = require('utils/number_utils');

App.WizardStep3Controller = Em.Controller.extend({
  name: 'wizardStep3Controller',
  hosts: [],
  content: [],
  bootHosts: [],
  registeredHosts: [],
  repoCategoryWarnings: [],
  diskCategoryWarnings: [],
  registrationStartedAt: null,
  registrationTimeoutSecs: function(){
    if(this.get('content.installOptions.manualInstall')){
      return 15;
    }
    return 120;
  }.property('content.installOptions.manualInstall'),
  stopBootstrap: false,
  isSubmitDisabled: true,

  categoryObject: Em.Object.extend({
    hostsCount: function () {
      var category = this;
      var hosts = this.get('controller.hosts').filter(function(_host) {
        if (_host.get('bootStatus') == category.get('hostsBootStatus')) {
          return true;
        } else {
          return (_host.get('bootStatus') == 'DONE' && category.get('hostsBootStatus') == 'REGISTERING');
        }
      }, this);
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
      self.categoryObject.create({value: Em.I18n.t('common.all'), hostsCount: function () {
        return this.get('controller.hosts.length');
      }.property('controller.hosts.length') }),
      self.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.installing'), hostsBootStatus: 'RUNNING'}),
      self.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.registering'), hostsBootStatus: 'REGISTERING'}),
      self.categoryObject.create({value: Em.I18n.t('common.success'), hostsBootStatus: 'REGISTERED' }),
      self.categoryObject.create({value: Em.I18n.t('common.fail'), hostsBootStatus: 'FAILED', last: true })
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
  isLoaded: false,

  navigateStep: function () {
    if(this.get('isLoaded')){
      if (this.get('content.installOptions.manualInstall') !== true) {
        if (!this.get('wizardController').getDBProperty('bootStatus')) {
          this.startBootstrap();
        }
      } else {
        this.set('bootHosts', this.get('hosts'));
        if (App.testMode) {
          this.getHostInfo();
          this.get('bootHosts').setEach('bootStatus', 'REGISTERED');
          this.get('bootHosts').setEach('cpu', '2');
          this.get('bootHosts').setEach('memory', '2000000');
          this.set('isSubmitDisabled', false);
        } else {
          this.set('registrationStartedAt', null);
          this.get('bootHosts').setEach('bootStatus', 'DONE');
          this.startRegistration();
        }
      }
    }
  }.observes('isLoaded'),

  clearStep: function () {
    this.set('stopBootstrap', false);
    this.set('hosts', []);
    this.get('bootHosts').clear();
    this.get('wizardController').setDBProperty('bootStatus', false);
    this.set('isSubmitDisabled', true);
    this.set('isRetryDisabled', true);
  },

  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    this.set('registrationStartedAt', null);
    this.set('isLoaded', false);
    this.disablePreviousSteps();

    this.clearStep();
    this.loadHosts();
    // hosts.setEach('bootStatus', 'RUNNING');
  },

  /* Loads the hostinfo from localStorage on the insertion of view. It's being called from view */
  loadHosts: function () {
    var hostsInfo = this.get('content.hosts');
    var hosts = [];

    for (var index in hostsInfo) {
      var hostInfo = App.HostInfo.create({
        name: hostsInfo[index].name,
        bootStatus: hostsInfo[index].bootStatus,
        isChecked: false
      });

      hosts.pushObject(hostInfo);
    }

    if(hosts.length > 200) {
      lazyloading.run({
        destination: this.get('hosts'),
        source: hosts,
        context: this,
        initSize: 100,
        chunkSize: 150,
        delay: 50
      });
    } else {
      this.set('hosts', hosts);
      this.set('isLoaded', true);
    }
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

  filterByCategory: function () {
    var category = this.get('category.hostsBootStatus');
    if (category) {
      this.get('hosts').forEach(function (host) {
        host.set('isVisible', (category === host.get('bootStatus')));
      });
    } else { // if (this.get('category') === 'All Hosts')
      this.get('hosts').setEach('isVisible', true);
    }
  }.observes('category', 'hosts.@each.bootStatus'),

  /* Returns the current set of visible hosts on view (All, Succeeded, Failed) */
  visibleHosts: function () {
    return this.get('hosts').filterProperty('isVisible');
  }.property('hosts.@each.isVisible'),

  removeHosts: function (hosts) {
    var self = this;
    App.showConfirmationPopup(function() {
      App.router.send('removeHosts', hosts);
      self.hosts.removeObjects(hosts);
      if (!self.hosts.length) {
        self.set('isSubmitDisabled', true);
      }
    },Em.I18n.t('installer.step3.hosts.remove.popup.body'));
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
    this.selectAllCategory();
    var bootStrapData = JSON.stringify({'verbose': true, 'sshKey': this.get('content.installOptions.sshKey'), 'hosts': hosts.mapProperty('name'), 'user': this.get('content.installOptions.sshUser')});
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
    //to display all hosts
    this.set('category', 'All');
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

  isRegistrationInProgress: true,

  setRegistrationInProgress: function () {
    var bootHosts = this.get('bootHosts');
    //if hosts aren't loaded yet then registration should be in progress
    var result = (bootHosts.length === 0);
    for (var i = 0, l = bootHosts.length; i < l; i++) {
      if (bootHosts[i].get('bootStatus') !== 'REGISTERED' && bootHosts[i].get('bootStatus') !== 'FAILED') {
        result = true;
        break;
      }
    }
    this.set('isRegistrationInProgress', result);
  }.observes('bootHosts.@each.bootStatus'),

  disablePreviousSteps: function () {
    App.router.get('installerController.isStepDisabled').filter(function (step) {
      return step.step >= 0 && step.step <= 2;
    }).setEach('value', this.get('isRegistrationInProgress'));
    if (this.get('isRegistrationInProgress')) {
      this.set('isSubmitDisabled', true);
    }
  }.observes('isRegistrationInProgress'),

  doBootstrap: function () {
    if (this.get('stopBootstrap')) {
      return;
    }
    this.numPolls++;

    App.ajax.send({
      name: 'wizard.step3.bootstrap',
      sender: this,
      data: {
        bootRequestId: this.get('content.installOptions.bootRequestId'),
        numPolls: this.numPolls
      },
      success: 'doBootstrapSuccessCallback'
    }).
      retry({
        times: App.maxRetries,
        timeout: App.timeout
      }).
      then(
        null,
        function () {
          App.showReloadPopup();
          console.log('Bootstrap failed');
        }
      );
  },

  doBootstrapSuccessCallback: function (data) {
    var self = this;
    var pollingInterval = 3000;
    if (data.hostsStatus === undefined) {
      console.log('Invalid response, setting timeout');
      window.setTimeout(function () {
        self.doBootstrap()
      }, pollingInterval);
    } else {
      // in case of bootstrapping just one host, the server returns an object rather than an array, so
      // force into an array
      if (!(data.hostsStatus instanceof Array)) {
        data.hostsStatus = [ data.hostsStatus ];
      }
      console.log("TRACE: In success function for the GET bootstrap call");
      var keepPolling = this.parseHostInfo(data.hostsStatus);

      // Single host : if the only hostname is invalid (data.status == 'ERROR')
      // Multiple hosts : if one or more hostnames are invalid
      // following check will mark the bootStatus as 'FAILED' for the invalid hostname
      if (data.status == 'ERROR' || data.hostsStatus.length != this.get('bootHosts').length) {

        var hosts = this.get('bootHosts');

        for (var i = 0; i < hosts.length; i++) {

          var isValidHost = data.hostsStatus.someProperty('hostName', hosts[i].get('name'));
          if(hosts[i].get('bootStatus') !== 'REGISTERED'){
            if (!isValidHost) {
              hosts[i].set('bootStatus', 'FAILED');
              hosts[i].set('bootLog', Em.I18n.t('installer.step3.hosts.bootLog.failed'));
            }
          }
        }
      }

      if (data.status == 'ERROR' || data.hostsStatus.someProperty('status', 'DONE') || data.hostsStatus.someProperty('status', 'FAILED')) {
        // kicking off registration polls after at least one host has succeeded
        this.startRegistration();
      }
      if (keepPolling) {
        window.setTimeout(function () {
          self.doBootstrap()
        }, pollingInterval);
      }
    }
  },

  startRegistration: function () {
    if (this.get('registrationStartedAt') == null) {
      this.set('registrationStartedAt', App.dateTime());
      console.log('registration started at ' + this.get('registrationStartedAt'));
      this.isHostsRegistered();
    }
  },

  isHostsRegistered: function () {
    if (this.get('stopBootstrap')) {
      return;
    }
    App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'isHostsRegisteredSuccessCallback'
    }).
      retry({
        times: App.maxRetries,
        timeout: App.timeout
      }).
        then(
          null,
          function () {
            App.showReloadPopup();
            console.log('Error: Getting registered host information from the server');
          }
        );
  },

  isHostsRegisteredSuccessCallback: function (data) {
    console.log('registration attempt...');
    var hosts = this.get('bootHosts');
    var jsonData = data;
    if (!jsonData) {
      console.warn("Error: jsonData is null");
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
      switch (_host.get('bootStatus')) {
        case 'DONE':
          _host.set('bootStatus', 'REGISTERING');
          _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
          // update registration timestamp so that the timeout is computed from the last host that finished bootstrapping
          this.set('registrationStartedAt', App.dateTime());
          stopPolling = false;
          break;
        case 'REGISTERING':
          if (jsonData.items.someProperty('Hosts.host_name', _host.name)) {
            _host.set('bootStatus', 'REGISTERED');
            _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
          } else {
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
      this.getHostInfo();
    } else if (hosts.someProperty('bootStatus', 'RUNNING') || App.dateTime() - this.get('registrationStartedAt') < this.get('registrationTimeoutSecs') * 1000) {
      // we want to keep polling for registration status if any of the hosts are still bootstrapping (so we check for RUNNING).
      var self = this;
      window.setTimeout(function () {
        self.isHostsRegistered();
      }, 3000);
    } else {
      // registration timed out.  mark all REGISTERING hosts to FAILED
      console.log('registration timed out');
      hosts.filterProperty('bootStatus', 'REGISTERING').forEach(function (_host) {
        _host.set('bootStatus', 'FAILED');
        _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.failed'));
      });
      this.getHostInfo();
    }
  },

  hasMoreRegisteredHosts: false,

  getAllRegisteredHosts: function() {
    App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'getAllRegisteredHostsCallback'
    });
  }.observes('bootHosts'),

  hostsInCluster: function() {
    return App.Host.find().getEach('hostName');
  }.property().volatile(),

  getAllRegisteredHostsCallback: function(hosts) {
    var registeredHosts = [];
    var hostsInCluster = this.get('hostsInCluster');
    var addedHosts = this.get('bootHosts').getEach('name');
    hosts.items.forEach(function(host){
      if (!hostsInCluster.contains(host.Hosts.host_name) && !addedHosts.contains(host.Hosts.host_name)) {
        registeredHosts.push(host.Hosts.host_name);
      }
    });
    if(registeredHosts.length) {
      this.set('hasMoreRegisteredHosts',true);
      this.set('registeredHosts',registeredHosts);
    } else {
      this.set('hasMoreRegisteredHosts',false);
      this.set('registeredHosts','');
    }
  },

  registerErrPopup: function (header, message) {
    App.ModalPopup.show({
      header: header,
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{view.message}}</p>'),
        message: message
      })
    });
  },

  /**
   * Get disk info and cpu count of booted hosts from server
   */
  getHostInfo: function () {
    this.set('isWarningsLoaded', false);
    App.ajax.send({
      name: 'wizard.step3.host_info',
      sender: this,
      success: 'getHostInfoSuccessCallback',
      error: 'getHostInfoErrorCallback'
    });
  },

  getHostInfoSuccessCallback: function (jsonData) {
    var hosts = this.get('bootHosts');
    var self = this;
    this.parseWarnings(jsonData);
    var repoWarnings = [];
    var hostsContext = [];
    var diskWarnings = [];
    var hostsDiskContext = [];
    var hostsDiskNames = [];
    var hostsRepoNames = [];
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
        _host.os_type = host.Hosts.os_type;
        _host.os_arch = host.Hosts.os_arch;
        _host.ip = host.Hosts.ip;

        var context = self.checkHostOSType(host.Hosts.os_type, host.Hosts.host_name);
        if(context) {
          hostsContext.push(context);
          hostsRepoNames.push(host.Hosts.host_name);
        }
        var diskContext = self.checkHostDiskSpace(host.Hosts.host_name, host.Hosts.disk_info);
        if (diskContext) {
          hostsDiskContext.push(diskContext);
          hostsDiskNames.push(host.Hosts.host_name);
        }

      }
    });
    if (hostsContext.length > 0) { // warning exist
      var repoWarning = {
        name: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.name'),
        hosts: hostsContext,
        hostsNames: hostsRepoNames,
        category: 'repositories',
        onSingleHost: false
      };
      repoWarnings.push(repoWarning);
    }
    if (hostsDiskContext.length > 0) { // disk space warning exist
      var diskWarning = {
        name: Em.I18n.t('installer.step3.hostWarningsPopup.disk.name'),
        hosts: hostsDiskContext,
        hostsNames: hostsDiskNames,
        category: 'disk',
        onSingleHost: false
      };
      diskWarnings.push(diskWarning);
    }

    this.set('repoCategoryWarnings', repoWarnings);
    this.set('diskCategoryWarnings', diskWarnings);
    this.set('bootHosts', hosts);
    this.stopRegistration();
  },

  getHostInfoErrorCallback: function () {
    console.log('INFO: Getting host information(cpu_count and total_mem) from the server failed');
    this.set('isWarningsLoaded', true);
    this.registerErrPopup(Em.I18n.t('installer.step3.hostInformation.popup.header'), Em.I18n.t('installer.step3.hostInformation.popup.body'));
  },

  stopRegistration: function () {
    this.set('isSubmitDisabled', !this.get('bootHosts').someProperty('bootStatus', 'REGISTERED'));
    this.set('isRetryDisabled', !this.get('bootHosts').someProperty('bootStatus', 'FAILED'));
  },

  /**
   * Check if the customized os group contains the registered host os type. If not the repo on that host is invalid.
   */
  checkHostOSType: function (osType, hostName) {
    if(this.get('content.stacks')){
      var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
      var selectedOS = [];
      var isValid = false;
      if (selectedStack && selectedStack.operatingSystems) {
        selectedStack.get('operatingSystems').filterProperty('selected', true).forEach( function(os) {
          selectedOS.pushObject(os.osType);
          if ( os.osType == osType) {
            isValid = true;
          }
        });
      }

      if (!isValid) {
        console.log('WARNING: Getting host os type does NOT match the user selected os group in step1. ' +
          'Host Name: '+ hostName + '. Host os type:' + osType + '. Selected group:' + selectedOS);
        return Em.I18n.t('installer.step3.hostWarningsPopup.repositories.context').format(hostName, osType, selectedOS);
      } else {
        return null;
      }
    }else{
      return null;
    }
  },

  /**
   * Check if current host has enough free disk usage.
   */
  checkHostDiskSpace: function (hostName, diskInfo) {
    var minFreeRootSpace = App.minDiskSpace * 1024 * 1024; //in kilobyte
    var minFreeUsrLibSpace = App.minDiskSpaceUsrLib * 1024 * 1024; //in kilobyte
    var warningString = '';

    diskInfo.forEach( function(info) {
      switch (info.mountpoint) {
        case '/':
          warningString = info.available < minFreeRootSpace ? Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpace + 'GB', info.mountpoint) + ' ' + warningString : warningString;
          break;
        case '/usr':
        case '/usr/lib':
          warningString = info.available < minFreeUsrLibSpace ? Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpaceUsrLib + 'GB', info.mountpoint) + ' ' + warningString : warningString;
          break;
        default:
          break;
      }
    });
    if (warningString) {
      console.log('WARNING: Getting host free disk space. ' + 'Host Name: '+ hostName);
      return Em.I18n.t('installer.step3.hostWarningsPopup.disk.context1').format(hostName) + ' ' + warningString;
    } else {
      return null;
    }
  },

  selectCategory: function(event, context){
    this.set('category', event.context);
  },

  selectAllCategory: function(){
    this.set('category', this.get('categories').get('firstObject'));
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
        if(this.get('isHostHaveWarnings')) {
            var self = this;
            App.showConfirmationPopup(
                function(){
                    self.set('content.hosts', self.get('bootHosts'));
                    App.router.send('next');
                },
                Em.I18n.t('installer.step3.hostWarningsPopup.hostHasWarnings'));
        }
        else {
              this.set('content.hosts', this.get('bootHosts'));
              App.router.send('next');
        }
    }
  },

  hostLogPopup: function (event, context) {
    var host = event.context;

    App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.hostLog.popup.header').format(host.get('name')),
      secondary: null,

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_host_log_popup'),
        host: host,
        didInsertElement: function () {
          var self = this;
          var button = $(this.get('element')).find('.textTrigger');
          button.click(function () {
            if (self.get('isTextArea')) {
              $(this).text(Em.I18n.t('installer.step3.hostLogPopup.highlight'));
            } else {
              $(this).text(Em.I18n.t('installer.step3.hostLogPopup.copy'));
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
  /**
   * check warnings from server and put it in parsing
    */
  rerunChecks: function () {
    var self = this;
    var currentProgress = 0;
    var interval = setInterval(function () {
      currentProgress += 100000 / self.get('warningsTimeInterval');
      if (currentProgress < 100) {
        self.set('checksUpdateProgress', currentProgress);
      } else {
        clearInterval(interval);
        App.ajax.send({
          name: 'wizard.step3.rerun_checks',
          sender: self,
          success: 'rerunChecksSuccessCallback',
          error: 'rerunChecksErrorCallback'
        });
      }
    }, 1000);
  },

  rerunChecksSuccessCallback: function (data) {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'SUCCESS');
    this.parseWarnings(data);
  },

  rerunChecksErrorCallback: function () {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'FAILED');
    console.log('INFO: Getting host information(last_agent_env) from the server failed');
  },

  warnings: [],
  warningsByHost: [],
  warningsTimeInterval: 60000,
  isWarningsLoaded: false,
  /**
   * check are hosts have any warnings
   */
  isHostHaveWarnings: function(){
    return this.get('warnings.length') > 0;
  }.property('warnings'),

  isWarningsBoxVisible: function(){
    return (App.testMode) ? true : !this.get('isRegistrationInProgress');
  }.property('isRegistrationInProgress'),

  checksUpdateProgress:0,
  checksUpdateStatus: null,
  /**
   * filter data for warnings parse
   * is data from host in bootStrap
   * @param data
   * @return {Object}
   */
  filterBootHosts: function (data) {
    var bootHostNames = {};
    this.get('bootHosts').forEach(function (bootHost) {
      bootHostNames[bootHost.get('name')] = true;
    });
    var filteredData = {
      href: data.href,
      items: []
    };
    data.items.forEach(function (host) {
      if (bootHostNames[host.Hosts.host_name]) {
        filteredData.items.push(host);
      }
    });
    return filteredData;
  },
  /**
   * parse warnings data for each host and total
   * @param data
   */
  parseWarnings: function (data) {
    data = App.testMode ? data : this.filterBootHosts(data);
    var warnings = [];
    var warning;
    var hosts = [];
    var warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {}
    };

    data.items.sortPropertyLight('Hosts.host_name').forEach(function (_host) {
      var host = {
        name: _host.Hosts.host_name,
        warnings: []
      };
      if (!_host.Hosts.last_agent_env) {
        // in some unusual circumstances when last_agent_env is not available from the _host,
        // skip the _host and proceed to process the rest of the hosts.
        console.log("last_agent_env is missing for " + _host.Hosts.host_name + ".  Skipping _host check.");
        return;
      }

      //parse all directories and files warnings for host

      //todo: to be removed after check in new API
      var stackFoldersAndFiles = _host.Hosts.last_agent_env.stackFoldersAndFiles || [];
      stackFoldersAndFiles.forEach(function (path) {
        warning = warningCategories.fileFoldersWarnings[path.name];
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warningCategories.fileFoldersWarnings[path.name] = warning = {
            name: path.name,
            hosts: [_host.Hosts.host_name],
            category: 'fileFolders',
            onSingleHost: true
          };
        }
        host.warnings.push(warning);
      }, this);

      //parse all package warnings for host
      _host.Hosts.last_agent_env.installedPackages.forEach(function (_package) {
          warning = warningCategories.packagesWarnings[_package.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.version = _package.version;
            warning.onSingleHost = false;
          } else {
            warningCategories.packagesWarnings[_package.name] = warning = {
              name: _package.name,
              version: _package.version,
              hosts: [_host.Hosts.host_name],
              category: 'packages',
              onSingleHost: true
            };
          }
          host.warnings.push(warning);
      }, this);

      //parse all process warnings for host

      //todo: to be removed after check in new API
      var javaProcs = _host.Hosts.last_agent_env.hostHealth ? _host.Hosts.last_agent_env.hostHealth.activeJavaProcs : _host.Hosts.last_agent_env.javaProcs;
      javaProcs.forEach(function (process) {
        warning = warningCategories.processesWarnings[process.pid];
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warningCategories.processesWarnings[process.pid] = warning = {
            name: (process.command.substr(0, 35) + '...'),
            hosts: [_host.Hosts.host_name],
            category: 'processes',
            user: process.user,
            pid: process.pid,
            command: '<table><tr><td style="word-break: break-all;">' +
                ((process.command.length < 500) ? process.command : process.command.substr(0, 230) + '...' +
                    '<p style="text-align: center">................</p>' +
                    '...' + process.command.substr(-230)) + '</td></tr></table>',
            onSingleHost: true
          };
        }
        host.warnings.push(warning);
      }, this);

      //parse all service warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.hostHealth && _host.Hosts.last_agent_env.hostHealth.liveServices) {
        _host.Hosts.last_agent_env.hostHealth.liveServices.forEach(function (service) {
          if (service.status === 'Unhealthy') {
            warning = warningCategories.servicesWarnings[service.name];
            if (warning) {
              warning.hosts.push(_host.Hosts.host_name);
              warning.onSingleHost = false;
            } else {
              warningCategories.servicesWarnings[service.name] = warning = {
                name: service.name,
                hosts: [_host.Hosts.host_name],
                category: 'services',
                onSingleHost: true
              };
            }
            host.warnings.push(warning);
          }
        }, this);
      }
      //parse all user warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.existingUsers) {
        _host.Hosts.last_agent_env.existingUsers.forEach(function (user) {
          warning = warningCategories.usersWarnings[user.userName];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.onSingleHost = false;
          } else {
            warningCategories.usersWarnings[user.userName] = warning = {
              name: user.userName,
              hosts: [_host.Hosts.host_name],
              category: 'users',
              onSingleHost: true
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse misc warnings for host
      var umask = _host.Hosts.last_agent_env.umask;
      if (umask && umask !== 18) {
        warning = warnings.filterProperty('category', 'misc').findProperty('name', umask);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: umask,
            hosts: [_host.Hosts.host_name],
            category: 'misc',
            onSingleHost: true
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      
      var firewallRunning = _host.Hosts.last_agent_env.iptablesIsRunning;
      if (firewallRunning!==null && firewallRunning) {
        var name = Em.I18n.t('installer.step3.hostWarningsPopup.firewall.name');
        warning = warnings.filterProperty('category', 'firewall').findProperty('name', name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            category: 'firewall',
            onSingleHost: true
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      hosts.push(host);
    }, this);

    for (var categoryId in warningCategories) {
      var category = warningCategories[categoryId]
      for (var warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    warnings.forEach(function (warn) {
      if (warn.hosts.length < 11) {
        warn.hostsList = warn.hosts.join('<br>')
      } else {
        warn.hostsList = warn.hosts.slice(0,10).join('<br>') + '<br> ' + Em.I18n.t('installer.step3.hostWarningsPopup.moreHosts').format(warn.hosts.length - 10);
      }
    });
    hosts.unshift({
      name: 'All Hosts',
      warnings: warnings
    });
    this.set('warnings', warnings);
    this.set('warningsByHost', hosts);
    this.set('isWarningsLoaded', true);
  },
  /**
   * open popup that contain hosts' warnings
   * @param event
   */
  hostWarningsPopup: function(event){
    var self = this;
    var repoCategoryWarnings = this.get('repoCategoryWarnings');
    var diskCategoryWarnings = this.get('diskCategoryWarnings');
    App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.warnings.popup.header'),
      secondary: Em.I18n.t('installer.step3.hostWarningsPopup.rerunChecks'),
      primary: Em.I18n.t('common.close'),
      onPrimary: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },
      onClose: function(){
        self.set('checksUpdateStatus', null);
        this.hide();
      },
      onSecondary: function() {
        self.rerunChecks();
      },
      didInsertElement: function () {
        this.fitHeight();
      },

      footerClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_host_warning_popup_footer'),
        classNames: ['modal-footer', 'host-checks-update'],
        footerControllerBinding: 'App.router.wizardStep3Controller',
        progressWidth: function(){
          return 'width:'+this.get('footerController.checksUpdateProgress')+'%';
        }.property('footerController.checksUpdateProgress'),
        isUpdateInProgress: function(){
          if((this.get('footerController.checksUpdateProgress') > 0) &&
             (this.get('footerController.checksUpdateProgress') < 100)){
            return true;
          }
        }.property('footerController.checksUpdateProgress'),
        updateStatusClass:function(){
          var status = this.get('footerController.checksUpdateStatus');
          if(status === 'SUCCESS'){
            return 'text-success';
          } else if(status === 'FAILED'){
            return 'text-error';
          } else {
            return null;
          }
        }.property('footerController.checksUpdateStatus'),
        updateStatus:function(){
          var status = this.get('footerController.checksUpdateStatus');
          if(status === 'SUCCESS'){
            return Em.I18n.t('installer.step3.warnings.updateChecks.success');
          } else if(status === 'FAILED'){
            return Em.I18n.t('installer.step3.warnings.updateChecks.failed');
          } else {
            return null;
          }
        }.property('footerController.checksUpdateStatus')
      }),

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_host_warnings_popup'),
        classNames: ['host-check'],
        bodyControllerBinding: 'App.router.wizardStep3Controller',
        didInsertElement: function () {
          Ember.run.next(this, function () {
            App.tooltip(this.$("[rel='HostsListTooltip']"), {html: true, placement: "right"});
            App.tooltip(this.$('#process .warning-name'), {html: true, placement: "top"});
          });
        }.observes('content'),
        hostSelectView: Ember.Select.extend({
          //content has default value "All Hosts" to bind selection to category
          content: ['All Hosts'],
          hosts: function () {
            return this.get('parentView.warningsByHost').mapProperty('name');
          }.property('parentView.warningsByHost'),
          isLoaded: false,
          selectionBinding: "parentView.category",
          didInsertElement: function(){
            this.initContent();
          },
          initContent: function () {
            this.set('isLoaded', false);
            //The lazy loading for select elements supported only by Firefox and Chrome
            var isBrowserSupported = $.browser.mozilla || ($.browser.safari && navigator.userAgent.indexOf('Chrome') !== -1);
            var isLazyLoading = isBrowserSupported && this.get('hosts').length > 100;
            this.set('isLazyLoading', isLazyLoading);
            if (isLazyLoading) {
              //select need at least 30 hosts to have scrollbar
              this.set('content', this.get('hosts').slice(0, 30));
            } else {
              this.set('content', this.get('hosts'));
              this.set('isLoaded', true);
            }
          }.observes('parentView.warningsByHost'),
          /**
           * on click start lazy loading
           */
          click: function () {
            if (!this.get('isLoaded') && this.get('isLazyLoading')) {
              //filter out hosts, which already pushed in select
              var source = this.get('hosts').filter(function (_host) {
                return !this.get('content').contains(_host);
              }, this).slice();
              lazyloading.run({
                destination: this.get('content'),
                source: source,
                context: this,
                initSize: 30,
                chunkSize: 200,
                delay: 50
              });
            }
          }
        }),
        warningsByHost: function () {
          return this.get('bodyController.warningsByHost');
        }.property('bodyController.warningsByHost'),
        warnings: function () {
          return this.get('bodyController.warnings');
        }.property('bodyController.warnings'),
        category: 'All Hosts',
        categoryWarnings: function () {
          return this.get('warningsByHost').findProperty('name', this.get('category')).warnings
        }.property('warningsByHost', 'category'),
        content: function () {
          var categoryWarnings = this.get('categoryWarnings');
          return [
            Ember.Object.create({
              warnings: diskCategoryWarnings,
              title: Em.I18n.t('installer.step3.hostWarningsPopup.disk'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.disk.message'),
              type: Em.I18n.t('common.issues'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.disk'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'disk',
              isCollapsed: true
            }),
            Ember.Object.create({
              warnings: repoCategoryWarnings,
              title: Em.I18n.t('installer.step3.hostWarningsPopup.repositories'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.message'),
              type: Em.I18n.t('common.issues'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.repositories'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.invalid'),
              category: 'repositories',
              isCollapsed: true
            }),
            Ember.Object.create({
             warnings: categoryWarnings.filterProperty('category', 'firewall'),
             title: Em.I18n.t('installer.step3.hostWarningsPopup.firewall'),
             message: Em.I18n.t('installer.step3.hostWarningsPopup.firewall.message'),
             type: Em.I18n.t('common.issues'),
             emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.firewall'),
             action: Em.I18n.t('installer.step3.hostWarningsPopup.action.running'),
             category: 'firewall',
             isCollapsed: true
            }),
            Ember.Object.create({
             warnings: categoryWarnings.filterProperty('category', 'processes'),
             title: Em.I18n.t('installer.step3.hostWarningsPopup.process'),
             message: Em.I18n.t('installer.step3.hostWarningsPopup.processes.message'),
             type: Em.I18n.t('common.process'),
             emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.processes'),
             action: Em.I18n.t('installer.step3.hostWarningsPopup.action.running'),
             category: 'process',
             isCollapsed: true
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'packages'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.package'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.packages.message'),
              type: Em.I18n.t('common.package'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.packages'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.installed'),
              category: 'package',
              isCollapsed: true
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'fileFolders'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.fileAndFolder'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.fileFolders.message'),
              type: Em.I18n.t('common.path'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.filesAndFolders'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'fileFolders',
              isCollapsed: true
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'services'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.service'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.services.message'),
              type: Em.I18n.t('common.service'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.services'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.notRunning'),
              category: 'service',
              isCollapsed: true
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'users'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.user'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.users.message'),
              type: Em.I18n.t('common.user'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.users'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'user',
              isCollapsed: true
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'misc'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.misc'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.misc.message'),
              type: Em.I18n.t('installer.step3.hostWarningsPopup.misc.umask'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.misc'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'misc',
              isCollapsed: true
            })
          ]
        }.property('category', 'warningsByHost'),

        showHostsPopup: function (hosts) {
          $('.tooltip').hide();
          App.ModalPopup.show({
            header: Em.I18n.t('installer.step3.hostWarningsPopup.allHosts'),
            bodyClass: Ember.View.extend({
              hosts: hosts.context,
              template: Ember.Handlebars.compile('<ul>{{#each host in view.hosts}}<li>{{host}}</li>{{/each}}</ul>')
            }),
            secondary: null
          });
        },

        onToggleBlock: function (category) {
          this.$('#' + category.context.category).toggle('blind', 500);
          category.context.set("isCollapsed", !category.context.get("isCollapsed"));
        },
        /**
         * generate number of hosts which had warnings, avoid duplicated host names in different warnings.
         */
        warningHostsNamesCount: function () {
          var hostNameMap = Ember.Object.create();
          var warningsByHost = self.get('warningsByHost').slice();
          warningsByHost.shift();
          warningsByHost.forEach( function( _host) {
            if (_host.warnings.length) {
              hostNameMap[_host.name] = true;
            }
          })
          if (repoCategoryWarnings.length) {
            repoCategoryWarnings[0].hostsNames.forEach(function (_hostName) {
              if (!hostNameMap[_hostName]) {
                hostNameMap[_hostName] = true;
              }
            })
          }
          if (diskCategoryWarnings.length) {
            diskCategoryWarnings[0].hostsNames.forEach(function (_hostName) {
              if (!hostNameMap[_hostName]) {
                hostNameMap[_hostName] = true;
              }
            })
          }
          var size = 0;
          for (var key in hostNameMap) {
            if (hostNameMap.hasOwnProperty(key)) size++;
          }
          return size;
        },
        warningsNotice: function () {
          var warnings = this.get('warnings');
          var issuesNumber = warnings.length + repoCategoryWarnings.length + diskCategoryWarnings.length;
          var issues = issuesNumber + ' ' + (issuesNumber.length === 1 ? Em.I18n.t('installer.step3.hostWarningsPopup.issue') : Em.I18n.t('installer.step3.hostWarningsPopup.issues'));
          var hostsCnt = this.warningHostsNamesCount();
          var hosts = hostsCnt + ' ' + (hostsCnt === 1 ? Em.I18n.t('installer.step3.hostWarningsPopup.host') : Em.I18n.t('installer.step3.hostWarningsPopup.hosts'));
          return Em.I18n.t('installer.step3.hostWarningsPopup.summary').format(issues, hosts);
        }.property('warnings', 'warningsByHost'),
        /**
         * generate detailed content to show it in new window
         */
        contentInDetails: function () {
          var content = this.get('content');
          var warningsByHost = this.get('warningsByHost').slice();
          warningsByHost.shift();
          var newContent = '';
          newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.header') + new Date;
          newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.hosts');
          newContent += warningsByHost.filterProperty('warnings.length').mapProperty('name').join(' ');
          if (content.findProperty('category', 'firewall').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.firewall');
            newContent += content.findProperty('category', 'firewall').warnings.mapProperty('name').join('<br>');
          }
          if (content.findProperty('category', 'fileFolders').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.fileFolders');
            newContent += content.findProperty('category', 'fileFolders').warnings.mapProperty('name').join(' ');
          }
          if (content.findProperty('category', 'process').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.process');
            content.findProperty('category', 'process').warnings.forEach(function (process, i) {
              process.hosts.forEach(function (host, j) {
                if (!!i || !!j) {
                  newContent += ',';
                }
                newContent += '(' + host + ',' + process.user + ',' + process.pid + ')';
              });
            });
          }
          if (content.findProperty('category', 'package').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.package');
            newContent += content.findProperty('category', 'package').warnings.mapProperty('name').join(' ');
          }
          if (content.findProperty('category', 'service').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.service');
            newContent += content.findProperty('category', 'service').warnings.mapProperty('name').join(' ');
          }
          if (content.findProperty('category', 'user').warnings.length) {
            newContent += Em.I18n.t('installer.step3.hostWarningsPopup.report.user');
            newContent += content.findProperty('category', 'user').warnings.mapProperty('name').join(' ');
          }
          newContent += '</p>';
          return newContent;
        }.property('content', 'warningsByHost'),
        /**
         * open new browser tab with detailed content
         */
        openWarningsInDialog: function(){
          var newWindow = window.open('');
          var newDocument = newWindow.document;
          newDocument.write(this.get('contentInDetails'));
          newWindow.focus();
        }
      })
    })
  },

  registeredHostsPopup: function(){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('registeredHosts').length),
      secondary: null,
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_registered_hosts_popup'),
        message: Em.I18n.t('installer.step3.registeredHostsPopup'),
        registeredHosts: self.get('registeredHosts')
      })
    })
  },

  back: function () {
    if (this.get('isRegistrationInProgress')) {
      return;
    }
    App.router.send('back');
  }

});

