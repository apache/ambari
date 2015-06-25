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

App.ClusterController = Em.Controller.extend({
  name: 'clusterController',
  isLoaded: false,
  ambariProperties: null,
  clusterDataLoadedPercent: 'width:0', // 0 to 1

  isGangliaUrlLoaded: false,

  /**
   * Provides the URL to use for Ganglia server. This URL
   * is helpful in populating links in UI.
   *
   * If null is returned, it means GANGLIA service is not installed.
   */
  gangliaUrl: null,

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
    'hosts': false,
    'serviceMetrics': false,
    'stackComponents': false,
    'services': false,
    'cluster': false,
    'clusterStatus': false,
    'racks': false,
    'componentConfigs': false,
    'componentsState': false,
    'rootService': false,
    'alertDefinitions': false,
    'securityStatus': false
  }),

  /**
   * load cluster name
   */
  loadClusterName: function (reload) {
    var dfd = $.Deferred();

    if (App.get('clusterName') && !reload) {
      App.set('clusterName', this.get('clusterName'));
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

  setGangliaUrl: function () {
    if (App.get('testMode')) {
      this.set('gangliaUrl', 'http://gangliaserver/ganglia/?t=yes');
      this.set('isGangliaUrlLoaded', true);
    } else {
      // We want live data here
      var gangliaServer = App.HostComponent.find().findProperty('componentName', 'GANGLIA_SERVER');
      if (this.get('isLoaded') && gangliaServer) {
        this.set('isGangliaUrlLoaded', false);
        App.ajax.send({
          name: 'hosts.for_quick_links',
          sender: this,
          data: {
            clusterName: App.get('clusterName'),
            masterHosts: gangliaServer.get('hostName'),
            urlParams: ''
          },
          success: 'setGangliaUrlSuccessCallback'
        });
      }
    }
  }.observes('App.router.updateController.isUpdated', 'dataLoadList.hosts', 'gangliaWebProtocol', 'isLoaded'),

  setGangliaUrlSuccessCallback: function (response) {
    var url = null;
    if (response.items.length > 0) {
      url = this.get('gangliaWebProtocol') + "://" + (App.singleNodeInstall ? App.singleNodeAlias + ":42080" : response.items[0].Hosts.public_host_name) + "/ganglia";
    }
    this.set('gangliaUrl', url);
    this.set('isGangliaUrlLoaded', true);
  },

  gangliaWebProtocol: function () {
    var properties = this.get('ambariProperties');
    if (properties && properties.hasOwnProperty('ganglia.https') && properties['ganglia.https']) {
      return "https";
    } else {
      return "http";
    }
  }.property('ambariProperties'),

  isGangliaInstalled: function () {
    return !!App.Service.find().findProperty('serviceName', 'GANGLIA');
  }.property('App.router.updateController.isUpdated', 'dataLoadList.serviceMetrics'),

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

    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    var racksUrl = "/data/racks/racks.json";


    var hostsController = App.router.get('mainHostController');
    hostsController.set('isCountersUpdating', true);
    hostsController.updateStatusCounters();

    App.HttpClient.get(racksUrl, App.racksMapper, {
      complete: function (jqXHR, textStatus) {
        self.updateLoadStatus('racks');
      }
    }, function (jqXHR, textStatus) {
      self.updateLoadStatus('racks');
    });

    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete: function (jqXHR, textStatus) {
        self.updateLoadStatus('cluster');
      }
    }, function (jqXHR, textStatus) {
      self.updateLoadStatus('cluster');
    });

    if (App.get('testMode')) {
      self.updateLoadStatus('clusterStatus');
    } else {
      App.clusterStatus.updateFromServer().complete(function () {
        self.updateLoadStatus('clusterStatus');
        if (App.get('supports.stackUpgrade')) {
          self.restoreUpgradeState();
        }
      });
    }

    /**
     * Order of loading:
     * 1. load all created service components
     * 2. request for service components supported by stack
     * 3. load stack components to model
     * 4. request for services
     * 5. put services in cache
     * 6. request for hosts and host-components (single call)
     * 7. request for service metrics
     * 8. load host-components to model
     * 9. load hosts to model
     * 10. load services from cache with metrics to model
     * 11. update stale_configs of host-components (depends on App.supports.hostOverrides)
     * 12. load root service (Ambari)
     * 13. load alert definitions to model
     * 14. load unhealthy alert instances
     * 15. load security status
     */
    self.loadServiceComponents(function () {
      self.loadStackServiceComponents(function (data) {
        data.items.forEach(function (service) {
          service.StackServices.is_selected = true;
          service.StackServices.is_installed = false;
        }, self);
        App.stackServiceMapper.mapStackServices(data);
        App.config.setPreDefinedServiceConfigs(true);
        var updater = App.router.get('updateController');
        self.updateLoadStatus('stackComponents');
        updater.updateServices(function () {
          self.updateLoadStatus('services');
          //force clear filters  for hosts page to load all data
          App.db.setFilterConditions('mainHostController', null);

          updater.updateHost(function () {
            self.updateLoadStatus('hosts');
          });

          updater.updateServiceMetric(function () {
            App.config.loadConfigsFromStack(App.Service.find().mapProperty('serviceName')).complete(function () {
              updater.updateComponentConfig(function () {
                self.updateLoadStatus('componentConfigs');
              });

              updater.updateComponentsState(function () {
                self.updateLoadStatus('componentsState');
              });
              self.updateLoadStatus('serviceMetrics');

              updater.updateAlertGroups(function () {
                updater.updateAlertDefinitions(function () {
                  updater.updateAlertDefinitionSummary(function () {
                    updater.updateUnhealthyAlertInstances(function () {
                      self.updateLoadStatus('alertGroups');
                      self.updateLoadStatus('alertDefinitions');
                      self.updateLoadStatus('alertInstancesUnhealthy');
                    });
                  });
                });
              });
            });
          });
        });
        self.loadRootService().done(function (data) {
          App.rootServiceMapper.map(data);
          self.updateLoadStatus('rootService');
        });
        // load security status
        App.router.get('mainAdminKerberosController').getSecurityStatus().always(function () {
          self.updateLoadStatus('securityStatus');
        });
      });
    });
  },

  /**
   * restore upgrade status from server
   * and make call to get latest status from server
   */
  restoreUpgradeState: function () {
    this.getAllUpgrades().done(function (data) {
      var upgradeController = App.router.get('mainAdminStackAndUpgradeController');
      var lastUpgradeData = data.items.sortProperty('Upgrade.request_id').pop();
      var dbUpgradeState = App.db.get('MainAdminStackAndUpgrade', 'upgradeState');

      if (!Em.isNone(dbUpgradeState)) {
        App.set('upgradeState', dbUpgradeState);
      }

      if (lastUpgradeData) {
        upgradeController.setDBProperty('upgradeId', lastUpgradeData.Upgrade.request_id);
        upgradeController.setDBProperty('isDowngrade', lastUpgradeData.Upgrade.direction === 'DOWNGRADE');
        upgradeController.setDBProperty('upgradeState', lastUpgradeData.Upgrade.request_status);
        upgradeController.loadRepoVersionsToModel().done(function () {
          upgradeController.setDBProperty('upgradeVersion', App.RepositoryVersion.find().findProperty('repositoryVersion', lastUpgradeData.Upgrade.to_version).get('displayName'));
          upgradeController.initDBProperties();
          upgradeController.loadUpgradeData(true);
        });
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
   * Load data about created service components
   * @param callback
   */
  loadServiceComponents: function (callback) {
    App.ajax.send({
      name: 'service.components.load',
      sender: this,
      data: {
        callback: callback
      },
      success: 'loadStackServiceComponentsSuccess'
    });
  },

  /**
   * Callback for load service components request
   * @param data
   * @param request
   * @param params
   */
  loadStackServiceComponentsSuccess: function (data, request, params) {
    var serviceComponents = [];
    data.items.forEach(function (service) {
      serviceComponents = serviceComponents.concat(service.components.mapProperty('ServiceComponentInfo.component_name'));
    });
    App.serviceComponents = serviceComponents;
    params.callback();
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
   * @param adminPrincipalValue
   * @param adminPasswordValue
   * @param ajaxOpt
   * @returns {$.ajax}
   */
  createKerberosAdminSession: function (adminPrincipalValue, adminPasswordValue, ajaxOpt) {
    return App.ajax.send({
      name: 'common.cluster.update',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        data: [{
          session_attributes: {
            kerberos_admin: {principal: adminPrincipalValue, password: adminPasswordValue}
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
