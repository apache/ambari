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

/**
 * Mixin with methods for component actions that needs to be done when a config with specific value is saved
 * Used in the service config controller
 * @type {Em.Mixin}
 */
App.ComponentActionsByConfigs = Em.Mixin.create({

  /**
   * Do component add/delete actions as inferred from value of service configs
   * @public
   * @method doConfigActions
   */
  doConfigActions: function() {
    var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var configActionComponents = serviceConfigs.filterProperty('configActionComponent');
    this.doComponentDeleteActions(configActionComponents);
    this.doComponentAddActions(configActionComponents);
  },

  /**
   * Method informs if any component will be added/deleted on saving configurations
   * @return {boolean}
   * @public
   * @method isComponentActionsPresent
   */
  isComponentActionsPresent: function() {
    var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var configActionComponents = serviceConfigs.filterProperty('configActionComponent');
    return !!(this.getComponentsToDelete(configActionComponents).length + this.getComponentsToAdd(configActionComponents).length);
  },

  /**
   * Get Component that will be deleted on saving configurations
   * @param configActionComponents {Object}
   * @return {array}
   * @private
   * @method getComponentsToDelete
   */
  getComponentsToDelete: function(configActionComponents) {
    return configActionComponents.filterProperty('configActionComponent.action', 'delete').map(function(item){
      return item.configActionComponent;
    }).filter(function(_componentToDelete){
      return  App.HostComponent.find().filterProperty('componentName',_componentToDelete.componentName).someProperty('hostName', _componentToDelete.hostName);
    }, this);
  },

  /**
   * Get Component that will be added on saving configurations
   * @param configActionComponents {Object}
   * @return {array}
   * @private
   * @method getComponentsToDelete
   */
  getComponentsToAdd: function(configActionComponents) {
    return configActionComponents.filterProperty('configActionComponent.action', 'add').map(function(item){
      return item.configActionComponent;
    }).filter(function(_componentToAdd) {
      var serviceNameForcomponent = App.StackServiceComponent.find().findProperty('componentName',_componentToAdd.componentName).get('serviceName');
      // List of host components to be added should not include ones that are already present in the cluster.
      // Need to do below check from App.Service model as it keeps getting polled and updated on service page.
      return  !App.Service.find().findProperty('serviceName', serviceNameForcomponent).get('hostComponents').
               filterProperty('componentName',_componentToAdd.componentName).someProperty('hostName', _componentToAdd.hostName);
    }, this);
  },

  /**
   * Do component Delete actions as inferred from value of service config
   * @param configActionComponents Object[]
   * @private
   * @method {configActionComponents}
   */
  doComponentDeleteActions: function(configActionComponents) {
    var componentsToDelete = this.getComponentsToDelete(configActionComponents);
    if (componentsToDelete.length) {
      // There is always only one item to delete when doing config actions.
      var componentToDelete  =  componentsToDelete[0];
      var componentName = componentToDelete.componentName;
      var hostName = componentToDelete.hostName;
      var displayName = App.StackServiceComponent.find().findProperty('componentName',  componentToDelete.componentName).get('displayName');
      var context = Em.I18n.t('requestInfo.stop').format(displayName);
      var batches =[];

      this.setRefreshYarnQueueRequest(batches);
      batches.push(this.getInstallHostComponentsRequest(hostName, componentName, context));
      batches.push(this.getDeleteHostComponentRequest(hostName, componentName));
      this.setOrderIdForBatches(batches);

      App.ajax.send({
        name: 'common.batch.request_schedules',
        sender: this,
        data: {
          intervalTimeSeconds: 1,
          tolerateSize: 0,
          batches: batches
        }
      });
    }
  },

  /**
   * Do component Add actions as inferred from value of service config
   * @param configActionComponents Object[]
   * @private
   * @method {doComponentAddActions}
   */
  doComponentAddActions: function(configActionComponents) {
    var self = this;
    var componentsToAdd = this.getComponentsToAdd(configActionComponents);
    var dependentComponents = [];
    if (componentsToAdd.length) {
      componentsToAdd.forEach(function(_component) {
        var dependencies = App.StackServiceComponent.find(_component.componentName).get('dependencies').filterProperty('scope', 'host').map(function(_dependency){
          return {
            componentName: _dependency.componentName,
            hostName:  _component.hostName,
            isClient: App.StackServiceComponent.find(_dependency.componentName).get('isClient')
          }
        }, this);
        var dependenciesToInstall =  dependencies.filter(function (_dependencyToAdd) {
          var isInstalled = App.HostComponent.find().filterProperty('componentName', _dependencyToAdd.componentName).someProperty('hostName', _dependencyToAdd.hostName);
          var isAddedToInstall =  dependentComponents.filterProperty('componentName',_dependencyToAdd.componentName).someProperty('hostName', _dependencyToAdd.hostName);
          return !(isInstalled || isAddedToInstall);
        }, this);
        dependentComponents = dependentComponents.concat(dependenciesToInstall);
      }, this);
      var allComponentsToAdd = componentsToAdd.concat(dependentComponents);
      var allComponentsToAddHosts = allComponentsToAdd.mapProperty('hostName').uniq();
      allComponentsToAddHosts.forEach(function(_hostName){
        var hostComponents = allComponentsToAdd.filterProperty('hostName', _hostName).mapProperty('componentName').uniq();
        var masterHostComponents =  allComponentsToAdd.filterProperty('hostName', _hostName).filterProperty('isClient', false).mapProperty('componentName').uniq();
        var displayNames = masterHostComponents.map(function(item) {
          return App.StackServiceComponent.find().findProperty('componentName', item).get('displayName');
        });
        var displayStr =  stringUtils.getFormattedStringFromArray(displayNames);
        var context = Em.I18n.t('requestInfo.start').format(displayStr);
        var batches =[];
        this.setRefreshYarnQueueRequest(batches);
        batches.push(this.getCreateHostComponentsRequest(_hostName, hostComponents));
        batches.push(this.getInstallHostComponentsRequest(_hostName, hostComponents));
        batches.push(this.getStartHostComponentsRequest(_hostName, masterHostComponents, context));
        this.setOrderIdForBatches(batches);

        App.ajax.send({
          name: 'common.batch.request_schedules',
          sender: this,
          data: {
            intervalTimeSeconds: 1,
            tolerateSize: 0,
            batches: batches
          }
        });
      }, this);
    }
  },

  /**
   * Sets order_id for each batch request in the `batches` array
   * @param batches {Array}
   * @private
   * @method {setOrderIdForBatches}
   */
  setOrderIdForBatches: function(batches) {
    batches.forEach(function(_batch, index){
      _batch.order_id = index + 1;
    }, this);
  },

  /**
   * Gets the API request to create multiple components on a host
   * @param hostName {String}
   * @param components {String[]}|{String}
   * @return {Object} Deferred promise
   */
  getCreateHostComponentsRequest: function(hostName, components) {
    var query =  "Hosts/host_name.in(" + hostName + ")";
    components = (Array.isArray(components)) ? components : [components];
    var hostComponent = components.map(function(_componentName){
      return {
        "HostRoles":{
          "component_name":_componentName
        }
      }
    }, this);

    return  {
      "type": 'POST',
      "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/hosts",
      "RequestBodyInfo": {
        "RequestInfo": {
          "query": query
        },
        "Body": {
          "host_components": hostComponent
        }
      }
    };
  },

  /**
   * Gets the API request to install multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object}
   */
  getInstallHostComponentsRequest: function(hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.installComponents');
    return this.getUpdateHostComponentsRequest(hostName, components, App.HostComponentStatus.stopped, context);
  },

  /**
   * Gets the API request to start multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object}
   */
  getStartHostComponentsRequest: function(hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.startHostComponents');
    return this.getUpdateHostComponentsRequest(hostName, components, App.HostComponentStatus.started, context);
  },


  /**
   * Gets the API request to start/stop multiple components on a host
   * @param hostName {String}
   * @param components
   * @param desiredState
   * @param context {String}
   * @private
   * @method {getUpdateHostComponentsRequest}
   * @return {Object}
   */
  getUpdateHostComponentsRequest: function(hostName, components, desiredState, context) {
    components = (Array.isArray(components)) ? components : [components];
    var query = "HostRoles/component_name.in(" + components.join(',') + ")";

    return  {
      "type": 'PUT',
      "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/host_components",
      "RequestBodyInfo": {
        "RequestInfo": {
          "context": context,
          "operation_level": {
            "level": "HOST",
            "cluster_name": App.get('clusterName'),
            "host_names": hostName
          },
          "query": query
        },
        "Body": {
          "HostRoles": {
            "state": desiredState
          }
        }
      }
    };
  },

  /**
   * Gets the API request to delete component on a host
   * @param hostName {String}
   * @param component {String}
   * @private
   * @method {getDeleteHostComponentRequest}
   * @return {Object}
   */
  getDeleteHostComponentRequest: function(hostName, component) {
    return {
      "type": 'DELETE',
      "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/hosts/" + hostName + "/host_components/" + component
    }
  },

  /**
   * Add `Refresh YARN Queue` as a request in the batched API call
   * @param batches  {Array}
   * @private
   * @method {setRefreshYarnQueueRequest}
   */
  setRefreshYarnQueueRequest: function(batches) {
    var capacitySchedulerConfigs = this.get('allConfigs').filterProperty('filename', 'capacity-scheduler.xml').filter(function(item){
      return item.get('value') !== item.get('initialValue');
    });

    if (capacitySchedulerConfigs.length) {
      var serviceName = 'YARN';
      var componentName = 'RESOURCEMANAGER';
      var commandName = 'REFRESHQUEUES';
      var tag = 'capacity-scheduler';
      var hostNames = App.Service.find(serviceName).get('hostComponents').filterProperty('componentName', componentName).mapProperty('hostName');
      batches.push({
        "type": 'POST',
        "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            "command": commandName,
            "parameters/forceRefreshConfigTags": tag
          },
          "Requests/resource_filters": [
            {
              service_name: serviceName,
              component_name: componentName,
              hosts: hostNames.join(',')
            }
          ]
        }
      });
    }
  }
});