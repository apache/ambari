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
var batchUtils = require('utils/batch_scheduled_requests');
require('views/main/service/service');
require('data/service_graph_config');

App.MainServiceInfoSummaryView = Em.View.extend(App.UserPref, {
  templateName: require('templates/main/service/info/summary'),
  /**
   * @property {Number} chunkSize - number of columns in Metrics section
   */
  chunkSize: 5,
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
      STORM: App.MainDashboardServiceStormView,
      YARN: App.MainDashboardServiceYARNView,
      RANGER: App.MainDashboardServiceRangerView,
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
        result = [
          {
            'host': servers[0].get('displayName'),
            'isComma': false,
            'isAnd': false
          }
        ];
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

  /*
   * 'Restart Required bar' start
   */
  componentsCount: null,
  hostsCount: null,

  alertsCount: function () {
    return this.get('controller.content.alertsCount');
  }.property('controller.content.alertsCount'),

  hasCriticalAlerts: function () {
    return this.get('controller.content.hasCriticalAlerts');
  }.property('controller.content.alertsCount'),

  /**
   * Define if service has alert definitions defined
   * @type {Boolean}
   */
  hasAlertDefinitions: function () {
    return App.AlertDefinition.find().someProperty('serviceName', this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName'),

  updateComponentInformation: function() {
    var hc = this.get('controller.content.restartRequiredHostsAndComponents');
    var hostsCount = 0;
    var componentsCount = 0;
    for (var host in hc) {
      hostsCount++;
      componentsCount += hc[host].length;
    }
    this.set('componentsCount', componentsCount);
    this.set('hostsCount', hostsCount);
  }.observes('controller.content.restartRequiredHostsAndComponents'),

  rollingRestartSlaveComponentName : function() {
    return batchUtils.getRollingRestartComponentName(this.get('serviceName'));
  }.property('serviceName'),
  rollingRestartActionName : function() {
    var label = null;
    var componentName = this.get('rollingRestartSlaveComponentName');
    if (componentName) {
      label = Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(componentName));
    }
    return label;
  }.property('rollingRestartSlaveComponentName'),

  restartAllStaleConfigComponents: function () {
    var self = this;
    var serviceDisplayName = this.get('service.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('service.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      var selectedService = self.get('service.id');
      batchUtils.restartAllServiceHostComponents(selectedService, true, query);
    }, bodyMessage);
  },
  rollingRestartStaleConfigSlaveComponents: function (componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('service.displayName'), this.get('service.passiveState') === "ON", true);
  },
  /*
   * 'Restart Required bar' ended
   */

   /*
   * Find the graph class associated with the graph name, and split
   * the array into sections of 5 for displaying on the page
   * (will only display rows with 5 items)
   */
  constructGraphObjects: function(graphNames) {
    var result = [], graphObjects = [], chunkSize = this.get('chunkSize');
    var self = this;

    if (!graphNames) {
      self.set('serviceMetricGraphs', []);
      self.set('isServiceMetricLoaded', true);
      return;
    }
    // load time range for current service from server
    self.getUserPref(self.get('persistKey')).complete(function () {
      var index = self.get('currentTimeRangeIndex');
      graphNames.forEach(function(graphName) {
        graphObjects.push(App["ChartServiceMetrics" + graphName].extend({
          currentTimeIndex : index
        }));
      });

      if (App.get('supports.customizedWidgets')) {
        graphObjects.push(Ember.View.extend({
          classNames: ['last-child'],
          template: Ember.Handlebars.compile('<div id="add-widget-action-box"><i class="icon-plus"></i></div>')
        }));
      }

      while(graphObjects.length) {
        result.push(graphObjects.splice(0, chunkSize));
      }
      self.set('serviceMetricGraphs', result);
      self.set('isServiceMetricLoaded', true);
    });
  },

  /**
   * Contains graphs for this particular service
   */
  serviceMetricGraphs: [],
  isServiceMetricLoaded: false,

  /**
   * Key-name to store time range in Persist
   * @type {string}
   */
  persistKey: function () {
    return 'time-range-service-' + this.get('service.serviceName');
  }.property(),

  getUserPrefSuccessCallback: function (response, request, data) {
    if (response) {
      console.log('Got persist value from server with key ' + data.key + '. Value is: ' + response);
      this.set('currentTimeRangeIndex', response);
    }
  },

  getUserPrefErrorCallback: function (request) {
    if (request.status == 404) {
      console.log('Persist did NOT find the key');
      this.postUserPref(this.get('persistKey'), 0);
      this.set('currentTimeRangeIndex', 0);
    }
  },

  /**
   * time range options for service metrics, a dropdown will list all options
   */
  timeRangeOptions: [
    {index: 0, name: Em.I18n.t('graphs.timeRange.hour'), seconds: 3600},
    {index: 1, name: Em.I18n.t('graphs.timeRange.twoHours'), seconds: 7200},
    {index: 2, name: Em.I18n.t('graphs.timeRange.fourHours'), seconds: 14400},
    {index: 3, name: Em.I18n.t('graphs.timeRange.twelveHours'), seconds: 43200},
    {index: 4, name: Em.I18n.t('graphs.timeRange.day'), seconds: 86400},
    {index: 5, name: Em.I18n.t('graphs.timeRange.week'), seconds: 604800},
    {index: 6, name: Em.I18n.t('graphs.timeRange.month'), seconds: 2592000},
    {index: 7, name: Em.I18n.t('graphs.timeRange.year'), seconds: 31104000}
  ],

  currentTimeRangeIndex: 0,
  currentTimeRange: function() {
    return this.get('timeRangeOptions').objectAt(this.get('currentTimeRangeIndex'));
  }.property('currentTimeRangeIndex'),

  /**
   * onclick handler for a time range option
   */
  setTimeRange: function (event) {
    var self = this;
    if (event && event.context) {
      self.postUserPref(self.get('persistKey'), event.context.index);
      self.set('currentTimeRangeIndex', event.context.index);
      var svcName = self.get('service.serviceName');
      if (svcName) {
        var result = [], graphObjects = [], chunkSize = this.get('chunkSize');
        App.service_graph_config[svcName.toLowerCase()].forEach(function(graphName) {
          graphObjects.push(App["ChartServiceMetrics" + graphName].extend({
            currentTimeIndex : event.context.index
          }));
        });
        while(graphObjects.length) {
          result.push(graphObjects.splice(0, chunkSize));
        }
        self.set('serviceMetricGraphs', result);
        self.set('isServiceMetricLoaded', true);
      }
    }
  },

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


  /**
   * Service metrics panel not displayed when metrics service (ex:Ganglia) is not in stack definition.
   */
  isNoServiceMetricsService: function() {
    return !App.get('services.serviceMetrics').length;
  }.property('App.services.serviceMetrics'),

  gangliaUrl:function () {
    var gangliaUrl = App.router.get('clusterController.gangliaUrl');
    if (!gangliaUrl) return null;
    var svcName = this.get('service.serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPSlaves&tab=m&vn=";
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

  willInsertElement: function () {
    App.router.get('updateController').updateServiceMetric(Em.K);
  },

  didInsertElement: function () {
    var svcName = this.get('service.serviceName');
    var isMetricsSupported = svcName != 'STORM' || App.get('isStormMetricsSupported');

    if (App.get('supports.customizedWidgets')) {
      var serviceName = this.get('controller.content.serviceName');
      var stackService = App.StackService.find().findProperty('serviceName', serviceName);
      if (stackService.get('isServiceWithWidgets')) {
        this.get('controller').loadWidgets();
      }
    }

    if (svcName && isMetricsSupported) {
      this.constructGraphObjects(App.service_graph_config[svcName.toLowerCase()]);
    }
    // adjust the summary table height
    var summaryTable = document.getElementById('summary-info');
    if (summaryTable) {
      var rows = $(summaryTable).find('tr');
      if (rows != null && rows.length > 0) {
        var minimumHeightSum = 20;
        var summaryActualHeight = summaryTable.clientHeight;
        // for summary window
        if (summaryActualHeight <= minimumHeightSum) {
          $(summaryTable).attr('style', "height:" + minimumHeightSum + "px;");
        }
      }
    }
  }
});
