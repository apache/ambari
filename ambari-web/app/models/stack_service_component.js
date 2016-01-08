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
var numberUtils = require('utils/number_utils');
/**
 * This model loads all serviceComponents supported by the stack
 * @type {*}
 */
App.StackServiceComponent = DS.Model.extend({
  componentName: DS.attr('string'),
  displayName: DS.attr('string'),
  cardinality: DS.attr('string'),
  customCommands: DS.attr('array'),
  dependencies: DS.attr('array'),
  serviceName: DS.attr('string'),
  componentCategory: DS.attr('string'),
  isMaster: DS.attr('boolean'),
  isClient: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  stackService: DS.belongsTo('App.StackService'),
  serviceComponentId: DS.attr('number', {defaultValue: 1}), // this is used on Assign Master page for multiple masters

  /**
   * Minimum required count for installation.
   *
   * @property {Number} minToInstall
   **/
  minToInstall: function() {
    return numberUtils.getCardinalityValue(this.get('cardinality'), false);
  }.property('cardinality'),

  /**
   * Maximum required count for installation.
   *
   * @property {Number} maxToInstall
   **/
  maxToInstall: function() {
    return numberUtils.getCardinalityValue(this.get('cardinality'), true);
  }.property('cardinality'),

  /** @property {Boolean} isRequired - component required to install **/
  isRequired: function() {
    return this.get('minToInstall') > 0;
  }.property('cardinality'),

  /** @property {Boolean} isMultipleAllowed - component can be assigned for more than one host **/
  isMultipleAllowed: function() {
    return this.get('maxToInstall') > 1;
  }.property('cardinality'),

  /** @property {Boolean} isSlave **/
  isSlave: function() {
   return this.get('componentCategory') === 'SLAVE';
  }.property('componentCategory'),

  /** @property {Boolean} isRestartable - component supports restart action **/
  isRestartable: function() {
    return !this.get('isClient');
  }.property('isClient'),

  /** @property {Boolean} isReassignable - component supports reassign action **/
  isReassignable: function() {
    return ['NAMENODE', 'SECONDARY_NAMENODE', 'JOBTRACKER', 'RESOURCEMANAGER', 'APP_TIMELINE_SERVER', 'OOZIE_SERVER', 'WEBHCAT_SERVER', 'HIVE_SERVER', 'HIVE_METASTORE', 'MYSQL_SERVER', 'METRICS_COLLECTOR'].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isNonHDPComponent - component not belongs to HDP services **/
  isNonHDPComponent: function() {
    return ['METRICS_COLLECTOR', 'METRICS_MONITOR'].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isRollinRestartAllowed - component supports rolling restart action **/
  isRollinRestartAllowed: function() {
    return this.get('isSlave') || this.get('componentName') === 'KAFKA_BROKER';
  }.property('componentName'),

  /** @property {Boolean} isDecommissionAllowed - component supports decommission action **/
  isDecommissionAllowed: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER"].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isRefreshConfigsAllowed - component supports refresh configs action **/
  isRefreshConfigsAllowed: function() {
    return ["FLUME_HANDLER"].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isAddableToHost - component can be added on host details page **/
  isAddableToHost: function() {
    return this.get('isMasterAddableInstallerWizard') || ((this.get('isNotAddableOnlyInInstall') || this.get('isSlave') || this.get('isClient')) && !this.get('isHAComponentOnly'));
  }.property('componentName'),

  /** @property {Boolean} isDeletable - component supports delete action **/
  isDeletable: function() {
    var ignored = [];
    return this.get('isAddableToHost') && !ignored.contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isShownOnInstallerAssignMasterPage - component visible on "Assign Masters" step of Install Wizard **/
  isShownOnInstallerAssignMasterPage: function() {
    var component = this.get('componentName');
    var mastersNotShown = ['MYSQL_SERVER', 'POSTGRESQL_SERVER'];
    return this.get('isMaster') && !mastersNotShown.contains(component);
  }.property('isMaster','componentName'),

  /** @property {Boolean} isShownOnInstallerSlaveClientPage - component visible on "Assign Slaves and Clients" step of Install Wizard**/
  isShownOnInstallerSlaveClientPage: function() {
    var component = this.get('componentName');
    var slavesNotShown = ['JOURNALNODE','ZKFC','APP_TIMELINE_SERVER'];
    return this.get('isSlave') && !this.get('isRequiredOnAllHosts') && !slavesNotShown.contains(component);
  }.property('isSlave','componentName', 'isRequiredOnAllHosts'),

  /** @property {Boolean} isShownOnAddServiceAssignMasterPage - component visible on "Assign Masters" step of Add Service Wizard **/
  isShownOnAddServiceAssignMasterPage: function() {
    var isVisible = this.get('isShownOnInstallerAssignMasterPage');
    if (App.get('isHaEnabled')) {
      isVisible =  isVisible && this.get('componentName') !== 'SECONDARY_NAMENODE';
    }
    return isVisible;
  }.property('isShownOnInstallerAssignMasterPage','App.isHaEnabled'),

  /** @property {Boolean} isMasterWithMultipleInstances **/
  isMasterWithMultipleInstances: function() {
    // @todo: safe removing JOURNALNODE from masters list
    return (this.get('isMaster') && this.get('isMultipleAllowed')) || this.get('componentName') == 'JOURNALNODE';
  }.property('componentName'),

  /**
   * Master component list that could be assigned for more than 1 host.
   * Some components like NameNode and ResourceManager have range cardinality value, so they are excluded using isMasterAddableOnlyOnHA property
   *
   * @property {Boolean} isMasterAddableInstallerWizard
   **/
  isMasterAddableInstallerWizard: function() {
    return this.get('isMaster') && this.get('isMultipleAllowed') && !this.get('isMasterAddableOnlyOnHA') && !this.get('isNotAddableOnlyInInstall');
  }.property('componentName'),

  /**
   * Master components with cardinality more than 1 (n+ or n-n) that could not be added in wizards
   * New instances of these components are added in appropriate HA wizards
   * @property {Boolean} isMasterAddableOnlyOnHA
   */
  isMasterAddableOnlyOnHA: function () {
    return ['NAMENODE', 'RESOURCEMANAGER', 'RANGER_ADMIN'].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isHAComponentOnly - Components that can be installed only if HA enabled **/
  isHAComponentOnly: function() {
    var HAComponentNames = ['ZKFC','JOURNALNODE'];
    return HAComponentNames.contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isRequiredOnAllHosts - Is It require to install the components on all hosts. used in step-6 wizard controller **/
  isRequiredOnAllHosts: function() {
    return this.get('minToInstall') == Infinity;
  }.property('stackService','isSlave'),

  /** @property {Number} defaultNoOfMasterHosts - default number of master hosts on Assign Master page: **/
  defaultNoOfMasterHosts: function() {
     if (this.get('isMasterAddableInstallerWizard')) {
       return this.get('componentName') == 'ZOOKEEPER_SERVER' ? 3 : this.get('minToInstall');
     }
  }.property('componentName'),

  /** @property {Boolean} coHostedComponents - components that are co-hosted with this component **/
  coHostedComponents: function() {
    var componentName = this.get('componentName');
    var key, coHostedComponents = [];
    for (key in App.StackServiceComponent.coHost) {
      if (App.StackServiceComponent.coHost[key] === componentName) {
        coHostedComponents.push(key)
      }
    }
    return coHostedComponents;
  }.property('componentName'),

  /** @property {Boolean} isOtherComponentCoHosted - Is any other component co-hosted with this component **/
  isOtherComponentCoHosted: function() {
    return !!this.get('coHostedComponents').length;
  }.property('coHostedComponents'),

  /** @property {Boolean} isCoHostedComponent - Is this component co-hosted with other component **/
  isCoHostedComponent: function() {
    var componentName = this.get('componentName');
    return !!App.StackServiceComponent.coHost[componentName];
  }.property('componentName'),

  /** @property {Boolean} isNotAddableOnlyInInstall - is this component addable, except Install and Add Service Wizards  **/
  isNotAddableOnlyInInstall: function() {
    return ['HIVE_METASTORE', 'HIVE_SERVER', 'RANGER_KMS_SERVER', 'OOZIE_SERVER'].contains(this.get('componentName'));
  }.property('componentName'),

  /** @property {Boolean} isNotAllowedOnSingleNodeCluster - is this component allowed on single node  **/
  isNotAllowedOnSingleNodeCluster: function() {
    return ['HAWQSTANDBY'].contains(this.get('componentName'));
   }.property('componentName')

});

App.StackServiceComponent.FIXTURES = [];

App.StackServiceComponent.coHost = {
  'WEBHCAT_SERVER': 'HIVE_SERVER'
};
