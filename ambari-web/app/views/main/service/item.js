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
      ADD_HBASE_MASTER_COMPONENT: {
        action: 'addHbaseMaster',
        cssClass: 'icon-plus',
        'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hbase.masterServer')),
        disabled: this.get('controller.isAddHBaseMasterDisabled'),
        tooltip: this.get('controller.addHBaseMasterDisabledTooltip')
      },
      ADD_ZOO_KEEPER_SERVER_COMPONENT: {
        action: 'addZooKeeperServer',
        cssClass: 'icon-plus',
        'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.zookeeper.server')),
        disabled: this.get('controller.isAddZooKeeperServerDisabled'),
        tooltip: this.get('controller.addZooKeeperServerDisabledTooltip')
      }
    }
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

    if (this.get('controller.isClientsOnlyService')) {
      options.push(actionMap.RUN_SMOKE_TEST);
      options.push(actionMap.REFRESH_CONFIGS);
      if (this.get('serviceName') === 'TEZ') {
        options = options.without(actionMap.RUN_SMOKE_TEST);
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

      options.push(actionMap.RUN_SMOKE_TEST);
      options.push(actionMap.TOGGLE_PASSIVE);

      var serviceName = service.get('serviceName');
      if (serviceName === 'HBASE') {
        options.push(actionMap.ADD_HBASE_MASTER_COMPONENT);
      }
      if (serviceName === 'ZOOKEEPER') {
        options.push(actionMap.ADD_ZOO_KEEPER_SERVER_COMPONENT);
      }
    }
    return options;
  }.property('controller.content', 'controller.isStopDisabled','controller.isClientsOnlyService', 'controller.content.isRestartRequired', 'isPassive'),

  isMaintenanceActive: function() {
    return this.get('maintenance').length !== 0;
  }.property('maintenance'),

  hasConfigTab: function() {
    return !App.get('services.noConfigTypes').concat('HCATALOG').contains(this.get('controller.content.serviceName'));
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
