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
var stringUtils = require('utils/string_utils');
var credentialUtils = require('utils/credentials');

App.ClusterController = Em.Controller.extend({
  name: 'clusterController',
  isLoaded: false,
  ambariProperties: null,
  clusterDataLoadedPercent: 'width:0', // 0 to 1

  isClusterNameLoaded: false,

  isAlertsLoaded: false,

  isComponentsStateLoaded: false,

  isHostsLoaded: false,

  isConfigsPropertiesLoaded: false,

  isComponentsConfigLoaded: false,

  isStackConfigsLoaded: false,

  isServiceMetricsLoaded: false,

  /**
   * @type {boolean}
   */
  isHostComponentMetricsLoaded: false,

  /**
   * Ambari uses custom jdk.
   * @type {Boolean}
   */
  isCustomJDK: false,

  isHostContentLoaded: function () {
    return this.get('isHostsLoaded') && this.get('isComponentsStateLoaded');
  }.property('isHostsLoaded', 'isComponentsStateLoaded'),

  isServiceContentFullyLoaded: function () {
    return this.get('isServiceMetricsLoaded') && this.get('isComponentsStateLoaded') && this.get('isComponentsConfigLoaded');
  }.property('isServiceMetricsLoaded', 'isComponentsStateLoaded', 'isComponentsConfigLoaded'),

  clusterName: function () {
    return App.get('clusterName');
  }.property('App.clusterName'),

  updateLoadStatus: function (item) {
    var loadList = this.get('dataLoadList');
    var loaded = true;
    var numLoaded = 0;
    var loadListLength = 0;
    loadList.set(item, true);
    for (var i in loadList) {
      if (loadList.hasOwnProperty(i)) {
        loadListLength++;
        if (!loadList[i] && loaded) {
          loaded = false;
        }
      }
      // calculate the number of true
      if (loadList.hasOwnProperty(i) && loadList[i]) {
        numLoaded++;
      }
    }
    this.set('isLoaded', loaded);
    this.set('clusterDataLoadedPercent', 'width:' + (Math.floor(numLoaded / loadListLength * 100)).toString() + '%');
  },

  dataLoadList: Em.Object.create({
    'stackComponents': false,
    'services': false
  }),

  /**
   * load cluster name
   */
  loadClusterName: function (reload) {
    var dfd = $.Deferred();

    if (App.get('clusterName') && !reload) {
      App.set('clusterName', this.get('clusterName'));
      this.set('isClusterNameLoaded', true);
      dfd.resolve();
    } else {
      App.ajax.send({
        name: 'cluster.load_cluster_name',
        sender: this,
        success: 'loadClusterNameSuccessCallback',
        error: 'loadClusterNameErrorCallback'
      }).complete(function () {
        if (!App.get('currentStackVersion')) {
          App.set('currentStackVersion', App.defaultStackVersion);
        }
        dfd.resolve();
      });
    }
    return dfd.promise()
  },

  loadClusterNameSuccessCallback: function (data) {
    if (data.items && data.items.length > 0) {
      App.set('clusterName', data.items[0].Clusters.cluster_name);
      App.set('currentStackVersion', data.items[0].Clusters.version);
      this.set('isClusterNameLoaded', true);
      App.set('isKerberosEnabled', data.items[0].Clusters.security_type === 'KERBEROS');
    }
  },

  loadClusterNameErrorCallback: function (request, ajaxOptions, error) {
    console.log('failed on loading cluster name');
    this.set('isLoaded', true);
  },

  /**
   * load current server clock in milli-seconds
   */
  loadClientServerClockDistance: function () {
    var dfd = $.Deferred();
    this.getServerClock().done(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },

  getServerClock: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields: '?fields=RootServiceComponents/server_clock'
      },
      success: 'getServerClockSuccessCallback',
      error: 'getServerClockErrorCallback'
    });
  },
  getServerClockSuccessCallback: function (data) {
    var clientClock = new Date().getTime();
    var serverClock = (data.RootServiceComponents.server_clock).toString();
    serverClock = serverClock.length < 13 ? serverClock + '000' : serverClock;
    App.set('clockDistance', serverClock - clientClock);
    App.set('currentServerTime', parseInt(serverClock));
    console.log('loading ambari server clock distance');
  },
  getServerClockErrorCallback: function () {
    console.log('Cannot load ambari server clock');
  },

  getUrl: function (testUrl, url) {
    return (App.get('testMode')) ? testUrl : App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + url;
  },

  /**
   *  load all data and update load status
   */
  loadClusterData: function () {
    var self = this;
    this.getAllHostNames();
    this.loadAmbariProperties();
    if (!App.get('clusterName')) {
      return;
    }

    if (this.get('isLoaded')) { // do not load data repeatedly
      App.router.get('mainController').startPolling();
      return;
    }
    App.router.get('userSettingsController').getAllUserSettings();
    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    var hostsController = App.router.get('mainHostController');
    hostsController.set('isCountersUpdating', true);
    hostsController.updateStatusCounters();

    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete: function (jqXHR, textStatus) {
        App.set('isCredentialStorePersistent', Em.getWithDefault(App.Cluster.find().findProperty('clusterName', App.get('clusterName')), 'isCredentialStorePersistent', false));
      }
    }, function (jqXHR, textStatus) {
    });


    if (App.get('supports.stackUpgrade')) {
      self.restoreUpgradeState();
    }


    var updater = App.router.get('updateController');

    /**
     * Order of loading:
     * 1. load all created service components
     * 1. request for service components supported by stack
     * 2. load stack components to model
     * 3. request for services
     * 4. put services in cache
     * 5. request for hosts and host-components (single call)
     * 6. request for service metrics
     * 7. load host-components to model
     * 8. load services from cache with metrics to model
     */
    self.loadStackServiceComponents(function (data) {
      data.items.forEach(function (service) {
        service.StackServices.is_selected = true;
        service.StackServices.is_installed = false;
      }, self);
      App.stackServiceMapper.mapStackServices(data);
      App.config.setPreDefinedServiceConfigs(true);
      self.updateLoadStatus('stackComponents');
      updater.updateServices(function () {
        self.updateLoadStatus('services');

        //hosts should be loaded after services in order to properly populate host-component relation in App.cache.services
        updater.updateHost(function () {
          self.set('isHostsLoaded', true);
        });
        App.config.loadConfigsFromStack(App.Service.find().mapProperty('serviceName')).complete(function () {
          App.config.loadClusterConfigsFromStack().complete(function () {
            self.set('isConfigsPropertiesLoaded', true);
          });
        });
        // components state loading doesn't affect overall progress
        updater.updateComponentsState(function () {
          self.set('isComponentsStateLoaded', true);
          // service metrics should be loaded after components state for mapping service components to service in the DS model
          // service metrics loading doesn't affect overall progress
          updater.updateServiceMetric(function () {
            self.set('isServiceMetricsLoaded', true);
            // make second call, because first is light since it doesn't request host-component metrics
            updater.updateServiceMetric(function() {
              self.set('isHostComponentMetricsLoaded', true);
            });
            // components config loading doesn't affect overall progress
            updater.updateComponentConfig(function () {
              self.set('isComponentsConfigLoaded', true);
            });
          });
        });
      });
    });

    //force clear filters  for hosts page to load all data
    App.db.setFilterConditions('mainHostController', null);

    // alerts loading doesn't affect overall progress
    console.time('Overall alerts loading time');
    updater.updateAlertGroups(function () {
      updater.updateAlertDefinitions(function () {
        updater.updateAlertDefinitionSummary(function () {
          updater.updateUnhealthyAlertInstances(function () {
            console.timeEnd('Overall alerts loading time');
            self.set('isAlertsLoaded', true);
          });
        });
      });
    });

    /*  Root service mapper maps all the data exposed under Ambari root service which includes ambari configurations i.e ambari-properties
     ** This is useful information but its not being used in the code anywhere as of now

     self.loadRootService().done(function (data) {
     App.rootServiceMapper.map(data);
     self.updateLoadStatus('rootService');
     });

     */
  },

  /**
   * restore upgrade status from server
   * and make call to get latest status from server
   */
  restoreUpgradeState: function () {
    return this.getAllUpgrades().done(function (data) {
      var upgradeController = App.router.get('mainAdminStackAndUpgradeController');
      var lastUpgradeData = data.items.sortProperty('Upgrade.request_id').pop();
      var dbUpgradeState = App.db.get('MainAdminStackAndUpgrade', 'upgradeState');

      if (!Em.isNone(dbUpgradeState)) {
        App.set('upgradeState', dbUpgradeState);
      }

      if (lastUpgradeData) {
        upgradeController.restoreLastUpgrade(lastUpgradeData);
      } else {
        upgradeController.initDBProperties();
        upgradeController.loadUpgradeData(true);
      }
      upgradeController.loadStackVersionsToModel(true).done(function () {
        App.set('stackVersionsAvailable', App.StackVersion.find().content.length > 0);
      });
    });
  },

  loadRootService: function () {
    return App.ajax.send({
      name: 'service.ambari',
      sender: this
    });
  },

  requestHosts: function (realUrl, callback) {
    var testHostUrl = '/data/hosts/HDP2/hosts.json';
    var url = this.getUrl(testHostUrl, realUrl);
    App.HttpClient.get(url, App.hostsMapper, {
      complete: callback
    }, callback)
  },

  /**
   *
   * @param callback
   */
  loadStackServiceComponents: function (callback) {
    var callbackObj = {
      loadStackServiceComponentsSuccess: callback
    };
    App.ajax.send({
      name: 'wizard.service_components',
      data: {
        stackUrl: App.get('stackVersionURL'),
        stackVersion: App.get('currentStackVersionNumber')
      },
      sender: callbackObj,
      success: 'loadStackServiceComponentsSuccess'
    });
  },

  loadAmbariProperties: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      success: 'loadAmbariPropertiesSuccess',
      error: 'loadAmbariPropertiesError'
    });
  },

  loadAmbariPropertiesSuccess: function (data) {
    console.log('loading ambari properties');
    this.set('ambariProperties', data.RootServiceComponents.properties);
    // Absence of 'jdk.name' and 'jce.name' properties says that ambari configured with custom jdk.
    this.set('isCustomJDK', App.isEmptyObject(App.permit(data.RootServiceComponents.properties, ['jdk.name', 'jce.name'])));
    App.router.get('mainController').monitorInactivity();
  },

  loadAmbariPropertiesError: function () {
    console.warn('can\'t get ambari properties');
  },

  updateClusterData: function () {
    var testUrl = '/data/clusters/HDP2/cluster.json';
    var clusterUrl = this.getUrl(testUrl, '?fields=Clusters');
    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete: function () {
      }
    });
  },

  /**
   *
   * @returns {*|Transport|$.ajax|boolean|ServerResponse}
   */
  getAllHostNames: function () {
    return App.ajax.send({
      name: 'hosts.all',
      sender: this,
      success: 'getHostNamesSuccess',
      error: 'getHostNamesError'
    });
  },

  getHostNamesSuccess: function (data) {
    App.set("allHostNames", data.items.mapProperty("Hosts.host_name"));
  },

  getHostNamesError: function () {
    console.error('failed to load hostNames');
  },


  /**
   * puts kerberos admin credentials in the live cluster session
   * and resend ajax request
   * @param {credentialResourceObject} credentialResource
   * @param {object} ajaxOpt
   * @returns {$.ajax}
   */
  createKerberosAdminSession: function (credentialResource, ajaxOpt) {
    if (App.get('supports.storeKDCCredentials')) {
      return credentialUtils.createOrUpdateCredentials(App.get('clusterName'), credentialUtils.ALIAS.KDC_CREDENTIALS, credentialResource).then(function() {
        if (ajaxOpt) {
          $.ajax(ajaxOpt);
        }
      });
    }

    return App.ajax.send({
      name: 'common.cluster.update',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        data: [{
          session_attributes: {
            kerberos_admin: {principal: credentialResource.principal, password: credentialResource.key}
          }
        }]
      }
    }).success(function () {
      if (ajaxOpt) {
        $.ajax(ajaxOpt);
      }
    });
  },

  //TODO Replace this check with any other which is applicable to non-HDP stack
  /**
   * Check if HDP stack version is more or equal than 2.2.2 to determine if pluggable metrics for Storm are supported
   * @method checkDetailedRepoVersion
   * @returns {promise|*|promise|promise|HTMLElement|promise}
   */
  checkDetailedRepoVersion: function () {
    var dfd;
    var currentStackName = App.get('currentStackName');
    var currentStackVersionNumber = App.get('currentStackVersionNumber');
    if (currentStackName == 'HDP' && currentStackVersionNumber == '2.2') {
      dfd = App.ajax.send({
        name: 'cluster.load_detailed_repo_version',
        sender: this,
        success: 'checkDetailedRepoVersionSuccessCallback',
        error: 'checkDetailedRepoVersionErrorCallback'
      });
    } else {
      dfd = $.Deferred();
      App.set('isStormMetricsSupported', currentStackName != 'HDP' || stringUtils.compareVersions(currentStackVersionNumber, '2.2') == 1);
      dfd.resolve();
    }
    return dfd.promise();
  },

  checkDetailedRepoVersionSuccessCallback: function (data) {
    var items = data.items;
    var version;
    if (items && items.length) {
      var repoVersions = items[0].repository_versions;
      if (repoVersions && repoVersions.length) {
        version = Em.get(repoVersions[0], 'RepositoryVersions.repository_version');
      }
    }
    App.set('isStormMetricsSupported', stringUtils.compareVersions(version, '2.2.2') > -1 || !version);
  },
  checkDetailedRepoVersionErrorCallback: function () {
    App.set('isStormMetricsSupported', true);
  },

  /**
   * Load required data for all upgrades from API
   * @returns {$.ajax}
   */
  getAllUpgrades: function () {
    return App.ajax.send({
      name: 'cluster.load_last_upgrade',
      sender: this
    });
  }
});
