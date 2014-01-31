/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * Default success callback for ajax-requests in this module
 * @type {Function}
 */
var defaultSuccessCallback = function() {
  App.router.get('applicationController').dataLoading().done(function(initValue) {
    if (initValue) {
      App.router.get('backgroundOperationsController').showPopup();
    }
  });
};
/**
 * Default error callback for ajax-requests in this module
 * @param {Object} xhr
 * @param {String} textStatus
 * @param {String} error
 * @param {Object} opt
 * @type {Function}
 */
var defaultErrorCallback = function(xhr, textStatus, error, opt) {
  App.ajax.defaultErrorHandler(xhr, opt.url, 'POST', xhr.status);
};

/**
 * Contains helpful utilities for handling batch and scheduled requests.
 */
module.exports = {

  /**
   * Some services have components which have a need for rolling restarts. This
   * method returns the name of the host-component which supports rolling
   * restarts for a service.
   * @param {String} serviceName
   */
  getRollingRestartComponentName: function(serviceName) {
    var rollingRestartComponents = {
      HDFS: 'DATANODE',
      YARN: 'NODEMANAGER',
      MAPREDUCE: 'TASKTRACKER',
      HBASE: 'HBASE_REGIONSERVER',
      STORM: 'SUPERVISOR'
    };
    return rollingRestartComponents[serviceName] ? rollingRestartComponents[serviceName] : null;
  },

  /**
   * Facade-function for restarting host components of specific service
   * @param {String} serviceName for which service hostComponents should be restarted
   * @param {Boolean} staleConfigsOnly restart only hostComponents with <code>staleConfig</code> true
   */
  restartAllServiceHostComponents: function(serviceName, staleConfigsOnly) {
    var service = App.Service.find(serviceName);
    if (service) {
      var hostComponents = service.get('hostComponents').filterProperty('passiveState','ACTIVE');
      if (staleConfigsOnly) {
        hostComponents = hostComponents.filterProperty('staleConfigs', true);
      }
      this.restartHostComponents(hostComponents);
    }
  },

  /**
   * Restart list of host components
   * @param {Array} hostComponentsList list of host components should be restarted
   */
  restartHostComponents: function(hostComponentsList) {
    /**
     * Format: {
     *  'DATANODE': ['host1', 'host2'],
     *  'NAMENODE': ['host1', 'host3']
     *  ...
     * }
     * @type {Object}
     */
    var componentToHostsMap = {};
    var componentServiceMap = App.QuickDataMapper.componentServiceMap();
    hostComponentsList.forEach(function(hc) {
      var componentName = hc.get('componentName');
      if (!componentToHostsMap[componentName]) {
        componentToHostsMap[componentName] = [];
      }
      componentToHostsMap[componentName].push(hc.get('host.hostName'));
    });
    for (var componentName in componentToHostsMap) {
      App.ajax.send({
        name: 'restart.service.hostComponents',
        sender: {
          successCallback: defaultSuccessCallback,
          errorCallback: defaultErrorCallback
        },
        data: {
          serviceName:  componentServiceMap[componentName],
          componentName: componentName,
          hosts: componentToHostsMap[componentName].join(",")
        },
        success: 'successCallback',
        error: 'errorCallback'
      });
    }
  },

  /**
   * Makes a REST call to the server requesting the rolling restart of the
   * provided host components.
   * @param {Array} restartHostComponents list of host components should be restarted
   * @param {Number} batchSize size of each batch
   * @param {Number} intervalTimeSeconds delay between two batches
   * @param {Number} tolerateSize task failure tolerance
   * @param {Function} successCallback
   * @param {Function} errorCallback
   */
  _doPostBatchRollingRestartRequest: function(restartHostComponents, batchSize, intervalTimeSeconds, tolerateSize, successCallback, errorCallback) {
    successCallback = successCallback ? successCallback : defaultSuccessCallback;
    errorCallback = errorCallback ? errorCallback : defaultErrorCallback;
    if (!restartHostComponents.length) {
      console.log('No batch rolling restart if no restartHostComponents provided!');
      return;
    }
    App.ajax.send({
      name: 'rolling_restart.post',
      sender: {
        successCallback: successCallback,
        errorCallback: errorCallback
      },
      data: {
        intervalTimeSeconds: intervalTimeSeconds,
        tolerateSize: tolerateSize,
        batches: this.getBatchesForRollingRestartRequest(restartHostComponents, batchSize)
      },
      success: 'successCallback',
      error: 'errorCallback'
    });
  },

  /**
   * Create list of batches for rolling restart request
   * @param {Array} restartHostComponents list host components should be restarted
   * @param {Number} batchSize size of each batch
   * @returns {Array} list of batches
   */
  getBatchesForRollingRestartRequest: function(restartHostComponents, batchSize) {
    var hostIndex = 0,
        batches = [],
        batchCount = Math.ceil(restartHostComponents.length / batchSize),
        sampleHostComponent = restartHostComponents.objectAt(0),
        componentName = sampleHostComponent.get('componentName'),
        serviceName = sampleHostComponent.get('service.serviceName');

    for ( var count = 0; count < batchCount; count++) {
      var hostNames = [];
      for ( var hc = 0; hc < batchSize && hostIndex < restartHostComponents.length; hc++) {
        hostNames.push(restartHostComponents.objectAt(hostIndex++).get('host.hostName'));
      }
      if (hostNames.length > 0) {
        batches.push({
          "order_id" : count + 1,
          "type" : "POST",
          "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
          "RequestBodyInfo" : {
            "RequestInfo" : {
              "context" : "_PARSE_.ROLLING-RESTART." + componentName + "." + (count + 1) + "." + batchCount,
              "command" : "RESTART",
              "service_name" : serviceName,
              "component_name" : componentName,
              "hosts" : hostNames.join(",")
            }
          }
        });
      }
    }
    return batches;
  },

  /**
   * Launches dialog to handle rolling restarts of host components.
   *
   * Rolling restart is supported only for components listed in <code>getRollingRestartComponentName</code>
   * @see getRollingRestartComponentName
   * @param {String} hostComponentName
   *           Type of host-component to restart across cluster
   *          (ex: DATANODE)
   * @param {Boolean} staleConfigsOnly
   *           Pre-select host-components which have stale
   *          configurations
   */
  launchHostComponentRollingRestart: function(hostComponentName, staleConfigsOnly) {
    var componentDisplayName = App.format.role(hostComponentName);
    if (!componentDisplayName) {
      componentDisplayName = hostComponentName;
    }
    var self = this;
    var title = Em.I18n.t('rollingrestart.dialog.title').format(componentDisplayName);
    var allowedHostComponents = ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "SUPERVISOR"];
    if (allowedHostComponents.contains(hostComponentName)) {
      App.ModalPopup.show({
        header : title,
        hostComponentName : hostComponentName,
        staleConfigsOnly : staleConfigsOnly,
        innerView : null,
        bodyClass : App.RollingRestartView.extend({
          hostComponentName : hostComponentName,
          staleConfigsOnly : staleConfigsOnly,
          didInsertElement : function() {
            this.set('parentView.innerView', this);
            this.initialize();
          }
        }),
        classNames : [ 'rolling-restart-popup' ],
        primary : Em.I18n.t('rollingrestart.dialog.primary'),
        onPrimary : function() {
          var dialog = this;
          if (!dialog.get('enablePrimary')) {
            return;
          }
          var restartComponents = this.get('innerView.restartHostComponents');
          var batchSize = this.get('innerView.batchSize');
          var waitTime = this.get('innerView.interBatchWaitTimeSeconds');
          var tolerateSize = this.get('innerView.tolerateSize');
          self._doPostBatchRollingRestartRequest(restartComponents, batchSize, waitTime, tolerateSize, function() {
            dialog.hide();
            defaultSuccessCallback();
          });
        },
        updateButtons : function() {
          var errors = this.get('innerView.errors');
          this.set('enablePrimary', !(errors != null && errors.length > 0))
        }.observes('innerView.errors')
      });
    } else {
      var msg = Em.I18n.t('rollingrestart.notsupported.hostComponent').format(componentDisplayName);
      console.log(msg);
      App.ModalPopup.show({
        header : title,
        secondary : false,
        msg : msg,
        bodyClass : Ember.View.extend({
          template : Ember.Handlebars.compile('<div class="alert alert-warning">{{msg}}</div>')
        })
      });
    }
  },

  /**
   * Retrieves the latest information about a specific request schedule
   * identified by 'requestScheduleId'
   *
   * @param {Number} requestScheduleId ID of the request schedule to get
   * @param {Function} successCallback Called with request_schedule data from server. An
   *          empty object returned for invalid ID.
   * @param {Function} errorCallback Optional error callback. Default behavior is to
   *          popup default error dialog.
   */
  getRequestSchedule: function(requestScheduleId, successCallback, errorCallback) {
    if (requestScheduleId != null && !isNaN(requestScheduleId) && requestScheduleId > -1) {
      errorCallback = errorCallback ? errorCallback : defaultErrorCallback;
      App.ajax.send({
        name : 'request_schedule.get',
        sender : {
          successCallbackFunction : function(data) {
            successCallback(data);
          },
          errorCallbackFunction : function(xhr, textStatus, error, opt) {
            errorCallback(xhr, textStatus, error, opt);
          }
        },
        data : {
          request_schedule_id : requestScheduleId
        },
        success : 'successCallbackFunction',
        error : 'errorCallbackFunction'
      });
    } else {
      successCallback({});
    }
  },

  /**
   * Attempts to abort a specific request schedule identified by 'requestScheduleId'
   *
   * @param {Number} requestScheduleId ID of the request schedule to get
   * @param {Function} successCallback Called when request schedule successfully aborted
   * @param {Function} errorCallback Optional error callback. Default behavior is to
   *          popup default error dialog.
   */
  doAbortRequestSchedule: function(requestScheduleId, successCallback, errorCallback) {
    if (requestScheduleId != null && !isNaN(requestScheduleId) && requestScheduleId > -1) {
      errorCallback = errorCallback ? errorCallback : defaultErrorCallback;
      App.ajax.send({
        name : 'request_schedule.delete',
        sender : {
          successCallbackFunction : function(data) {
            successCallback(data);
          },
          errorCallbackFunction : function(xhr, textStatus, error, opt) {
            errorCallback(xhr, textStatus, error, opt);
          }
        },
        data : {
          request_schedule_id : requestScheduleId
        },
        success : 'successCallbackFunction',
        error : 'errorCallbackFunction'
      });
    } else {
      successCallback({});
    }
  }
};