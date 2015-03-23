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
       'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hive.metastore')),
       service: 'HIVE',
       component: 'HIVE_METASTORE',
       isHidden: !App.get('isHadoop22Stack')
      },
      {
       cssClass: 'icon-plus',
       'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hive.server2')),
       service: 'HIVE',
       component: 'HIVE_SERVER',
       isHidden: !App.get('isHadoop22Stack')
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
   * @param {Object} option - one of the options that return by <code>App.HostComponentActionMap.getMap()</code>
   * @param {Object} fields - option fields to add/rewrite
   * @return {Object}
   */
  createOption: function(option, fields) {
    return $.extend(true, {}, option, fields);
  },

  maintenance: [],

  observeMaintenance: function() {
    Em.run.once(this, 'observeMaintenanceOnce');
  },

  observeMaintenanceOnce: function() {
    var self = this;
    var options = [];
    var service = this.get('controller.content');
    var allMasters = service.get('hostComponents').filterProperty('isMaster').mapProperty('componentName').uniq();
    var allSlaves = service.get('slaveComponents').rejectProperty('totalCount', 0).mapProperty('componentName');
    var actionMap = App.HostComponentActionMap.getMap(this);
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
        options.push(actionMap.REFRESHQUEUES);
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
          label: actionMap.MOVE_COMPONENT.label.format(App.format.role(master)),
          disabled: App.allHostNames.length === App.HostComponent.find().filterProperty('componentName', master).mapProperty('hostName').length
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
        options.push(actionMap.REBALANCEHDFS);
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

    if (!this.get('maintenance').length) {
      this.set('maintenance', options);
    } else {
      this.get('maintenance').forEach(function(option, index) {
        if ( JSON.stringify(option) != JSON.stringify(options[index])  ) {
          self.get('maintenance').removeAt(index).insertAt(index, options[index]);
        }
      });
      options.forEach(function(opt, index) {
        if ( JSON.stringify(opt) != JSON.stringify(self.get('maintenance')[index])  ) {
          self.get('maintenance').pushObject(opt);
        }
      });
    }
  },

  isMaintenanceActive: function() {
    return this.get('state') !== 'inDOM' || this.get('maintenance').length !== 0;
  }.property('maintenance'),

  hasConfigTab: function() {
    return !App.get('services.noConfigTypes').contains(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName','App.services.noConfigTypes'),

  didInsertElement: function () {
    this.get('controller').setStartStopState();
    if (App.get('supports.customizedWidgets')) {
      var serviceName = this.get('controller.content.serviceName');
      var stackService = App.StackService.find().findProperty('serviceName', serviceName);
      if (stackService.get('isServiceWithWidgets')) {
        this.get('controller').loadWidgets();
      }
    }
  },

  willInsertElement: function () {
    this.addObserver('controller.isStopDisabled', this, 'observeMaintenance');
    this.addObserver('controller.isClientsOnlyService', this, 'observeMaintenance');
    this.addObserver('controller.content.isRestartRequired', this, 'observeMaintenance');
  },

  willDestroyElement: function() {
    this.removeObserver('controller.isStopDisabled', this, 'observeMaintenance');
    this.removeObserver('controller.isClientsOnlyService', this, 'observeMaintenance');
    this.removeObserver('controller.content.isRestartRequired', this, 'observeMaintenance');
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
