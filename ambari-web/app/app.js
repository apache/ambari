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

// Application bootstrapper
require('utils/ember_reopen');
var stringUtils = require('utils/string_utils');

module.exports = Em.Application.create({
  name: 'Ambari Web',
  rootElement: '#wrapper',

  store: DS.Store.create({
    revision: 4,
    adapter: DS.FixtureAdapter.create({
      simulateRemoteResponse: false
    }),
    typeMaps: {},
    recordCache: []
  }),
  isAdmin: false,
  isOperator: false,
  isPermissionDataLoaded: false,

  /**
   * state of stack upgrade process
   * states:
   *  - INIT
   *  - PENDING
   *  - IN_PROGRESS
   *  - HOLDING
   *  - COMPLETED
   * @type {String}
   */
  upgradeState: 'INIT',

  /**
   * flag is true when upgrade process is running
   * @returns {boolean}
   */
  upgradeInProgress: function() {
    return ["IN_PROGRESS"].contains(this.get('upgradeState'));
  }.property('upgradeState'),

  /**
   * flag is true when upgrade process is waiting for user action
   * to procced, retry, perform manual steps etc.
   * @returns {boolean}
   */
  upgradeHolding: function() {
    return this.get('upgradeState').contains("HOLDING");
  }.property('upgradeState'),

  /**
   * flag is true when upgrade process is aborted
   * @returns {boolean}
   */
  upgradeAborted: function () {
    return this.get('upgradeState') === "ABORTED";
  }.property('upgradeState'),

  /**
   * RU is running
   * @type {boolean}
   */
  upgradeIsRunning: function() {
    return this.get('upgradeInProgress') || this.get('upgradeHolding');
  }.property('upgradeInProgress', 'upgradeHolding'),

  /**
   * flag is true when upgrade process is running or aborted
   * @returns {boolean}
   */
  upgradeIsNotFinished: function () {
    return this.get('upgradeIsRunning') || this.get('upgradeAborted');
  }.property('upgradeIsRunning', 'upgradeAborted'),

  /**
   * compute user access rights by permission type
   * types:
   *  - ADMIN
   *  - MANAGER
   *  - OPERATOR
   *  - ONLY_ADMIN
   * prefix "upgrade_" mean that element will not be unconditionally blocked while stack upgrade running
   * @param type {string}
   * @return {boolean}
   */
  isAccessible: function (type) {
    if (!(this.get('upgradeState') == "ABORTED") &&
        !App.get('supports.opsDuringRollingUpgrade') &&
        !['INIT', 'COMPLETED'].contains(this.get('upgradeState')) &&
        !type.contains('upgrade_')) {
      return false;
    }

    if (type.contains('upgrade_')) {
      //slice off "upgrade_" prefix to have actual permission type
      type = type.slice(8);
    }

    switch (type) {
      case 'ADMIN':
        return this.get('isAdmin');
      case 'NON_ADMIN':
        return !this.get('isAdmin');
      case 'MANAGER':
        return this.get('isAdmin') || this.get('isOperator');
      case 'OPERATOR':
        return this.get('isOperator');
      case 'ONLY_ADMIN':
        return this.get('isAdmin') && !this.get('isOperator');
      default:
        return false;
    }
  },

  isStackServicesLoaded: false,
  /**
   * return url prefix with number value of version of HDP stack
   */
  stackVersionURL: function () {
    return '/stacks/{0}/versions/{1}'.format(this.get('currentStackName') || 'HDP', this.get('currentStackVersionNumber'));
  }.property('currentStackName','currentStackVersionNumber'),

  falconServerURL: function () {
    var falconService = this.Service.find().findProperty('serviceName', 'FALCON');
    if (falconService) {
      return falconService.get('hostComponents').findProperty('componentName', 'FALCON_SERVER').get('hostName');
    }
    return '';
  }.property().volatile(),

  /* Determine if Application Timeline Service supports Kerberization.
   * Because this value is retrieved from the cardinality of the component, it is safe to keep in app.js
   * since its value will not change during the lifetime of the application.
   */
  doesATSSupportKerberos: function() {
    var YARNService = App.StackServiceComponent.find().filterProperty('serviceName', 'YARN');
    if (YARNService.length) {
      var ATS = App.StackServiceComponent.find().findProperty('componentName', 'APP_TIMELINE_SERVER');
      return (!!ATS && !!ATS.get('minToInstall'));
    }
    return false;
  }.property('router.clusterController.isLoaded'),

  clusterName: null,
  clockDistance: null, // server clock - client clock
  currentStackVersion: '',
  currentStackName: function() {
    return Em.get((this.get('currentStackVersion') || this.get('defaultStackVersion')).match(/(.+)-\d.+/), '1');
  }.property('currentStackVersion', 'defaultStackVersion'),

  /**
   * true if cluster has only 1 host
   * for now is used to disable move/HA actions
   * @type {boolean}
   */
  isSingleNode: function() {
    return this.get('allHostNames.length') === 1;
  }.property('allHostNames.length'),

  allHostNames: [],

  uiOnlyConfigDerivedFromTheme: [],

  currentStackVersionNumber: function () {
    var regExp = new RegExp(this.get('currentStackName') + '-');
    return (this.get('currentStackVersion') || this.get('defaultStackVersion')).replace(regExp, '');
  }.property('currentStackVersion', 'defaultStackVersion', 'currentStackName'),

  isHadoop23Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.3") > -1);
  }.property('currentStackVersionNumber'),

  isHadoop22Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.2") > -1);
  }.property('currentStackVersionNumber'),

  /**
   * Determines if current stack is 2.0.*
   * @type {boolean}
   */
  isHadoop20Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.1") == -1 && stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") > -1);
  }.property('currentStackVersionNumber'),

  isHadoopWindowsStack: function() {
    return this.get('currentStackName') == "HDPWIN";
  }.property('currentStackName'),

  /**
   * when working with enhanced configs we should rely on stack version
   * as version that is below 2.2 doesn't supports it
   * even if flag <code>supports.enhancedConfigs<code> is true
   * @type {boolean}
   */
  isClusterSupportsEnhancedConfigs: function() {
    return this.get('isHadoop22Stack') && this.get('supports.enhancedConfigs');
  }.property('isHadoop22Stack', 'supports.enhancedConfigs'),

  /**
   * If NameNode High Availability is enabled
   * Based on <code>clusterStatus.isInstalled</code>, stack version, <code>SNameNode</code> availability
   *
   * @type {bool}
   */
  isHaEnabled: function () {
    var isHDFSInstalled = App.Service.find().findProperty('serviceName','HDFS');
    return !!isHDFSInstalled && !this.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE');
  }.property('router.clusterController.dataLoadList.services', 'router.clusterController.isServiceContentFullyLoaded'),

  /**
   * If ResourceManager High Availability is enabled
   * Based on number of ResourceManager host components installed
   *
   * @type {bool}
   */
  isRMHaEnabled: function () {
    var result = false;
    var rmStackComponent = App.StackServiceComponent.find().findProperty('componentName','RESOURCEMANAGER');
    if (rmStackComponent && rmStackComponent.get('isMultipleAllowed')) {
      result = this.HostComponent.find().filterProperty('componentName', 'RESOURCEMANAGER').length > 1;
    }
    return result;
  }.property('router.clusterController.isLoaded', 'isStackServicesLoaded'),

  /**
   * If Ranger Admin High Availability is enabled
   * Based on number of Ranger Admin host components installed
   *
   * @type {bool}
   */
  isRAHaEnabled: function () {
    var result = false;
    var raStackComponent = App.StackServiceComponent.find().findProperty('componentName','RANGER_ADMIN');
    if (raStackComponent && raStackComponent.get('isMultipleAllowed')) {
      result = App.HostComponent.find().filterProperty('componentName', 'RANGER_ADMIN').length > 1;
    }
    return result;
  }.property('router.clusterController.isLoaded', 'isStackServicesLoaded'),

  /**
   * Object with utility functions for list of service names with similar behavior
   */
  services: Em.Object.create({
    all: function () {
      return App.StackService.find().mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    clientOnly: function () {
      return App.StackService.find().filterProperty('isClientOnlyService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasClient: function () {
      return App.StackService.find().filterProperty('hasClient').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasMaster: function () {
      return App.StackService.find().filterProperty('hasMaster').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasSlave: function () {
      return App.StackService.find().filterProperty('hasSlave').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    noConfigTypes: function () {
      return App.StackService.find().filterProperty('isNoConfigTypes').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    servicesWithHeatmapTab: function () {
      return App.StackService.find().filterProperty('hasHeatmapSection').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    monitoring: function () {
      return App.StackService.find().filterProperty('isMonitoringService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hostMetrics: function () {
      return App.StackService.find().filterProperty('isHostMetricsService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    serviceMetrics: function () {
      return App.StackService.find().filterProperty('isServiceMetricsService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    supportsServiceCheck: function() {
      return App.StackService.find().filterProperty('serviceCheckSupported').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded')
  }),

  /**
   * List of components with allowed action for them
   * @type {Em.Object}
   */
  components: Em.Object.create({
    allComponents: function () {
      return App.StackServiceComponent.find().mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    reassignable: function () {
      return App.StackServiceComponent.find().filterProperty('isReassignable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    restartable: function () {
      return App.StackServiceComponent.find().filterProperty('isRestartable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    deletable: function () {
      return App.StackServiceComponent.find().filterProperty('isDeletable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    rollinRestartAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRollinRestartAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    decommissionAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isDecommissionAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    refreshConfigsAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRefreshConfigsAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableToHost: function () {
      return App.StackServiceComponent.find().filterProperty('isAddableToHost').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableMasterInstallerWizard: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterAddableInstallerWizard').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    multipleMasters: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterWithMultipleInstances').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    slaves: function () {
      return App.StackServiceComponent.find().filterProperty('isSlave').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    masters: function () {
      return App.StackServiceComponent.find().filterProperty('isMaster').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    clients: function () {
      return App.StackServiceComponent.find().filterProperty('isClient').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    nonHDP: function () {
      return App.StackServiceComponent.find().filterProperty('isNonHDPComponent').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded')
  })
});
