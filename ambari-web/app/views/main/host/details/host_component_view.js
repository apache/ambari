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
var uiEffects = require('utils/ui_effects');

App.HostComponentView = Em.View.extend({
  templateName: require('templates/main/host/details/host_component'),
  /**
   * @type {App.HostComponent}
   */
  content: null,
  didInsertElement: function () {
    App.tooltip($('[rel=componentHealthTooltip]'));
    App.tooltip($('[rel=passiveTooltip]'));
    if (this.get('isInProgress')) {
      this.doBlinking();
    }
    if (this.get('isDataNode')){
      this.loadDataNodeDecommissionStatus();
    }
    if (this.get('isNodeManager')){
      this.loadNodeManagerDecommissionStatus();
    }
    if (this.get('isTaskTracker')){
      this.loadTaskTrackerDecommissionStatus();
    }
    if (this.get('isRegionServer')){
      this.loadRegionServerDecommissionStatus();
    }
  },
  /**
   * @type {App.HostComponent}
   */
  hostComponent: function () {
    var hostComponent = null;
    var serviceComponent = this.get('content');
    var host = App.router.get('mainHostDetailsController.content');
    if (host) {
      hostComponent = host.get('hostComponents').findProperty('componentName', serviceComponent.get('componentName'));
    }
    return hostComponent;
  }.property('content', 'App.router.mainHostDetailsController.content'),
  /**
   * @type {String}
   */
  workStatus: function () {
    var workStatus = this.get('content.workStatus');
    var hostComponent = this.get('hostComponent');
    if (hostComponent) {
      workStatus = hostComponent.get('workStatus');
    }
    return workStatus;
  }.property('content.workStatus', 'hostComponent.workStatus'),

  /**
   * Return host component text status
   * @type {String}
   */
  componentTextStatus: function () {
    if (this.get('content.passiveState') != 'ACTIVE') {
      return Em.I18n.t('hosts.component.passive.short.mode');
    }
    var workStatus = this.get("workStatus");
    var componentTextStatus = this.get('content.componentTextStatus');
    var hostComponent = this.get('hostComponent');
    if (hostComponent) {
      componentTextStatus = hostComponent.get('componentTextStatus');
      if(this.get("isDataNode") && this.get('isDataNodeRecommissionAvailable')){
        if(this.get('isDataNodeDecommissioning')){
          componentTextStatus = Em.I18n.t('hosts.host.decommissioning');
        } else {
          componentTextStatus = Em.I18n.t('hosts.host.decommissioned');
        }
      }
      if(this.get("isNodeManager") && this.get('isNodeManagerRecommissionAvailable')){
        if(this.get('isNodeManagerDecommissioning')){
          componentTextStatus = Em.I18n.t('hosts.host.decommissioning');
        } else {
          componentTextStatus = Em.I18n.t('hosts.host.decommissioned');
        }
      }
      if(this.get("isTaskTracker") && this.get('isTaskTrackerRecommissionAvailable')){
        if(this.get('isTaskTrackerDecommissioning')){
          componentTextStatus = Em.I18n.t('hosts.host.decommissioning');
        } else {
          componentTextStatus = Em.I18n.t('hosts.host.decommissioned');
        }
      }
      if(this.get("isRegionServer") && this.get('isRegionServerRecommissionAvailable')){
        if(this.get('isRegionServerDecommissioning')){
          componentTextStatus = Em.I18n.t('hosts.host.decommissioning');
        } else {
          componentTextStatus = Em.I18n.t('hosts.host.decommissioned');
        }
      }
    }
    return componentTextStatus;
  }.property('content.passiveState','workStatus','isDataNodeRecommissionAvailable', 'isDataNodeDecommissioning', 'isNodeManagerRecommissionAvailable', 'isNodeManagerDecommissioning',
      'isTaskTrackerRecommissionAvailable', 'isTaskTrackerDecommissioning', 'isRegionServerRecommissionAvailable', 'isRegionServerDecommissioning'),

  /**
   * @type {String}
   */
  passiveImpliedTextStatus: function() {
    if(this.get('parentView.content.passiveState') === 'PASSIVE') {
      return Em.I18n.t('hosts.component.passive.implied.host.mode.tooltip');
    }
    else
      if(this.get('content.service.passiveState') === 'PASSIVE') {
        return Em.I18n.t('hosts.component.passive.implied.service.mode.tooltip').format(this.get('content.service.serviceName'));
      }
    return '';
  }.property('content.passiveState','parentView.content.passiveState'),

  statusClass: function () {
    //If the component is DataNode
    if (this.get('isDataNode')) {
      if (this.get('isDataNodeRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
        return 'health-status-DEAD-ORANGE';
      }
    }

    //If the component is NodeManager
    if (this.get('isNodeManager')) {
      if (this.get('isNodeManagerRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
        return 'health-status-DEAD-ORANGE';
      }
    }

    //If the component is TaskTracker
    if (this.get('isTaskTracker')) {
      if (this.get('isTaskTrackerRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
        return 'health-status-DEAD-ORANGE';
      }
    }

    //If the component is RegionServer
    if (this.get('isRegionServer')) {
      if (this.get('isRegionServerRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
        return 'health-status-DEAD-ORANGE';
      }
    }

    //Class when install failed
    if (this.get('workStatus') === App.HostComponentStatus.install_failed) {
      return 'health-status-color-red icon-cog';
    }

    //Class when installing
    if (this.get('workStatus') === App.HostComponentStatus.installing) {
      return 'health-status-color-blue icon-cog';
    }

    //Class when maintenance
    if (this.get('content.passiveState') != "ACTIVE") {
      return 'icon-medkit';
    }

    //For all other cases
    return 'health-status-' + App.HostComponentStatus.getKeyName(this.get('workStatus'));

  }.property('content.passiveState','workStatus', 'isDataNodeRecommissionAvailable', 'isNodeManagerRecommissionAvailable', 'isTaskTrackerRecommissionAvailable', 'isRegionServerRecommissionAvailable'),

  /**
   * @type {String}
   */
  disabled: function () {
    return (this.get('parentView.content.healthClass') === "health-status-DEAD-YELLOW") ? 'disabled' : '';
  }.property('parentView.content.healthClass'),
  /**
   * For Upgrade failed state
   */
  isUpgradeFailed: function () {
    return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "upgrade_failed";
  }.property("workStatus"),
  /**
   * For Install failed state
   */
  isInstallFailed: function () {
    return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "install_failed";
  }.property("workStatus"),
  /**
   * Do blinking for 1 minute
   */
  doBlinking: function () {
    var workStatus = this.get('workStatus');
    var self = this;
    var pulsate = [ App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.installing].contains(workStatus);
    if (!pulsate && (this.get('isDataNode') || this.get('isRegionServer') || this.get('isNodeManager') || this.get('isTaskTracker'))) {
      var component = this.get('content');
      if (component && workStatus != "INSTALLED") {
        pulsate = this.get('isDecommissioning');
      }
    }
    if (pulsate && !self.get('isBlinking')) {
      self.set('isBlinking', true);
      uiEffects.pulsate(self.$('.components-health'), 1000, function () {
        self.set('isBlinking', false);
        self.doBlinking();
      });
    }
  },
  /**
   * Start blinking when host component is starting/stopping/decommissioning
   */
  startBlinking: function () {
    this.$('.components-health').stop(true, true);
    this.$('.components-health').css({opacity: 1.0});
    this.doBlinking();
  }.observes('workStatus','isDataNodeRecommissionAvailable', 'isDecommissioning', 'isRegionServerRecommissionAvailable',
      'isNodeManagerRecommissionAvailable', 'isTaskTrackerRecommissionAvailable'),

  isStart: function () {
    return (this.get('workStatus') == App.HostComponentStatus.started || this.get('workStatus') == App.HostComponentStatus.starting);
  }.property('workStatus'),

  isStop: function () {
    return (this.get('workStatus') == App.HostComponentStatus.stopped);
  }.property('workStatus'),

  isInstalling: function () {
    return (this.get('workStatus') == App.HostComponentStatus.installing);
  }.property('workStatus'),
  /**
   * No action available while component is starting/stopping/unknown
   */
  noActionAvailable: function () {
    var workStatus = this.get('workStatus');
    if ([App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown].contains(workStatus)) {
      return "hidden";
    }else{
      return "";
    }
  }.property('workStatus'),

  isInProgress: function () {
    return (this.get('workStatus') === App.HostComponentStatus.stopping ||
      this.get('workStatus') === App.HostComponentStatus.starting) ||
      this.get('isDecommissioning');
  }.property('workStatus', 'isDecommissioning'),

  isDataNode: function () {
    return this.get('content.componentName') === 'DATANODE';
  }.property('content'),
  isNodeManager: function () {
    return this.get('content.componentName') === 'NODEMANAGER';
  }.property('content'),
  isTaskTracker: function () {
    return this.get('content.componentName') === 'TASKTRACKER';
  }.property('content'),
  isRegionServer: function () {
    return this.get('content.componentName') === 'HBASE_REGIONSERVER';
  }.property('content'),

  isActive: function () {
    return (this.get('content.passiveState') == "ACTIVE");
  }.property('content.passiveState'),

  isImplied: function() {
    return (this.get('parentView.content.passiveState') === 'PASSIVE' || this.get('content.service.passiveState') === 'PASSIVE');
  }.property('content.passiveState'),

  isDecommissioning: function () {
    return ( (this.get('isDataNode') && this.get("isDataNodeDecommissioning")) || (this.get('isRegionServer') && this.get("isRegionServerDecommissioning"))
      || (this.get('isNodeManager') && this.get("isNodeManagerDecommissioning")) || (this.get('isTaskTracker') && this.get('isTaskTrackerDecommissioning')));
  }.property("workStatus", "isDataNodeDecommissioning", "isRegionServerDecommissioning", "isNodeManagerDecommissioning", "isTaskTrackerDecommissioning"),

  isDataNodeDecommissioning: null,
  isDataNodeDecommissionAvailable: null,
  isDataNodeRecommissionAvailable: null,
  /**
   * load Recommission/Decommission status from adminState of each live node
   */
  loadDataNodeDecommissionStatus: function () {
    var clusterName = App.router.get('clusterController.clusterName');
    var hostName = App.router.get('mainHostDetailsController.content.hostName');
    var componentName = 'NAMENODE';
    var slaveType = 'DATANODE';
    var version = App.get('currentStackVersionNumber');
    var dfd = $.Deferred();
    var self = this;
    this.getDNDecommissionStatus(clusterName, hostName, componentName).done(function () {
      var curObj = self.get('decommissionedStatusObject');
      self.set('decommissionedStatusObject', null);
      // HDP-2 stack
      if (version.charAt(0) == 2) {
        if (curObj) {
          var liveNodesJson = App.parseJSON(curObj.LiveNodes);
          if (liveNodesJson && liveNodesJson[hostName] ) {
            switch(liveNodesJson[hostName].adminState) {
              case "In Service":
                self.set('isDataNodeRecommissionAvailable', false);
                self.set('isDataNodeDecommissioning', false);
                self.set('isDataNodeDecommissionAvailable', self.get('isStart'));
                break;
              case "Decommission In Progress":
                self.set('isDataNodeRecommissionAvailable', true);
                self.set('isDataNodeDecommissioning', true);
                self.set('isDataNodeDecommissionAvailable', false);
                break;
              case "Decommissioned":
                self.set('isDataNodeRecommissionAvailable', true);
                self.set('isDataNodeDecommissioning', false);
                self.set('isDataNodeDecommissionAvailable', false);
                break;
            }
          } else {
            // if namenode is down, get desired_admin_state to decide if the user had issued a decommission
            var deferred = $.Deferred();
            self.getDesiredAdminState(clusterName, hostName, slaveType).done( function () {
              var desired_admin_state = self.get('desiredAdminState');
              self.set('desiredAdminState', null);
              switch(desired_admin_state) {
                case "INSERVICE":
                  self.set('isDataNodeRecommissionAvailable', false);
                  self.set('isDataNodeDecommissioning', false);
                  self.set('isDataNodeDecommissionAvailable', self.get('isStart'));
                  break;
                case "DECOMMISSIONED":
                  self.set('isDataNodeRecommissionAvailable', true);
                  self.set('isDataNodeDecommissioning', false);
                  self.set('isDataNodeDecommissionAvailable', false);
                  break;
              }
              deferred.resolve(desired_admin_state);
            });
          }
        }
      }
      // HDP-1 stack
      if (version.charAt(0) == 1) {
        if (curObj) {
          var liveNodesJson = App.parseJSON(curObj.LiveNodes);
          var decomNodesJson = App.parseJSON(curObj.DecomNodes);
          var deadNodesJson = App.parseJSON(curObj.DeadNodes);
          if (decomNodesJson && decomNodesJson[hostName] ) {
            self.set('isDataNodeRecommissionAvailable', true);
            self.set('isDataNodeDecommissioning', true);
            self.set('isDataNodeDecommissionAvailable', false);
          } else if (deadNodesJson && deadNodesJson[hostName] ) {
            self.set('isDataNodeRecommissionAvailable', true);
            self.set('isDataNodeDecommissioning', false);
            self.set('isDataNodeDecommissionAvailable', false);
          } else if (liveNodesJson && liveNodesJson[hostName] ) {
            self.set('isDataNodeRecommissionAvailable', false);
            self.set('isDataNodeDecommissioning', false);
            self.set('isDataNodeDecommissionAvailable', self.get('isStart'));
          } else {
            // if namenode is down, get desired_admin_state to decide if the user had issued a decommission
            var deferred = $.Deferred();
            self.getDesiredAdminState(clusterName, hostName, slaveType).done( function () {
              var desired_admin_state = self.get('desiredAdminState');
              self.set('desiredAdminState', null);
              switch(desired_admin_state) {
                case "INSERVICE":
                  self.set('isDataNodeRecommissionAvailable', false);
                  self.set('isDataNodeDecommissioning', false);
                  self.set('isDataNodeDecommissionAvailable', self.get('isStart'));
                  break;
                case "DECOMMISSIONED":
                  self.set('isDataNodeRecommissionAvailable', true);
                  self.set('isDataNodeDecommissioning', false);
                  self.set('isDataNodeDecommissionAvailable', false);
                  break;
              }
              deferred.resolve(desired_admin_state);
            });
          }
        }
      }
      dfd.resolve(curObj);
    });
    return dfd.promise();
  }.observes('App.router.mainHostDetailsController.content'),

  /**
   * get datanodes decommission status: from NAMENODE component, liveNodes property
   */
  getDNDecommissionStatus: function(clusterName, hostName, componentName){
    return App.ajax.send({
      name: 'host.host_component.datanodes_decommission_status',
      sender: this,
      data: {
        clusterName: clusterName,
        hostName: hostName,
        componentName: componentName
      },
      success: 'getDNDecommissionStatusSuccessCallback',
      error: 'getDNDecommissionStatusErrorCallback'
    });
  },

  decommissionedStatusObject: null,
  getDNDecommissionStatusSuccessCallback: function (response, request, data) {
    var statusObject = response.ServiceComponentInfo;
    if ( statusObject != null) {
      this.set('decommissionedStatusObject', statusObject);
      return statusObject;
    }
  },
  getDNDecommissionStatusErrorCallback: function (request, ajaxOptions, error) {
    console.log('ERROR: '+ error);
    this.set('decommissionedStatusObject', null);
    return null;
  },

  /**
   * get desired_admin_state status of DataNode, TaskTracker, NodeManager and RegionServer
   */
  getDesiredAdminState: function(clusterName, hostName, componentName){
    return App.ajax.send({
      name: 'host.host_component.slave_desired_admin_state',
      sender: this,
      data: {
        clusterName: clusterName,
        hostName: hostName,
        componentName: componentName
      },
      success: 'getDesiredAdminStateSuccessCallback',
      error: 'getDesiredAdminStateErrorCallback'
    });
  },
  desiredAdminState: null,
  getDesiredAdminStateSuccessCallback: function (response, request, data) {
    var status = response.HostRoles.desired_admin_state;
    if ( status != null) {
      this.set('desiredAdminState', status);
      return status;
    }
  },
  getDesiredAdminStateErrorCallback: function (request, ajaxOptions, error) {
    console.log('ERROR: '+ error);
    this.set('desiredAdminState', null);
    return null;
  },

  isNodeManagerDecommissionAvailable: null,
  isNodeManagerRecommissionAvailable: null,
  isNodeManagerDecommissioning: null,
  /**
   * load Recommission/Decommission status for nodeManager from nodeManagers list
   */
  loadNodeManagerDecommissionStatus: function () {
    var clusterName = App.router.get('clusterController.clusterName');
    var hostName = App.router.get('mainHostDetailsController.content.hostName');
    var componentName = 'RESOURCEMANAGER';
    var slaveType = 'NODEMANAGER';
    var dfd = $.Deferred();
    var self = this;

    this.getDesiredAdminState(clusterName, hostName, slaveType).done( function () {
      var desired_admin_state = self.get('desiredAdminState');
      self.set('desiredAdminState', null);
      switch(desired_admin_state) {
        case "INSERVICE":
          // can be decommissioned if already started
          self.set('isNodeManagerRecommissionAvailable', false);
          self.set('isNodeManagerDecommissioning', false);
          self.set('isNodeManagerDecommissionAvailable', self.get('isStart'));
          break;
        case "DECOMMISSIONED":
          var deferred = $.Deferred();
          self.getNMDecommissionStatus(clusterName, hostName, componentName).done( function() {
            var curObj = self.get('decommissionedStatusObject');
            self.set('decommissionedStatusObject', null);
            if (curObj && curObj.rm_metrics) {
              var nodeManagersArray = App.parseJSON(curObj.rm_metrics.cluster.nodeManagers);
              if (nodeManagersArray.findProperty('HostName', hostName)){
                // decommisioning ..
                self.set('isNodeManagerRecommissionAvailable', true);
                self.set('isNodeManagerDecommissioning', true);
                self.set('isNodeManagerDecommissionAvailable', false);
              } else {
                // decommissioned ..
                self.set('isNodeManagerRecommissionAvailable', true);
                self.set('isNodeManagerDecommissioning', false);
                self.set('isNodeManagerDecommissionAvailable', false);
              }
            }
            deferred.resolve(curObj);
          });
          break;
      }
      dfd.resolve(desired_admin_state);
    });
    return dfd.promise();
  }.observes('App.router.mainHostDetailsController.content'),

  /**
   * get NodeManager decommission status: from RESOURCEMANAGER component, rm_metrics/nodeManagers property
   */
  getNMDecommissionStatus: function(clusterName, hostName, componentName){
    return App.ajax.send({
      name: 'host.host_component.nodemanager_decommission_status',
      sender: this,
      data: {
        clusterName: clusterName,
        hostName: hostName,
        componentName: componentName
      },
      success: 'getDNDecommissionStatusSuccessCallback',
      error: 'getDNDecommissionStatusErrorCallback'
    });
  },

  isTaskTrackerDecommissionAvailable: null,
  isTaskTrackerRecommissionAvailable: null,
  isTaskTrackerDecommissioning: null,
  /**
   * load Recommission/Decommission status for TaskTracker from JobTracker/AliveNodes list
   */
  loadTaskTrackerDecommissionStatus: function () {
    var clusterName = App.router.get('clusterController.clusterName');
    var hostName = App.router.get('mainHostDetailsController.content.hostName');
    var componentName = 'JOBTRACKER';
    var slaveType = 'TASKTRACKER';
    var dfd = $.Deferred();
    var self = this;
    this.getDesiredAdminState(clusterName, hostName, slaveType).done( function () {
      var desired_admin_state = self.get('desiredAdminState');
      self.set('desiredAdminState', null);
      switch(desired_admin_state) {
        case "INSERVICE":
          // can be decommissioned if already started
          self.set('isTaskTrackerRecommissionAvailable', false);
          self.set('isTaskTrackerDecommissioning', false);
          self.set('isTaskTrackerDecommissionAvailable', self.get('isStart'));
          break;
        case "DECOMMISSIONED":
          var deferred = $.Deferred();
          self.getTTDecommissionStatus(clusterName, hostName, componentName).done( function() {
            var curObj = self.get('decommissionedStatusObject');
            self.set('decommissionedStatusObject', null);
            if (curObj) {
              var aliveNodesArray = App.parseJSON(curObj.AliveNodes);
              if (aliveNodesArray != null) {
                if (aliveNodesArray.findProperty('hostname', hostName)){
                  //decommissioning ..
                  self.set('isTaskTrackerRecommissionAvailable', true);
                  self.set('isTaskTrackerDecommissioning', true);
                  self.set('isTaskTrackerDecommissionAvailable', false);
                } else {
                  //decommissioned
                  self.set('isTaskTrackerRecommissionAvailable', true);
                  self.set('isTaskTrackerDecommissioning', false);
                  self.set('isTaskTrackerDecommissionAvailable', false);
                }
              }

            }
            deferred.resolve(curObj);
          });
          break;
      }
      dfd.resolve(desired_admin_state);
    });
    return dfd.promise();
  }.observes('App.router.mainHostDetailsController.content'),

  /**
   * get TaskTracker decommission status: from JobTracker component, AliveNodes property
   */
  getTTDecommissionStatus: function(clusterName, hostName, componentName){
    return App.ajax.send({
      name: 'host.host_component.tasktracker_decommission_status',
      sender: this,
      data: {
        clusterName: clusterName,
        hostName: hostName,
        componentName: componentName
      },
      success: 'getDNDecommissionStatusSuccessCallback',
      error: 'getDNDecommissionStatusErrorCallback'
    });
  },

  isRegionServerDecommissioning: null,
  isRegionServerDecommissionAvailable: null,
  isRegionServerRecommissionAvailable: null,
  /**
   * load Recommission/Decommission status of RegionServer
   */
  loadRegionServerDecommissionStatus: function () {
    var clusterName = App.router.get('clusterController.clusterName');
    var hostName = App.router.get('mainHostDetailsController.content.hostName');
    var slaveType = 'HBASE_REGIONSERVER';
    var self = this;
    var deferred = $.Deferred();
    self.getDesiredAdminState(clusterName, hostName, slaveType).done( function () {
      var desired_admin_state = self.get('desiredAdminState');
      self.set('desiredAdminState', null);
      switch(desired_admin_state) {
        case "INSERVICE":
          self.set('isRegionServerRecommissionAvailable', false);
          self.set('isRegionServerDecommissioning', false);
          self.set('isRegionServerDecommissionAvailable', self.get('isStart'));
          break;
        case "DECOMMISSIONED":
          self.set('isRegionServerRecommissionAvailable', true);
          self.set('isRegionServerDecommissioning', self.get('isStart'));
          self.set('isRegionServerDecommissionAvailable', false);
          break;
      }
      deferred.resolve(desired_admin_state);
    });
    return deferred.promise();
  }.observes('App.router.mainHostDetailsController.content'),

  /**
   * Shows whether we need to show Delete button
   */
  isDeletableComponent: function () {
    return App.get('components.deletable').contains(this.get('content.componentName'));
  }.property('content'),

  isDeleteComponentDisabled: function () {
    return !(this.get('workStatus') == App.HostComponentStatus.stopped || this.get('workStatus') == App.HostComponentStatus.unknown ||
      this.get('workStatus') == App.HostComponentStatus.install_failed || this.get('workStatus') == App.HostComponentStatus.upgrade_failed);
  }.property('workStatus'),

  isReassignable: function () {
    return App.supports.reassignMaster && App.get('components.reassignable').contains(this.get('content.componentName')) && App.Host.find().content.length > 1;
  }.property('content.componentName'),

  isRestartableComponent: function() {
    return App.get('components.restartable').contains(this.get('content.componentName'));
  }.property('content'),

  isRestartComponentDisabled: function() {
    var allowableStates = [App.HostComponentStatus.started];
    return !allowableStates.contains(this.get('workStatus'));
  }.property('workStatus')

});
