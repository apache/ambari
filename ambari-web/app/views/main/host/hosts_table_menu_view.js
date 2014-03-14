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

App.HostTableMenuView = Em.View.extend({

  templateName: require('templates/main/host/bulk_operation_menu'),

  /**
   * Get third-level menu items for slave components
   * @param {String} componentNameForDecommission for decommission and recommission used another component name
   * @param {String} componentNameForOtherActions host component name that should be processed
   * operationData format:
   * <code>
   *  {
   *    action: 'STARTED|INSTALLED|RESTART|DECOMMISSION|DECOMMISSION_OFF', // action for selected host components
   *    message: 'some text', // just text to BG popup
   *    componentName: 'DATANODE|NODEMANAGER...', //component name that should be processed
   *    realComponentName: 'DATANODE|NODEMANAGER...', // used only for decommission(_off) actions
   *    serviceName: 'HDFS|YARN|HBASE...', // service name of the processed component
   *    componentNameFormatted: 'DataNodes|NodeManagers...' // "user-friendly" string with component name (used in BG popup)
   *  }
   *  </code>
   *
   * @returns {Array}
   */
  getSlaveItemsTemplate: function(componentNameForDecommission, componentNameForOtherActions) {
    var menuItems = Em.A([
      Em.Object.create({
        label: Em.I18n.t('common.start'),
        operationData: Em.Object.create({
          action: App.HostComponentStatus.started,
          message: Em.I18n.t('common.start'),
          componentName: componentNameForOtherActions
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('common.stop'),
        operationData: Em.Object.create({
          action: App.HostComponentStatus.stopped,
          message: Em.I18n.t('common.stop'),
          componentName: componentNameForOtherActions
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('common.restart'),
        operationData: Em.Object.create({
          action: 'RESTART',
          message: Em.I18n.t('common.restart'),
          componentName: componentNameForOtherActions
        })
      })
    ]);
    if(App.get('components.decommissionAllowed').contains(componentNameForOtherActions)) {
      menuItems.pushObject(Em.Object.create({
        label: Em.I18n.t('common.decommission'),
        operationData: Em.Object.create({
          action: 'DECOMMISSION',
          message: Em.I18n.t('common.decommission'),
          componentName: componentNameForDecommission,
          realComponentName: componentNameForOtherActions
        })
      }));
      menuItems.pushObject(Em.Object.create({
        label: Em.I18n.t('common.recommission'),
        operationData: Em.Object.create({
          action: 'DECOMMISSION_OFF',
          message: Em.I18n.t('common.recommission'),
          componentName: componentNameForDecommission,
          realComponentName: componentNameForOtherActions
        })
      }));
    }
    return menuItems;
  },

  /**
   * Get third-level menu items for Hosts
   * operationData format:
   * <code>
   *  {
   *    action: 'STARTED|INSTALLED|RESTART..', // action for selected hosts (will be applied for each host component in selected hosts)
   *    actionToCheck: 'INSTALLED|STARTED..' // state to filter host components should be processed
   *    message: 'some text', // just text to BG popup
   *  }
   *  </code>
   * @returns {Array}
   */
  getHostItemsTemplate: function() {
    return Em.A([
      Em.Object.create({
        label: Em.I18n.t('hosts.host.details.startAllComponents'),
        operationData: Em.Object.create({
          action: 'STARTED',
          actionToCheck: 'INSTALLED',
          message: Em.I18n.t('hosts.host.details.startAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('hosts.host.details.stopAllComponents'),
        operationData: Em.Object.create({
          action: 'INSTALLED',
          actionToCheck: 'STARTED',
          message: Em.I18n.t('hosts.host.details.stopAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('hosts.table.menu.l2.restartAllComponents'),
        operationData: Em.Object.create({
          action: 'RESTART',
          message: Em.I18n.t('hosts.table.menu.l2.restartAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('passiveState.turnOn'),
        operationData: Em.Object.create({
          state: 'ON',
          action: 'PASSIVE_STATE',
          message: Em.I18n.t('passiveState.turnOnFor').format('hosts')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('passiveState.turnOff'),
        operationData: Em.Object.create({
          state: 'OFF',
          action: 'PASSIVE_STATE',
          message: Em.I18n.t('passiveState.turnOffFor').format('hosts')
        })
      })
    ]);
  },

  /**
   * Get second-level menu
   * @param {String} selection
   * <code>
   *   "s" - selected hosts
   *   "a" - all hosts
   *   "f" - filtered hosts
   * </code>
   * @returns {Array}
   */
  getSubMenuItemsTemplate: function(selection) {
    var submenu = Em.A([{label: Em.I18n.t('common.hosts'), submenu: this.getHostItemsTemplate()}]);

    if (!!App.HDFSService.find().content.length) {
      var slaveItemsForHdfs = this.getSlaveItemsTemplate('NAMENODE', 'DATANODE');
      slaveItemsForHdfs.setEach('operationData.serviceName', 'HDFS');
      slaveItemsForHdfs.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.hdfs.datanodes'));
      submenu.push({label: Em.I18n.t('dashboard.services.hdfs.datanodes'), submenu: slaveItemsForHdfs});
    }

    if (!!App.YARNService.find().content.length) {
      var slaveItemsForYarn = this.getSlaveItemsTemplate('RESOURCEMANAGER', 'NODEMANAGER');
      slaveItemsForYarn.setEach('operationData.serviceName', 'YARN');
      slaveItemsForYarn.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.yarn.nodeManagers'));
      submenu.push({label: Em.I18n.t('dashboard.services.yarn.nodeManagers'), submenu: slaveItemsForYarn});
    }

    if (!!App.HBaseService.find().content.length) {
      var slaveItemsForHBase = this.getSlaveItemsTemplate('HBASE_MASTER', 'HBASE_REGIONSERVER');
      slaveItemsForHBase.setEach('operationData.serviceName', 'HBASE');
      slaveItemsForHBase.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.hbase.regionServers'));
      submenu.push({label: Em.I18n.t('dashboard.services.hbase.regionServers'), submenu: slaveItemsForHBase});
    }

    if (!!App.MapReduceService.find().content.length) {
      var slaveItemsForMapReduce = this.getSlaveItemsTemplate('JOBTRACKER', 'TASKTRACKER');
      slaveItemsForMapReduce.setEach('operationData.serviceName', 'MAPREDUCE');
      slaveItemsForMapReduce.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.mapreduce.taskTrackers'));
      submenu.push({label: Em.I18n.t('dashboard.services.mapreduce.taskTrackers'), submenu: slaveItemsForMapReduce});
    }

    if (!!App.Service.find().filterProperty('serviceName', 'STORM').length) {
      var slaveItemsForStorm = this.getSlaveItemsTemplate('SUPERVISOR', 'SUPERVISOR');
      slaveItemsForStorm.setEach('operationData.serviceName', 'STORM');
      slaveItemsForStorm.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.storm.supervisors'));
      submenu.push({label: Em.I18n.t('dashboard.services.storm.supervisors'), submenu: slaveItemsForStorm});
    }

    submenu.forEach(function(item) {
      item.submenu.forEach(function(subitem) {
        subitem.operationData.selection = selection;
      });
    });
    return submenu;
  },

  /**
   * Menu-items for Hosts table
   * @type {Object}
   */
  menuItems: function() {
    return {
      s: {label: Em.I18n.t('hosts.table.menu.l1.selectedHosts'), submenu: this.getSubMenuItemsTemplate('s')},
      f: {label: Em.I18n.t('hosts.table.menu.l1.filteredHosts'), submenu: this.getSubMenuItemsTemplate('f')},
      a: {label: Em.I18n.t('hosts.table.menu.l1.allHosts'), submenu: this.getSubMenuItemsTemplate('a')}
    };
  }.property('App.router.clusterController.isLoaded')
});
