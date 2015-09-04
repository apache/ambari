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

App.WizardStep3Controller = Em.Controller.extend(App.ReloadPopupMixin, {

  name: 'wizardStep3Controller',

  hosts: [],

  content: [],

  bootHosts: [],

  registeredHosts: [],

  /**
   * @typedef {{
   *  name: string,
   *  hosts: string[],
   *  hostsLong: string[],
   *  hostsNames: string[],
   *  onSingleHost: boolean
   * }} checkWarning
   */

  /**
   * @type {checkWarning[]}
   */
  hostCheckWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  repoCategoryWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  diskCategoryWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  thpCategoryWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  jdkCategoryWarnings: null,

  jdkRequestIndex: null,

  registrationStartedAt: null,

  requestId: 0,

  /**
   * Timeout for registration
   * Based on <code>installOptions.manualInstall</code>
   * @type {number}
   */
  registrationTimeoutSecs: function () {
    return this.get('content.installOptions.manualInstall') ? 15 : 120;
  }.property('content.installOptions.manualInstall'),

  /**
   * Bootstrap calls are stopped
   * @type {bool}
   */
  stopBootstrap: false,

  /**
   * is Submit button disabled
   * @type {bool}
   */
  isSubmitDisabled: true,

  /**
   * is Retry button disabled
   * @type {bool}
   */
  isRetryDisabled: function() {
    return (this.get('isBackDisabled')) ? this.get('isBackDisabled') : !this.get('bootHosts').filterProperty('bootStatus', 'FAILED').length;
  }.property('bootHosts.@each.bootStatus', 'isBackDisabled'),

  /**
   * Is Back button disabled
   * @return {bool}
   */
  isBackDisabled: function () {
    return this.get('isRegistrationInProgress') || !this.get('isWarningsLoaded');
  }.property('isRegistrationInProgress', 'isWarningsLoaded'),

  /**
   * Controller is using in Add Host Wizard
   * @return {bool}
   */
  isAddHostWizard: function () {
    return this.get('content.controllerName') === 'addHostController';
  }.property('content.controllerName'),

  /**
   * @type {bool}
   */
  isLoaded: false,

  /**
   * Polls count
   * @type {number}
   */
  numPolls: 0,

  /**
   * Is hosts registration in progress
   * @type {bool}
   */
  isRegistrationInProgress: true,

  /**
   * Are some registered hosts which are not added by user
   * @type {bool}
   */
  hasMoreRegisteredHosts: false,

  /**
   * Contain data about installed packages on hosts
   * @type {Array}
   */
  hostsPackagesData: [],

  /**
   * List of installed hostnames
   * @type {string[]}
   */
  hostsInCluster: function () {
    var installedHostsName = [];
    var hosts = this.get('content.hosts');

    for (var hostName in hosts) {
      if (hosts[hostName].isInstalled) {
        installedHostsName.push(hostName);
      }
    }
    return installedHostsName;
  }.property('content.hosts'),

  /**
   * All hosts warnings
   * @type {object[]}
   */
  warnings: [],

  /**
   * Warnings grouped by host
   * @type {Ember.Enumerable}
   */
  warningsByHost: [],

  /**
   * Timeout for "warning"-requests
   * @type {number}
   */
  warningsTimeInterval: 60000,

  /**
   * Are hosts warnings loaded
   * @type {bool}
   */
  isWarningsLoaded: function () {
    return this.get('isJDKWarningsLoaded') && this.get('isHostsWarningsLoaded');
  }.property('isJDKWarningsLoaded', 'isHostsWarningsLoaded'),

  /**
   * Check are hosts have any warnings
   * @type {bool}
   */
  isHostHaveWarnings: function () {
    return this.get('warnings.length') > 0;
  }.property('warnings'),

  /**
   * Should warnings-box be visible
   * @type {bool}
   */
  isWarningsBoxVisible: function () {
    return (App.get('testMode')) ? true : !this.get('isRegistrationInProgress');
  }.property('isRegistrationInProgress'),

  /**
   * Progress value for "update hosts status" process
   * @type {number}
   */
  checksUpdateProgress: 0,

  /**
   *
   * @type {object}
   */
  checksUpdateStatus: null,

  /**
   *
   * @method navigateStep
   */
  navigateStep: function () {
    if (this.get('isLoaded')) {
      if (!this.get('content.installOptions.manualInstall')) {
        if (!this.get('wizardController').getDBProperty('bootStatus')) {
          this.setupBootStrap();
        }
      } else {
        this.set('bootHosts', this.get('hosts'));
        if (App.get('testMode')) {
          this.startHostcheck();
          this.get('bootHosts').setEach('cpu', '2');
          this.get('bootHosts').setEach('memory', '2000000');
          this.set('isSubmitDisabled', false);
        } else {
          this.set('registrationStartedAt', null);
          this.startRegistration();
        }
      }
    }
  }.observes('isLoaded'),

  /**
   * Clear controller data
   * @method clearStep
   */
  clearStep: function () {
    this.set('stopBootstrap', false);
    this.set('hosts', []);
    this.get('bootHosts').clear();
    this.get('wizardController').setDBProperty('bootStatus', false);
    this.set('isHostsWarningsLoaded', false);
    this.set('isJDKWarningsLoaded', false);
    this.set('registrationStartedAt', null);
    this.set('isLoaded', false);
    this.set('isSubmitDisabled', true);
    this.set('stopChecking', false);
  },

  /**
   * setup bootstrap data and completion callback for bootstrap call
   * @method setupBootStrap
   */
  setupBootStrap: function () {
    var self = this;
    var bootStrapData = JSON.stringify({
        'verbose': true,
        'sshKey': this.get('content.installOptions.sshKey'),
        'hosts': this.getBootstrapHosts(),
        'user': this.get('content.installOptions.sshUser'),
        'userRunAs': App.get('supports.customizeAgentUserAccount') ? this.get('content.installOptions.agentUser') : 'root'
    });
    App.router.get(this.get('content.controllerName')).launchBootstrap(bootStrapData, function (requestId) {
      if (requestId == '0') {
        self.startBootstrap();
      } else if (requestId) {
        self.set('content.installOptions.bootRequestId', requestId);
        App.router.get(self.get('content.controllerName')).save('installOptions');
        self.startBootstrap();
      }
    });
  },

  getBootstrapHosts: function () {
    var hosts = this.get('content.hosts');
    var bootstrapHosts = [];
    for (var host in hosts) {
      if (hosts.hasOwnProperty(host)) {
        if (!hosts[host].isInstalled) {
          bootstrapHosts.push(host);
        }
      }
    }

    return bootstrapHosts;
  },

  /**
   * Make basic init steps
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step3: Confirm Hosts");
    this.disablePreviousSteps();
    this.clearStep();
    App.router.get('clusterController').loadAmbariProperties();
    this.loadHosts();
  },

  /**
   * Loads the hostinfo from localStorage on the insertion of view. It's being called from view
   * @method loadHosts
   */
  loadHosts: function () {
    var hostsInfo = this.get('content.hosts');
    var hosts = [];
    var bootStatus = (this.get('content.installOptions.manualInstall')) ? 'DONE' : 'PENDING';
    if (App.get('testMode')) {
      bootStatus = 'REGISTERED';
    }

    for (var index in hostsInfo) {
      if (hostsInfo.hasOwnProperty(index) && !hostsInfo[index].isInstalled) {
        hosts.pushObject(App.HostInfo.create({
          name: hostsInfo[index].name,
          bootStatus: bootStatus,
          isChecked: false
        }));
      }
    }
    this.set('hosts', hosts);
    this.set('isLoaded', true);
  },

  /**
   * Parses and updates the content based on bootstrap API response.
   * @return {bool} true if polling should continue (some hosts are in "RUNNING" state); false otherwise
   * @method parseHostInfo
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

  /**
   * Remove list of hosts
   * @param {Ember.Enumerable} hosts
   * @return {App.ModalPopup}
   * @method removeHosts
   */
  removeHosts: function (hosts) {
    var self = this;
    return App.showConfirmationPopup(function () {
      App.router.send('removeHosts', hosts);
      self.hosts.removeObjects(hosts);
      self.stopRegistration();
      if (!self.hosts.length) {
        self.set('isSubmitDisabled', true);
      }
    }, Em.I18n.t('installer.step3.hosts.remove.popup.body'));
  },

  /**
   * Removes a single element on the trash icon click. Called from View
   * @param {object} hostInfo
   * @method removeHost
   */
  removeHost: function (hostInfo) {
    if (!this.get('isBackDisabled'))
      this.removeHosts([hostInfo]);
  },

  /**
   * Remove selected hosts (click-handler)
   * @return App.ModalPopup
   * @method removeSelectedHosts
   */
  removeSelectedHosts: function () {
    var selectedHosts = this.get('hosts').filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.name);
    });
    return this.removeHosts(selectedHosts);
  },

  /**
   * Show popup with the list of hosts which are selected
   * @return App.ModalPopup
   * @method selectedHostsPopup
   */
  selectedHostsPopup: function () {
    var selectedHosts = this.get('hosts').filterProperty('isChecked').mapProperty('name');
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.selectedHosts.popup.header'),
      secondary: null,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/items_list_popup'),
        items: selectedHosts,
        insertedItems: [],
        didInsertElement: function () {
          lazyloading.run({
            destination: this.get('insertedItems'),
            source: this.get('items'),
            context: this,
            initSize: 100,
            chunkSize: 500,
            delay: 100
          });
        }
      })
    });
  },

  /**
   * Retry one host {click-handler}
   * @param {object} hostInfo
   * @method retryHost
   */
  retryHost: function (hostInfo) {
    this.retryHosts([hostInfo]);
  },

  /**
   * Retry list of hosts
   * @param {object[]} hosts
   * @method retryHosts
   */
  retryHosts: function (hosts) {
    var self = this;
    var bootStrapData = JSON.stringify({
        'verbose': true,
        'sshKey': this.get('content.installOptions.sshKey'),
        'hosts': hosts.mapProperty('name'),
        'user': this.get('content.installOptions.sshUser'),
        'userRunAs': App.get('supports.customizeAgentUserAccount') ? this.get('content.installOptions.agentUser') : 'root'
      });
    this.set('numPolls', 0);
    this.set('registrationStartedAt', null);
    this.set('isHostsWarningsLoaded', false);
    this.set('stopChecking', false);
    this.set('isSubmitDisabled', true);
    if (this.get('content.installOptions.manualInstall')) {
      this.startRegistration();
    } else {
      App.router.get(this.get('content.controllerName')).launchBootstrap(bootStrapData, function (requestId) {
        self.set('content.installOptions.bootRequestId', requestId);
        self.doBootstrap();
      });
    }
  },

  /**
   * Retry selected hosts (click-handler)
   * @method retrySelectedHosts
   */
  retrySelectedHosts: function () {
    if (!this.get('isRetryDisabled')) {
      var selectedHosts = this.get('bootHosts').filterProperty('bootStatus', 'FAILED');
      selectedHosts.forEach(function (_host) {
        _host.set('bootStatus', 'DONE');
        _host.set('bootLog', 'Retrying ...');
      }, this);
      this.retryHosts(selectedHosts);
    }
  },

  /**
   * Init bootstrap settings and start it
   * @method startBootstrap
   */
  startBootstrap: function () {
    //this.set('isSubmitDisabled', true);    //TODO: uncomment after actual hookup
    this.set('numPolls', 0);
    this.set('registrationStartedAt', null);
    this.set('bootHosts', this.get('hosts'));
    this.doBootstrap();
  },

  /**
   * Update <code>isRegistrationInProgress</code> once
   * @method setRegistrationInProgressOnce
   */
  setRegistrationInProgressOnce: function () {
    Em.run.once(this, 'setRegistrationInProgress');
  }.observes('bootHosts.@each.bootStatus'),

  /**
   * Set <code>isRegistrationInProgress</code> value based on each host boot status
   * @method setRegistrationInProgress
   */
  setRegistrationInProgress: function () {
    var bootHosts = this.get('bootHosts');
    //if hosts aren't loaded yet then registration should be in progress
    var result = (bootHosts.length === 0 && !this.get('isLoaded'));
    for (var i = 0, l = bootHosts.length; i < l; i++) {
      if (bootHosts[i].get('bootStatus') !== 'REGISTERED' && bootHosts[i].get('bootStatus') !== 'FAILED') {
        result = true;
        break;
      }
    }
    this.set('isRegistrationInProgress', result);
  },

  /**
   * Disable wizard's previous steps (while registering)
   * @method disablePreviousSteps
   */
  disablePreviousSteps: function () {
    App.router.get('installerController.isStepDisabled').filter(function (step) {
      return step.step >= 0 && step.step <= 2;
    }).setEach('value', this.get('isBackDisabled'));
    App.router.get('addHostController.isStepDisabled').filter(function (step) {
      return step.step >= 0 && step.step <= 1;
    }).setEach('value', this.get('isBackDisabled'));
  }.observes('isBackDisabled'),

  /**
   * Do bootstrap calls
   * @method doBootstrap
   * @return {$.ajax|null}
   */
  doBootstrap: function () {
    var self = this;
    if (this.get('stopBootstrap')) {
      return null;
    }
    this.incrementProperty('numPolls');

    return App.ajax.send({
      name: 'wizard.step3.bootstrap',
      sender: this,
      data: {
        bootRequestId: this.get('content.installOptions.bootRequestId'),
        numPolls: this.get('numPolls')
      },
      success: 'doBootstrapSuccessCallback'
    }).
      retry({
        times: App.maxRetries,
        timeout: App.timeout
      }).
      then(
        function () {
          self.closeReloadPopup();
        },
        function () {
          self.showReloadPopup();
          console.log('Bootstrap failed');
        }
      );
  },

  /**
   * Success-callback for each boostrap request
   * @param {object} data
   * @method doBootstrapSuccessCallback
   */
  doBootstrapSuccessCallback: function (data) {
    var self = this;
    var pollingInterval = 3000;
    if (Em.isNone(data.hostsStatus)) {
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
      var installedHosts = App.Host.find().mapProperty('hostName');
      if (data.status == 'ERROR' || data.hostsStatus.mapProperty('hostName').removeObjects(installedHosts).length != this.get('bootHosts').length) {

        var hosts = this.get('bootHosts');

        for (var i = 0; i < hosts.length; i++) {

          var isValidHost = data.hostsStatus.someProperty('hostName', hosts[i].get('name'));
          if (hosts[i].get('bootStatus') !== 'REGISTERED') {
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

  /**
   * Start hosts registration
   * @method startRegistration
   */
  startRegistration: function () {
    if (Em.isNone(this.get('registrationStartedAt'))) {
      this.set('registrationStartedAt', App.dateTime());
      console.log('registration started at ' + this.get('registrationStartedAt'));
      this.isHostsRegistered();
    }
  },

  /**
   * Do requests to check if hosts are already registered
   * @return {$.ajax|null}
   * @method isHostsRegistered
   */
  isHostsRegistered: function () {
    if (this.get('stopBootstrap')) {
      return null;
    }
    var self = this;
    return App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'isHostsRegisteredSuccessCallback'
    }).
      retry({
        times: App.maxRetries,
        timeout: App.timeout
      }).
      then(
        function () {
          self.closeReloadPopup();
        },
        function () {
          self.showReloadPopup();
          console.log('Error: Getting registered host information from the server');
        }
      );
  },

  /**
   * Success-callback for registered hosts request
   * @param {object} data
   * @method isHostsRegisteredSuccessCallback
   */
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
      if (App.get('testMode')) {
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
          if (jsonData.items.someProperty('Hosts.host_name', _host.name) && !jsonData.items.filterProperty('Hosts.host_name', _host.name).someProperty('Hosts.host_status', 'UNKNOWN')) {
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
      this.startHostcheck();
    }
    else {
      if (hosts.someProperty('bootStatus', 'RUNNING') || App.dateTime() - this.get('registrationStartedAt') < this.get('registrationTimeoutSecs') * 1000) {
        // we want to keep polling for registration status if any of the hosts are still bootstrapping (so we check for RUNNING).
        var self = this;
        window.setTimeout(function () {
          self.isHostsRegistered();
        }, 3000);
      }
      else {
        // registration timed out.  mark all REGISTERING hosts to FAILED
        console.log('registration timed out');
        hosts.filterProperty('bootStatus', 'REGISTERING').forEach(function (_host) {
          _host.set('bootStatus', 'FAILED');
          _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.failed'));
        });
        this.startHostcheck();
      }
    }
  },

  /**
   * Do request for all registered hosts
   * @return {$.ajax}
   * @method getAllRegisteredHosts
   */
  getAllRegisteredHosts: function () {
    return App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'getAllRegisteredHostsCallback'
    });
  }.observes('bootHosts'),

  /**
   * Success-callback for all registered hosts request
   * @param {object} hosts
   * @method getAllRegisteredHostsCallback
   */
  getAllRegisteredHostsCallback: function (hosts) {
    var registeredHosts = [];
    var hostsInCluster = this.get('hostsInCluster');
    var addedHosts = this.get('bootHosts').getEach('name');
    hosts.items.forEach(function (host) {
      if (!hostsInCluster.contains(host.Hosts.host_name) && !addedHosts.contains(host.Hosts.host_name)) {
        registeredHosts.push(host.Hosts.host_name);
      }
    });
    if (registeredHosts.length) {
      this.set('hasMoreRegisteredHosts', true);
      this.set('registeredHosts', registeredHosts);
    } else {
      this.set('hasMoreRegisteredHosts', false);
      this.set('registeredHosts', '');
    }
  },

  /**
   * Show popup with regitration error-message
   * @param {string} header
   * @param {string} message
   * @return {App.ModalPopup}
   * @method registerErrPopup
   */
  registerErrPopup: function (header, message) {
    return App.ModalPopup.show({
      header: header,
      secondary: false,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{view.message}}</p>'),
        message: message
      })
    });
  },

  /**
   * Get JDK name from server to determine if user had setup a customized JDK path when doing 'ambari-server setup'.
   * The Ambari properties are different from default ambari-server setup, property 'jdk.name' will be missing if a customized jdk path is applied.
   * @return {$.ajax}
   * @method getJDKName
   */
  getJDKName: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields : '?fields=RootServiceComponents/properties/jdk.name,RootServiceComponents/properties/java.home,RootServiceComponents/properties/jdk_location'
      },
      success: 'getJDKNameSuccessCallback'
    });
  },

  /**
    * Success callback for JDK name, property 'jdk.name' will be missing if a customized jdk path is applied
    * @param {object} data
    * @method getJDKNameSuccessCallback
    */
  getJDKNameSuccessCallback: function (data) {
    this.set('needJDKCheckOnHosts', !data.RootServiceComponents.properties["jdk.name"]);
    this.set('jdkLocation', Em.get(data, "RootServiceComponents.properties.jdk_location"));
    this.set('javaHome', data.RootServiceComponents.properties["java.home"]);
  },

  doCheckJDK: function () {
    var hostsNames = (!this.get('content.installOptions.manualInstall')) ? this.get('bootHosts').filterProperty('bootStatus', 'REGISTERED').getEach('name').join(",") : this.get('bootHosts').getEach('name').join(",");
    var javaHome = this.get('javaHome');
    var jdkLocation = this.get('jdkLocation');
    App.ajax.send({
      name: 'wizard.step3.jdk_check',
      sender: this,
      data: {
        host_names: hostsNames,
        java_home: javaHome,
        jdk_location: jdkLocation
      },
      success: 'doCheckJDKsuccessCallback',
      error: 'doCheckJDKerrorCallback'
    });
  },

  doCheckJDKsuccessCallback: function (data) {
    if(data){
      this.set('jdkRequestIndex', data.href.split('/')[data.href.split('/').length - 1]);
    }
    if (this.get('jdkCategoryWarnings') == null) {
      // get jdk check results for all hosts
      App.ajax.send({
        name: 'wizard.step3.jdk_check.get_results',
        sender: this,
        data: {
          requestIndex: this.get('jdkRequestIndex')
        },
        success: 'parseJDKCheckResults'
      })
    } else {
      this.set('isJDKWarningsLoaded', true);
    }
  },
  doCheckJDKerrorCallback: function () {
    console.log('INFO: Doing JDK check for host failed');
    this.set('isJDKWarningsLoaded', true);
  },
  parseJDKCheckResults: function (data) {
    var jdkWarnings = [], hostsJDKContext = [], hostsJDKNames = [];
    // check if the request ended
    if (data.Requests.end_time > 0 && data.tasks) {
      data.tasks.forEach( function(task) {
        // generate warning context
        if (Em.get(task, "Tasks.structured_out.java_home_check.exit_code") == 1){
          var jdkContext = Em.I18n.t('installer.step3.hostWarningsPopup.jdk.context').format(task.Tasks.host_name);
          hostsJDKContext.push(jdkContext);
          hostsJDKNames.push(task.Tasks.host_name);
        }
      });
      if (hostsJDKContext.length > 0) { // java jdk warning exist
        var invalidJavaHome = this.get('javaHome');
        jdkWarnings.push({
          name: Em.I18n.t('installer.step3.hostWarningsPopup.jdk.name').format(invalidJavaHome),
          hosts: hostsJDKContext,
          hostsLong: hostsJDKContext,
          hostsNames: hostsJDKNames,
          category: 'jdk',
          onSingleHost: false
        });
      }
      this.set('jdkCategoryWarnings', jdkWarnings);
    } else {
      // still doing JDK check, data not ready to be parsed
      this.set('jdkCategoryWarnings', null);
    }
    this.doCheckJDKsuccessCallback();
  },

  /**
   * Check JDK issues on registered hosts.
   */
  checkHostJDK: function () {
    this.set('isJDKWarningsLoaded', false);
    this.set('jdkCategoryWarnings', null);
    var self = this;
    this.getJDKName().done( function() {
      if (self.get('needJDKCheckOnHosts')) {
        // need to do JDK check on each host
       self.doCheckJDK();
      } else {
        // no customized JDK path, so no need to check jdk
        self.set('jdkCategoryWarnings', []);
        self.set('isJDKWarningsLoaded', true);
      }
    });
  },

  /**
   * Get disk info and cpu count of booted hosts from server
   * @return {$.ajax}
   * @method getHostInfo
   */
  getHostInfo: function () {
    this.set('isHostsWarningsLoaded', false);
    // begin JDK check for each host
    return App.ajax.send({
      name: 'wizard.step3.host_info',
      sender: this,
      success: 'getHostInfoSuccessCallback',
      error: 'getHostInfoErrorCallback'
    });
  },


  startHostcheck: function() {
    this.set('isWarningsLoaded', false);
    this.getHostNameResolution();
    this.checkHostJDK();
  },

  getHostNameResolution: function () {
    if (App.get('testMode')) {
      this.getHostCheckSuccess();
    } else {
      var data = this.getDataForCheckRequest("host_resolution_check", true);
      data ? this.requestToPerformHostCheck(data) : this.stopHostCheck();
    }
  },

  getGeneralHostCheck: function () {
    if (App.get('testMode')) {
      this.getHostInfo();
    } else {
      var data = this.getDataForCheckRequest("last_agent_env_check,installed_packages,existing_repos,transparentHugePage", false);
      data ? this.requestToPerformHostCheck(data) : this.stopHostCheck();
    }
  },

  /**
   * set all fields from which depends running host check to true value
   * which force finish checking;
   */
  stopHostCheck: function() {
    this.set('stopChecking', true);
    this.set('isJDKWarningsLoaded', true);
    this.set('isHostsWarningsLoaded', true);
  },

  getHostCheckSuccess: function(response) {
    if (!App.get('testMode')) {
      this.set("requestId", response.Requests.id);
    }
    this.getHostCheckTasks();
  },

  /**
   * generates data for reuest to perform check
   * @param {string} checkExecuteList - for now supported:
   *  <code>"last_agent_env_check"<code>
   *  <code>"host_resolution_check"<code>
   * @param {boolean} addHostsParameter - define whether add hosts parameter to RequestInfo
   * @return {object|null}
   * @method getDataForCheckRequest
   */
  getDataForCheckRequest: function (checkExecuteList, addHostsParameter) {
    var newHosts = this.get('bootHosts').filterProperty('bootStatus', 'REGISTERED').getEach('name');
    var hosts = this.get('isAddHostWizard') ? [].concat.apply([], App.MasterComponent.find().mapProperty('hostNames')).concat(newHosts).uniq() : newHosts;
    hosts = hosts.join(',');
    if (hosts.length == 0) return null;
    var jdk_location = App.router.get('clusterController.ambariProperties.jdk_location');
    var RequestInfo = {
      "action": "check_host",
      "context": "Check host",
      "parameters": {
        "check_execute_list": checkExecuteList,
        "jdk_location" : jdk_location,
        "threshold": "20"
      }
    };
    if (addHostsParameter) {
      RequestInfo.parameters.hosts = hosts;
    }
    var resource_filters = {
      "hosts": hosts
    };
    return {
      RequestInfo: RequestInfo,
      resource_filters: resource_filters
    }
  },

  /**
   * send request to ceate tasks for performing hosts checks
   * @params {object} data
   *    {
   *       RequestInfo: {
   *           "action": {string},
   *           "context": {string},
   *           "parameters": {
   *             "check_execute_list": {string},
   *             "jdk_location" : {string},
   *             "threshold": {string}
   *             "hosts": {string|undefined}
   *       },
   *       resource_filters: {
   *         "hosts": {string}
   *       }
   *    }
   * @returns {$.ajax}
   * @method requestToPerformHostCheck
   */
  requestToPerformHostCheck: function(data) {
    return App.ajax.send({
      name: 'preinstalled.checks',
      sender: this,
      data: {
        RequestInfo: data.RequestInfo,
        resource_filters: data.resource_filters
      },
      success: "getHostCheckSuccess",
      error: "getHostCheckError"
    })
  },

  /**
   * send ajax request to get all tasks
   * @method getHostCheckTasks
   */
  getHostCheckTasks: function () {
    var self = this;
    var requestId = this.get("requestId");
    var checker = setTimeout(function () {
      if (self.get('stopChecking') == true) {
        clearTimeout(checker);
      } else {
        App.ajax.send({
          name: 'preinstalled.checks.tasks',
          sender: self,
          data: {
            requestId: requestId
          },
          success: 'getHostCheckTasksSuccess',
          error: 'getHostCheckTasksError'
        });
      }
    }, 1000);
  },

  /**
   * add warnings to host warning popup if needed
   * @param data {Object} - json
   * @method getHostCheckTasksSuccess
   */
  getHostCheckTasksSuccess: function (data) {
    console.log('checking attempt...');
    if (!data) {
      console.warn("Error: jsonData is null");
      return;
    }
    if (["FAILED", "COMPLETED", "TIMEDOUT"].contains(data.Requests.request_status)) {
      if (data.Requests.inputs.indexOf("last_agent_env_check") != -1) {
        this.set('stopChecking', true);
        this.set('hostsPackagesData', data.tasks.map(function (task) {
          var installed_packages = Em.get(task, 'Tasks.structured_out.installed_packages');
          return {
            hostName: Em.get(task, 'Tasks.host_name'),
            transparentHugePage: Em.get(task, 'Tasks.structured_out.transparentHugePage.message'),
            installedPackages: installed_packages ? installed_packages : []
          };
        }));
        this.getHostInfo();
      } else if (data.Requests.inputs.indexOf("host_resolution_check") != -1) {
        this.parseHostNameResolution(data);
        this.getGeneralHostCheck();
       }
    } else {
      this.getHostCheckTasks();
    }
  },

  /**
   * parse warnings for host names resolution only
   * @param {object} data
   * @method parseHostNameResolution
   */
  parseHostNameResolution: function (data) {
    if (!data) {
      console.warn("Error: jsonData is null");
      return;
    }
    data.tasks.forEach(function (task) {
      var name = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.error');
      var hostInfo = this.get("hostCheckWarnings").findProperty('name', name);
      if (["FAILED", "COMPLETED", "TIMEDOUT"].contains(task.Tasks.status)) {
        if (task.Tasks.status == "COMPLETED" && Em.get(task, "Tasks.structured_out.host_resolution_check.failed_count") != 0) {
          var targetHostName = Em.get(task, "Tasks.host_name");
          var relatedHostNames = Em.get(task, "Tasks.structured_out.host_resolution_check.failures")
            ? Em.get(task, "Tasks.structured_out.host_resolution_check.failures").mapProperty('host') : [];
          var contextMessage = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format(targetHostName, relatedHostNames.length + ' ' + Em.I18n.t('installer.step3.hostWarningsPopup.host' + (relatedHostNames.length == 1 ? '' : 's')));
          var contextMessageLong = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format(targetHostName, relatedHostNames.join(', '));
          if (!hostInfo) {
            hostInfo = {
              name: name,
              hosts: [contextMessage],
              hostsLong: [contextMessageLong],
              hostsNames: [targetHostName],
              onSingleHost: true
            };
            this.get("hostCheckWarnings").push(hostInfo);
          } else {
            if (!hostInfo.hostsNames.contains(targetHostName)) {
              hostInfo.hosts.push(contextMessage);
              hostInfo.hostsLong.push(contextMessageLong);
              hostInfo.hostsNames.push(targetHostName);
              hostInfo.onSingleHost = false;
            }
          }
        }
      }
    }, this);
  },

  getHostCheckError: function() {
    this.getHostInfo();
  },

  stopChecking: false,

  /**
   * @method getHostCheckTasksError
   */
  getHostCheckTasksError: function() {
    console.warn("failed to cheek hostName resolution");
    this.set('stopChecking', true);
  },

  /**
   * Success-callback for hosts info request
   * @param {object} jsonData
   * @method getHostInfoSuccessCallback
   */
  getHostInfoSuccessCallback: function (jsonData) {
    var hosts = this.get('bootHosts'),
      self = this,
      repoWarnings = [], hostsRepoNames = [], hostsContext = [],
      diskWarnings = [], hostsDiskContext = [], hostsDiskNames = [],
      thpWarnings = [], thpContext = [], thpHostsNames = [];

    // parse host checks warning
    this.parseWarnings(jsonData);
    this.set('isHostsWarningsLoaded', true);
    hosts.forEach(function (_host) {
      var host = (App.get('testMode')) ? jsonData.items[0] : jsonData.items.findProperty('Hosts.host_name', _host.name);
      if (App.get('skipBootstrap')) {
        self._setHostDataWithSkipBootstrap(_host);
      }
      else {
        if (host) {
          self._setHostDataFromLoadedHostInfo(_host, host);
          var host_name = Em.get(host, 'Hosts.host_name');

          var context = self.checkHostOSType(host.Hosts.os_family, host_name);
          if (context) {
            hostsContext.push(context);
            hostsRepoNames.push(host_name);
          }
          var diskContext = self.checkHostDiskSpace(host_name, host.Hosts.disk_info);
          if (diskContext) {
            hostsDiskContext.push(diskContext);
            hostsDiskNames.push(host_name);
          }
          // "Transparent Huge Pages" check
          var _hostPackagesData = self.get('hostsPackagesData').findProperty('hostName', host.Hosts.host_name);
          if (_hostPackagesData) {
            var transparentHugePage = _hostPackagesData.transparentHugePage;
            context = self.checkTHP(host_name, transparentHugePage);
          } else {
            context = self.checkTHP(host_name, Em.get(host, 'Hosts.last_agent_env.transparentHugePage'));
          }
          if (context) {
            thpContext.push(context);
            thpHostsNames.push(host_name);
          }
        }
      }
    });
    if (hostsContext.length > 0) { // repository warning exist
      repoWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.name'),
        hosts: hostsContext,
        hostsLong: hostsContext,
        hostsNames: hostsRepoNames,
        category: 'repositories',
        onSingleHost: false
      });
    }
    if (hostsDiskContext.length > 0) { // disk space warning exist
      diskWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.disk.name'),
        hosts: hostsDiskContext,
        hostsLong: hostsDiskContext,
        hostsNames: hostsDiskNames,
        category: 'disk',
        onSingleHost: false
      });
    }
    if (thpContext.length > 0) { // THP warning existed
      thpWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.thp.name'),
        hosts: thpContext,
        hostsLong: thpContext,
        hostsNames: thpHostsNames,
        category: 'thp',
        onSingleHost: false
      });
    }

    this.set('repoCategoryWarnings', repoWarnings);
    this.set('diskCategoryWarnings', diskWarnings);
    this.set('thpCategoryWarnings', thpWarnings);
    this.stopRegistration();
  },

  /**
   * Set metrics to host object
   * Used when <code>App.skipBootstrap</code> is true
   * @param {Ember.Object} host
   * @returns {object}
   * @private
   * @methos _setHostDataWithSkipBootstrap
   */
  _setHostDataWithSkipBootstrap: function(host) {
    host.set('cpu', 2);
    host.set('memory', ((parseInt(2000000))).toFixed(2));
    host.set('disk_info', [
      {"mountpoint": "/", "type": "ext4"},
      {"mountpoint": "/grid/0", "type": "ext4"},
      {"mountpoint": "/grid/1", "type": "ext4"},
      {"mountpoint": "/grid/2", "type": "ext4"}
    ]);
    return host;
  },

  /**
   * Set loaded metrics to host object
   * @param {object} host
   * @param {object} hostInfo
   * @returns {object}
   * @method _setHostDataFromLoadedHostInfo
   * @private
   */
  _setHostDataFromLoadedHostInfo: function(host, hostInfo) {
    host.set('cpu', Em.get(hostInfo, 'Hosts.cpu_count'));
    host.set('memory', ((parseInt(Em.get(hostInfo, 'Hosts.total_mem')))).toFixed(2));
    host.set('disk_info', Em.get(hostInfo, 'Hosts.disk_info').filter(function (h) {
      return h.mountpoint != "/boot"
    }));
    host.set('os_type', Em.get(hostInfo, 'Hosts.os_type'));
    host.set('os_family', Em.get(hostInfo, 'Hosts.os_family'));
    host.set('os_arch', Em.get(hostInfo, 'Hosts.os_arch'));
    host.set('ip', Em.get(hostInfo, 'Hosts.ip'));
    return host;
  },

  /**
   * Error-callback for hosts info request
   * @method getHostInfoErrorCallback
   */
  getHostInfoErrorCallback: function () {
    console.log('INFO: Getting host information(cpu_count and total_mem) from the server failed');
    this.set('isHostsWarningsLoaded', true);
    this.registerErrPopup(Em.I18n.t('installer.step3.hostInformation.popup.header'), Em.I18n.t('installer.step3.hostInformation.popup.body'));
  },

  /**
   * Enable or disable submit/retry buttons according to hosts boot statuses
   * @method stopRegistration
   */
  stopRegistration: function () {
    this.set('isSubmitDisabled', !this.get('bootHosts').someProperty('bootStatus', 'REGISTERED'));
  },

  /**
   * Check if the 'Transparent Huge Pages' enabled.
   * @param {string} transparentHugePage
   * @param {string} hostName
   * @return {string} error-message or empty string
   * @method checkTHP
   */
  checkTHP: function (hostName, transparentHugePage) {
    if (transparentHugePage == "always") {
      console.log('WARNING: Transparent Huge Page enabled on host: '+ hostName);
      return Em.I18n.t('installer.step3.hostWarningsPopup.thp.context').format(hostName);
    } else {
      return '';
    }
  },

  /**
   * Check if the customized os group contains the registered host os type. If not the repo on that host is invalid.
   * @param {string} osType
   * @param {string} hostName
   * @return {string} error-message or empty string
   * @method checkHostOSType
   */
  checkHostOSType: function (osFamily, hostName) {
    if (this.get('content.stacks')) {
      var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
      var selectedOS = [];
      var isValid = false;
      if (selectedStack && selectedStack.get('operatingSystems')) {
        selectedStack.get('operatingSystems').filterProperty('isSelected', true).forEach(function (os) {
          selectedOS.pushObject(os.get('osType'));
          if (os.get('osType') === osFamily) {
            isValid = true;
          }
        });
      }
      if (isValid) {
        return '';
      } else {
        console.log('WARNING: Getting host os type does NOT match the user selected os group in step1. ' +
          'Host Name: ' + hostName + '. Host os type:' + osFamily + '. Selected group:' + selectedOS.uniq());
        return Em.I18n.t('installer.step3.hostWarningsPopup.repositories.context').format(hostName, osFamily, selectedOS.uniq());
      }
    } else {
      return '';
    }
  },

  /**
   * Check if current host has enough free disk usage.
   * @param {string} hostName
   * @param {object} diskInfo
   * @return {string} error-message or empty string
   * @method checkHostDiskSpace
   */
  checkHostDiskSpace: function (hostName, diskInfo) {
    var minFreeRootSpace = App.minDiskSpace * 1024 * 1024; //in kilobyte
    var minFreeUsrLibSpace = App.minDiskSpaceUsrLib * 1024 * 1024; //in kilobyte
    var warningString = '';

    diskInfo.forEach(function (info) {
      switch (info.mountpoint) {
        case '/':
          warningString = info.available < minFreeRootSpace ?
            Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpace + 'GB', info.mountpoint) + ' ' + warningString :
            warningString;
          break;
        case '/usr':
        case '/usr/lib':
          warningString = info.available < minFreeUsrLibSpace ?
            Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpaceUsrLib + 'GB', info.mountpoint) + ' ' + warningString :
            warningString;
          break;
        default:
          break;
      }
    });
    if (warningString) {
      console.log('WARNING: Getting host free disk space. ' + 'Host Name: ' + hostName);
      return Em.I18n.t('installer.step3.hostWarningsPopup.disk.context1').format(hostName) + ' ' + warningString;
    } else {
      return '';
    }
  },

  /**
   * Submit-click handler
   * @return {App.ModalPopup|null}
   * @method submit
   */
  submit: function () {
    var self = this;

    if (this.get('isHostHaveWarnings')) {
      return App.showConfirmationPopup(
        function () {
          self.set('confirmedHosts', self.get('bootHosts'));
          App.router.send('next');
        },
        Em.I18n.t('installer.step3.hostWarningsPopup.hostHasWarnings'));
    }
    else {
      this.set('confirmedHosts', this.get('bootHosts'));
      App.router.send('next');
    }
    return null;
  },

  /**
   * Show popup with host log
   * @param {object} event
   * @return {App.ModalPopup}
   */
  hostLogPopup: function (event) {
    var host = event.context;

    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hostLog.popup.header').format(host.get('name')),
      secondary: null,
      host: host,
      bodyClass: App.WizardStep3HostLogPopupBody
    });
  },

  /**
   * Check warnings from server and put it in parsing
   * @method rerunChecks
   */
  rerunChecks: function () {
    var self = this;
    var currentProgress = 0;
    this.getHostNameResolution();
    this.set('stopChecking', false);
    this.getGeneralHostCheck();
    this.checkHostJDK();
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

  /**
   * Success-callback for rerun request
   * @param {object} data
   * @method rerunChecksSuccessCallback
   */
  rerunChecksSuccessCallback: function (data) {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'SUCCESS');
    this.parseWarnings(data);
  },

  /**
   * Error-callback for rerun request
   * @method rerunChecksErrorCallback
   */
  rerunChecksErrorCallback: function () {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'FAILED');
    console.log('INFO: Getting host information(last_agent_env) from the server failed');
  },

  /**
   * Filter data for warnings parse
   * is data from host in bootStrap
   * @param {object} data
   * @return {Object}
   * @method filterBootHosts
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
   * Parse warnings data for each host and total
   * @param {object} data
   * @method parseWarnings
   */
  parseWarnings: function (data) {
    data = App.get('testMode') ? data : this.filterBootHosts(data);
    var warnings = [];
    var warning;
    var hosts = [];
    var warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {},
      alternativeWarnings: {}
    };
    var hostsPackagesData = this.get('hostsPackagesData');

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
          warning.hostsLong.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warningCategories.fileFoldersWarnings[path.name] = warning = {
            name: path.name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'fileFolders',
            onSingleHost: true
          };
        }
        host.warnings.push(warning);
      }, this);

      //parse all package warnings for host
      var _hostPackagesData = hostsPackagesData.findProperty('hostName', _host.Hosts.host_name);

      if (_hostPackagesData) {
        _hostPackagesData.installedPackages.forEach(function (_package) {
          warning = warningCategories.packagesWarnings[_package.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.version = _package.version;
            warning.onSingleHost = false;
          } else {
            warningCategories.packagesWarnings[_package.name] = warning = {
              name: _package.name,
              version: _package.version,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'packages',
              onSingleHost: true
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse all process warnings for host

      //todo: to be removed after check in new API
      var javaProcs = _host.Hosts.last_agent_env.hostHealth ? _host.Hosts.last_agent_env.hostHealth.activeJavaProcs : _host.Hosts.last_agent_env.javaProcs;
      if (javaProcs) {
        javaProcs.forEach(function (process) {
          warning = warningCategories.processesWarnings[process.pid];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.onSingleHost = false;
          } else {
            warningCategories.processesWarnings[process.pid] = warning = {
              name: (process.command.substr(0, 35) + '...'),
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
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
      }

      //parse all service warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.hostHealth && _host.Hosts.last_agent_env.hostHealth.liveServices) {
        _host.Hosts.last_agent_env.hostHealth.liveServices.forEach(function (service) {
          if (service.status === 'Unhealthy') {
            warning = warningCategories.servicesWarnings[service.name];
            if (warning) {
              warning.hosts.push(_host.Hosts.host_name);
              warning.hostsLong.push(_host.Hosts.host_name);
              warning.onSingleHost = false;
            } else {
              warningCategories.servicesWarnings[service.name] = warning = {
                name: service.name,
                hosts: [_host.Hosts.host_name],
                hostsLong: [_host.Hosts.host_name],
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
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.onSingleHost = false;
          } else {
            warningCategories.usersWarnings[user.userName] = warning = {
              name: user.userName,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'users',
              onSingleHost: true
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse misc warnings for host
      var umask = _host.Hosts.last_agent_env.umask;
      if (umask && umask > 23) {
        warning = warnings.filterProperty('category', 'misc').findProperty('name', umask);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: umask,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'misc',
            onSingleHost: true
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      var firewallRunning = _host.Hosts.last_agent_env.firewallRunning;
      if (firewallRunning !== null && firewallRunning) {
        var name = _host.Hosts.last_agent_env.firewallName + " Running";
        warning = warnings.filterProperty('category', 'firewall').findProperty('name', name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'firewall',
            onSingleHost: true
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      if (_host.Hosts.last_agent_env.alternatives) {
        _host.Hosts.last_agent_env.alternatives.forEach(function (alternative) {
          warning = warningCategories.alternativeWarnings[alternative.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.onSingleHost = false;
          } else {
            warningCategories.alternativeWarnings[alternative.name] = warning = {
              name: alternative.name,
              target: alternative.target,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'alternatives',
              onSingleHost: true
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      if (_host.Hosts.last_agent_env.reverseLookup === false) {
        var name = Em.I18n.t('installer.step3.hostWarningsPopup.reverseLookup.name');
        warning = warnings.filterProperty('category', 'reverseLookup').findProperty('name', name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
          warning.onSingleHost = false;
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'reverseLookup',
            onSingleHost: true
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      hosts.push(host);
    }, this);

    for (var categoryId in warningCategories) {
      var category = warningCategories[categoryId];
      for (var warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    hosts.unshift({
      name: 'All Hosts',
      warnings: warnings
    });
    this.set('warnings', warnings);
    this.set('warningsByHost', hosts);
  },

  /**
   * Open popup that contain hosts' warnings
   * @return {App.ModalPopup}
   * @method hostWarningsPopup
   */
  hostWarningsPopup: function () {
    var self = this;
    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.warnings.popup.header'),

      secondary: Em.I18n.t('installer.step3.hostWarningsPopup.rerunChecks'),

      primary: Em.I18n.t('common.close'),

      autoHeight: false,

      onPrimary: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onClose: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onSecondary: function () {
        self.rerunChecks();
      },

      didInsertElement: function () {
        this._super();
        this.fitHeight();
      },

      footerClass: App.WizardStep3HostWarningPopupFooter,

      bodyClass: App.WizardStep3HostWarningPopupBody
    });
  },

  /**
   * Show popup with registered hosts
   * @return {App.ModalPopup}
   * @method registeredHostsPopup
   */
  registeredHostsPopup: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('registeredHosts').length),
      secondary: null,
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step3/step3_registered_hosts_popup'),
        message: Em.I18n.t('installer.step3.registeredHostsPopup'),
        registeredHosts: self.get('registeredHosts')
      })
    })
  }

});
