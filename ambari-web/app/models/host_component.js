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

App.HostComponent = DS.Model.extend({
  workStatus: DS.attr('string'),
  passiveState: DS.attr('string'),
  componentName: DS.attr('string'),
  displayName: DS.attr('string'),
  haStatus: DS.attr('string'),
  displayNameAdvanced: DS.attr('string'),
  staleConfigs: DS.attr('boolean'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  adminState: DS.attr('string'),

  summaryLabelClassName:function(){
    return 'label_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),

  summaryValueClassName:function(){
    return 'value_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),
  /**
   * Determine if component is client
   * @returns {bool}
   */
  isClient: function () {
    return App.get('components.clients').contains(this.get('componentName'));
  }.property('componentName'),
  /**
   * Determine if component is running now
   * Based on <code>workStatus</code>
   * @returns {bool}
   */
  isRunning: function () {
    return (this.get('workStatus') == 'STARTED' || this.get('workStatus') == 'STARTING');
  }.property('workStatus'),

  /**
   * Determine if component is master
   * @returns {bool}
   */
  isMaster: function () {
    return App.get('components.masters').contains(this.get('componentName'));
  }.property('componentName', 'App.components.masters'),

  /**
   * Determine if component is slave
   * @returns {bool}
   */
  isSlave: function () {
    return App.get('components.slaves').contains(this.get('componentName'));
  }.property('componentName'),
  /**
   * Only certain components can be deleted.
   * They include some from master components,
   * some from slave components, and rest from
   * client components.
   * @returns {bool}
   */
  isDeletable: function () {
    return App.get('components.deletable').contains(this.get('componentName'));
  }.property('componentName'),
  /**
   * A host-component is decommissioning when it is in HDFS service's list of
   * decomNodes.
   * @returns {bool}
   */
  isDecommissioning: function () {
    var decommissioning = false;
    var hostName = this.get('hostName');
    var componentName = this.get('componentName');
    var hdfsSvc = App.HDFSService.find().objectAt(0);
    if (componentName === 'DATANODE' && hdfsSvc) {
      var decomNodes = hdfsSvc.get('decommissionDataNodes');
      var decomNode = decomNodes != null ? decomNodes.findProperty("hostName", hostName) : null;
      decommissioning = decomNode != null;
    }
    return decommissioning;
  }.property('componentName', 'hostName', 'App.router.clusterController.isLoaded', 'App.router.updateController.isUpdated'),
  /**
   * User friendly host component status
   * @returns {String}
   */
  isActive: function () {
    return (this.get('passiveState') == 'OFF');
  }.property('passiveState'),

  /**
   * Determine if component is a HDP component
   * @returns {bool}
   */
  isHDPComponent: function () {
    return !App.get('components.nonHDP').contains(this.get('componentName'));
  }.property('componentName', 'App.components.nonHDP'),

  passiveTooltip: function () {
    if (!this.get('isActive')) {
      return Em.I18n.t('hosts.component.passive.mode');
    }
  }.property('isActive'),

  statusClass: function () {
    return this.get('isActive') ? this.get('workStatus') : 'icon-medkit';
  }.property('workStatus', 'isActive'),

  statusIconClass: function () {
    switch (this.get('statusClass')) {
      case 'STARTED':
      case 'STARTING':
        return App.healthIconClassGreen;
        break;
      case 'INSTALLED':
      case 'STOPPING':
        return App.healthIconClassRed;
        break;
      case 'UNKNOWN':
        return App.healthIconClassYellow;
        break;
      default:
        return "";
        break;
    }
  }.property('statusClass'),

  componentTextStatus: function () {
    return App.HostComponentStatus.getTextStatus(this.get("workStatus"));
  }.property('workStatus', 'isDecommissioning')
});

App.HostComponent.FIXTURES = [];


/**
 * get particular counter of host-component by name
 * @param {string} componentName
 * @param {string} type (installedCount|startedCount|totalCount)
 * @returns {number}
 */
App.HostComponent.getCount = function (componentName, type) {
  switch (App.StackServiceComponent.find(componentName).get('componentCategory')) {
    case 'MASTER':
      return Number(App.MasterComponent.find(componentName).get(type));
    case 'SLAVE':
      return Number(App.SlaveComponent.find(componentName).get(type));
    case 'CLIENT':
      return Number(App.ClientComponent.find(componentName).get(type));
    default:
      return 0;
  }
};

App.HostComponentStatus = {
  started: "STARTED",
  starting: "STARTING",
  stopped: "INSTALLED",
  stopping: "STOPPING",
  install_failed: "INSTALL_FAILED",
  installing: "INSTALLING",
  upgrade_failed: "UPGRADE_FAILED",
  unknown: "UNKNOWN",
  disabled: "DISABLED",
  init: "INIT",

  /**
   * Get host component status in "machine" format
   * @param {String} value
   * @returns {String}
   */
  getKeyName: function (value) {
    switch (value) {
      case this.started:
        return 'started';
      case this.starting:
        return 'starting';
      case this.stopped:
        return 'installed';
      case this.stopping:
        return 'stopping';
      case this.install_failed:
        return 'install_failed';
      case this.installing:
        return 'installing';
      case this.upgrade_failed:
        return 'upgrade_failed';
      case this.disabled:
      case this.unknown:
        return 'unknown';
    }
    return 'unknown';
  },

  /**
   * Get user-friendly host component status
   * @param {String} value
   * @returns {String}
   */
  getTextStatus: function (value) {
    switch (value) {
      case this.installing:
        return 'Installing...';
      case this.install_failed:
        return 'Install Failed';
      case this.stopped:
        return 'Stopped';
      case this.started:
        return 'Started';
      case this.starting:
        return 'Starting...';
      case this.stopping:
        return 'Stopping...';
      case this.unknown:
        return 'Heartbeat Lost';
      case this.upgrade_failed:
        return 'Upgrade Failed';
      case this.disabled:
        return 'Disabled';
      case this.init:
        return 'Install Pending...';
    }
    return 'Unknown';
  },

  /**
   * Get list of possible <code>App.HostComponent</code> statuses
   * @returns {String[]}
   */
  getStatusesList: function () {
    var ret = [];
    for (var st in this) {
      if (this.hasOwnProperty(st) && Em.typeOf(this[st]) == 'string') {
        ret.push(this[st]);
      }
    }
    return ret;
  }
};

App.HostComponentActionMap = {
  getMap: function(ctx) {
    var HM = ctx.get('controller.content.hostComponents').findProperty('componentName', 'HAWQMASTER');
    var HS = ctx.get('controller.content.hostComponents').findProperty('componentName', 'HAWQSTANDBY');
    var HMComponent = App.MasterComponent.find('HAWQMASTER');
    var HSComponent = App.MasterComponent.find('HAWQSTANDBY');
    return {
      RESTART_ALL: {
        action: 'restartAllHostComponents',
        context: ctx.get('serviceName'),
        label: Em.I18n.t('restart.service.all'),
        cssClass: 'icon-repeat',
        disabled: false
      },
      RUN_SMOKE_TEST: {
        action: 'runSmokeTest',
        label: Em.I18n.t('services.service.actions.run.smoke'),
        cssClass: 'icon-thumbs-up-alt',
        disabled: ctx.get('controller.isClientsOnlyService') ? false : ctx.get('controller.isStopDisabled')
      },
      REFRESH_CONFIGS: {
        action: 'refreshConfigs',
        label: Em.I18n.t('hosts.host.details.refreshConfigs'),
        cssClass: 'icon-refresh',
        disabled: false
      },
      REFRESHQUEUES: {
        action: 'refreshYarnQueues',
        customCommand: 'REFRESHQUEUES',
        context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
        label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
        cssClass: 'icon-refresh',
        disabled: false
      },
      ROLLING_RESTART: {
        action: 'rollingRestart',
        context: ctx.get('rollingRestartComponent'),
        label: Em.I18n.t('rollingrestart.dialog.title'),
        cssClass: 'icon-time',
        disabled: false
      },
      TOGGLE_PASSIVE: {
        action: 'turnOnOffPassive',
        context: ctx.get('isPassive') ? Em.I18n.t('passiveState.turnOffFor').format(ctx.get('displayName')) : Em.I18n.t('passiveState.turnOnFor').format(ctx.get('displayName')),
        label: ctx.get('isPassive') ? Em.I18n.t('passiveState.turnOff') : Em.I18n.t('passiveState.turnOn'),
        cssClass: 'icon-medkit',
        disabled: false
      },
      TOGGLE_NN_HA: {
        action: App.get('isHaEnabled') ? 'disableHighAvailability' : 'enableHighAvailability',
        label: App.get('isHaEnabled') ? Em.I18n.t('admin.highAvailability.button.disable') : Em.I18n.t('admin.highAvailability.button.enable'),
        cssClass: App.get('isHaEnabled') ? 'icon-arrow-down' : 'icon-arrow-up',
        isHidden: App.get('isHaEnabled'),
        disabled: App.get('isSingleNode')
      },
      TOGGLE_RM_HA: {
        action: 'enableRMHighAvailability',
        label: Em.I18n.t('admin.rm_highAvailability.button.enable'),
        cssClass: 'icon-arrow-up',
        isHidden: App.get('isRMHaEnabled'),
        disabled: App.get('isSingleNode')
      },
      TOGGLE_RA_HA: {
        action: 'enableRAHighAvailability',
        label: Em.I18n.t('admin.ra_highAvailability.button.enable'),
        cssClass: 'icon-arrow-up',
        isHidden: App.get('isRAHaEnabled'),
        disabled: App.get('isSingleNode')
      },
      MOVE_COMPONENT: {
        action: 'reassignMaster',
        context: '',
        label: Em.I18n.t('services.service.actions.reassign.master'),
        cssClass: 'icon-share-alt'
      },
      STARTDEMOLDAP: {
        action: 'startLdapKnox',
        customCommand: 'STARTDEMOLDAP',
        context: Em.I18n.t('services.service.actions.run.startLdapKnox.context'),
        label: Em.I18n.t('services.service.actions.run.startLdapKnox.context'),
        cssClass: 'icon-play-sign',
        disabled: false
      },
      STOPDEMOLDAP: {
        action: 'stopLdapKnox',
        customCommand: 'STOPDEMOLDAP',
        context: Em.I18n.t('services.service.actions.run.stopLdapKnox.context'),
        label: Em.I18n.t('services.service.actions.run.stopLdapKnox.context'),
        cssClass: 'icon-stop',
        disabled: false
      },
      REBALANCEHDFS: {
        action: 'rebalanceHdfsNodes',
        customCommand: 'REBALANCEHDFS',
        context: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.context'),
        label: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes'),
        cssClass: 'icon-refresh',
        disabled: false
      },
      DOWNLOAD_CLIENT_CONFIGS: {
        action: ctx.get('controller.isSeveralClients') ? '' : 'downloadClientConfigs',
        label: Em.I18n.t('services.service.actions.downloadClientConfigs'),
        cssClass: 'icon-download-alt',
        isHidden: !!ctx.get('controller.content.clientComponents') ? ctx.get('controller.content.clientComponents').rejectProperty('totalCount', 0).length == 0 : false,
        disabled: false,
        hasSubmenu: ctx.get('controller.isSeveralClients'),
        submenuOptions: ctx.get('controller.clientComponents')
      },
      IMMEDIATE_STOP_HAWQ_SERVICE: {
        action: 'executeHawqCustomCommand',
        customCommand: 'IMMEDIATE_STOP_HAWQ_SERVICE',
        context: Em.I18n.t('services.service.actions.run.immediateStopHawqService.context'),
        label: Em.I18n.t('services.service.actions.run.immediateStopHawqService.label'),
        cssClass: 'icon-stop',
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started
      },
      IMMEDIATE_STOP_HAWQ_SEGMENT: {
        customCommand: 'IMMEDIATE_STOP_HAWQ_SEGMENT',
        context: Em.I18n.t('services.service.actions.run.immediateStopHawqSegment.context'),
        label: Em.I18n.t('services.service.actions.run.immediateStopHawqSegment.label'),
        cssClass: 'icon-stop'
      },
      RESYNC_HAWQ_STANDBY: {
        action: 'executeHawqCustomCommand',
        customCommand: 'RESYNC_HAWQ_STANDBY',
        context: Em.I18n.t('services.service.actions.run.resyncHawqStandby.context'),
        label: Em.I18n.t('services.service.actions.run.resyncHawqStandby.label'),
        cssClass: 'icon-refresh',
        isHidden : App.get('isSingleNode') || !HS ,
        disabled: !((!!HMComponent && HMComponent.get('startedCount') === 1) && (!!HSComponent && HSComponent.get('startedCount') === 1))
      },
      TOGGLE_ADD_HAWQ_STANDBY: {
        action: 'addHawqStandby',
        label: Em.I18n.t('admin.addHawqStandby.button.enable'),
        cssClass: 'icon-plus',
        isHidden: App.get('isSingleNode') || HS,
        disabled: false
      },
      REMOVE_HAWQ_STANDBY: {
        action: 'removeHawqStandby',
        context: Em.I18n.t('admin.removeHawqStandby.button.enable'),
        label: Em.I18n.t('admin.removeHawqStandby.button.enable'),
        cssClass: 'icon-minus',
        isHidden: App.get('isSingleNode') || !HS,
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started,
        hideFromComponentView: true
      },
      ACTIVATE_HAWQ_STANDBY: {
        action: 'activateHawqStandby',
        context: Em.I18n.t('admin.activateHawqStandby.button.enable'),
        label: Em.I18n.t('admin.activateHawqStandby.button.enable'),
        cssClass: 'icon-arrow-up',
        isHidden: App.get('isSingleNode') || !HS,
        disabled: false,
        hideFromComponentView: true
      },
      HAWQ_CLEAR_CACHE: {
        action: 'executeHawqCustomCommand',
        customCommand: 'HAWQ_CLEAR_CACHE',
        context: Em.I18n.t('services.service.actions.run.clearHawqCache.label'),
        label: Em.I18n.t('services.service.actions.run.clearHawqCache.label'),
        cssClass: 'icon-refresh',
        isHidden : false,
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started
      },
      RUN_HAWQ_CHECK: {
        action: 'executeHawqCustomCommand',
        customCommand: 'RUN_HAWQ_CHECK',
        context: Em.I18n.t('services.service.actions.run.runHawqCheck.label'),
        label: Em.I18n.t('services.service.actions.run.runHawqCheck.label'),
        cssClass: 'icon-thumbs-up-alt',
        isHidden : false,
        disabled: false
      },
      MASTER_CUSTOM_COMMAND: {
        action: 'executeCustomCommand',
        cssClass: 'icon-play-circle',
        isHidden: false,
        disabled: false
      }
    }
  }
};
