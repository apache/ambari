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

App.MainHostSummaryView = Em.View.extend(App.TimeRangeMixin, {

  templateName: require('templates/main/host/summary'),

  /**
   * List of custom view for some host components
   * @type {Em.Object}
   * Format:
   *  <code>
   *    {
   *      COMPONENT_NAME1: VIEW1,
   *      COMPONENT_NAME2: VIEW2
   *      ....
   *    }
   *  </code>
   */
  hostComponentViewMap: Em.Object.create({
    'DATANODE': App.DataNodeComponentView,
    'NODEMANAGER': App.NodeManagerComponentView,
    'HBASE_REGIONSERVER': App.RegionServerComponentView,
    'TASKTRACKER': App.TaskTrackerComponentView
  }),

  /**
   * @type {bool}
   */
  isStopCommand: true,

  /**
   * @type {App.Host}
   */
  content: Em.computed.alias('App.router.mainHostDetailsController.content'),

  /**
   * Host metrics panel not displayed when Metrics service (ex:Ganglia) is not in stack definition.
   */
  isNoHostMetricsService: Em.computed.equal('App.services.hostMetrics.length', 0),

  /**
   * Message for "restart" block
   * @type {String}
   */
  needToRestartMessage: function() {
    var componentsCount, word;
    componentsCount = this.get('content.componentsWithStaleConfigsCount');
    if (componentsCount > 1) {
      word = Em.I18n.t('common.components').toLowerCase();
    } else {
      word = Em.I18n.t('common.component').toLowerCase();
    }
    return Em.I18n.t('hosts.host.details.needToRestart').format(this.get('content.componentsWithStaleConfigsCount'), word);
  }.property('content.componentsWithStaleConfigsCount'),

  /**
   * Reset <code>sortedComponents</code>
   * Used when some component was deleted from host
   */
  redrawComponents: function() {
    if (App.router.get('mainHostDetailsController.redrawComponents')) {
      this.set('sortedComponents', []);
      this.sortedComponentsFormatter();
      App.router.set('mainHostDetailsController.redrawComponents', false);
    }
  }.observes('App.router.mainHostDetailsController.redrawComponents'),

  willInsertElement: function() {
    this.set('sortedComponents', []);
    this.sortedComponentsFormatter();
    this.addObserver('content.hostComponents.length', this, 'sortedComponentsFormatter');
  },

  didInsertElement: function () {
    this._super();
    this.addToolTip();
  },

  /**
   * Create tooltip for "Add" button if nothing to add to the current host
   */
  addToolTip: function() {
    if (this.get('addComponentDisabled')) {
      App.tooltip($('#add_component'), {title: Em.I18n.t('services.nothingToAdd')});
    }
  }.observes('addComponentDisabled'),

  /**
   * List of installed services
   * @type {String[]}
   */
  installedServices: function () {
    return App.Service.find().mapProperty('serviceName');
  }.property('App.router.clusterController.dataLoadList.services'),

  /**
   * List of installed masters and slaves
   * Masters first, then slaves
   * @type {App.HostComponent[]}
   */
  sortedComponents: [],

  /**
   * Update <code>sortedComponents</code>
   * Master components first, then slaves and clients
   */
  sortedComponentsFormatter: function() {
    const updatebleProperties = Em.A(['workStatus', 'passiveState', 'staleConfigs', 'haStatus']);
    const hostComponentViewMap = this.get('hostComponentViewMap');
    const masters = [], slaves = [], clients = [];
    // Remove deleted components
    this.get('sortedComponents').forEach((sortedComponent, index) => {
      if (!this.get('content.hostComponents').findProperty('id', sortedComponent.get('id'))) {
        this.get('sortedComponents').removeAt(index, 1);
      }
    });

    this.get('content.hostComponents').forEach(function (component) {
      const obj = this.get('sortedComponents').findProperty('id', component.get('id'));
      if (obj) {
        // Update existing component
        updatebleProperties.forEach(function (property) {
          obj.set(property, component.get(property));
        });
      } else {
        component.set('viewClass', hostComponentViewMap[component.get('componentName')] ? hostComponentViewMap[component.get('componentName')] : App.HostComponentView);
        if (component.get('isMaster')) {
          masters.push(component);
        } else if (component.get('isSlave')) {
          slaves.push(component);
        } else if (component.get('isClient')) {
          component.set('isLast', true);
          component.set('isInstallFailed', ['INSTALL_FAILED', 'INIT'].contains(component.get('workStatus')));
          clients.pushObject(component);
        }
      }
    }, this);
    this.set('sortedComponents', masters.concat(slaves, clients));
  },

  /**
   * Template for addable component
   * @type {Em.Object}
   */
  addableComponentObject: Em.Object.extend({
    componentName: '',
    displayName: function () {
      return App.format.role(this.get('componentName'), false);
    }.property('componentName')
  }),

  /**
   * If host lost heartbeat, components can't be added on it
   * @type {bool}
   */
  isAddComponent: Em.computed.notEqual('content.healthClass', 'health-status-DEAD-YELLOW'),

  /**
   * Disable "Add" button if components can't be added to the current host
   * @type {bool}
   */
  addComponentDisabled: Em.computed.or('!isAddComponent', '!addableComponents.length'),

  /**
   * List of components that may be added to the current host
   * @type {Em.Object[]}
   */
  addableComponents: function () {
    var components = [];
    var self = this;
    if (this.get('content.hostComponents')) {
      var installedComponents = this.get('content.hostComponents').mapProperty('componentName');
      var addableToHostComponents = App.StackServiceComponent.find().filterProperty('isAddableToHost');
      var installedServices = this.get('installedServices');

      addableToHostComponents.forEach(function (addableComponent) {
        if (installedServices.contains(addableComponent.get('serviceName'))
            && !installedComponents.contains(addableComponent.get('componentName'))
            && !this.hasCardinalityConflict(addableComponent.get('componentName'))) {
          if ((addableComponent.get('componentName') === 'OOZIE_SERVER') && !App.router.get('mainHostDetailsController.isOozieServerAddable')) {
            return;
          }
          components.pushObject(self.addableComponentObject.create({
            'componentName': addableComponent.get('componentName'),
            'serviceName': addableComponent.get('serviceName')
          }));
        }
      }, this);
    }
    return components;
  }.property('content.hostComponents.length', 'App.components.addableToHost.@each'),

  /**
   *
   * @param {string} componentName
   * @returns {boolean}
   */
  hasCardinalityConflict: function(componentName) {
    var totalCount = App.SlaveComponent.find(componentName).get('totalCount');
    var maxToInstall = App.StackServiceComponent.find(componentName).get('maxToInstall');
    return !(totalCount < maxToInstall);
  },

  /**
   * Formatted with <code>$.timeago</code> value of host's last heartbeat
   * @type {String}
   */
  timeSinceHeartBeat: function () {
    var d = this.get('content.rawLastHeartBeatTime');
    return d ? $.timeago(d) : '';
  }.property('content.rawLastHeartBeatTime')
});
