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
var batchUtils = require('utils/batch_scheduled_requests');

App.MainServiceItemView = Em.View.extend({
  templateName: require('templates/main/service/item'),

  serviceName: function() {
    return this.get('controller.content.serviceName');
  }.property('controller.content.serviceName'),

  displayName: function() {
    return this.get('controller.content.displayName');
  }.property('controller.content.displayName'),

  isPassive: function() {
    return this.get('controller.content.passiveState') === 'ON';
  }.property('controller.content.passiveState'),

  /**
   * Some custom commands need custom logic to be executed
   */
  mastersExcludedCommands: {
    'NAMENODE': ['DECOMMISSION', 'REBALANCEHDFS'],
    'RESOURCEMANAGER': ['DECOMMISSION', 'REFRESHQUEUES'],
    'HBASE_MASTER': ['DECOMMISSION'],
    'KNOX_GATEWAY': ['STARTDEMOLDAP','STOPDEMOLDAP']
  },

  actionMap: function() {
    return {
      RESTART_ALL: {
        action: 'restartAllHostComponents',
        context: this.get('serviceName'),
        label: Em.I18n.t('restart.service.all'),
        cssClass: 'icon-repeat',
        disabled: false
      },
      RUN_SMOKE_TEST: {
        action: 'runSmokeTest',
        label: Em.I18n.t('services.service.actions.run.smoke'),
        cssClass: 'icon-thumbs-up-alt'
      },
      REFRESH_CONFIGS: {
        action: 'refreshConfigs',
        label: Em.I18n.t('hosts.host.details.refreshConfigs'),
        cssClass: 'icon-refresh',
        disabled: !this.get('controller.content.isRestartRequired')
      },
      REFRESH_YARN_QUEUE: {
        action: 'refreshYarnQueues',
        customCommand: 'REFRESHQUEUES',
        label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
        cssClass: 'icon-refresh',
        disabled: false
      },
      ROLLING_RESTART: {
        action: 'rollingRestart',
        context: this.get('rollingRestartComponent'),
        label: Em.I18n.t('rollingrestart.dialog.title'),
        cssClass: 'icon-time',
        disabled: false
      },
      TOGGLE_PASSIVE: {
        action: 'turnOnOffPassive',
        context: this.get('isPassive') ? Em.I18n.t('passiveState.turnOffFor').format(this.get('displayName')) : Em.I18n.t('passiveState.turnOnFor').format(this.get('displayName')),
        label: this.get('isPassive') ? Em.I18n.t('passiveState.turnOff') : Em.I18n.t('passiveState.turnOn'),
        cssClass: 'icon-medkit',
        disabled: false
      },
      TOGGLE_NN_HA: {
        action: App.get('isHaEnabled') ? 'disableHighAvailability' : 'enableHighAvailability',
        label: App.get('isHaEnabled') ? Em.I18n.t('admin.highAvailability.button.disable') : Em.I18n.t('admin.highAvailability.button.enable'),
        cssClass: App.get('isHaEnabled') ? 'icon-arrow-down' : 'icon-arrow-up',
        isHidden: (App.get('isHaEnabled') && !App.get('supports.autoRollbackHA'))
      },
      TOGGLE_RM_HA: {
        action: 'enableRMHighAvailability',
        label: Em.I18n.t('admin.rm_highAvailability.button.enable'),
        cssClass: 'icon-arrow-up',
        isHidden: !App.get('supports.resourceManagerHighAvailability') || App.get('isRMHaEnabled')
      },
      MOVE_COMPONENT: {
        action: 'reassignMaster',
        context: '',
        label: Em.I18n.t('services.service.actions.reassign.master'),
        cssClass: 'icon-share-alt',
        disabled: false
      },
      STARTDEMOLDAP: {
        action: 'startLdapKnox',
        customCommand: 'STARTDEMOLDAP',
        label: Em.I18n.t('services.service.actions.run.startLdapKnox.context'),
        cssClass: 'icon-play-sign',
        disabled: false
      },
      STOPDEMOLDAP: {
        action: 'stopLdapKnox',
        customCommand: 'STOPDEMOLDAP',
        label: Em.I18n.t('services.service.actions.run.stopLdapKnox.context'),
        cssClass: 'icon-stop',
        disabled: false
      },
      REBALANCE_HDFS: {
        action: 'rebalanceHdfsNodes',
        customCommand: 'REBALANCEHDFS',
        context: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.context'),
        label: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes'),
        cssClass: 'icon-refresh',
        disabled: false
      },
      DOWNLOAD_CLIENT_CONFIGS: {
        action: this.get('controller.isSeveralClients') ? '' : 'downloadClientConfigs',
        label: Em.I18n.t('services.service.actions.downloadClientConfigs'),
        cssClass: 'icon-download-alt',
        isHidden: !(App.get('supports.downloadClientConfigs') && this.get('controller.content.hostComponents').findProperty('isClient')),
        disabled: false,
        hasSubmenu: this.get('controller.isSeveralClients'),
        submenuOptions: this.get('controller.clientComponents')
      },
      MASTER_CUSTOM_COMMAND: {
        action: 'executeCustomCommand',
        cssClass: 'icon-play-circle',
        isHidden: false,
        disabled: false
      }
    }
  },

   addActionMap: function() {
     return [
      {
        cssClass: 'icon-plus',
        'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hbase.masterServer')),
        service: 'HBASE',
        component: 'HBASE_MASTER'
      },
      {
        cssClass: 'icon-plus',
        'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.zookeeper.server')),
        service: 'ZOOKEEPER',
        component: 'ZOOKEEPER_SERVER'
      },
      {
        cssClass: 'icon-plus',
        'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.flume.agentLabel')),
        service: 'FLUME',
        component: 'FLUME_HANDLER'
      }
    ]
  },
  /**
   * Create option for MOVE_COMPONENT or ROLLING_RESTART task.
   *
   * @param {Object} option - one of the options that return by <code>actionMap()</code>
   * @param {Object} fields - option fields to add/rewrite
   * @return {Object}
   */
  createOption: function(option, fields) {
    return $.extend(true, {}, option, fields);
  },

  maintenance: function(){
    var self = this;
    var options = [];
    var service = this.get('controller.content');
    var allMasters = service.get('hostComponents').filterProperty('isMaster').mapProperty('componentName').uniq();
    var allSlaves = service.get('hostComponents').filterProperty('isSlave').mapProperty('componentName').uniq();
    var actionMap = this.actionMap();
    var serviceCheckSupported = App.get('services.supportsServiceCheck').contains(service.get('serviceName'));
    var hasConfigTab = this.get('hasConfigTab');
    var excludedCommands = this.get('mastersExcludedCommands');

    if (this.get('controller.isClientsOnlyService')) {
      if (serviceCheckSupported) {
        options.push(actionMap.RUN_SMOKE_TEST);
      }
      if (hasConfigTab) {
	      options.push(actionMap.REFRESH_CONFIGS);
	    }
    } else {
      if (this.get('serviceName') === 'FLUME') {
        options.push(actionMap.REFRESH_CONFIGS);
      }
      if (this.get('serviceName') === 'YARN') {
        options.push(actionMap.REFRESH_YARN_QUEUE);
      }
      options.push(actionMap.RESTART_ALL);
      allSlaves.filter(function (slave) {
        return App.get('components.rollinRestartAllowed').contains(slave);
      }).forEach(function(slave) {
        options.push(self.createOption(actionMap.ROLLING_RESTART, {
          context: slave,
          label: actionMap.ROLLING_RESTART.label.format(App.format.role(slave))
        }));
      });
      allMasters.filter(function(master) {
        return App.get('components.reassignable').contains(master);
      }).forEach(function(master) {
        options.push(self.createOption(actionMap.MOVE_COMPONENT, {
          context: master,
          label: actionMap.MOVE_COMPONENT.label.format(App.format.role(master))
        }));
      });
      if (service.get('serviceTypes').contains('HA_MODE')) {
        switch (service.get('serviceName')) {
          case 'HDFS':
            options.push(actionMap.TOGGLE_NN_HA);
            break;
          case 'YARN':
            options.push(actionMap.TOGGLE_RM_HA);
            break;
        }
      }
      if (serviceCheckSupported) {
        options.push(actionMap.RUN_SMOKE_TEST);
      }
      options.push(actionMap.TOGGLE_PASSIVE);
      var serviceName = service.get('serviceName');
      var nnComponent = App.StackServiceComponent.find().findProperty('componentName','NAMENODE');
      var knoxGatewayComponent = App.StackServiceComponent.find().findProperty('componentName','KNOX_GATEWAY');
      if (serviceName === 'HDFS' && nnComponent) {
        var namenodeCustomCommands = nnComponent.get('customCommands');
        if (namenodeCustomCommands && namenodeCustomCommands.contains('REBALANCEHDFS'))
        options.push(actionMap.REBALANCE_HDFS);
      }

      if (serviceName === 'KNOX' && knoxGatewayComponent) {
        var knoxGatewayCustomCommands = knoxGatewayComponent.get('customCommands');
        knoxGatewayCustomCommands.forEach(function(command) {
          if (actionMap[command]) {
            options.push(actionMap[command]);
          }
        });
      }
      self.addActionMap().filterProperty('service', serviceName).forEach(function(item) {
        item.action = 'add' + item.component;
        item.disabled = self.get('controller.isAddDisabled-' + item.component);
        item.tooltip = self.get('controller.addDisabledTooltip' + item.component);
        options.push(item);
      });

      allMasters.forEach(function(master) {
        var component = App.StackServiceComponent.find(master);
        var commands = component.get('customCommands');

        if (!commands.length) {
          return false;
        }

        commands.forEach(function(command) {
          if (excludedCommands[master] && excludedCommands[master].contains(command)){
            return false;
          }

          options.push(self.createOption(actionMap.MASTER_CUSTOM_COMMAND, {
            label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format(command),
            context: {
              label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format(command),
              service: component.get('serviceName'),
              component: component.get('componentName'),
              command: command
            }
          }));
        });
      });
    }

    if (hasConfigTab) {
      options.push(actionMap.DOWNLOAD_CLIENT_CONFIGS);
    }

    return options;
  }.property('controller.content', 'controller.isStopDisabled','controller.isClientsOnlyService', 'controller.content.isRestartRequired', 'isPassive'),

  isMaintenanceActive: function() {
    return this.get('maintenance').length !== 0;
  }.property('maintenance'),

  hasConfigTab: function() {
    return !App.get('services.noConfigTypes').contains(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName','App.services.noConfigTypes'),

  didInsertElement: function () {
    this.get('controller').setStartStopState();
  },
  service:function () {
    var svc = this.get('controller.content');
    var svcName = svc.get('serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'yarn':
          svc = App.YARNService.find().objectAt(0);
          break;
        case 'mapreduce':
          svc = App.MapReduceService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName').volatile()
});
