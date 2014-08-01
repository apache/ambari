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
require('views/main/service/service');

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
  /**
   *  @property {String} templatePathPrefix - base path for custom templates
   *    if you want to add custom template, add <service_name>.hbs file to
   *    templates/main/service/info/summary folder.
   */
  templatePathPrefix: 'templates/main/service/info/summary/',
  /** @property {Ember.View} serviceSummaryView - view to embed, computed in
   *  <code>loadServiceSummary()</code>
   */
  serviceSummaryView: null,
  /**
   * @property {Object} serviceCustomViewsMap - custom views to embed
   *
   */
  serviceCustomViewsMap: function() {
    return {
      HBASE: App.MainDashboardServiceHbaseView,
      HDFS: App.MainDashboardServiceHdfsView,
      MAPREDUCE: App.MainDashboardServiceMapreduceView,
      STORM: App.MainDashboardServiceStormView,
      YARN: App.MainDashboardServiceYARNView,
      FLUME: Em.View.extend({
        template: Em.Handlebars.compile('' +
          '<tr>' +
            '<td>' +
              '{{view App.MainDashboardServiceFlumeView serviceBinding="view.service"}}' +
            '</td>' +
          '</tr>')
      })
    }
  }.property('serviceName'),
  /** @property collapsedMetrics {object[]} - metrics list for collapsed section
   *    structure of element from list:
   *      @property {string} header - title for section
   *      @property {string} id - id of section for toggling, like: metric1
   *      @property {string} toggleIndex - passed to `data-parent` attribute, like: #metric1
   *      @property {Em.View} metricView - metric view class
   */
  collapsedSections: null,

  servicesHaveClients: function() {
    return App.get('services.hasClient');
  }.property('App.services.hasClient'),

  alertsControllerBinding: 'App.router.mainAlertsController',
  alerts: function () {
    return this.get('alertsController.alerts');
  }.property('alertsController.alerts'),

  noTemplateService: function () {
    var serviceName = this.get("service.serviceName");
    //services with only master components
    return serviceName == "WEBHCAT" || serviceName == "NAGIOS";
  }.property('controller.content'),

  hasManyServers: function () {
    return this.get('servers').length > 1;
  }.property('servers'),

  clientsHostText: function () {
    if (this.get('controller.content.installedClients').length == 0) {
      return '';
    } else if (this.get("hasManyClients")) {
      return Em.I18n.t('services.service.summary.viewHosts');
    } else {
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("hasManyClients"),

  hasManyClients: function () {
    return this.get('controller.content.installedClients').length > 1;
  }.property('service.installedClients'),

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

  mastersObj: function() {
    return this.get('service.hostComponents').filterProperty('isMaster', true);
  }.property('service'),

  /**
   * Contain array with list of client components models <code>App.ClientComponent</code>.
   * @type {Array}
   */
  clientObj: function () {
    var clientComponents = this.get('controller.content.clientComponents').toArray();
    return clientComponents.get('length') ? clientComponents : [];
  }.property('service.clientComponents.@each.totalCount'),

  /**
   * Contain array with list of slave components models <code>App.SlaveComponent</code>.
   * @type {Array}
   */
  slavesObj: function() {
    var slaveComponents = this.get('controller.content.slaveComponents').toArray();
    return slaveComponents.get('length') ? slaveComponents : [];
  }.property('service.slaveComponents.@each.totalCount', 'service.slaveComponents.@each.startedCount'),

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
          graphs = [[App.ChartServiceMetricsFlume_ChannelSizeMMA.extend(),
             App.ChartServiceMetricsFlume_ChannelSizeSum.extend(),
             App.ChartServiceMetricsFlume_IncommingMMA.extend(),
             App.ChartServiceMetricsFlume_IncommingSum],
             [App.ChartServiceMetricsFlume_OutgoingMMA,
               App.ChartServiceMetricsFlume_OutgoingSum,
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

  loadServiceSummary: function () {
    var serviceName = this.get('serviceName');
    var serviceSummaryView = null;

    if (!serviceName) {
      return;
    }

    if (this.get('oldServiceName')) {
      // do not delete it!
      return;
    }

    var customServiceView = this.get('serviceCustomViewsMap')[serviceName];
    if (customServiceView) {
      serviceSummaryView  = customServiceView.extend({
        service: this.get('service')
      });
    } else  {
      serviceSummaryView = Em.View.extend({
        templateName: this.get('templatePathPrefix') + 'base',
        content: this
      });
    }
    this.set('serviceSummaryView', serviceSummaryView);
    this.set('oldServiceName', serviceName);
  }.observes('serviceName'),

  /*
   * Alerts panel not display for PIG, SQOOP and TEZ Service
   */
  isNoAlertsService: function () {
    return !!this.get('service.serviceName') && App.get('services.clientOnly').contains(this.get('service.serviceName'));
  }.property(''),

  gangliaUrl:function () {
    var gangliaUrl = App.router.get('clusterController.gangliaUrl');
    if (!gangliaUrl) return null;
    var svcName = this.get('service.serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPSlaves&tab=m&vn=";
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
  }.observes('alertsController.isLoaded')
});
