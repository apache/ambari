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

App.WizardStep3Controller = Em.Controller.extend({
  name: 'wizardStep3Controller',
  hosts: [],
  content: [],
  bootHosts: [],
  registeredHosts: [],
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

      console.log('pushing ' + hostInfo.name);
      hosts.pushObject(hostInfo);
    }

    if(hosts.length > 100) {
      lazyloading.run({
        destination: this.get('hosts'),
        source: hosts,
        context: this,
        initSize: 20,
        chunkSize: 100,
        delay: 300
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

  /* Returns the current set of visible hosts on view (All, Succeeded, Failed) */
  visibleHosts: function () {
    if (this.get('category.hostsBootStatus')) {
      return this.hosts.filterProperty('bootStatus', this.get('category.hostsBootStatus'));
    } else { // if (this.get('category') === 'All Hosts')
      return this.hosts;
    }
  }.property('category', 'hosts.@each.bootStatus'),

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

  isInstallInProgress: function(){
    var bootStatuses = this.get('bootHosts').getEach('bootStatus');
    if(bootStatuses.length &&
      (bootStatuses.contains('REGISTERING') ||
        bootStatuses.contains('DONE') ||
        bootStatuses.contains('RUNNING') ||
        bootStatuses.contains('PENDING'))){
      return true;
    }
    return false;
  }.property('bootHosts.@each.bootStatus'),

  disablePreviousSteps: function(){
    if(this.get('isInstallInProgress')){
      App.router.get('installerController').setLowerStepsDisable(3);
      this.set('isSubmitDisabled', true);
    } else {
      App.router.get('installerController.isStepDisabled').filter(function(step){
        if(step.step >= 0 && step.step <= 2) return true;
      }).setEach('value', false);
    }
  }.observes('isInstallInProgress'),

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

      if (data.hostsStatus.someProperty('status', 'DONE') || data.hostsStatus.someProperty('status', 'FAILED')) {
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
      this.set('registrationStartedAt', new Date().getTime());
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
      console.log(_host.name + ' bootStatus=' + _host.get('bootStatus'));
      switch (_host.get('bootStatus')) {
        case 'DONE':
          _host.set('bootStatus', 'REGISTERING');
          _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
          // update registration timestamp so that the timeout is computed from the last host that finished bootstrapping
          this.set('registrationStartedAt', new Date().getTime());
          stopPolling = false;
          break;
        case 'REGISTERING':
          if (jsonData.items.someProperty('Hosts.host_name', _host.name)) {
            console.log(_host.name + ' has been registered');
            _host.set('bootStatus', 'REGISTERED');
            _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
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
      this.getHostInfo();
    } else if (hosts.someProperty('bootStatus', 'RUNNING') || new Date().getTime() - this.get('registrationStartedAt') < this.get('registrationTimeoutSecs') * 1000) {
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

  allHostsComplete: function() {
    var result = true;
    this.get('bootHosts').forEach(function(host) {
      var status = host.get('bootStatus');
      if (status != 'REGISTERED' && status != 'FAILED') {
        result = false;
      }
    });
    return result;
  }.property('bootHosts.@each.bootStatus'),

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

        var context = self.checkHostOSType(host.Hosts.os_type, host.Hosts.host_name);
        if(context) {
          hostsContext.push(context);
        }
        console.log("The value of memory is: " + _host.memory);
      }
    });
    if (hostsContext.length > 0) { // warning exist
      var repoWarning = {
        name: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.name'),
        hosts: hostsContext,
        category: 'repositories',
        onSingleHost: false
      };
      repoWarnings.push(repoWarning);
    }
    this.set('repoCategoryWarnings', repoWarnings);
    this.set('bootHosts', hosts);
    console.log("The value of hosts: " + JSON.stringify(hosts));
    this.stopRegistration();
  },

  getHostInfoErrorCallback: function () {
    console.log('INFO: Getting host information(cpu_count and total_mem) from the server failed');
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

  selectCategory: function(event, context){
    this.set('category', event.context);
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
  /**
   * check are hosts have any warnings
   */
  isHostHaveWarnings: function(){
    return this.get('warnings.length') > 0;
  }.property('warnings'),

  isWarningsBoxVisible: function(){
    return (App.testMode) ? true : this.get('allHostsComplete');
  }.property('allHostsComplete'),

  checksUpdateProgress:0,
  checksUpdateStatus: null,
  /**
   * filter data for warnings parse
   * is data from host in bootStrap
   * @param data
   * @return {Object}
   */
  filterBootHosts: function (data) {
    var bootHostNames = this.get('bootHosts').mapProperty('name');
    var filteredData = {
      href: data.href,
      items: []
    };
    data.items.forEach(function (host) {
      if (bootHostNames.contains(host.Hosts.host_name)) {
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
    data.items.sort(function (a, b) {
      if (a.Hosts.host_name > b.Hosts.host_name) {
        return 1;
      }
      if (a.Hosts.host_name < b.Hosts.host_name) {
        return -1;
      }
      return 0;
    });
    data.items.forEach(function (_host) {
      var host = {
        name: _host.Hosts.host_name,
        warnings: []
      }
      if (!_host.Hosts.last_agent_env) {
        // in some unusual circumstances when last_agent_env is not available from the _host,
        // skip the _host and proceed to process the rest of the hosts.
        console.log("last_agent_env is missing for " + _host.Hosts.host_name + ".  Skipping _host check.");
        return;
      }

      //parse all directories and files warnings for host

      //todo: to be removed after check in new API
      var stackFoldersAndFiles = _host.Hosts.last_agent_env.stackFoldersAndFiles || _host.Hosts.last_agent_env.paths;

      stackFoldersAndFiles.forEach(function (path) {
        warning = warnings.filterProperty('category', 'fileFolders').findProperty('name', path.name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: path.name,
            hosts: [_host.Hosts.host_name],
            category: 'fileFolders',
            onSingleHost: true
          }
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }, this);

      //parse all package warnings for host
      _host.Hosts.last_agent_env.installedPackages.forEach(function (_package) {
          warning = warnings.filterProperty('category', 'packages').findProperty('name', _package.name);
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.version = _package.version,
            warning.onSingleHost = false;
          } else {
            warning = {
              name: _package.name,
              version: _package.version,
              hosts: [_host.Hosts.host_name],
              category: 'packages',
              onSingleHost: true
            }
            warnings.push(warning);
          }
          host.warnings.push(warning);
      }, this);

      //parse all process warnings for host

      //todo: to be removed after check in new API
      var javaProcs = _host.Hosts.last_agent_env.hostHealth ? _host.Hosts.last_agent_env.hostHealth.activeJavaProcs : _host.Hosts.last_agent_env.javaProcs;

      javaProcs.forEach(function (process) {
        warning = warnings.filterProperty('category', 'processes').findProperty('pid', process.pid);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
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
          }
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }, this);

      //parse all service warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.hostHealth && _host.Hosts.last_agent_env.hostHealth.liveServices) {

        _host.Hosts.last_agent_env.hostHealth.liveServices.forEach(function (service) {
          if (service.status === 'Unhealthy') {
            warning = warnings.filterProperty('category', 'services').findProperty('name', service.name);
            if (warning) {
              warning.hosts.push(_host.Hosts.host_name);
              warning.onSingleHost = false;
            } else {
              warning = {
                name: service.name,
                hosts: [_host.Hosts.host_name],
                category: 'services',
                onSingleHost: true
              }
              warnings.push(warning);
            }
            host.warnings.push(warning);
          }
        }, this);
      }
      //parse all user warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.existingUsers) {

        _host.Hosts.last_agent_env.existingUsers.forEach(function (user) {
          warning = warnings.filterProperty('category', 'users').findProperty('name', user.userName);
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.onSingleHost = false;
          } else {
            warning = {
              name: user.userName,
              hosts: [_host.Hosts.host_name],
              category: 'users',
              onSingleHost: true
            }
            warnings.push(warning);
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
          }
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
          }
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      hosts.push(host);
    }, this);
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
  },
  /**
   * open popup that contain hosts' warnings
   * @param event
   */
  hostWarningsPopup: function(event){
    var self = this;
    var repoCategoryWarnings = this.get('repoCategoryWarnings');
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
        template: Ember.Handlebars.compile([
          '<div class="update-progress pull-left">',
          '{{#if view.isUpdateInProgress}}',
          '<div class="progress-info active progress">',
          '<div class="bar" {{bindAttr style="view.progressWidth"}}></div></div>',
          '{{else}}<label {{bindAttr class="view.updateStatusClass"}}>{{view.updateStatus}}</label>',
          '{{/if}}</div>',
          '{{#if view.parentView.secondary}}<button type="button" class="btn btn-info" {{bindAttr disabled="view.isUpdateInProgress"}} {{action onSecondary target="view.parentView"}}><i class="icon-repeat"></i>&nbsp;{{view.parentView.secondary}}</button>{{/if}}',
          '{{#if view.parentView.primary}}<button type="button" class="btn" {{action onPrimary target="view.parentView"}}>{{view.parentView.primary}}</button>{{/if}}'
        ].join('')),
        classNames: ['modal-footer', 'host-checks-update'],
        progressWidth: function(){
          return 'width:'+App.router.get('wizardStep3Controller.checksUpdateProgress')+'%';
        }.property('App.router.wizardStep3Controller.checksUpdateProgress'),
        isUpdateInProgress: function(){
          if((App.router.get('wizardStep3Controller.checksUpdateProgress') > 0) &&
             (App.router.get('wizardStep3Controller.checksUpdateProgress') < 100)){
            return true;
          }
        }.property('App.router.wizardStep3Controller.checksUpdateProgress'),
        updateStatusClass:function(){
          var status = App.router.get('wizardStep3Controller.checksUpdateStatus');
          if(status === 'SUCCESS'){
            return 'text-success';
          } else if(status === 'FAILED'){
            return 'text-error';
          } else {
            return null;
          }
        }.property('App.router.wizardStep3Controller.checksUpdateStatus'),
        updateStatus:function(){
          var status = App.router.get('wizardStep3Controller.checksUpdateStatus');
          if(status === 'SUCCESS'){
            return Em.I18n.t('installer.step3.warnings.updateChecks.success');
          } else if(status === 'FAILED'){
            return Em.I18n.t('installer.step3.warnings.updateChecks.failed');
          } else {
            return null;
          }
        }.property('App.router.wizardStep3Controller.checksUpdateStatus')
      }),

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step3_host_warnings_popup'),
        classNames: ['host-check'],
        didInsertElement: function () {
          Ember.run.next(this, function () {
            this.$("[rel='HostsListTooltip']").tooltip({html: true, placement: "right"});
            this.$('#process .warning-name').tooltip({html: true, placement: "top"});
          })
        }.observes('content'),
        warningsByHost: function () {
          return App.router.get('wizardStep3Controller.warningsByHost');
        }.property('App.router.wizardStep3Controller.warningsByHost'),
        warnings: function () {
          return App.router.get('wizardStep3Controller.warnings');
        }.property('App.router.wizardStep3Controller.warnings'),
        categories: function () {
          return this.get('warningsByHost').mapProperty('name');
        }.property('warningsByHost'),
        category: 'All Hosts',
        categoryWarnings: function () {
          return this.get('warningsByHost').findProperty('name', this.get('category')).warnings
        }.property('warningsByHost', 'category'),
        content: function () {
          var categoryWarnings = this.get('categoryWarnings');
          return [
             Ember.Object.create({
                warnings: repoCategoryWarnings,
                title: Em.I18n.t('installer.step3.hostWarningsPopup.repositories'),
                message: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.message'),
                type: Em.I18n.t('common.issues'),
                emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.repositories'),
                action: Em.I18n.t('installer.step3.hostWarningsPopup.action.invalid'),
                category: 'repositories'
             }),
             Ember.Object.create({
               warnings: categoryWarnings.filterProperty('category', 'firewall'),
               title: Em.I18n.t('installer.step3.hostWarningsPopup.firewall'),
               message: Em.I18n.t('installer.step3.hostWarningsPopup.firewall.message'),
               type: Em.I18n.t('common.issues'),
               emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.firewall'),
               action: Em.I18n.t('installer.step3.hostWarningsPopup.action.running'),
               category: 'firewall'
             }),
             Ember.Object.create({
               warnings: categoryWarnings.filterProperty('category', 'processes'),
               title: Em.I18n.t('installer.step3.hostWarningsPopup.process'),
               message: Em.I18n.t('installer.step3.hostWarningsPopup.processes.message'),
               type: Em.I18n.t('common.process'),
               emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.processes'),
               action: Em.I18n.t('installer.step3.hostWarningsPopup.action.running'),
               category: 'process'
             }),
             Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'packages'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.package'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.packages.message'),
              type: Em.I18n.t('common.package'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.packages'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.installed'),
              category: 'package'
            }),
             Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'fileFolders'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.fileAndFolder'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.fileFolders.message'),
              type: Em.I18n.t('common.path'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.filesAndFolders'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'fileFolders'
            }),
             Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'services'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.service'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.services.message'),
              type: Em.I18n.t('common.service'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.services'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.notRunning'),
              category: 'service'
            }),
             Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'users'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.user'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.users.message'),
              type: Em.I18n.t('common.user'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.users'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'user'
            }),
            Ember.Object.create({
              warnings: categoryWarnings.filterProperty('category', 'misc'),
              title: Em.I18n.t('installer.step3.hostWarningsPopup.misc'),
              message: Em.I18n.t('installer.step3.hostWarningsPopup.misc.message'),
              type: Em.I18n.t('installer.step3.hostWarningsPopup.misc.umask'),
              emptyName: Em.I18n.t('installer.step3.hostWarningsPopup.empty.misc'),
              action: Em.I18n.t('installer.step3.hostWarningsPopup.action.exists'),
              category: 'misc'
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
            onPrimary: function () {
              this.hide();
            },
            secondary: null
          });
        },

        onToggleBlock: function (category) {
          this.$('#' + category.context.category).toggle('blind', 500);
        },
        warningsNotice: function () {
          var warnings = this.get('warnings');
          var warningsByHost = self.get('warningsByHost').slice();
          warningsByHost.shift();
          var issues = warnings.length + ' ' + (warnings.length === 1 ? Em.I18n.t('installer.step3.hostWarningsPopup.issue') : Em.I18n.t('installer.step3.hostWarningsPopup.issues'));
          var hostsNumber = warningsByHost.length - warningsByHost.filterProperty('warnings.length', 0).length;
          var hosts = hostsNumber + ' ' + (hostsNumber === 1 ? Em.I18n.t('installer.step3.hostWarningsPopup.host') : Em.I18n.t('installer.step3.hostWarningsPopup.hosts'));
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
        template: Ember.Handlebars.compile([
          '<p>{{view.message}}</p>',
          '<ul>{{#each host in view.registeredHosts}}',
              '<li>{{host}}</li>',
          '{{/each}}</ul>'
        ].join('')),
        message: Em.I18n.t('installer.step3.registeredHostsPopup'),
        registeredHosts: self.get('registeredHosts')
      })
    })
  },

  back: function () {
    if (this.get('isInstallInProgress')) {
      return;
    }
    App.router.send('back');
  }

});

