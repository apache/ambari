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

App.MainServiceItemView = Em.View.extend({
  templateName: require('templates/main/service/item'),

  serviceName: Em.computed.alias('controller.content.serviceName'),

  displayName: Em.computed.alias('controller.content.displayName'),

  isPassive: Em.computed.equal('controller.content.passiveState', 'ON'),

  /**
   * Some custom commands need custom logic to be execute.
   * Typically, these services already have Custom Commands, so we must exclude the default ones
   * in order to make more changes to them like icons and rules.
   */
  mastersExcludedCommands: {
    'NAMENODE': ['DECOMMISSION', 'REBALANCEHDFS'],
    'RESOURCEMANAGER': ['DECOMMISSION', 'REFRESHQUEUES'],
    'HBASE_MASTER': ['DECOMMISSION'],
    'KNOX_GATEWAY': ['STARTDEMOLDAP','STOPDEMOLDAP'],
    'HAWQMASTER': ['IMMEDIATE_STOP_HAWQ_SERVICE', 'RUN_HAWQ_CHECK', 'HAWQ_CLEAR_CACHE', 'REMOVE_HAWQ_STANDBY', 'RESYNC_HAWQ_STANDBY'],
    'HAWQSEGMENT': ['IMMEDIATE_STOP_HAWQ_SEGMENT'],
    'HAWQSTANDBY': ['ACTIVATE_HAWQ_STANDBY'],
    'HIVE_SERVER_INTERACTIVE' : ["RESTART_LLAP"]
  },

   addActionMap: function() {
     return [
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hbase.masterServer')),
         service: 'HBASE',
         component: 'HBASE_MASTER'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hive.metastore')),
         service: 'HIVE',
         component: 'HIVE_METASTORE'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.hive.server2')),
         service: 'HIVE',
         component: 'HIVE_SERVER'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.zookeeper.server')),
         service: 'ZOOKEEPER',
         component: 'ZOOKEEPER_SERVER'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), Em.I18n.t('dashboard.services.flume.agentLabel')),
         service: 'FLUME',
         component: 'FLUME_HANDLER'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), App.format.role('RANGER_KMS_SERVER', false)),
         service: 'RANGER_KMS',
         component: 'RANGER_KMS_SERVER'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), App.format.role('NIMBUS', false)),
         service: 'STORM',
         component: 'NIMBUS'
       },
       {
         cssClass: 'glyphicon glyphicon-plus',
         'label': '{0} {1}'.format(Em.I18n.t('add'), App.format.role('OOZIE_SERVER', false)),
         service: 'OOZIE',
         component: 'OOZIE_SERVER'
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

  isMaintenanceSet: false,

  observeMaintenance: function() {
    if (!this.get('isMaintenanceSet') && this.get('controller.isServicesInfoLoaded') && this.get('controller.isServiceConfigsLoaded')) {
      this.observeMaintenanceOnce();
    }
    Em.run.once(this, 'clearIsMaintenanceSet');
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
      allSlaves.concat(allMasters).filter(function (_component) {
        return App.get('components.rollinRestartAllowed').contains(_component);
      }).forEach(function(_component) {
        var _componentNamePluralized = pluralize(App.format.role(_component, false));
        options.push(self.createOption(actionMap.ROLLING_RESTART, {
          context: _component,
          label: actionMap.ROLLING_RESTART.label.format(_componentNamePluralized)
        }));
      });
      allMasters.filter(function(master) {
        return App.get('components.reassignable').contains(master);
      }).forEach(function(master) {
        options.push(self.createOption(actionMap.MOVE_COMPONENT, {
          context: master,
          label: actionMap.MOVE_COMPONENT.label.format(App.format.role(master, false)),
          disabled: App.allHostNames.length === App.HostComponent.find().filterProperty('componentName', master).mapProperty('hostName').length
        }));
      });
      // add "Manage JournalNode" when NNHA is enabled and there is more hosts than JNs
      var JNCount = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').get('length');
      if (App.get('supports.manageJournalNode') && service.get('serviceName') == 'HDFS' && service.get('serviceTypes').contains('HA_MODE')
          && (App.router.get('mainHostController.totalCount') > JNCount || JNCount > 3)) {
        options.push(actionMap.MANAGE_JN);
      }
      if (service.get('serviceTypes').contains('HA_MODE') && App.isAuthorized('SERVICE.ENABLE_HA')) {
        switch (service.get('serviceName')) {
          case 'HDFS':
            options.push(actionMap.TOGGLE_NN_HA);
            break;
          case 'YARN':
            options.push(actionMap.TOGGLE_RM_HA);
            break;
          case 'RANGER':
            options.push(actionMap.TOGGLE_RA_HA);
            break;
          case 'HAWQ':
            options.push(actionMap.TOGGLE_ADD_HAWQ_STANDBY);
            break;
        }
      }
      if (serviceCheckSupported) {
        options.push(actionMap.RUN_SMOKE_TEST);
      }
      options.push(actionMap.TOGGLE_PASSIVE);
      var serviceName = service.get('serviceName');
      var nnComponent = App.StackServiceComponent.find().findProperty('componentName', 'NAMENODE');
      var knoxGatewayComponent = App.StackServiceComponent.find().findProperty('componentName', 'KNOX_GATEWAY');
      if (serviceName === 'HDFS' && nnComponent) {
        var namenodeCustomCommands = nnComponent.get('customCommands');
        if (namenodeCustomCommands && namenodeCustomCommands.contains('REBALANCEHDFS')) {
          options.push(actionMap.REBALANCEHDFS);
        }
      }

      if (serviceName === 'KNOX' && knoxGatewayComponent) {
        var knoxGatewayCustomCommands = knoxGatewayComponent.get('customCommands');
        knoxGatewayCustomCommands.forEach(function(command) {
          if (actionMap[command]) {
            options.push(actionMap[command]);
          }
        });
      }

      if (serviceName === 'HIVE') {
        var hiveServerInteractiveComponent = App.StackServiceComponent.find().findProperty('componentName', 'HIVE_SERVER_INTERACTIVE');
        var isHiveInteractiveServerPresent = allMasters.contains('HIVE_SERVER_INTERACTIVE');
        if (hiveServerInteractiveComponent && isHiveInteractiveServerPresent) {
          var LLAPCustomCommands = hiveServerInteractiveComponent.get('customCommands');
          LLAPCustomCommands.forEach(function (command) {
            if (actionMap[command]) {
              options.push(actionMap[command]);
            }
          });
        }
      }

      /**
       * Display all custom commands of Master and StandBy on Service page.
       **/
      if(serviceName === 'HAWQ') {
        var hawqMasterComponent = App.StackServiceComponent.find().findProperty('componentName','HAWQMASTER');
        var hawqStandByComponent = App.StackServiceComponent.find().findProperty('componentName','HAWQSTANDBY');
        [hawqMasterComponent,hawqStandByComponent].forEach(function(component){
          component.get('customCommands').forEach(function(command){
            options.push(self.createOption(actionMap[command], {
              context: {
                label: actionMap[command].context,
                service: component.get('serviceName'),
                component: component.get('componentName'),
                command: command
              }
            }));
          });
        });
      }

      if(App.isAuthorized('HOST.ADD_DELETE_COMPONENTS')){
        self.addActionMap().filterProperty('service', serviceName).forEach(function(item) {
        if (App.get('components.addableToHost').contains(item.component)) {

          var isEnabled = App.HostComponent.find().filterProperty('componentName', item.component).length < App.get('allHostNames.length');

          if (item.component === 'OOZIE_SERVER') {
            isEnabled = isEnabled && !(Em.isEmpty(self.get('controller.configs.oozie-env.oozie_database')) || self.get('controller.configs.oozie-env.oozie_database') === 'New Derby Database');
          }

          item.action = 'addComponent';
          item.disabled = isEnabled ? '' : 'disabled';
          item.tooltip = isEnabled ? '' : Em.I18n.t('services.summary.allHostsAlreadyRunComponent').format(item.component);
          item.context = item.component;

          options.push(item);
        }
       });
      }

      if (App.get('isKerberosEnabled')){
        options.push(actionMap.REGENERATE_KEYTAB_FILE_OPERATIONS);
      }

      allMasters.forEach(function(master) {
        var component = App.StackServiceComponent.find(master);
        var commands = component.get('customCommands');

        if (!commands.length) {
          return;
        }

        commands.forEach(function(command) {
          if (excludedCommands[master] && excludedCommands[master].contains(command)){
            return;
          }

          options.push(self.createOption(actionMap.MASTER_CUSTOM_COMMAND, {
            label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format(App.format.normalizeNameBySeparators(command, ["_", "-", " "])),
            context: {
              label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format(App.format.normalizeNameBySeparators(command, ["_", "-", " "])),
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

    if (App.isAuthorized("SERVICE.ADD_DELETE_SERVICES") && App.supports.enableAddDeleteServices) {
      options.push(actionMap.DELETE_SERVICE);
    }

    if (this.get('maintenance.length')) {
      this.get('maintenance').forEach(function(option, index) {
        if (JSON.stringify(option) !== JSON.stringify(options[index])) {
          self.get('maintenance').removeAt(index).insertAt(index, options[index]);
        }
      });
      options.forEach(function(opt, index) {
        if (JSON.stringify(opt) !== JSON.stringify(self.get('maintenance')[index])) {
          self.get('maintenance').pushObject(opt);
        }
      });
    } else {
      this.set('maintenance', options);
    }
    this.set('isMaintenanceSet', true);
  },

  clearIsMaintenanceSet: function () {
    this.set('isMaintenanceSet', false);
  },

  isMaintenanceActive: function() {
    return this.get('state') !== 'inDOM' || this.get('maintenance').length !== 0;
  }.property('maintenance'),

  hasConfigTab: function() {
    return App.havePermissions('CLUSTER.VIEW_CONFIGS') && !App.get('services.noConfigTypes').contains(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName','App.services.noConfigTypes'),

  hasHeatmapTab: function() {
    return App.get('services.servicesWithHeatmapTab').contains(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName', 'App.services.servicesWithHeatmapTab'),

  hasMetricTab: function() {
    let serviceName = this.get('controller.content.serviceName');
    let graphs = require('data/service_graph_config')[serviceName.toLowerCase()];
    return graphs || App.StackService.find(serviceName).get('isServiceWithWidgets');
  }.property('controller.content.serviceName'),

  didInsertElement: function () {
    this.get('controller').setStartStopState();
  },

  maintenanceObsFields: ['isStopDisabled', 'isClientsOnlyService', 'content.isRestartRequired', 'isServicesInfoLoaded', 'isServiceConfigsLoaded'],

  willInsertElement: function () {
    var self = this;
    this.get('controller').loadConfigs();
    this.get('maintenanceObsFields').forEach(function (field) {
      self.addObserver('controller.' + field, self, 'observeMaintenance');
    });
  },

  willDestroyElement: function() {
    var self = this;
    this.get('maintenanceObsFields').forEach(function (field) {
      self.removeObserver('controller.' + field, self, 'observeMaintenance');
    });
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
