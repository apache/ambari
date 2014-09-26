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

App.MainDashboardWidgetsView = Em.View.extend(App.UserPref, App.LocalStorage, {

  name: 'mainDashboardWidgetsView',

  templateName:require('templates/main/dashboard/widgets'),

  didInsertElement:function () {
    this.setWidgetsDataModel();
    this.setInitPrefObject();
    this.setOnLoadVisibleWidgets();
    this.set('isDataLoaded',true);
    Em.run.next(this, 'makeSortable');
  },

  /**
   * List of services
   * @type {Ember.Enumerable}
   */
  content:[],

  /**
   * @type {bool}
   */
  isDataLoaded: false,

  /**
   * Define if some widget is currently moving
   * @type {bool}
   */
  isMoving: false,

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    $( "#sortable" ).sortable({
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
      activate: function(event, ui) {
        self.set('isMoving', true);
      },
      deactivate: function(event, ui) {
        self.set('isMoving', false);
      }
    }).disableSelection();
  },

  /**
   * Set Service model values
   */
  setWidgetsDataModel: function () {
    var services = App.Service.find();
    var self = this;
    services.forEach(function (item) {
      switch (item.get('serviceName')) {
        case "HDFS":
          self.set('hdfs_model',  App.HDFSService.find(item.get('id')) || item);
          break;
        case "YARN":
          self.set('yarn_model', App.YARNService.find(item.get('id')) || item);
          break;
        case "MAPREDUCE":
          self.set('mapreduce_model', App.MapReduceService.find(item.get('id')) || item);
          break;
        case "HBASE":
          self.set('hbase_model', App.HBaseService.find(item.get('id')) || item);
          break;
        case "STORM":
          self.set('storm_model', item);
          break;
        case "FLUME":
          self.set('flume_model', item);
          break;
      }
    }, this);
  },

  /**
   * Load widget statuses to <code>initPrefObject</code>
   */
  setInitPrefObject: function() {
    //in case of some service not installed
    var visibleFull = [
      '2', '4', '8', '10',
      '17', '11', '12', '13', '14',
      '18', '1', '6', '5', '9',
      '3', '7', '15', '16', '20',
      '19', '21', '23',
      '24', '25', '26', '27',// all yarn
      '28', // storm
      '29' // flume
    ]; // all in order
    var hiddenFull = [['22','Region In Transition']];
    if (this.get('hdfs_model') == null) {
      var hdfs= ['1', '2', '3', '4', '5', '15', '17'];
      hdfs.forEach ( function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('mapreduce_model') == null) {
      var map = ['6', '7', '8', '9', '10', '16', '18'];
      map.forEach ( function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('hbase_model') == null) {
      var hbase = ['19', '20', '21', '23'];
      hbase.forEach ( function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
      hiddenFull = [];
    }
    if (this.get('yarn_model') == null) {
      var yarn = ['24', '25', '26', '27'];
      yarn.forEach ( function (item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('storm_model') == null) {
      var storm = ['28'];
      storm.forEach(function(item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    if (this.get('flume_model') == null) {
      var flume = ['29'];
      flume.forEach(function(item) {
        visibleFull = visibleFull.without(item);
      }, this);
    }
    var obj = this.get('initPrefObject');
    obj.set('visible', visibleFull);
    obj.set('hidden', hiddenFull);
  },

  hdfs_model: null,

  mapreduce_model: null,

  mapreduce2_model: null,

  yarn_model: null,

  hbase_model: null,

  storm_model: null,

  flume_model: null,

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
   */
  plusButtonFilterView: Ember.View.extend({
      templateName: require('templates/main/dashboard/plus_button_filter'),
      hiddenWidgetsBinding: 'parentView.hiddenWidgets',
      visibleWidgetsBinding: 'parentView.visibleWidgets',
      valueBinding: '',
      widgetCheckbox: Em.Checkbox.extend({
        didInsertElement: function() {
          $('.checkbox').click(function(event) {
            event.stopPropagation();
          });
        }
      }),
      closeFilter:function () {
      },
      applyFilter:function() {
        this.closeFilter();
        var parent = this.get('parentView');
        var hiddenWidgets = this.get('hiddenWidgets');
        var checkedWidgets = hiddenWidgets.filterProperty('checked', true);

        if (App.get('testMode')) {
          var visibleWidgets = this.get('visibleWidgets');
          checkedWidgets.forEach(function(item){
            var newObj = parent.widgetsMapper(item.id);
            visibleWidgets.pushObject(newObj);
            hiddenWidgets.removeObject(item);
          }, this);
        } else {
          //save in persist
          parent.getUserPref(parent.get('persistKey')).complete(function () {
            var oldValue = parent.get('currentPrefObject') || parent.getDbProperty(parent.get('persistKey'));
            var newValue = Em.Object.create({
              dashboardVersion: oldValue.dashboardVersion,
              visible: oldValue.visible,
              hidden: [],
              threshold: oldValue.threshold
            });
            checkedWidgets.forEach(function (item) {
              newValue.visible.push(item.id);
              hiddenWidgets.removeObject(item);
            }, this);
            hiddenWidgets.forEach(function (item) {
              newValue.hidden.push([item.id, item.displayName]);
            }, this);
            parent.postUserPref(parent.get('persistKey'), newValue);
            parent.setDBProperty(parent.get('persistKey'), newValue);
            parent.translateToReal(newValue);
          });
        }
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

    if (version == 'classic') {
      this.set('isClassicDashboard', true);
    } else if (version == 'new') {
      this.set('isClassicDashboard', false);
      var visibleWidgets = [];
      var hiddenWidgets = [];
      // re-construct visibleWidgets and hiddenWidgets
      for (var j = 0; j <= visible.length -1; j++) {
        var id = visible[j];
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
      for (var j = 0; j <= hidden.length -1; j++) {
        var id = hidden[j][0];
        var title = hidden[j][1];
        hiddenWidgets.pushObject(Em.Object.create({displayName:title , id: id, checked: false}));
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
      self.getUserPref(this.get('persistKey')).complete(function () {
        var currentPrefObject = self.get('currentPrefObject') || self.getDBProperty(self.get('persistKey'));
        if (currentPrefObject) { // fit for no dashboard version
          if (!currentPrefObject.dashboardVersion) {
            currentPrefObject.dashboardVersion = 'new';
            self.postUserPref(self.get('persistKey'), currentPrefObject);
            self.setDBProperty(self.get('persistKey'), currentPrefObject);
          }
          self.set('currentPrefObject', self.checkServicesChange(currentPrefObject));
          self.translateToReal(self.get('currentPrefObject'));
        }
        else {
          // post persist then translate init object
          self.postUserPref(self.get('persistKey'), self.get('initPrefObject'));
          self.setDBProperty(self.get('persistKey'), self.get('initPrefObject'));
          self.translateToReal(self.get('initPrefObject'));
        }
      });
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
    for (var j = 0; j <= value.hidden.length -1; j++) {
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
    var flag = value.visible.contains (widget);
    for (var j = 0; j <= value.hidden.length -1; j++) {
      if ( !flag && value.hidden[j][0] == widget) {
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
    var self = this;

    // check each service, find out the newly added service and already deleted service
    if (this.get('hdfs_model') != null) {
      var hdfsAndMetrics= ['1', '2', '3', '4', '5', '15', '17', '11', '12', '13', '14'];
      hdfsAndMetrics.forEach ( function (item) {
        toDelete = self.removeWidget(toDelete, item);
      }, this);
    }
    else {
      var graphs = ['11', '12', '13', '14'];
      graphs.forEach ( function (item) {
        toDelete = self.removeWidget(toDelete, item);
      }, this);
    }
    if (this.get('mapreduce_model') != null) {
      var map = ['6', '7', '8', '9', '10', '16', '18'];
      var flag = self.containsWidget(toDelete, map[0]);
      if (flag) {
        map.forEach ( function (item) {
          toDelete = self.removeWidget(toDelete, item);
        }, this);
      } else {
        toAdd = toAdd.concat(map);
      }
    }
    if (this.get('hbase_model') != null) {
      var hbase = ['19', '20', '21', '22', '23'];
      var flag = self.containsWidget(toDelete, hbase[0]);
      if (flag) {
        hbase.forEach ( function (item) {
          toDelete = self.removeWidget(toDelete, item);
        }, this);
      } else {
        toAdd = toAdd.concat(hbase);
      }
    }
    if (this.get('yarn_model') != null) {
      var yarn = ['24', '25', '26', '27'];
      var flag = self.containsWidget(toDelete, yarn[0]);
      if (flag) {
        yarn.forEach ( function (item) {
          toDelete = self.removeWidget(toDelete, item);
        }, this);
      } else {
        toAdd = toAdd.concat(yarn);
      }
    }
    if (this.get('storm_model') != null) {
      var storm = ['28'];
      var flag = self.containsWidget(toDelete, storm[0]);
      if (flag) {
        storm.forEach ( function (item) {
          toDelete = self.removeWidget(toDelete, item);
        }, this);
      } else {
        toAdd = toAdd.concat(storm);
      }
    }
    if (this.get('flume_model') != null) {
      var flume = ['29'];
      var flag = self.containsWidget(toDelete, flume[0]);
      if (flag) {
        flume.forEach ( function (item) {
          toDelete = self.removeWidget(toDelete, item);
        }, this);
      } else {
        toAdd = toAdd.concat(flume);
      }
    }
    var value = currentPrefObject;
    if (toDelete.visible.length || toDelete.hidden.length) {
      toDelete.visible.forEach ( function (item) {
        value = self.removeWidget(value, item);
      }, this);
      toDelete.hidden.forEach ( function (item) {
        value = self.removeWidget(value, item[0]);
      }, this);
    }
    if (toAdd.length) {
      value.visible = value.visible.concat(toAdd);
      var allThreshold = this.get('initPrefObject').threshold;
      // add new threshold OR override with default value
      toAdd.forEach ( function (item) {
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
      '6': App.JobTrackerHeapPieChartView,
      '7': App.JobTrackerCpuPieChartView,
      '8': App.TaskTrackerUpView,
      '9': App.JobTrackerRpcView,
      '10': App.MapReduceSlotsView,
      '11': App.ChartClusterMetricsMemoryWidgetView,
      '12': App.ChartClusterMetricsNetworkWidgetView,
      '13': App.ChartClusterMetricsCPUWidgetView,
      '14': App.ChartClusterMetricsLoadWidgetView,
      '15': App.NameNodeUptimeView,
      '16': App.JobTrackerUptimeView,
      '17': App.HDFSLinksView,
      '18': App.MapReduceLinksView,
      '19': App.HBaseLinksView,
      '20': App.HBaseMasterHeapPieChartView,
      '21': App.HBaseAverageLoadView,
      '22': App.HBaseRegionsInTransitionView,
      '23': App.HBaseMasterUptimeView,
      '24': App.ResourceManagerHeapPieChartView,
      '25': App.ResourceManagerUptimeView,
      '26': App.NodeManagersLiveView,
      '27': App.YARNMemoryPieChartView,
      '28': App.SuperVisorUpView,
      '29': App.FlumeAgentUpView
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
    threshold: {1: [80, 90], 2: [85, 95], 3: [90, 95], 4: [80, 90], 5: [1000, 3000], 6: [70, 90], 7: [90, 95], 8: [50, 75], 9: [30000, 120000],
      10: [], 11: [], 12: [], 13: [], 14: [], 15: [], 16: [], 17: [], 18: [], 19: [], 20: [70, 90], 21: [10, 19.2], 22: [3, 10], 23: [],
      24: [70, 90], 25: [], 26: [50, 75], 27: [50, 75], 28: [85, 95], 29: [85, 95]} // id:[thresh1, thresh2]
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
  resetAllWidgets: function() {
    var self = this;
    App.showConfirmationPopup(function() {
      if(!App.get('testMode')) {
        self.postUserPref(self.get('persistKey'), self.get('initPrefObject'));
        self.setDBProperty(self.get('persistKey'), self.get('initPrefObject'));
      }
      self.translateToReal(self.get('initPrefObject'));
    });
  },

  /**
   * @type {string}
   */
  gangliaUrl: function () {
    return App.router.get('clusterController.gangliaUrl') + "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPSlaves&tab=m&vn=";
  }.property('App.router.clusterController.gangliaUrl'),

  showAlertsPopup: function (event) {
    var service = event.context;
    App.router.get('mainAlertsController').loadAlerts(service.get('serviceName'), "SERVICE");
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/alert_notification_popup'),
        service: service,
        controllerBinding: 'App.router.mainAlertsController',
        warnAlerts: function () {
          return this.get('controller.alerts').filterProperty('isOk', false).filterProperty('ignoredForServices', false);
        }.property('controller.alerts'),

        warnAlertsCount: function () {
          return this.get('warnAlerts').length;
        }.property('warnAlerts'),

        warnAlertsMessage: function() {
          return Em.I18n.t('services.alerts.head').format(this.get('warnAlertsCount'));
        }.property('warnAlertsCount'),

        nagiosUrl: function () {
          return App.router.get('clusterController.nagiosUrl');
        }.property('App.router.clusterController.nagiosUrl'),

        closePopup: function () {
          this.get('parentView').hide();
        },

        viewNagiosUrl: function () {
          window.open(this.get('nagiosUrl'), "_blank");
          this.closePopup();
        },

        selectService: function () {
          App.router.transitionTo('services.service.summary', service);
          this.closePopup();
        }
      }),
      primary: Em.I18n.t('common.close'),
      secondary : null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
        this.$().children('.modal').css({'margin-top': '-350px'});
      }
    });
    event.stopPropagation();
  }

});

