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
   * Do component Delete actions as inferred from value of service config
   * @param configActionComponents Object[]
   * @private
   * @method {configActionComponents}
   */
  doComponentDeleteActions: function(configActionComponents) {
    var self = this;
    var componentsToDelete = configActionComponents.filterProperty('configActionComponent.action', 'delete').map(function(item){
      return item.configActionComponent;
    }).filter(function(_componentToDelete){
      return  App.HostComponent.find().filterProperty('componentName',_componentToDelete.componentName).someProperty('hostName', _componentToDelete.hostName);
    }, this);

    if (componentsToDelete.length) {
      componentsToDelete.forEach(function(_componentToDelete){
        var displayName = App.StackServiceComponent.find().findProperty('componentName',  _componentToDelete.componentName).get('displayName');
        var context = Em.I18n.t('requestInfo.stop').format(displayName);
        self.refreshYarnQueues().done(function(data) {
          self.isRequestCompleted(data).done(function() {
            self.installHostComponents( _componentToDelete.hostName, _componentToDelete.componentName, context).done(function(data){
              self.isRequestCompleted(data).done(function() {
                self.deleteHostComponent(_componentToDelete.hostName, _componentToDelete.componentName);
              });
            });
          });
        });
      }, self);
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
    var componentsToAdd = configActionComponents.filterProperty('configActionComponent.action', 'add').map(function(item){
      return item.configActionComponent;
    }).filter(function(_componentToAdd){
      return  !App.HostComponent.find().filterProperty('componentName',_componentToAdd.componentName).someProperty('hostName', _componentToAdd.hostName);
    }, this);

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
        self.refreshYarnQueues().done(function(data) {
          self.isRequestCompleted(data).done(function() {
            self.createHostComponents(_hostName, hostComponents).done(function() {
              self.installHostComponents(_hostName, hostComponents).done(function(data){
                self.isRequestCompleted(data).done(function() {
                  var displayNames = masterHostComponents.map(function(item) {
                    return App.StackServiceComponent.find().findProperty('componentName', item).get('displayName');
                  });
                  var displayStr =  stringUtils.getFormattedStringFromArray(displayNames);
                  var context = Em.I18n.t('requestInfo.start').format(displayStr);
                  self.startHostComponents(_hostName, masterHostComponents, context);
                });
              });
            });
          });
        });
      }, self);
    }
  },

  /**
   * Calls the API to create multiple components on a host
   * @param hostName {String}
   * @param components {String[]}|{String}
   * @return {Object} Deferred promise
   */
  createHostComponents: function(hostName, components) {
    var query =  "Hosts/host_name.in(" + hostName + ")";
    components = (Array.isArray(components)) ? components : [components];
    var hostComponent = components.map(function(_componentName){
      return {
        "HostRoles":{
          "component_name":_componentName
        }
      }
    }, this);
    return App.ajax.send({
      name: 'common.host.host_components.create',
      sender: this,
      data: {
        query: query,
        host_components: hostComponent,
        type: ''
      }
    });
  },

  /**
   * Calls the API to install multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object} Deferred promise
   */
  installHostComponents: function(hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.installComponents');
    return this.updateHostComponents(hostName, components, App.HostComponentStatus.stopped, context);
  },

  /**
   * Calls the API to start multiple components on a host
   * @param hostName {String}
   * @param components {String[]}
   * @param context {String} Optional
   * @return {Object} Deferred promise
   */
  startHostComponents: function(hostName, components, context) {
    context = context || Em.I18n.t('requestInfo.startHostComponents');
    return this.updateHostComponents(hostName, components, App.HostComponentStatus.started, context);
  },

  /**
   * Calls the API to start/stop multiple components on a host
   * @param hostName {String}
   * @param components
   * @param desiredState
   * @param context {String}
   * @private
   * @method {updateHostComponents}
   * @return {Object} Deferred promise
   */
  updateHostComponents: function(hostName, components, desiredState, context) {
    var data = {
      hostName: hostName,
      context: context,
      HostRoles: {
        state: desiredState
      }
    };
    components = (Array.isArray(components)) ? components : [components];
    data.query = "HostRoles/component_name.in(" + components.join(',') + ")";

    return App.ajax.send({
      name: 'common.host.host_components.update',
      sender: this,
      data: data
    });
  },

  /**
   * Calls the API to delete component on a host
   * @param hostName {String}
   * @param component {String}
   * @return {Object} Deferred
   */
  deleteHostComponent: function(hostName, component) {
    return App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        hostName: hostName,
        componentName: component
      }
    });
  },

  /**
   * Calls the API to refresh yarn queue
   * @private
   * @method {refreshYarnQueues}
   * @return {Object} Deferred
   */
  refreshYarnQueues: function () {
    var dfd = $.Deferred();
    var capacitySchedulerConfigs = this.get('allConfigs').filterProperty('filename', 'capacity-scheduler.xml').filter(function(item){
      return item.get('value') !== item.get('initialValue');
    });

    if (capacitySchedulerConfigs.length) {
      var serviceName = 'YARN';
      var componentName = 'RESOURCEMANAGER';
      var commandName = 'REFRESHQUEUES';
      var tag = 'capacity-scheduler';
      var hosts = App.Service.find(serviceName).get('hostComponents').filterProperty('componentName', componentName).mapProperty('hostName');
      return App.ajax.send({
        name : 'service.item.refreshQueueYarnRequest',
        sender: this,
        data : {
          command : commandName,
          context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context') ,
          hosts : hosts.join(','),
          serviceName : serviceName,
          componentName : componentName,
          forceRefreshConfigTags : tag
        }
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  /**
   * Fetched the Request Id to poll and starts the polling to check
   * if the request is ongoing or completed until deferred is resolved
   * @param data {Object} Json
   * @private
   * @method {isRequestCompleted}
   * @return {Object} Deferred promise
   */
  isRequestCompleted: function(data) {
    var dfd = $.Deferred();
    if (!data) {
      dfd.resolve();
    } else {
      var requestId = data.Requests.id;
      this.getRequestStatus(requestId, dfd);
    }
    return dfd.promise();
  },

  /**
   * keeps checking on requestId if the request is ongoing or completed until deferred is resolved
   * @param requestId {Integer}
   * @param requestCompletedDfd {Object} Deferred Object to resolve when the requestId shows that the request is completed
   * @private
   * @method {isRequestCompleted}
   */
  getRequestStatus: function(requestId, requestCompletedDfd) {
    var self = this;
    var POLL_INTERVAL = 5000;
    var TIMEOUT = 3000;

    var dfdPromise =  App.ajax.send({
      name: 'common.get.request.status',
      sender: this,
      data: {
        requestId: requestId,
        timeout: TIMEOUT
      }
    });

    var successCallback = function(data) {
      if (['PENDING','IN_PROGRESS'].contains(data.Requests.request_status)) {
        window.setTimeout(function () {
          self.getRequestStatus(requestId, requestCompletedDfd);
        }, POLL_INTERVAL);
      } else if (data.Requests.request_status === 'COMPLETED') {
        requestCompletedDfd.resolve();
      } else {
        requestCompletedDfd.fail();
      }
    };

    var errorCallback = function()  {
      window.setTimeout(function () {
        self.getRequestStatus(requestId, requestCompletedDfd);
      }, POLL_INTERVAL);
    };

    dfdPromise.then(successCallback, errorCallback);
  }
});