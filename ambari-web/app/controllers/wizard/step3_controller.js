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
      var hosts = this.get('controller.hosts').filter(function(_host) {
        if (_host.get('bootStatus') == category.get('hostsBootStatus')) {
          return true;
        } else if (_host.get('bootStatus') == 'DONE' && category.get('hostsBootStatus') == 'REGISTERING') {
          return true;
        } else {
          return false;
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
    if(App.testMode){
      this.getHostInfo();
    }
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
        this.set('isSubmitDisabled', false);
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
      App.router.get('installerController.isStepDisabled').findProperty('step', 1).set('value', false);
      App.router.get('installerController.isStepDisabled').findProperty('step', 2).set('value', false);
    }
  }.observes('isInstallInProgress'),

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
      success: function (data) {
        if (data.hostsStatus !== null) {
          // in case of bootstrapping just one host, the server returns an object rather than an array, so
          // force into an array
          if (!(data.hostsStatus instanceof Array)) {
            data.hostsStatus = [ data.hostsStatus ];
          }
          console.log("TRACE: In success function for the GET bootstrap call");
          var keepPolling = self.parseHostInfo(data.hostsStatus);

          // Single host : if the only hostname is invalid (data.status == 'ERROR')
          // Multiple hosts : if one or more hostnames are invalid
          // following check will mark the bootStatus as 'FAILED' for the invalid hostname
          if (data.status == 'ERROR' || data.hostsStatus.length != self.get('bootHosts').length) {

            var hosts = self.get('bootHosts');

            for (var i = 0; i < hosts.length; i++) {

              var isValidHost = data.hostsStatus.someProperty('hostName', hosts[i].get('name'));
              if(hosts[i].get('bootStatus') !== 'REGISTERED'){
                if (!isValidHost) {
                  hosts[i].set('bootStatus', 'FAILED');
                  hosts[i].set('bootLog', 'Registration with the server failed.');
                }
              }
            }
          }

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
              self.set('registrationStartedAt', new Date().getTime());
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
        } else if (hosts.someProperty('bootStatus', 'RUNNING') || new Date().getTime() - self.get('registrationStartedAt') < self.get('registrationTimeoutSecs') * 1000) {
          // we want to keep polling for registration status if any of the hosts are still bootstrapping (so we check for RUNNING).
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
    var url = App.testMode ? '/data/wizard/bootstrap/two_hosts_information.json' : App.apiPrefix + '/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env';
    var method = 'GET';
    $.ajax({
      type: 'GET',
      url: url,
      contentType: 'application/json',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = (App.testMode) ? data : jQuery.parseJSON(data);
        self.parseWarnings(jsonData);
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
  /**
   * check warnings from server and put it in parsing
    */
  rerunChecks: function(){
    var self = this;
    var url = App.testMode ? '/data/wizard/bootstrap/two_hosts_information.json' : App.apiPrefix + '/hosts?fields=Hosts/last_agent_env';
    var currentProgress = 0;
    var interval = setInterval(function(){
      self.set('checksUpdateProgress', Math.ceil((++currentProgress/60)*100))
    }, 1000);
    setTimeout(function(){
      clearInterval(interval);
      $.ajax({
        type: 'GET',
        url: url,
        contentType: 'application/json',
        timeout: App.timeout,
        success: function (data) {
          var jsonData = (App.testMode) ? data : jQuery.parseJSON(data);
          self.set('checksUpdateProgress', 100);
          self.set('checksUpdateStatus', 'SUCCESS');
          self.parseWarnings(jsonData);
        },
        error: function () {
          self.set('checksUpdateProgress', 100);
          self.set('checksUpdateStatus', 'FAILED');
          console.log('INFO: Getting host information(last_agent_env) from the server failed');
        },
        statusCode: require('data/statusCodes')
      })
    }, this.get('warningsTimeInterval'));

  },
  warnings: [],
  warningsTimeInterval: 60000,
  /**
   * check are hosts have any warnings
   */
  isHostHaveWarnings: function(){
    var isWarning = false;
    this.get('warnings').forEach(function(warning){
      if(!isWarning && (warning.directoriesFiles.someProperty('isWarn', true) ||
      warning.packages.someProperty('isWarn', true) ||
      warning.processes.someProperty('isWarn', true))){
        isWarning = true;
      }
    }, this);
    return isWarning;
  }.property('warnings'),
  isWarningsBoxVisible: function(){
    return (App.testMode) ? true : !this.get('isSubmitDisabled');
  }.property('isSubmitDisabled'),
  checksUpdateProgress:0,
  checksUpdateStatus: null,
  /**
   * filter data for warnings parse
   * is data from host in bootStrap
   * @param data
   * @return {Object}
   */
  filterBootHosts: function(data){
    var bootHosts = this.get('bootHosts');
    var filteredData = {
      href: data.href,
      items: []
    };
    bootHosts.forEach(function(bootHost){
      data.items.forEach(function(host){
        if(host.Hosts.host_name == bootHost.get('name')){
          filteredData.items.push(host);
        }
      })
    })
    return filteredData;
  },
  /**
   * parse warnings data for each host and total
   * @param data
   */
  parseWarnings: function(data){
    data = this.filterBootHosts(data);
    var warnings = [];
    var totalWarnings = {
      hostName: 'All Hosts',
      directoriesFiles: [],
      packages: [],
      processes: []
    }
    //alphabetical sorting
    var sortingFunc = function(a, b){
      var a1= a.name, b1= b.name;
      if(a1== b1) return 0;
      return a1> b1? 1: -1;
    }
    data.items.forEach(function(host){
      var warningsByHost = {
        hostName: host.Hosts.host_name,
        directoriesFiles: [],
        packages: [],
        processes: []
      };

      //render all directories and files for each host
      host.Hosts.last_agent_env.paths.forEach(function(path){
        var parsedPath = {
          name: path.name,
          isWarn: (path.type == 'not_exist') ? false : true,
          message: (path.type == 'not_exist') ? 'OK' : 'WARN: already exists on host'
        }
        warningsByHost.directoriesFiles.push(parsedPath);
        // parsing total warnings
        if(!totalWarnings.directoriesFiles.someProperty('name', parsedPath.name)){
          totalWarnings.directoriesFiles.push({
            name:parsedPath.name,
            isWarn: parsedPath.isWarn,
            message: (parsedPath.isWarn) ? 'WARN: already exists on 1 host': 'OK',
            warnCount: (parsedPath.isWarn) ? 1 : 0
          })
        } else if(parsedPath.isWarn){
            totalWarnings.directoriesFiles.forEach(function(item, index){
              if(item.name == parsedPath.name){
                totalWarnings.directoriesFiles[index].isWarn = true;
                totalWarnings.directoriesFiles[index].warnCount++;
                totalWarnings.directoriesFiles[index].message = 'WARN: already exists on '+ totalWarnings.directoriesFiles[index].warnCount +' hosts';
              }
            });
        }
      }, this);

      //render all packages for each host
      host.Hosts.last_agent_env.rpms.forEach(function(_package){
        var parsedPackage = {
          name: _package.name,
          isWarn: _package.installed,
          message: (_package.installed) ? 'WARN: already installed on host' : 'OK'
        }
        warningsByHost.packages.push(parsedPackage);
        // parsing total warnings
        if(!totalWarnings.packages.someProperty('name', parsedPackage.name)){
          totalWarnings.packages.push({
            name:parsedPackage.name,
            isWarn: parsedPackage.isWarn,
            message: (parsedPackage.isWarn) ? 'WARN: already exists on 1 host': 'OK',
            warnCount: (parsedPackage.isWarn) ? 1 : 0
          })
        } else if(parsedPackage.isWarn){
          totalWarnings.packages.forEach(function(item, index){
            if(item.name == parsedPackage.name){
              totalWarnings.packages[index].isWarn = true;
              totalWarnings.packages[index].warnCount++;
              totalWarnings.packages[index].message = 'WARN: already exists on '+ totalWarnings.packages[index].warnCount +' hosts';
            }
          });
        }
      }, this);

      // render all process for each host
      host.Hosts.last_agent_env.javaProcs.forEach(function(process){
          var parsedProcess = {
            user: process.user,
            isWarn: process.hadoop,
            pid: process.pid,
            command: process.command,
            shortCommand: (process.command.substr(0, 15)+'...'),
            message: (process.hadoop) ? 'WARN: running on host' : 'OK'
          }
          warningsByHost.processes.push(parsedProcess);
          // parsing total warnings
          if(!totalWarnings.processes.someProperty('pid', parsedProcess.name)){
            totalWarnings.processes.push({
              user: process.user,
              pid: process.pid,
              command: process.command,
              shortCommand: (process.command.substr(0, 15)+'...'),
              isWarn: parsedProcess.isWarn,
              message: (parsedProcess.isWarn) ? 'WARN: running on 1 host': 'OK',
              warnCount: (parsedProcess.isWarn) ? 1 : 0
            })
          } else if(parsedProcess.isWarn){
            totalWarnings.processes.forEach(function(item, index){
              if(item.pid == parsedProcess.pid){
                totalWarnings.processes[index].isWarn = true;
                totalWarnings.processes[index].warnCount++;
                totalWarnings.processes[index].message = 'WARN: running on '+ totalWarnings.processes[index].warnCount +' hosts';
              }
            });
          }
      }, this);
      warningsByHost.directoriesFiles.sort(sortingFunc);
      warningsByHost.packages.sort(sortingFunc);
      warnings.push(warningsByHost);
    }, this);

    totalWarnings.directoriesFiles.sort(sortingFunc);
    totalWarnings.packages.sort(sortingFunc);
    warnings.unshift(totalWarnings);
    this.set('warnings', warnings);
  },
  /**
   * open popup that contain hosts' warnings
   * @param event
   */
  hostWarningsPopup: function(event){
    var self = this;
    App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.warnings.popup.header'),
      secondary: 'Rerun Checks',
      primary: 'Close',
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
        warnings: function(){
          return App.router.get('wizardStep3Controller.warnings');
        }.property('App.router.wizardStep3Controller.warnings'),
        categories: function(){
          var categories = this.get('warnings').getEach('hostName');
          return categories;
        }.property('warnings'),
        category: 'All Hosts',
        content: function(){
          return this.get('warnings').findProperty('hostName', this.get('category'));
        }.property('category', 'warnings'),
        /**
         * generate detailed content to show it in new window
         */
        contentInDetails: function(){
          var content = this.get('content');
          var newContent = '';
          if(content.hostName == 'All Hosts'){
            newContent += '<h4>Warnings across all hosts</h4>';
          } else {
            newContent += '<h4>Warnings on ' + content.hostName + '</h4>';
          }
          newContent += '<div>DIRECTORIES AND FILES</div><div>';
          content.directoriesFiles.filterProperty('isWarn', true).forEach(function(path){
              newContent += path.name + '&nbsp;'
          });
          if(content.directoriesFiles.filterProperty('isWarn', true).length == 0){
            newContent += 'No warnings';
          }
          newContent += '</div><br/><div>PACKAGES</div><div>';
          content.packages.filterProperty('isWarn', true).forEach(function(_package){
              newContent += _package.name + '&nbsp;'
          });
          if(content.packages.filterProperty('isWarn', true).length == 0){
            newContent += 'No warnings';
          }
          newContent += '</div><br/><div>PROCESSES</div><div>';
          content.processes.filterProperty('isWarn', true).forEach(function(process, index){
              newContent += '(' + content.hostName + ',' + process.pid + ',' + process.user + ')';
              newContent += (index != (content.processes.filterProperty('isWarn', true).length-1)) ? ',' : '';
          })
          if(content.processes.filterProperty('isWarn', true).length == 0){
            newContent += 'No warnings';
          }
          return newContent;
        }.property('content'),
        /**
         * open new browser tab with detailed content
         */
        openWarningsInDialog: function(){
          var newWindow = window.open('', this.get('category')+' warnings');
          var newDocument = newWindow.document;
          newDocument.write(this.get('contentInDetails'));
          newWindow.focus();
        }
      })
    })
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

