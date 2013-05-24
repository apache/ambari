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
    $("div[rel=tooltip]").tooltip();
    $(".tooltip").remove();
  }
})

App.MainServiceInfoSummaryView = Em.View.extend({
  templateName: require('templates/main/service/info/summary'),
  attributes:null,
  serviceStatus:{
    hdfs:false,
    mapreduce:false,
    mapreduce2:false,
    hbase:false,
    zookeeper:false,
    oozie:false,
    hive:false,
    ganglia:false,
    nagios:false,
    hue: false
  },

  clients: function () {
    var service = this.get('controller.content');
    if (["OOZIE", "ZOOKEEPER", "HIVE", "MAPREDUCE2"].contains(service.get("id"))) {
      return service.get('hostComponents').filterProperty('isClient');
    }
    return [];
  }.property('controller.content'),

  hasManyServers: function () {
    if (this.get('servers').length > 1) {
      return true;
    }
    return false;
  }.property('servers'),

  hasManyClients: function () {
    if (this.get('clients').length > 1) {
      return true;
    }
    return false;
  }.property('clients'),

  servers: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER") {
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

  monitors: function () {
    var result = '';
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('hostComponents').filterProperty('isMaster', false);
      if (monitors.length) {
        result = monitors.length - 1 ? Em.I18n.t('services.service.info.summary.hostsRunningMonitor').format(monitors.length) : Em.I18n.t('services.service.info.summary.hostRunningMonitor');
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
    if (service.get("id") == "ZOOKEEPER") {
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
    if (["OOZIE", "ZOOKEEPER", "HIVE", "MAPREDUCE2"].contains(service.get("id"))) {
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
   * Get public host name (should be Master) for service
   * @param {String} serviceName - GANGLIA, NAGIOS etc
   * @return {*}
   */
  getServer: function(serviceName) {
    var service=this.get('controller.content');
    if(service.get("id") == serviceName) {
      return service.get("hostComponents").findProperty('isMaster', true).get("host").get("publicHostName");
    }
    else {
      return '';
    }
  },
  gangliaServer:function() {
    return this.getServer("GANGLIA");
  }.property('controller.content'),
  nagiosServer:function(){
    return this.getServer("NAGIOS");
  }.property('controller.content'),
  oozieServer:function(){
    return this.getServer("OOZIE");
  }.property('controller.content'),
  hueServer:function(){
    return this.getServer("HUE");
  }.property('controller.content'),

  /**
   * Array of the hostComponents for service
   */
  components: [],

  /**
   * Copy hostComponents from controller to view to avoid flickering Summary block while data is updating in the controller
   * rand - just marker in the Service model for determining that Service was updated (value changes in the service_mapper)
   */
  hostComponentsUpd: function() {
      var components = [];
      this.get('controller.content.hostComponents').forEach(function(component) {
        var obj = {};
        for(var prop in component){
          if( component.hasOwnProperty(prop)
            && prop.indexOf('__ember') < 0
            && prop.indexOf('_super') < 0
            && Ember.typeOf(component.get(prop)) !== 'function'
            ) {
            obj[prop] = component.get(prop);
          }
        }
        obj.displayName = component.get('displayName'); // this is computed property and wasn't copied in the top block of code
        components.push(obj);
      });
      this.set('components', components);
  }.observes('controller.content.rand', 'controller.content.hostComponents.@each.isMaster', 'controller.content.hostComponents.@each.host'),
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
        case 'mapreduce':
          svc = App.MapReduceService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
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

  gangliaUrl:function () {
    var gangliaUrl = App.router.get('clusterController.gangliaUrl');
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
    // We have to make the height of the Alerts section
    // match the height of the Summary section.
    var summaryTable = document.getElementById('summary-info');
    var alertsList = document.getElementById('summary-alerts-list');
    if (summaryTable && alertsList) {
      var rows = $(summaryTable).find('tr');
      if (rows != null && rows.length > 0) {
        var minimumHeight = 50;
        var calculatedHeight = summaryTable.clientHeight;
        if (calculatedHeight < minimumHeight) {
          $(alertsList).attr('style', "height:" + minimumHeight + "px;");
          $(summaryTable).append('<tr><td></td></tr>');
          $(summaryTable).attr('style', "height:" + minimumHeight + "px;");
        } else {
          $(alertsList).attr('style', "height:" + calculatedHeight + "px;");
        }
      } else if (alertsList.clientHeight > 0) {
        $(summaryTable).append('<tr><td></td></tr>');
        $(summaryTable).attr('style', "height:" + alertsList.clientHeight + "px;");
      }
    }
  },

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