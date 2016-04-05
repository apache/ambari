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

  didInsertElement: function () {
    this._super();
    this.setWidgetsDataModel();
    this.setInitPrefObject();
    this.setOnLoadVisibleWidgets();
    this.set('isDataLoaded', true);
    App.loadTimer.finish('Dashboard Metrics Page');
    Em.run.next(this, 'makeSortable');
  },

  /**
   * List of services
   * @type {Ember.Enumerable}
   */
  content: [],

  /**
   * @type {boolean}
   */
  isDataLoaded: false,

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  timeRangeClassName: 'pull-left',

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    $("#sortable").sortable({
      items: "> div",
      //placeholder: "sortable-placeholder",
      cursor: "move",
      tolerance: "pointer",
      scroll: false,
      update: function (event, ui) {
        if (!App.get('testMode')) {
          // update persist then translate to real
          var widgetsArray = $('div[viewid]'); // get all in DOM
          self.getUserPref(self.get('persistKey')).complete(function () {
            var oldValue = self.get('currentPrefObject') || self.getDBProperty(self.get('persistKey'));
            var newValue = Em.Object.create({
              dashboardVersion: oldValue.dashboardVersion,
              visible: [],
              hidden: oldValue.hidden,
              threshold: oldValue.threshold
            });
            var size = oldValue.visible.length;
            for (var j = 0; j <= size - 1; j++) {
              var viewID = widgetsArray.get(j).getAttribute('viewid');
              var id = viewID.split("-").get(1);
              newValue.visible.push(id);
            }
            self.postUserPref(self.get('persistKey'), newValue);
            self.setDBProperty(self.get('persistKey'), newValue);
            //self.translateToReal(newValue);
          });
        }
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
   * Set Service model values
   */
  setWidgetsDataModel: function () {
    if (App.get('services.hostMetrics').length > 0) {
      this.set('host_metrics_model', App.get('services.hostMetrics'));
    }
    App.Service.find().forEach(function (item) {
      var extendedModel = App.Service.extendedModel[item.get('serviceName')];
      var key = item.get('serviceName').toLowerCase() + '_model';
      if (extendedModel && App[extendedModel].find(item.get('id'))) {
        this.set(key, App[extendedModel].find(item.get('id')));
      } else {
        this.set(key, item);
      }
    }, this);
  },

  /**
   * Load widget statuses to <code>initPrefObject</code>
   */
  setInitPrefObject: function () {
    //in case of some service not installed
    var visibleFull = [
      '2', '4', '11', //hdfs
      '6', '7', '8', '9', //host metrics
      '1', '5', '3',  '10', //hdfs
      '13', '12', '14', '16', //hbase
      '17', '18', '19', '20', '23', // all yarn
      '21', // storm
      '22', // flume
      '24' // hawq
    ]; // all in order
    var hiddenFull = [
      ['15', 'Region In Transition']
    ];

    // Display widgets for host metrics if the stack definition has a host metrics service to display it.
    if (this.get('host_metrics_model') == null) {
      var hostMetrics = ['6', '7', '8', '9'];
      hostMetrics.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }

    if (this.get('hdfs_model') == null) {
      var hdfs = ['1', '2', '3', '4', '5', '10', '11'];
      hdfs.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('hbase_model') == null) {
      var hbase = ['12', '13', '14', '16'];
      hbase.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
      hiddenFull = [];
    }
    if (this.get('yarn_model') == null) {
      var yarn = ['17', '18', '19', '20', '23'];
      yarn.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('storm_model') == null) {
      var storm = ['21'];
      storm.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('flume_model') == null) {
      var flume = ['22'];
      flume.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('hawq_model') == null) {
      var hawq = ['24'];
      hawq.forEach(function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    var obj = this.get('initPrefObject');
    obj.set('visible', visibleFull);
    obj.set('hidden', hiddenFull);
  },

  host_metrics_model: null,

  hdfs_model: null,

  mapreduce2_model: null,

  yarn_model: null,

  hbase_model: null,

  storm_model: null,

  flume_model: null,

  hawq_model: null,

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

  /**
   * Submenu view for New Dashboard style
   * @type {Ember.View}
   * @class
   */
  plusButtonFilterView: Ember.View.extend({
    templateName: require('templates/main/dashboard/plus_button_filter'),
    hiddenWidgetsBinding: 'parentView.hiddenWidgets',
    visibleWidgetsBinding: 'parentView.visibleWidgets',
    valueBinding: '',
    widgetCheckbox: Em.Checkbox.extend({
      didInsertElement: function () {
        $('.checkbox').click(function (event) {
          event.stopPropagation();
        });
      }
    }),
    closeFilter: Em.K,
    applyFilter: function () {
      var self = this;
      var parent = this.get('parentView');
      var hiddenWidgets = this.get('hiddenWidgets');
      var checkedWidgets = hiddenWidgets.filterProperty('checked', true);

      if (App.get('testMode')) {
        var visibleWidgets = this.get('visibleWidgets');
        checkedWidgets.forEach(function (item) {
          var newObj = parent.widgetsMapper(item.id);
          visibleWidgets.pushObject(newObj);
          hiddenWidgets.removeObject(item);
        }, this);
      } else {
        //save in persist
        parent.getUserPref(parent.get('persistKey')).complete(function(){
          self.applyFilterComplete.apply(self);
        });
      }
    },
    applyFilterComplete: function () {
      var parent = this.get('parentView'),
        hiddenWidgets = this.get('hiddenWidgets'),
        oldValue = parent.get('currentPrefObject'),
        newValue = Em.Object.create({
          dashboardVersion: oldValue.dashboardVersion,
          visible: oldValue.visible,
          hidden: [],
          threshold: oldValue.threshold
        });
      hiddenWidgets.filterProperty('checked').forEach(function (item) {
        newValue.visible.push(item.id);
        hiddenWidgets.removeObject(item);
      }, this);
      hiddenWidgets.forEach(function (item) {
        newValue.hidden.push([item.id, item.displayName]);
      }, this);
      parent.postUserPref(parent.get('persistKey'), newValue);
      parent.translateToReal(newValue);
    }
  }),

  /**
   * Translate from Json value got from persist to real widgets view
   */
  translateToReal: function (value) {
    var version = value.dashboardVersion;
    var visible = value.visible;
    var hidden = value.hidden;
    var threshold = value.threshold;

    if (version == 'new') {
      var visibleWidgets = [];
      var hiddenWidgets = [];
      // re-construct visibleWidgets and hiddenWidgets
      for (var i = 0; i < visible.length; i++) {
        var id = visible[i];
        var widgetClass = this.widgetsMapper(id);
        //override with new threshold
        if (threshold[id].length > 0) {
          widgetClass.reopen({
            thresh1: threshold[id][0],
            thresh2: threshold[id][1]
          });
        }
        visibleWidgets.pushObject(widgetClass);
      }
      for (var j = 0; j < hidden.length; j++) {
        var title = hidden[j][1];
        hiddenWidgets.pushObject(Em.Object.create({displayName: title, id: hidden[j][0], checked: false}));
      }
      this.set('visibleWidgets', visibleWidgets);
      this.set('hiddenWidgets', hiddenWidgets);
    }
  },

  /**
   * Set visibility-status for widgets
   */
  setOnLoadVisibleWidgets: function () {
    var self = this;
    if (App.get('testMode')) {
      this.translateToReal(this.get('initPrefObject'));
    } else {
      // called when first load/refresh/jump back page
      this.getUserPref(this.get('persistKey')).complete(function () {
        self.setOnLoadVisibleWidgetsComplete.apply(self);
      });
    }
  },

  /**
   * complete load of visible widgets
   */
  setOnLoadVisibleWidgetsComplete: function () {
    var currentPrefObject = this.get('currentPrefObject') || this.getDBProperty(this.get('persistKey'));
    if (currentPrefObject) { // fit for no dashboard version
      if (!currentPrefObject.dashboardVersion) {
        currentPrefObject.dashboardVersion = 'new';
        this.postUserPref(this.get('persistKey'), currentPrefObject);
        this.setDBProperty(this.get('persistKey'), currentPrefObject);
      }
      this.set('currentPrefObject', this.checkServicesChange(currentPrefObject));
      this.translateToReal(this.get('currentPrefObject'));
    }
    else {
      // post persist then translate init object
      this.postUserPref(this.get('persistKey'), this.get('initPrefObject'));
      this.setDBProperty(this.get('persistKey'), this.get('initPrefObject'));
      this.translateToReal(this.get('initPrefObject'));
    }
  },

  /**
   * Remove widget from visible and hidden lists
   * @param {Object} value
   * @param {Object} widget
   * @returns {*}
   */
  removeWidget: function (value, widget) {
    value.visible = value.visible.without(widget);
    for (var j = 0; j < value.hidden.length; j++) {
      if (value.hidden[j][0] == widget) {
        value.hidden.splice(j, 1);
      }
    }
    return value;
  },

  /**
   * Check if widget is in visible or hidden list
   * @param {Object} value
   * @param {Object} widget
   * @returns {bool}
   */
  containsWidget: function (value, widget) {
    var flag = value.visible.contains(widget);
    for (var j = 0; j < value.hidden.length; j++) {
      if (!flag && value.hidden[j][0] == widget) {
        flag = true;
        break;
      }
    }
    return flag;
  },

  /**
   * check if stack has upgraded from HDP 1.0 to 2.0 OR add/delete services.
   * Update the value on server if true.
   * @param {Object} currentPrefObject
   * @return {Object}
   */
  checkServicesChange: function (currentPrefObject) {
    var toDelete = $.extend(true, {}, currentPrefObject);
    var toAdd = [];
    var serviceWidgetsMap = {
      hdfs_model: ['1', '2', '3', '4', '5', '10', '11'],
      host_metrics_model: ['6', '7', '8', '9'],
      hbase_model: ['12', '13', '14', '15', '16'],
      yarn_model: ['17', '18', '19', '20', '23'],
      storm_model: ['21'],
      flume_model: ['22'],
      hawq_model: ['24']
    };

    // check each service, find out the newly added service and already deleted service
    Em.keys(serviceWidgetsMap).forEach(function (modelName) {
      if (!Em.isNone(this.get(modelName))) {
        var ids = serviceWidgetsMap[modelName];
        var flag = this.containsWidget(toDelete, ids[0]);
        if (flag) {
          ids.forEach(function (item) {
            toDelete = this.removeWidget(toDelete, item);
          }, this);
        } else {
          toAdd = toAdd.concat(ids);
        }
      }
    }, this);

    var value = currentPrefObject;
    if (toDelete.visible.length || toDelete.hidden.length) {
      toDelete.visible.forEach(function (item) {
        value = this.removeWidget(value, item);
      }, this);
      toDelete.hidden.forEach(function (item) {
        value = this.removeWidget(value, item[0]);
      }, this);
    }
    if (toAdd.length) {
      value.visible = value.visible.concat(toAdd);
      var allThreshold = this.get('initPrefObject').threshold;
      // add new threshold OR override with default value
      toAdd.forEach(function (item) {
        value.threshold[item] = allThreshold[item];
      }, this);
    }
    return value;
  },

  /**
   * Get view for widget by widget's id
   * @param {string} id
   * @returns {Ember.View}
   */
  widgetsMapper: function (id) {
    return Em.get({
      '1': App.NameNodeHeapPieChartView,
      '2': App.NameNodeCapacityPieChartView,
      '3': App.NameNodeCpuPieChartView,
      '4': App.DataNodeUpView,
      '5': App.NameNodeRpcView,
      '6': App.ChartClusterMetricsMemoryWidgetView,
      '7': App.ChartClusterMetricsNetworkWidgetView,
      '8': App.ChartClusterMetricsCPUWidgetView,
      '9': App.ChartClusterMetricsLoadWidgetView,
      '10': App.NameNodeUptimeView,
      '11': App.HDFSLinksView,
      '12': App.HBaseLinksView,
      '13': App.HBaseMasterHeapPieChartView,
      '14': App.HBaseAverageLoadView,
      '15': App.HBaseRegionsInTransitionView,
      '16': App.HBaseMasterUptimeView,
      '17': App.ResourceManagerHeapPieChartView,
      '18': App.ResourceManagerUptimeView,
      '19': App.NodeManagersLiveView,
      '20': App.YARNMemoryPieChartView,
      '21': App.SuperVisorUpView,
      '22': App.FlumeAgentUpView,
      '23': App.YARNLinksView,
      '24': App.HawqSegmentUpView
    }, id);
  },

  /**
   * @type {Object|null}
   */
  currentPrefObject: null,

  /**
   * @type {Ember.Object}
   */
  initPrefObject: Em.Object.create({
    dashboardVersion: 'new',
    visible: [],
    hidden: [],
    threshold: {1: [80, 90], 2: [85, 95], 3: [90, 95], 4: [80, 90], 5: [1000, 3000], 6: [], 7: [], 8: [], 9: [], 10: [], 11: [], 12: [], 13: [70, 90], 14: [150, 250], 15: [3, 10], 16: [],
      17: [70, 90], 18: [], 19: [50, 75], 20: [50, 75], 21: [85, 95], 22: [85, 95], 23: [], 24: [75, 90]} // id:[thresh1, thresh2]
  }),

  /**
   * Key-name to store data in Local Storage and Persist
   * @type {string}
   */
  persistKey: function () {
    return 'user-pref-' + App.router.get('loginName') + '-dashboard';
  }.property(),

  getUserPrefSuccessCallback: function (response, request, data) {
    if (response) {
      console.log('Got persist value from server with key ' + data.key + '. Value is: ' + response);
      var initPrefObject = this.get('initPrefObject');
      initPrefObject.get('threshold');
      for(var k in response.threshold) {
        if (response.threshold.hasOwnProperty(k)) {
          if (response.threshold[k].length === 0 && initPrefObject.get('threshold')[k] && initPrefObject.get('threshold')[k].length) {
            response.threshold[k] = initPrefObject.get('threshold')[k];
          }
        }
      }
      this.set('currentPrefObject', response);
    }
  },

  getUserPrefErrorCallback: function (request) {
    // this user is first time login
    if (request.status == 404) {
      console.log('Persist did NOT find the key');
    }
  },

  /**
   * Reset widgets visibility-status
   */
  resetAllWidgets: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      if (!App.get('testMode')) {
        self.postUserPref(self.get('persistKey'), self.get('initPrefObject'));
        self.setDBProperty(self.get('persistKey'), self.get('initPrefObject'));
      }
      self.setProperties({
        currentTimeRangeIndex: 0,
        customStartTime: null,
        customEndTime: null
      });
      self.translateToReal(self.get('initPrefObject'));
    });
  },

  showAlertsPopup: Em.K

});

