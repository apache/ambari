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

/**
 * Contains helpful utilities for handling batch and scheduled requests.
 */
module.exports = {

  /**
   * Some services have components which have a need for rolling restarts. This
   * method returns the name of the host-component which supports rolling
   * restarts for a service.
   */
  getRollingRestartComponentName : function(serviceName) {
    var rollingRestartComponent = null;
    switch (serviceName) {
    case 'HDFS':
      rollingRestartComponent = 'DATANODE';
      break;
    case 'YARN':
      rollingRestartComponent = 'NODEMANAGER';
      break;
    case 'MAPREDUCE':
      rollingRestartComponent = 'TASKTRACKER';
      break;
    case 'HBASE':
      rollingRestartComponent = 'HBASE_REGIONSERVER';
      break;
    case 'STORM':
      rollingRestartComponent = 'SUPERVISOR';
      break;
    default:
      break;
    }
    return rollingRestartComponent;
  },

  doPostRestartAllServiceComponents : function(serviceName) {
    var allHostComponents = App.HostComponent.find();
    var componentToHostsMap = {};
    var componentCount = 0;
    allHostComponents.forEach(function(hc) {
      if (serviceName == hc.get('service.serviceName')) {
        var componentName = hc.get('componentName');
        if (!componentToHostsMap[componentName]) {
          componentToHostsMap[componentName] = [];
          componentCount++;
        }
        componentToHostsMap[componentName].push(hc.get('host.hostName'));
      }
    });
    for ( var componentName in componentToHostsMap) {
      var hosts = componentToHostsMap[componentName].join(",");
      var data = {
        serviceName : serviceName,
        componentName : componentName,
        hosts : hosts
      }
      var sender = {
        successFunction : function() {
          App.router.get('applicationController').dataLoading().done(function (initValue) {
            if (initValue) {
              App.router.get('backgroundOperationsController').showPopup();
            }
          });
        },
        errorFunction : function(xhr, textStatus, error, opt) {
          App.ajax.defaultErrorHandler(xhr, opt.url, 'POST', xhr.status);
        }
      }
      App.ajax.send({
        name : 'restart.service.hostComponents',
        sender : sender,
        data : data,
        success : 'successFunction',
        error : 'errorFunction'
      });
    }
  },

  /**
   * Makes a REST call to the server requesting the rolling restart of the
   * provided host components.
   */
  doPostBatchRollingRestartRequest : function(restartHostComponents, batchSize, intervalTimeSeconds, tolerateSize, successCallback, errorCallback) {
    var clusterName = App.get('clusterName');
    var data = {
      restartHostComponents : restartHostComponents,
      batchSize : batchSize,
      intervalTimeSeconds : intervalTimeSeconds,
      tolerateSize : tolerateSize,
      clusterName : clusterName
    }
    var sender = {
      successFunction : function() {
        successCallback();
      },
      errorFunction : function(xhr, textStatus, error, opt) {
        errorCallback(xhr, textStatus, error, opt);
      }
    }
    App.ajax.send({
      name : 'rolling_restart.post',
      sender : sender,
      data : data,
      success : 'successFunction',
      error : 'errorFunction'
    });
  },

  /**
   * Launches dialog to handle rolling restarts of host components.
   *
   * Rolling restart is supported for the following host components only
   * <ul>
   * <li>Data Nodes (HDFS)
   * <li>Task Trackers (MapReduce)
   * <li>Node Managers (YARN)
   * <li>Region Servers (HBase)
   * <li>Supervisors (Storm)
   * </ul>
   *
   * @param {String}
   *          hostComponentName Type of host-component to restart across cluster
   *          (ex: DATANODE)
   * @param {Boolean}
   *          staleConfigsOnly Pre-select host-components which have stale
   *          configurations
   */
  launchHostComponentRollingRestart : function(hostComponentName, staleConfigsOnly) {
    var componentDisplayName = App.format.role(hostComponentName);
    if (!componentDisplayName) {
      componentDisplayName = hostComponentName;
    }
    var self = this;
    var title = Em.I18n.t('rollingrestart.dialog.title').format(componentDisplayName)
    if (hostComponentName == "DATANODE" || hostComponentName == "TASKTRACKER" || hostComponentName == "NODEMANAGER"
        || hostComponentName == "HBASE_REGIONSERVER" || hostComponentName == "SUPERVISOR") {
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
        secondary : Em.I18n.t('common.cancel'),
        onPrimary : function() {
          var dialog = this;
          if (!dialog.get('enablePrimary')) {
            return false;
          }
          var restartComponents = this.get('innerView.restartHostComponents');
          var batchSize = this.get('innerView.batchSize');
          var waitTime = this.get('innerView.interBatchWaitTimeSeconds');
          var tolerateSize = this.get('innerView.tolerateSize');
          self.doPostBatchRollingRestartRequest(restartComponents, batchSize, waitTime, tolerateSize, function() {
            dialog.hide();
            App.router.get('applicationController').dataLoading().done(function (initValue) {
              if (initValue) {
                App.router.get('backgroundOperationsController').showPopup();
              }
            });
          }, function(xhr, textStatus, error, opt) {
            App.ajax.defaultErrorHandler(xhr, opt.url, 'POST', xhr.status);
          });
          return;
        },
        onSecondary : function() {
          this.hide();
        },
        onClose : function() {
          this.hide();
        },
        updateButtons : function() {
          var errors = this.get('innerView.errors');
          this.set('enablePrimary', !(errors != null && errors.length > 0))
        }.observes('innerView.errors'),
      });
    } else {
      var msg = Em.I18n.t('rollingrestart.notsupported.hostComponent').format(componentDisplayName);
      console.log(msg);
      App.ModalPopup.show({
        header : title,
        secondary : false,
        msg : msg,
        bodyClass : Ember.View.extend({
          template : Ember.Handlebars.compile('<div class="alert alert-warning">{{msg}}</div>'),
          msg : msg
        })
      });
    }
  }
}