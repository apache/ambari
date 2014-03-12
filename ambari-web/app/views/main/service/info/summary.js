/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.AlertItemView = Em.View.extend({
  tagName:"li",
  templateName: require('templates/main/service/info/summary_alert'),
  classNameBindings: ["status"],
  status: function () {
    return "status-" + this.get("content.status");
  }.property('content'),
  didInsertElement: function () {
    // Tooltips for alerts need to be enabled.
    App.tooltip($("div[rel=tooltip]"));
    $(".tooltip").remove();
  }
});

App.MainServiceInfoSummaryView = Em.View.extend({
  templateName: require('templates/main/service/info/summary'),
  attributes:null,
  serviceStatus:{
    hdfs:false,
    yarn:false,
    mapreduce:false,
    mapreduce2:false,
    hbase:false,
    zookeeper:false,
    oozie:false,
    hive:false,
    ganglia:false,
    nagios:false,
    hue: false,
    flume: false,
    falcon: false,
    storm: false,
    tez: false,
    pig :false,
    sqoop: false
  },

  servicesHaveClients: ["OOZIE", "ZOOKEEPER", "HIVE", "MAPREDUCE2", "TEZ", "SQOOP", "PIG","FALCON"],

  sumMasterComponentView : Em.View.extend({
    didInsertElement: function() {
      App.tooltip($('[rel=healthTooltip]'));
    },
    templateName: require('templates/main/service/info/summary/master_components'),
    mastersComp : function(){
      return this.get('parentView.service.hostComponents').filterProperty('isMaster', true);
    }.property("service")
  }),

  alertsControllerBinding: 'App.router.mainAlertsController',
  alerts: function () {
    return this.get('alertsController.alerts');
  }.property('alertsController.alerts'),

  noTemplateService: function () {
    var serviceName = this.get("service.serviceName");
    //services with only master components
    return serviceName == "WEBHCAT" || serviceName == "NAGIOS";
  }.property('controller.content'),

  clients: function () {
    var service = this.get('controller.content');
    if (this.get('servicesHaveClients').contains(service.get("id"))) {
      return service.get('hostComponents').filterProperty('isClient');
    }
    return [];
  }.property('controller.content'),

  hasManyServers: function () {
    return this.get('servers').length > 1;
  }.property('servers'),

  clientsHostText: function () {
    if (this.get('clients').length == 0) {
      return '';
    } else if (this.get("hasManyClients")) {
      return Em.I18n.t('services.service.summary.viewHosts');
    } else {
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("hasManyClients"),

  hasManyClients: function () {
    return this.get('clients').length > 1;
  }.property('clients'),

  servers: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER" || service.get("id") == "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        result = [{
          'host': servers[0].get('displayName'),
          'isComma': false,
          'isAnd': false
        }];
      }
      if (servers.length > 1) {
        result[0].isComma = true;
        result.push({
          'host': servers[1].get('displayName'),
          'isComma': false,
          'isAnd': false
        });
      }
      if (servers.length > 2) {
        result[1].isAnd = true;
        result.push({
          'host': Em.I18n.t('services.service.info.summary.serversHostCount').format(servers.length - 2),
          'isComma': false,
          'isAnd': false
        });
      }
    }
    return result;
  }.property('controller.content'),

  historyServerUI: function(){
    var service=this.get('controller.content');
    return (App.singleNodeInstall ? "http://" + App.singleNodeAlias + ":19888" : "http://" + service.get("hostComponents").findProperty('isMaster', true).get("host").get("publicHostName")+":19888");
  }.property('controller.content'),

  monitors: function () {
    var result = '';
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('hostComponents').filterProperty('isMaster', false);
      var liveMonitors = monitors.filterProperty("workStatus","STARTED").length;
      this.set("liveMonitors", liveMonitors);
      if (monitors.length) {
        result = Em.I18n.t('services.service.info.summary.hostsRunningMonitor').format(liveMonitors, monitors.length);
      }
    }
    return result;
  }.property('controller.content'),

  monitorsLiveTextView: App.ComponentLiveTextView.extend({
    liveComponents: function () {
      var service = this.get('service');
      if (service.get("id") == "GANGLIA") {
        return service.get('hostComponents').filterProperty('isMaster', false).filterProperty("workStatus","STARTED").length;
      } else {
        return null;
      }
    }.property('service.hostComponents.@each'),
    monitors: function () {
      var result = '';
      var service = this.get('parentView.controller.content');
      if (service.get("id") == "GANGLIA") {
        var monitors = service.get('hostComponents').filterProperty('isMaster', false);
        var liveMonitors = monitors.filterProperty("workStatus","STARTED").length;
        if (monitors.length) {
          result = Em.I18n.t('services.service.info.summary.hostsRunningMonitor').format(liveMonitors, monitors.length);
        }
      }
      return result;
    }.property('service.hostComponents.@each')
  }),

  hasManyMonitors: function () {
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('hostComponents').filterProperty('isMaster', false);
      if (monitors.length == 0) {
        return '';
      } else if (monitors.length > 1) {
        return Em.I18n.t('services.service.summary.viewHosts');
      } else {
        return Em.I18n.t('services.service.summary.viewHost');
      }
    }
    return result;
  }.property('controller.content'),

  /**
   * Property related to GANGLIA service, is unused for other services
   * @return {Object}
   */
  monitorsObj: function(){
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('hostComponents').filterProperty('isMaster', false);
      if (monitors.length) {
        return monitors[0];
      }
    }
    return {};
  }.property('controller.content'),

  /**
   * Property related to ZOOKEEPER service, is unused for other services
   * @return {Object}
   */
  serversHost: function() {
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER" || service.get("id") == "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        return servers[0];
      }
    }
    return {};
  }.property('controller.content'),

  /**
   * Property related to OOZIE and ZOOKEEPER services, is unused for other services
   * HIVE is supported too
   * @return {Object}
   */
  clientObj: function() {
    var service = this.get('controller.content');
    if (this.get('servicesHaveClients').contains(service.get("id"))) {
      var clients = service.get('hostComponents').filterProperty('isClient', true);
      if (clients.length > 0) {
        return clients[0];
      }
    }
    return {};
  }.property('controller.content'),

  data:{
    hive:{
      "database":"PostgreSQL",
      "databaseName":"hive",
      "user":"hive"
    }
  },

  /**
   * Wrapper for displayName. used to render correct display name for mysql_server
   */
  componentNameView: Ember.View.extend({
    template: Ember.Handlebars.compile('{{view.displayName}}'),
    comp : null,
    displayName: function(){
      if(this.get('comp.componentName') == 'MYSQL_SERVER'){
        return this.t('services.hive.databaseComponent');
      }
      return this.get('comp.displayName');
    }.property('comp')
  }),

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
        case 'mapreduce':
          svc = App.MapReduceService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        case 'storm':
          svc = App.StormService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName').volatile(),

  isHide:true,
  moreStatsView:Em.View.extend({
    tagName:"a",
    template:Ember.Handlebars.compile('{{t services.service.summary.moreStats}}'),
    attributeBindings:[ 'href' ],
    classNames:[ 'more-stats' ],
    click:function (event) {
      this._parentView._parentView.set('isHide', false);
      this.remove();
    },
    href:'javascript:void(null)'
  }),

  serviceName:function () {
    return this.get('service.serviceName');
  }.property('service'),

  oldServiceName:'',

  /**
   * Contains graphs for this particular service
   */
  serviceMetricGraphs:function () {
    var svcName = this.get('service.serviceName');
    var graphs = [];
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          graphs = [ [App.ChartServiceMetricsHDFS_SpaceUtilization.extend(),
            App.ChartServiceMetricsHDFS_FileOperations.extend(),
            App.ChartServiceMetricsHDFS_BlockStatus.extend(),
            App.ChartServiceMetricsHDFS_IO.extend()],
            [App.ChartServiceMetricsHDFS_RPC.extend(),
            App.ChartServiceMetricsHDFS_GC.extend(),
            App.ChartServiceMetricsHDFS_JVMHeap.extend(),
            App.ChartServiceMetricsHDFS_JVMThreads.extend()]];
          break;
        case 'yarn':
          graphs = [[App.ChartServiceMetricsYARN_AllocatedMemory.extend(),
              App.ChartServiceMetricsYARN_QMR.extend(),
              App.ChartServiceMetricsYARN_AllocatedContainer.extend(),
              App.ChartServiceMetricsYARN_NMS.extend()],
            [App.ChartServiceMetricsYARN_ApplicationCurrentStates.extend(),
             App.ChartServiceMetricsYARN_ApplicationFinishedStates.extend(),
             App.ChartServiceMetricsYARN_RPC.extend(),
            App.ChartServiceMetricsYARN_GC.extend()
            ],
            [App.ChartServiceMetricsYARN_JVMThreads.extend(),
             App.ChartServiceMetricsYARN_JVMHeap.extend()]];
          break;
        case 'mapreduce':
          graphs = [ [App.ChartServiceMetricsMapReduce_JobsStatus.extend(),
            App.ChartServiceMetricsMapReduce_TasksRunningWaiting.extend(),
            App.ChartServiceMetricsMapReduce_MapSlots.extend(),
            App.ChartServiceMetricsMapReduce_ReduceSlots.extend()],
            [App.ChartServiceMetricsMapReduce_GC.extend(),
            App.ChartServiceMetricsMapReduce_RPC.extend(),
            App.ChartServiceMetricsMapReduce_JVMHeap.extend(),
            App.ChartServiceMetricsMapReduce_JVMThreads.extend()]];
          break;
        case 'hbase':
          graphs = [  [App.ChartServiceMetricsHBASE_ClusterRequests.extend(),
            App.ChartServiceMetricsHBASE_RegionServerReadWriteRequests.extend(),
            App.ChartServiceMetricsHBASE_RegionServerRegions.extend(),
            App.ChartServiceMetricsHBASE_RegionServerQueueSize.extend()],
            [App.ChartServiceMetricsHBASE_HlogSplitTime.extend(),
            App.ChartServiceMetricsHBASE_HlogSplitSize.extend()]];
          break;
        case 'flume':
          graphs = [[App.ChartServiceMetricsFlume_ChannelFillPercent.extend(),
             App.ChartServiceMetricsFlume_ChannelSize.extend(),
             App.ChartServiceMetricsFlume_SourceAcceptedCount.extend(),
             App.ChartServiceMetricsFlume_SinkDrainSuccessCount.extend()],
             [App.ChartServiceMetricsFlume_SinkConnectionFailedCount.extend(),
              //App.ChartServiceMetricsFlume_GarbageCollection.extend(),
              //App.ChartServiceMetricsFlume_JVMHeapUsed.extend(),
              //App.ChartServiceMetricsFlume_JVMThreadsRunnable.extend(),
              App.ChartServiceMetricsFlume_CPUUser.extend()]];
          break;
        case 'storm':
          graphs = [
            [
              App.ChartServiceMetricsSTORM_SlotsNumber.extend(),
              App.ChartServiceMetricsSTORM_Executors.extend(),
              App.ChartServiceMetricsSTORM_Topologies.extend(),
              App.ChartServiceMetricsSTORM_Tasks.extend()
            ]
          ];
          break;
        default:
          break;
      }
    }
    return graphs;
  }.property(''),

  loadServiceSummary:function (serviceName) {

    var serviceName = this.get('serviceName');
    if (!serviceName) {
      return;
    }

    if (this.get('oldServiceName')) {
      // do not delete it!
      return;
    }

    var summaryView = this;
    var serviceStatus = summaryView.get('serviceStatus');
    $.each(serviceStatus, function (key, value) {
      if (key.toUpperCase() == serviceName) {
        summaryView.set('serviceStatus.' + key, true);
      } else {
        summaryView.set('serviceStatus.' + key, false);
      }
    });

    console.log('load ', serviceName, ' info');
    this.set('oldServiceName', serviceName);
    serviceName = serviceName.toLowerCase();
  }.observes('serviceName'),

  /*
   * Alerts panel not display for PIG, SQOOP and TEZ Service
   */
  isNoAlertsService: function () {
    var serviceName = this.get('service.serviceName');
    if (!serviceName) {
      return false;
    }
    var noAlertsServices = ['PIG', 'SQOOP', 'TEZ'];
    return noAlertsServices.indexOf(serviceName) > -1;
  }.property(''),

  gangliaUrl:function () {
    var gangliaUrl = App.router.get('clusterController.gangliaUrl');
    if (!gangliaUrl) return null;
    var svcName = this.get('service.serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPNameNode&tab=m&vn=";
          break;
        case 'mapreduce':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPJobTracker&tab=m&vn=";
          break;
        case 'hbase':
          gangliaUrl += "?r=hour&cs=&ce=&m=&s=by+name&c=HDPHBaseMaster&tab=m&vn=";
          break;
        default:
          break;
      }
    }
    return gangliaUrl;
  }.property('App.router.clusterController.gangliaUrl', 'service.serviceName'),

  didInsertElement:function () {
    var alertsController = this.get('alertsController');
    alertsController.loadAlerts(this.get('service.serviceName'), "SERVICE");
    alertsController.set('isUpdating', true);
    //TODO delegate style calculation to css
    // We have to make the height of the Alerts section
    // match the height of the Summary section.
    var summaryTable = document.getElementById('summary-info');
    var alertsList = document.getElementById('summary-alerts-list');
    if (summaryTable && alertsList) {
      var rows = $(summaryTable).find('tr');
      if (rows != null && rows.length > 0) {
        var minimumHeightSum = 20;
        var summaryActualHeight = summaryTable.clientHeight;
        // for summary window
        if (summaryActualHeight <= minimumHeightSum) {
          $(summaryTable).attr('style', "height:" + minimumHeightSum + "px;");
        }
      } else if (alertsList.clientHeight > 0) {
        $(summaryTable).append('<tr><td></td></tr>');
        $(summaryTable).attr('style', "height:" + alertsList.clientHeight + "px;");
      }
    }
  },
  willDestroyElement: function(){
    this.get('alertsController').set('isUpdating', false);
  },

  setAlertsWindowSize: function() {
    // for alerts window
    var summaryTable = document.getElementById('summary-info');
    var alertsList = document.getElementById('summary-alerts-list');
    var alertsNum = this.get('alerts').length;
    if (summaryTable && alertsList && alertsNum != null) {
      var summaryActualHeight = summaryTable.clientHeight;
      var alertsActualHeight = alertsNum * 60;
      var alertsMinHeight = 58;
      if (alertsNum == 0) {
        $(alertsList).attr('style', "height:" + alertsMinHeight + "px;");
      } else if ( alertsNum <= 4) {
        // set window size to actual alerts height
        $(alertsList).attr('style', "height:" + alertsActualHeight + "px;");
      } else {
        // set window size : sum of first 4 alerts height
        $(alertsList).attr('style', "height:" + 240 + "px;");
      }
      var curSize = alertsList.clientHeight;
      if ( summaryActualHeight >= curSize) {
        $(alertsList).attr('style', "height:" + summaryActualHeight + "px;");
      }
    }
  }.observes('alertsController.isLoaded'),

  clientHosts:App.Host.find(),

  clientHostsLength:function () {
    var text = this.t('services.service.summary.clientCount');
    var self = this;
    return text.format(self.get('clientHosts.length'));
  }.property('clientHosts'),

  clientComponents:function () {
    return App.HostComponent.find().filterProperty('isClient', true);
  }.property(),

  clientComponentsString:function () {
    var components = this.get('clientComponents');
    var names = [];
    components.forEach(function (component) {
      if (names.indexOf(component.get('displayName')) == -1) {
        names.push(component.get('displayName'));
      }
    });

    return names.length ? names.join(', ') : false;
  }.property('clientComponents')
});
