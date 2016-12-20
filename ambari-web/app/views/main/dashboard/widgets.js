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
var filters = require('views/common/filter_view');

App.MainDashboardWidgetsView = Em.View.extend(App.UserPref, App.LocalStorage, App.TimeRangeMixin, {
  name: 'mainDashboardWidgetsView',
  templateName: require('templates/main/dashboard/widgets'),

  widgetsDefinition: [
    {
      id: 1,
      viewName: 'NameNodeHeapPieChartView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.NameNodeHeap'),
      threshold: [80, 90]
    },
    {
      id: 2,
      viewName: 'NameNodeCapacityPieChartView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.HDFSDiskUsage'),
      threshold: [85, 95]
    },
    {
      id: 3,
      viewName: 'NameNodeCpuPieChartView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.NameNodeCpu'),
      threshold: [90, 95]
    },
    {
      id: 4,
      viewName: 'DataNodeUpView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.DataNodeUp'),
      threshold: [80, 90]
    },
    {
      id: 5,
      viewName: 'NameNodeRpcView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.NameNodeRpc'),
      threshold: [1000, 3000]
    },
    {
      id: 6,
      viewName: 'ChartClusterMetricsMemoryWidgetView',
      sourceName: 'HOST_METRICS',
      title: Em.I18n.t('dashboard.clusterMetrics.memory'),
      threshold: []
    },
    {
      id: 7,
      viewName: 'ChartClusterMetricsNetworkWidgetView',
      sourceName: 'HOST_METRICS',
      title: Em.I18n.t('dashboard.clusterMetrics.network'),
      threshold: []
    },
    {
      id: 8,
      viewName: 'ChartClusterMetricsCPUWidgetView',
      sourceName: 'HOST_METRICS',
      title: Em.I18n.t('dashboard.clusterMetrics.cpu'),
      threshold: []
    },
    {
      id: 9,
      viewName: 'ChartClusterMetricsLoadWidgetView',
      sourceName: 'HOST_METRICS',
      title: Em.I18n.t('dashboard.clusterMetrics.load'),
      threshold: []
    },
    {
      id: 10,
      viewName: 'NameNodeUptimeView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.NameNodeUptime'),
      threshold: []
    },
    {
      id: 11,
      viewName: 'HDFSLinksView',
      sourceName: 'HDFS',
      title: Em.I18n.t('dashboard.widgets.HDFSLinks'),
      threshold: []
    },
    {
      id: 12,
      viewName: 'HBaseLinksView',
      sourceName: 'HBASE',
      title: Em.I18n.t('dashboard.widgets.HBaseLinks'),
      threshold: []
    },
    {
      id: 13,
      viewName: 'HBaseMasterHeapPieChartView',
      sourceName: 'HBASE',
      title: Em.I18n.t('dashboard.widgets.HBaseMasterHeap'),
      threshold: [70, 90]
    },
    {
      id: 14,
      viewName: 'HBaseAverageLoadView',
      sourceName: 'HBASE',
      title: Em.I18n.t('dashboard.widgets.HBaseAverageLoad'),
      threshold: [150, 250]
    },
    {
      id: 15,
      viewName: 'HBaseRegionsInTransitionView',
      sourceName: 'HBASE',
      title: Em.I18n.t('dashboard.widgets.HBaseRegionsInTransition'),
      threshold: [3, 10],
      isHiddenByDefault: true
    },
    {
      id: 16,
      viewName: 'HBaseMasterUptimeView',
      sourceName: 'HBASE',
      title: Em.I18n.t('dashboard.widgets.HBaseMasterUptime'),
      threshold: []
    },
    {
      id: 17,
      viewName: 'ResourceManagerHeapPieChartView',
      sourceName: 'YARN',
      title: Em.I18n.t('dashboard.widgets.ResourceManagerHeap'),
      threshold: [70, 90]
    },
    {
      id: 18,
      viewName: 'ResourceManagerUptimeView',
      sourceName: 'YARN',
      title: Em.I18n.t('dashboard.widgets.ResourceManagerUptime'),
      threshold: []
    },
    {
      id: 19,
      viewName: 'NodeManagersLiveView',
      sourceName: 'YARN',
      title: Em.I18n.t('dashboard.widgets.NodeManagersLive'),
      threshold: [50, 75]
    },
    {
      id: 20,
      viewName: 'YARNMemoryPieChartView',
      sourceName: 'YARN',
      title: Em.I18n.t('dashboard.widgets.YARNMemory'),
      threshold: [50, 75]
    },
    {
      id: 21,
      viewName: 'SuperVisorUpView',
      sourceName: 'STORM',
      title: Em.I18n.t('dashboard.widgets.SuperVisorUp'),
      threshold: [85, 95]
    },
    {
      id: 22,
      viewName: 'FlumeAgentUpView',
      sourceName: 'FLUME',
      title: Em.I18n.t('dashboard.widgets.FlumeAgentUp'),
      threshold: [85, 95]
    },
    {
      id: 23,
      viewName: 'YARNLinksView',
      sourceName: 'YARN',
      title: Em.I18n.t('dashboard.widgets.YARNLinks'),
      threshold: []
    },
    {
      id: 24,
      viewName: 'HawqSegmentUpView',
      sourceName: 'HAWQ',
      title: Em.I18n.t('dashboard.widgets.HawqSegmentUp'),
      threshold: [75, 90]
    },
    {
      id: 25,
      viewName: 'PxfUpView',
      sourceName: 'PXF',
      title: Em.I18n.t('dashboard.widgets.PxfUp'),
      threshold: []
    }
  ],

  /**
   * List of services
   * @type {Ember.Enumerable}
   */
  content: [],

  /**
   * Key-name to store data in Local Storage and Persist
   * @type {string}
   */
  persistKey: Em.computed.format('user-pref-{0}-dashboard', 'App.router.loginName'),

  /**
   * @type {boolean}
   */
  isDataLoaded: false,

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  /**
   * List of visible widgets
   * @type {Ember.Enumerable}
   */
  visibleWidgets: [],

  /**
   * List of hidden widgets
   * @type {Ember.Enumerable}
   */
  hiddenWidgets: [], // widget child view will push object in this array if deleted

  timeRangeClassName: 'pull-left',

  /**
   * Example:
   * {
   *   visible: [1, 2, 4],
   *   hidden: [3, 5],
   *   threshold: {
   *     1: [80, 90],
   *     2: [],
   *     3: [1, 2]
   *   }
   * }
   * @type {Object|null}
   */
  userPreferences: null,

  didInsertElement: function () {
    var self = this;

    this._super();
    this.loadWidgetsSettings().complete(function() {
      self.checkServicesChange();
      self.renderWidgets();
      self.set('isDataLoaded', true);
      App.loadTimer.finish('Dashboard Metrics Page');
      Em.run.next(self, 'makeSortable');
    });
  },

  /**
   * Set visibility-status for widgets
   */
  loadWidgetsSettings: function () {
    return this.getUserPref(this.get('persistKey'));
  },

  /**
   * make POST call to save settings
   * @param {object} settings
   */
  saveWidgetsSettings: function (settings) {
    this.set('userPreferences', settings);
    this.setDBProperty(this.get('persistKey'), settings);
    this.postUserPref(this.get('persistKey'), settings);
  },

  getUserPrefSuccessCallback: function (response) {
    if (response) {
      this.set('userPreferences', response);
    } else {
      this.getUserPrefErrorCallback();
    }
  },

  getUserPrefErrorCallback: function () {
    var userPreferences = this.generateDefaultUserPreferences();
    this.saveWidgetsSettings(userPreferences);
  },

  resolveConfigDependencies: function(widgetsDefinition) {
    var clusterEnv = App.router.get('clusterController.clusterEnv').properties;
    var yarnMemoryWidget = widgetsDefinition.findProperty('id', 20);

    if (clusterEnv['hide_yarn_memory_widget'] === 'true') {
      yarnMemoryWidget.isHiddenByDefault = true;
    }
  },

  generateDefaultUserPreferences: function() {
    var widgetsDefinition = this.get('widgetsDefinition');
    var preferences = {
      visible: [],
      hidden: [],
      threshold: {}
    };

    this.resolveConfigDependencies(widgetsDefinition);

    widgetsDefinition.forEach(function(widget) {
      if (App.Service.find(widget.sourceName).get('isLoaded') || widget.sourceName === 'HOST_METRICS') {
        if (widget.isHiddenByDefault) {
          preferences.hidden.push(widget.id);
        } else {
          preferences.visible.push(widget.id);
        }
      }
      preferences.threshold[widget.id] = widget.threshold;
    });

    return preferences;
  },

  /**
   * set widgets to view in order to render
   */
  renderWidgets: function () {
    var widgetsDefinitionMap = this.get('widgetsDefinition').toMapByProperty('id');
    var userPreferences = this.get('userPreferences');
    var visibleWidgets = [];
    var hiddenWidgets = [];

    userPreferences.visible.forEach(function(id) {
      var widget = widgetsDefinitionMap[id];
      visibleWidgets.push(Em.Object.create({
        id: id,
        threshold: userPreferences.threshold[id],
        viewClass: App[widget.viewName],
        sourceName: widget.sourceName,
        title: widget.title
      }));
    });

    userPreferences.hidden.forEach(function(id) {
      var widget = widgetsDefinitionMap[id];
      hiddenWidgets.push(Em.Object.create({
        id: id,
        title: widget.title,
        checked: false
      }));
    });

    this.set('visibleWidgets', visibleWidgets);
    this.set('hiddenWidgets', hiddenWidgets);
  },

  /**
   * check if stack has upgraded from HDP 1.0 to 2.0 OR add/delete services.
   * Update the value on server if true.
   */
  checkServicesChange: function () {
    var userPreferences = this.get('userPreferences');
    var defaultPreferences = this.generateDefaultUserPreferences();
    var newValue = {
      visible: userPreferences.visible.slice(0),
      hidden: userPreferences.hidden.slice(0),
      threshold: userPreferences.threshold
    };
    var isChanged = false;

    defaultPreferences.visible.forEach(function(id) {
      if (!userPreferences.visible.contains(id) && !userPreferences.hidden.contains(id)) {
        isChanged = true;
        newValue.visible.push(id);
      }
    });

    defaultPreferences.hidden.forEach(function(id) {
      if (!userPreferences.visible.contains(id) && !userPreferences.hidden.contains(id)) {
        isChanged = true;
        newValue.hidden.push(id);
      }
    });
    if (isChanged) {
      this.saveWidgetsSettings(newValue);
    }
  },

  /**
   * Reset widgets visibility-status
   */
  resetAllWidgets: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      self.saveWidgetsSettings(self.generateDefaultUserPreferences());
      self.setProperties({
        currentTimeRangeIndex: 0,
        customStartTime: null,
        customEndTime: null
      });
      self.renderWidgets();
    });
  },

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    return $("#sortable").sortable({
      items: "> div",
      cursor: "move",
      tolerance: "pointer",
      scroll: false,
      update: function () {
        var widgetsArray = $('div[viewid]');

        var userPreferences = self.get('userPreferences') || self.getDBProperty(self.get('persistKey'));
        var newValue = Em.Object.create({
          visible: [],
          hidden: userPreferences.hidden,
          threshold: userPreferences.threshold
        });
        var size = userPreferences.visible.length;
        for (var j = 0; j <= size - 1; j++) {
          var viewID = widgetsArray.get(j).getAttribute('viewid');
          var id = Number(viewID.split("-").get(1));
          newValue.visible.push(id);
        }
        self.saveWidgetsSettings(newValue);
      },
      activate: function (event, ui) {
        self.set('isMoving', true);
      },
      deactivate: function (event, ui) {
        self.set('isMoving', false);
      }
    }).disableSelection();
  },

  /**
   * Submenu view for New Dashboard style
   * @type {Ember.View}
   * @class
   */
  plusButtonFilterView: Ember.View.extend({
    tagName: 'ul',
    classNames: ['dropdown-menu'],
    templateName: require('templates/main/dashboard/plus_button_filter'),
    hiddenWidgetsBinding: 'parentView.hiddenWidgets',
    valueBinding: '',
    widgetCheckbox: App.CheckboxView.extend({
      didInsertElement: function () {
        $('.checkbox').click(function (event) {
          event.stopPropagation();
        });
      }
    }),
    applyFilter: function () {
      var parent = this.get('parentView'),
        hiddenWidgets = this.get('hiddenWidgets'),
        userPreferences = parent.get('userPreferences'),
        newValue = {
          visible: userPreferences.visible.slice(0),
          hidden: userPreferences.hidden.slice(0),
          threshold: userPreferences.threshold
        };

      hiddenWidgets.filterProperty('checked').forEach(function (item) {
        newValue.visible.push(item.id);
        newValue.hidden = newValue.hidden.without(item.id);
        hiddenWidgets.removeObject(item);
      }, this);
      parent.saveWidgetsSettings(newValue);
      parent.renderWidgets();
    }
  }),

  showAlertsPopup: Em.K

});
