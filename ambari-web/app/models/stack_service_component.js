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
/**
 * This model loads all serviceComponents supported by the stack
 * @type {*}
 */
App.StackServiceComponent = DS.Model.extend({
  componentName: DS.attr('string'),
  dependencies: DS.attr('array'),
  serviceName: DS.attr('string'),
  componentCategory: DS.attr('string'),
  isMaster: DS.attr('boolean'),
  isClient: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  stackService: DS.belongsTo('App.StackService'),
  serviceComponentId: DS.attr('number', {defaultValue: 1}), // this is used on Assign Master page for multiple masters

  displayName: function() {
    if (App.format.role(this.get('componentName'))) {
      return App.format.role(this.get('componentName'));
    } else {
      return this.get('componentName');
    }
  }.property('componentName'),

  isSlave: function() {
   return this.get('componentCategory') === 'SLAVE';
  }.property('componentCategory'),

  isRestartable: function() {
    return !this.get('isClient');
  }.property('isClient'),

  isReassignable: function() {
    return ['NAMENODE', 'SECONDARY_NAMENODE', 'JOBTRACKER', 'RESOURCEMANAGER'].contains(this.get('componentName'));
  }.property('componentName'),

  isDeletable: function() {
    return ['SUPERVISOR', 'HBASE_MASTER', 'DATANODE', 'TASKTRACKER', 'NODEMANAGER', 'HBASE_REGIONSERVER', 'GANGLIA_MONITOR', 'ZOOKEEPER_SERVER'].contains(this.get('componentName'));
  }.property('componentName'),

  isRollinRestartAllowed: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "SUPERVISOR"].contains(this.get('componentName'));
  }.property('componentName'),

  isDecommissionAllowed: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER"].contains(this.get('componentName'));
  }.property('componentName'),

  isRefreshConfigsAllowed: function() {
    return ["FLUME_HANDLER"].contains(this.get('componentName'));
  }.property('componentName'),

  isAddableToHost: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "HBASE_MASTER", "ZOOKEEPER_SERVER", "SUPERVISOR", "GANGLIA_MONITOR"].contains(this.get('componentName'));
  }.property('componentName'),

  isShownOnInstallerAssignMasterPage: function() {
    var component = this.get('componentName');
    var mastersNotShown = ['MYSQL_SERVER'];
    return ((this.get('isMaster') && !mastersNotShown.contains(component)) || component === 'APP_TIMELINE_SERVER');
  }.property('isMaster','componentName'),

  isShownOnInstallerSlaveClientPage: function() {
    var component = this.get('componentName');
    var slavesNotShown = ['JOURNALNODE','ZKFC','APP_TIMELINE_SERVER','GANGLIA_MONITOR'];
    return this.get('isSlave') && !slavesNotShown.contains(component);
  }.property('isSlave','componentName'),

  isShownOnAddServiceAssignMasterPage: function() {
    var isVisible = this.get('isShownOnInstallerAssignMasterPage');
    if (App.get('isHaEnabled')) {
      isVisible =  isVisible && this.get('componentName') !== 'SECONDARY_NAMENODE';
    }
    return isVisible;
  }.property('isShownOnInstallerAssignMasterPage','App.isHaEnabled'),

  isMasterWithMultipleInstances: function() {
    var masters = ['ZOOKEEPER_SERVER', 'HBASE_MASTER', 'NAMENODE', 'JOURNALNODE'];
    return masters.contains(this.get('componentName'));
  }.property('componentName'),

  isMasterAddableInstallerWizard: function() {
    var masters = ['ZOOKEEPER_SERVER', 'HBASE_MASTER'];
    return masters.contains(this.get('componentName'));
  }.property('componentName'),

  /** Some non master components can be assigned as master **/
  isMasterBehavior: function() {
    var componentsName = ['APP_TIMELINE_SERVER'];
    return componentsName.contains(this.get('componentName'));
  }.property('componentName'),

  /** Some non client components can be assigned as clients **/
  isClientBehavior: function() {
    var componentName = ['GANGLIA_MONITOR'];
    return componentName.contains(this.get('componentName'));
  }.property('componentName'),

  /** Components that can be installed only if HA enabled **/
  isHAComponentOnly: function() {
    var HAComponentNames = ['ZKFC','JOURNALNODE'];
    return HAComponentNames.contains(this.get('componentName'));
  }.property('componentName'),

  // Is It require to install the components on all hosts. used in step-6 wizard controller
  isRequiredOnAllHosts: function() {
    var service = this.get('stackService');
    return service.get('isMonitoringService') && this.get('isSlave') ;
  }.property('stackService','isSlave'),

  // components that are not to be installed with ambari server
  isNotPreferableOnAmbariServerHost: function() {
    var service = ['STORM_UI_SERVER', 'DRPC_SERVER', 'STORM_REST_API', 'NIMBUS', 'GANGLIA_SERVER', 'NAGIOS_SERVER', 'HUE_SERVER'];
    return service.contains(this.get('componentName'));
  }.property('componentName'),

  // default number of master hosts on Assign Master page:
  defaultNoOfMasterHosts: function() {
    var componentName = this.get('componentName');
     if (this.get('isMasterWithMultipleInstances')) {
       return App.StackServiceComponent.cardinality(componentName).min;
     }
  }.property('componentName'),

  selectionSchemeForMasterComponent: function() {
    return App.StackServiceComponent.selectionScheme(this.get('componentName'));
  }.property('componentName'),

  isMasterWithMultipleInstancesHaWizard: function() {
    var masters = ['NAMENODE', 'JOURNALNODE'];
    return masters.contains(this.get('componentName'));
  }.property('componentName'),

  // components that are co-hosted with this component
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

  // Is any other component co-hosted with this component
  isOtherComponentCoHosted: function() {
    return !!this.get('coHostedComponents').length;
  }.property('coHostedComponents'),

  // Is this component co-hosted with other component
  isCoHostedComponent: function() {
    var componentName = this.get('componentName');
    return !!App.StackServiceComponent.coHost[componentName];
  }.property('componentName')

});

App.StackServiceComponent.FIXTURES = [];

App.StackServiceComponent.selectionScheme = function (componentName){
  switch (componentName) {
    case 'NAMENODE' :
      return {"else": 0};
    case 'SECONDARY_NAMENODE' :
      return {"else": 1};
    case 'HBASE_MASTER':
      return {"6": 0, "31": 2, "else": 3};
    case 'JOBTRACKER':
    case 'HISTORYSERVER':
    case 'RESOURCEMANAGER':
    case 'APP_TIMELINE_SERVER':
      return {"31": 1, "else": 2};
    case 'OOZIE_SERVER':
    case 'FALCON_SERVER' :
      return {"6": 1, "31": 2, "else": 3};
    case 'HIVE_SERVER' :
    case 'HIVE_METASTORE' :
    case 'WEBHCAT_SERVER' :
      return {"6": 1, "31": 2, "else": 4};
    default:
      return {"else": 0};
  }
};

App.StackServiceComponent.cardinality = function (componentName) {
  switch (componentName) {
    case 'ZOOKEEPER_SERVER':
      return {min: 3};
    case 'HBASE_MASTER':
      return {min: 1};
    default:
      return {min:1, max:1};
  }
};

App.StackServiceComponent.coHost = {
  'HIVE_METASTORE': 'HIVE_SERVER',
  'WEBHCAT_SERVER': 'HIVE_SERVER'
};
