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

App.HostTableMenuView = Em.View.extend({

  templateName: require('templates/main/host/bulk_operation_menu'),

  /**
   * Get third-level menu items for slave components (but not for DataNode!)
   * @returns {Array}
   */
  getSlaveItemsTemplate: function() {
    return Em.A([
      Em.Object.create({
        label: Em.I18n.t('common.start'),
        operationData: Em.Object.create({
          action: 'start',
          message: Em.I18n.t('common.start')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('common.stop'),
        operationData: Em.Object.create({
          action: 'stop',
          message: Em.I18n.t('common.stop')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('common.restart'),
        operationData: Em.Object.create({
          action: 'restart',
          message: Em.I18n.t('common.restart')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('maintenance.turnOn'),
        operationData: Em.Object.create({
          action: 'turn_on_maintenance',
          message: Em.I18n.t('maintenance.turnOnFor')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('maintenance.turnOff'),
        operationData: Em.Object.create({
          action: 'turn_off_maintenance',
          message: Em.I18n.t('maintenance.turnOffFor')
        })
      })
    ]);
  },

  /**
   * Get third-level menu items for DataNode
   * @returns {Array}
   */
  getDataNodeItemsTemplate: function() {
    var dataNodesItems = this.getSlaveItemsTemplate();
    dataNodesItems.push(Em.Object.create({
      label: Em.I18n.t('common.decommission'),
      operationData: Em.Object.create({
        action: 'decommission',
        message: Em.I18n.t('common.decommission')
      })
    }));
    dataNodesItems.push(Em.Object.create({
      label: Em.I18n.t('common.recommission'),
      operationData: Em.Object.create({
        action: 'recommission',
        message: Em.I18n.t('common.recommission')
      })
    }));
    dataNodesItems.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.hdfs.datanodes'));
    dataNodesItems.setEach('operationData.componentName', 'DATANODE');
    return dataNodesItems;
  },

  /**
   * Get third-level menu items for Hosts
   * @returns {Array}
   */
  getHostItemsTemplate: function() {
    return Em.A([
      Em.Object.create({
        label: Em.I18n.t('hosts.host.details.startAllComponents'),
        operationData: Em.Object.create({
          action: 'start_all',
          message: Em.I18n.t('hosts.host.details.startAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('hosts.host.details.stopAllComponents'),
        operationData: Em.Object.create({
          action: 'stop_all',
          message: Em.I18n.t('hosts.host.details.stopAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('hosts.table.menu.l2.restartAllComponents'),
        operationData: Em.Object.create({
          action: 'restart_all',
          message: Em.I18n.t('hosts.table.menu.l2.restartAllComponents')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('maintenance.turnOn'),
        operationData: Em.Object.create({
          action: 'turn_on_maintenance',
          message: Em.I18n.t('maintenance.turnOn')
        })
      }),
      Em.Object.create({
        label: Em.I18n.t('maintenance.turnOff'),
        operationData: Em.Object.create({
          action: 'turn_off_maintenance',
          message: Em.I18n.t('maintenance.turnOff')
        })
      })
    ]);
  },

  /**
   * Get second-level menu
   * @param {String} selection
   * @returns {Array}
   */
  getSubMenuItemsTemplate: function(selection) {
    var submenu = [{label: Em.I18n.t('common.hosts'), submenu: this.getHostItemsTemplate()}];

    if (!!App.HDFSService.find().content.length) {
      submenu.push({label: Em.I18n.t('dashboard.services.hdfs.datanodes'), submenu: this.getDataNodeItemsTemplate()});
    }

    if (!!App.YARNService.find().content.length) {
      var slaveItemsForYarn = this.getSlaveItemsTemplate();
      slaveItemsForYarn.setEach('operationData.componentName', 'NODEMANAGER');
      slaveItemsForYarn.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.yarn.nodeManagers'));
      submenu.push({label: Em.I18n.t('dashboard.services.yarn.nodeManagers'), submenu: slaveItemsForYarn});
    }

    if (!!App.HBaseService.find().content.length) {
      var slaveItemsForHBase = this.getSlaveItemsTemplate();
      slaveItemsForHBase.setEach('operationData.componentName', 'HBASE_REGIONSERVER');
      slaveItemsForHBase.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.hbase.regionServers'));
      submenu.push({label: Em.I18n.t('dashboard.services.hbase.regionServers'), submenu: slaveItemsForHBase});
    }

    if (!!App.MapReduceService.find().content.length) {
      var slaveItemsForMapReduce = this.getSlaveItemsTemplate();
      slaveItemsForMapReduce.setEach('operationData.componentName', 'TASKTRACKER');
      slaveItemsForMapReduce.setEach('operationData.componentNameFormatted', Em.I18n.t('dashboard.services.mapreduce.taskTrackers'));
      submenu.push({label: Em.I18n.t('dashboard.services.mapreduce.taskTrackers'), submenu: slaveItemsForMapReduce});
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
   * {Object}
   */
  menuItems: function() {
    return {
      s: {label: Em.I18n.t('hosts.table.menu.l1.selectedHosts'), submenu: this.getSubMenuItemsTemplate('s')},
      f: {label: Em.I18n.t('hosts.table.menu.l1.filteredHosts'), submenu: this.getSubMenuItemsTemplate('f')},
      a: {label: Em.I18n.t('hosts.table.menu.l1.allHosts'), submenu: this.getSubMenuItemsTemplate('a')}
    };
  }.property('App.router.clusterController.isLoaded')
});