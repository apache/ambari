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
  isManager: function() {
    return this.get('isAdmin') || this.get('isOperator');
  }.property('isAdmin','isOperator'),

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
  }.property('currentStackVersion'),

  allHostNames: [],

  currentStackVersionNumber: function () {
    var regExp = new RegExp(this.get('currentStackName') + '-');
    return (this.get('currentStackVersion') || this.get('defaultStackVersion')).replace(regExp, '');
  }.property('currentStackVersion', 'currentStackName'),

  isHadoop2Stack: function () {
    var result = true;
    var hdfsService = App.StackService.find().findProperty('serviceName','HDFS');
    if (hdfsService) {
      result = stringUtils.compareVersions(hdfsService.get('serviceVersion'), "2.0") > -1;
    } else {
      result = stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") > -1;
    }
    return result;
  }.property('router.clusterController.isLoaded', 'isStackServicesLoaded','currentStackVersionNumber'),

  isHadoop22Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.2") > -1);
  }.property('currentStackVersionNumber'),

  /**
   * If NameNode High Availability is enabled
   * Based on <code>clusterStatus.isInstalled</code>, stack version, <code>SNameNode</code> availability
   *
   * @type {bool}
   */
  isHaEnabled: function () {
    if (!this.get('isHadoop2Stack')) return false;
    var isHDFSInstalled = App.Service.find().findProperty('serviceName','HDFS');
    return !!isHDFSInstalled && !this.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE');
  }.property('router.clusterController.isLoaded', 'isHadoop2Stack'),

  /**
   * If ResourceManager High Availability is enabled
   * Based on number of ResourceManager components host components installed
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
  }.property('router.clusterController.isLoaded'),

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

    monitoring: function () {
      return App.StackService.find().filterProperty('isMonitoringService').mapProperty('serviceName');
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
    }.property('App.router.clusterController.isLoaded')
  })
});
