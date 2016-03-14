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

  menuItems: function () {
    return {
      s: {label: Em.I18n.t('hosts.table.menu.l1.selectedHosts')},
      f: {label: Em.I18n.t('hosts.table.menu.l1.filteredHosts')},
      a: {label: Em.I18n.t('hosts.table.menu.l1.allHosts')}
    };
  }.property('App.router.clusterController.isLoaded'),

  components: function(){
    var menuItems = [
    Em.Object.create({
      serviceName: 'HDFS',
      componentName: 'DATANODE',
      masterComponentName: 'NAMENODE',
      componentNameFormatted: Em.I18n.t('dashboard.services.hdfs.datanodes')
    }),
    Em.Object.create({
      serviceName: 'YARN',
      componentName: 'NODEMANAGER',
      masterComponentName: 'RESOURCEMANAGER',
      componentNameFormatted: Em.I18n.t('dashboard.services.yarn.nodeManagers')
    }),
    Em.Object.create({
      serviceName: 'HAWQ',
      componentName: 'HAWQSEGMENT',
      componentNameFormatted: Em.I18n.t('dashboard.services.hawq.hawqSegments')
    }),
     Em.Object.create({
      serviceName: 'PXF',
      componentName: 'PXF',
      componentNameFormatted: Em.I18n.t('dashboard.services.pxf.pxfHosts')
    }),
    Em.Object.create({
      serviceName: 'HBASE',
      componentName: 'HBASE_REGIONSERVER',
      masterComponentName: 'HBASE_MASTER',
      componentNameFormatted: Em.I18n.t('dashboard.services.hbase.regionServers')
    }),
    Em.Object.create({
      serviceName: 'STORM',
      componentName: 'SUPERVISOR',
      masterComponentName: 'SUPERVISOR',
      componentNameFormatted: Em.I18n.t('dashboard.services.storm.supervisors')
    })];

    return menuItems.filter(function(item){
      return App.Service.find().findProperty('serviceName',item.serviceName);
    });
  }.property(),


  /**
   * slaveItemView build second-level menu
   * for slave component
   */

  slaveItemView: Em.View.extend({

    tagName: 'li',

    classNames: ['dropdown-submenu'],

    /**
     * Get third-level menu items ingo for slave components
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

    operationsInfo: function () {
      var content = this.get('content');
      var menuItems = Em.A([
        Em.Object.create({
          label: Em.I18n.t('common.start'),
          operationData: Em.Object.create({
            action: App.HostComponentStatus.started,
            message: Em.I18n.t('common.start'),
            componentName: content.componentName,
            serviceName: content.serviceName,
            componentNameFormatted: content.componentNameFormatted
          })
        }),
        Em.Object.create({
          label: Em.I18n.t('common.stop'),
          operationData: Em.Object.create({
            action: App.HostComponentStatus.stopped,
            message: Em.I18n.t('common.stop'),
            componentName: content.componentName,
            serviceName: content.serviceName,
            componentNameFormatted: content.componentNameFormatted
          })
        }),
        Em.Object.create({
          label: Em.I18n.t('common.restart'),
          operationData: Em.Object.create({
            action: 'RESTART',
            message: Em.I18n.t('common.restart'),
            componentName: content.componentName,
            serviceName: content.serviceName,
            componentNameFormatted: content.componentNameFormatted
          })
        })
      ]);
      if (App.get('components.decommissionAllowed').contains(content.componentName)) {
        menuItems.pushObject(Em.Object.create({
          label: Em.I18n.t('common.decommission'),
          decommission: true,
          operationData: Em.Object.create({
            action: 'DECOMMISSION',
            message: Em.I18n.t('common.decommission'),
            componentName: content.masterComponentName,
            realComponentName: content.componentName,
            serviceName: content.serviceName,
            componentNameFormatted: content.componentNameFormatted
          })
        }));
        menuItems.pushObject(Em.Object.create({
          label: Em.I18n.t('common.recommission'),
          decommission: true,
          operationData: Em.Object.create({
            action: 'DECOMMISSION_OFF',
            message: Em.I18n.t('common.recommission'),
            componentName: content.masterComponentName,
            realComponentName: content.componentName,
            serviceName: content.serviceName,
            componentNameFormatted: content.componentNameFormatted
          })
        }));
      }
      return menuItems;
    }.property("content"),

    /**
     * commonOperationView is used for third-level menu items
     * for simple operations ('START','STOP','RESTART')
     */
    commonOperationView: Em.View.extend({
      tagName: 'li',

      /**
       * click function use
       * App.MainHostView as a thirl level parent
       * and runs it's function
       */
      click: function () {
        this.get('parentView.parentView.parentView').bulkOperationConfirm(this.get('content'), this.get('selection'));
      }
    }),

    /**
     * advancedOperationView is used for third level menu item
     * for advanced operations ('RECOMMISSION','DECOMMISSION')
     */
    advancedOperationView: Em.View.extend({
      tagName: 'li',
      rel: 'menuTooltip',
      classNameBindings: ['disabledElement'],
      attributeBindings: ['tooltipMsg:data-original-title'],

      service: function () {
        return App.router.get('mainServiceController.content').findProperty('serviceName', this.get('content.serviceName'))
      }.property('App.router.mainServiceController.content.@each', 'content'),

      tooltipMsg: function () {
        return (this.get('disabledElement') == 'disabled') ?
           Em.I18n.t('hosts.decommission.tooltip.warning').format(this.get('content.message'),  App.format.role(this.get('content.componentName'), false)) : '';
      }.property('disabledElement','content.componentName'),

      disabledElement: function () {
        return (this.get('service.workStatus') != 'STARTED') ? 'disabled' : '';
      }.property('service.workStatus'),

      /**
       * click function use
       * App.MainHostView as a thirl level parent
       * and runs it's function
       */
      click: function () {
        if (this.get('disabledElement') == 'disabled') {
          return;
        }
        this.get('parentView.parentView.parentView').bulkOperationConfirm(this.get('content'), this.get('selection'));
      },

      didInsertElement: function () {
        App.tooltip($(this.get('element')));
      }
    })
  }),

  /**
   * hostItemView build second-level menu
   * for host
   */

  hostItemView: Em.View.extend({

    tagName: 'li',

    classNames: ['dropdown-submenu'],

    label: Em.I18n.t('common.hosts'),

    /** Get third-level menu items for Hosts
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
    operationsInfo: function () {
      return [
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
          label: Em.I18n.t('hosts.table.menu.l2.reinstallFailedComponents'),
          operationData: Em.Object.create({
            action: 'REINSTALL',
            message: Em.I18n.t('hosts.table.menu.l2.reinstallFailedComponents')
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
        }),
        Em.Object.create({
          label: Em.I18n.t('hosts.host.details.setRackId'),
          operationData: Em.Object.create({
            action: 'SET_RACK_INFO',
            message: Em.I18n.t('hosts.host.details.setRackId').format('hosts')
          })
        })
      ];
    }.property(),

    /**
     * commonOperationView is used for third-level menu items
     * for all operations for host
     */
    operationView: Em.View.extend({
      tagName: 'li',

      /**
       * click function use
       * App.MainHostView as a thirl level parent
       * and runs it's function
       */
      click: function () {
        this.get('parentView.parentView.parentView').bulkOperationConfirm(this.get('content'), this.get('selection'));
      }
    })
  })
});
