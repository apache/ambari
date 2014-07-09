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
    })
  }),
  isAdmin: false,
  /**
   * return url prefix with number value of version of HDP stack
   */
  stackVersionURL: function () {
    var stackVersion = this.get('currentStackVersion') || this.get('defaultStackVersion');
    if (stackVersion.indexOf('HDPLocal') !== -1) {
      return '/stacks/HDPLocal/versions/' + stackVersion.replace(/HDPLocal-/g, '');
    }
    return '/stacks/HDP/versions/' + stackVersion.replace(/HDP-/g, '');
  }.property('currentStackVersion'),

  /**
   * return url prefix with number value of version of HDP stack
   */
  stack2VersionURL: function () {
    var stackVersion = this.get('currentStackVersion') || this.get('defaultStackVersion');
    if (stackVersion.indexOf('HDPLocal') !== -1) {
      return '/stacks2/HDPLocal/versions/' + stackVersion.replace(/HDPLocal-/g, '');
    }
    return '/stacks2/HDP/versions/' + stackVersion.replace(/HDP-/g, '');
  }.property('currentStackVersion'),

  falconServerURL: function () {
    var falconService = this.Service.find().findProperty('serviceName', 'FALCON');
    if (falconService) {
      return falconService.get('hostComponents').findProperty('componentName', 'FALCON_SERVER').get('host.hostName');
    }
    return '';
  }.property().volatile(),

  clusterName: null,
  clockDistance: null, // server clock - client clock
  currentStackVersion: '',
  currentStackVersionNumber: function () {
    return this.get('currentStackVersion').replace(/HDP(Local)?-/, '');
  }.property('currentStackVersion'),
  isHadoop2Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") === 1 ||
      stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.0") === 0)
  }.property('currentStackVersionNumber'),
  isHadoop21Stack: function () {
    return (stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.1") === 1 ||
      stringUtils.compareVersions(this.get('currentStackVersionNumber'), "2.1") === 0)
  }.property('currentStackVersionNumber'),

  /**
   * If High Availability is enabled
   * Based on <code>clusterStatus.isInstalled</code>, stack version, <code>SNameNode</code> availability
   *
   * @type {bool}
   */
  isHaEnabled: function () {
    if (!this.get('isHadoop2Stack')) return false;
    return !this.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE');
  }.property('router.clusterController.isLoaded', 'isHadoop2Stack'),

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

    noConfigTypes: function () {
      return App.StackService.find().filterProperty('isNoConfigTypes').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),
    monitoring: function () {
      return App.StackService.find().filterProperty('isMonitoringService').mapProperty('serviceName');
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
      return App.StackServiceComponent.find().filterProperty('isReassignable', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    restartable: function () {
      return App.StackServiceComponent.find().filterProperty('isRestartable', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    deletable: function () {
      return App.StackServiceComponent.find().filterProperty('isDeletable', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    rollinRestartAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRollinRestartAllowed', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    decommissionAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isDecommissionAllowed', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    refreshConfigsAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRefreshConfigsAllowed', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableToHost: function () {
      return App.StackServiceComponent.find().filterProperty('isAddableToHost', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableMasterInstallerWizard: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterWithMultipleInstances', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableMasterHaWizard: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterWithMultipleInstancesHaWizard', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    slaves: function () {
      return App.StackServiceComponent.find().filterProperty('isMaster', false).filterProperty('isClient', false).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    masters: function () {
      return App.StackServiceComponent.find().filterProperty('isMaster', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    clients: function () {
      return App.StackServiceComponent.find().filterProperty('isClient', true).mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded')
  })
});
