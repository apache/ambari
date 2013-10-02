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

App.MainDashboardView = Em.View.extend({
  templateName:require('templates/main/dashboard'),
  didInsertElement:function () {
    this.services();
    this.setWidgetsDataModel();
    this.setInitPrefObject();
    this.setOnLoadVisibleWidgets();
    this.set('isDataLoaded',true);
    Ember.run.next(this, 'makeSortable');
  },
  content:[],
  isDataLoaded: false,
  isClassicDashboard: false,

  makeSortable: function () {
    var self = this;
    $( "#sortable" ).sortable({
      items: "> div",
      //placeholder: "sortable-placeholder",
      cursor: "move",
      update: function (event, ui) {
        if (!App.testMode) {
          // update persist then translate to real
          var widgetsArray = $('div[viewid]'); // get all in DOM
          self.getUserPref(self.get('persistKey'));
          var oldValue = self.get('currentPrefObject');
          var newValue = Em.Object.create({
            dashboardVersion: oldValue.dashboardVersion,
            visible: [],
            hidden: oldValue.hidden,
            threshold: oldValue.threshold
          });
          var size = oldValue.visible.length;
          for(var j = 0; j <= size -1; j++){
            var viewID = widgetsArray.get(j).getAttribute('viewid');
            var id = viewID.split("-").get(1);
            newValue.visible.push(id);
          }
          self.postUserPref(self.get('persistKey'), newValue);
          //self.translateToReal(newValue);
        }
      }
    });
    $( "#sortable" ).disableSelection();
  },

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
      }
    }, this);
  },
  setInitPrefObject: function() {
    //in case of some service not installed
    var visibleFull = [
      '2', '4', '8', '10',
      '17', '11', '12', '13', '14',
      '18', '1', '6', '5', '9',
      '3', '7', '15', '16', '20',
      '19', '21', '23',
      '24', '25', '26', '27'// all yarn
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
    }if (this.get('yarn_model') == null) {
      var yarn = ['24', '25', '26', '27'];
      yarn.forEach ( function (item) {
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
  visibleWidgets: [],
  hiddenWidgets: [], // widget child view will push object in this array if deleted

  plusButtonFilterView: filters.createComponentView({
    /**
     * Base methods was implemented in <code>filters.componentFieldView</code>
     */
    hiddenWidgetsBinding: 'parentView.hiddenWidgets',
    visibleWidgetsBinding: 'parentView.visibleWidgets',
    layout: null,

    filterView: filters.componentFieldView.extend({
      templateName:require('templates/main/dashboard/plus_button_filter'),
      hiddenWidgetsBinding: 'parentView.hiddenWidgets',
      visibleWidgetsBinding: 'parentView.visibleWidgets',
      valueBinding: '',
      applyFilter:function() {
        this._super();
        var parent = this.get('parentView').get('parentView');
        var hiddenWidgets = this.get('hiddenWidgets');
        var checkedWidgets = hiddenWidgets.filterProperty('checked', true);

        if (App.testMode) {
          var visibleWidgets = this.get('visibleWidgets');
          checkedWidgets.forEach(function(item){
            var newObj = parent.widgetsMapper(item.id);
            visibleWidgets.pushObject(newObj);
            hiddenWidgets.removeObject(item);
          }, this);
        } else {
          //save in persist
          parent.getUserPref(parent.get('persistKey'));
          var oldValue = parent.get('currentPrefObject');
          var newValue = Em.Object.create({
            dashboardVersion: oldValue.dashboardVersion,
            visible: oldValue.visible,
            hidden: [],
            threshold: oldValue.threshold
          });
          checkedWidgets.forEach(function(item){
            newValue.visible.push(item.id);
            hiddenWidgets.removeObject(item);
          }, this);
          hiddenWidgets.forEach(function(item){
            newValue.hidden.push([item.id, item.displayName]);
          }, this);

          parent.postUserPref(parent.get('persistKey'), newValue);
          parent.translateToReal(newValue);
        }
      }
    })
  }),

  /**
   * translate from Json value got from persist to real widgets view
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

  setOnLoadVisibleWidgets: function () {
    if (App.testMode) {
      this.translateToReal(this.get('initPrefObject'));
    } else {
      // called when first load/refresh/jump back page
      this.getUserPref(this.get('persistKey'));
      var currentPrefObject = this.get('currentPrefObject');
      if (currentPrefObject) { // fit for no dashboard version
        if (!currentPrefObject.dashboardVersion) {
          currentPrefObject.dashboardVersion = 'new';
          this.postUserPref(this.get('persistKey'), currentPrefObject);
        }
        this.set('currentPrefObject', this.checkServicesChange(currentPrefObject));
        this.translateToReal(this.get('currentPrefObject'));
      } else {
        // post persist then translate init object
        if(App.get('isAdmin')) {
          this.postUserPref(this.get('persistKey'), this.get('initPrefObject'));
        }
        this.translateToReal(this.get('initPrefObject'));
      }
    }
  },
  removeWidget: function (value, itemToRemove) {
    value.visible = value.visible.without(itemToRemove);
    for (var j = 0; j <= value.hidden.length -1; j++) {
      if (value.hidden[j][0] == itemToRemove) {
        value.hidden.splice(j, 1);
      }
    }
    return value;
  },
  containsWidget: function (value, item) {
    var flag = value.visible.contains (item);
    for (var j = 0; j <= value.hidden.length -1; j++) {
      if ( !flag && value.hidden[j][0] == item) {
        flag = true;
        break;
      }
    }
    return flag;
  },
  /**
   * check if stack has upgraded from HDP 1.0 to 2.0 OR add/delete services.
   * Update the value on server if true.
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

  widgetsMapper: function (id) {
    switch(id){
      case '1': return App.NameNodeHeapPieChartView;
      case '2': return App.NameNodeCapacityPieChartView;
      case '3': return App.NameNodeCpuPieChartView;
      case '4': return App.DataNodeUpView;
      case '5': return App.NameNodeRpcView;
      case '6': return App.JobTrackerHeapPieChartView;
      case '7': return App.JobTrackerCpuPieChartView;
      case '8': return App.TaskTrackerUpView;
      case '9': return App.JobTrackerRpcView;
      case '10': return App.MapReduceSlotsView;
      case '11': return App.ChartClusterMetricsMemoryWidgetView;
      case '12': return App.ChartClusterMetricsNetworkWidgetView;
      case '13': return App.ChartClusterMetricsCPUWidgetView;
      case '14': return App.ChartClusterMetricsLoadWidgetView;
      case '15': return App.NameNodeUptimeView;
      case '16': return App.JobTrackerUptimeView;
      case '17': return App.HDFSLinksView;
      case '18': return App.MapReduceLinksView;
      case '19': return App.HBaseLinksView;
      case '20': return App.HBaseMasterHeapPieChartView;
      case '21': return App.HBaseAverageLoadView;
      case '22': return App.HBaseRegionsInTransitionView;
      case '23': return App.HBaseMasterUptimeView;
      case '24': return App.ResourceManagerHeapPieChartView;
      case '25': return App.ResourceManagerUptimeView;
      case '26': return App.NodeManagersLiveView;
      case '27': return App.YARNMemoryPieChartView;
    }
  },

  currentPrefObject: null,
  initPrefObject: Em.Object.create({
    dashboardVersion: 'new',
    visible: [],
    hidden: [],
    threshold: {1: [80, 90], 2: [85, 95], 3: [90, 95], 4: [80, 90], 5: [1000, 3000], 6: [70, 90], 7: [90, 95], 8: [50, 75], 9: [30000, 120000],
      10: [], 11: [], 12: [], 13: [], 14: [], 15: [], 16: [], 17: [], 18: [], 19: [], 20: [70, 90], 21: [10, 19.2], 22: [3, 10], 23: [],
      24: [70, 90], 25: [], 26: [50, 75], 27: [50, 75]} // id:[thresh1, thresh2]
  }),
  persistKey: function () {
    var loginName = App.router.get('loginName');
    return 'user-pref-' + loginName + '-dashboard';
  }.property(''),

  /**
   * get persist value from server with persistKey
   */
  getUserPref: function(key){
    App.ajax.send({
      name: 'dashboard.get.user_pref',
      sender: this,
      data: {
        key: key
      },
      success: 'getUserPrefSuccessCallback',
      error: 'getUserPrefErrorCallback'
    });
  },

  getUserPrefSuccessCallback: function (response, request, data) {
    if (response) {
      console.log('Got persist value from server with key ' + data.key + '. Value is: ' + response);
      this.set('currentPrefObject', response);
    }
  },

  getUserPrefErrorCallback: function (request, ajaxOptions, error) {
    // this user is first time login
    if (request.status == 404) {
      console.log('Persist did NOT find the key');
      return null;
    }
  },

  /**
   * post persist key/value to server, value is object
   */
  postUserPref: function (key, value) {
    var url = App.apiPrefix + '/persist/';
    var keyValuePair = {};
    keyValuePair[key] = JSON.stringify(value);

    App.ajax.send({
      'name': 'dashboard.post.user_pref',
      'sender': this,
      'beforeSend': 'postUserPrefBeforeSend',
      'data': {
        'keyValuePair': keyValuePair
      }
    });
  },

  postUserPrefBeforeSend: function(request, ajaxOptions, data){
    console.log('BeforeSend to persist: persistKeyValues', data.keyValuePair);
  },

  resetAllWidgets: function(){
    var self = this;
    App.showConfirmationPopup(function() {
      if(!App.testMode){
        self.postUserPref(self.get('persistKey'), self.get('initPrefObject'));
      }
      self.translateToReal(self.get('initPrefObject'));
    });
  },

  switchToClassic: function () {
    if(!App.testMode){
      this.getUserPref(this.get('persistKey'));
      var oldValue = this.get('currentPrefObject');
      oldValue.dashboardVersion = 'classic';
      this.postUserPref(this.get('persistKey'), oldValue);
    }else{
      var oldValue = this.get('initPrefObject');
      oldValue.dashboardVersion = 'classic';
    }
    this.translateToReal(oldValue);
  },
  switchToNew: function () {
    if(!App.testMode){
      this.getUserPref(this.get('persistKey'));
      var oldValue = this.get('currentPrefObject');
      oldValue.dashboardVersion = 'new';
      this.postUserPref(this.get('persistKey'), oldValue);
      this.didInsertElement();
    }else{
      var oldValue = this.get('initPrefObject');
      oldValue.dashboardVersion = 'new';
      this.translateToReal(oldValue);
    }
  },

  updateServices: function(){
    var services = App.Service.find();
    services.forEach(function (item) {
      var view;
      switch (item.get('serviceName')) {
        case "HDFS":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceHdfsView);
          view.objectAt(0).set('model', App.HDFSService.find(item.get('id')));
          break;
        case "MAPREDUCE":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceMapreduceView);
          view.objectAt(0).set('model', App.MapReduceService.find(item.get('id')));
          break;
        case "HBASE":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceHbaseView);
          view.objectAt(0).set('model', App.HBaseService.find(item.get('id')));
      }
    }, this);
  }.observes('App.router.updateController.isUpdate'),
  services: function () {
    var services = App.Service.find();
    if (this.get('content').length > 0) {
      return false
    }
    services.forEach(function (item) {
      var vName;
      var item2;
      switch (item.get('serviceName')) {
        case "HDFS":
          vName = App.MainDashboardServiceHdfsView;
          item2 = App.HDFSService.find(item.get('id'));
          break;
        case "YARN":
          vName = App.MainDashboardServiceYARNView;
          item2 = App.YARNService.find(item.get('id'));
          break;
        case "MAPREDUCE":
          vName = App.MainDashboardServiceMapreduceView;
          item2 = App.MapReduceService.find(item.get('id'));
          break;
        case "MAPREDUCE2":
          vName = App.MainDashboardServiceMapreduce2View;
          break;
        case "HBASE":
          vName = App.MainDashboardServiceHbaseView;
          item2 = App.HBaseService.find(item.get('id'));
          break;
        case "HIVE":
          vName = App.MainDashboardServiceHiveView;
          break;
        case "ZOOKEEPER":
          vName = App.MainDashboardServiceZookeperView;
          break;
        case "OOZIE":
          vName = App.MainDashboardServiceOozieView;
          break;
        default:
          vName = Em.View;
      }
      this.get('content').pushObject(Em.Object.create({
        viewName: vName,
        model: item2 || item
      }))
    }, this);
  },

  gangliaUrl: function () {
    return App.router.get('clusterController.gangliaUrl') + "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPSlaves&tab=m&vn=";
  }.property('App.router.clusterController.gangliaUrl'),

  showAlertsPopup: function (event) {
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        service: event.context,
        warnAlerts: function () {
          var allAlerts = App.router.get('clusterController.alerts');
          var serviceId = this.get('service.serviceName');
          if (serviceId) {
            return allAlerts.filterProperty('serviceType', serviceId).filterProperty('isOk', false).filterProperty('ignoredForServices', false);
          }
          return 0;
        }.property('App.router.clusterController.alerts'),

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
          App.router.transitionTo('services.service.summary', event.context)
          this.closePopup();
        },
        templateName: require('templates/main/dashboard/alert_notification_popup')
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
